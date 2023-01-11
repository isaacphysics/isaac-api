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

import com.google.api.client.util.Sets;
import com.google.common.collect.Lists;
import uk.ac.cam.cl.dtg.segue.api.Constants;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static uk.ac.cam.cl.dtg.isaac.api.Constants.*;
import static uk.ac.cam.cl.dtg.isaac.api.Constants.EVENT_TYPE;

public class IsaacSearchInstructionBuilder {

    private final ISearchProvider searchProvider;

    private final boolean includeOnlyPublishedContent;
    private final boolean excludeRegressionTestContent;
    private final boolean excludeNofilterContent;
    private boolean includePastEvents;

    private final Set<String> includedContentTypes;
    private final Set<String> priorityIncludedContentTypes;
    private static final float PRIORITY_CONTENT_BOOST = 5L;

    private final List<SearchInField> searchesInFields;
    private static final Long FIELD_BOOST = 5L;
    private static final Long FIELD_BOOST_FUZZY = 1L;
    private static final Long WILDCARD_FIELD_BOOST = 1L;

    private static final Long HIGH_PRIORITY_FIELD_BOOST = 10L;
    private static final Long HIGH_PRIORITY_FIELD_BOOST_FUZZY = 3L;
    private static final Long HIGH_PRIORITY_WILDCARD_FIELD_BOOST = 2L;

    private static final Long EVENT_ADDRESS_FIELD_BOOST = 3L;
    private static final Long EVENT_ADDRESS_FIELD_BOOST_FUZZY = 1L;

    public enum Priority {
        NORMAL,
        HIGH
    }

    public enum Strategy {
        DEFAULT,
        FUZZY
    }

    private final BooleanInstruction masterInstruction;

    public IsaacSearchInstructionBuilder(final ISearchProvider searchProvider, final boolean includeOnlyPublishedContent,
                                         final boolean excludeRegressionTestContent, final boolean excludeNofilterContent) {
        this.searchProvider = searchProvider;

        this.searchesInFields = new ArrayList<>();

        includedContentTypes = Sets.newHashSet();
        priorityIncludedContentTypes = Sets.newHashSet();

        // We may or may not filter some things out of all queries, depending on config options
        this.includeOnlyPublishedContent = includeOnlyPublishedContent;
        this.excludeRegressionTestContent = excludeRegressionTestContent;

        this.excludeNofilterContent = excludeNofilterContent;
        this.includePastEvents = false;

        this.masterInstruction = this.buildBaseSearchQuery(new BooleanInstruction());
    }

    /**
     * Builds the base search instructions. The base instructions exclude content marked deprecated, no-filter content
     * (depending on role), unpublished content (depending on config properties) and content tagged with
     * "regression-test" (depending on config properties).
     *
     * @param instruction The existing instruction to augment with these base instructions.
     * @return The augmented instruction.
     */
    public BooleanInstruction buildBaseSearchQuery(final BooleanInstruction instruction) {
        // Exclude unpublished content (based on config)
        if (this.includeOnlyPublishedContent) {
            instruction.mustNot(new MatchInstruction(Constants.PUBLISHED_FIELDNAME, "false"));
        }

        // Exclude regression test content (based on config)
        if (this.excludeRegressionTestContent) {
            instruction.mustNot(new MatchInstruction(Constants.TAGS_FIELDNAME, "false"));
        }

        // Exclude "no-filter" content (based on user role)
        if (this.excludeNofilterContent) {
            instruction.mustNot(new MatchInstruction(Constants.TAGS_FIELDNAME, HIDE_FROM_FILTER_TAG));
        }

        // Exclude deprecated content
        instruction.mustNot(new MatchInstruction(Constants.DEPRECATED_FIELDNAME, "true"));

        return instruction;
    }

    /**
     * Include only content of {@code contentTypes} in results. Other content types will be excluded. Subsequent calls
     * add to the set of included content types.
     *
     * @param contentTypes A list of content types to match.
     * @return This IsaacSearchQueryBuilder, to allow chained operations.
     */
    public IsaacSearchInstructionBuilder includeContentTypes(final Set<String> contentTypes) {
        return this.includeContentTypes(contentTypes, Priority.NORMAL);
    }

    /**
     * Include only content of {@code contentTypes} in results. Other content types will be excluded. Subsequent calls
     * add to the set of included content types.
     *
     * @param contentTypes A list of content types to match.
     * @param priority The priority of matches for this content type.
     * @return This IsaacSearchQueryBuilder, to allow chained operations.
     */
    public IsaacSearchInstructionBuilder includeContentTypes(final Set<String> contentTypes, final Priority priority) {
        if (priority == Priority.HIGH) {
            this.priorityIncludedContentTypes.addAll(contentTypes);
        } else {
            this.includedContentTypes.addAll(contentTypes);
        }
        return this;
    }

    /**
     * Include content where {@code field} is equal to one or more of {@code terms} in the results.
     *
     * @param field The content field to search within
     * @param terms The terms to match within {@code field}
     * @return This IsaacSearchQueryBuilder, to allow chained operations.
     */
    public IsaacSearchInstructionBuilder searchInField(final String field, final Set<String> terms) {
        return this.searchInField(field, terms, Priority.NORMAL, Strategy.DEFAULT);
    }

    /**
     * Include content where {@code field} is equal to one or more of {@code terms} in the results.
     *
     * @param field The content field to search within
     * @param terms The terms to match within {@code field}
     * @param priority The priority of matches for this field and terms.
     * @return This IsaacSearchQueryBuilder, to allow chained operations.
     */
    public IsaacSearchInstructionBuilder searchInField(final String field, final Set<String> terms, final Priority priority) {
        return this.searchInField(field, terms, priority, Strategy.DEFAULT);
    }

    /**
     * Include content where {@code field} is equal to one or more of {@code terms} in the results.
     *
     * @param field The content field to search within
     * @param terms The terms to match within {@code field}
     * @param strategy The strategy to use to match this field and terms.
     * @return This IsaacSearchQueryBuilder, to allow chained operations.
     */
    public IsaacSearchInstructionBuilder searchInField(final String field, final Set<String> terms, final Strategy strategy) {
        return this.searchInField(field, terms, Priority.NORMAL, strategy);
    }

    /**
     * Include content where {@code field} is equal to one or more of {@code terms} in the results.
     *
     * @param field The content field to search within
     * @param terms The terms to match within {@code field}
     * @param priority The priority of matches for this field and terms.
     * @param strategy The strategy to use to match this field and terms.
     * @return This IsaacSearchQueryBuilder, to allow chained operations.
     */
    public IsaacSearchInstructionBuilder searchInField(final String field, final Set<String> terms, final Priority priority,
                                                       final Strategy strategy) {
        if (!terms.isEmpty()) {
            this.searchesInFields.add(new SearchInField(field, terms, strategy, priority));
        }
        return this;
    }

    /**
     * Sets whether to return events where the date field contains a date in the past. Defaults to false, and has no
     * effect if the event content type is excluded.
     *
     * @param includePastEvents - whether to include past events.
     * @return This IsaacSearchQueryBuilder, to allow chained operations.
     */
    public IsaacSearchInstructionBuilder includePastEvents(final boolean includePastEvents) {
        this.includePastEvents = includePastEvents;
        return this;
    }

    /**
     * Builds and returns the final BooleanMatchInstruction reflecting the builder's settings.
     *
     * @return A BooleanMatchInstruction reflecting the builder's settings.
     */
    public BooleanInstruction build() {
        List<String> contentTypes = Optional.ofNullable(this.includedContentTypes)
                .orElse(Collections.emptySet())
                .stream()
                .filter(SEARCHABLE_DOC_TYPES::contains)
                .collect(Collectors.toList());

        if (contentTypes.isEmpty()) {
            contentTypes = Lists.newArrayList(SITE_WIDE_SEARCH_VALID_DOC_TYPES);
        }

        for (String contentType : contentTypes) {
            BooleanInstruction contentInstruction = new BooleanInstruction();

            // Add content type matching instruction
            contentInstruction.must(new MatchInstruction(Constants.TYPE_FIELDNAME, contentType));

            // Add matching instruction for generic pages only if they are specifically tagged
            if (contentType.equals(PAGE_TYPE)) {
                contentInstruction.must(new MatchInstruction(Constants.TAGS_FIELDNAME, SEARCHABLE_TAG));
            }

            // Optionally add instruction to match only events that have not yet taken place
            if (contentType.equals(EVENT_TYPE) && !includePastEvents) {
                LocalDate today = LocalDate.now();
                long now = today.atStartOfDay(ZoneId.systemDefault()).toEpochSecond() * Constants.EVENT_DATE_EPOCH_MULTIPLIER;
                contentInstruction.must(new RangeInstruction<Long>(Constants.DATE_FIELDNAME).greaterThanOrEqual(now));
            }

            // Apply instructions to search for specific terms in specific fields
            this.addSearchesInFieldsToInstruction(contentInstruction, searchesInFields, contentType);

            if (this.priorityIncludedContentTypes.contains(contentType)) {
                contentInstruction.setBoost(PRIORITY_CONTENT_BOOST);
            }

            // If there are no "should"s, having a 'minimum should match' > 0 will give us no results. Otherwise, it is
            // desirable.
            if (!contentInstruction.getShoulds().isEmpty()) {
                contentInstruction.setMinimumShouldMatch(1);
            }

            this.masterInstruction.should(contentInstruction);
        }
        return this.masterInstruction;
    }

    /**
     * Augments {@code instruction} with the field search instructions specified via {@code searchInField()} and friends.
     *
     *
     * @param instruction A BooleanMatchInstruction for a particular content type to augment with the field-search instructions.
     * @param searchesInFields A list of {@code SearchInField}s encapsulating fields and terms to search for, as well as
     *                         the strategy and priority to use for each.
     * @param contentType The content type {@code instruction} relates to, so we can decide how/whether to process certain
     *                    field searches.
     *
     *                    (todo: Consider replacing with content-type-specific implementations of this method, that process
     *                    only fields relevant to the content type. Alternatively, this class could provide
     *                    content-type-specific search builders to clients.)
     */
    private void addSearchesInFieldsToInstruction(final BooleanInstruction instruction, final List<SearchInField> searchesInFields,
                                                  final String contentType) {

        Map<String, NestedInstruction> nestedQueriesByPath = new HashMap<>();

        for (SearchInField searchInField : searchesInFields) {

            // Special fields
            if (Objects.equals(contentType, EVENT_TYPE) &&
                    Arrays.stream(Constants.ADDRESS_PATH_FIELDNAME).collect(Collectors.toList()).contains(searchInField.getField())) {
                // Address fields
                // If we're searching any "address" fields, search all of them
                String nestedFieldConnector = searchProvider.getNestedFieldConnector();
                String addressPath = String.join(nestedFieldConnector, Constants.ADDRESS_PATH_FIELDNAME);

                for (String addressField : Constants.ADDRESS_FIELDNAMES) {
                    for (String term : searchInField.getTerms()) {
                        String field = addressPath + nestedFieldConnector + addressField;
                        instruction.should(new MatchInstruction(field, term, EVENT_ADDRESS_FIELD_BOOST,
                                false));
                        instruction.should(new MatchInstruction(field, term, EVENT_ADDRESS_FIELD_BOOST_FUZZY,
                                true));
                    }
                }
            } else if (Constants.NESTED_QUERY_FIELDS.contains(searchInField.getField())) {
                // Nested fields
                /* (This has a specific meaning in ES -
                see https://www.elastic.co/guide/en/elasticsearch/reference/7.17/query-dsl-nested-query.html) */
                // Ensure nested field queries are grouped together
                for (String term : searchInField.getTerms()) {
                    String nestedPath = searchInField.getField().split("\\.")[0];
                    nestedQueriesByPath.putIfAbsent(nestedPath, new NestedInstruction(nestedPath));
                    nestedQueriesByPath.get(nestedPath).should(new MatchInstruction(searchInField.getField(), term));
                }
            } else {
                // Generic fields
                for (String term : searchInField.getTerms()) {
                    if (searchInField.getStrategy() == Strategy.DEFAULT) {
                        if (searchInField.getPriority() == Priority.HIGH) {
                            instruction.should(new MatchInstruction(searchInField.getField(), term, HIGH_PRIORITY_FIELD_BOOST, false));
                            instruction.should(new MatchInstruction(searchInField.getField(), term, HIGH_PRIORITY_FIELD_BOOST_FUZZY, true));
                        } else if (searchInField.getPriority() == Priority.NORMAL) {
                            instruction.should(new MatchInstruction(searchInField.getField(), term, FIELD_BOOST, false));
                            instruction.should(new MatchInstruction(searchInField.getField(), term, FIELD_BOOST_FUZZY, true));
                        }
                    } else if (searchInField.getStrategy() == Strategy.FUZZY) {
                        if (searchInField.getPriority() == Priority.HIGH) {
                            instruction.should(new MatchInstruction(searchInField.getField(), term, HIGH_PRIORITY_WILDCARD_FIELD_BOOST, true));
                            instruction.should(new WildcardInstruction(searchInField.getField(), "*" + term + "*", HIGH_PRIORITY_WILDCARD_FIELD_BOOST));
                        } else if (searchInField.getPriority() == Priority.NORMAL) {
                            instruction.should(new MatchInstruction(searchInField.getField(), term, WILDCARD_FIELD_BOOST, true));
                            instruction.should(new WildcardInstruction(searchInField.getField(), "*" + term + "*", WILDCARD_FIELD_BOOST));
                        }
                        instruction.should(new MultiMatchInstruction(searchInField.getField(), searchInField.getTerms().toArray(String[]::new), 2L));
                    }
                }
            }
        }

        // Apply grouped nested queries
        for (Map.Entry<String, NestedInstruction> entry : nestedQueriesByPath.entrySet()) {
            instruction.must(entry.getValue());
        }
    }
}
