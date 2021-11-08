package uk.ac.cam.cl.dtg.segue.dto;

import uk.ac.cam.cl.dtg.segue.dto.content.EmailTemplateDTO;

import java.util.List;

public class ContentEmailDTO extends EmailTemplateDTO {
    private List<Long> userIds;

    /**
     * Default constructor.
     */
    public ContentEmailDTO() {}

    public List<Long> getUserIds() {
        return userIds;
    }

    public void setUserIds(List<Long> userIds) {
        this.userIds = userIds;
    }
}
