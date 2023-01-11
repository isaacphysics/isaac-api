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

import com.google.api.client.util.Lists;

import java.util.List;

public class NestedInstruction extends AbstractInstruction {
    private List<AbstractInstruction> musts = Lists.newArrayList();
    private List<AbstractInstruction> shoulds = Lists.newArrayList();
    private String path;

    public NestedInstruction(String path) {
        this.path = path;
    }

    public List<AbstractInstruction> getMusts() {
        return musts;
    }

    public void must(final AbstractInstruction abstractInstruction) {
        musts.add(abstractInstruction);
    }

    public List<AbstractInstruction> getShoulds() {
        return shoulds;
    }

    public void should(final AbstractInstruction abstractInstruction) {
        shoulds.add(abstractInstruction);
    }

    public String getPath() {
        return path;
    }
}
