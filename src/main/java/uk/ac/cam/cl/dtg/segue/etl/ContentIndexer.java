package uk.ac.cam.cl.dtg.segue.etl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.Validate;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.configuration.IsaacGuiceConfigurationModule;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacEventPage;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacNumericQuestion;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacSymbolicChemistryQuestion;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacSymbolicQuestion;
import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.configuration.SchoolLookupConfigurationModule;
import uk.ac.cam.cl.dtg.segue.configuration.SegueGuiceConfigurationModule;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentMapper;
import uk.ac.cam.cl.dtg.segue.database.GitDb;
import uk.ac.cam.cl.dtg.segue.dos.content.*;
import uk.ac.cam.cl.dtg.segue.search.SegueSearchOperationException;

import javax.annotation.Nullable;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Paths;
import java.util.*;

import static com.google.common.collect.Maps.immutableEntry;

/**
 * Created by Ian on 17/10/2016.
 */
public class ContentIndexer {
    private static final Logger log = LoggerFactory.getLogger(Content.class);

    private ElasticSearchIndexer es;
    private GitDb database;
    private ContentMapper mapper;

    public final void clearCache(final String version) {
        Validate.notBlank(version);

        if (es.hasIndex(version)) {
            es.expungeIndexFromSearchCache(version);
            // TODO: Sort out tags and units
            //tagsList.remove(version);
            //allUnits.remove(version);
        }
    }

    public static void main(String[] args) {


        Injector injector = Guice.createInjector(new EtlConfigurationModule());

        ContentIndexer indexer = injector.getInstance(ContentIndexer.class);

        try {
            indexer.loadAndIndexContent("d5e89976d7235142231fbdedc47739f96f9b5c25", true, true);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Inject
    public ContentIndexer(GitDb database, ElasticSearchIndexer es, ContentMapper mapper) {
        this.database = database;
        this.es = es;
        this.mapper = mapper;
    }

    private void loadAndIndexContent(String version, boolean includeUnpublished, boolean setLive) throws Exception {
        final Map<String, Map<String, Content>> typesToIndex;

        // now we have acquired the lock check if someone else has indexed this.
        boolean searchIndexed = es.hasIndex(version);
        if (searchIndexed) {
            log.info("Content already indexed: " + version);
            return;
        }

        log.info(String.format(
                "Rebuilding content index as sha (%s) does not exist in search provider.",
                version));

        Map<String, Content> contentCache = new HashMap<>();
        Set<String> tagsList = new HashSet<>();
        Map<String, String> allUnits = new HashMap<>();
        Map<Content, List<String>> indexProblemCache = new HashMap<>();

        buildGitContentIndex(version, includeUnpublished, contentCache, tagsList, allUnits, indexProblemCache);

        checkForContentErrors(version, contentCache, indexProblemCache);

        buildElasticSearchIndex(version, contentCache, tagsList, allUnits, indexProblemCache);

        // Verify the version requested is now available
        if (!es.hasIndex(version)) {
            throw new Exception(String.format("Failed to index version %s.", version));
        }

        // TODO: At the end of all this, if this is the new live version, set the index alias to point here
        es.addOrMoveIndexAlias("latest", version);
        if(setLive) {
            es.addOrMoveIndexAlias("live", version);
        }
    }

    /**
     * This method will populate the internal gitCache based on the content object files found for a given SHA.
     *
     * Currently it only looks for json files in the repository.
     *
     * @param sha
     *            - the version to index.
     * @return the map representing all indexed content.
     * @throws ContentManagerException
     */
    private synchronized void buildGitContentIndex(final String sha,
                                                   final boolean includeUnpublished,
                                                   Map<String, Content> contentCache,
                                                   Set<String> tagsList,
                                                   Map<String, String> allUnits,
                                                   Map<Content, List<String>> indexProblemCache)
            throws ContentManagerException {

        if (null == sha) {
            throw new ContentManagerException("SHA is null. Cannot index.");
        }

        Repository repository = database.getGitRepository();

        try {
            ObjectId commitId = repository.resolve(sha);

            if (null == commitId) {
                throw new ContentManagerException("Failed to buildGitIndex - Unable to locate resource with SHA: "
                        + sha);
            }

            TreeWalk treeWalk = database.getTreeWalk(sha, ".json");
            log.info("Populating git content cache based on sha " + sha + " ...");

            // Traverse the git repository looking for the .json files
            while (treeWalk.next()) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                ObjectLoader loader = repository.open(treeWalk.getObjectId(0));
                loader.copyTo(out);

                // setup object mapper to use preconfigured deserializer
                // module. Required to deal with type polymorphism
                ObjectMapper objectMapper = mapper.getSharedContentObjectMapper();

                Content content = null;
                try {
                    content = (Content) objectMapper.readValue(out.toString(), ContentBase.class);

                    // check if we only want to index published content
                    if (!includeUnpublished && !content.getPublished()) {
                        log.debug("Skipping unpublished content: " + content.getId());
                        continue;
                    }

                    content = this.augmentChildContent(content, treeWalk.getPathString(), null);

                    if (null != content) {
                        // add children (and parent) from flattened Set to
                        // cache if they have ids
                        for (Content flattenedContent : this.flattenContentObjects(content)) {
                            if (flattenedContent.getId() == null) {
                                continue;
                            }

                            if (flattenedContent.getId().contains(".")) {
                                // Otherwise, duplicate IDs with different content,
                                // therefore log an error
                                log.warn("Resource with invalid ID (" + content.getId()
                                        + ") detected in cache. Skipping " + treeWalk.getPathString());

                                this.registerContentProblem(sha, flattenedContent, "Index failure - Invalid ID "
                                        + flattenedContent.getId() + " found in file " + treeWalk.getPathString()
                                        + ". Must not contain restricted characters.", indexProblemCache);
                                continue;
                            }

                            // check if we have seen this key before if
                            // we have then we don't want to add it
                            // again
                            if (!contentCache.containsKey(flattenedContent.getId())) {
                                // It must be new so we can add it
                                log.debug("Loading into cache: " + flattenedContent.getId() + "("
                                        + flattenedContent.getType() + ")" + " from " + treeWalk.getPathString());
                                contentCache.put(flattenedContent.getId(), flattenedContent);
                                registerTagsWithVersion(sha, flattenedContent.getTags(), tagsList);

                                // If this is a numeric question, extract any
                                // units from its answers.

                                if (flattenedContent instanceof IsaacNumericQuestion) {
                                    registerUnitsWithVersion(sha, (IsaacNumericQuestion) flattenedContent, allUnits);
                                }

                                continue; // our work here is done
                            }

                            // shaCache contains key already, compare the
                            // content
                            if (contentCache.get(flattenedContent.getId()).equals(flattenedContent)) {
                                // content is the same therefore it is just
                                // reuse of a content object so that is
                                // fine.
                                log.debug("Resource (" + content.getId() + ") already seen in cache. Skipping "
                                        + treeWalk.getPathString());
                                continue;
                            }

                            // Otherwise, duplicate IDs with different content,
                            // therefore log an error
                            log.warn("Resource with duplicate ID (" + content.getId()
                                    + ") detected in cache. Skipping " + treeWalk.getPathString());
                            this.registerContentProblem(sha, flattenedContent,
                                    "Index failure - Duplicate ID found in file " + treeWalk.getPathString() + " and "
                                            + contentCache.get(flattenedContent.getId()).getCanonicalSourceFile()
                                            + " only one will be available", indexProblemCache);
                        }
                    }
                } catch (JsonMappingException e) {
                    log.warn(String.format("Unable to parse the json file found %s as a content object. "
                            + "Skipping file due to error: \n %s", treeWalk.getPathString(), e.getMessage()));
                    Content dummyContent = new Content();
                    dummyContent.setCanonicalSourceFile(treeWalk.getPathString());
                    this.registerContentProblem(sha, dummyContent, "Index failure - Unable to parse json file found - "
                            + treeWalk.getPathString() + ". The following error occurred: " + e.getMessage(), indexProblemCache);
                } catch (IOException e) {
                    log.error("IOException while trying to parse " + treeWalk.getPathString(), e);
                    Content dummyContent = new Content();
                    dummyContent.setCanonicalSourceFile(treeWalk.getPathString());
                    this.registerContentProblem(sha, dummyContent,
                            "Index failure - Unable to read the json file found - " + treeWalk.getPathString()
                                    + ". The following error occurred: " + e.getMessage(), indexProblemCache);
                }
            }

            repository.close();
            log.debug("Tags available " + tagsList);
            log.debug("All units: " + allUnits);
            log.info("Git content cache population for " + sha + " completed!");

        } catch (IOException e) {
            log.error("IOException while trying to access git repository. ", e);
            throw new ContentManagerException("Unable to index content, due to an IOException.");
        }
    }

    /**
     * Augments all child objects recursively to include additional information.
     *
     * This should be done before saving to the local gitCache in memory storage.
     *
     * This method will also attempt to reconstruct object id's of nested content such that they are unique to the page
     * by default.
     *
     * @param content
     *            - content to augment
     * @param canonicalSourceFile
     *            - source file to add to child content
     * @param parentId
     *            - used to construct nested ids for child elements.
     * @return Content object with new reference
     */
    private Content augmentChildContent(final Content content, final String canonicalSourceFile,
            @Nullable final String parentId) {
        if (null == content) {
            return null;
        }

        // If this object is of type question then we need to give it a random
        // id if it doesn't have one.
        if (content instanceof Question && content.getId() == null) {
            log.warn("Found question without id " + content.getTitle() + " " + canonicalSourceFile);
        }

        // Try to figure out the parent ids.
        String newParentId = null;
        if (null == parentId && content.getId() != null) {
            newParentId = content.getId();
        } else {
            if (content.getId() != null) {
                newParentId = parentId + Constants.ID_SEPARATOR + content.getId();
            } else {
                newParentId = parentId;
            }
        }

        content.setCanonicalSourceFile(canonicalSourceFile);

        if (!content.getChildren().isEmpty()) {
            for (ContentBase cb : content.getChildren()) {
                if (cb instanceof Content) {
                    Content c = (Content) cb;

                    this.augmentChildContent(c, canonicalSourceFile, newParentId);
                }
            }
        }

        if (content instanceof Choice) {
            Choice choice = (Choice) content;
            this.augmentChildContent((Content) choice.getExplanation(), canonicalSourceFile,
                    newParentId);
        }

        // TODO: hack to get hints to apply as children
        if (content instanceof Question) {
            Question question = (Question) content;
            if (question.getHints() != null) {
                for (ContentBase cb : question.getHints()) {
                    Content c = (Content) cb;
                    this.augmentChildContent(c, canonicalSourceFile, newParentId);
                }
            }

            // Augment question answers
            if (question.getAnswer() != null) {
                Content answer = (Content) question.getAnswer();
                if (answer.getChildren() != null) {
                    for (ContentBase cb : answer.getChildren()) {
                        Content c = (Content) cb;
                        this.augmentChildContent(c, canonicalSourceFile, newParentId);
                    }
                }
            }

            if (content instanceof ChoiceQuestion) {
                ChoiceQuestion choiceQuestion = (ChoiceQuestion) content;
                if (choiceQuestion.getChoices() != null) {
                    for (ContentBase cb : choiceQuestion.getChoices()) {
                        Content c = (Content) cb;
                        this.augmentChildContent(c, canonicalSourceFile, newParentId);
                    }
                }
            }
        }

        // try to determine if we have media as fields to deal with in this class
        Method[] methods = content.getClass().getDeclaredMethods();
        for (Method method : methods) {
            if (Media.class.isAssignableFrom(method.getReturnType())) {
                try {
                    Media media = (Media) method.invoke(content);
                    if (media != null) {
                        media.setSrc(fixMediaSrc(canonicalSourceFile, media.getSrc()));
                    }
                } catch (SecurityException | IllegalAccessException | IllegalArgumentException
                        | InvocationTargetException e) {
                    log.error("Unable to access method using reflection: attempting to fix Media Src", e);
                }
            }
        }

        if (content instanceof Media) {
            Media media = (Media) content;
            media.setSrc(fixMediaSrc(canonicalSourceFile, media.getSrc()));

            // for tracking purposes we want to generate an id for all image content objects.
            if (media.getId() == null && media.getSrc() != null) {
                media.setId(parentId + Constants.ID_SEPARATOR
                        + Arrays.toString(Base64.encodeBase64(media.getSrc().getBytes())));
            }
        }

        // Concatenate the parentId with our id to get a fully qualified
        // identifier.
        if (content.getId() != null && parentId != null) {
            content.setId(parentId + Constants.ID_SEPARATOR + content.getId());
        }

        return content;
    }

    /**
     * @param canonicalSourceFile
     *            - the canonical path to use for concat operations.
     * @param originalSrc
     *            - to modify
     * @return src with relative paths fixed.
     */
    private String fixMediaSrc(final String canonicalSourceFile, final String originalSrc) {
        if (originalSrc != null && !(originalSrc.startsWith("http://") || originalSrc.startsWith("https://"))) {
            return FilenameUtils.normalize(FilenameUtils.getPath(canonicalSourceFile) + originalSrc, true);
        }
        return originalSrc;
    }

    /**
     * Unpack the content objects into one big set. Useful for validation but could produce a very large set
     *
     * @param content
     *            content object to flatten
     * @return Set of content objects comprised of all children and the parent.
     */
    private Set<Content> flattenContentObjects(final Content content) {
        Set<Content> setOfContentObjects = new HashSet<>();
        if (!content.getChildren().isEmpty()) {

            List<ContentBase> children = content.getChildren();

            for (ContentBase child : children) {
                setOfContentObjects.add((Content) child);
                setOfContentObjects.addAll(flattenContentObjects((Content) child));
            }
        }

        setOfContentObjects.add(content);

        return setOfContentObjects;
    }


    /**
     * Helper method to register problems with content objects.
     *
     * @param version
     *            - to which the problem relates
     * @param c
     *            - Partial content object to represent the object that has problems.
     * @param message
     *            - Error message to associate with the problem file / content.
     */
    private synchronized void registerContentProblem(final String version, final Content c, final String message, Map<Content, List<String>> indexProblemCache) {
        Validate.notNull(c);

        // try and make sure each dummy content object has a title
        if (c.getTitle() == null) {
            c.setTitle(Paths.get(c.getCanonicalSourceFile()).getFileName().toString());
        }

        if (!indexProblemCache.containsKey(c)) {
            indexProblemCache.put(c, new ArrayList<String>());
        }

        indexProblemCache.get(c).add(message);//.replace("_", "\\_"));
    }

    /**
     * Helper function to build up a set of used tags for each version.
     *
     * @param version
     *            - version to register the tag for.
     * @param tags
     *            - set of tags to register.
     */
    private synchronized void registerTagsWithVersion(final String version, final Set<String> tags, Set<String> tagsList) {
        Validate.notBlank(version);

        if (null == tags || tags.isEmpty()) {
            // don't do anything.
            return;
        }

        Set<String> newTagSet = Sets.newHashSet();

        // sanity check that tags are trimmed.
        for (String tag : tags) {
            newTagSet.add(tag.trim());
        }

        tagsList.addAll(newTagSet);
    }

    /**
     * Helper function to accumulate the set of all units used in numeric question answers.
     *
     * @param version
     *            - version to register the units for.
     * @param q
     *            - numeric question from which to extract units.
     */
    private synchronized void registerUnitsWithVersion(final String version, final IsaacNumericQuestion q, Map<String, String> allUnits) {

        HashMap<String, String> newUnits = Maps.newHashMap();

        for (Choice c : q.getChoices()) {
            if (c instanceof Quantity) {
                Quantity quantity = (Quantity) c;

                if (!quantity.getUnits().isEmpty()) {
                    String units = quantity.getUnits();
                    String cleanKey = units.replace("\t", "").replace("\n", "").replace(" ", "");

                    // May overwrite previous entry, doesn't matter as there is
                    // no mechanism by which to choose a winner
                    newUnits.put(cleanKey, units);
                }
            }
        }

        if (newUnits.isEmpty()) {
            // This question contained no units.
            return;
        }

        allUnits.putAll(newUnits);
    }




    /**
     * This method will send off the information in the git cache to the search provider for indexing.
     *
     * @param sha
     *            - the version in the git cache to send to the search provider.
     * @param gitCache
     *            a map that represents indexed content for a given sha.
     */
    private synchronized void buildElasticSearchIndex(final String sha,
                                                      final Map<String, Content> gitCache,
                                                      Set<String> tagsList,
                                                      Map<String, String> allUnits,
                                                      Map<Content, List<String>> indexProblemCache) {
        if (es.hasIndex(sha)) {
            log.info("Deleting existing index for version " + sha);
            es.expungeIndexFromSearchCache(sha);
        }

        log.info("Building search index for: " + sha);

        // setup object mapper to use pre-configured deserializer module.
        // Required to deal with type polymorphism
        List<Map.Entry<String, String>> contentToIndex = Lists.newArrayList();
        ObjectMapper objectMapper = mapper.generateNewPreconfiguredContentMapper();
        for (Content content : gitCache.values()) {
            try {
                contentToIndex.add(immutableEntry(content.getId(), objectMapper.writeValueAsString(content)));
            } catch (JsonProcessingException e) {
                log.error("Unable to serialize content object: " + content.getId()
                        + " for indexing with the search provider.", e);
                this.registerContentProblem(sha, content, "Search Index Error: " + content.getId()
                        + content.getCanonicalSourceFile() + " Exception: " + e.toString(), indexProblemCache);
            }
        }

        List<Map.Entry<String, String>> metadataToIndex = Lists.newArrayList();
        metadataToIndex.add(immutableEntry("version", sha));

        try {
            metadataToIndex.add(immutableEntry("tags", objectMapper.writeValueAsString(tagsList)));
            metadataToIndex.add(immutableEntry("units", objectMapper.writeValueAsString(allUnits)));
            metadataToIndex.add(immutableEntry("content_errors", objectMapper.writeValueAsString(indexProblemCache)));
        } catch (JsonProcessingException e) {
            log.error("Unable to serialise tags, units or content errors.");
        }


        try {
            es.bulkIndex(sha, "content", contentToIndex);
            es.bulkIndex(sha, "metadata", metadataToIndex);

            log.info("Search index request sent for: " + sha);
        } catch (SegueSearchOperationException e) {
            log.error("Error whilst trying to perform bulk index operation.", e);
        }
    }


    /*
     * This method will attempt to traverse the cache to ensure that all content references are valid.
     *
     * @param sha
     *            version to validate integrity of.
     * @param gitCache
     *            Data structure containing all content for a given sha.
     * @return True if we are happy with the integrity of the git repository, False if there is something wrong.
     */
    private boolean checkForContentErrors(final String sha, final Map<String, Content> gitCache,
                                          Map<Content, List<String>> indexProblemCache) {
        log.info(String.format("Starting content Validation (%s).", sha));
        Set<Content> allObjectsSeen = new HashSet<>();
        Set<String> expectedIds = new HashSet<>();
        Set<String> definedIds = new HashSet<>();
        Set<String> missingContent = new HashSet<>();
        Map<String, Content> whoAmI = new HashMap<>();

        // Build up a set of all content (and content fragments for validation)
        for (Content c : gitCache.values()) {
// TODO work out why this was here and why removing it didn't seem to do anything!
//            if (c instanceof IsaacSymbolicQuestion) {
//                // do not validate these questions for now.
//                continue;
//            }
            allObjectsSeen.addAll(this.flattenContentObjects(c));
        }

        // Start looking for issues in the flattened content data
        for (Content c : allObjectsSeen) {
            // add the id to the list of defined ids
            if (c.getId() != null) {
                definedIds.add(c.getId());
            }

            // add the ids to the list of expected ids
            if (c.getRelatedContent() != null) {
                expectedIds.addAll(c.getRelatedContent());
                // record which content object was referencing which ID
                for (String id : c.getRelatedContent()) {
                    whoAmI.put(id, c);
                }
            }

            // ensure content does not have children and a value
            if (c.getValue() != null && !c.getChildren().isEmpty()) {
                String id = c.getId();
                String firstLine = "Content";
                if (id != null) {
                    firstLine += ": " + id;
                }

                this.registerContentProblem(sha, c, firstLine + " in " + c.getCanonicalSourceFile()
                        + " found with both children and a value. "
                        + "Content objects are only allowed to have one or the other.", indexProblemCache);

                log.error("Invalid content item detected: The object with ID (" + id
                        + ") has both children and a value.");
            }

            // content type specific checks
            if (c instanceof Media) {
                Media f = (Media) c;

                if (f.getSrc() != null
                        && !f.getSrc().startsWith("http") && !database.verifyGitObject(sha, f.getSrc())) {
                    this.registerContentProblem(sha, c, "Unable to find Image: " + f.getSrc()
                            + " in Git. Could the reference be incorrect? SourceFile is " + c.getCanonicalSourceFile(), indexProblemCache);
                }

                // check that there is some alt text.
                if (f.getAltText() == null || f.getAltText().isEmpty()) {
                    this.registerContentProblem(sha, c, "No altText attribute set for media element: " + f.getSrc()
                            + " in Git source file " + c.getCanonicalSourceFile(), indexProblemCache);
                }
            }
            if (c instanceof Question && c.getId() == null) {
                this.registerContentProblem(sha, c, "Question: " + c.getTitle() + " in " + c.getCanonicalSourceFile()
                        + " found without a unqiue id. " + "This question cannot be logged correctly.", indexProblemCache);
            }
            // TODO: remove reference to isaac specific types from here.
            if (c instanceof ChoiceQuestion
                    && !(c.getType().equals("isaacQuestion"))) {
                ChoiceQuestion question = (ChoiceQuestion) c;

                if (question.getChoices() == null || question.getChoices().isEmpty()) {
                    this.registerContentProblem(sha, question,
                            "Question: " + question.getId() + " found without any choice metadata. "
                                    + "This question will always be automatically " + "marked as incorrect", indexProblemCache);
                } else {
                    boolean correctOptionFound = false;
                    for (Choice choice : question.getChoices()) {
                        if (choice.isCorrect()) {
                            correctOptionFound = true;
                        }
                    }

                    if (!correctOptionFound) {
                        this.registerContentProblem(sha, question,
                                "Question: " + question.getId() + " found without a correct answer. "
                                        + "This question will always be automatically marked as incorrect", indexProblemCache);
                    }
                }
            }

            if (c instanceof EmailTemplate) {
                EmailTemplate e = (EmailTemplate) c;
                if (e.getPlainTextContent() == null) {
                    this.registerContentProblem(sha, c,
                            "Email template should always have plain text content field", indexProblemCache);
                }

                if (e.getReplyToEmailAddress() != null && null == e.getReplyToName()) {
                    this.registerContentProblem(sha, c,
                            "Email template contains replyToEmailAddress but not replyToName", indexProblemCache);
                }
            }

            if (c instanceof IsaacEventPage) {
                IsaacEventPage e = (IsaacEventPage) c;
                if (e.getEndDate() != null && e.getEndDate().before(e.getDate())) {
                    this.registerContentProblem(sha, c, "Event has end date before start date", indexProblemCache);
                }
            }

            // TODO: the following things are all highly Isaac specific. I guess they should be elsewhere . . .
            // Find quantities with values that cannot be parsed as numbers.
            if (c instanceof IsaacNumericQuestion) {
                IsaacNumericQuestion q = (IsaacNumericQuestion) c;
                for (Choice choice : q.getChoices()) {
                    if (choice instanceof Quantity) {
                        Quantity quantity = (Quantity) choice;

                        try {
                            //noinspection ResultOfMethodCallIgnored
                            Double.parseDouble(quantity.getValue());
                        } catch (NumberFormatException e) {
                            this.registerContentProblem(sha, c,
                                    "Numeric Question: " + q.getId() + " has Quantity (" + quantity.getValue()
                                            + ")  with value that cannot be interpreted as a number. "
                                            + "Users will never be able to match this answer.", indexProblemCache);
                        }
                    } else if (q.getRequireUnits()) {
                        this.registerContentProblem(sha, c, "Numeric Question: " + q.getId() + " has non-Quantity Choice ("
                                + choice.getValue() + "). It must be deleted and a new Quantity Choice created.", indexProblemCache);
                    }
                }

            }

            // Find Symbolic Questions with broken properties. Need to exclude Chemistry questions!
            if (c instanceof IsaacSymbolicQuestion) {
                if (c.getClass().equals(IsaacSymbolicQuestion.class)) {
                    IsaacSymbolicQuestion q = (IsaacSymbolicQuestion) c;
                    for (String sym : q.getAvailableSymbols()) {
                        if (sym.contains("\\")) {
                            this.registerContentProblem(sha, c, "Symbolic Question: " + q.getId() + " has availableSymbol ("
                                    + sym + ") which contains a '\\' character.", indexProblemCache);
                        }
                    }
                    for (Choice choice : q.getChoices()) {
                        if (choice instanceof Formula) {
                            Formula f = (Formula) choice;
                            if (f.getPythonExpression().contains("\\")) {
                                this.registerContentProblem(sha, c, "Symbolic Question: " + q.getId() + " has Formula ("
                                        + choice.getValue() + ") with pythonExpression which contains a '\\' character.", indexProblemCache);
                            } else if (f.getPythonExpression() == null || f.getPythonExpression().isEmpty()) {
                                this.registerContentProblem(sha, c, "Symbolic Question: " + q.getId() + " has Formula ("
                                        + choice.getValue() + ") with empty pythonExpression!", indexProblemCache);
                            }
                        } else {
                            this.registerContentProblem(sha, c, "Symbolic Question: " + q.getId() + " has non-Formula Choice ("
                                    + choice.getValue() + "). It must be deleted and a new Formula Choice created.", indexProblemCache);
                        }
                    }
                } else if (c.getClass().equals(IsaacSymbolicChemistryQuestion.class)) {
                    IsaacSymbolicChemistryQuestion q = (IsaacSymbolicChemistryQuestion) c;
                    for (Choice choice : q.getChoices()) {
                        if (choice instanceof ChemicalFormula) {
                            ChemicalFormula f = (ChemicalFormula) choice;
                            if (f.getMhchemExpression() == null || f.getMhchemExpression().isEmpty()) {
                                this.registerContentProblem(sha, c, "Chemistry Question: " + q.getId() + " has ChemicalFormula"
                                        + " with empty mhchemExpression!", indexProblemCache);
                            }
                        } else {
                            this.registerContentProblem(sha, c, "Chemistry Question: " + q.getId() + " has non-ChemicalFormula Choice ("
                                    + choice.getValue() + "). It must be deleted and a new ChemicalFormula Choice created.", indexProblemCache);
                        }
                    }
                }
            }
        }

        if (expectedIds.equals(definedIds) && missingContent.isEmpty()) {
            return true;
        } else {
            expectedIds.removeAll(definedIds);
            missingContent.addAll(expectedIds);

            for (String id : missingContent) {
                this.registerContentProblem(sha, whoAmI.get(id), "This id (" + id + ") was referenced by "
                        + whoAmI.get(id).getCanonicalSourceFile() + " but the content with that "
                        + "ID cannot be found.", indexProblemCache);
            }
            if (missingContent.size() > 0) {
                log.warn("Referential integrity broken for (" + missingContent.size() + ") related Content items. "
                        + "The following ids are referenced but do not exist: " + expectedIds.toString());
            }
        }
        log.info(String.format("Validation processing (%s) complete. There are %s files with content problems", sha,
                indexProblemCache.size()));

        return false;
    }
//
//
//
///*
//    @Override
//    public void setIndexRestriction(final boolean loadOnlyPublishedContent) {
//        this.indexOnlyPublishedParentContent = loadOnlyPublishedContent;
//    }*/

    // GitContentManager ensureCache


}
