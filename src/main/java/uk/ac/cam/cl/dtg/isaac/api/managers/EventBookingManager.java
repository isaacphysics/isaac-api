/**
 * Copyright 2015 Stephen Cummins
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

package uk.ac.cam.cl.dtg.isaac.api.managers;

import static uk.ac.cam.cl.dtg.isaac.api.Constants.EMAIL_TEMPLATE_ID_EVENT_BOOKING_CONFIRMED;
import static uk.ac.cam.cl.dtg.isaac.api.Constants.EMAIL_TEMPLATE_ID_WAITING_LIST_ADDITION;
import static uk.ac.cam.cl.dtg.isaac.api.Constants.EMAIL_TEMPLATE_ID_WAITING_LIST_ONLY_ADDITION;
import static uk.ac.cam.cl.dtg.isaac.api.Constants.EMAIL_TEMPLATE_TOKEN_AUTHORIZATION_LINK;
import static uk.ac.cam.cl.dtg.isaac.api.Constants.EMAIL_TEMPLATE_TOKEN_CONTACT_US_URL;
import static uk.ac.cam.cl.dtg.isaac.api.Constants.EMAIL_TEMPLATE_TOKEN_EVENT;
import static uk.ac.cam.cl.dtg.isaac.api.Constants.EMAIL_TEMPLATE_TOKEN_EVENT_DETAILS;
import static uk.ac.cam.cl.dtg.isaac.api.Constants.EMAIL_TEMPLATE_TOKEN_EVENT_URL;
import static uk.ac.cam.cl.dtg.isaac.api.Constants.EXCEPTION_MESSAGE_TEMPLATE_CANCELLED_EVENT;
import static uk.ac.cam.cl.dtg.isaac.api.Constants.EXCEPTION_MESSAGE_TEMPLATE_DUPLICATE_BOOKING;
import static uk.ac.cam.cl.dtg.isaac.api.Constants.EXCEPTION_MESSAGE_TEMPLATE_UNABLE_TO_SEND_EMAIL;
import static uk.ac.cam.cl.dtg.segue.api.Constants.DEFAULT_TIME_LOCALITY;
import static uk.ac.cam.cl.dtg.segue.api.Constants.EVENT_ADMIN_EMAIL;
import static uk.ac.cam.cl.dtg.segue.api.Constants.EVENT_ICAL_UID_DOMAIN;
import static uk.ac.cam.cl.dtg.segue.api.Constants.EVENT_RESERVATION_CLOSE_INTERVAL_DAYS;
import static uk.ac.cam.cl.dtg.segue.api.Constants.HOST_NAME;
import static uk.ac.cam.cl.dtg.segue.api.Constants.MAIL_NAME;
import static uk.ac.cam.cl.dtg.util.NameFormatter.getTeacherNameFromUser;

import biweekly.Biweekly;
import biweekly.ICalendar;
import biweekly.component.VEvent;
import biweekly.io.TimezoneAssignment;
import biweekly.property.Organizer;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TimeZone;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.dao.EventBookingPersistenceManager;
import uk.ac.cam.cl.dtg.isaac.dos.AssociationToken;
import uk.ac.cam.cl.dtg.isaac.dos.EventStatus;
import uk.ac.cam.cl.dtg.isaac.dos.ITransaction;
import uk.ac.cam.cl.dtg.isaac.dos.eventbookings.BookingStatus;
import uk.ac.cam.cl.dtg.isaac.dos.users.EmailVerificationStatus;
import uk.ac.cam.cl.dtg.isaac.dos.users.Role;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacEventPageDTO;
import uk.ac.cam.cl.dtg.isaac.dto.UserGroupDTO;
import uk.ac.cam.cl.dtg.isaac.dto.eventbookings.DetailedEventBookingDTO;
import uk.ac.cam.cl.dtg.isaac.dto.eventbookings.EventBookingDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.UserSummaryDTO;
import uk.ac.cam.cl.dtg.segue.api.managers.GroupManager;
import uk.ac.cam.cl.dtg.segue.api.managers.ITransactionManager;
import uk.ac.cam.cl.dtg.segue.api.managers.IUserAccountManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAssociationManager;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserException;
import uk.ac.cam.cl.dtg.segue.comm.EmailAttachment;
import uk.ac.cam.cl.dtg.segue.comm.EmailManager;
import uk.ac.cam.cl.dtg.segue.comm.EmailMustBeVerifiedException;
import uk.ac.cam.cl.dtg.segue.comm.EmailType;
import uk.ac.cam.cl.dtg.segue.dao.ResourceNotFoundException;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.associations.InvalidUserAssociationTokenException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

/**
 * EventBookingManager.
 * This class is responsible for controlling event bookings throughout the platform.
 */
public class EventBookingManager {
  private static final Logger log = LoggerFactory.getLogger(EventBookingManager.class);

  private static final String AUTH_TOKEN_LINK = "https://%s/account?authToken=%s";
  private static final String EVENT_STAGE_STUDENT = "student";

  private final EventBookingPersistenceManager bookingPersistenceManager;
  private final EmailManager emailManager;
  private final UserAssociationManager userAssociationManager;
  private final PropertiesLoader propertiesLoader;
  private final GroupManager groupManager;
  private final IUserAccountManager userAccountManager;
  private final ITransactionManager transactionManager;

  /**
   * EventBookingManager.
   *
   * @param bookingPersistenceManager - to allow bookings to be persisted in the database
   * @param emailManager              - email manager
   * @param userAssociationManager    - the userAssociationManager manager object
   * @param propertiesLoader          - Instance of properties Loader
   * @param groupManager              - Instance of Group Manager
   * @param userAccountManager        - Instance of User Account Manager, for retrieving users
   * @param transactionManager        - Instance of Transaction Manager, used for locking database while managing
   *                                        bookings
   */
  @Inject
  public EventBookingManager(final EventBookingPersistenceManager bookingPersistenceManager,
                             final EmailManager emailManager,
                             final UserAssociationManager userAssociationManager,
                             final PropertiesLoader propertiesLoader,
                             final GroupManager groupManager,
                             final IUserAccountManager userAccountManager,
                             final ITransactionManager transactionManager) {
    this.bookingPersistenceManager = bookingPersistenceManager;
    this.emailManager = emailManager;
    this.userAssociationManager = userAssociationManager;
    this.propertiesLoader = propertiesLoader;
    this.groupManager = groupManager;
    this.userAccountManager = userAccountManager;
    this.transactionManager = transactionManager;
  }

  /**
   * This will get all bookings for a given user.
   *
   * @param userId - user of interest.
   * @return events
   * @throws SegueDatabaseException - if an error occurs.
   */
  public Map<String, BookingStatus> getAllEventStatesForUser(final Long userId) throws SegueDatabaseException {
    final ImmutableMap.Builder<String, BookingStatus> bookingStatusBuilder = new ImmutableMap.Builder<>();

    for (EventBookingDTO booking : this.bookingPersistenceManager.getEventsByUserId(userId)) {
      bookingStatusBuilder.put(booking.getEventId(), booking.getBookingStatus());
    }

    return bookingStatusBuilder.build();
  }

  /**
   * This will get all reservations made by a given user.
   *
   * @param userId - user of interest.
   * @return events
   * @throws SegueDatabaseException - if an error occurs.
   */
  public List<DetailedEventBookingDTO> getAllEventReservationsForUser(final Long userId) throws SegueDatabaseException {
    return this.bookingPersistenceManager.getEventReservationsByUserId(userId);
  }

  /**
   * @param bookingId - of interest
   * @return event booking
   * @throws SegueDatabaseException - if an error occurs.
   */
  public DetailedEventBookingDTO getDetailedBookingById(final Long bookingId) throws SegueDatabaseException {
    return this.bookingPersistenceManager.getDetailedBookingById(bookingId);
  }

  /**
   * Count all bookings in the database.
   *
   * @return event bookings
   * @throws SegueDatabaseException - if an error occurs.
   */
  public Long getCountOfEventBookings() throws SegueDatabaseException {
    return this.bookingPersistenceManager.countAllBookings();
  }

  /**
   * @param eventId - of interest
   * @return event bookings
   * @throws SegueDatabaseException - if an error occurs.
   */
  public List<DetailedEventBookingDTO> getBookingsByEventId(final String eventId) throws SegueDatabaseException {
    return this.bookingPersistenceManager.getBookingsByEventId(eventId);
  }

  public EventBookingDTO getBookingByEventIdAndUserId(final String eventId, final Long userId)
      throws SegueDatabaseException {
    return this.bookingPersistenceManager.getBookingByEventIdAndUserId(eventId, userId);
  }

  /**
   * @param eventId - of interest
   * @return event bookings
   * @throws SegueDatabaseException - if an error occurs.
   */
  public List<DetailedEventBookingDTO> adminGetBookingsByEventId(final String eventId) throws SegueDatabaseException {
    return this.bookingPersistenceManager.adminGetBookingsByEventId(eventId);
  }

  public Map<String, List<DetailedEventBookingDTO>> adminGetBookingsByEventIds(final List<String> eventIds)
      throws SegueDatabaseException {
    Map<String, List<DetailedEventBookingDTO>> result = new HashMap<>();

    for (String eventId : eventIds) {
      result.put(eventId, adminGetBookingsByEventId(eventId));
    }

    return result;
  }


  /**
   * Ensure an event allows group bookings.
   *
   * @param event The ID of the event
   * @return whether group bookings are allowed
   */
  public static boolean eventAllowsGroupBookings(final IsaacEventPageDTO event) {
    return event.getAllowGroupReservations() != null && event.getAllowGroupReservations();
  }

  /**
   * Check if the registered user is able to manage the given event.
   * Event managers and admins can manage all events where as event leaders can only manage events for which they are
   * managers of the event's associated group.
   *
   * @param user  wishing to manage the event.
   * @param event to be managed.
   * @return either true or false if the user is able to manage the event.
   * @throws SegueDatabaseException if there is a problem with the database while retrieving associations or groups.
   */
  public boolean isUserAbleToManageEvent(final RegisteredUserDTO user, final IsaacEventPageDTO event)
      throws SegueDatabaseException {
    if (Arrays.asList(Role.EVENT_MANAGER, Role.ADMIN).contains(user.getRole())) {
      return true;
    }
    if (Role.EVENT_LEADER.equals(user.getRole())) {
      try {
        String eventGroupTokenString = event.getIsaacGroupToken();
        if (eventGroupTokenString == null || eventGroupTokenString.isEmpty()) {
          return false;
        }
        AssociationToken eventGroupToken = userAssociationManager.lookupTokenDetails(user, eventGroupTokenString);
        UserGroupDTO eventGroup = groupManager.getGroupById(eventGroupToken.getGroupId());
        if (GroupManager.isOwnerOrAdditionalManager(eventGroup, user.getId())) {
          return true;
        }
      } catch (InvalidUserAssociationTokenException e) {
        log.error("Event {} has an invalid user association token - ignoring", event.getId());
      } catch (ResourceNotFoundException ignored) {
        // The group for this event has been deleted or does not exist. Since deletion is common, fail silently.
      }
    }
    return false;
  }

  /**
   * Check if the requesting/logged-in user is the one who made the reservation.
   *
   * @param user                  The logged in user
   * @param userOwningReservation The reserved user
   * @param event                 The event on which the reserved user is reserved
   * @return either true or false whether the requesting user made the reservation
   * @throws SegueDatabaseException if there is a problem retrieving the event and booking info
   */
  public boolean isReservationMadeByRequestingUser(final RegisteredUserDTO user,
                                                   final RegisteredUserDTO userOwningReservation,
                                                   final IsaacEventPageDTO event)
      throws SegueDatabaseException {

    EventBookingDTO booking = this.getBookingByEventIdAndUserId(event.getId(), userOwningReservation.getId());
    Long reservingUserId = booking.getReservedById();
    return user.getId().equals(reservingUserId);
  }

  /**
   * Create booking on behalf of a user.
   * This method will allow users to be booked onto an event providing there is space. If there is no space the user
   * will simply be added to the waiting list.
   *
   * @param event                      - of interest
   * @param user                       - user to book on to the event.
   * @param additionalEventInformation - any additional information for the event organisers (nullable)
   * @return the newly created booking.
   * @throws SegueDatabaseException    - if an error occurs.
   * @throws DuplicateBookingException - Duplicate booking, only unique bookings.
   */
  public EventBookingDTO createBookingOrAddToWaitingList(final IsaacEventPageDTO event, final RegisteredUserDTO user,
                                                         final Map<String, String> additionalEventInformation)
      throws SegueDatabaseException, DuplicateBookingException, EventIsCancelledException {
    // check if already booked
    if (this.isUserBooked(event.getId(), user.getId())) {
      throw new DuplicateBookingException(
          String.format(EXCEPTION_MESSAGE_TEMPLATE_DUPLICATE_BOOKING, event.getId(), user.getEmail()));
    }

    // if an event admin wants to add a user to a waiting list only event they will need to promote them afterwards.
    EventBookingDTO booking;
    if (EventStatus.WAITING_LIST_ONLY.equals(event.getEventStatus())) {
      try {
        booking = this.createBooking(event, user, additionalEventInformation, BookingStatus.WAITING_LIST);
      } catch (EventIsFullException e1) {
        throw new RuntimeException("Creating a waiting list booking should never throw an event is full exception "
            + "- something went terribly wrong for this to have happened", e1);
      }
      return booking;
    }

    // attempt to create a confirmed booking for the user.
    try {
      booking = this.createBooking(event, user, additionalEventInformation, BookingStatus.CONFIRMED);

    } catch (EventIsFullException e) {
      // book the user on the waiting list instead as the event is full
      try {
        booking = this.createBooking(event, user, additionalEventInformation, BookingStatus.WAITING_LIST);
      } catch (EventIsFullException e1) {
        throw new RuntimeException("Creating a waiting list booking should never throw an event is full exception "
            + "- something went terribly wrong for this to have happened", e1);
      }
    }

    return booking;
  }

  /**
   * Create booking on behalf of a user.
   * This method will allow users to be booked onto an event providing there is space. No other rules are applied.
   * This is likely to be only for admin users.
   * <br>
   * This method will not enforce some of the restrictions such as event deadlines and email verification
   *
   * @param event                      - of interest
   * @param user                       - user to book on to the event.
   * @param additionalEventInformation - any additional information for the event organisers (nullable)
   * @param status                     - the booking status to create i.e. CONFIRMED, WAITING_LIST etc.
   * @return the newly created booking.
   * @throws SegueDatabaseException    - if an error occurs.
   * @throws EventIsFullException      - No space on the event
   * @throws DuplicateBookingException - Duplicate booking, only unique bookings.
   */
  public EventBookingDTO createBooking(final IsaacEventPageDTO event, final RegisteredUserDTO user,
                                       final Map<String, String> additionalEventInformation,
                                       final BookingStatus status)
      throws SegueDatabaseException, DuplicateBookingException, EventIsFullException, EventIsCancelledException {
    // Check if event is cancelled
    if (EventStatus.CANCELLED.equals(event.getEventStatus())) {
      throw new EventIsCancelledException(
          String.format(EXCEPTION_MESSAGE_TEMPLATE_CANCELLED_EVENT, user.getId(),
              event.getId()));
    }

    // check if already booked
    if (this.isUserBooked(event.getId(), user.getId())) {
      throw new DuplicateBookingException(
          String.format(EXCEPTION_MESSAGE_TEMPLATE_DUPLICATE_BOOKING, event.getId(), user.getEmail()));
    }

    EventBookingDTO booking;
    try (ITransaction transaction = transactionManager.getTransaction()) {
      // Obtain an exclusive database lock to lock the booking
      this.bookingPersistenceManager.lockEventUntilTransactionComplete(transaction, event.getId());

      if (BookingStatus.CONFIRMED.equals(status)) {
        this.ensureCapacity(event, user);
      }

      booking = this.bookingPersistenceManager.createBooking(transaction, event.getId(), user.getId(), status,
          additionalEventInformation);
      transaction.commit();
    }

    addUserToEventGroup(event, user);

    try {
      // Send an email notifying the user (unless they are being added after the event for the sake of our records)
      Date bookingDate = new Date();
      if (event.getEndDate() == null || bookingDate.before(event.getEndDate())) {
        if (BookingStatus.CONFIRMED.equals(status)) {
          sendEventBookingConfirmationNotificationEmail(event, user, booking);
        } else if (BookingStatus.WAITING_LIST.equals(status)) {
          sendBookingWaitingListAdditionNotificationEmail2(event, user);
        }
      }
    } catch (ContentManagerException e) {
      log.error(String.format("Unable to send booking confirmation email (%s) to user (%s)", event.getId(), user
          .getEmail()), e);
    }

    return booking;
  }

  /**
   * Attempt to book onto an event.
   * This method will allow attempt to book a onto an event if the rules are not broken.
   *
   * @param event                      - of interest
   * @param user                       - user to book on to the event.
   * @param additionalEventInformation - any additional information for the event organisers (nullable)
   * @return the newly created booking.
   * @throws SegueDatabaseException       - if there is a database error
   * @throws EmailMustBeVerifiedException - if this method requires a validated e-mail address.
   * @throws DuplicateBookingException    - Duplicate booking, only unique bookings.
   * @throws EventIsFullException         - No space on the event
   * @throws EventDeadlineException       - The deadline for booking has passed.
   */
  public EventBookingDTO requestBooking(final IsaacEventPageDTO event, final RegisteredUserDTO user,
                                        final Map<String, String> additionalEventInformation)
      throws SegueDatabaseException, EmailMustBeVerifiedException, DuplicateBookingException,
      EventIsFullException, EventDeadlineException, EventIsCancelledException {
    this.ensureValidEventAndUser(event, user, true);

    if (EventStatus.CANCELLED.equals(event.getEventStatus())) {
      throw new EventIsCancelledException(
          String.format(EXCEPTION_MESSAGE_TEMPLATE_CANCELLED_EVENT, user.getId(),
              event.getId()));
    }

    EventBookingDTO booking;
    try (ITransaction transaction = transactionManager.getTransaction()) {
      // Obtain an exclusive database lock to lock the event
      this.bookingPersistenceManager.lockEventUntilTransactionComplete(transaction, event.getId());
      // attempt to book them on the event
      BookingStatus existingBookingStatus = this.getBookingStatus(event.getId(), user.getId());

      if (existingBookingStatus != null) {
        // User has existing booking - update it or throw an exception as appropriate
        booking = switch (existingBookingStatus) {
          case CONFIRMED, ATTENDED, ABSENT ->
              // If the user already has a confirmed booking, don't allow a duplicate
              // Attended and absent should be caught by event validity check but handle anyway
              throw new DuplicateBookingException(
                  String.format(EXCEPTION_MESSAGE_TEMPLATE_DUPLICATE_BOOKING, event.getId(), user.getEmail()));
          case CANCELLED -> {
            // If the user has previously cancelled we should check capacity and let them book again
            this.ensureCapacity(event, user);
            yield this.bookingPersistenceManager.updateBookingStatus(transaction, event.getId(), user.getId(),
                BookingStatus.CONFIRMED, additionalEventInformation);
          }
          case WAITING_LIST -> {
            // If the user is on the waiting list we should check capacity and update their existing booking
            // Automated promotion means this case probably shouldn't happen but handle it just in case
            this.ensureCapacity(event, List.of(user), true);
            yield this.bookingPersistenceManager.updateBookingStatus(transaction, event.getId(), user.getId(),
                BookingStatus.CONFIRMED, additionalEventInformation);
          }
          case RESERVED ->
              // as reserved bookings already count toward capacity we DO NOT check capacity
              this.bookingPersistenceManager.updateBookingStatus(transaction, event.getId(), user.getId(),
                  BookingStatus.CONFIRMED, additionalEventInformation);
        };
      } else {
        // check capacity at this moment in time and then create booking
        this.ensureCapacity(event, user);
        booking = this.bookingPersistenceManager.createBooking(transaction, event.getId(), user.getId(),
            BookingStatus.CONFIRMED, additionalEventInformation);
      }
      transaction.commit();
    }
    // Done with transaction from here:
    addUserToEventGroup(event, user);

    try {
      // This should send a confirmation email in any case.
      sendEventBookingConfirmationNotificationEmail(event, user, booking);

    } catch (ContentManagerException e) {
      log.error(EXCEPTION_MESSAGE_TEMPLATE_UNABLE_TO_SEND_EMAIL, event.getId(), user.getEmail(), e);
    }

    return booking;
  }

  /**
   * Request a reservation for the given batch of users on the given event.
   * This method will allow teachers and group managers to "soft-book" students providing they do not exceed their
   * allocated limit.
   * There is no additional event information passed now, this can be filled in later when actual bookings are
   * requested.
   *
   * @param event         - to reserve the user on
   * @param users         - to reserve on the event
   * @param reservingUser - the user making the reservation
   * @return confirmation of reservation
   */
  public List<EventBookingDTO> requestReservations(final IsaacEventPageDTO event, final List<RegisteredUserDTO> users,
                                                   final RegisteredUserDTO reservingUser)
      throws EventDeadlineException, DuplicateBookingException, EventIsFullException,
      EventGroupReservationLimitException,
      SegueDatabaseException, EventIsCancelledException {

    // Cannot reserve spots onto a cancelled event
    if (EventStatus.CANCELLED.equals(event.getEventStatus())) {
      throw new EventIsCancelledException(String.format(
          "User (%s) was unable to reserve places for event (%s); the event is cancelled.",
          reservingUser.getId(), event.getId()
      ));
    }

    List<RegisteredUserDTO> unreservableUsers = new ArrayList<>();
    for (RegisteredUserDTO user : users) {
      try {
        this.ensureValidEventAndUser(event, user, true);
      } catch (EmailMustBeVerifiedException e) {
        unreservableUsers.add(user);
      }
    }

    List<EventBookingDTO> reservations = new ArrayList<>();
    // Wrap this into a database transaction:
    try (ITransaction transaction = transactionManager.getTransaction()) {
      try {
        // Obtain an exclusive database lock for the event:
        this.bookingPersistenceManager.lockEventUntilTransactionComplete(transaction, event.getId());

        // is there space on the event? Teachers don't count for student events.
        // work out capacity information for the event at this moment in time.
        // If there is no space, no reservations are made. Throw an exception and handle in EventsFacade.
        this.ensureCapacity(event, users, null);

        // Is the request for more reservations that this event allows?
        this.enforceReservationLimit(event, users, reservingUser);

        for (RegisteredUserDTO user : users) {
          // attempt to book them on the event
          EventBookingDTO reservation;

          // Set the reservation close date (date at which an unconfirmed reservation is cancelled) to
          // the day of the event or in EVENT_RESERVATION_CLOSE_INTERVAL_DAYS from now, whichever is earlier.
          Calendar calendar = Calendar.getInstance();
          calendar.add(Calendar.DAY_OF_MONTH, EVENT_RESERVATION_CLOSE_INTERVAL_DAYS);
          Date reservationCloseDate = Stream.of(calendar.getTime(), event.getDate())
              .min(Comparator.comparing(Date::getTime))
              .orElseThrow(NoSuchElementException::new);

          SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
          Map<String, String> additionalEventInformation = new HashMap<>();
          additionalEventInformation.put("reservationCloseDate", dateFormat.format(reservationCloseDate));

          // attempt to book them on the event
          BookingStatus existingBookingStatus = this.getBookingStatus(event.getId(), user.getId());
          if (existingBookingStatus != null && List.of(BookingStatus.RESERVED, BookingStatus.WAITING_LIST,
              BookingStatus.CONFIRMED).contains(existingBookingStatus)) {
            throw new DuplicateBookingException(String.format("Unable to reserve onto event (%s) as user (%s) is"
                + " already reserved, on the waiting list or booked on to it.", event.getId(), user.getEmail()));
          } else if (BookingStatus.CANCELLED.equals(existingBookingStatus)) {
            // if the user has previously cancelled we should let them book again.
            reservation = this.bookingPersistenceManager.updateBookingStatus(transaction, event.getId(),
                user.getId(), reservingUser.getId(), BookingStatus.RESERVED,
                additionalEventInformation);
          } else {
            reservation = this.bookingPersistenceManager.createBooking(transaction, event.getId(), user.getId(),
                reservingUser.getId(), BookingStatus.RESERVED, additionalEventInformation);
          }
          reservations.add(reservation);
        }
        transaction.commit();
      } catch (SegueDatabaseException e) {
        // Something happened, we just roll the transaction back and rethrow.
        // Apparently, this is the only exception that can be thrown after we began the transaction.
        transaction.rollback();
        throw e;
      }
    }

    // Send email to individual reserved users
    for (EventBookingDTO reservation : reservations) {
      try {
        UserSummaryDTO userSummary = reservation.getUserBooked();
        Long userId = userSummary.getId();
        RegisteredUserDTO user = userAccountManager.getUserDTOById(userId);
        sendBookingReservationRequestNotificationEmail(event, user, getTeacherNameFromUser(reservingUser));
      } catch (NoUserException e) {
        // This should never really happen, though...
        log.error(String.format("Unable to find reserved user while sending emails for event (%s)",
            event.getId()), e);
      } catch (SegueDatabaseException | ContentManagerException e) {
        log.error(EXCEPTION_MESSAGE_TEMPLATE_UNABLE_TO_SEND_EMAIL, event.getId(), reservation.getUserBooked().getId(),
            e);
      }
    }

    // Send recap email to reserving user
    try {
      StringBuilder htmlSB = new StringBuilder();
      StringBuilder plainTextSB = new StringBuilder();
      htmlSB.append("<ul>");
      for (EventBookingDTO reservation : reservations) {
        RegisteredUserDTO user = userAccountManager.getUserDTOById(reservation.getUserBooked().getId());
        String userFullName = String.format("%s %s", user.getGivenName(), user.getFamilyName());
        htmlSB.append(String.format("<li>%s</li>", userFullName));
        plainTextSB.append(String.format("- %s\n", userFullName));
      }
      htmlSB.append("</ul>");
      sendEventReservationRecapEmail(event, reservingUser, htmlSB, plainTextSB);
    } catch (NoUserException e) {
      // This should never really happen, though...
      log.error(
          String.format("Unable to find reserved user while sending recap email for event (%s) to reserving user (%s)",
              event.getId(), reservingUser.getId()), e);
    } catch (SegueDatabaseException | ContentManagerException e) {
      log.error(String.format("Unable to send event reservation recap email (%s) to user (%s)",
          event.getId(), reservingUser.getId()), e);
    }

    // If the frontend prevents selection of unreservable users, then this email should never go out.
    if (!unreservableUsers.isEmpty()) {
      // Log that the reserving user tried to reserve invalid users.
      log.error("User ({}) tried to request a reservation for invalid users on an event ({}). Users requested: {}",
          reservingUser.getId(), event.getId(), unreservableUsers);
    }

    return reservations;
  }

  /**
   * Attempt to book onto the waiting list for an event.
   *
   * @param event                 - of interest
   * @param user                  - user to book on to the event.
   * @param additionalInformation additional information to be stored with this booking e.g. dietary requirements.
   * @return the newly created booking.
   * @throws SegueDatabaseException       - if there is a database error
   * @throws EmailMustBeVerifiedException - if this method requires a validated e-mail address.
   * @throws DuplicateBookingException    - Duplicate booking, only unique bookings.
   * @throws EventIsNotFullException      - There is space on the event
   * @throws EventDeadlineException       - The deadline for booking has passed.
   */
  public EventBookingDTO requestWaitingListBooking(final IsaacEventPageDTO event, final RegisteredUserDTO user,
                                                   final Map<String, String> additionalInformation) throws
      SegueDatabaseException, EmailMustBeVerifiedException, DuplicateBookingException,
      EventDeadlineException, EventIsNotFullException, EventIsCancelledException {

    if (EventStatus.CANCELLED.equals(event.getEventStatus())) {
      throw new EventIsCancelledException(
          String.format(EXCEPTION_MESSAGE_TEMPLATE_CANCELLED_EVENT, user.getId(),
              event.getId()));
    }

    final Date now = new Date();
    this.ensureValidEventAndUser(event, user, true);

    EventBookingDTO booking;
    try (ITransaction transaction = transactionManager.getTransaction()) {
      // Obtain an exclusive database lock to lock the event
      this.bookingPersistenceManager.lockEventUntilTransactionComplete(transaction, event.getId());

      Integer numberOfPlaces = getPlacesAvailable(event);
      // check the number of places - if some available then check if the event deadline has passed. If not
      // throw error.
      if (numberOfPlaces != null && !EventStatus.WAITING_LIST_ONLY.equals(event.getEventStatus()) && numberOfPlaces > 0
          && !(event.getBookingDeadline() != null && now.after(event.getBookingDeadline()))) {
        throw new EventIsNotFullException("There are still spaces on this event. Please attempt to book on it.");
      }

      BookingStatus existingBookingStatus = this.getBookingStatus(event.getId(), user.getId());
      // attempt to book them on the waiting list of the event.
      if (existingBookingStatus != null && List.of(BookingStatus.CONFIRMED, BookingStatus.WAITING_LIST,
          BookingStatus.RESERVED).contains(existingBookingStatus)) {
        throw new DuplicateBookingException(String.format("Unable to add to event (%s) waiting list as user (%s) is"
            + " already on it, reserved or booked.", event.getId(), user.getEmail()));
      } else if (BookingStatus.CANCELLED.equals(existingBookingStatus)) {
        // if the user has previously cancelled we should let them book again.
        booking = this.bookingPersistenceManager.updateBookingStatus(transaction, event.getId(),
            user.getId(),
            BookingStatus.WAITING_LIST,
            additionalInformation);
      } else {
        booking = this.bookingPersistenceManager.createBooking(transaction, event.getId(),
            user.getId(),
            null,
            BookingStatus.WAITING_LIST,
            additionalInformation);
      }
      transaction.commit();
    }

    // Auto add user to the event group if the event is a special Waiting List Only type event
    if (EventStatus.WAITING_LIST_ONLY.equals(event.getEventStatus())) {
      addUserToEventGroup(event, user);
    }

    try {
      sendWaitingListAdditionNotificationEmail3(event, user);
    } catch (ContentManagerException e) {
      log.error(EXCEPTION_MESSAGE_TEMPLATE_UNABLE_TO_SEND_EMAIL, event.getId(), user.getEmail(), e);
    }

    return booking;
  }

  /**
   * Allows an admin user to promote someone from a waiting list or cancellation to a confirmed booking.
   *
   * @param event   - The event in question.
   * @param userDTO - The user whose booking should be updated
   * @return the updated booking.
   * @throws SegueDatabaseException      - if there is a database error
   * @throws EventIsFullException        - No space on the event
   * @throws EventBookingUpdateException - Unable to update the event booking.
   */
  public EventBookingDTO promoteToConfirmedBooking(final IsaacEventPageDTO event, final RegisteredUserDTO userDTO)
      throws SegueDatabaseException, EventBookingUpdateException, EventIsFullException, EventIsCancelledException {

    if (EventStatus.CANCELLED.equals(event.getEventStatus())) {
      throw new EventIsCancelledException(
          String.format("Unable to confirm user (%s) booking on event (%s); the event is cancelled.", userDTO.getId(),
              event.getId()));
    }

    EventBookingDTO updatedStatus;
    try (ITransaction transaction = transactionManager.getTransaction()) {
      this.bookingPersistenceManager.lockEventUntilTransactionComplete(transaction, event.getId());

      final DetailedEventBookingDTO eventBooking = this.bookingPersistenceManager.getBookingByEventIdAndUserId(
          event.getId(), userDTO.getId());
      if (null == eventBooking) {
        throw new EventBookingUpdateException("Unable to promote a booking that doesn't exist.");
      }

      if (BookingStatus.CONFIRMED.equals(eventBooking.getBookingStatus())) {
        throw new EventBookingUpdateException("Unable to promote a booking that is CONFIRMED already.");
      }

      if (BookingStatus.RESERVED.equals(eventBooking.getBookingStatus())) {
        throw new EventBookingUpdateException("Unable to promote a booking that is RESERVED as we "
            + "might not have all of the required user information.");
      }

      final Integer placesAvailable = this.getPlacesAvailable(event, true);
      if (placesAvailable != null && placesAvailable <= 0) {
        throw new EventIsFullException("The event you are attempting promote a booking for is at or "
            + "over capacity.");
      }

      updatedStatus = this.bookingPersistenceManager
          .updateBookingStatus(transaction, eventBooking.getEventId(), userDTO.getId(),
              BookingStatus.CONFIRMED, eventBooking.getAdditionalInformation());
      transaction.commit();
    }

    addUserToEventGroup(event, userDTO);
    sendEventBookingPromotionNotificationEmail(event, userDTO, updatedStatus);

    return updatedStatus;
  }

  private void sendEventBookingPromotionNotificationEmail(IsaacEventPageDTO event, RegisteredUserDTO userDTO,
                                                          EventBookingDTO updatedStatus)
      throws SegueDatabaseException, EventBookingUpdateException {
    try {
      // Send an email notifying the user (unless they are being promoted after the event for the sake of our records)
      Date promotionDate = new Date();
      if (event.getEndDate() == null || promotionDate.before(event.getEndDate())) {
        sendWaitingListPromotionConfirmationNotificationEmail(event, userDTO, updatedStatus);
      }
    } catch (ContentManagerException e) {
      log.error(EXCEPTION_MESSAGE_TEMPLATE_UNABLE_TO_SEND_EMAIL, event.getId(), userDTO.getEmail(), e);
      throw new EventBookingUpdateException("Unable to send event email, failed to update event booking");
    }
  }

  /**
   * Allows an admin user to record the attendance of a booking as either attended or absent.
   *
   * @param event    - The event in question.
   * @param userDTO  - The user whose booking should be updated.
   * @param attended - Whether the user attended the event or not.
   * @return the updated booking.
   * @throws SegueDatabaseException      - Database error.
   * @throws EventBookingUpdateException - Unable to update the event booking.
   */
  public DetailedEventBookingDTO recordAttendance(final IsaacEventPageDTO event, final RegisteredUserDTO
      userDTO, final boolean attended)
      throws SegueDatabaseException, EventBookingUpdateException, EventIsCancelledException {

    if (EventStatus.CANCELLED.equals(event.getEventStatus())) {
      throw new EventIsCancelledException(
          String.format("Unable to record user (%s) attendance for event (%s); the event is cancelled.",
              userDTO.getId(), event.getId()));
    }

    DetailedEventBookingDTO updatedBooking;
    try (ITransaction transaction = transactionManager.getTransaction()) {
      this.bookingPersistenceManager.lockEventUntilTransactionComplete(transaction, event.getId());

      final DetailedEventBookingDTO eventBooking = this.bookingPersistenceManager.getBookingByEventIdAndUserId(
          event.getId(), userDTO.getId());
      if (null == eventBooking) {
        throw new EventBookingUpdateException("Unable to record attendance for booking that doesn't exist.");
      }

      BookingStatus attendanceStatus = attended ? BookingStatus.ATTENDED : BookingStatus.ABSENT;
      if (attendanceStatus.equals(eventBooking.getBookingStatus())) {
        throw new EventBookingUpdateException("Booking attendance is already registered.");
      }

      updatedBooking = this.bookingPersistenceManager.updateBookingStatus(transaction, eventBooking.getEventId(),
          userDTO.getId(), attendanceStatus, eventBooking.getAdditionalInformation());

      transaction.commit();
    }
    return updatedBooking;
  }

  /**
   * getPlacesAvailable.
   * This method is not threadsafe and will not acquire a lock.
   * It assumes that both WAITING_LIST and CONFIRMED bookings count towards capacity for all events apart from
   * WAITING_LIST_ONLY events where only confirmed bookings count.
   * <br>
   * This assumption allows waiting list bookings to be manually changed into CONFIRMED by event
   * managers without the possibility of someone creating a new booking to occupy the space after a confirmed
   * cancellation.
   * <br>
   * It also assumes teachers don't count on student events.
   *
   * @param event - the event we care about
   * @return the number of places available or Null if there is no limit. If a negative number would be returned the
   *     method will only return 0. This allows for manual overbooking.
   * @throws SegueDatabaseException - if we cannot contact the database.
   */
  public Integer getPlacesAvailable(final IsaacEventPageDTO event) throws SegueDatabaseException {
    boolean isWaitingListOnly = EventStatus.WAITING_LIST_ONLY.equals(event.getEventStatus());
    return this.getPlacesAvailable(event, isWaitingListOnly);
  }

  /**
   * getPlacesAvailable.
   * This method is not threadsafe and will not acquire a lock.
   * <br>
   * It also assumes teachers don't count on student events.
   *
   * @param event              - the event we care about
   * @param countOnlyConfirmed - if true only count confirmed bookings (i.e. ignore waiting list ones.
   * @return the number of places available or Null if there is no limit. If a negative number would be returned
   *     the method will only return 0. This allows for manual overbooking.
   * @throws SegueDatabaseException - if we cannot contact the database.
   */
  private Integer getPlacesAvailable(final IsaacEventPageDTO event, final boolean countOnlyConfirmed)
      throws SegueDatabaseException {
    boolean isStudentEvent = event.getTags().contains(EVENT_STAGE_STUDENT);
    Integer numberOfPlaces = event.getNumberOfPlaces();
    if (null == numberOfPlaces) {
      return null;
    }

    // include deleted users' bookings events only if the event is in the past so it doesn't mess with ability for new
    // users to book on future events.
    boolean includeDeletedUsersInCounts = event.getDate() != null && event.getDate().before(new Date());

    Map<BookingStatus, Map<Role, Integer>> eventBookingStatusCounts =
        this.bookingPersistenceManager.getEventBookingStatusCounts(event.getId(), includeDeletedUsersInCounts);

    Map<Role, Integer> roleCounts;
    if (countOnlyConfirmed) {
      roleCounts = eventBookingStatusCounts.getOrDefault(BookingStatus.CONFIRMED, new HashMap<>());
    } else {
      roleCounts = getCombinedBookingCountsByRole(eventBookingStatusCounts);
    }

    // Calculate remaining capacity with a lower bound of zero (no negatives)
    if (isStudentEvent) {
      // For Student events we only limit the number of students, other roles do not count against the capacity
      Integer studentCount = roleCounts.getOrDefault(Role.STUDENT, 0);
      return Math.max(0, numberOfPlaces - studentCount);
    } else {
      // For other events, count all roles
      Integer totalBooked = roleCounts.values().stream().reduce(0, Integer::sum);
      return Math.max(0, numberOfPlaces - totalBooked);
    }
  }

  private static Map<Role, Integer> getCombinedBookingCountsByRole(
      Map<BookingStatus, Map<Role, Integer>> eventBookingStatusCounts) {
    List<Map<Role, Integer>> listOfBookingCountByRoleMaps = new ArrayList<>(3);
    // Extract the count-by-role maps for each relevant status and add them to a list
    if (eventBookingStatusCounts.get(BookingStatus.CONFIRMED) != null) {
      listOfBookingCountByRoleMaps.add(eventBookingStatusCounts.get(BookingStatus.CONFIRMED));
    }
    if (eventBookingStatusCounts.get(BookingStatus.WAITING_LIST) != null) {
      listOfBookingCountByRoleMaps.add(eventBookingStatusCounts.get(BookingStatus.WAITING_LIST));
    }
    if (eventBookingStatusCounts.get(BookingStatus.RESERVED) != null) {
      listOfBookingCountByRoleMaps.add(eventBookingStatusCounts.get(BookingStatus.RESERVED));
    }
    // Flatten the list and return a single map of the summed counts for each role
    return listOfBookingCountByRoleMaps.stream().flatMap(map -> map.entrySet().stream())
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, Integer::sum));
  }

  /**
   * Find out if a user is already booked on an event.
   *
   * @param eventId - of interest
   * @param userId  - of interest.
   * @return true if a booking exists false if not
   * @throws SegueDatabaseException - if an error occurs.
   */
  public boolean isUserBooked(final String eventId, final Long userId) throws SegueDatabaseException {
    return this.bookingPersistenceManager.isUserBooked(eventId, userId);
  }

  /**
   * Find out if a user has a booking with a given status.
   *
   * @param eventId       - of interest
   * @param userId        - of interest.
   * @param bookingStatus - the status of the booking.
   * @return true if a waiting list booking exists false if not
   * @throws SegueDatabaseException - if an error occurs.
   */
  public boolean hasBookingWithStatus(final String eventId, final Long userId, final BookingStatus bookingStatus)
      throws SegueDatabaseException {
    try {
      EventBookingDTO eb = this.bookingPersistenceManager.getBookingByEventIdAndUserId(eventId, userId);
      return null != eb && bookingStatus.equals(eb.getBookingStatus());
    } catch (ResourceNotFoundException e) {
      return false;
    }
  }

  /**
   * Find out if a user has a booking with any of the given statuses.
   *
   * @param eventId         - of interest
   * @param userId          - of interest.
   * @param bookingStatuses - the statuses of the booking.
   * @return true if a waiting list booking exists false if not
   * @throws SegueDatabaseException - if an error occurs.
   */
  public boolean hasBookingWithAnyOfStatuses(final String eventId, final Long userId,
                                             final Set<BookingStatus> bookingStatuses)
      throws SegueDatabaseException {
    try {
      EventBookingDTO eb = this.bookingPersistenceManager.getBookingByEventIdAndUserId(eventId, userId);
      return null != eb && bookingStatuses.contains(eb.getBookingStatus());
    } catch (ResourceNotFoundException e) {
      return false;
    }
  }

  /**
   * Return the booking status for the given user and event IDs.
   *
   * @param eventId - of interest
   * @param userId  - of interest.
   * @return bookingStatus - the status of the booking.
   * @throws SegueDatabaseException - if an error occurs.
   */
  public BookingStatus getBookingStatus(final String eventId, final Long userId)
      throws SegueDatabaseException {
    try {
      EventBookingDTO eb = this.bookingPersistenceManager.getBookingByEventIdAndUserId(eventId, userId);
      return eb != null ? eb.getBookingStatus() : null;
    } catch (ResourceNotFoundException e) {
      return null;
    }
  }

  /**
   * Cancel a booking.
   * <br>
   * Note: cancelled bookings no longer occupy space on an events capacity calculations.
   *
   * @param event - event
   * @param user  - user to unbook
   * @throws SegueDatabaseException  - if a database error occurs.
   * @throws ContentManagerException - if a content error occurs.
   */
  public void cancelBooking(final IsaacEventPageDTO event, final RegisteredUserDTO user)
      throws SegueDatabaseException, ContentManagerException {

    Long reservedById;
    BookingStatus previousBookingStatus;
    EventBookingDTO updatedWaitingListBooking;
    try (ITransaction transaction = transactionManager.getTransaction()) {
      // Obtain an exclusive database lock to lock the booking
      this.bookingPersistenceManager.lockEventUntilTransactionComplete(transaction, event.getId());

      EventBookingDTO previousBooking =
          this.bookingPersistenceManager.getBookingByEventIdAndUserId(event.getId(), user.getId());
      reservedById = previousBooking.getReservedById();
      previousBookingStatus = previousBooking.getBookingStatus();
      this.bookingPersistenceManager.updateBookingStatus(transaction, event.getId(), user.getId(),
          BookingStatus.CANCELLED, null);

      // If the status was CONFIRMED or RESERVED then promote the oldest booking from the waiting list, if one exists
      // WAITING_LIST bookings can also be cancelled but do not trigger promotion
      if (previousBookingStatus == BookingStatus.CONFIRMED || previousBookingStatus == BookingStatus.RESERVED) {
        List<DetailedEventBookingDTO> waitingListBookings =
            this.bookingPersistenceManager.adminGetBookingsByEventIdAndStatus(event.getId(),
                BookingStatus.WAITING_LIST);
        if (!waitingListBookings.isEmpty()) {
          DetailedEventBookingDTO oldestWaitingListBooking =
              Collections.min(waitingListBookings, Comparator.comparing(EventBookingDTO::getBookingDate));
          updatedWaitingListBooking = this.bookingPersistenceManager.updateBookingStatus(transaction, event.getId(),
              oldestWaitingListBooking.getUserBooked().getId(), BookingStatus.CONFIRMED,
              oldestWaitingListBooking.getAdditionalInformation());
        } else {
          updatedWaitingListBooking = null;
        }
      } else {
        updatedWaitingListBooking = null;
      }

      transaction.commit();
    }

    // Reservations do not auto add users to the event's group, so no need to remove them.
    if (!previousBookingStatus.equals(BookingStatus.RESERVED)) {
      // auto remove them from the group
      this.removeUserFromEventGroup(event, user);
    }
    try {
      // Send an email notifying the user (unless they are being canceled after the event for the sake of our records)
      sendEventBookingCancellationNotificationEmails(event, user, reservedById, previousBookingStatus);
    } catch (NoUserException e) {
      log.error("Unable to resolve reserving user ({}) in the database, notification of "
          + "student cancellation email was not sent", reservedById);
    }

    if (updatedWaitingListBooking != null) {
      Long promotedBookingUserId = updatedWaitingListBooking.getUserBooked().getId();
      try {
        RegisteredUserDTO promotedBookingUser =
            userAccountManager.getUserDTOById(promotedBookingUserId);
        addUserToEventGroup(event, promotedBookingUser);
        sendEventBookingPromotionNotificationEmail(event, promotedBookingUser, updatedWaitingListBooking);
      } catch (NoUserException e) {
        log.error("An error occurred while promoting the booking for user {} on event {}."
                + " They have not been added to the event group, nor has a notification email been sent",
            promotedBookingUserId, event.getId(), e);
      } catch (EventBookingUpdateException e) {
        log.error("An error occurred while promoting the booking for user {} on event {}."
            + "A notification email could not be sent.", promotedBookingUserId, event.getId(), e);
      }
    }
  }

  private void sendEventBookingCancellationNotificationEmails(IsaacEventPageDTO event, RegisteredUserDTO user,
                                                              Long reservedById, BookingStatus previousBookingStatus)
      throws ContentManagerException, SegueDatabaseException, NoUserException {
    Date bookingCancellationDate = new Date();
    if (event.getEndDate() == null || bookingCancellationDate.before(event.getEndDate())) {
      if (previousBookingStatus.equals(BookingStatus.RESERVED) && reservedById != null) {
        sendEventReservationCancellationNotificationEmailToAttendee(event, user);
        // We may also want to send an email to userAccountManager.getUserDTOById(reservedById)
        RegisteredUserDTO reserver = userAccountManager.getUserDTOById(reservedById);
        sendEventReservationCancellationNotificationEmailToReserver(event, user, reserver);
      } else {
        sendConfirmedEventBookingCancellationNotification(event, user);
      }
    }
  }

  /**
   * Delete a booking permanently.
   *
   * @param event - event context
   * @param user  - user to unbook
   * @throws SegueDatabaseException - if an error occurs.
   */
  public void deleteBooking(final IsaacEventPageDTO event, final RegisteredUserDTO user) throws SegueDatabaseException {
    try (ITransaction transaction = transactionManager.getTransaction()) {
      // Obtain an exclusive database lock to lock the booking
      this.bookingPersistenceManager.lockEventUntilTransactionComplete(transaction, event.getId());

      this.bookingPersistenceManager.deleteBooking(transaction, event.getId(), user.getId());
      this.removeUserFromEventGroup(event, user);
      transaction.commit();
    }
  }

  /**
   * Expunge additional information fields for all of a given user's bookings i.e. to remove PII.
   *
   * @param user - user to unbook
   * @throws SegueDatabaseException - if an error occurs.
   */
  public void deleteUsersAdditionalInformationBooking(final RegisteredUserDTO user) throws SegueDatabaseException {
    this.bookingPersistenceManager.deleteAdditionalInformation(user.getId());
  }

  /**
   * This method will attempt to resend the last email that a user booked on an event should have received.
   * E.g. if their status is confirmed it would be a welcome email, if cancelled it would be a cancellation one.
   *
   * @param event - event that the user was booked on.
   * @param user  - user to be emailed.
   */
  public void resendEventEmail(final IsaacEventPageDTO event, final RegisteredUserDTO user)
      throws SegueDatabaseException, ContentManagerException, EventIsCancelledException {

    if (EventStatus.CANCELLED.equals(event.getEventStatus())) {
      throw new EventIsCancelledException(
          String.format("Cannot resent event emails for user (%s). event (%s) is cancelled.", user.getId(),
              event.getId()));
    }

    EventBookingDTO booking
        = this.bookingPersistenceManager.getBookingByEventIdAndUserId(event.getId(), user.getId());

    if (booking.getBookingStatus().equals(BookingStatus.CONFIRMED)) {
      sendEventBookingConfirmationNotificationEmail(event, user, booking);

    } else if (booking.getBookingStatus().equals(BookingStatus.CANCELLED)) {
      sendConfirmedEventBookingCancellationNotification(event, user);

    } else if (booking.getBookingStatus().equals(BookingStatus.WAITING_LIST)) {
      sendBookingWaitingListAdditionNotificationEmail(event, user);
    } else if (booking.getBookingStatus().equals(BookingStatus.RESERVED)) {
      String reservingUserName;
      try {
        RegisteredUserDTO reservedByUser = userAccountManager.getUserDTOById(booking.getReservedById(), true);
        reservingUserName = String.format("%s %s", reservedByUser.getGivenName(), reservedByUser.getFamilyName());
      } catch (NoUserException e) {
        reservingUserName = "";
        log.error(
            String.format("Unable to find the reserving user (%d) for this event (%s).", booking.getReservedById(),
                event.getId()));
      }
      sendBookingReservationRequestNotificationEmail(event, user, reservingUserName);
    } else {
      log.error("Unknown event booking status. Unable to select correct email.");
    }
  }

  private void sendBookingReservationRequestNotificationEmail(IsaacEventPageDTO event, RegisteredUserDTO user,
                                                              String reservingUserName)
      throws ContentManagerException, SegueDatabaseException {
    emailManager.sendTemplatedEmailToUser(user,
        emailManager.getEmailTemplateDTO("email-event-reservation-requested"),
        new ImmutableMap.Builder<String, Object>()
            .put("reservingUser", reservingUserName)
            .put(EMAIL_TEMPLATE_TOKEN_CONTACT_US_URL, generateEventContactUsURL(event))
            .put(EMAIL_TEMPLATE_TOKEN_EVENT_URL, String.format("https://%s/eventbooking/%s",
                propertiesLoader.getProperty(HOST_NAME), event.getId()))
            .put(EMAIL_TEMPLATE_TOKEN_EVENT_DETAILS,
                event.getEmailEventDetails() == null ? "" : event.getEmailEventDetails())
            .put(EMAIL_TEMPLATE_TOKEN_EVENT, event)
            .build(),
        EmailType.SYSTEM);
  }

  private void sendEventReservationRecapEmail(IsaacEventPageDTO event, RegisteredUserDTO reservingUser,
                                              StringBuilder htmlSB, StringBuilder plainTextSB)
      throws ContentManagerException, SegueDatabaseException {
    emailManager.sendTemplatedEmailToUser(reservingUser,
        emailManager.getEmailTemplateDTO("email-event-reservation-recap"),
        new ImmutableMap.Builder<String, Object>()
            .put(EMAIL_TEMPLATE_TOKEN_CONTACT_US_URL, generateEventContactUsURL(event))
            .put(EMAIL_TEMPLATE_TOKEN_EVENT_URL,
                String.format("https://%s/events/%s", propertiesLoader.getProperty(HOST_NAME), event.getId()))
            .put(EMAIL_TEMPLATE_TOKEN_EVENT, event)
            .put("studentsList", plainTextSB.toString())
            .put("studentsList_HTML", htmlSB.toString())
            .build(),
        EmailType.SYSTEM);
  }

  private void sendWaitingListPromotionConfirmationNotificationEmail(IsaacEventPageDTO event, RegisteredUserDTO userDTO,
                                                                     EventBookingDTO updatedStatus)
      throws ContentManagerException, SegueDatabaseException {
    String emailTemplateContentId;
    if (event.getEventStatus() == EventStatus.WAITING_LIST_ONLY) {
      emailTemplateContentId = "email-event-booking-waiting-list-only-promotion-confirmed";
    } else {
      emailTemplateContentId = "email-event-booking-waiting-list-promotion-confirmed";
    }

    emailManager.sendTemplatedEmailToUser(userDTO,
        emailManager.getEmailTemplateDTO(emailTemplateContentId),
        new ImmutableMap.Builder<String, Object>()
            .put(EMAIL_TEMPLATE_TOKEN_CONTACT_US_URL, generateEventContactUsURL(event))
            .put(EMAIL_TEMPLATE_TOKEN_AUTHORIZATION_LINK, String.format(AUTH_TOKEN_LINK,
                propertiesLoader.getProperty(HOST_NAME), event.getIsaacGroupToken()))
            .put(EMAIL_TEMPLATE_TOKEN_EVENT_DETAILS,
                event.getEmailEventDetails() == null ? "" : event.getEmailEventDetails())
            .put(EMAIL_TEMPLATE_TOKEN_EVENT, event)
            .build(),
        EmailType.SYSTEM,
        Collections.singletonList(generateEventICSFile(event, updatedStatus)));
  }

  private void sendBookingWaitingListAdditionNotificationEmail(IsaacEventPageDTO event, RegisteredUserDTO user)
      throws ContentManagerException, SegueDatabaseException {
    emailManager.sendTemplatedEmailToUser(user,
        emailManager.getEmailTemplateDTO(EMAIL_TEMPLATE_ID_WAITING_LIST_ADDITION),
        new ImmutableMap.Builder<String, Object>()
            .put(EMAIL_TEMPLATE_TOKEN_CONTACT_US_URL, generateEventContactUsURL(event))
            .put(EMAIL_TEMPLATE_TOKEN_EVENT_DETAILS,
                event.getEmailEventDetails() == null ? "" : event.getEmailEventDetails())
            .put(EMAIL_TEMPLATE_TOKEN_EVENT, event)
            .build(),
        EmailType.SYSTEM);
  }

  private void sendBookingWaitingListAdditionNotificationEmail2(IsaacEventPageDTO event, RegisteredUserDTO user)
      throws ContentManagerException, SegueDatabaseException {
    String emailTemplateContentId;
    if (event.getEventStatus() == EventStatus.WAITING_LIST_ONLY) {
      emailTemplateContentId = EMAIL_TEMPLATE_ID_WAITING_LIST_ONLY_ADDITION;
    } else {
      emailTemplateContentId = EMAIL_TEMPLATE_ID_WAITING_LIST_ADDITION;
    }
    emailManager.sendTemplatedEmailToUser(user,
        emailManager.getEmailTemplateDTO(emailTemplateContentId),
        new ImmutableMap.Builder<String, Object>()
            .put(EMAIL_TEMPLATE_TOKEN_CONTACT_US_URL, generateEventContactUsURL(event))
            .put(EMAIL_TEMPLATE_TOKEN_EVENT_DETAILS,
                event.getEmailEventDetails() == null ? "" : event.getEmailEventDetails())
            .put(EMAIL_TEMPLATE_TOKEN_EVENT, event)
            .build(),
        EmailType.SYSTEM);
  }

  private void sendWaitingListAdditionNotificationEmail3(IsaacEventPageDTO event, RegisteredUserDTO user)
      throws ContentManagerException, SegueDatabaseException {
    String emailTemplateContentId;
    if (event.getEventStatus() == EventStatus.WAITING_LIST_ONLY) {
      emailTemplateContentId = EMAIL_TEMPLATE_ID_WAITING_LIST_ONLY_ADDITION;
    } else {
      emailTemplateContentId = EMAIL_TEMPLATE_ID_WAITING_LIST_ADDITION;
    }
    emailManager.sendTemplatedEmailToUser(user,
        emailManager.getEmailTemplateDTO(emailTemplateContentId),
        new ImmutableMap.Builder<String, Object>()
            .put(EMAIL_TEMPLATE_TOKEN_CONTACT_US_URL, generateEventContactUsURL(event))
            .put(EMAIL_TEMPLATE_TOKEN_EVENT, event)
            .build(),
        EmailType.SYSTEM);
  }

  private void sendEventBookingConfirmationNotificationEmail(IsaacEventPageDTO event, RegisteredUserDTO user,
                                                             EventBookingDTO booking)
      throws ContentManagerException, SegueDatabaseException {
    emailManager.sendTemplatedEmailToUser(user,
        emailManager.getEmailTemplateDTO(EMAIL_TEMPLATE_ID_EVENT_BOOKING_CONFIRMED),
        new ImmutableMap.Builder<String, Object>()
            .put(EMAIL_TEMPLATE_TOKEN_CONTACT_US_URL, generateEventContactUsURL(event))
            .put(EMAIL_TEMPLATE_TOKEN_AUTHORIZATION_LINK, String.format(AUTH_TOKEN_LINK,
                propertiesLoader.getProperty(HOST_NAME), event.getIsaacGroupToken()))
            .put(EMAIL_TEMPLATE_TOKEN_EVENT_DETAILS,
                event.getEmailEventDetails() == null ? "" : event.getEmailEventDetails())
            .put(EMAIL_TEMPLATE_TOKEN_EVENT, event)
            .build(),
        EmailType.SYSTEM,
        Collections.singletonList(generateEventICSFile(event, booking)));
  }

  private void sendConfirmedEventBookingCancellationNotification(IsaacEventPageDTO event, RegisteredUserDTO user)
      throws ContentManagerException, SegueDatabaseException {
    emailManager.sendTemplatedEmailToUser(user,
        emailManager.getEmailTemplateDTO("email-event-booking-cancellation-confirmed"),
        new ImmutableMap.Builder<String, Object>()
            .put(EMAIL_TEMPLATE_TOKEN_CONTACT_US_URL, generateEventContactUsURL(event))
            .put(EMAIL_TEMPLATE_TOKEN_EVENT_DETAILS,
                event.getEmailEventDetails() == null ? "" : event.getEmailEventDetails())
            .put(EMAIL_TEMPLATE_TOKEN_EVENT, event)
            .build(),
        EmailType.SYSTEM);
  }

  private void sendEventReservationCancellationNotificationEmailToReserver(IsaacEventPageDTO event,
                                                                           RegisteredUserDTO user,
                                                                           RegisteredUserDTO reserver)
      throws ContentManagerException, SegueDatabaseException {
    emailManager.sendTemplatedEmailToUser(reserver,
        emailManager.getEmailTemplateDTO("email_event_reservation_cancellation_reserver_notification"),
        new ImmutableMap.Builder<String, Object>()
            .put(EMAIL_TEMPLATE_TOKEN_CONTACT_US_URL, generateEventContactUsURL(event))
            .put(EMAIL_TEMPLATE_TOKEN_EVENT_DETAILS,
                event.getEmailEventDetails() == null ? "" : event.getEmailEventDetails())
            .put(EMAIL_TEMPLATE_TOKEN_EVENT, event)
            .put("reservedName", user.getGivenName() + " " + user.getFamilyName())
            .build(),
        EmailType.SYSTEM);
  }

  private void sendEventReservationCancellationNotificationEmailToAttendee(IsaacEventPageDTO event,
                                                                           RegisteredUserDTO user)
      throws ContentManagerException, SegueDatabaseException {
    emailManager.sendTemplatedEmailToUser(user,
        emailManager.getEmailTemplateDTO("email-event-reservation-cancellation-confirmed"),
        new ImmutableMap.Builder<String, Object>()
            .put(EMAIL_TEMPLATE_TOKEN_CONTACT_US_URL, generateEventContactUsURL(event))
            .put(EMAIL_TEMPLATE_TOKEN_EVENT_DETAILS,
                event.getEmailEventDetails() == null ? "" : event.getEmailEventDetails())
            .put(EMAIL_TEMPLATE_TOKEN_EVENT, event)
            .build(),
        EmailType.SYSTEM);
  }

  /**
   * Helper method to ensure that the booking would not violate space restrictions on the event.
   * <br>
   * If it does an exception will be thrown otherwise no exception will be thrown.
   *
   * @param event the event the user wants to book on to
   * @param user  the user who is trying to be booked onto the event.
   * @throws SegueDatabaseException - if an error occurs
   * @throws EventIsFullException   - if the event is full according to the event rules established.
   */
  private void ensureCapacity(final IsaacEventPageDTO event, final RegisteredUserDTO user) throws
      SegueDatabaseException, EventIsFullException {
    this.ensureCapacity(event, List.of(user), null);
  }

  /**
   * Helper method to ensure a batch can be booked onto an event without violating space restrictions on the event.
   * <br>
   * If it does an exception will be thrown otherwise no exception will be thrown.
   *
   * @param event the event the user wants to book on to
   * @param users the users who are trying to be booked onto the event.
   * @throws SegueDatabaseException - if an error occurs
   * @throws EventIsFullException   - if the event is full according to the event rules established.
   */
  private void ensureCapacity(final IsaacEventPageDTO event, final List<RegisteredUserDTO> users,
                              final Boolean countOnlyConfirmed) throws SegueDatabaseException, EventIsFullException {
    final boolean isStudentEvent = event.getTags().contains(EVENT_STAGE_STUDENT);
    Integer numberOfPlaces =
        countOnlyConfirmed != null ? getPlacesAvailable(event, countOnlyConfirmed) : getPlacesAvailable(event);
    if (numberOfPlaces != null) {
      long numberOfRequests = users.stream()
          // Consider tutors as students with regard to teacher events (for now)
          .filter(user -> !isStudentEvent || Role.STUDENT.equals(user.getRole()) || Role.TUTOR.equals(user.getRole()))
          .count();
      if (numberOfPlaces - numberOfRequests < 0) {
        throw new EventIsFullException(
            String.format("Unable to book %s (%s) onto event (%s) as there are not enough places available",
                users.size() > 1 ? "batch" : "user", users, event.getId()));
      }
    }
  }

  private void enforceReservationLimit(final IsaacEventPageDTO event, final List<RegisteredUserDTO> users,
                                       final RegisteredUserDTO reservingUser)
      throws SegueDatabaseException, EventGroupReservationLimitException, NullPointerException {

    if (reservingUser == null) {
      throw new NullPointerException("Reserving user must be specified.");
    }

    long numberOfExistingReservations = getBookingsByEventId(event.getId()).stream()
        // only reserving user's reservations
        .filter(reservation -> reservingUser.getId().equals(reservation.getReservedById()))
        // cancelled reservations do not count towards limit
        .filter(reservation -> !BookingStatus.CANCELLED.equals(reservation.getBookingStatus()))
        .count();

    final boolean isStudentEvent = event.getTags().contains(EVENT_STAGE_STUDENT);
    Integer groupReservationLimit = event.getGroupReservationLimit();
    if (groupReservationLimit != null) { // This should never be null
      long numberOfRequests = users.stream()
          // Teachers don't count toward student event limits
          // Consider tutors as students with regard to teacher events (for now)
          .filter(user -> !isStudentEvent || !Role.TEACHER.equals(user.getRole()))
          .count();

      if (groupReservationLimit - numberOfExistingReservations - numberOfRequests < 0) {
        throw new EventGroupReservationLimitException(String.format("You can request a maximum of %d student "
            + "reservations for event (%s)", numberOfRequests, event.getId()));
      }
    }
  }

  /**
   * Enforce common business logic on event and users for bookings.
   *
   * @param event                  of interest
   * @param user                   user to book on to the event.
   * @param enforceBookingDeadline - whether or not to enforce the booking deadline of the event
   * @throws EmailMustBeVerifiedException - if this method requires a validated e-mail address.
   * @throws EventDeadlineException       - The deadline for booking has passed.
   */
  private void ensureValidEventAndUser(final IsaacEventPageDTO event, final RegisteredUserDTO user, final boolean
      enforceBookingDeadline) throws EmailMustBeVerifiedException, EventDeadlineException {
    Date now = new Date();

    // check if the end date has passed, if one is set:
    if (event.getEndDate() != null) {
      if (now.after(event.getEndDate())) {
        throw new EventDeadlineException("The event is in the past.");
      }
    } else {
      // if there is not an endDate, ensure the start has not passed:
      if (event.getDate() != null && now.after(event.getDate())) {
        throw new EventDeadlineException("The event is in the past.");
      }
    }

    // if we are enforcing the booking deadline then enforce it.
    if (enforceBookingDeadline && event.getBookingDeadline() != null && now.after(event.getBookingDeadline())) {
      throw new EventDeadlineException("The booking deadline has passed.");
    }

    // must have verified email
    if (!EmailVerificationStatus.VERIFIED.equals(user.getEmailVerificationStatus())) {
      throw new EmailMustBeVerifiedException(String.format("Unable to book onto event (%s) without a "
              + "verified email address for user (%s).",
          event.getId(),
          user.getEmail()));
    }
  }

  /**
   * Helper method to generate an ics file for emailing to users who have booked on to an event.
   * <br>
   * Note: This method may return null in the event we cannot communicate with a third party service.
   *
   * @param event          - the event booked on
   * @param bookingDetails - the booking details.
   * @return email attachment containing an ics file.
   */
  private EmailAttachment generateEventICSFile(final IsaacEventPageDTO event, final EventBookingDTO bookingDetails) {

    try {
      // note this library will go out to a third part to get a sensible timezone value.
      TimezoneAssignment london = TimezoneAssignment.download(TimeZone.getTimeZone(DEFAULT_TIME_LOCALITY), false);

      ICalendar ical = new ICalendar();
      ical.getTimezoneInfo().setDefaultTimezone(london);

      VEvent icalEvent = new VEvent();
      icalEvent.setSummary(event.getTitle());
      icalEvent.setDateStart(event.getDate(), true);
      icalEvent.setDateEnd(event.getEndDate(), true);
      icalEvent.setDescription(event.getSubtitle());

      icalEvent.setOrganizer(new Organizer(propertiesLoader.getProperty(MAIL_NAME),
          propertiesLoader.getProperty(EVENT_ADMIN_EMAIL)));
      icalEvent.setUid(String.format("%s@%s.%s", bookingDetails.getUserBooked().getId(),
          event.getId(), propertiesLoader.getProperty(EVENT_ICAL_UID_DOMAIN)));
      icalEvent.setUrl(String.format("https://%s/events/%s",
          propertiesLoader.getProperty(HOST_NAME), event.getId()));

      if (event.getLocation() != null && event.getAddress() != null) {
        icalEvent.setLocation(event.getLocation().getAddress().toString());
      }

      ical.addEvent(icalEvent);
      return new EmailAttachment("event.ics",
          "text/calendar; charset=\"utf-8\"; method=PUBLISH", Biweekly.write(ical).go());
    } catch (IllegalArgumentException e) {
      log.error("Unable to generate ics file for event email", e);
      return null;
    }
  }

  /**
   * Helper to generate a url with a pre-generated subject field for the contact page.
   *
   * @param event - the event of interest
   * @return customised contactUs url for the event.
   */
  private String generateEventContactUsURL(final IsaacEventPageDTO event) {
    String defaultURL = String.format("https://%s/contact", propertiesLoader.getProperty(HOST_NAME));
    if (event.getDate() == null) {
      return defaultURL;
    }

    DateFormat shortDateFormatter = DateFormat.getDateInstance(DateFormat.SHORT);
    String location = event.getLocation() != null
        && event.getLocation().getAddress() != null
        && event.getLocation().getAddress().getAddressLine1() != null
        ? event.getLocation().getAddress().getAddressLine1()
        : "";

    String contactUsSubject = "Event - " + location + " - " + shortDateFormatter.format(event.getDate());

    return String.format("https://%s/contact?subject=%s",
        propertiesLoader.getProperty(HOST_NAME),
        URLEncoder.encode(contactUsSubject, StandardCharsets.UTF_8));

  }

  /**
   * Helper method to add the group association made when a user books on an event.
   *
   * @param event context
   * @param user  to add
   * @throws SegueDatabaseException if there is a database error.
   */
  private void addUserToEventGroup(final IsaacEventPageDTO event, final RegisteredUserDTO user)
      throws SegueDatabaseException {
    // auto add them to the group and grant the owner permission
    if (event.getIsaacGroupToken() != null && !event.getIsaacGroupToken().isEmpty()) {
      try {
        this.userAssociationManager.createAssociationWithToken(event.getIsaacGroupToken(), user);
      } catch (InvalidUserAssociationTokenException e) {
        log.error(String.format("Unable to auto add user (%s) to event (%s) using token as the token is invalid.",
            user.getEmail(), event.getTitle()));
      }
    }
  }

  /**
   * Helper method to undo the group association made when a user books on an event.
   *
   * @param event context
   * @param user  to remove
   * @throws SegueDatabaseException if there is a database error.
   */
  private void removeUserFromEventGroup(final IsaacEventPageDTO event, final RegisteredUserDTO user)
      throws SegueDatabaseException {
    try {
      // auto remove them from the group
      if (event.getIsaacGroupToken() != null && !event.getIsaacGroupToken().isEmpty()) {
        AssociationToken associationToken =
            this.userAssociationManager.lookupTokenDetails(user, event.getIsaacGroupToken());
        UserGroupDTO group = this.groupManager.getGroupById(associationToken.getGroupId());
        if (group != null) {
          this.groupManager.removeUserFromGroup(group, user);
        }
      }
    } catch (InvalidUserAssociationTokenException e) {
      log.error(String.format("Unable to auto remove user (%s) from event (%s) using token as the token is invalid.",
          user.getEmail(), event.getTitle()));
    }
  }
}
