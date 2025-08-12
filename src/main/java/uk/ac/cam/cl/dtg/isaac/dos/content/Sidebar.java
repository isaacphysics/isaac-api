package uk.ac.cam.cl.dtg.isaac.dos.content;

import uk.ac.cam.cl.dtg.isaac.dto.content.SidebarDTO;

import java.util.List;

@DTOMapping(SidebarDTO.class)
@JsonContentType("sidebar")
public class Sidebar extends Content {

    private List<SidebarEntry> sidebarEntries;

    /**
     * Default constructor for mapping.
     */
    public Sidebar() {
    }

    public List<SidebarEntry> getSidebarEntries() {
        return sidebarEntries;
    }

    public void setSidebarEntries(final List<SidebarEntry> sidebarEntries) {
        this.sidebarEntries = sidebarEntries;
    }
}
