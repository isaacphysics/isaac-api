/*
 * Copyright 2025 James Sharkey
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
package uk.ac.cam.cl.dtg.segue.dao.content;

import ma.glasnost.orika.MappingContext;
import ma.glasnost.orika.metadata.Type;
import uk.ac.cam.cl.dtg.isaac.dos.content.SidebarEntry;
import uk.ac.cam.cl.dtg.isaac.dos.content.SidebarGroup;
import uk.ac.cam.cl.dtg.isaac.dto.content.SidebarEntryDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.SidebarGroupDTO;

/**
 * Converts SidebarEntry objects to and from their DTO equivalents.
 *
 */
public class SidebarEntryOrikaConverter extends AbstractPolymorphicBidirectionalConverter<SidebarEntry, SidebarEntryDTO> {

    /**
     * Constructs an Orika Converter to select the correct subclass for SidebarEntry objects.
     *
     */
    public SidebarEntryOrikaConverter() {

    }

    @Override
    public SidebarEntryDTO convertTo(final SidebarEntry source, final Type<SidebarEntryDTO> destinationType, MappingContext _context) {
        if (null == source) {
            return null;
        }

        if (source instanceof SidebarGroup) {
            return super.mapperFacade.map(source, SidebarGroupDTO.class);
        } else {
            SidebarEntryDTO sidebarEntryDTO = new SidebarEntryDTO();
            super.mapperFacade.map(source, sidebarEntryDTO);
            return sidebarEntryDTO;
        }
    }

    @Override
    public SidebarEntry convertFrom(final SidebarEntryDTO source, final Type<SidebarEntry> destinationType, MappingContext _context) {
        if (null == source) {
            return null;
        }

        if (source instanceof SidebarGroupDTO) {
            return super.mapperFacade.map(source, SidebarGroup.class);
        } else {
            SidebarEntry sidebarEntry = new SidebarEntry();
            super.mapperFacade.map(source, sidebarEntry);
            return sidebarEntry;
        }
    }
}
