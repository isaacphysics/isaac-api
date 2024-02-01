/*
 * Copyright 2017 Dan Underwood
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

package uk.ac.cam.cl.dtg.segue.dao;

import uk.ac.cam.cl.dtg.isaac.dto.users.AbstractSegueUserDTO;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.HashSet;

import static uk.ac.cam.cl.dtg.segue.api.Constants.*;

/**
 *  Abstract class for publishing logged events to interested listeners.
 *  Uses decorator pattern.
 *
 *  @author Dan Underwood
 */
public abstract class LogManagerEventPublisher implements ILogManager {

    private final ILogManager logManager;
    private final Collection<LoggingEventHandler> logListeners;

    public LogManagerEventPublisher(final ILogManager logManager) {
        this.logManager = logManager;
        this.logListeners = new HashSet<>();
    }


    /**
     * Add listener object to collection of listeners that wish to subscribe to events raised.
     *
     * @param listener
     *            - the listener who wants to subscribe to raised events
     */
    public void addListener(final LoggingEventHandler listener) {
        this.logListeners.add(listener);
    }

    /*Method Overrides*/

    @Override
    public void logEvent(final AbstractSegueUserDTO user, final HttpServletRequest httpRequest, final LogType eventType, final Object eventDetails) {

        this.logManager.logEvent(user, httpRequest, eventType, eventDetails);

        for (LoggingEventHandler listener : logListeners) {
            listener.handleEvent(user, httpRequest, eventType.name(), eventDetails);
        }

    }

    @Override
    public void logExternalEvent(final AbstractSegueUserDTO user, final HttpServletRequest httpRequest, final String eventType, final Object eventDetails) {

        this.logManager.logExternalEvent(user, httpRequest, eventType, eventDetails);

        for (LoggingEventHandler listener : logListeners) {
            listener.handleEvent(user, httpRequest, eventType, eventDetails);
        }

    }

    @Override
    public void logInternalEvent(final AbstractSegueUserDTO user, final LogType eventType, final Object eventDetails) {

        this.logManager.logInternalEvent(user, eventType, eventDetails);

        for (LoggingEventHandler listener : logListeners) {
            listener.handleEvent(user, null, eventType.name(), eventDetails);
        }

    }

    @Override
    public void transferLogEventsToRegisteredUser(final String oldUserId, final String newUserId) {

        this.logManager.transferLogEventsToRegisteredUser(oldUserId, newUserId);

        for (LoggingEventHandler listener : logListeners) {
            listener.transferLogEventsToRegisteredUser(oldUserId, newUserId);
        }

    }

    @Override
    public Long getLogCountByType(final String type) throws SegueDatabaseException {

        return this.logManager.getLogCountByType(type);

    }
}