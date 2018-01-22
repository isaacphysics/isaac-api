/*
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

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.annotation.Nullable;
import javax.mail.Message;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.apache.commons.lang3.Validate;
import uk.ac.cam.cl.dtg.segue.comm.EmailAttachment;

/**
 * Mailer Class Utility Class for sending e-mails such as contact us forms or
 * notifications.
 * 
 * @author Stephen Cummins
 */
public class Mailer {
	private String smtpAddress;
	private String mailAddress;
	private String mailName;
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
	 * @param mailName
	 *			  The name to send emails from.
	 */
	public Mailer(final String smtpAddress, final String mailAddress, final String mailName) {
		this.smtpAddress = smtpAddress;
		this.mailAddress = mailAddress;
		this.mailName = mailName;
	}

    /**
     * SendMail Utility Method Sends e-mail to a given recipient using the hard-coded MAIL_ADDRESS and SMTP details.
     * 
     * @param recipient
     *            - string array of recipients that the message should be sent to
     * @param from
     *            - the e-mail address that should be used as the sending address
     * @param replyTo
     *            - (nullable) the e-mail address that should be used as the reply-to address
	 * @param replyToName
	 *            - (nullable) the name that should be used as the reply-to name
     * @param subject
     *            - The message subject
     * @param contents
     *            - The message body
     * @throws MessagingException
     *             - if we cannot send the message for some reason.
     * @throws AddressException
     *             - if the address is not valid.
     */
    public void sendPlainTextMail(final String[] recipient, final String from, @Nullable final String replyTo,
            @Nullable final String replyToName, final String subject, final String contents)
			throws MessagingException, UnsupportedEncodingException {
        Message msg = this.setupMessage(recipient, from, replyTo, replyToName, subject);
        
        msg.setText(contents);

        Transport.send(msg);
    }

    /**
     * Utility method to allow us to send multipart messages using HTML and plain text.
     *
     * 
     * @param recipient
     *            - string array of recipients that the message should be sent to
     * @param from
     *            - the e-mail address that should be used as the sending address
     * @param replyTo
     *            - (nullable) the e-mail address that should be used as the reply-to address
	 * @param replyToName
	 *            - (nullable) the name that should be used as the reply-to name
     * @param subject
     *            - The message subject
     * @param plainText
     *            - The message body
     * @param html
     *            - The message body in html
	 * @param attachments
	 * 			  - list of attachment objects
     * @throws MessagingException
     *             - if we cannot send the message for some reason.
     * @throws AddressException
     *             - if the address is not valid.
     */
    public void sendMultiPartMail(final String[] recipient, final String from, @Nullable final String replyTo,
			@Nullable final String replyToName, final String subject, final String plainText, final String html,
			final List<EmailAttachment> attachments)
			throws MessagingException, AddressException, UnsupportedEncodingException {

    	Message msg = this.setupMessage(recipient, from, replyTo, replyToName, subject);
        
        // Create the text part
        MimeBodyPart textPart = new MimeBodyPart();
        textPart.setText(plainText, "utf-8");

        MimeBodyPart htmlPart = new MimeBodyPart();
        htmlPart.setContent(html, "text/html; charset=utf-8");

        Multipart multiPart = new MimeMultipart("alternative");
        multiPart.addBodyPart(textPart);
        multiPart.addBodyPart(htmlPart);

		if (attachments != null) {
			for (EmailAttachment attachment : attachments) {
				if (null == attachment) {
					continue;
				}

				MimeBodyPart attachmentBodyPart = new MimeBodyPart();
				DataHandler dataHandler = new DataHandler(attachment.getAttachment(), attachment.getMimeType());
				attachmentBodyPart.setDataHandler(dataHandler);
				attachmentBodyPart.setFileName(attachment.getFileName());
				multiPart.addBodyPart(attachmentBodyPart);
			}
		}

        msg.setContent(multiPart);
        
        Transport.send(msg);
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
	 * Gets the mailName.
	 * @return the mailName
	 */
	public String getMailName() {
		return mailName;
	}

	/**
	 * Sets the mailName.
	 * @param mailName the mailName to set
	 */
	public void setMailName(final String mailName) {
		this.mailName = mailName;
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
	
    /**
     * @param recipient
     *            - string array of recipients that the message should be sent to
     * @param from
     *            - the e-mail address that should be used as the sending address
     * @param replyTo
     *            - the e-mail address that should be used as the reply-to address
     * @param subject
     *            - The message subject
     * @return a newly created message with all of the headers populated.
     * @throws MessagingException - if there is an error in setting up the message
     */
    private Message setupMessage(final String[] recipient, final String from, @Nullable final String replyTo,
			 @Nullable final String replyToName, final String subject)
			throws MessagingException, UnsupportedEncodingException {
        Validate.notEmpty(recipient);
        Validate.notBlank(recipient[0]);
        Validate.notBlank(from);
        
        Properties p = new Properties();
        p.put("mail.smtp.host", smtpAddress);

        if (null != smtpPort) {
            p.put("mail.smtp.port", smtpPort);
        }

        p.put("mail.smtp.starttls.enable", "true");

        Session s = Session.getDefaultInstance(p);
        Message msg = new MimeMessage(s);

        InternetAddress sentBy;
        InternetAddress[] sender = new InternetAddress[1];
        InternetAddress[] receivers = new InternetAddress[recipient.length];

        sentBy = new InternetAddress(mailAddress, mailName);
        sender[0] = new InternetAddress(from);
        for (int i = 0; i < recipient.length; i++) {
            receivers[i] = new InternetAddress(recipient[i]);
        }

		msg.setFrom(sentBy);
		msg.setRecipients(RecipientType.TO, receivers);
		msg.setSubject(subject);

		if (null != replyTo && null != replyToName) {
			msg.setReplyTo(new InternetAddress[] { new InternetAddress(replyTo, replyToName) });
		}

        return msg;
    }
}