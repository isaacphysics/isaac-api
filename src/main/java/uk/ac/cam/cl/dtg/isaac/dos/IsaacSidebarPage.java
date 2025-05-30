package uk.ac.cam.cl.dtg.isaac.dos;

import uk.ac.cam.cl.dtg.isaac.dos.content.DTOMapping;
import uk.ac.cam.cl.dtg.isaac.dos.content.JsonContentType;
import uk.ac.cam.cl.dtg.isaac.dos.content.SeguePage;
import uk.ac.cam.cl.dtg.isaac.dos.content.SidebarEntry;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacSidebarPageDTO;

import java.util.List;

/**
 * Sidebar Page DO.
 *
 */
@DTOMapping(IsaacSidebarPageDTO.class)
@JsonContentType("isaacSidebarPage")
public class IsaacSidebarPage extends SeguePage {
    private List<SidebarEntry> sidebarEntries;

    /**
     * Default constructor for Jackson.
     */
    public IsaacSidebarPage() {
    }

    public List<SidebarEntry> getSidebarEntries() {
        return sidebarEntries;
    }

    public void setSidebarEntries(final List<SidebarEntry> sidebarEntries) {
        this.sidebarEntries = sidebarEntries;
    }
}
