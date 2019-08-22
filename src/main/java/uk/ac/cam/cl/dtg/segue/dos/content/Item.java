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
package uk.ac.cam.cl.dtg.segue.dos.content;

import uk.ac.cam.cl.dtg.segue.dto.content.ItemDTO;

/**
 * Specific content for items in Item Choices and Questions.
 *
 */
@DTOMapping(ItemDTO.class)
@JsonContentType("item")
public class Item extends Content {

    /**
     * Default constructor required for mapping.
     */
    public Item() {
    }

    /**
     * Constructor to make testing easier.
     *
     * @param id - the ID for the Item
     * @param value - the value of the Item
     */
    public Item(final String id, final String value) {
        this.id = id;
        this.value = value;
    }
}
