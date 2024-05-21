/**
 * Copyright 2017 Dan Underwood
 * <br>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * <br>
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * <br>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.ac.cam.cl.dtg.segue.dao;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import uk.ac.cam.cl.dtg.isaac.dos.LogEvent;
import uk.ac.cam.cl.dtg.isaac.dto.users.AbstractSegueUserDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.segue.api.Constants.LogType;

/**
 * Abstract class for publishing logged events to interested listeners.
 * Uses decorator pattern.
 *
 * @author Dan Underwood
 */
public abstract class LogManagerEventPublisher implements ILogManager {

  private ILogManager logManager;
  private Collection<LoggingEventHandler> logListeners;

  public LogManagerEventPublisher(final ILogManager logManager) {
    this.logManager = logManager;
  }


  /**
   * Add listener object to collection of listeners that wish to subscribe to events raised.
   *
   * @param listener the listener who wants to subscribe to raised events
   */
  public void addListener(final LoggingEventHandler listener) {

    if (null == logListeners) {
      logListeners = new HashSet<>();
    }

    this.logListeners.add(listener);
  }


  /**
   * Method Overrides.
   */

  @Override
  public void logEvent(final AbstractSegueUserDTO user, final HttpServletRequest httpRequest, final LogType eventType,
                       final Object eventDetails) {

    this.logManager.logEvent(user, httpRequest, eventType, eventDetails);

    if (null != logListeners) {

      for (LoggingEventHandler listener : logListeners) {
        listener.handleEvent(user, httpRequest, eventType.name(), eventDetails);
      }

    }
  }

  @Override
  public void logExternalEvent(final AbstractSegueUserDTO user, final HttpServletRequest httpRequest,
                               final String eventType, final Object eventDetails) {

    this.logManager.logExternalEvent(user, httpRequest, eventType, eventDetails);

    if (null != logListeners) {

      for (LoggingEventHandler listener : logListeners) {
        listener.handleEvent(user, httpRequest, eventType, eventDetails);
      }

    }
  }

  @Override
  public void logInternalEvent(final AbstractSegueUserDTO user, final LogType eventType, final Object eventDetails) {

    this.logManager.logInternalEvent(user, eventType, eventDetails);

    if (null != logListeners) {

      for (LoggingEventHandler listener : logListeners) {
        listener.handleEvent(user, null, eventType.name(), eventDetails);
      }

    }

  }

  @Override
  public void transferLogEventsToRegisteredUser(final String oldUserId, final String newUserId) {

    this.logManager.transferLogEventsToRegisteredUser(oldUserId, newUserId);

    if (null != logListeners) {

      for (LoggingEventHandler listener : logListeners) {
        listener.transferLogEventsToRegisteredUser(oldUserId, newUserId);
      }

    }

  }

  @Override
  public Collection<LogEvent> getLogsByType(final String type, final Instant fromDate, final Instant toDate)
      throws SegueDatabaseException {

    return this.logManager.getLogsByType(type, fromDate, toDate);
  }

  @Override
  public Collection<LogEvent> getLogsByType(final String type, final Instant fromDate, final Instant toDate,
                                            final List<RegisteredUserDTO> usersOfInterest)
      throws SegueDatabaseException {

    return this.logManager.getLogsByType(type, fromDate, toDate, usersOfInterest);

  }

  @Override
  public Long getLogCountByType(final String type) throws SegueDatabaseException {

    return this.logManager.getLogCountByType(type);

  }

  @Override
  public Map<String, Map<LocalDate, Long>> getLogCountByDate(
      final Collection<String> eventTypes, final Instant fromDate, final Instant toDate,
      final List<RegisteredUserDTO> usersOfInterest, final boolean binDataByMonth) throws SegueDatabaseException {

    return this.logManager.getLogCountByDate(eventTypes, fromDate, toDate, usersOfInterest, binDataByMonth);

  }

  @Override
  public Set<String> getAllIpAddresses() {

    return this.logManager.getAllIpAddresses();

  }

  @Override
  public Map<String, Instant> getLastLogDateForAllUsers(final String qualifyingLogEventType)
      throws SegueDatabaseException {

    return this.logManager.getLastLogDateForAllUsers(qualifyingLogEventType);

  }

  @Override
  public Set<String> getAllEventTypes() throws SegueDatabaseException {

    return this.logManager.getAllEventTypes();

  }
}