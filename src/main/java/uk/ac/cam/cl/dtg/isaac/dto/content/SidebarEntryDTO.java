package uk.ac.cam.cl.dtg.isaac.dto.content;

import uk.ac.cam.cl.dtg.isaac.dos.content.JsonContentType;

/**
 * DTO to represent pages inside a sidebar.
 */
@JsonContentType("sidebarEntry")
public class SidebarEntryDTO extends ContentDTO {

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
