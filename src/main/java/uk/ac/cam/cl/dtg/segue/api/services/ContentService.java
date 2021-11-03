/**
 * Copyright 2021 Raspberry Pi Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.cam.cl.dtg.segue.api.services;

import com.google.api.client.util.Lists;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dao.content.IContentManager;
import uk.ac.cam.cl.dtg.segue.dto.ResultsWrapper;
import uk.ac.cam.cl.dtg.segue.dto.content.ContentDTO;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

import static uk.ac.cam.cl.dtg.segue.api.Constants.CONTENT_INDEX;
import static uk.ac.cam.cl.dtg.segue.api.Constants.ID_FIELDNAME;
import static uk.ac.cam.cl.dtg.segue.api.Constants.NESTED_FIELDS;
import static uk.ac.cam.cl.dtg.segue.api.Constants.TYPE_FIELDNAME;

public class ContentService {
    private final IContentManager contentManager;
    private final String contentIndex;

    @Inject
    public ContentService(final IContentManager contentManager, @Named(CONTENT_INDEX) final String contentIndex) {
        this.contentManager = contentManager;
        this.contentIndex = contentIndex;
    }

    /**
     * This method will return a ResultsWrapper<ContentDTO> based on the parameters supplied.
     *
     * @param version       - the version of the content to search. If null it will default to the current live version.
     * @param fieldsToMatch - List of Boolean search clauses that must be true for the returned content.
     * @param startIndex    - the start index for the search results.
     * @param limit         - the max number of results to return.
     * @return Response containing a ResultsWrapper<ContentDTO> or a Response containing null if none found.
     */
    public final ResultsWrapper<ContentDTO> findMatchingContent(
            final String version, final List<IContentManager.BooleanSearchClause> fieldsToMatch,
            @Nullable final Integer startIndex, @Nullable final Integer limit
    ) throws ContentManagerException {

        String newVersion = this.contentIndex;
        Integer newLimit = Constants.DEFAULT_RESULTS_LIMIT;
        Integer newStartIndex = 0;

        if (version != null) {
            newVersion = version;
        }
        if (limit != null) {
            newLimit = limit;
        }
        if (startIndex != null) {
            newStartIndex = startIndex;
        }

        return this.contentManager.findByFieldNames(newVersion, fieldsToMatch, newStartIndex, newLimit);
    }

    /**
     * Library method that searches the content manager for some search string and provides map of fields that must
     * match.
     *
     * @param searchString        - to pass to the search engine.
     * @param version             - of the content to search.
     * @param fieldsThatMustMatch - a map of fieldName to list of possible matches.
     * @param startIndex          - the start index for the search results.
     * @param limit               - the max number of results to return.
     * @return a response containing the search results (results wrapper) or an empty list.
     * @throws ContentManagerException - an exception when the content is not found
     */
    public final ResultsWrapper<ContentDTO> segueSearch(final String searchString, @Nullable final String version,
                                                        @Nullable final Map<String, List<String>> fieldsThatMustMatch,
                                                        @Nullable final Integer startIndex, @Nullable final Integer limit)
        throws ContentManagerException {
        String newVersion = this.contentIndex;
        int newLimit = Constants.DEFAULT_RESULTS_LIMIT;
        int newStartIndex = 0;

        if (version != null) {
            newVersion = version;
        }

        if (limit != null) {
            newLimit = limit;
        }

        if (startIndex != null) {
            newStartIndex = startIndex;
        }

        return this.contentManager.searchForContent(newVersion, searchString, fieldsThatMustMatch, newStartIndex, newLimit);
    }

    /**
     * Helper method to generate field to match requirements for search queries.
     *
     * Assumes whether to filter by 'any' or 'all' on a field by field basis, with the default being 'all'.
     * You can pass an optional map specifying particular kinds of matching for specific fields
     *
     * @param fieldsToMatch
     *            - expects a map of the form fieldname -> list of queries to match
     * @param booleanOperatorOverrideMap
     *            - an optional map of the form fieldname -> one of 'AND', 'OR' or 'NOT', to specify the
     *              type of matching needed for that field. Overrides any other default matching behaviour
     *              for the given fields
     * @return A list of boolean search clauses to be passed to a content provider
     */
    public static List<IContentManager.BooleanSearchClause> generateDefaultFieldToMatch(final Map<String, List<String>> fieldsToMatch,
                @Nullable final Map<String, Constants.BooleanOperator> booleanOperatorOverrideMap) {
        List<IContentManager.BooleanSearchClause> fieldsToMatchOutput = Lists.newArrayList();

        for (Map.Entry<String, List<String>> pair : fieldsToMatch.entrySet()) {
            // First check if the field needs to be forced to a particular kind of matching
            if (null != booleanOperatorOverrideMap && booleanOperatorOverrideMap.containsKey(pair.getKey())) {
                fieldsToMatchOutput.add(new IContentManager.BooleanSearchClause(
                        pair.getKey(), booleanOperatorOverrideMap.get(pair.getKey()), pair.getValue()));
            } else if (pair.getKey().equals(ID_FIELDNAME)) {
                fieldsToMatchOutput.add(new IContentManager.BooleanSearchClause(
                        pair.getKey(), Constants.BooleanOperator.OR, pair.getValue()));

            } else if (pair.getKey().equals(TYPE_FIELDNAME) && pair.getValue().size() > 1) {
                // special case of when you want to allow more than one
                fieldsToMatchOutput.add(new IContentManager.BooleanSearchClause(
                        pair.getKey(), Constants.BooleanOperator.OR, pair.getValue()));
            } else if (NESTED_FIELDS.contains(pair.getKey())) { // these should match if either are true
                fieldsToMatchOutput.add(new IContentManager.BooleanSearchClause(
                        pair.getKey(), Constants.BooleanOperator.OR, pair.getValue()));
            } else {
                fieldsToMatchOutput.add(new IContentManager.BooleanSearchClause(
                        pair.getKey(), Constants.BooleanOperator.AND, pair.getValue()));
            }
        }

        return fieldsToMatchOutput;
    }
}
