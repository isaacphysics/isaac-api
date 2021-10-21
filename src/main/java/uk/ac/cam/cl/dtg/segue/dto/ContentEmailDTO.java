package uk.ac.cam.cl.dtg.segue.dto;

import java.util.List;

public class ContentEmailDTO {
    private List<Long> userIds;
    private String plaintextTemplate;
    private String htmlTemplate;
    private String emailSubject;

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

    public String getPlaintextTemplate() {
        return plaintextTemplate;
    }

    public void setPlaintextTemplate(String plaintextTemplate) {
        this.plaintextTemplate = plaintextTemplate;
    }

    public String getHtmlTemplate() {
        return htmlTemplate;
    }

    public void setHtmlTemplate(String htmlTemplate) {
        this.htmlTemplate = htmlTemplate;
    }

    public String getEmailSubject() {
        return emailSubject;
    }

    public void setEmailSubject(String emailSubject) {
        this.emailSubject = emailSubject;
    }
}
