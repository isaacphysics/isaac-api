package uk.ac.cam.cl.dtg.segue.etl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.Validate;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.elasticsearch.action.ActionRequestValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacEventPage;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacNumericQuestion;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacQuiz;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacQuizSection;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacSymbolicChemistryQuestion;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacSymbolicQuestion;
import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentMapper;
import uk.ac.cam.cl.dtg.segue.database.GitDb;
import uk.ac.cam.cl.dtg.segue.dos.content.*;
import uk.ac.cam.cl.dtg.segue.search.SegueSearchException;

import javax.annotation.Nullable;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.google.common.collect.Maps.immutableEntry;
import static uk.ac.cam.cl.dtg.segue.api.Constants.CONTENT_INDEX_TYPE;

/**
 * Created by Ian on 17/10/2016.
 */
public class ContentIndexer {
    private static final Logger log = LoggerFactory.getLogger(Content.class);

    private static ConcurrentHashMap<String, Boolean> versionLocks = new ConcurrentHashMap<>();

    private ElasticSearchIndexer es;
    private GitDb database;
    private ContentMapper mapper;

    private static final int MEDIA_FILE_SIZE_LIMIT = 300 * 1024; // Bytes

    @Inject
    public ContentIndexer(GitDb database, ElasticSearchIndexer es, ContentMapper mapper) {
        this.database = database;
        this.es = es;
        this.mapper = mapper;
    }


    void loadAndIndexContent(String version) throws Exception, VersionLockedException {

        // Take version lock or fail
        Boolean alreadyLocked = versionLocks.putIfAbsent(version, true);

        if (Boolean.TRUE.equals(alreadyLocked)) {
            throw new VersionLockedException(version);
        }

        log.info("Acquired lock for version " + version + ". Indexing.");

        try {

            database.fetchLatestFromRemote();

            // now we have acquired the lock check if someone else has indexed this.
            boolean searchIndexed = es.hasIndex(version, CONTENT_INDEX_TYPE.CONTENT.toString());
            if (searchIndexed) {
                //log.info("Index already exists. Deleting.");
                //es.expungeIndexFromSearchCache(version);
                log.info("Content already indexed: " + version);
                return;
            }

            log.info(String.format(
                    "Rebuilding content index as sha (%s) does not exist in search provider.",
                    version));

            Map<String, Content> contentCache = new HashMap<>();
            Set<String> tagsList = new HashSet<>();
            Map<String, String> allUnits = new HashMap<>();
            Map<String, String> publishedUnits = new HashMap<>();
            Map<Content, List<String>> indexProblemCache = new HashMap<>();

            buildGitContentIndex(version, true, contentCache, tagsList, allUnits, publishedUnits, indexProblemCache);

            checkForContentErrors(version, contentCache, indexProblemCache);

            buildElasticSearchIndex(version, contentCache, tagsList, allUnits, publishedUnits, indexProblemCache);

            // Verify the version requested is now available
            if (!es.hasIndex(version, CONTENT_INDEX_TYPE.CONTENT.toString())) {
                throw new Exception(String.format("Failed to index version %s. Don't know why.", version));
            }

            log.info("Finished indexing version " + version);

        } finally {
            versionLocks.remove(version);
        }

    }

    void setNamedVersion(String alias, String version) {
        List<String> allContentTypes = Arrays.stream(CONTENT_INDEX_TYPE.values())
                .map((contentIndexType) -> contentIndexType.toString()).collect(Collectors.toList());
        es.addOrMoveIndexAlias(alias, version, allContentTypes);
    }

    void setLatestVersion(String version) {
        this.setNamedVersion("latest", version);
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
                                                   final Map<String, Content> contentCache,
                                                   final Set<String> tagsList,
                                                   final Map<String, String> allUnits,
                                                   final Map<String, String> publishedUnits,
                                                   final Map<Content, List<String>> indexProblemCache)
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

                Content content;
                try {
                    content = (Content) objectMapper.readValue(out.toString(), ContentBase.class);

                    // check if we only want to index published content
                    if (!includeUnpublished && !content.getPublished()) {
                        log.debug("Skipping unpublished content: " + content.getId());
                        continue;
                    }

                    content = this.augmentChildContent(content, treeWalk.getPathString(), null, content.getPublished());

                    if (null != content) {
                        // Walk the content for site-wide searchable fields
                        StringBuilder searchableContentBuilder = new StringBuilder();
                        this.collateSearchableContent(content, searchableContentBuilder);
                        content.setSearchableContent(searchableContentBuilder.toString());

                        // add children (and parent) from flattened Set to
                        // cache if they have ids
                        for (Content flattenedContent : this.flattenContentObjects(content)) {
                            if (flattenedContent.getId() == null) {
                                continue;
                            }

                            // Prevents ETL indexing of quizzes that contain anything that is not an IsaacQuizSection
                            // in the top-level children array.
                            // NOTE: I'm not sure this is the right place for this but I couldn't find a better one.
                            // This also seems to be the only time we can prevent a file from being indexed entirely.
                            if (flattenedContent instanceof IsaacQuiz) {
                                List<ContentBase> children = flattenedContent.getChildren();
                                if (children.stream().anyMatch(c -> !(c instanceof IsaacQuizSection))) {
                                    log.debug("IsaacQuiz (" + flattenedContent.getId()
                                           + ") contains top-level non-quiz sections. Skipping.");
                                    this.registerContentProblem(flattenedContent, "Index failure - Invalid "
                                           + "content type among quiz sections. Quizzes can only contain quiz sections "
                                           + "in the top-level children array.", indexProblemCache);
                                    continue;
                                }
                            }

                            if (flattenedContent.getId().length() > 512) {
                                log.debug("Content ID too long: " + flattenedContent.getId());
                                this.registerContentProblem(flattenedContent, "Content ID too long: " + flattenedContent.getId(), indexProblemCache);
                                continue;
                            }

                            if (flattenedContent.getId().contains(".")) {
                                // Otherwise, duplicate IDs with different content,
                                // therefore log an error
                                log.debug("Resource with invalid ID (" + content.getId()
                                        + ") detected in cache. Skipping " + treeWalk.getPathString());

                                this.registerContentProblem(flattenedContent, "Index failure - Invalid ID "
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
                                registerTags(flattenedContent.getTags(), tagsList);

                                // If this is a numeric question, extract any
                                // units from its answers.

                                if (flattenedContent instanceof IsaacNumericQuestion) {
                                    registerUnits((IsaacNumericQuestion) flattenedContent, allUnits, publishedUnits);
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
                            log.debug("Resource with duplicate ID (" + content.getId()
                                    + ") detected in cache. Skipping " + treeWalk.getPathString());
                            this.registerContentProblem(flattenedContent,
                                    "Index failure - Duplicate ID found in file " + treeWalk.getPathString() + " and "
                                            + contentCache.get(flattenedContent.getId()).getCanonicalSourceFile()
                                            + " only one will be available", indexProblemCache);
                        }
                    }
                } catch (JsonMappingException e) {
                    log.debug(String.format("Unable to parse the json file found %s as a content object. "
                            + "Skipping file due to error: \n %s", treeWalk.getPathString(), e.getMessage()));
                    Content dummyContent = new Content();
                    dummyContent.setCanonicalSourceFile(treeWalk.getPathString());
                    this.registerContentProblem(dummyContent, "Index failure - Unable to parse json file found - "
                            + treeWalk.getPathString() + ". The following error occurred: " + e.getMessage(), indexProblemCache);
                } catch (IOException e) {
                    log.error("IOException while trying to parse " + treeWalk.getPathString(), e);
                    Content dummyContent = new Content();
                    dummyContent.setCanonicalSourceFile(treeWalk.getPathString());
                    this.registerContentProblem(dummyContent,
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
            @Nullable final String parentId, final boolean parentPublished) {
        if (null == content) {
            return null;
        }

        // If this object is of type question then we need to give it a random
        // id if it doesn't have one.
        if (content instanceof Question && content.getId() == null) {
            log.debug("Found question without id " + content.getTitle() + " " + canonicalSourceFile);
        }

        // Try to figure out the parent ids.
        String newParentId;
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

                    this.augmentChildContent(c, canonicalSourceFile, newParentId, parentPublished);
                }
            }
        }

        if (content instanceof Choice) {
            Choice choice = (Choice) content;
            this.augmentChildContent((Content) choice.getExplanation(), canonicalSourceFile,
                    newParentId, parentPublished);
        }

        // TODO: hack to get hints to apply as children
        if (content instanceof Question) {
            Question question = (Question) content;
            if (question.getHints() != null) {
                for (ContentBase cb : question.getHints()) {
                    Content c = (Content) cb;
                    this.augmentChildContent(c, canonicalSourceFile, newParentId, parentPublished);
                }
            }

            // Augment question answers
            if (question.getAnswer() != null) {
                Content answer = (Content) question.getAnswer();
                if (answer.getChildren() != null) {
                    for (ContentBase cb : answer.getChildren()) {
                        Content c = (Content) cb;
                        this.augmentChildContent(c, canonicalSourceFile, newParentId, parentPublished);
                    }
                }
            }

            if (content instanceof ChoiceQuestion) {
                ChoiceQuestion choiceQuestion = (ChoiceQuestion) content;
                if (choiceQuestion.getChoices() != null) {
                    for (ContentBase cb : choiceQuestion.getChoices()) {
                        Content c = (Content) cb;
                        this.augmentChildContent(c, canonicalSourceFile, newParentId, parentPublished);
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
                        + Base64.encodeBase64String(media.getSrc().getBytes()));
            }
        }

        // Concatenate the parentId with our id to get a fully qualified
        // identifier.
        if (content.getId() != null && parentId != null) {
            content.setId(parentId + Constants.ID_SEPARATOR + content.getId());
            content.setPublished(parentPublished);
        }

        return content;
    }

    private void collateSearchableContent(final Content content, final StringBuilder searchableContentBuilder) {
        if (null != content) {
            // Add the fields of interest to the string builder
            if (null != content.getTitle()) {
                searchableContentBuilder.append(content.getTitle() + "\n");
            }
            if (null != content.getValue()) {
                searchableContentBuilder.append(content.getValue() + "\n");
            }

            // Repeat the process for each child
            if (!content.getChildren().isEmpty()) {
                for (ContentBase childContentBase : content.getChildren()) {
                    if (childContentBase instanceof Content) {
                        Content child = (Content) childContentBase;
                        this.collateSearchableContent(child, searchableContentBuilder);
                    }
                }
            }
        }
    }

    /**
     * @param canonicalSourceFile
     *            - the canonical path to use for concat operations.
     * @param originalSrc
     *            - to modify
     * @return src with relative paths fixed.
     */
    private String fixMediaSrc(final String canonicalSourceFile, final String originalSrc) {
        if (originalSrc != null && !(originalSrc.startsWith("http://") || originalSrc.startsWith("https://") || originalSrc.startsWith("/assets/"))) {
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
     *            - to which the problem relates
     * @param c
     *            - Partial content object to represent the object that has problems.
     * @param message
     *            - Error message to associate with the problem file / content.
     */
    private synchronized void registerContentProblem(final Content c, final String message, Map<Content, List<String>> indexProblemCache) {
        Validate.notNull(c);

        // try and make sure each dummy content object has a title
        if (c.getTitle() == null) {
            c.setTitle(Paths.get(c.getCanonicalSourceFile()).getFileName().toString());
        }

        if (!indexProblemCache.containsKey(c)) {
            indexProblemCache.put(c, new ArrayList<String>());
        }

        log.debug(message);
        indexProblemCache.get(c).add(message);//.replace("_", "\\_"));
    }

    /**
     * Helper function to build up a set of used tags for each version.
     *
     * @param tags
     *            - set of tags to register.
     */
    private synchronized void registerTags(final Set<String> tags, Set<String> tagsList) {

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
     * @param q
     *            - numeric question from which to extract units.
     */
    private synchronized void registerUnits(final IsaacNumericQuestion q, Map<String, String> allUnits, Map<String, String> publishedUnits) {

        HashMap<String, String> newUnits = Maps.newHashMap();

        for (Choice c : q.getChoices()) {
            if (c instanceof Quantity) {
                Quantity quantity = (Quantity) c;

                if (quantity.getUnits() != null && !quantity.getUnits().isEmpty()) {
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
        if (q.getPublished()) {
            publishedUnits.putAll(newUnits);
        }
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
                                                      final Set<String> tagsList,
                                                      final Map<String, String> allUnits,
                                                      final Map<String, String> publishedUnits,
                                                      final Map<Content, List<String>> indexProblemCache) {
        if (es.hasIndex(sha, CONTENT_INDEX_TYPE.CONTENT.toString())) {
            log.info("Deleting existing indexes for version " + sha);
            for (CONTENT_INDEX_TYPE contentIndexType : CONTENT_INDEX_TYPE.values()) {
                es.expungeIndexFromSearchCache(sha, contentIndexType.toString());
            }
        }

        log.info("Building search indexes for: " + sha);

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
                this.registerContentProblem(content, "Search Index Error: " + content.getId()
                        + content.getCanonicalSourceFile() + " Exception: " + e.toString(), indexProblemCache);
            }
        }


        try {
            es.indexObject(sha, CONTENT_INDEX_TYPE.METADATA.toString(),
                    objectMapper.writeValueAsString(ImmutableMap.of("version", sha, "created", new Date().toString())), "general");
            es.indexObject(sha, CONTENT_INDEX_TYPE.METADATA.toString(),
                    objectMapper.writeValueAsString(ImmutableMap.of("tags", tagsList)), "tags");

            // TODO: Should probably bulk index these
            for (String k : allUnits.keySet()) {
                es.indexObject(sha, CONTENT_INDEX_TYPE.UNIT.toString(),
                        objectMapper.writeValueAsString(ImmutableMap.of("cleanKey", k, "unit", allUnits.get(k))));
            }
            for (String k : publishedUnits.keySet()) {
                es.indexObject(sha, CONTENT_INDEX_TYPE.PUBLISHED_UNIT.toString(),
                        objectMapper.writeValueAsString(ImmutableMap.of("cleanKey", k, "unit", publishedUnits.get(k))));
            }

            for (Content c: indexProblemCache.keySet()) {
                es.indexObject(sha, CONTENT_INDEX_TYPE.CONTENT_ERROR.toString(),
                        objectMapper.writeValueAsString(ImmutableMap.of(
                                "canonicalSourceFile", c.getCanonicalSourceFile(),
                                "id", c.getId() == null ? "" : c.getId(),
                                "title", c.getTitle() == null ? "" : c.getTitle(),
                                // "tags", c.getTags(), // TODO: Add tags
                                "published", c.getPublished() == null ? "" : c.getPublished(),
                                "errors", indexProblemCache.get(c).toArray())));
            }
        } catch (JsonProcessingException e) {
            log.error("Unable to serialise sha, tags, units or content errors.");
        } catch (SegueSearchException e) {
            log.error("Unable to index sha, tags, units or content errors.");
        }


        try {
            es.bulkIndex(sha, CONTENT_INDEX_TYPE.CONTENT.toString(), contentToIndex);
            log.info("Search index request sent for: " + sha);
        } catch (SegueSearchException e) {
            log.error("Error whilst trying to perform bulk index operation.", e);
        } catch (ActionRequestValidationException e) {
            log.error("Error validating content during index",e);
        }
    }


    /**
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

        Set<Content> allObjectsSeen = new HashSet<>();
        Set<String> expectedIds = new HashSet<>();
        Map<String, Content> contentById = new HashMap<>();
        Map<String, Set<Content>> incomingReferences = new HashMap<>(); // my id -> set of who references me

        // Build up a set of all content (and content fragments for validation)
        for (Content c : gitCache.values()) {
            allObjectsSeen.addAll(this.flattenContentObjects(c));
        }

        // Start looking for issues in the flattened content data
        for (Content c : allObjectsSeen) {
            // add the id to the list of defined ids
            if (c.getId() != null) {
                contentById.put(c.getId(), c);
            }

            // add the ids to the list of expected ids
            if (c.getRelatedContent() != null) {
                expectedIds.addAll(c.getRelatedContent());
                // record which content object was referencing which ID
                for (String id : c.getRelatedContent()) {
                    if (!incomingReferences.containsKey(id)) {
                        incomingReferences.put(id, new HashSet<>());
                    }
                    incomingReferences.get(id).add(c);
                }
            }

            // ensure content does not have children and a value
            if (c.getValue() != null && !c.getChildren().isEmpty()) {
                String id = c.getId();
                String firstLine = "Content";
                if (id != null) {
                    firstLine += ": " + id;
                }

                this.registerContentProblem(c, firstLine + " in " + c.getCanonicalSourceFile()
                        + " found with both children and a value. "
                        + "Content objects are only allowed to have one or the other.", indexProblemCache);

                log.error("Invalid content item detected: The object with ID (" + id
                        + ") has both children and a value.");
            }

            // content type specific checks
            if (c instanceof Media) {
                Media f = (Media) c;

                if (f.getSrc() != null && !f.getSrc().startsWith("http")) {
                    ByteArrayOutputStream fileData = null;
                    try {
                        // This will return null if the file is not found:
                        fileData = database.getFileByCommitSHA(sha, f.getSrc());
                    } catch (IOException | UnsupportedOperationException e) {
                        // Leave fileData = null;
                    }
                    if (null == fileData) {
                        this.registerContentProblem(c, "Unable to find Image: " + f.getSrc()
                                + " in Git. Could the reference be incorrect? SourceFile is " + c.getCanonicalSourceFile(), indexProblemCache);
                    } else if (fileData.size() > MEDIA_FILE_SIZE_LIMIT) {
                        int sizeInKiloBytes = fileData.size() / 1024;
                        this.registerContentProblem(c, String.format("Image (%s) is %s kB and exceeds file size warning limit!",
                                f.getSrc(), sizeInKiloBytes), indexProblemCache);
                    }
                }

                // check that there is some alt text.
                if (f.getAltText() == null || f.getAltText().isEmpty()) {
                    if (!(f instanceof Video)) {
                        // Videos probably don't need alt text unless there is a good reason.
                        this.registerContentProblem(c, "No altText attribute set for media element: " + f.getSrc()
                                + " in Git source file " + c.getCanonicalSourceFile(), indexProblemCache);
                    }
                }
            }
            if (c instanceof Question && c.getId() == null) {
                this.registerContentProblem(c, "Question: " + c.getTitle() + " in " + c.getCanonicalSourceFile()
                        + " found without a unqiue id. " + "This question cannot be logged correctly.", indexProblemCache);
            }
            // TODO: remove reference to isaac specific types from here.
            if (c instanceof ChoiceQuestion
                    && !(c.getType().equals("isaacQuestion"))) {
                ChoiceQuestion question = (ChoiceQuestion) c;

                if (question.getChoices() == null || question.getChoices().isEmpty()) {
                    this.registerContentProblem(question,
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
                        this.registerContentProblem(question,
                                "Question: " + question.getId() + " found without a correct answer. "
                                        + "This question will always be automatically marked as incorrect", indexProblemCache);
                    }
                }
            }

            if (c instanceof EmailTemplate) {
                EmailTemplate e = (EmailTemplate) c;
                if (e.getPlainTextContent() == null) {
                    this.registerContentProblem(c,
                            "Email template should always have plain text content field", indexProblemCache);
                }
            }

            if (c instanceof IsaacEventPage) {
                IsaacEventPage e = (IsaacEventPage) c;
                if (e.getEndDate() != null && e.getEndDate().before(e.getDate())) {
                    this.registerContentProblem(c, "Event has end date before start date", indexProblemCache);
                }
            }

            // TODO: the following things are all highly Isaac specific. I guess they should be elsewhere . . .
            // Find quantities with values that cannot be parsed as numbers.
            if (c instanceof IsaacNumericQuestion) {
                IsaacNumericQuestion q = (IsaacNumericQuestion) c;
                for (Choice choice : q.getChoices()) {
                    if (choice instanceof Quantity) {
                        Quantity quantity = (Quantity) choice;

                        // Check valid number:
                        try {
                            //noinspection ResultOfMethodCallIgnored
                            Double.parseDouble(quantity.getValue());
                        } catch (NumberFormatException e) {
                            this.registerContentProblem(c,
                                    "Numeric Question: " + q.getId() + " has Quantity (" + quantity.getValue()
                                            + ")  with value that cannot be interpreted as a number. "
                                            + "Users will never be able to match this answer.", indexProblemCache);
                        }

                        if (!q.getRequireUnits() && (null != quantity.getUnits() && !quantity.getUnits().isEmpty())) {
                            this.registerContentProblem(c, "Numeric Question: " + q.getId() +
                                    " has a Quantity with units but does not require units!", indexProblemCache);
                        }


                    } else {
                        this.registerContentProblem(c, "Numeric Question: " + q.getId() + " has non-Quantity Choice ("
                                + choice.getValue() + "). It must be deleted and a new Quantity Choice created.", indexProblemCache);
                    }
                }
                if (q.getRequireUnits() && (null != q.getDisplayUnit() && !q.getDisplayUnit().isEmpty())) {
                    this.registerContentProblem(c, "Numeric Question: " + q.getId() + " has a displayUnit set but also requiresUnits!"
                                    + " Units will be ignored for this question!", indexProblemCache);
                }

            }

            // Find Symbolic Questions with broken properties. Need to exclude Chemistry questions!
            if (c instanceof IsaacSymbolicQuestion) {
                if (c.getClass().equals(IsaacSymbolicQuestion.class)) {
                    IsaacSymbolicQuestion q = (IsaacSymbolicQuestion) c;
                    for (String sym : q.getAvailableSymbols()) {
                        if (sym.contains("\\")) {
                            this.registerContentProblem(c, "Symbolic Question: " + q.getId() + " has availableSymbol ("
                                    + sym + ") which contains a '\\' character.", indexProblemCache);
                        }
                    }
                    for (Choice choice : q.getChoices()) {
                        if (choice instanceof Formula) {
                            Formula f = (Formula) choice;
                            if (f.getPythonExpression().contains("\\")) {
                                this.registerContentProblem(c, "Symbolic Question: " + q.getId() + " has Formula ("
                                        + choice.getValue() + ") with pythonExpression which contains a '\\' character.", indexProblemCache);
                            } else if (f.getPythonExpression() == null || f.getPythonExpression().isEmpty()) {
                                this.registerContentProblem( c, "Symbolic Question: " + q.getId() + " has Formula ("
                                        + choice.getValue() + ") with empty pythonExpression!", indexProblemCache);
                            }
                        } else {
                            this.registerContentProblem(c, "Symbolic Question: " + q.getId() + " has non-Formula Choice ("
                                    + choice.getValue() + "). It must be deleted and a new Formula Choice created.", indexProblemCache);
                        }
                    }
                } else if (c.getClass().equals(IsaacSymbolicChemistryQuestion.class)) {
                    IsaacSymbolicChemistryQuestion q = (IsaacSymbolicChemistryQuestion) c;
                    for (Choice choice : q.getChoices()) {
                        if (choice instanceof ChemicalFormula) {
                            ChemicalFormula f = (ChemicalFormula) choice;
                            if (f.getMhchemExpression() == null || f.getMhchemExpression().isEmpty()) {
                                this.registerContentProblem(c, "Chemistry Question: " + q.getId() + " has ChemicalFormula"
                                        + " with empty mhchemExpression!", indexProblemCache);
                            }
                        } else {
                            this.registerContentProblem(c, "Chemistry Question: " + q.getId() + " has non-ChemicalFormula Choice ("
                                    + choice.getValue() + "). It must be deleted and a new ChemicalFormula Choice created.", indexProblemCache);
                        }
                    }
                }
            }
        }

        // Find all references to missing content.
        Set<String> missingContent = new HashSet<>(expectedIds);
        missingContent.removeAll(contentById.keySet());

        for (String id : missingContent) {
            for (Content src : incomingReferences.get(id)) {
                this.registerContentProblem(src, "The id '" + id + "' was referenced by "
                        + src.getCanonicalSourceFile() + " but the content with that "
                        + "ID cannot be found.", indexProblemCache);
            }
        }
        if (missingContent.size() > 0) {
            log.debug("Referential integrity broken for (" + missingContent.size() + ") related Content items. "
                    + "The following ids are referenced but do not exist: " + expectedIds.toString());
        }

        // Find all references from published content to unpublished content.
        for (String refTargetId : incomingReferences.keySet()) {
            Content refTarget = contentById.get(refTargetId);
            if (refTarget != null) {
                for (Content refSrc : incomingReferences.get(refTargetId)) {
                    if (refSrc.getPublished() && !refTarget.getPublished()) {
                        this.registerContentProblem(refSrc, "Content is published, "
                                + "but references unpublished content '" + refTargetId + "'.", indexProblemCache);
                    }
                }
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
