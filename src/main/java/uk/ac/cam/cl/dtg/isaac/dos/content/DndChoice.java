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

import uk.ac.cam.cl.dtg.isaac.dto.content.DndChoiceDTO;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Choice for Item Questions, containing a list of Items.
 *
 */
@DTOMapping(DndChoiceDTO.class)
@JsonContentType("dndChoice")
public class DndChoice extends Choice {

    private Boolean allowSubsetMatch;
    private List<DndItem> items;

    /**
     * Default constructor required for mapping.
     */
    public DndChoice() {
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

    public boolean matches(final DndChoice rhs) {
        return this.items.stream().allMatch(lhsItem -> dropZoneEql(lhsItem, rhs))
                && this.items.size() == rhs.getItems().size();
    }

    public int countPartialMatchesIn(final DndChoice rhs) {
        return this.items.stream()
                .map(lhsItem -> dropZoneEql(lhsItem, rhs) ? 1 : 0)
                .mapToInt(Integer::intValue)
                .sum();
    }

    public Map<String, Boolean> getDropZonesCorrect(final DndChoice rhs) {
        return this.items.stream()
                .filter(lhsItem -> rhs.getItemByDropZone(lhsItem.getDropZoneId()).isPresent())
                .collect(Collectors.toMap(
                        DndItem::getDropZoneId,
                        lhsItem -> dropZoneEql(lhsItem, rhs))
                );
    }

    private static boolean dropZoneEql(DndItem lhsItem, DndChoice rhs) {
        return rhs.getItemByDropZone(lhsItem.getDropZoneId())
                .map(rhsItem -> rhsItem.getId().equals(lhsItem.getId()))
                .orElse(false);
    }

    private Optional<DndItem> getItemByDropZone(final String dropZoneId) {
        return this.items.stream()
                .filter(item -> item.getDropZoneId().equals(dropZoneId))
                .findFirst();
    }
}
