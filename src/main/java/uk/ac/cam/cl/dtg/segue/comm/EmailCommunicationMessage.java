/**
 * Copyright 2015 Alistair Stead
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

/**
 * Class to hold the contents of an email.
 *
 * @author Alistair Stead
 *
 */
public class EmailCommunicationMessage implements ICommunicationMessage {
	
	private final String message;
	
	private final String recipientAddress;
	
	private final String recipientName;
	
	private final String subject;
	
	
	/**
	 * @return the message
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * @return the recipientAddress
	 */
	public String getRecipientAddress() {
		return recipientAddress;
	}

	/**
	 * @return the recipientName
	 */
	public String getRecipientName() {
		return recipientName;
	}

	/**
	 * @return the subject
	 */
	public String getSubject() {
		return subject;
	}


	/**
	 * 
	 * @param recipientAddress 
	 * 			address of user
	 * @param recipientName
	 * 			user name 
	 * @param subject
	 * 			subject of email
	 * @param message
	 * 			message in email
	 */
	public EmailCommunicationMessage(final String recipientAddress, 
					final String recipientName, final String subject, final String message) {
		this.message = message;
		this.recipientAddress = recipientAddress;
		this.recipientName = recipientName;
		this.subject = subject;
	}

}
