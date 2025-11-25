/*
 * Copyright 2014 Stephen Cummins
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

import uk.ac.cam.cl.dtg.isaac.dto.content.FigureDTO;
import uk.ac.cam.cl.dtg.util.DropZone;

import java.util.List;

/**
 * Figure class is a specialisation of an Image.
 */
@DTOMapping(FigureDTO.class)
@JsonContentType("figure")
public class Figure extends Image {
    private List<DropZone> dropZones;
    private String condensedMaxWidth;

    public List<DropZone> getDropZones() {
        return dropZones;
    }

    public void setDropZones(List<DropZone> dropZones) {
        this.dropZones = dropZones;
    }

    public String getCondensedMaxWidth() {
        return condensedMaxWidth;
    }

    public void setCondensedMaxWidth(String condensedMaxWidth) {
        this.condensedMaxWidth = condensedMaxWidth;
    }
}
