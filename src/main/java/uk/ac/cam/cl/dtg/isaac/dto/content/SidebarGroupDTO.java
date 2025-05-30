package uk.ac.cam.cl.dtg.isaac.dto.content;

import java.util.List;

/**
 * DTO to represent groups of pages inside a sidebar.
 */
public class SidebarGroupDTO extends SidebarEntryDTO {

    private String label;
    private List<SidebarEntryDTO> sidebarEntries;

    public String getLabel() {
        return label;
    }

    public void setLabel(final String label) {
        this.label = label;
    }

    public List<SidebarEntryDTO> getSidebarEntries() {
        return sidebarEntries;
    }

    public void setSidebarEntries(final List<SidebarEntryDTO> sidebarEntries) {
        this.sidebarEntries = sidebarEntries;
    }
}
