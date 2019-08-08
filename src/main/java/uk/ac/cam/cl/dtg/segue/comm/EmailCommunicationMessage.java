/**
 * Copyright 2015 Alistair Stead
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
package uk.ac.cam.cl.dtg.segue.comm;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Class to hold the contents of an email.
 *
 * @author Alistair Stead
 */
public class EmailCommunicationMessage implements ICommunicationMessage {
	
	private final Long userId;

	private final String plainTextMessage;

    private final String recipientAddress;

    private final String subject;

    private final String htmlMessage;

    private final String overrideFromAddress;

    private final String overrideFromName;

    private final String replyToAddress;

    private final String replyToName;
    
    private final EmailType emailType;

    private final List<EmailAttachment> attachments;
    
    /**
     * @param userId (nullable) id of the user
     * @param recipientAddress address of user
     * @param subject subject of email
     * @param plainTextMessage message in email
     * @param overrideFromAddress an override from address
     * @param overrideFromName an override of the from name
     * @param htmlMessage html message in email
     * @param emailType the type of the message
     * @param replyToAddress (nullable) the preferred reply to address.
     * @param replyToName the reply to name for the email message
     * @param attachments list of attachments for the message
     *
     */
    public EmailCommunicationMessage(@Nullable final Long userId, final String recipientAddress,
                                     final String subject, final String plainTextMessage,
                                     final String htmlMessage, final EmailType emailType,
                                     @Nullable final String overrideFromAddress, @Nullable final String overrideFromName,
                                     @Nullable final String replyToAddress, @Nullable final String replyToName,
                                     @Nullable final List<EmailAttachment> attachments) {
        this.userId = userId;
    	this.plainTextMessage = plainTextMessage;
        this.recipientAddress = recipientAddress;
        this.subject = subject;
        this.htmlMessage = htmlMessage;
        this.overrideFromAddress = overrideFromAddress;
        this.overrideFromName = overrideFromName;
        this.replyToAddress = replyToAddress;
        this.replyToName = replyToName;
        this.emailType = emailType;
        this.attachments = attachments;
    }

    /**
     * @param recipientAddress address of user
     * @param subject subject of email
     * @param plainTextMessage message in email
     * @param htmlMessage html message in email
     * @param emailType the type of the message
     *
     */
    public EmailCommunicationMessage(final String recipientAddress, final String subject, final String plainTextMessage,
                                     final String htmlMessage, final EmailType emailType) {
        this.userId = null;
        this.plainTextMessage = plainTextMessage;
        this.recipientAddress = recipientAddress;
        this.subject = subject;
        this.htmlMessage = htmlMessage;
        this.overrideFromAddress = null;
        this.overrideFromName = null;
        this.replyToAddress = null;
        this.replyToName = null;
        this.emailType = emailType;
        this.attachments = null;
    }

    /**
	 * @return the userId
	 */
	public Long getUserId() {
		return userId;
	}
    
    /**
     * @return the plain text message
     */
    public String getPlainTextMessage() {
        return plainTextMessage;
    }

    /**
     * @return the html message
     */
    public String getHTMLMessage() {
        return htmlMessage;
    }

    /**
     * @return the recipientAddress
     */
    public String getRecipientAddress() {
        return recipientAddress;
    }

    /**
     * @return the subject
     */
    public String getSubject() {
        return subject;
    }

    /**
     * @return overrideFromAddress if set.
     */
    public String getOverrideFromAddress() {
        return overrideFromAddress;
    }

    /**
     * @return overrideFromName if set.
     */
    public String getOverrideFromName() {
        return overrideFromName;
    }
    /**
     * @return replyToAddress if set.
     */
    public String getReplyToAddress() {
        return replyToAddress;
    }

    /**
     * @return replyToName if set.
     */
    public String getReplyToName() {
        return replyToName;
    }

	@Override
	public int getPriority() {
		return emailType.getPriority();
	}
	
    /**
	 * @return the type
	 */
	public EmailType getEmailType() {
		return emailType;
	}

    /**
     * @return the list of attachments
     */
    public List<EmailAttachment> getAttachments() {
        return attachments;
    }
}
