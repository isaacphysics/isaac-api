package uk.ac.cam.cl.dtg.isaac.dto;

import java.util.List;
import uk.ac.cam.cl.dtg.isaac.dto.content.EmailTemplateDTO;

public class ContentEmailDTO {
  private List<Long> userIds;
  private EmailTemplateDTO emailTemplate;

  /**
   * Default constructor.
   */
  public ContentEmailDTO() {
  }

  public List<Long> getUserIds() {
    return userIds;
  }

  public void setUserIds(final List<Long> userIds) {
    this.userIds = userIds;
  }

  public EmailTemplateDTO getEmailTemplate() {
    return emailTemplate;
  }

  public void setEmailTemplate(final EmailTemplateDTO emailTemplate) {
    this.emailTemplate = emailTemplate;
  }
}
