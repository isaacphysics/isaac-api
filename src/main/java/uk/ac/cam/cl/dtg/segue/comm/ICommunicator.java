/*
 * Copyright 2014 Nick Rogers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.cam.cl.dtg.segue.comm;

/**
 * 
 * @author nr378
 *
 * @param <T>
 *            the type of object it communicates
 */
public interface ICommunicator<T extends ICommunicationMessage> {

    /**
     * Send a message.
     * 
     * @param message - to send
     * @throws CommunicationException - if there is a failure in sending the message
     */
    void sendMessage(final T message) throws CommunicationException;

}
