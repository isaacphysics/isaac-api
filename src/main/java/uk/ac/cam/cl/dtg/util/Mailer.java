/**
 * Copyright 2014 Stephen Cummins
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
package uk.ac.cam.cl.dtg.util;

import java.util.Properties;

import javax.mail.Message;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

/**
 * Mailer Class Utility Class for sending e-mails such as contact us forms or
 * notifications.
 * 
 * @author Stephen Cummins
 */
public class Mailer {

	private String smtpAddress;
	private String mailAddress;
	private String smtpPort;

	/**
	 * Mailer Class.
	 * 
	 * @param smtpAddress
	 *            The address of the smtp server - this implementation assumes
	 *            that the port is the default (25)
	 * @param mailAddress
	 *            The mail address of the sending account - used for
	 *            authentication sometimes.
	 */
	public Mailer(final String smtpAddress, final String mailAddress) {
		this.smtpAddress = smtpAddress;
		this.mailAddress = mailAddress;
	}

	/**
	 * SendMail Utility Method Sends e-mail to a given recipient using the
	 * hard-coded MAIL_ADDRESS and SMTP details.
	 * 
	 * @param recipient
	 *            - string array of recipients that the message should be sent
	 *            to
	 * @param from
	 *            - the e-mail address that should be used as the reply-to
	 *            address (e.g. the true senders address)
	 * @param subject
	 *            - The message subject
	 * @param contents
	 *            - The message body
	 * @throws MessagingException - if we cannot send the message for some reason.
	 * @throws AddressException - if the address is not valid.
	 */
	public void sendMail(final String[] recipient, final String from, final String subject,
			final String contents) throws MessagingException, AddressException {

		Properties p = new Properties();
		p.put("mail.smtp.host", smtpAddress);

		if (null != smtpPort) {
			p.put("mail.smtp.port", smtpPort);
		}

		p.put("mail.smtp.starttls.enable", "true");

		Session s = Session.getDefaultInstance(p);
		Message msg = new MimeMessage(s);

		InternetAddress sentBy = null;
		InternetAddress[] sender = new InternetAddress[1];
		InternetAddress[] recievers = new InternetAddress[recipient.length];

		sentBy = new InternetAddress(mailAddress);
		sender[0] = new InternetAddress(from);
		for (int i = 0; i < recipient.length; i++) {
			recievers[i] = new InternetAddress(recipient[i]);
		}

		if (sentBy != null && sender != null && recievers != null) {
			msg.setFrom(sentBy);
			msg.setReplyTo(sender);
			msg.setRecipients(RecipientType.TO, recievers);
			msg.setSubject(subject);
			msg.setText(contents);

			Transport.send(msg);
		}
	}

	/**
	 * Gets the smtpAddress.
	 * @return the smtpAddress
	 */
	public String getSmtpAddress() {
		return smtpAddress;
	}

	/**
	 * Sets the smtpAddress.
	 * @param smtpAddress the smtpAddress to set
	 */
	public void setSmtpAddress(final String smtpAddress) {
		this.smtpAddress = smtpAddress;
	}

	/**
	 * Gets the mailAddress.
	 * @return the mailAddress
	 */
	public String getMailAddress() {
		return mailAddress;
	}

	/**
	 * Sets the mailAddress.
	 * @param mailAddress the mailAddress to set
	 */
	public void setMailAddress(final String mailAddress) {
		this.mailAddress = mailAddress;
	}

	/**
	 * Gets the smtpPort.
	 * @return the smtpPort
	 */
	public String getSmtpPort() {
		return smtpPort;
	}

	/**
	 * Sets the smtpPort.
	 * @param smtpPort the smtpPort to set
	 */
	public void setSmtpPort(final String smtpPort) {
		this.smtpPort = smtpPort;
	}
}