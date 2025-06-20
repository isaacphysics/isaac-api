package uk.ac.cam.cl.dtg.isaac.dos.content;

import uk.ac.cam.cl.dtg.isaac.dto.content.SidebarEntryDTO;

/**
 * DO to represent pages inside a sidebar.
 */
@DTOMapping(SidebarEntryDTO.class)
@JsonContentType("sidebarEntry")
public class SidebarEntry extends Content {

    private String label;
    private String pageId;
    private String pageType;

    public String getLabel() {
        return label;
    }

    public void setLabel(final String label) {
        this.label = label;
    }

    public String getPageId() {
        return pageId;
    }

    public void setPageId(final String pageId) {
        this.pageId = pageId;
    }

    public String getPageType() {
        return pageType;
    }

    public void setPageType(final String pageType) {
        this.pageType = pageType;
    }
}
