/*
 * Copyright 2019 James Sharkey
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
package uk.ac.cam.cl.dtg.segue.dto.content;

/**
 * DTO to represent items in Parsons Choices and Questions.
 *
 */
public class ParsonsItemDTO extends ItemDTO {

    private Integer indentation;

    /**
     * Default constructor required for mapping.
     */
    public ParsonsItemDTO() {
    }

    public Integer getIndentation() {
        return indentation;
    }

    public void setIndentation(final Integer indentation) {
        this.indentation = indentation;
    }
}
