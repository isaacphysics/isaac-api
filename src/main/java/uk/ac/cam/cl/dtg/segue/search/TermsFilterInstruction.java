/*
 * Copyright 2017 Stephen Cummins
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

import java.util.Collection;

/**
 * TermsFilterInstruction.
 * A class to help encapsulate filter instructions.
 *
 * This instruction will expect to match at least one of the terms in the list provided.
 */
public class TermsFilterInstruction extends AbstractFilterInstruction {
    private final Collection<String> matchValues;

    public TermsFilterInstruction(Collection<String> matchValues) {
        this.matchValues = matchValues;
    }

    public Collection<String> getMatchValues() {
        return matchValues;
    }
}
