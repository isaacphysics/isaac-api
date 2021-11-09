package uk.ac.cam.cl.dtg.segue.dto;

import uk.ac.cam.cl.dtg.segue.dto.content.EmailTemplateDTO;

import java.util.List;

public class ContentEmailDTO {
    private List<Long> userIds;
    private EmailTemplateDTO emailTemplate;

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

    public EmailTemplateDTO getEmailTemplate() {
        return emailTemplate;
    }

    public void setEmailTemplate(EmailTemplateDTO emailTemplate) {
        this.emailTemplate = emailTemplate;
    }
}
