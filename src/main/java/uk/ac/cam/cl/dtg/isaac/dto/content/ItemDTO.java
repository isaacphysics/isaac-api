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
package uk.ac.cam.cl.dtg.isaac.dto.content;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * DTO to represent items in Item Choices and Questions.
 *
 */
public class ItemDTO extends ContentDTO {

    private String altText;

    /**
     * Default constructor required for mapping.
     */
    public ItemDTO() {
    }

    // We don't want this field appearing in the DTO JSON!
    @Override
    @JsonIgnore
    public Boolean getPublished() {
        return super.getPublished();
    }

    public String getAltText() {
        return this.altText;
    }

    public void setAltText(final String altText) {
        this.altText = altText;
    }

}
