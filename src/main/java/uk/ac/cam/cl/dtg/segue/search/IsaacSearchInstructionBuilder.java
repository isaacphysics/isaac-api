/*
 * Copyright 2022 Matthew Trew
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.ac.cam.cl.dtg.segue.search;

import com.google.api.client.util.Maps;
import com.google.api.client.util.Sets;
import com.google.common.collect.Lists;
import uk.ac.cam.cl.dtg.segue.api.Constants;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static uk.ac.cam.cl.dtg.isaac.api.Constants.*;

public class IsaacSearchInstructionBuilder {

    private final ISearchProvider searchProvider;

    private final boolean includeOnlyPublishedContent;
    private final boolean excludeRegressionTestContent;
    private final boolean excludeNofilterContent;
    private boolean includePastEvents;

    private Set<String> includedContentTypes;
    private Set<String> priorityIncludedContentTypes;
    private static final float PRIORITY_CONTENT_BOOST = 5L;

    private List<SearchInField> searchesInFields;
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
        SIMPLE,
        DEFAULT,
        FUZZY
    }


    /**
     * Builder for a {@code BooleanInstruction} defining a search through the content. The final instruction is structured
     * like so:
     * > Master instruction
     *     > Base instruction
     *        - Exclude any content with "deprecated" in field "tags"
     *        - Exclude any content with "nofilter" in field "tags"
     *        - etc.
     *     > Content type A instruction
     *        - Search for "hello" in field "title"
     *        - Search for "goodbye" in field "tags", with high priority
     *        - etc...
     *     > Content type B instruction
     *        - Search for "hello" in field "title"
     *        - Search for "goodbye" in field "tags", with high priority
     *        - etc...
     *     > Content type C instruction
     *        - Search for "hello" in field "title"
     *        - Search for "goodbye" in field "tags", with high priority
     *        - etc...
     *
     * @param searchProvider The search provider, so we can look up implementation-specific things. It's probably going
     *                       to be ElasticSearch at this point, let's be honest.
     * @param includeOnlyPublishedContent Exclude unpublished content from the results.
     * @param excludeRegressionTestContent Exclude regression test content from the results.
     * @param excludeNofilterContent Exclude 'nofilter' content from the results.
     */
    public IsaacSearchInstructionBuilder(final ISearchProvider searchProvider, final boolean includeOnlyPublishedContent,
                                         final boolean excludeRegressionTestContent, final boolean excludeNofilterContent) {
        this.searchProvider = searchProvider;

        this.searchesInFields = new ArrayList<>();

        this.includedContentTypes = Sets.newHashSet();
        this.priorityIncludedContentTypes = Sets.newHashSet();

        this.includeOnlyPublishedContent = includeOnlyPublishedContent;
        this.excludeRegressionTestContent = excludeRegressionTestContent;

        this.excludeNofilterContent = excludeNofilterContent;
        this.includePastEvents = false;
    }

    /**
     * Builds the base search instructions. The base instructions exclude content marked deprecated, no-filter content
     * (depending on role), unpublished content (depending on config properties) and content tagged with
     * "regression-test" (depending on config properties).
     *
     * @param instruction The existing master instruction to augment with these base instructions.
     * @return The augmented instruction.
     */
    public BooleanInstruction buildBaseInstructions(final BooleanInstruction instruction) {
        // Exclude unpublished content (based on config)
        if (this.includeOnlyPublishedContent) {
            instruction.must(new MatchInstruction(Constants.PUBLISHED_FIELDNAME, "true"));
        }

        // Exclude regression test content (based on config)
        if (this.excludeRegressionTestContent) {
            instruction.mustNot(new MatchInstruction(Constants.TAGS_FIELDNAME, REGRESSION_TEST_TAG));
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
     * @return This {@link IsaacSearchInstructionBuilder}, to allow chained operations.
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
     * @return This {@link IsaacSearchInstructionBuilder}, to allow chained operations.
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
     * @param searchInField A {@link SearchInField} instance describing terms to look for in a particular field, and
     *                      what priority resultant matches will have in the results (in absence of any sorting applied
     *                      elsewhere). If no search terms are defined for {@code searchInField} it is ignored.
     *
     * @return This IsaacSearchInstructionBuilder, to allow chained operations.
     */
    public IsaacSearchInstructionBuilder searchFor(final SearchInField searchInField) {
        if (!searchInField.getTerms().isEmpty()) {
            this.searchesInFields.add(searchInField);
        }
        return this;
    }

    /**
     * Sets whether to return events where the date field contains a date in the past. Defaults to false, and has no
     * effect if the event content type is excluded.
     *
     * @param includePastEvents Whether to include past events.
     * @return This {@link IsaacSearchInstructionBuilder}, to allow chained operations.
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
        BooleanInstruction masterInstruction = this.buildBaseInstructions(new BooleanInstruction());

        masterInstruction.setMinimumShouldMatch(1);

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

            // For a particular content type's sub-query, if there are no "should"s, having a 'minimum should match'
            // > 0 will give us no results. Otherwise, it is desirable.
            if (!contentInstruction.getShoulds().isEmpty()) {
                contentInstruction.setMinimumShouldMatch(1);
            }

            masterInstruction.should(contentInstruction);
        }

        // Reset the relevant builder state so subsequent 'build' calls do not accumulate instructions unexpectedly.
        this.searchesInFields = new ArrayList<>();
        this.includedContentTypes = Sets.newHashSet();
        this.priorityIncludedContentTypes = Sets.newHashSet();
        this.includePastEvents = false;

        return masterInstruction;
    }

    /**
     * Augments {@code instruction} with the field search instructions specified via {@code searchFor()}.
     *
     * @param instruction A BooleanMatchInstruction for a particular content type to augment with the field-search instructions.
     * @param searchesInFields A list of {@code SearchInField}s encapsulating fields and terms to search for, as well as
     *                         the strategy and priority to use for each.
     * @param contentType The content type {@code instruction} relates to, so we can decide how/whether to process certain
     *                    field searches.
     *                    (todo: Consider replacing with content-type-specific implementations of this method that process
     *                    only fields relevant to the content type. Alternatively, this class could provide
     *                    content-type-specific search builders to clients.)
     */
    private void addSearchesInFieldsToInstruction(final BooleanInstruction instruction, final List<SearchInField> searchesInFields,
                                                  final String contentType) {

        // Multi-match and nested instructions are grouped together across searchInField instances.
        Map<String, BooleanInstruction> nestedInstructionsGroupedByField = Maps.newHashMap();
        Map<String, Set<String>> multiMatchSearchesGroupedByTerm = Maps.newHashMap();

        for (SearchInField searchInField : searchesInFields) {
            // This holds the instructions we generate for a particular search-in-field, so we can apply them all to
            // the parent instruction at the end.
            List<AbstractInstruction> generatedSubInstructions = Lists.newArrayList();

            // Special fields
            if (Arrays.stream(Constants.ADDRESS_PATH_FIELDNAME).collect(Collectors.toList()).contains(searchInField.getField())) {
                // Address fields
                // Non-event content types ignore this
                if (!Objects.equals(contentType, EVENT_TYPE)) {
                    continue;
                }
                // If we're searching any "address" fields, search all of them
                String nestedFieldConnector = searchProvider.getNestedFieldConnector();
                String addressPath = String.join(nestedFieldConnector, Constants.ADDRESS_PATH_FIELDNAME);

                for (String addressField : Constants.ADDRESS_FIELDNAMES) {
                    for (String term : searchInField.getTerms()) {
                        String field = addressPath + nestedFieldConnector + addressField;
                        generatedSubInstructions.add(new MatchInstruction(field, term, EVENT_ADDRESS_FIELD_BOOST, false));
                        generatedSubInstructions.add(new MatchInstruction(field, term, EVENT_ADDRESS_FIELD_BOOST_FUZZY, true));
                    }
                }
            } else if (Constants.NESTED_QUERY_FIELDS.contains(searchInField.getField())) {
                // Nested fields
                /* (This has a specific meaning in ES -
                see https://www.elastic.co/guide/en/elasticsearch/reference/7.17/query-dsl-nested-query.html) */
                // Ensure nested instructions for a particular top-level field are grouped together
                String nestedPath = searchInField.getField().split("\\.")[0];

                BooleanInstruction nestedInstructionForField = new BooleanInstruction(1);

                for (String term : searchInField.getTerms()) {
                    nestedInstructionForField.should(new MatchInstruction(searchInField.getField(), term));
                }

                nestedInstructionsGroupedByField.putIfAbsent(nestedPath, new BooleanInstruction());
                nestedInstructionsGroupedByField.get(nestedPath).must(nestedInstructionForField);

            } else {
                // Generic fields
                for (String term : searchInField.getTerms()) {
                    if (searchInField.getStrategy() == Strategy.DEFAULT) {
                        Long boost = searchInField.getPriority() == Priority.HIGH ? HIGH_PRIORITY_FIELD_BOOST : FIELD_BOOST;
                        Long fuzzyBoost = searchInField.getPriority() == Priority.HIGH ? HIGH_PRIORITY_FIELD_BOOST_FUZZY : FIELD_BOOST_FUZZY;

                        generatedSubInstructions.add(new MatchInstruction(searchInField.getField(), term, boost, false));
                        generatedSubInstructions.add(new MatchInstruction(searchInField.getField(), term, fuzzyBoost, true));

                    } else if (searchInField.getStrategy() == Strategy.FUZZY) {
                        Long boost = searchInField.getPriority() == Priority.HIGH ? HIGH_PRIORITY_WILDCARD_FIELD_BOOST : WILDCARD_FIELD_BOOST;

                        generatedSubInstructions.add(new MatchInstruction(searchInField.getField(), term, boost, true));
                        generatedSubInstructions.add(new WildcardInstruction(searchInField.getField(), "*" + term + "*", boost));
                        // Use a multi-match instruction, and ensure multi-match instructions for a particular term are
                        // grouped together
                        multiMatchSearchesGroupedByTerm.putIfAbsent(term, Sets.newHashSet());
                        multiMatchSearchesGroupedByTerm.get(term).add(searchInField.getField());

                    } else if (searchInField.getStrategy() == Strategy.SIMPLE) {
                        Long boost = searchInField.getPriority() == Priority.HIGH ? HIGH_PRIORITY_FIELD_BOOST : 1L;
                        generatedSubInstructions.add(
                            new MatchInstruction(searchInField.getField(), term, boost, false)
                        );
                    }
                }
            }

            // Add all generated sub-instructions to parent instruction as either "should" or "must" depending on the
            // 'required' and 'optional' flag.
            if (searchInField.getRequired()) {
                generatedSubInstructions.forEach(instruction::must);
            } else if (searchInField.isAtLeastOne()) {
                // Create a boolean sub-instruction such that at least one term must match
                // This means a should 1 match clause attached to the parent as a must match
                BooleanInstruction subInstruction = new BooleanInstruction();
                generatedSubInstructions.forEach(subInstruction::should);
                subInstruction.setMinimumShouldMatch(1);
                instruction.must(subInstruction);
            } else {
                generatedSubInstructions.forEach(instruction::should);
            }
        }

        // Add grouped nested instructions to parent instruction as a single instruction.
        for (Map.Entry<String, BooleanInstruction> entry : nestedInstructionsGroupedByField.entrySet()) {
            instruction.must(new NestedInstruction(entry.getKey(), entry.getValue()));
        }

        // Add grouped multi-match instructions to parent instruction as single instruction.
        for (Map.Entry<String, Set<String>> entry : multiMatchSearchesGroupedByTerm.entrySet()) {
            instruction.should(new MultiMatchInstruction(entry.getKey(), entry.getValue().toArray(String[]::new), 2L));
        }
    }
}
