/*
 * Copyright 2022 Matthew Trew
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

package uk.ac.cam.cl.dtg.segue.search;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import uk.ac.cam.cl.dtg.segue.api.Constants;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static uk.ac.cam.cl.dtg.isaac.api.Constants.*;
import static uk.ac.cam.cl.dtg.isaac.api.Constants.EVENT_TYPE;

public class IsaacSearchQueryBuilder {

    private final ISearchProvider searchProvider;

    private final String searchString;

    private final boolean includeOnlyPublishedContent;
    private final boolean excludeRegressionTestContent;

    private Set<String> matchContentTypes;
    private Set<String> priorityContentTypes;
    private float priorityContentBoost = 5L;

    private Set<String> matchFields;
    private Long genericMatchFieldBoost = 5L;
    private Long genericMatchFieldBoostFuzzy = 1L;

    private Long eventAddressMatchFieldBoost = 3L;
    private Long eventAddressMatchFieldBoostFuzzy = 1L;

    private Set<String> priorityMatchFields;
    private Long priorityMatchFieldBoost = 10L;
    private Long priorityMatchFieldBoostFuzzy = 3L;

    private final BooleanMatchInstruction query;

    public IsaacSearchQueryBuilder(final String searchString, final ISearchProvider searchProvider,
                                   final boolean includeOnlyPublishedContent, final boolean excludeRegressionTestContent) {
        this.searchProvider = searchProvider;

        // We may or may not filter some things out of all queries, depending on config options
        this.includeOnlyPublishedContent = includeOnlyPublishedContent;
        this.excludeRegressionTestContent = excludeRegressionTestContent;

        this.searchString = searchString;
        this.query = buildBaseSearchQuery(new BooleanMatchInstruction());
    }

    /*
    The base query excludes no-filter content, content marked deprecated, unpublished content (depending on
    config properties) and content tagged with "regression-test" (depending on config properties).
     */
    final BooleanMatchInstruction buildBaseSearchQuery(final BooleanMatchInstruction query) {
        // Exclude unpublished content (based on config)
        if (this.includeOnlyPublishedContent) {
            query.mustNot(new MustMatchInstruction(Constants.PUBLISHED_FIELDNAME, "false"));
        }

        // Exclude regression test content (based on config)
        if (this.excludeRegressionTestContent) {
            query.mustNot(new MustMatchInstruction(Constants.TAGS_FIELDNAME, "false"));
        }

        // Exclude "no-filter" content
        query.mustNot(new MustMatchInstruction(Constants.TAGS_FIELDNAME, HIDE_FROM_FILTER_TAG));

        // Exclude deprecated content
        query.mustNot(new MustMatchInstruction(Constants.DEPRECATED_FIELDNAME, "true"));

        return query;
    }

    /**
     * Sets the types of content to match. If left unspecified, the built query will match all content types.
     *
     * @param contentTypes A list of content types to match.
     * @return This IsaacSearchQueryBuilder, to allow chained operations.
     */
    public IsaacSearchQueryBuilder matchContentTypes(final Set<String> contentTypes) {
        this.matchContentTypes = contentTypes;
        return this;
    }

    /**
     * Sets the types of content to prioritise. Expected to be a subset of the matched content types.
     * If left unspecified, the built query will not prioritise any content types.
     *
     * @param contentTypes A list of content types to match.
     * @return This IsaacSearchQueryBuilder, to allow chained operations.
     */
    public IsaacSearchQueryBuilder prioritiseContentTypes(final Set<String> contentTypes) {
        this.priorityContentTypes = contentTypes;
        return this;
    }

    public IsaacSearchQueryBuilder matchFields(final Set<String> fields) {
        this.matchFields = fields;
        return this;
    }

    public IsaacSearchQueryBuilder prioritiseFields(final Set<String> fields) {
        this.priorityMatchFields = fields;
        return this;
    }



    public BooleanMatchInstruction build() {
        String nestedFieldConnector = searchProvider.getNestedFieldConnector();

        int minimumMatchInstructions = 1;

        List<String> contentTypes = Optional.ofNullable(this.matchContentTypes)
                .orElse(Collections.emptySet())
                .stream()
                .filter(SITE_WIDE_SEARCH_VALID_DOC_TYPES::contains)
                .collect(Collectors.toList());

        if (contentTypes.isEmpty()) {
            contentTypes = Lists.newArrayList(SITE_WIDE_SEARCH_VALID_DOC_TYPES);
        }

        // Determine field priorities
        List<String> ordinaryFields = new ArrayList<>(Sets.difference(this.matchFields, this.priorityMatchFields));

        for (String contentType : contentTypes) {
            BooleanMatchInstruction contentQuery = new BooleanMatchInstruction();

            // Add content type matching instruction
            contentQuery.must(new MustMatchInstruction(Constants.TYPE_FIELDNAME, contentType));

            // Add general field matching instructions
            for (String field : this.priorityMatchFields) {
                contentQuery.should(new ShouldMatchInstruction(field, searchString, this.priorityMatchFieldBoost, false));
                contentQuery.should(new ShouldMatchInstruction(field, searchString, this.priorityMatchFieldBoostFuzzy, true));
            }
            for (String field : ordinaryFields) {
                contentQuery.should(new ShouldMatchInstruction(field, searchString, this.genericMatchFieldBoost, false));
                contentQuery.should(new ShouldMatchInstruction(field, searchString, this.genericMatchFieldBoostFuzzy, true));
            }

            // Add matching instruction for generic pages only if they are specifically tagged
            if (contentType.equals(PAGE_TYPE)) {
                contentQuery.must(new MustMatchInstruction(Constants.TAGS_FIELDNAME, SEARCHABLE_TAG));
            }

            // Add instruction to match the address on event pages
            if (contentType.equals(EVENT_TYPE)) {
                String addressPath = String.join(nestedFieldConnector, Constants.ADDRESS_PATH_FIELDNAME);
                for (String addressField : Constants.ADDRESS_FIELDNAMES) {
                    String field = addressPath + nestedFieldConnector + addressField;
                    contentQuery.should(new ShouldMatchInstruction(field, searchString, eventAddressMatchFieldBoost, false));
                    contentQuery.should(new ShouldMatchInstruction(field, searchString, eventAddressMatchFieldBoostFuzzy, true));
                }
            }

            // Add instruction to match only events that have not yet taken place
            if (contentType.equals(EVENT_TYPE)) {
                LocalDate today = LocalDate.now();
                long now = today.atStartOfDay(ZoneId.systemDefault()).toEpochSecond() * Constants.EVENT_DATE_EPOCH_MULTIPLIER;
                contentQuery.must(new RangeMatchInstruction<Long>(Constants.DATE_FIELDNAME).greaterThanOrEqual(now));
            }

            contentQuery.setMinimumShouldMatch(minimumMatchInstructions);

            if (this.priorityContentTypes.contains(contentType)) {
                contentQuery.setBoost(priorityContentBoost);
            }

            this.query.should(contentQuery);
        }
        return this.query;
    }
}
