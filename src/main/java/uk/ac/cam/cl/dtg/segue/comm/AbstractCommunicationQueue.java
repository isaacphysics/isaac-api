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

import java.util.Comparator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.segue.dao.ILogManager;


/**
 * Abstract message queue class.
 *
 * @author Alistair Stead
 *
 * @param <T>
 *            type of message to send
 */
public abstract class AbstractCommunicationQueue<T extends ICommunicationMessage> {
	
    private final ILogManager logManager;
	
	/**
	 * Comparator that tells the priority queue which email should be sent first.
	 */
	private Comparator<ICommunicationMessage> emailPriorityComparator = new Comparator<ICommunicationMessage>() {
		@Override
		public int compare(final ICommunicationMessage first, final ICommunicationMessage second) {
			if (first.getPriority() == second.getPriority()) {
				return 0;
			} else if (first.getPriority() <= second.getPriority()) {
				return -1;
			} else {
				return 1;
			}
		}
    	
    };

    private PriorityBlockingQueue<T> messageSenderRunnableQueue = 
    				new PriorityBlockingQueue<T>(100, emailPriorityComparator);

    private static final Logger log = LoggerFactory.getLogger(AbstractCommunicationQueue.class);

    private ICommunicator<T> communicator;

    private final ExecutorService executorService;
    

    /**
     * FIFO queue manager that sends messages.
     * 
     * @param communicator
     *            A class to send messages
     * @param logManager
     *            the log manager
     */
    public AbstractCommunicationQueue(final ICommunicator<T> communicator, 
    	    final ILogManager logManager) {
        this.communicator = communicator;
        this.logManager = logManager;
        this.executorService = Executors.newFixedThreadPool(2); 
    }

    /**
     * @param queueObject
     *            object of type S that can be added to the queue
     */
    protected void addToQueue(final T queueObject) {
    	messageSenderRunnableQueue.add(queueObject);
    	executorService.submit(new MessageSenderRunnable());
    	log.info("Added to the email queue. Current size: " + messageSenderRunnableQueue.size());
    }


    /**
     * @return an object from the head of the queue
     * @throws InterruptedException
     *             if object cannot be retrieved
     */
    private T getLatestQueueItem() throws InterruptedException {
        return messageSenderRunnableQueue.poll();
    }
    
    /**
     * @return current queue length
     */
    public int getQueueLength() {
    	return messageSenderRunnableQueue.size();
    }

    /**
     * Runnable class that sends the oldest message on the queue.
     *
     * @author Alistair Stead
     *
     */
    class MessageSenderRunnable implements Runnable {
    	    	
        @Override
        public void run() {
            // Send the actual message
            try {
            	T queueItem = getLatestQueueItem();
            	log.warn("About to send message ");
                communicator.sendMessage(queueItem);
            } catch (CommunicationException e) {
                log.warn("Communication Exception:" + e.getMessage());
            } catch (InterruptedException e) {
                log.warn("Interrupted Exception:" + e.getMessage());
			} catch (Exception e) {
                log.warn("Generic Exception:" + e.getMessage());
                e.printStackTrace();
			}
        }
    }    
}