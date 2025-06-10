package uk.ac.cam.cl.dtg.isaac.dto.content;

import java.util.List;

/**
 * DTO to represent groups of pages inside a sidebar.
 */
public class SidebarGroupDTO extends SidebarEntryDTO {

    private List<SidebarEntryDTO> sidebarEntries;

    public List<SidebarEntryDTO> getSidebarEntries() {
        return sidebarEntries;
    }

    public void setSidebarEntries(final List<SidebarEntryDTO> sidebarEntries) {
        this.sidebarEntries = sidebarEntries;
    }
}
