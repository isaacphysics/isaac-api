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

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.segue.comm.EmailAttachment;

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
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Mailer Class Utility Class for sending e-mails such as contact us forms or
 * notifications.
 * 
 * @author Stephen Cummins
 */
public class Mailer {
    private static final Logger log = LoggerFactory.getLogger(Mailer.class);
	private String smtpAddress;
	private String mailAddress;
	private String smtpPort;
	private final static ConcurrentMap<Integer, Session> sessionCache = new ConcurrentHashMap<>();

	/**
	 * Mailer Class.
	 * 
	 * @param smtpAddress
	 *            The address of the smtp server - this implementation assumes
	 *            that the port is the default (25)
	 * @param mailAddress
	 *            The Envelope-From address of the sending account - used for
	 *            authentication sometimes, and will receive bounce emails.
	 */
	public Mailer(final String smtpAddress, final String mailAddress) {
		this.smtpAddress = smtpAddress;
		this.mailAddress = mailAddress;
	}

    /**
     * SendMail Utility Method Sends e-mail to a given recipient using the hard-coded MAIL_ADDRESS and SMTP details.
     * 
     * @param recipient
     *            - string array of recipients that the message should be sent to
     * @param fromAddress
     *            - the e-mail address that should be used as the sending address
     * @param overrideEnvelopeFrom
     *            - (nullable) the e-mail address that should be used as the envelope from address, useful for routing
     * @param replyTo
     *            - (nullable) the e-mail address that should be used as the reply-to address
     * @param subject
     *            - The message subject
     * @param contents
     *            - The message body
     * @throws MessagingException
     *             - if we cannot send the message for some reason.
     * @throws AddressException
     *             - if the address is not valid.
     */
    public void sendPlainTextMail(final String[] recipient, final InternetAddress fromAddress,
                                  @Nullable final String overrideEnvelopeFrom, @Nullable final InternetAddress replyTo,
                                  final String subject, final String contents)
			throws MessagingException {
        Message msg = this.setupMessage(recipient, fromAddress, overrideEnvelopeFrom, replyTo, subject);
        
        msg.setText(contents);

        Transport.send(msg);
    }

    /**
     * Utility method to allow us to send multipart messages using HTML and plain text.
     *
     * 
     * @param recipient
     *            - string array of recipients that the message should be sent to
     * @param fromAddress
     *            - the e-mail address that should be used as the sending address
     * @param overrideEnvelopeFrom
     *            - (nullable) the e-mail address that should be used as the envelope from address, useful for routing
     * @param replyTo
     *            - (nullable) the e-mail address that should be used as the reply-to address
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
    public void sendMultiPartMail(final String[] recipient, final InternetAddress fromAddress,
                                  @Nullable final String overrideEnvelopeFrom, @Nullable final InternetAddress replyTo,
                                  final String subject, final String plainText, final String html,
                                  final List<EmailAttachment> attachments)
			throws MessagingException, AddressException {

    	Message msg = this.setupMessage(recipient, fromAddress, overrideEnvelopeFrom, replyTo, subject);
        
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
     * @param fromAddress
     *            - the e-mail address that should be used as the sending address
     * @param overrideEnvelopeFrom
     *            - (nullable) the e-mail address that should be used as the envelope from address, useful for routing
     * @param replyTo
     *            - the e-mail address that should be used as the reply-to address
     * @param subject
     *            - The message subject
     * @return a newly created message with all of the headers populated.
     * @throws MessagingException - if there is an error in setting up the message
     */
    private Message setupMessage(final String[] recipient, final InternetAddress fromAddress,
                                 final String overrideEnvelopeFrom, @Nullable final InternetAddress replyTo,
                                 final String subject)
			throws MessagingException {
        Validate.notEmpty(recipient);
        Validate.notBlank(recipient[0]);
        Validate.notNull(fromAddress);
        
        Properties p = new Properties();

        // Configure the SMTP server settings:
        p.put("mail.smtp.host", smtpAddress);
        if (null != smtpPort) {
            p.put("mail.smtp.port", smtpPort);
        }
        p.put("mail.smtp.starttls.enable", "true");

        // Configure the email headers and routing:
        String envelopeFrom = mailAddress;
        if (null != overrideEnvelopeFrom) {
            envelopeFrom = overrideEnvelopeFrom;
        }
        p.put("mail.smtp.from", envelopeFrom);  // Used for Return-Path
        p.put("mail.from", fromAddress.getAddress()); // Should only affect Message-ID, since From overridden below

        // Create the javax.mail.Session object needed to send the email.
        // These are expensive to create so cache them based on the properties
        // they are configured with (using fact that hashcodes are equal only if objects equal):
        Integer propertiesHash = p.hashCode();
        Session s = sessionCache.computeIfAbsent(propertiesHash, k -> {
            log.info(String.format("Creating new mail Session with properties: %s", p));
            return Session.getInstance(p);
        });
        // Create the message and set the recipients:
        Message msg = new MimeMessage(s);

        InternetAddress[] receivers = new InternetAddress[recipient.length];

        for (int i = 0; i < recipient.length; i++) {
            receivers[i] = new InternetAddress(recipient[i]);
        }

		msg.setFrom(fromAddress);
		msg.setRecipients(RecipientType.TO, receivers);
		msg.setSubject(subject);

		if (null != replyTo) {
			msg.setReplyTo(new InternetAddress[] { replyTo });
		}

        return msg;
    }
}