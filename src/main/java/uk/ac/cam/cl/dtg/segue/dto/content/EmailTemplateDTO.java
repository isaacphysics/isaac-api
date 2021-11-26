/*
 * Copyright 2015 Stephen Cummins
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.cam.cl.dtg.segue.dto.content;

import uk.ac.cam.cl.dtg.segue.dos.content.JsonContentType;

import java.util.List;
import java.util.Set;

/**
 * EmailTemplate.
 * 
 */
@JsonContentType("emailTemplate")
public class EmailTemplateDTO extends ContentDTO {
    private String subject;
    private String plainTextContent;
    private String htmlContent;
    private String overrideFromAddress;
    private String overrideFromName;
    private String overrideEnvelopeFrom;
    private String replyToEmailAddress;
    private String replyToName;
    
    /**
     * EmailTemplate.
     * 
     * @param _id
     * @param id
     * @param title
     * @param subtitle
     * @param type
     * @param author
     * @param encoding
     * @param canonicalSourceFile
     * @param layout
     * @param children
     * @param value
     * @param attribution
     * @param relatedContent
     * @param published
     * @param deprecated
     * @param tags
     * @param level
     */
    public EmailTemplateDTO(final String _id, final String id, final String title, final String subtitle,
            final String type, final String author, final String encoding, final String canonicalSourceFile,
            final String layout, final List<ContentBaseDTO> children, final String value, final String attribution,
            final List<ContentSummaryDTO> relatedContent, final Boolean published, final Boolean deprecated,
            final Set<String> tags, final Integer level) {
        super(id, title, subtitle, type, author, encoding, canonicalSourceFile, layout, children, value,
                attribution, relatedContent, published, deprecated, tags, level);
    }

    /**
     * @param value
     */
    public EmailTemplateDTO(final String value) {
        super(value);
    }

    /**
     * 
     */
    public EmailTemplateDTO() {

    }

    /**
     * Gets the subject.
     * @return the subject
     */
    public String getSubject() {
        return subject;
    }

    /**
     * Sets the subject.
     * @param subject the subject to set
     */
    public void setSubject(final String subject) {
        this.subject = subject;
    }

    /**
     * Gets the plainTextContent.
     * @return the plainTextContent
     */
    public String getPlainTextContent() {
        return plainTextContent;
    }

    /**
     * Sets the plainTextContent.
     * @param plainTextContent the plainTextContent to set
     */
    public void setPlainTextContent(final String plainTextContent) {
        this.plainTextContent = plainTextContent;
    }

    /**
     * Gets the htmlContent.
     * @return the htmlContent
     */
    public String getHtmlContent() {
        return htmlContent;
    }

    /**
     * Sets the htmlContent.
     * @param htmlContent the htmlContent to set
     */
    public void setHtmlContent(final String htmlContent) {
        this.htmlContent = htmlContent;
    }

    /**
     * Gets the overrideFromAddress.
     * @return the overrideFromAddress
     */
    public String getOverrideFromAddress() {
        return overrideFromAddress;
    }

    /**
     * Sets the overrideFromAddress.
     * @param overrideFromAddress the overrideFromAddress to set
     */
    public void setOverrideFromAddress(final String overrideFromAddress) {
        this.overrideFromAddress = overrideFromAddress;
    }
    /**
     * Gets the overrideFromName.
     * @return the overrideFromName
     */
    public String getOverrideFromName() {
        return overrideFromName;
    }
    /**
     * Sets the overrideFromName.
     * @param overrideFromName the overrideFromName to set
     */
    public void setOverrideFromName(final String overrideFromName) {
        this.overrideFromName = overrideFromName;
    }

    /**
     * Gets the overrideEnvelopeFrom.
     * @return the overrideEnvelopeFrom
     */
    public String getOverrideEnvelopeFrom() {
        return overrideEnvelopeFrom;
    }

    /**
     * Sets the overrideEnvelopeFrom.
     * @param overrideEnvelopeFrom the overrideEnvelopeFrom to set
     */
    public void setOverrideEnvelopeFrom(final String overrideEnvelopeFrom) {
        this.overrideEnvelopeFrom = overrideEnvelopeFrom;
    }
    /**
     * Gets the replyToEmailAddress.
     * @return the replyToEmailAddress
     */
    public String getReplyToEmailAddress() {
        return replyToEmailAddress;
    }

    /**
     * Sets the replyToEmailAddress.
     * @param replyToEmailAddress the replyToEmailAddress to set
     */
    public void setReplyToEmailAddress(final String replyToEmailAddress) {
        this.replyToEmailAddress = replyToEmailAddress;
    }

    /**
     * Gets the replyToName.
     * @return the replyToName
     */
    public String getReplyToName() {
        return replyToName;
    }

    /**
     * Sets the replyToName.
     * @param replyToName the replyToName to set
     */
    public void setReplyToName(final String replyToName) {
        this.replyToName = replyToName;
    }
}
