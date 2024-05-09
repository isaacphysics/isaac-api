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

import uk.ac.cam.cl.dtg.segue.search.IsaacSearchInstructionBuilder.Priority;
import uk.ac.cam.cl.dtg.segue.search.IsaacSearchInstructionBuilder.Strategy;

import java.util.Set;


public class SearchInField {

    private final String field;
    private final Set<String> terms;
    private Strategy strategy;
    private Priority priority;
    private boolean required;
    private boolean atLeastOne;


    /**
     * Describes to {@code IsaacSearchInstructionBuilder} what terms to look for in a particular field, and what priority
     * resultant matches will have in the results.
     */
    public SearchInField(final String field, final Set<String> terms) {
        this.field = field;
        this.terms = terms;
        this.strategy = Strategy.DEFAULT;
        this.priority = Priority.NORMAL;
        this.required = false;
        this.atLeastOne = false;
    }

    /**
     * @param strategy The strategy to use to match the terms in question. Determines query type used in the resultant
     *                 instruction.
     * @return This SearchInField instance for chained operations.
     */
    public SearchInField strategy(Strategy strategy) {
        this.strategy = strategy;
        return this;
    }

    /**
     * @param priority The priority of any matches in the results. Determines "boost" value in the resultant instruction.
     * @return This SearchInField instance for chained operations.
     */
    public SearchInField priority(Priority priority) {
        this.priority = priority;
        return this;
    }

    /**
     * Determines whether this clause is a "should" or "must" in the resultant instruction, i.e. the difference between
     * influencing result ranking (required=false) / whether the result is returned at all (required=true).
     *
     * @param required Whether this clause is a "should" or "must" in the resultant instruction.
     * @return This SearchInField instance for chained operations.
     */
    public SearchInField required(Boolean required) {
        this.required = required;
        return this;
    }

    public SearchInField atLeastOne(Boolean atLeastOne) {
        this.atLeastOne = atLeastOne;
        return this;
    }

    public String getField() {
        return field;
    }

    public Set<String> getTerms() {
        return terms;
    }

    public IsaacSearchInstructionBuilder.Strategy getStrategy() {
        return strategy;
    }

    public IsaacSearchInstructionBuilder.Priority getPriority() {
        return priority;
    }

    public boolean getRequired() {
        return required;
    }

    public boolean isAtLeastOne() {
        return atLeastOne;
    }
}
