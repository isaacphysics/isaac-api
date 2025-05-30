package uk.ac.cam.cl.dtg.isaac.dos.content;

import uk.ac.cam.cl.dtg.isaac.dto.content.SidebarGroupDTO;

import java.util.List;

/**
 * DO to represent groups of pages inside a sidebar.
 */
@DTOMapping(SidebarGroupDTO.class)
@JsonContentType("sidebarGroup")
public class SidebarGroup extends SidebarEntry {

    private String label;
    private List<SidebarEntry> sidebarEntries;

    public String getLabel() {
        return label;
    }

    public void setLabel(final String label) {
        this.label = label;
    }

    public List<SidebarEntry> getSidebarEntries() {
        return sidebarEntries;
    }

    public void setSidebarEntries(final List<SidebarEntry> sidebarEntries) {
        this.sidebarEntries = sidebarEntries;
    }
}
