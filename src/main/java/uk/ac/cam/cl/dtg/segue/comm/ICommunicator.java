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

/**
 * @author nr378
 */
public interface ICommunicator {

    /**
     * Send a message.
     * 
     * @param recipientAddress
     *            - The address of the recipient, eg. email address or phone number
     * @param recipientName
     *            - The name of the recipient
     * @param subject
     *            - The subject of the message
     * @param message
     *            - The message body
     * @throws CommunicationException
     *             - if a fault occurs whilst sending the communique
     */
    void sendMessage(String recipientAddress, String recipientName, String subject, String message)
            throws CommunicationException;
}
