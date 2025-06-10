package uk.ac.cam.cl.dtg.isaac.dto.content;

import java.util.List;

public class SidebarDTO extends ContentDTO {

    private List<SidebarEntryDTO> sidebarEntries;

    /**
     * Default constructor for mapping.
     */
    public SidebarDTO() {
    }

    public List<SidebarEntryDTO> getSidebarEntries() {
        return sidebarEntries;
    }

    public void setSidebarEntries(final List<SidebarEntryDTO> sidebarEntries) {
        this.sidebarEntries = sidebarEntries;
    }
}
