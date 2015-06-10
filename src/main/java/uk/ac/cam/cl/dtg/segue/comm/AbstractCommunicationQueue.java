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

import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.dtg.isaac.configuration.IsaacGuiceConfigurationModule;

/**
 * Abstract message queue class.
 *
 * @author Alistair Stead
 *
 * @param <T>
 *            type of message to send
 */
public abstract class AbstractCommunicationQueue<T extends ICommunicationMessage> {

    private LinkedBlockingQueue<T> messageQueue = new LinkedBlockingQueue<T>();

    private static final Logger log = LoggerFactory.getLogger(IsaacGuiceConfigurationModule.class);

    private ICommunicator<T> communicator;

    private Thread processingThread;

    /**
     * FIFO queue manager that sends messages.
     * 
     * @param communicator
     *            A class to send messages
     */
    public AbstractCommunicationQueue(final ICommunicator<T> communicator) {
        this.communicator = communicator;
        processingThread = new Thread(new MessageSender());
    }

    /**
     * @param queueObject
     *            object of type S that can be added to the queue
     */
    public void addToQueue(final T queueObject) {
        messageQueue.add(queueObject);

        if (!processingThread.isAlive()) {
            // TODO re-awaken it
            processingThread = new Thread(new MessageSender());
            processingThread.start();
        }
    }

    /**
     * @return true if there are messages left on the queue
     */
    public boolean messagesLeftOnQueue() {
        log.info("Message queue size:" + messageQueue.size());
        return messageQueue.size() > 0;
    }

    /**
     * @return an object from the head of the queue
     * @throws InterruptedException
     *             if object cannot be retrieved
     */
    private T getLatestQueueItem() throws InterruptedException {
        return messageQueue.take();
    }

    /**
     * Runnable class that sends the oldest message on the queue.
     *
     * @author Alistair Stead
     *
     */
    class MessageSender implements Runnable {
        @Override
        public void run() {
            while (messagesLeftOnQueue()) {
                // Send the actual message
                try {
                    T item = getLatestQueueItem();
                    communicator.sendMessage(item);
                } catch (InterruptedException e) {
                    log.error("Interrupted Exception:" + e.getMessage());
                } catch (CommunicationException e) {
                    log.error("Communication Exception:" + e.getMessage());
                }
            }
        }
    }
}
