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

import java.util.Set;

public class SearchInField {

    private final String field;
    private final Set<String> terms;
    private final IsaacSearchInstructionBuilder.Strategy strategy;
    private final IsaacSearchInstructionBuilder.Priority priority;

    public SearchInField(final String field, final Set<String> terms, final IsaacSearchInstructionBuilder.Strategy strategy,
                         final IsaacSearchInstructionBuilder.Priority priority) {
        this.field = field;
        this.terms = terms;
        this.strategy = strategy;
        this.priority = priority;
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
}
