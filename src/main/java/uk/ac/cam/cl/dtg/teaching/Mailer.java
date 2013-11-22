package uk.ac.cam.cl.dtg.teaching;

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
 * Mailer Class
 * Utility Class for sending e-mails such as contact us forms or notifications
 * 
 * @author sac92
 */
public class Mailer {

	private String smtpAddress;
	private String mailAddress;
	private String smtpPort;

	/**
	 * Mailer Class 
	 * @param smtpAddress The address of the smtp server - this implementation assumes that the port is the default (25)
	 * @param mailAddress The mail address of the sending account - used for authentication sometimes.
	 */
	public Mailer(String smtpAddress, String mailAddress){
		this.smtpAddress = smtpAddress;
		this.mailAddress = mailAddress;	
	}

	/**
	 * SendMail Utility Method
	 * Sends e-mail to a given recipient using the hard-coded MAIL_ADDRESS and SMTP details
	 * @param recipient - string array of recipients that the message should be sent to
	 * @param from - the e-mail address that should be used as the reply-to address (e.g. the true senders address)
	 * @param subject - The message subject
	 * @param contents - The message body
	 * @throws MessagingException
	 * @throws AddressException
	 */
	public void sendMail(String[] recipient, String from, String subject, String contents) throws MessagingException, AddressException {

		Properties p = new Properties();
		p.put("mail.smtp.host", smtpAddress);

		if(null != smtpPort){
			p.put("mail.smtp.port", smtpPort);
		}

		p.put("mail.smtp.starttls.enable", "true");

		Session s = Session.getDefaultInstance(p);
		Message msg = new MimeMessage(s);

		InternetAddress sentBy = null;
		InternetAddress[] sender = new InternetAddress[1];
		InternetAddress[] recievers = new InternetAddress[recipient.length];

		sentBy = new InternetAddress(mailAddress);
		sender[0]  = new InternetAddress(from);
		for(int i=0; i<recipient.length; i++){
			recievers[i] = new InternetAddress(recipient[i]);
		}

		if(sentBy!=null&&sender!=null&&recievers!=null){
			msg.setFrom(sentBy);            
			msg.setReplyTo(sender);
			msg.setRecipients(RecipientType.TO, recievers);
			msg.setSubject(subject);
			msg.setText(contents);

			Transport.send(msg);
		}
	}

	public String getSmtpAddress() {
		return smtpAddress;
	}

	public void setSmtpAddress(String smtpAddress) {
		this.smtpAddress = smtpAddress;
	}

	public String getMailAddress() {
		return mailAddress;
	}

	public void setMailAddress(String mailAddress) {
		this.mailAddress = mailAddress;
	}

	public String getSmtpPort() {
		return smtpPort;
	}

	public void setSmtpPort(String smtpPort) {
		this.smtpPort = smtpPort;
	}
}