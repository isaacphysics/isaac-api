package uk.ac.cam.cl.dtg.segue.etl;

/**
 * Created by Ian on 17/10/2016.
 */
public class Indexer {

    // ELASTICSEARCHPROVIDER

//
//    @Override
//    public void indexObject(final String index, final String indexType, final String content)
//            throws SegueSearchOperationException {
//        indexObject(index, indexType, content, null);
//    }
//
//    @Override
//    public void bulkIndex(final String index, final String indexType, final List<Map.Entry<String, String>> dataToIndex)
//            throws SegueSearchOperationException {
//        // check index already exists if not execute any initialisation steps.
//        if (!this.hasIndex(index)) {
//            this.sendMappingCorrections(index, indexType);
//        }
//
//        // build bulk request
//        BulkRequestBuilder bulkRequest = client.prepareBulk();
//        for (Map.Entry<String, String> itemToIndex : dataToIndex) {
//            bulkRequest.add(client.prepareIndex(index, indexType, itemToIndex.getKey()).setSource(
//                    itemToIndex.getValue()));
//        }
//
//        try {
//            // execute bulk request
//            BulkResponse bulkResponse = bulkRequest.setRefresh(true).execute().actionGet();
//            if (bulkResponse.hasFailures()) {
//                // process failures by iterating through each bulk response item
//                for (BulkItemResponse itemResponse : bulkResponse.getItems()) {
//                    log.error("Unable to index the following item: " + itemResponse.getFailureMessage());
//                }
//            }
//        } catch (ElasticsearchException e) {
//            throw new SegueSearchOperationException("Error during bulk index operation.", e);
//        }
//    }
//
//    @Override
//    public void indexObject(final String index, final String indexType, final String content, final String uniqueId)
//            throws SegueSearchOperationException {
//        // check index already exists if not execute any initialisation steps.
//        if (!this.hasIndex(index)) {
//            this.sendMappingCorrections(index, indexType);
//        }
//
//        try {
//            IndexResponse indexResponse = client.prepareIndex(index, indexType, uniqueId).setSource(content).execute()
//                    .actionGet();
//            log.debug("Document: " + indexResponse.getId() + " indexed.");
//
//        } catch (ElasticsearchException e) {
//            throw new SegueSearchOperationException("Error during index operation.", e);
//        }
//    }
//    @Override
//    public boolean expungeEntireSearchCache() {
//        return this.expungeIndexFromSearchCache("_all");
//    }
//
//    @Override
//    public boolean expungeIndexFromSearchCache(final String index) {
//        Validate.notBlank(index);
//
//        try {
//            log.info("Sending delete request to ElasticSearch for search index: " + index);
//            client.admin().indices().delete(new DeleteIndexRequest(index)).actionGet();
//        } catch (ElasticsearchException e) {
//            log.error("ElasticSearch exception while trying to delete index " + index, e);
//            return false;
//        }
//
//        return true;
//    }

//
//    @Override
//    public boolean expungeIndexTypeFromSearchCache(final String index, final String indexType) {
//        try {
//            DeleteMappingRequest deleteMapping = new DeleteMappingRequest(index).types(indexType);
//            client.admin().indices().deleteMapping(deleteMapping).actionGet();
//        } catch (ElasticsearchException e) {
//            log.error("ElasticSearch exception while trying to delete index " + index + " type " + indexType);
//            return false;
//        }
//        return true;
//    }
//
//  @Override
//  public void registerRawStringFields(final List<String> fieldNames) {
//      this.rawFieldsList.addAll(fieldNames);
//  }
//




    // GitContentManager
    //searchProvider.registerRawStringFields(Lists.newArrayList(Constants.ID_FIELDNAME, Constants.TITLE_FIELDNAME));

//
//    @Override
//    public final void clearCache() {
//        log.info("Clearing all content caches.");
//        searchProvider.expungeEntireSearchCache();
//        indexProblemCache.clear();
//        tagsList.clear();
//        allUnits.clear();
//        cache.invalidateAll();
//    }
//
//    @Override
//    public final void clearCache(final String version) {
//        Validate.notBlank(version);
//
//        if (this.searchProvider.hasIndex(version)) {
//            indexProblemCache.remove(version);
//            searchProvider.expungeIndexFromSearchCache(version);
//            tagsList.remove(version);
//            allUnits.remove(version);
//            cache.invalidateAll();
//        }
//    }
//
//    /**
//     * This method will send off the information in the git cache to the search provider for indexing.
//     *
//     * @param sha
//     *            - the version in the git cache to send to the search provider.
//     * @param gitCache
//     *            a map that represents indexed content for a given sha.
//     */
//    private synchronized void buildSearchIndexFromLocalGitIndex(final String sha, final Map<String, Content> gitCache) {
//        if (this.searchProvider.hasIndex(sha)) {
//            log.info("Search index has already been updated by" + " another thread. No need to reindex. Aborting...");
//            return;
//        }
//
//        log.info("Building search index for: " + sha);
//
//        // setup object mapper to use pre-configured deserializer module.
//        // Required to deal with type polymorphism
//        List<Map.Entry<String, String>> thingsToIndex = Lists.newArrayList();
//        ObjectMapper objectMapper = mapper.generateNewPreconfiguredContentMapper();
//        for (Content content : gitCache.values()) {
//            try {
//                thingsToIndex.add(immutableEntry(content.getId(), objectMapper.writeValueAsString(content)));
//            } catch (JsonProcessingException e) {
//                log.error("Unable to serialize content object: " + content.getId()
//                        + " for indexing with the search provider.", e);
//                this.registerContentProblem(sha, content, "Search Index Error: " + content.getId()
//                        + content.getCanonicalSourceFile() + " Exception: " + e.toString());
//            }
//        }
//
//        try {
//            this.searchProvider.bulkIndex(sha, CONTENT_TYPE, thingsToIndex);
//            log.info("Search index request sent for: " + sha);
//        } catch (SegueSearchOperationException e) {
//            log.error("Error whilst trying to perform bulk index operation.", e);
//        }
//    }
//
//    /**
//     * This method will populate the internal gitCache based on the content object files found for a given SHA.
//     *
//     * Currently it only looks for json files in the repository.
//     *
//     * @param sha
//     *            - the version to index.
//     * @return the map representing all indexed content.
//     * @throws ContentManagerException
//     */
//    private synchronized Map<String, Content> buildGitContentIndex(final String sha) throws ContentManagerException {
//        // This set of code only needs to happen if we have to read from git
//        // again.
//        if (null == sha) {
//            throw new ContentManagerException(String.format("SHA: %s is null. Cannot index.", sha));
//        }
//
//        if (this.indexProblemCache.containsKey(sha)) {
//            throw new ContentManagerException(String.format("SHA: %s has already been indexed. Failing... ", sha));
//        }
//
//        // iterate through them to create content objects
//        Repository repository = database.getGitRepository();
//
//        try {
//            ObjectId commitId = repository.resolve(sha);
//
//            if (null == commitId) {
//                throw new ContentManagerException("Failed to buildGitIndex - Unable to locate resource with SHA: "
//                        + sha);
//            }
//
//            Map<String, Content> shaCache = new HashMap<String, Content>();
//
//            TreeWalk treeWalk = database.getTreeWalk(sha, ".json");
//            log.info("Populating git content cache based on sha " + sha + " ...");
//
//            // Traverse the git repository looking for the .json files
//            while (treeWalk.next()) {
//                ByteArrayOutputStream out = new ByteArrayOutputStream();
//                ObjectLoader loader = repository.open(treeWalk.getObjectId(0));
//                loader.copyTo(out);
//
//                // setup object mapper to use preconfigured deserializer
//                // module. Required to deal with type polymorphism
//                ObjectMapper objectMapper = mapper.getSharedContentObjectMapper();
//
//                Content content = null;
//                try {
//                    content = (Content) objectMapper.readValue(out.toString(), ContentBase.class);
//
//                    // check if we only want to index published content
//                    if (indexOnlyPublishedParentContent && !content.getPublished()) {
//                        log.debug("Skipping unpublished content: " + content.getId());
//                        continue;
//                    }
//
//                    content = this.augmentChildContent(content, treeWalk.getPathString(), null);
//
//                    if (null != content) {
//                        // add children (and parent) from flattened Set to
//                        // cache if they have ids
//                        for (Content flattenedContent : this.flattenContentObjects(content)) {
//                            if (flattenedContent.getId() == null) {
//                                continue;
//                            }
//
//                            if (flattenedContent.getId().contains(".")) {
//                                // Otherwise, duplicate IDs with different content,
//                                // therefore log an error
//                                log.warn("Resource with invalid ID (" + content.getId()
//                                        + ") detected in cache. Skipping " + treeWalk.getPathString());
//
//                                this.registerContentProblem(sha, flattenedContent, "Index failure - Invalid ID "
//                                        + flattenedContent.getId() + " found in file " + treeWalk.getPathString()
//                                        + ". Must not contain restricted characters.");
//                                continue;
//                            }
//
//                            // check if we have seen this key before if
//                            // we have then we don't want to add it
//                            // again
//                            if (!shaCache.containsKey(flattenedContent.getId())) {
//                                // It must be new so we can add it
//                                log.debug("Loading into cache: " + flattenedContent.getId() + "("
//                                        + flattenedContent.getType() + ")" + " from " + treeWalk.getPathString());
//                                shaCache.put(flattenedContent.getId(), flattenedContent);
//                                registerTagsWithVersion(sha, flattenedContent.getTags());
//
//                                // If this is a numeric question, extract any
//                                // units from its answers.
//
//                                if (flattenedContent instanceof IsaacNumericQuestion) {
//                                    registerUnitsWithVersion(sha, (IsaacNumericQuestion) flattenedContent);
//                                }
//
//                                continue; // our work here is done
//                            }
//
//                            // shaCache contains key already, compare the
//                            // content
//                            if (shaCache.get(flattenedContent.getId()).equals(flattenedContent)) {
//                                // content is the same therefore it is just
//                                // reuse of a content object so that is
//                                // fine.
//                                log.debug("Resource (" + content.getId() + ") already seen in cache. Skipping "
//                                        + treeWalk.getPathString());
//                                continue;
//                            }
//
//                            // Otherwise, duplicate IDs with different content,
//                            // therefore log an error
//                            log.warn("Resource with duplicate ID (" + content.getId()
//                                    + ") detected in cache. Skipping " + treeWalk.getPathString());
//                            this.registerContentProblem(sha, flattenedContent,
//                                    "Index failure - Duplicate ID found in file " + treeWalk.getPathString() + " and "
//                                            + shaCache.get(flattenedContent.getId()).getCanonicalSourceFile()
//                                            + " only one will be available");
//                        }
//                    }
//                } catch (JsonMappingException e) {
//                    log.warn(String.format("Unable to parse the json file found %s as a content object. "
//                            + "Skipping file due to error: \n %s", treeWalk.getPathString(), e.getMessage()));
//                    Content dummyContent = new Content();
//                    dummyContent.setCanonicalSourceFile(treeWalk.getPathString());
//                    this.registerContentProblem(sha, dummyContent, "Index failure - Unable to parse json file found - "
//                            + treeWalk.getPathString() + ". The following error occurred: " + e.getMessage());
//                } catch (IOException e) {
//                    log.error("IOException while trying to parse " + treeWalk.getPathString(), e);
//                    Content dummyContent = new Content();
//                    dummyContent.setCanonicalSourceFile(treeWalk.getPathString());
//                    this.registerContentProblem(sha, dummyContent,
//                            "Index failure - Unable to read the json file found - " + treeWalk.getPathString()
//                                    + ". The following error occurred: " + e.getMessage());
//                }
//            }
//
//            repository.close();
//            log.debug("Tags available " + tagsList);
//            log.debug("All units: " + allUnits);
//            log.info("Git content cache population for " + sha + " completed!");
//
//            return shaCache;
//        } catch (IOException e) {
//            log.error("IOException while trying to access git repository. ", e);
//            throw new ContentManagerException("Unable to index content, due to an IOException.");
//        }
//    }
//
//    /**
//     * Augments all child objects recursively to include additional information.
//     *
//     * This should be done before saving to the local gitCache in memory storage.
//     *
//     * This method will also attempt to reconstruct object id's of nested content such that they are unique to the page
//     * by default.
//     *
//     * @param content
//     *            - content to augment
//     * @param canonicalSourceFile
//     *            - source file to add to child content
//     * @param parentId
//     *            - used to construct nested ids for child elements.
//     * @return Content object with new reference
//     */
//    private Content augmentChildContent(final Content content, final String canonicalSourceFile,
//            @Nullable final String parentId) {
//        if (null == content) {
//            return null;
//        }
//
//        // If this object is of type question then we need to give it a random
//        // id if it doesn't have one.
//        if (content instanceof Question && content.getId() == null) {
//            log.warn("Found question without id " + content.getTitle() + " " + canonicalSourceFile);
//        }
//
//        // Try to figure out the parent ids.
//        String newParentId = null;
//        if (null == parentId && content.getId() != null) {
//            newParentId = content.getId();
//        } else {
//            if (content.getId() != null) {
//                newParentId = parentId + Constants.ID_SEPARATOR + content.getId();
//            } else {
//                newParentId = parentId;
//            }
//        }
//
//        content.setCanonicalSourceFile(canonicalSourceFile);
//
//        if (!content.getChildren().isEmpty()) {
//            for (ContentBase cb : content.getChildren()) {
//                if (cb instanceof Content) {
//                    Content c = (Content) cb;
//
//                    this.augmentChildContent(c, canonicalSourceFile, newParentId);
//                }
//            }
//        }
//
//        if (content instanceof Choice) {
//            Choice choice = (Choice) content;
//            this.augmentChildContent((Content) choice.getExplanation(), canonicalSourceFile,
//                    newParentId);
//        }
//
//        // TODO: hack to get hints to apply as children
//        if (content instanceof Question) {
//            Question question = (Question) content;
//            if (question.getHints() != null) {
//                for (ContentBase cb : question.getHints()) {
//                    Content c = (Content) cb;
//                    this.augmentChildContent(c, canonicalSourceFile, newParentId);
//                }
//            }
//
//            // Augment question answers
//            if (question.getAnswer() != null) {
//                Content answer = (Content) question.getAnswer();
//                if (answer.getChildren() != null) {
//                    for (ContentBase cb : answer.getChildren()) {
//                        Content c = (Content) cb;
//                        this.augmentChildContent(c, canonicalSourceFile, newParentId);
//                    }
//                }
//            }
//
//            if (content instanceof ChoiceQuestion) {
//                ChoiceQuestion choiceQuestion = (ChoiceQuestion) content;
//                if (choiceQuestion.getChoices() != null) {
//                    for (ContentBase cb : choiceQuestion.getChoices()) {
//                        Content c = (Content) cb;
//                        this.augmentChildContent(c, canonicalSourceFile, newParentId);
//                    }
//                }
//            }
//        }
//
//        // try to determine if we have media as fields to deal with in this class
//        Method[] methods = content.getClass().getDeclaredMethods();
//        for (int i = 0; i < methods.length; i++) {
//            if (Media.class.isAssignableFrom(methods[i].getReturnType())) {
//                try {
//                    Media media = (Media) methods[i].invoke(content);
//                    if (media != null) {
//                        media.setSrc(fixMediaSrc(canonicalSourceFile, media.getSrc()));
//                    }
//                } catch (SecurityException | IllegalAccessException | IllegalArgumentException
//                        | InvocationTargetException e) {
//                    log.error("Unable to access method using reflection: attempting to fix Media Src", e);
//                }
//            }
//        }
//
//        if (content instanceof Media) {
//            Media media = (Media) content;
//            media.setSrc(fixMediaSrc(canonicalSourceFile, media.getSrc()));
//
//            // for tracking purposes we want to generate an id for all image content objects.
//            if (media.getId() == null && media.getSrc() != null) {
//                media.setId(new String(parentId + Constants.ID_SEPARATOR
//                        + Base64.encodeBase64(media.getSrc().getBytes())));
//            }
//        }
//
//        // Concatenate the parentId with our id to get a fully qualified
//        // identifier.
//        if (content.getId() != null && parentId != null) {
//            content.setId(parentId + Constants.ID_SEPARATOR + content.getId());
//        }
//
//        return content;
//    }
//
//    /**
//     * @param canonicalSourceFile
//     *            - the canonical path to use for concat operations.
//     * @param originalSrc
//     *            - to modify
//     * @return src with relative paths fixed.
//     */
//    private String fixMediaSrc(final String canonicalSourceFile, final String originalSrc) {
//        if (originalSrc != null && !(originalSrc.startsWith("http://") || originalSrc.startsWith("https://"))) {
//            String newPath = FilenameUtils.normalize(FilenameUtils.getPath(canonicalSourceFile) + originalSrc, true);
//            return newPath;
//        }
//        return originalSrc;
//    }
//    /*
//     * This method will attempt to traverse the cache to ensure that all content references are valid.
//     *
//     * @param sha
//     *            version to validate integrity of.
//     * @param gitCache
//     *            Data structure containing all content for a given sha.
//     * @return True if we are happy with the integrity of the git repository, False if there is something wrong.
//     *//*
//    private boolean checkForContentErrors(final String sha, final Map<String, Content> gitCache) {
//        log.info(String.format("Starting content Validation (%s).", sha));
//        Set<Content> allObjectsSeen = new HashSet<Content>();
//        Set<String> expectedIds = new HashSet<String>();
//        Set<String> definedIds = new HashSet<String>();
//        Set<String> missingContent = new HashSet<String>();
//        Map<String, Content> whoAmI = new HashMap<String, Content>();
//
//        // Build up a set of all content (and content fragments for validation)
//        for (Content c : gitCache.values()) {
//// TODO work out why this was here and why removing it didn't seem to do anything!
////            if (c instanceof IsaacSymbolicQuestion) {
////                // do not validate these questions for now.
////                continue;
////            }
//            allObjectsSeen.addAll(this.flattenContentObjects(c));
//        }
//
//        // Start looking for issues in the flattened content data
//        for (Content c : allObjectsSeen) {
//            // add the id to the list of defined ids
//            if (c.getId() != null) {
//                definedIds.add(c.getId());
//            }
//
//            // add the ids to the list of expected ids
//            if (c.getRelatedContent() != null) {
//                expectedIds.addAll(c.getRelatedContent());
//                // record which content object was referencing which ID
//                for (String id : c.getRelatedContent()) {
//                    whoAmI.put(id, c);
//                }
//            }
//
//            // ensure content does not have children and a value
//            if (c.getValue() != null && !c.getChildren().isEmpty()) {
//                String id = c.getId();
//                String firstLine = "Content";
//                if (id != null) {
//                    firstLine += ": " + id;
//                }
//
//                this.registerContentProblem(sha, c, firstLine + " in " + c.getCanonicalSourceFile()
//                        + " found with both children and a value. "
//                        + "Content objects are only allowed to have one or the other.");
//
//                log.error("Invalid content item detected: The object with ID (" + id
//                        + ") has both children and a value.");
//            }
//
//            // content type specific checks
//            if (c instanceof Media) {
//                Media f = (Media) c;
//
//                if (f.getSrc() != null
//                        && !f.getSrc().startsWith("http") && !database.verifyGitObject(sha, f.getSrc())) {
//                    this.registerContentProblem(sha, c, "Unable to find Image: " + f.getSrc()
//                            + " in Git. Could the reference be incorrect? SourceFile is " + c.getCanonicalSourceFile());
//                }
//
//                // check that there is some alt text.
//                if (f.getAltText() == null || f.getAltText().isEmpty()) {
//                    this.registerContentProblem(sha, c, "No altText attribute set for media element: " + f.getSrc()
//                            + " in Git source file " + c.getCanonicalSourceFile());
//                }
//            }
//            if (c instanceof Question && c.getId() == null) {
//                this.registerContentProblem(sha, c, "Question: " + c.getTitle() + " in " + c.getCanonicalSourceFile()
//                        + " found without a unqiue id. " + "This question cannot be logged correctly.");
//            }
//            // TODO: remove reference to isaac specific types from here.
//            if (c instanceof ChoiceQuestion
//                    && !(c.getType().equals("isaacQuestion"))) {
//                ChoiceQuestion question = (ChoiceQuestion) c;
//
//                if (question.getChoices() == null || question.getChoices().isEmpty()) {
//                    this.registerContentProblem(sha, question,
//                            "Question: " + question.getId() + " found without any choice metadata. "
//                                    + "This question will always be automatically " + "marked as incorrect");
//                } else {
//                    boolean correctOptionFound = false;
//                    for (Choice choice : question.getChoices()) {
//                        if (choice.isCorrect()) {
//                            correctOptionFound = true;
//                        }
//                    }
//
//                    if (!correctOptionFound) {
//                        this.registerContentProblem(sha, question,
//                                "Question: " + question.getId() + " found without a correct answer. "
//                                        + "This question will always be automatically marked as incorrect");
//                    }
//                }
//            }
//
//            if (c instanceof EmailTemplate) {
//                EmailTemplate e = (EmailTemplate) c;
//                if (e.getPlainTextContent() == null) {
//                    this.registerContentProblem(sha, c,
//                            "Email template should always have plain text content field");
//                }
//
//                if (e.getReplyToEmailAddress() != null && null == e.getReplyToName()) {
//                    this.registerContentProblem(sha, c,
//                            "Email template contains replyToEmailAddress but not replyToName");
//                }
//            }
//
//            if (c instanceof IsaacEventPage) {
//                IsaacEventPage e = (IsaacEventPage) c;
//                if (e.getEndDate() != null && e.getEndDate().before(e.getDate())) {
//                    this.registerContentProblem(sha, c, "Event has end date before start date");
//                }
//            }
//
//            // TODO: the following things are all highly Isaac specific. I guess they should be elsewhere . . .
//            // Find quantities with values that cannot be parsed as numbers.
//            if (c instanceof IsaacNumericQuestion) {
//                IsaacNumericQuestion q = (IsaacNumericQuestion) c;
//                for (Choice choice : q.getChoices()) {
//                    if (choice instanceof Quantity) {
//                        Quantity quantity = (Quantity) choice;
//
//                        try {
//                            Double.parseDouble(quantity.getValue());
//                        } catch (NumberFormatException e) {
//                            this.registerContentProblem(sha, c,
//                                    "Numeric Question: " + q.getId() + " has Quantity (" + quantity.getValue()
//                                            + ")  with value that cannot be interpreted as a number. "
//                                            + "Users will never be able to match this answer.");
//                        }
//                    } else if (q.getRequireUnits()) {
//                        this.registerContentProblem(sha, c, "Numeric Question: " + q.getId() + " has non-Quantity Choice ("
//                                + choice.getValue() + "). It must be deleted and a new Quantity Choice created.");
//                    }
//                }
//
//            }
//
//            // Find Symbolic Questions with broken properties. Need to exclude Chemistry questions!
//            if (c instanceof IsaacSymbolicQuestion) {
//                if (c.getClass().equals(IsaacSymbolicQuestion.class)) {
//                    IsaacSymbolicQuestion q = (IsaacSymbolicQuestion) c;
//                    for (String sym : q.getAvailableSymbols()) {
//                        if (sym.contains("\\")) {
//                            this.registerContentProblem(sha, c, "Symbolic Question: " + q.getId() + " has availableSymbol ("
//                                    + sym + ") which contains a '\\' character.");
//                        }
//                    }
//                    for (Choice choice : q.getChoices()) {
//                        if (choice instanceof Formula) {
//                            Formula f = (Formula) choice;
//                            if (f.getPythonExpression().contains("\\")) {
//                                this.registerContentProblem(sha, c, "Symbolic Question: " + q.getId() + " has Formula ("
//                                        + choice.getValue() + ") with pythonExpression which contains a '\\' character.");
//                            } else if (f.getPythonExpression() == null || f.getPythonExpression().isEmpty()) {
//                                this.registerContentProblem(sha, c, "Symbolic Question: " + q.getId() + " has Formula ("
//                                        + choice.getValue() + ") with empty pythonExpression!");
//                            }
//                        } else {
//                            this.registerContentProblem(sha, c, "Symbolic Question: " + q.getId() + " has non-Formula Choice ("
//                                    + choice.getValue() + "). It must be deleted and a new Formula Choice created.");
//                        }
//                    }
//                } else if (c.getClass().equals(IsaacSymbolicChemistryQuestion.class)) {
//                    IsaacSymbolicChemistryQuestion q = (IsaacSymbolicChemistryQuestion) c;
//                    for (Choice choice : q.getChoices()) {
//                        if (choice instanceof ChemicalFormula) {
//                            ChemicalFormula f = (ChemicalFormula) choice;
//                            if (f.getMhchemExpression() == null || f.getMhchemExpression().isEmpty()) {
//                                this.registerContentProblem(sha, c, "Chemistry Question: " + q.getId() + " has ChemicalFormula"
//                                        + " with empty mhchemExpression!");
//                            }
//                        } else {
//                            this.registerContentProblem(sha, c, "Chemistry Question: " + q.getId() + " has non-ChemicalFormula Choice ("
//                                    + choice.getValue() + "). It must be deleted and a new ChemicalFormula Choice created.");
//                        }
//                    }
//                }
//            }
//        }
//
//        if (expectedIds.equals(definedIds) && missingContent.isEmpty()) {
//            return true;
//        } else {
//            expectedIds.removeAll(definedIds);
//            missingContent.addAll(expectedIds);
//
//            for (String id : missingContent) {
//                this.registerContentProblem(sha, whoAmI.get(id), "This id (" + id + ") was referenced by "
//                        + whoAmI.get(id).getCanonicalSourceFile() + " but the content with that "
//                        + "ID cannot be found.");
//            }
//            if (missingContent.size() > 0) {
//                log.warn("Referential integrity broken for (" + missingContent.size() + ") related Content items. "
//                        + "The following ids are referenced but do not exist: " + expectedIds.toString());
//            }
//        }
//        log.info(String.format("Validation processing (%s) complete. There are %s files with content problems", sha,
//                this.indexProblemCache.get(sha).size()));
//
//        return false;
//    }*/
//
//    /**
//     * Unpack the content objects into one big set. Useful for validation but could produce a very large set
//     *
//     * @param content
//     *            content object to flatten
//     * @return Set of content objects comprised of all children and the parent.
//     *//*
//    private Set<Content> flattenContentObjects(final Content content) {
//        Set<Content> setOfContentObjects = new HashSet<Content>();
//        if (!content.getChildren().isEmpty()) {
//
//            List<ContentBase> children = content.getChildren();
//
//            for (ContentBase child : children) {
//                setOfContentObjects.add((Content) child);
//                setOfContentObjects.addAll(flattenContentObjects((Content) child));
//            }
//        }
//
//        setOfContentObjects.add(content);
//
//        return setOfContentObjects;
//    }*/
//
//    /**
//     * Helper function to build up a set of used tags for each version.
//     *
//     * @param version
//     *            - version to register the tag for.
//     * @param tags
//     *            - set of tags to register.
//     *//*
//    private synchronized void registerTagsWithVersion(final String version, final Set<String> tags) {
//        Validate.notBlank(version);
//
//        if (null == tags || tags.isEmpty()) {
//            // don't do anything.
//            return;
//        }
//
//        if (!tagsList.containsKey(version)) {
//            tagsList.put(version, new HashSet<String>());
//        }
//        Set<String> newTagSet = Sets.newHashSet();
//
//        // sanity check that tags are trimmed.
//        for (String tag : tags) {
//            newTagSet.add(tag.trim());
//        }
//
//        tagsList.get(version).addAll(newTagSet);
//    }*/
//
//    /**
//     * Helper function to accumulate the set of all units used in numeric question answers.
//     *
//     * @param version
//     *            - version to register the units for.
//     * @param q
//     *            - numeric question from which to extract units.
//     *//*
//    private synchronized void registerUnitsWithVersion(final String version, final IsaacNumericQuestion q) {
//
//        HashMap<String, String> newUnits = Maps.newHashMap();
//
//        for (Choice c : q.getChoices()) {
//            if (c instanceof Quantity) {
//                Quantity quantity = (Quantity) c;
//
//                if (!quantity.getUnits().isEmpty()) {
//                    String units = quantity.getUnits();
//                    String cleanKey = units.replace("\t", "").replace("\n", "").replace(" ", "");
//
//                    // May overwrite previous entry, doesn't matter as there is
//                    // no mechanism by which to choose a winner
//                    newUnits.put(cleanKey, units);
//                }
//            }
//        }
//
//        if (newUnits.isEmpty()) {
//            // This question contained no units.
//            return;
//        }
//
//        if (!allUnits.containsKey(version)) {
//            allUnits.put(version, new HashMap<String, String>());
//        }
//
//        allUnits.get(version).putAll(newUnits);
//    }*/
//
//    /**
//     * Helper method to register problems with content objects.
//     *
//     * @param version
//     *            - to which the problem relates
//     * @param c
//     *            - Partial content object to represent the object that has problems.
//     * @param message
//     *            - Error message to associate with the problem file / content.
//     *//*
//    private synchronized void registerContentProblem(final String version, final Content c, final String message) {
//        Validate.notNull(c);
//
//        // try and make sure each dummy content object has a title
//        if (c.getTitle() == null) {
//            c.setTitle(Paths.get(c.getCanonicalSourceFile()).getFileName().toString());
//        }
//
//        if (!indexProblemCache.containsKey(version)) {
//            indexProblemCache.put(version, new HashMap<Content, List<String>>());
//        }
//
//        if (!indexProblemCache.get(version).containsKey(c)) {
//            indexProblemCache.get(version).put(c, new ArrayList<String>());
//        }
//
//        indexProblemCache.get(version).get(c).add(message);//.replace("_", "\\_"));
//    }*/
///*
//    @Override
//    public void setIndexRestriction(final boolean loadOnlyPublishedContent) {
//        this.indexOnlyPublishedParentContent = loadOnlyPublishedContent;
//    }*/

    // GitContentManager ensureCache


//    synchronized (this) {
//        final Map<String, Content> gitCache;
//
//        // now we have acquired the lock check if someone else has indexed this.
//        searchIndexed = searchProvider.hasIndex(version);
//        if (searchIndexed && this.indexProblemCache.containsKey(version)) {
//            return;
//        }
//
//        log.info(String.format(
//                "Rebuilding content index as sha (%s) does not exist in search provider.",
//                version));
//
//        // anytime we build the git index we have to empty the problem cache
//        this.indexProblemCache.remove(version);
//        gitCache = buildGitContentIndex(version);
//
//        // may as well spawn a new thread to do the validation
//        // work now.
//        Thread validationJob = new Thread() {
//            @Override
//            public void run() {
//                checkForContentErrors(version, gitCache);
//            }
//        };
//
//        validationJob.setDaemon(true);
//        validationJob.start();
//
//        if (!searchIndexed) {
//            buildSearchIndexFromLocalGitIndex(version, gitCache);
//        } else {
//            log.info(String.format("Search index for %s is already available. Not reindexing...", version));
//        }
//    }
//}
//
//    // verification step. Make sure that this segue instance is happy it can access the content requested.
//    // if not then throw an exception.
//    StringBuilder errorMessageStringBuilder = new StringBuilder();
//        searchIndexed = searchProvider.hasIndex(version);
//
//                if (!searchIndexed) {
//                errorMessageStringBuilder.append();
//                }
//
//                String message = errorMessageStringBuilder.toString();
//                if (!message.isEmpty()) {
//                }










    // ContentVersionController

    //    /**
//     * Trigger a sync job that will request a sync and subsequent index of the latest version of the content available
//     *
//     * This method will cause a new job to be added to the indexer queue.
//     *
//     * This method is asynchronous and can be made to block by invoking the get method on the object returned.
//     *
//     * @return a future containing a string which is the version id.
//     */
//    public Future<String> triggerSyncJob() {
//        return this.triggerSyncJob(null);
//    }
//
//    /**
//     * Trigger a sync job that will request a sync and subsequent index of a specific version of the content.
//     *
//     * This method will cause a new job to be added to the indexer queue.
//     *
//     * This method is asynchronous and can be made to block by invoking the get method on the object returned.
//     *
//     * @param version
//     *            to sync
//     * @return a future containing a string which is the version id.
//     */
//    public Future<String> triggerSyncJob(final String version) {
//        ContentSynchronisationWorker worker = new ContentSynchronisationWorker(this, version);
//
//        log.info("Adding sync job for version " + version + " to the queue (" + this.indexQueue.size() + ")");
//        // add the job to the indexers internal queue
//        Future<String> future = indexer.submit(worker);
//
//        // add it to our queue so that we have the option of cancelling it if necessary
//        this.indexQueue.add(future);
//
//        return future;
//    }
//
//    /**
//     * This method is intended to be used by Synchronisation jobs to inform the controller that they have completed
//     * their work.
//     *
//     * @param version
//     *            the version that has just been indexed.
//     * @param success
//     *            - whether or not the job completed successfully.
//     */
//    public synchronized void syncJobCompleteCallback(final String version, final boolean success) {
//        // this job is about to complete so remove it from the queue.
//        this.indexQueue.remove();
//
//        // for use by ContentSynchronisationWorkers to alert the controller that
//        // they have finished
//        if (!success) {
//            log.error(String.format("ContentSynchronisationWorker reported a failure to synchronise %s. Giving up...",
//                    version));
//            return;
//        }
//
//        // verify that the version is indeed cached
//        if (!contentManager.getCachedVersionList().contains(version)) {
//            // if not just return without doing anything.
//            log.error("Sync job informed version controller "
//                    + "that a version was ready and it lied. The version is no longer cached. "
//                    + "Terminating sync job.");
//            return;
//        }
//
//        // Decide if we have to update the live version or not.
//        if (Boolean.parseBoolean(properties.getProperty(Constants.FOLLOW_GIT_VERSION))) {
//
//            // if we are in FOLLOW_GIT_VERSION mode then we do have to try to update.
//            // acquire the lock for an atomic update
//            synchronized (liveVersion) {
//                // set it to the live version only if it is newer than the
//                // current live version OR if the current live version no-longer
//                // exists (a rebase might have happened, for example).
//
//                boolean newer;
//                try {
//                    newer = contentManager.compareTo(version, this.getLiveVersion()) > 0;
//                } catch (NotFoundException e) {
//                    // The current live version was not found. A rebase probably happened underneath us.
//                    log.info("Failed to find current live version, someone probably rebased and force-pushed. Tut tut.");
//                    newer = true;
//                }
//
//                if (newer) {
//                    this.setLiveVersion(version);
//                } else {
//                    log.info("Not changing live version as part of sync job as the " + "version (" + version
//                            + ") just indexed is older than (or the same as) the current one (" + this.getLiveVersion()
//                            + ").");
//                }
//            }
//
//            cleanUpTheIndexQueue();
//
//        } else {
//            // we don't want to change the latest version until told to do so.
//            log.info("New content version " + version + " indexed and available. Not changing liveVersion of the "
//                    + "site until told to do so.");
//        }
//
//        if (success) {
//            this.cleanupCache(version);
//        }
//
//        log.debug("Sync job completed - callback received and finished.");
//    }
//
//    /**
//     * Change the version that the controller considers to be the live version.
//     *
//     * This method is threadsafe.
//     *
//     * @param newLiveVersion
//     *            - the version to make live.
//     */
//    public void setLiveVersion(final String newLiveVersion) {
//        if (!contentManager.getCachedVersionList().contains(newLiveVersion)) {
//            log.warn("New version hasn't been synced yet. Requesting sync job.");
//
//            // trigger sync job
//            try {
//                this.triggerSyncJob(newLiveVersion).get(); // we want this to block.
//            } catch (InterruptedException | ExecutionException e) {
//                log.error("Unable to complete sync job");
//            }
//        }
//
//        synchronized (liveVersion) {
//            log.info("Changing live version from " + this.getLiveVersion() + " to " + newLiveVersion);
//
//            // assume we always want to modify the initial version too.
//            try {
//                this.versionPropertiesManager.saveProperty(Constants.INITIAL_LIVE_VERSION, newLiveVersion);
//            } catch (IOException e) {
//                log.error("Unable to save new version to properties file.", e);
//            }
//
//            liveVersion = newLiveVersion;
//        }
//    }
//    /**
//     * This method should use the configuration settings to maintain the cache of the content manager object.
//     *
//     * @param versionJustIndexed
//     *            - the version we just indexed.
//     */
//    public synchronized void cleanupCache(final String versionJustIndexed) {
//        int maxCacheSize = Integer.parseInt(properties.getProperty(Constants.MAX_VERSIONS_TO_CACHE));
//
//        // clean up task queue
//        for (Future<?> future : this.indexQueue) {
//            if (future.isDone() || future.isCancelled()) {
//                this.indexQueue.remove(future);
//            }
//        }
//        log.info("Index job queue currently of size (" + this.indexQueue.size() + ")");
//
//        // first check if our cache is bigger than we want it to be
//        if (contentManager.getCachedVersionList().size() > maxCacheSize) {
//            log.info("Cache is too full (" + contentManager.getCachedVersionList().size()
//                    + ") finding and deleting old versions");
//
//
//            // Now we want to decide which versions we can safely get rid of.
//            List<String> allCachedVersions = Lists.newArrayList(contentManager.getCachedVersionList());
//            // sort them so they are in ascending order with the oldest version first.
//            Collections.sort(allCachedVersions, new Comparator<String>() {
//                @Override
//                public int compare(final String arg0, final String arg1) {
//                    return contentManager.compareTo(arg0, arg1);
//                }
//            });
//
//            for (String version : allCachedVersions) {
//                // we want to stop when we have deleted enough.
//                if (contentManager.getCachedVersionList().size() <= maxCacheSize) {
//                    log.info("Cache clear complete");
//                    break;
//                }
//
//                // check we are not deleting the version that is currently
//                // in use before we delete it.
//                if (!isVersionInUse(version) && !versionJustIndexed.equals(version)) {
//                    log.info("Requesting to delete the content at version " + version
//                            + " from the cache.");
//                    contentManager.clearCache(version);
//                }
//            }
//
//            // we couldn't free up enough space
//            if (contentManager.getCachedVersionList().size() > maxCacheSize) {
//                log.warn("Warning unable to reduce cache to target size: current cache size is "
//                        + contentManager.getCachedVersionList().size());
//            }
//        } else {
//            log.info("Not evicting cache as we have enough space: current cache size is "
//                    + contentManager.getCachedVersionList().size() + ".");
//        }
//    }
//
//    /**
//     * Instructs all content Managers to dump all cache information and any associated search indices.
//     */
//    public synchronized void deleteAllCacheData() {
//        log.info("Clearing all caches and search indices.");
//        contentManager.clearCache();
//    }
//
//    /**
//     * get a string representation of what is in the to IndexQueue.
//     *
//     * @return a list of tasks in the index queue.
//     */
//    public Collection<String> getToIndexQueue() {
//        ArrayList<String> newArrayList = Lists.newArrayList();
//        for (Future<String> f : this.indexQueue) {
//            newArrayList.add(f.toString());
//        }
//
//        return newArrayList;
//    }
//
//    /**
//     * Utility method that will empty the to index queue of any unstarted jobs.
//     */
//    public void cleanUpTheIndexQueue() {
//        // remove all but the latest one as the chances are the others are old requests
//        while (this.indexQueue.size() > 1) {
//            Future<String> f = this.indexQueue.remove();
//            f.cancel(false);
//            log.info("Cancelling pending (old) index operations as we are in follow git mode. Queue is currently: ("
//                    + this.indexQueue.size() + ")");
//        }
//    }

    // //SchoolListReader init
    //searchProvider.registerRawStringFields(Arrays.asList(SCHOOL_URN_FIELDNAME.toLowerCase()));


//    /**
//     * Trigger a thread to index the schools list. If needed.
//     */
//    public synchronized void prepareSchoolList() {
//
//        // We mustn't throw any exceptions here, as this is called from the constructor of SchoolLookupServiceFacade,
//        // called by Guice. And if anything dies while Guice is working, we never recover.
//
//        Thread thread = new Thread() {
//            public void run() {
//                log.info("Starting a new thread to index schools list.");
//                try {
//                    indexSchoolsWithSearchProvider();
//                } catch (UnableToIndexSchoolsException e) {
//                    log.error("Unable to index the schools list.");
//                }
//            }
//        };
//        thread.setDaemon(true);
//        thread.start();
//    }

//    /**
//     * Build the index for the search schools provider.
//     *
//     * @throws UnableToIndexSchoolsException
//     *             - when there is a problem building the index of schools.
//     */
//    private synchronized void indexSchoolsWithSearchProvider() throws UnableToIndexSchoolsException {
//        if (!searchProvider.hasIndex(SCHOOLS_SEARCH_INDEX)) {
//            log.info("Creating schools index with search provider.");
//            List<School> schoolList = this.loadAndBuildSchoolList();
//            List<Map.Entry<String, String>> indexList = Lists.newArrayList();
//
//            for (School school : schoolList) {
//                try {
//                    indexList.add(immutableEntry(school.getUrn().toString(), mapper.writeValueAsString(school)));
//                } catch (JsonProcessingException e) {
//                    log.error("Unable to serialize the school object into json.", e);
//                }
//            }
//
//            try {
//                searchProvider.bulkIndex(SCHOOLS_SEARCH_INDEX, SCHOOLS_SEARCH_TYPE, indexList);
//                log.info("School list index request complete.");
//            } catch (SegueSearchOperationException e) {
//                log.error("Unable to complete bulk index operation for schools list.", e);
//            }
//        } else {
//            log.info("Cancelling school search index operation as another thread has already done it.");
//        }
//    }


    //***************************
    // ADMIN FACADE
    //***************************

//    /**
//     * This method will allow the live version served by the site to be changed.
//     *
//     * @param request
//     *            - to help determine access rights.
//     * @param version
//     *            - version to use as updated version of content store.
//     * @return Success shown by returning the new liveSHA or failed message "Invalid version selected".
//     */
//    @POST
//    @Path("/live_version/{version}")
//    @Produces(MediaType.APPLICATION_JSON)
//    public synchronized Response changeLiveVersion(@Context final HttpServletRequest request,
//            @PathParam("version") final String version) {
//
//        try {
//            if (isUserAnAdmin(request)) {
//                IContentManager contentPersistenceManager = contentVersionController.getContentManager();
//                String newVersion;
//                if (!contentPersistenceManager.isValidVersion(version)) {
//                    SegueErrorResponse error = new SegueErrorResponse(Status.BAD_REQUEST, "Invalid version selected: "
//                            + version);
//                    log.warn(error.getErrorMessage());
//                    return error.toResponse();
//                }
//
//                if (!contentPersistenceManager.getCachedVersionList().contains(version)) {
//                    newVersion = contentVersionController.triggerSyncJob(version).get();
//                } else {
//                    newVersion = version;
//                }
//
//                Collection<String> availableVersions = contentPersistenceManager.getCachedVersionList();
//
//                if (!availableVersions.contains(version)) {
//                    SegueErrorResponse error = new SegueErrorResponse(Status.BAD_REQUEST, "Invalid version selected: "
//                            + version);
//                    log.warn(error.getErrorMessage());
//                    return error.toResponse();
//                }
//
//                contentVersionController.setLiveVersion(newVersion);
//                log.info("Live version of the site changed to: " + newVersion + " by user: "
//                        + this.userManager.getCurrentRegisteredUser(request).getEmail());
//
//                return Response.ok().build();
//            } else {
//                return new SegueErrorResponse(Status.FORBIDDEN,
//                        "You must be logged in as an admin to access this function.").toResponse();
//            }
//        } catch (NoUserLoggedInException e) {
//            return SegueErrorResponse.getNotLoggedInResponse();
//        } catch (InterruptedException e) {
//            log.error("ExecutorException during version change.", e);
//            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Error while trying to terminate a process.", e)
//                    .toResponse();
//        } catch (ExecutionException e) {
//            log.error("ExecutorException during version change.", e);
//            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Error during verison change.", e).toResponse();
//        }
//    }
//
//    /**
//     * This method will try to bring the live version that Segue is using to host content up-to-date with the latest in
//     * the database.
//     *
//     * @param request
//     *            - to enable security checking.
//     * @return a response to indicate the synchronise job has triggered.
//     */
//    @POST
//    @Path("/synchronise_datastores")
//    public synchronized Response synchroniseDataStores(@Context final HttpServletRequest request) {
//        try {
//            // check if we are authorized to do this operation.
//            // no authorisation required in DEV mode, but in PROD we need to be
//            // an admin.
//            if (!this.getProperties().getProperty(Constants.SEGUE_APP_ENVIRONMENT)
//                    .equals(Constants.EnvironmentType.PROD.name())
//                    || isUserAnAdmin(request)) {
//                log.info("Informed of content change; " + "so triggering new synchronisation job.");
//                contentVersionController.triggerSyncJob().get();
//                return Response.ok("success - job started").build();
//            } else {
//                log.warn("Unable to trigger synch job as not an admin or this server is set to the PROD environment.");
//                return new SegueErrorResponse(Status.FORBIDDEN, "You must be an administrator to use this function.")
//                        .toResponse();
//            }
//        } catch (NoUserLoggedInException e) {
//            log.warn("Unable to trigger synch job as not logged in.");
//            return SegueErrorResponse.getNotLoggedInResponse();
//        } catch (InterruptedException e) {
//            log.error("ExecutorException during synchronise datastores operation.", e);
//            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Error while trying to terminate a process.", e)
//                    .toResponse();
//        } catch (ExecutionException e) {
//            log.error("ExecutorException during synchronise datastores operation.", e);
//            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Error during verison change.", e).toResponse();
//        }
//    }
//
//    /**
//     * This method is only intended to be used on development / staging servers.
//     *
//     * It will try to bring the live version that Segue is using to host content up-to-date with the latest in the
//     * database.
//     *
//     * @param request
//     *            - to enable security checking.
//     * @return a response to indicate the synchronise job has triggered.
//     */
//    @POST
//    @Path("/new_version_alert")
//    @Produces(MediaType.APPLICATION_JSON)
//    public Response versionChangeNotification(@Context final HttpServletRequest request) {
//        // check if we are authorized to do this operation.
//        // no authorisation required in DEV mode, but in PROD we need to be
//        // an admin.
//        try {
//            if (!this.getProperties().getProperty(Constants.SEGUE_APP_ENVIRONMENT)
//                    .equals(Constants.EnvironmentType.PROD.name())
//                    || this.isUserAnAdmin(request)) {
//                log.info("Informed of content change; so triggering new async synchronisation job.");
//                // on this occasion we don't want to wait for a response.
//                contentVersionController.triggerSyncJob();
//                return Response.ok().build();
//            } else {
//                log.warn("Unable to trigger synch job as this segue environment is "
//                        + "configured in PROD mode unless you are an ADMIN.");
//                return new SegueErrorResponse(Status.FORBIDDEN,
//                        "You must be an administrator to use this function on the PROD environment.").toResponse();
//            }
//        } catch (NoUserLoggedInException e) {
//            return new SegueErrorResponse(Status.UNAUTHORIZED,
//                    "You must be logged in to use this function on a PROD environment.").toResponse();
//        }
//    }
//
//    /**
//     * This method will delete all cached data from the CMS and any search indices.
//     *
//     * @param request
//     *            - containing user session information.
//     *
//     * @return the latest version id that will be cached if content is requested.
//     */
//    @POST
//    @Produces(MediaType.APPLICATION_JSON)
//    @Path("/clear_caches")
//    public synchronized Response clearCaches(@Context final HttpServletRequest request) {
//        try {
//            if (isUserAnAdmin(request)) {
//                IContentManager contentPersistenceManager = contentVersionController.getContentManager();
//                RegisteredUserDTO currentRegisteredUser = userManager.getCurrentRegisteredUser(request);
//
//                log.info(String.format("Admin user: (%s) triggered cache clears...", currentRegisteredUser.getEmail()));
//                contentPersistenceManager.clearCache();
//
//                ImmutableMap<String, String> response = new ImmutableMap.Builder<String, String>().put("result",
//                        "success").build();
//
//                return Response.ok(response).build();
//            } else {
//                return new SegueErrorResponse(Status.FORBIDDEN, "You must be an administrator to use this function.")
//                        .toResponse();
//            }
//
//        } catch (NoUserLoggedInException e) {
//            return SegueErrorResponse.getNotLoggedInResponse();
//        }
//    }
//
//    /**
//     * This method will show a string representation of all jobs in the to index queue.
//     *
//     * @param request
//     *            - containing user session information.
//     *
//     * @return the latest queue information
//     */
//    @GET
//    @Produces(MediaType.APPLICATION_JSON)
//    @Path("/content_index_queue")
//    public synchronized Response getCurrentIndexQueue(@Context final HttpServletRequest request) {
//        try {
//            if (isUserStaff(request)) {
//                ImmutableMap<String, Object> response = new ImmutableMap.Builder<String, Object>().put("queue",
//                        contentVersionController.getToIndexQueue()).build();
//
//                return Response.ok(response).build();
//            } else {
//                return new SegueErrorResponse(Status.FORBIDDEN, "You must be an administrator to use this function.")
//                        .toResponse();
//            }
//
//        } catch (NoUserLoggedInException e) {
//            return SegueErrorResponse.getNotLoggedInResponse();
//        }
//    }
//
//    /**
//     * This method will delete all jobs not yet started in the indexer queue.
//     *
//     * @param request
//     *            - containing user session information.
//     *
//     * @return the new queue.
//     */
//    @DELETE
//    @Produces(MediaType.APPLICATION_JSON)
//    @Path("/content_index_queue")
//    public synchronized Response deleteAllInCurrentIndexQueue(@Context final HttpServletRequest request) {
//        try {
//            if (isUserAnAdmin(request)) {
//                RegisteredUserDTO u = userManager.getCurrentRegisteredUser(request);
//                log.info(String.format("Admin user (%s) requested to empty indexer queue.", u.getEmail()));
//
//                contentVersionController.cleanUpTheIndexQueue();
//
//                ImmutableMap<String, Object> response = new ImmutableMap.Builder<String, Object>().put("queue",
//                        contentVersionController.getToIndexQueue()).build();
//
//                return Response.ok(response).build();
//            } else {
//                return new SegueErrorResponse(Status.FORBIDDEN, "You must be an administrator to use this function.")
//                        .toResponse();
//            }
//        } catch (NoUserLoggedInException e) {
//            return SegueErrorResponse.getNotLoggedInResponse();
//        }
//    }
}
