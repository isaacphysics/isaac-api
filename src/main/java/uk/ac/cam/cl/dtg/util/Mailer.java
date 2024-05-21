/**
 * Copyright 2014 Stephen Cummins
 * <br>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * <br>
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * <br>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.ac.cam.cl.dtg.util;

import static java.util.Objects.requireNonNull;

import jakarta.activation.DataHandler;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.Message.RecipientType;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.segue.comm.EmailAttachment;

/**
 * Mailer Class Utility Class for sending e-mails such as contact us forms or
 * notifications.
 *
 * @author Stephen Cummins
 */
public class Mailer {
  private static final Logger log = LoggerFactory.getLogger(Mailer.class);
  private final String smtpAddress;
  private final String mailAddress;
  private final String smtpPort;
  private final String smtpUsername;
  private final String smtpPassword;
  private static final ConcurrentMap<Integer, Session> SESSION_CACHE = new ConcurrentHashMap<>();

  /**
   * Mailer Class.
   *
   * @param smtpAddress  The address of the smtp server - this implementation assumes that the port is the default (25)
   * @param mailAddress  The Envelope-From address of the sending account - used for authentication sometimes,
   *                     and will receive bounce emails.
   * @param smtpPort     The SMTP port.
   * @param smtpUsername The SMTP Username.
   * @param smtpPassword The SMTP Password.
   */
  public Mailer(final String smtpAddress,
                final String mailAddress,
                final String smtpPort,
                final String smtpUsername,
                final String smtpPassword) {
    this.smtpAddress = smtpAddress;
    this.mailAddress = mailAddress;
    this.smtpPort = smtpPort;
    this.smtpUsername = smtpUsername;
    this.smtpPassword = smtpPassword;
  }

  /**
   * SendMail Utility Method Sends e-mail to a given recipient using the hard-coded MAIL_ADDRESS and SMTP details.
   *
   * @param emailCommonParameters a Data Object containing the basic parameters for setting up an email message
   * @param contents              The message body
   * @throws MessagingException if we cannot send the message for some reason.
   * @throws AddressException   if the address is not valid.
   */
  public void sendPlainTextMail(final EmailCommonParameters emailCommonParameters, final String contents)
      throws MessagingException {
    Message msg = this.setupMessage(emailCommonParameters);

    msg.setText(contents);

    Transport.send(msg);
  }

  /**
   * Utility method to allow us to send multipart messages using HTML and plain text.
   * <br>
   *
   * @param emailCommonParameters a Data Object containing the basic parameters for setting up an email message
   * @param plainText             The message body
   * @param html                  The message body in html
   * @param attachments           list of attachment objects
   * @throws MessagingException if we cannot send the message for some reason.
   * @throws AddressException   if the address is not valid.
   */
  public void sendMultiPartMail(final EmailCommonParameters emailCommonParameters, final String plainText,
                                final String html,
                                final List<EmailAttachment> attachments) throws MessagingException, AddressException {

    Message msg = this.setupMessage(emailCommonParameters);

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
   * Construct an email Message object using a collection of common variables.
   *
   * @param emailCommonParameters a Data Object containing the basic parameters for setting up an email message
   * @return a newly created message with all of the headers populated.
   * @throws MessagingException if there is an error in setting up the message
   */
  private Message setupMessage(final EmailCommonParameters emailCommonParameters) throws MessagingException {
    Validate.notEmpty(emailCommonParameters.getRecipient());
    Validate.notBlank(emailCommonParameters.getRecipient()[0]);
    requireNonNull(emailCommonParameters.getFromAddress());

    Properties p = new Properties();

    // Configure the SMTP server settings:
    p.put("mail.smtp.host", smtpAddress);
    p.put("mail.smtp.starttls.enable", "true");

    // Configure the email headers and routing:
    String envelopeFrom = mailAddress;
    if (null != emailCommonParameters.getOverrideEnvelopeFrom()) {
      envelopeFrom = emailCommonParameters.getOverrideEnvelopeFrom();
    }
    p.put("mail.smtp.from", envelopeFrom);  // Used for Return-Path
    p.put("mail.from", emailCommonParameters.getFromAddress()
        .getAddress()); // Should only affect Message-ID, since From overridden below
    p.put("mail.smtp.port", smtpPort);
    p.put("mail.smtp.auth", "true");
    // Create the jakarta.mail.Session object needed to send the email.
    // These are expensive to create so cache them based on the properties
    // they are configured with (using fact that hashcodes are equal only if objects equal):
    Integer propertiesHash = p.hashCode();
    Session s = SESSION_CACHE.computeIfAbsent(propertiesHash, k -> {
      log.info(String.format("Creating new mail Session with properties: %s", p));
      return Session.getInstance(p, new Authenticator() {
        @Override
        protected PasswordAuthentication getPasswordAuthentication() {
          return new PasswordAuthentication(smtpUsername, smtpPassword);
        }
      });
    });
    // Create the message and set the recipients:
    Message msg = new MimeMessage(s);

    InternetAddress[] receivers = new InternetAddress[emailCommonParameters.getRecipient().length];

    for (int i = 0; i < emailCommonParameters.getRecipient().length; i++) {
      receivers[i] = new InternetAddress(emailCommonParameters.getRecipient()[i]);
    }

    msg.setFrom(emailCommonParameters.getFromAddress());
    msg.setRecipients(RecipientType.TO, receivers);
    msg.setSubject(emailCommonParameters.getSubject());

    if (null != emailCommonParameters.getReplyTo()) {
      msg.setReplyTo(new InternetAddress[] {emailCommonParameters.getReplyTo()});
    }

    return msg;
  }
}
