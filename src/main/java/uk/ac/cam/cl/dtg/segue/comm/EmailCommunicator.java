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

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.commons.lang3.Validate;
import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.util.Mailer;

import javax.mail.MessagingException;

/**
 * @author nr378
 */
public class EmailCommunicator implements ICommunicator {
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
	 *            - The email address to to show as the from address.
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

	@Override
	public void sendMessage(final String recipientAddress, final String recipientName, final String subject,
			final String message) throws CommunicationException {

		// Construct message
		StringBuilder messageBuilder = new StringBuilder();
		messageBuilder.append(String.format("Hello %s,\n\n", recipientName));
		messageBuilder.append(message);
		messageBuilder.append(String.format("\n\n%s", SIGNATURE));

		// Send email
		try {
			mailer.sendMail(new String[] { recipientAddress }, this.fromAddress,
					String.format("%s - %s", subject, SIGNATURE), messageBuilder.toString());
		} catch (MessagingException e) {
			throw new CommunicationException(e);
		}
	}
}
