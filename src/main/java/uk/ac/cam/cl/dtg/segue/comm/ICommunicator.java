package uk.ac.cam.cl.dtg.segue.comm;

/**
 * @author nr378
 */
public interface ICommunicator {
	/**
	 * Send a message.
	 *
	 * @param recipientAddress - The address of the recipient, eg. email address or phone number
	 * @param recipientName - The name of the recipient
	 * @param subject - The subject of the message
	 * @param message - The message body
	 * @throws CommunicationException - if a fault occurs whilst sending the communique
	 */
	public void sendMessage(String recipientAddress, String recipientName, String subject,
	                        String message) throws CommunicationException;
}
