package uk.ac.cam.cl.dtg.util;

import jakarta.annotation.Nullable;
import jakarta.mail.internet.InternetAddress;

public class EmailCommonParameters {
  private final String[] recipient;
  private final InternetAddress fromAddress;
  @Nullable
  private final String overrideEnvelopeFrom;
  @Nullable
  private final InternetAddress replyTo;
  private final String subject;

  /**
   * A Data Object containing the basic parameters for setting up an email message.
   *
   * @param recipient            - string array of recipients that the message should be sent to
   * @param fromAddress          - the e-mail address that should be used as the sending address
   * @param overrideEnvelopeFrom - (nullable) the e-mail address that should be used as the envelope from address, useful for routing
   * @param replyTo              - (nullable) the e-mail address that should be used as the reply-to address
   * @param subject              - The message subject
   */
  public EmailCommonParameters(final String[] recipient, final InternetAddress fromAddress,
                               @Nullable final String overrideEnvelopeFrom,
                               final @Nullable InternetAddress replyTo, final String subject) {
    this.recipient = recipient;
    this.fromAddress = fromAddress;
    this.overrideEnvelopeFrom = overrideEnvelopeFrom;
    this.replyTo = replyTo;
    this.subject = subject;
  }

  public String[] getRecipient() {
    return recipient;
  }

  public InternetAddress getFromAddress() {
    return fromAddress;
  }

  public String getOverrideEnvelopeFrom() {
    return overrideEnvelopeFrom;
  }

  public InternetAddress getReplyTo() {
    return replyTo;
  }

  public String getSubject() {
    return subject;
  }
}
