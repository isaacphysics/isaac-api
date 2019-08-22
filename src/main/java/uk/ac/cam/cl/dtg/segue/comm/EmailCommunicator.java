/*
 * Copyright 2014 Nick Rogers
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

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.commons.lang3.Validate;
import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.util.Mailer;

import javax.mail.MessagingException;
import java.io.UnsupportedEncodingException;

/**
 * @author nr378 and Alistair Stead
 */
public class EmailCommunicator implements ICommunicator<EmailCommunicationMessage> {
	private Mailer mailer;
	private String defaultFromAddress;
	private String mailName;

	/**
	 * Creates an instance of an email communicator that can send e-mails.
	 * 
	 * @param smtpAddress
	 *            the smtp server to use to send e-mails. Must be open for this
	 *            implementation.
	 * @param defaultFromAddress
	 *            - The email address to show as the from address.
	 * @param mailName
	 *            - The name email will be sent from.
	 */
	@Inject
	public EmailCommunicator(@Named(Constants.MAILER_SMTP_SERVER) final String smtpAddress,
							 @Named(Constants.MAIL_FROM_ADDRESS) final String defaultFromAddress,
							 @Named(Constants.MAIL_NAME) final String mailName) {
		Validate.notNull(smtpAddress);
		Validate.notNull(defaultFromAddress);

		this.defaultFromAddress = defaultFromAddress;
		this.mailName = mailName;

		// Construct a new instance of the mailer object
		this.mailer = new Mailer(smtpAddress, defaultFromAddress);
	}

    /**
     * @param email
     *            - message to be sent. Will be plain text if no HTML is provided
     * @throws CommunicationException
     *            - if email fails to be created and added to queue
     */
	@Override
	public void sendMessage(final EmailCommunicationMessage email) throws CommunicationException {
	    String fromAddress = this.defaultFromAddress;
	    String fromName = this.mailName;

	    // If override "From" details specified, use them:
	    if (email.getOverrideFromAddress() != null && !email.getOverrideFromAddress().isEmpty()) {
	        fromAddress = email.getOverrideFromAddress();
        }
	    if (email.getOverrideFromName() != null && !email.getOverrideFromName().isEmpty()) {
	        fromName = email.getOverrideFromName();
        }

        try {
            if (email.getHTMLMessage() == null) {
                mailer.sendPlainTextMail(new String[] { email.getRecipientAddress() }, fromAddress, fromName,
                        email.getReplyToAddress(), email.getReplyToName(), email.getSubject(),
                        email.getPlainTextMessage());
            } else {
                mailer.sendMultiPartMail(new String[] { email.getRecipientAddress() }, fromAddress, fromName,
                        email.getReplyToAddress(), email.getReplyToName(), email.getSubject(),
                        email.getPlainTextMessage(), email.getHTMLMessage(), email.getAttachments());
            }
        } catch (MessagingException | UnsupportedEncodingException e) {
            throw new CommunicationException(e);
        }
    }
}