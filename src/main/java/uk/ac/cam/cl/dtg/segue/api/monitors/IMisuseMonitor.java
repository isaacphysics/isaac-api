/**
 * Copyright 2015 Stephen Cummins
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * <p>
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.ac.cam.cl.dtg.segue.api.monitors;

import java.util.List;
import java.util.Map;
import uk.ac.cam.cl.dtg.isaac.dto.MisuseStatisticDTO;
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
  void notifyEvent(String agentIdentifier, String eventLabel) throws SegueResourceMisuseException;

  /**
   * Method to notify the monitor that an event which is protected has been triggered. This method allows for the
   * internal value to be adjusted by an arbitrary amount.
   *
   * @param agentIdentifier
   *            - a unique identifier for the agent using the resource
   * @param eventLabel
   *            - event describing the use of the resource and any threshold criteria.
   * @param adjustmentValue
   *            - Allows the data recorded regarding the quantity of events to be adjusted. This is useful if you wish
   *            use weighted events.
   * @throws SegueResourceMisuseException
   *             - this only happens when the hard threshold has been reached and indicates possible misuse.
   */
  void notifyEvent(String agentIdentifier, String eventLabel, Integer adjustmentValue)
      throws SegueResourceMisuseException;

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
   * Allows inspection of internal state such that we can give early warning as to whether user will have reached threshold
   * before an event notification / exception takes place.
   *
   * @param agentIdentifier
   *            - unique identifier for user.
   * @param eventToCheck
   *            - the identifier of the event we are interested in.
   * @param adjustmentValue
   *            - Weight of the action.
   * @return true if the user will have reached the hard limit defined and they would trigger an exception if they used
   *         notifyEvent with the provided adjustment value.
   */
  boolean willHaveMisused(String agentIdentifier, String eventToCheck, Integer adjustmentValue);

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

  /**
   * Allows us to reset the misuse log for a specific user and event.
   *
   * @param agentIdentifier
   *            - a unique identifier for the agent using the resource
   * @param eventLabel
   *            - event describing the use of the resource and any threshold criteria.
   */
  void resetMisuseCount(String agentIdentifier, String eventLabel);

  /**
   * Allows us to get a summary of the misuse statistics for the site.
   *
   * @param n - the number of entries to return per event label. The "top" n should be returned (those with the highest
   *            misuse counts)
   * @return a map from event label to a list of misuse stats (of length n) for that event
   */
  Map<String, List<MisuseStatisticDTO>> getMisuseStatistics(long n);
}
