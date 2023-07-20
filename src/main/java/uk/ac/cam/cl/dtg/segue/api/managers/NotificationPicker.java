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
package uk.ac.cam.cl.dtg.segue.api.managers;

import com.google.api.client.util.Lists;
import com.google.api.client.util.Maps;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.api.Constants.BooleanOperator;
import uk.ac.cam.cl.dtg.segue.dao.ResourceNotFoundException;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dao.content.GitContentManager;
import uk.ac.cam.cl.dtg.isaac.dos.IUserNotification;
import uk.ac.cam.cl.dtg.isaac.dos.IUserNotification.NotificationStatus;
import uk.ac.cam.cl.dtg.isaac.dos.IUserNotifications;
import uk.ac.cam.cl.dtg.isaac.dos.PgUserNotifications;
import uk.ac.cam.cl.dtg.isaac.dto.ResultsWrapper;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.NotificationDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.RegisteredUserDTO;

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static uk.ac.cam.cl.dtg.segue.api.Constants.CONTENT_INDEX;
import static uk.ac.cam.cl.dtg.segue.api.Constants.TYPE_FIELDNAME;

/**
 * This class is responsible for selecting notifications from various sources so that users can be told about them.
 *
 */
public class NotificationPicker {
    private IUserNotifications notifications;
    private final GitContentManager contentManager;
    private final String contentIndex;

    /**
     * @param contentManager
     *            - so we can lookup notifications created in the segue content system.
     * @param notifications
     *            - the DAO allowing the recording of which notifications have been shown to whom.
     */
    @Inject
    public NotificationPicker(final GitContentManager contentManager, @Named(CONTENT_INDEX) final String contentIndex,
                              final PgUserNotifications notifications) {
        this.contentManager = contentManager;
        this.contentIndex = contentIndex;
        this.notifications = notifications;
    }

    /**
     * getAvailableNotificationsForUser.
     * 
     * @param user
     *            to select notifications for.
     * @return the list of content to show to the user.
     * @throws ContentManagerException
     *             - if something goes wrong looking up the content.
     * @throws SegueDatabaseException
     *             - if something goes wrong consulting the personalisation database.
     */
    public List<ContentDTO> getAvailableNotificationsForUser(final RegisteredUserDTO user)
            throws ContentManagerException, SegueDatabaseException {
        // get users notification record
        List<GitContentManager.BooleanSearchClause> fieldsToMatch = Lists.newArrayList();
        fieldsToMatch.add(new GitContentManager.BooleanSearchClause(
                TYPE_FIELDNAME, BooleanOperator.AND, Collections.singletonList("notification")));

        ResultsWrapper<ContentDTO> allContentNotifications = this.contentManager
                .findByFieldNames(fieldsToMatch, 0, -1);

        Map<String, IUserNotification> listOfRecordedNotifications = getMapOfRecordedNotifications(user);

        List<ContentDTO> resultsToReturn = Lists.newArrayList();

        for (ContentDTO c : allContentNotifications.getResults()) {
        	IUserNotification record = listOfRecordedNotifications.get(c.getId());
            if (!(c instanceof NotificationDTO)) {
                // skip if not a notification somehow.
                continue;
            }
            
            NotificationDTO notification = (NotificationDTO) c;
            if (notification.getExpiry() != null && new Date().after(notification.getExpiry())) {
                // skip expired notifications
                continue;
            }

            String roleTag = user.getRole().name().toLowerCase();
            if (null == c.getTags() || !c.getTags().contains(roleTag)) {
                // Skip irrelevant notifications
                continue;
            }

            if (null == record) {
                // either the use hasn't responded to the notification before...
                resultsToReturn.add(c);
            } else if (record.getStatus().equals(NotificationStatus.POSTPONED)) {
                // or they have but they postponed it...
                Calendar postPoneExpiry = Calendar.getInstance();
                postPoneExpiry.setTime(record.getCreated());
                postPoneExpiry.add(Calendar.SECOND, Constants.NUMBER_SECONDS_IN_ONE_DAY);

                if (new Date().after(postPoneExpiry.getTime())) {
                    resultsToReturn.add(c);
                }
            } else {
                // or they have and they don't want to see it again
                continue;
            }

        }

        return resultsToReturn;
    }

    /**
     * getListOfRecordedNotifications.
     * 
     * @param user
     *            - to lookup the notification history for.
     * @return a map of NotificationId --> UserNotificationRecord.
     * @throws SegueDatabaseException
     *             - if something goes wrong with the DB io step.
     */
    public Map<String, IUserNotification> getMapOfRecordedNotifications(final RegisteredUserDTO user)
            throws SegueDatabaseException {
        Map<String, IUserNotification> result = Maps.newHashMap();

        List<IUserNotification> userNotifications = notifications.getUserNotifications(user.getId());

        for (IUserNotification recordedNotification : userNotifications) {
            result.put(recordedNotification.getContentNotificationId(), recordedNotification);
        }

        return result;
    }

    /**
     * Allows notifications to be dismissed on a per user basis.
     * 
     * @param user
     *            - that the notification pertains to.
     * @param notificationId
     *            - the id of the notification
     * @param status
     *            - the status of the notification e.g. dismissed, postponed, disabled
     * @throws SegueDatabaseException
     *             - if something goes wrong with the DB io step.
     * @throws ContentManagerException
     *             - if something goes wrong looking up the content.
     */
    public void recordNotificationAction(final RegisteredUserDTO user, final String notificationId,
            final NotificationStatus status) throws SegueDatabaseException, ContentManagerException {
        ContentDTO notification = this.contentManager.getContentById(notificationId);

        if (null == notification) {
            throw new ResourceNotFoundException(String.format(
                    "The resource with id: %s and type Notification could not be found.", notificationId));
        }

        // update the users record with the action they have taken.
        notifications.saveUserNotification(user.getId(), notificationId, status);
    }

    /**
     * getNotificationById.
     * @param notificationId - the id of the notification.
     * @return get the notification content dto.
     * @throws ResourceNotFoundException
     *             - if we can't find the item of interest.
     * @throws ContentManagerException
     *             - if something goes wrong looking up the content.
     */
    public ContentDTO getNotificationById(final String notificationId) throws ContentManagerException,
            ResourceNotFoundException {
        // get available notifications that still can be displayed
        ContentDTO notification = this.contentManager.getContentById(notificationId);

        if (notification instanceof NotificationDTO) {
            return notification;
        } else {
            throw new ResourceNotFoundException(String.format(
                    "The resource with id: %s and type Notification could not be found.", notificationId));
        }
    }
}
