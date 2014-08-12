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

	@Inject
	public EmailCommunicator(
			@Named(Constants.MAILER_SMTP_SERVER) final String smtpAddress,
			@Named(Constants.MAIL_FROM_ADDRESS) final String mailAddress) {
		Validate.notNull(smtpAddress);
		Validate.notNull(mailAddress);

		this.fromAddress = mailAddress;

		// Construct a new instance of the mailer object
		this.mailer = new Mailer(smtpAddress, mailAddress);
	}

	@Override
	public void sendMessage(final String recipientAddress, final String recipientName, String subject,
	                        final String message) throws CommunicationException {
		subject = String.format("%s - %s", subject, SIGNATURE);

		// Construct message
		StringBuilder messageBuilder = new StringBuilder();
		messageBuilder.append(String.format("Hello %s,\n\n", recipientName));
		messageBuilder.append(message);
		messageBuilder.append(String.format("\n\n%s", SIGNATURE));

		// Send email
		try {
			mailer.sendMail(new String[]{recipientAddress}, this.fromAddress, subject, messageBuilder.toString());
		} catch (MessagingException e) {
			throw new CommunicationException(e);
		}
	}
}
