/**
 * Copyright 2015 Stephen Cummins
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
package uk.ac.cam.cl.dtg.segue.api.monitors;

import uk.ac.cam.cl.dtg.segue.api.managers.SegueResourceMisuseException;

/**
 * This interface provides a mechanism for monitoring and imposing limits on resource access.
 */
public interface IMisuseMonitor {

    /**
     * Method to notify the monitor that an event which is protected has been triggered.
     * 
     * @param agentIdentifier
     *            - a unique identifier for the agent using the resource
     * @param eventLabel
     *            - event describing the use of the resource and any threshold criteria.
     * @throws SegueResourceMisuseException
     *             - this only happens when the hard threshold has been reached and indicates possible misuse.
     */
    void notifyEvent(final String agentIdentifier, final String eventLabel) throws SegueResourceMisuseException;

    /**
     * Allows inspection of internal state such that we can give early warning as to whether user has reached threshold
     * before an event notification / exception takes place.
     * 
     * @param agentIdentifier
     *            - unique identifier for user.
     * @param eventToCheck
     *            - the identifier of the event we are interested in.
     * @return true if the user has reached the hard limit defined and they would trigger an exception if they used
     *         notifyEvent.
     */
    boolean hasMisused(String agentIdentifier, String eventToCheck);

    /**
     * Only one handler is allowed per event string and subsequent calls to this method with the same event will replace
     * the handler.
     * 
     * @param eventToHandle
     *            - label for when the handler should be used
     * @param handler
     *            - the handler that should be invoked when an event is seen.
     */
    void registerHandler(String eventToHandle, IMisuseHandler handler);
}
