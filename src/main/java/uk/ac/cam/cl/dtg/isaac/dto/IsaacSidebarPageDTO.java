package uk.ac.cam.cl.dtg.isaac.dto;

import uk.ac.cam.cl.dtg.isaac.dos.content.JsonContentType;
import uk.ac.cam.cl.dtg.isaac.dto.content.SeguePageDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.SidebarEntryDTO;

import java.util.List;

/**
 * Sidebar Page DTO.
 *
 */
@JsonContentType("isaacSidebarPage")
public class IsaacSidebarPageDTO extends SeguePageDTO {
    private List<SidebarEntryDTO> sidebarEntries;

    /**
     * Default constructor for Jackson.
     */
    public IsaacSidebarPageDTO() {
    }

    public List<SidebarEntryDTO> getSidebarEntries() {
        return sidebarEntries;
    }

    public void setSidebarEntries(final List<SidebarEntryDTO> sidebarEntries) {
        this.sidebarEntries = sidebarEntries;
    }
}
