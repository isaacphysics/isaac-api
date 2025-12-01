/*
 * Copyright 2019 James Sharkey
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
package uk.ac.cam.cl.dtg.isaac.dos.content;

import uk.ac.cam.cl.dtg.isaac.dto.content.DndItemChoiceDTO;

import java.util.List;
import java.util.Optional;

/**
 * Choice for Item Questions, containing a list of Items.
 *
 */
@DTOMapping(DndItemChoiceDTO.class)
@JsonContentType("dndChoice")
public class DndItemChoice extends Choice {

    private Boolean allowSubsetMatch;
    private List<DndItem> items;

    /**
     * Default constructor required for mapping.
     */
    public DndItemChoice() {
    }

    public List<DndItem> getItems() {
        return items;
    }

    public void setItems(final List<DndItem> items) {
        this.items = items;
    }

    public Boolean isAllowSubsetMatch() {
        return this.allowSubsetMatch;
    }

    public void setAllowSubsetMatch(final boolean allowSubsetMatch) {
        this.allowSubsetMatch = allowSubsetMatch;
    }

    private Optional<DndItem> getItemByDropZone(final String dropZoneId) {
        return this.items.stream()
                .filter(item -> item.getDropZoneId().equals(dropZoneId))
                .findFirst();
    }

    public int matchStrength(final DndItemChoice rhs) {
        return this.items.stream()
            .map(lhsItem ->
                rhs.getItemByDropZone(lhsItem.getDropZoneId())
                    .map(rhsItem -> rhsItem.getId().equals(lhsItem.getId()) ? -1 : 0)
                    .orElse(0)
            )
            .mapToInt(Integer::intValue)
            .sum();
    }
}
