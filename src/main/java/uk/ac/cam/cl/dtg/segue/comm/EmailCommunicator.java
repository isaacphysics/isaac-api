/**
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

import javax.mail.MessagingException;

import org.apache.commons.lang3.Validate;

import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.util.Mailer;

import com.google.inject.Inject;
import com.google.inject.name.Named;

/**
 * @author nr378 and Alistair Stead
 */
public class EmailCommunicator implements ICommunicator<EmailCommunicationMessage> {
	private Mailer mailer;
	private String fromAddress;

	// TODO: remove reference to isaac
	private static final String SIGNATURE = "Isaac Physics";

	/**
	 * Creates an instance of an email communicator that can send e-mails.
	 * 
	 * @param smtpAddress
	 *            the smtp server to use to send e-mails. Must be open for this
	 *            implementation.
	 * @param fromAddress
	 *            - The email address to show as the from address.
	 */
	@Inject
	public EmailCommunicator(@Named(Constants.MAILER_SMTP_SERVER) final String smtpAddress,
							 @Named(Constants.MAIL_FROM_ADDRESS) final String fromAddress) {
		Validate.notNull(smtpAddress);
		Validate.notNull(fromAddress);

		this.fromAddress = fromAddress;

		// Construct a new instance of the mailer object
		this.mailer = new Mailer(smtpAddress, fromAddress);
	}


	    /**
     * @param email
     *            - message to be sent. Will be plain text if no HTML is provided
     * @throws CommunicationException
     */
	@Override
	public void sendMessage(final EmailCommunicationMessage email)
			throws CommunicationException {
		
		try {
            if (email.getHTMLMessage() == null) {
                mailer.sendPlainTextMail(new String[] { email.getRecipientAddress() }, this.fromAddress,
                        String.format("%s - %s", email.getSubject(), SIGNATURE), email.getPlainTextMessage());
            } else {
                mailer.sendMultiPartMail(new String[] { email.getRecipientAddress() }, this.fromAddress,
                        String.format("%s - %s", email.getSubject(), SIGNATURE), email.getPlainTextMessage(),
                        email.getHTMLMessage());
            }
		} catch (MessagingException e) {
			throw new CommunicationException(e);
		}
	}



}
