package uk.ac.cam.cl.dtg.isaac.api.managers;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static uk.ac.cam.cl.dtg.isaac.api.Constants.EMAIL_TEMPLATE_TOKEN_AUTHORIZATION_LINK;
import static uk.ac.cam.cl.dtg.isaac.api.Constants.EMAIL_TEMPLATE_TOKEN_CONTACT_US_URL;
import static uk.ac.cam.cl.dtg.isaac.api.Constants.EMAIL_TEMPLATE_TOKEN_EVENT;
import static uk.ac.cam.cl.dtg.isaac.api.Constants.EMAIL_TEMPLATE_TOKEN_EVENT_DETAILS;
import static uk.ac.cam.cl.dtg.segue.api.Constants.EVENT_ADMIN_EMAIL;
import static uk.ac.cam.cl.dtg.segue.api.Constants.EVENT_ICAL_UID_DOMAIN;
import static uk.ac.cam.cl.dtg.segue.api.Constants.HOST_NAME;
import static uk.ac.cam.cl.dtg.segue.api.Constants.MAIL_NAME;

import com.google.api.client.util.Maps;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.easymock.EasyMock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.ac.cam.cl.dtg.isaac.dao.EventBookingPersistenceManager;
import uk.ac.cam.cl.dtg.isaac.dos.AssociationToken;
import uk.ac.cam.cl.dtg.isaac.dos.EventStatus;
import uk.ac.cam.cl.dtg.isaac.dos.ITransaction;
import uk.ac.cam.cl.dtg.isaac.dos.eventbookings.BookingStatus;
import uk.ac.cam.cl.dtg.isaac.dos.users.EmailVerificationStatus;
import uk.ac.cam.cl.dtg.isaac.dos.users.Role;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacEventPageDTO;
import uk.ac.cam.cl.dtg.isaac.dto.UserGroupDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.EmailTemplateDTO;
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
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

/**
 * EventBookingManagerTest.
 */
class EventBookingManagerTest {
  private static final Date someFutureDate = new Date(System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000);
  private static final Date someLessFutureDate = new Date(System.currentTimeMillis() + 6 * 24 * 60 * 60 * 1000);
  private static final Date someMoreFutureDate = new Date(System.currentTimeMillis() + 8 * 24 * 60 * 60 * 1000);
  private static final DateFormat urlDateFormatter = DateFormat.getDateInstance(DateFormat.SHORT);
  private static final String urlDate = urlDateFormatter.format(someFutureDate).replace("/", "%2F");

  private EventBookingPersistenceManager dummyEventBookingPersistenceManager;
  private EmailManager dummyEmailManager;
  private UserAssociationManager dummyUserAssociationManager;
  private Map<String, String> someAdditionalInformation;
  private PropertiesLoader dummyPropertiesLoader;
  private GroupManager dummyGroupManager;
  private IUserAccountManager dummyUserAccountManager;
  private ITransactionManager dummyTransactionManager;
  private ITransaction dummyTransaction;
  private Object[] mockedObjects;

  /**
   * Initial configuration of tests.
   */
  @BeforeEach
  public final void setUp() {
    this.dummyEmailManager = createMock(EmailManager.class);
    this.dummyEventBookingPersistenceManager = createMock(EventBookingPersistenceManager.class);
    this.dummyUserAssociationManager = createMock(UserAssociationManager.class);
    this.dummyGroupManager = createMock(GroupManager.class);
    this.dummyPropertiesLoader = createMock(PropertiesLoader.class);
    this.dummyUserAccountManager = createMock(IUserAccountManager.class);
    this.dummyTransactionManager = createMock(ITransactionManager.class);
    this.dummyTransaction = createMock(ITransaction.class);
    // Replay and Verify this collection of mocks each test to be sure of catching all behaviour!
    this.mockedObjects = new Object[] {
        dummyEmailManager, dummyEventBookingPersistenceManager, dummyUserAssociationManager, dummyGroupManager,
        dummyPropertiesLoader, dummyUserAccountManager, dummyTransactionManager, dummyTransaction
    };

    expect(this.dummyPropertiesLoader.getProperty(HOST_NAME)).andReturn("hostname.com").anyTimes();
    expect(this.dummyPropertiesLoader.getProperty(MAIL_NAME)).andReturn("Isaac Computer Science").anyTimes();
    expect(this.dummyPropertiesLoader.getProperty(EVENT_ADMIN_EMAIL)).andReturn("admin@hostname.com").anyTimes();
    expect(this.dummyPropertiesLoader.getProperty(EVENT_ICAL_UID_DOMAIN)).andReturn("hostname.com").anyTimes();
    this.someAdditionalInformation = Maps.newHashMap();
  }

  @Test
  void requestBooking_checkTeacherAllowedOnStudentEventDespiteCapacityFull_noExceptionThrown() throws
      Exception {
    EventBookingManager ebm = this.buildEventBookingManager();
    IsaacEventPageDTO testEvent = prepareIsaacEventPageDtoWithEventDetails(studentCSTags);

    RegisteredUserDTO someUser = new RegisteredUserDTO();
    someUser.setId(6L);
    someUser.setEmailVerificationStatus(EmailVerificationStatus.VERIFIED);
    someUser.setRole(Role.TEACHER);

    RegisteredUserDTO someStudentUser = new RegisteredUserDTO();
    someStudentUser.setId(1L);
    someStudentUser.setEmailVerificationStatus(EmailVerificationStatus.VERIFIED);
    someStudentUser.setRole(Role.STUDENT);

    EventBookingDTO firstBooking = prepareEventBookingDto(someStudentUser.getId(), BookingStatus.CONFIRMED,
        Role.STUDENT);

    Map<BookingStatus, Map<Role, Long>> placesAvailableMap = generatePlacesAvailableMap();
    placesAvailableMap.get(BookingStatus.CONFIRMED).put(Role.STUDENT, 1L);
    expect(dummyEventBookingPersistenceManager.getEventBookingStatusCounts(testEvent.getId(), false)).andReturn(
        placesAvailableMap).atLeastOnce();

    expect(dummyEventBookingPersistenceManager.getBookingByEventIdAndUserId(testEvent.getId(), someUser.getId()))
        .andReturn(null).once();

    dummyEventBookingPersistenceManager.lockEventUntilTransactionComplete(dummyTransaction, testEvent.getId());
    expectLastCall().once();
    expect(dummyTransactionManager.getTransaction()).andReturn(dummyTransaction).once();
    dummyTransaction.commit();
    expectLastCall().once();
    dummyTransaction.close();
    expectLastCall().once();

    expect(dummyEventBookingPersistenceManager.createBooking(dummyTransaction, testEvent.getId(), someUser.getId(),
        BookingStatus
            .CONFIRMED, someAdditionalInformation)).andReturn(firstBooking).atLeastOnce();

    expect(dummyEmailManager.getEmailTemplateDTO("email-event-booking-confirmed")).andReturn(new EmailTemplateDTO())
        .atLeastOnce();

    dummyEmailManager.sendTemplatedEmailToUser(anyObject(), anyObject(), anyObject(), anyObject(), anyObject());
    expectLastCall().atLeastOnce();

    replay(mockedObjects);
    ebm.requestBooking(testEvent, someUser, someAdditionalInformation);
    verify(mockedObjects);
  }

  @Test
  void requestBooking_checkStudentNotAllowedOnStudentEventAsCapacityFull_eventFullExceptionThrown() throws
      Exception {
    EventBookingManager ebm = this.buildEventBookingManager();
    IsaacEventPageDTO testEvent = prepareIsaacEventPageDto(studentCSTags);

    RegisteredUserDTO someUser = new RegisteredUserDTO();
    someUser.setId(6L);
    someUser.setEmailVerificationStatus(EmailVerificationStatus.VERIFIED);
    someUser.setRole(Role.STUDENT);

    Map<BookingStatus, Map<Role, Long>> placesAvailableMap = generatePlacesAvailableMap();
    placesAvailableMap.get(BookingStatus.CONFIRMED).put(Role.STUDENT, 1L);
    expect(dummyEventBookingPersistenceManager.getEventBookingStatusCounts(testEvent.getId(), false)).andReturn(
        placesAvailableMap).atLeastOnce();

    expect(dummyEventBookingPersistenceManager.getBookingByEventIdAndUserId(testEvent.getId(),
        someUser.getId())).andReturn(null)
        .once();

    dummyEventBookingPersistenceManager.lockEventUntilTransactionComplete(dummyTransaction, testEvent.getId());
    expectLastCall().once();
    expect(dummyTransactionManager.getTransaction()).andReturn(dummyTransaction).once();
    dummyTransaction.commit();
    expectLastCall().once();
    dummyTransaction.close();
    expectLastCall().once();

    replay(mockedObjects);
    try {
      ebm.requestBooking(testEvent, someUser, someAdditionalInformation);
      fail("Expected an EventFullException and one didn't happen.");
    } catch (EventIsFullException e) {
      // success !
    }
    verify(dummyEventBookingPersistenceManager);
  }

  @Test
  void requestBooking_checkTeacherNotAllowedOnTeacherEventAsCapacityFull_eventFullExceptionThrown() throws
      Exception {
    EventBookingManager ebm = this.buildEventBookingManager();
    IsaacEventPageDTO testEvent = prepareIsaacEventPageDto(teacherCSTags);

    RegisteredUserDTO someUser = new RegisteredUserDTO();
    someUser.setId(6L);
    someUser.setEmailVerificationStatus(EmailVerificationStatus.VERIFIED);
    someUser.setRole(Role.TEACHER);

    Map<BookingStatus, Map<Role, Long>> placesAvailableMap = generatePlacesAvailableMap();
    placesAvailableMap.get(BookingStatus.CONFIRMED).put(Role.TEACHER, 1L);
    expect(dummyEventBookingPersistenceManager.getEventBookingStatusCounts(testEvent.getId(), false)).andReturn(
        placesAvailableMap).atLeastOnce();

    expect(dummyEventBookingPersistenceManager.getBookingByEventIdAndUserId(testEvent.getId(),
        someUser.getId())).andReturn(null)
        .once();

    dummyEventBookingPersistenceManager.lockEventUntilTransactionComplete(dummyTransaction, testEvent.getId());
    expectLastCall().once();
    expect(dummyTransactionManager.getTransaction()).andReturn(dummyTransaction).once();
    dummyTransaction.commit();
    expectLastCall().once();
    dummyTransaction.close();
    expectLastCall().once();

    replay(mockedObjects);
    try {
      ebm.requestBooking(testEvent, someUser, someAdditionalInformation);
      fail("Expected an EventFullException and one didn't happen.");
    } catch (EventIsFullException e) {
      // success !
    }
    verify(dummyEventBookingPersistenceManager);
  }

  @Test
  void requestBooking_addressNotVerified_addressNotVerifiedExceptionThrown() throws Exception {
    EventBookingManager ebm = this.buildEventBookingManager();
    IsaacEventPageDTO testEvent = prepareIsaacEventPageDto(studentCSTags);

    RegisteredUserDTO someUser = new RegisteredUserDTO();
    someUser.setId(6L);
    someUser.setEmailVerificationStatus(EmailVerificationStatus.NOT_VERIFIED);
    someUser.setRole(Role.STUDENT);

    try {
      ebm.requestBooking(testEvent, someUser, someAdditionalInformation);
      fail("Expected an EventFullException and one didn't happen.");
    } catch (EmailMustBeVerifiedException e) {
      // success !
    }
  }

  @Test
  void requestBooking_expiredBooking_EventExpiredExceptionThrown() throws Exception {
    EventBookingManager ebm = this.buildEventBookingManager();
    IsaacEventPageDTO testEvent = prepareIsaacEventPageDto(studentCSTags);

    // old deadline
    Date old = new Date();
    old.setTime(958074310000L);
    testEvent.setBookingDeadline(old);

    RegisteredUserDTO someUser = new RegisteredUserDTO();
    someUser.setId(6L);
    someUser.setEmailVerificationStatus(EmailVerificationStatus.NOT_VERIFIED);
    someUser.setRole(Role.STUDENT);

    replay(mockedObjects);
    try {
      ebm.requestBooking(testEvent, someUser, someAdditionalInformation);
      fail("Expected an Event Expiry Exception and one didn't happen.");
    } catch (EventDeadlineException e) {
      // success !
    }
    verify(dummyEventBookingPersistenceManager);
  }

  @Test
  void requestBooking_cancelledSpaceAndWaitingList_SpaceRemainsFull() throws Exception {
    EventBookingManager ebm = this.buildEventBookingManager();
    IsaacEventPageDTO testEvent = prepareIsaacEventPageDto(teacherCSTags);

    RegisteredUserDTO someUser = new RegisteredUserDTO();
    someUser.setId(6L);
    someUser.setEmailVerificationStatus(EmailVerificationStatus.VERIFIED);
    someUser.setRole(Role.TEACHER);

    Map<BookingStatus, Map<Role, Long>> placesAvailableMap = generatePlacesAvailableMap();
    placesAvailableMap.get(BookingStatus.WAITING_LIST).put(Role.TEACHER, 1L);
    placesAvailableMap.get(BookingStatus.CANCELLED).put(Role.TEACHER, 1L);
    expect(dummyEventBookingPersistenceManager.getEventBookingStatusCounts(testEvent.getId(), false)).andReturn(
        placesAvailableMap).atLeastOnce();

    expect(dummyEventBookingPersistenceManager.getBookingByEventIdAndUserId(testEvent.getId(),
        someUser.getId())).andReturn(null)
        .once();

    dummyEventBookingPersistenceManager.lockEventUntilTransactionComplete(dummyTransaction, testEvent.getId());
    expectLastCall().once();
    expect(dummyTransactionManager.getTransaction()).andReturn(dummyTransaction).once();
    dummyTransaction.commit();
    expectLastCall().once();
    dummyTransaction.close();
    expectLastCall().once();

    replay(mockedObjects);
    try {
      ebm.requestBooking(testEvent, someUser, someAdditionalInformation);
      fail("Expected an EventFullException and one didn't happen.");
    } catch (EventIsFullException e) {
      // success !
    }
    verify(dummyEventBookingPersistenceManager);
  }

  @Test
  void requestBooking_cancelledSpaceAndNoWaitingList_Success() throws Exception {
    EventBookingManager ebm = this.buildEventBookingManager();
    IsaacEventPageDTO testEvent = prepareIsaacEventPageDtoWithEventDetails(studentCSTags);

    RegisteredUserDTO someUser = new RegisteredUserDTO();
    someUser.setId(6L);
    someUser.setEmailVerificationStatus(EmailVerificationStatus.VERIFIED);
    someUser.setRole(Role.TEACHER);

    EventBookingDTO secondBooking = prepareEventBookingDto(7L, BookingStatus.CANCELLED, Role.TEACHER);

    Map<BookingStatus, Map<Role, Long>> placesAvailableMap = generatePlacesAvailableMap();
    placesAvailableMap.get(BookingStatus.CANCELLED).put(Role.TEACHER, 1L);
    expect(dummyEventBookingPersistenceManager.getEventBookingStatusCounts(testEvent.getId(), false)).andReturn(
        placesAvailableMap).atLeastOnce();

    expect(dummyEventBookingPersistenceManager.getBookingByEventIdAndUserId(testEvent.getId(), someUser.getId()))
        .andReturn(null).once();

    dummyEventBookingPersistenceManager.lockEventUntilTransactionComplete(dummyTransaction, testEvent.getId());
    expectLastCall().once();
    expect(dummyTransactionManager.getTransaction()).andReturn(dummyTransaction).once();
    dummyTransaction.commit();
    expectLastCall().once();
    dummyTransaction.close();
    expectLastCall().once();

    expect(dummyEventBookingPersistenceManager.createBooking(dummyTransaction, testEvent.getId(), someUser.getId(),
        BookingStatus.CONFIRMED, someAdditionalInformation)).andReturn(secondBooking).atLeastOnce();

    expect(dummyEmailManager.getEmailTemplateDTO("email-event-booking-confirmed")).andReturn(new EmailTemplateDTO())
        .atLeastOnce();

    dummyEmailManager.sendTemplatedEmailToUser(anyObject(), anyObject(), anyObject(), anyObject(), anyObject());
    expectLastCall().atLeastOnce();

    replay(mockedObjects);
    try {
      ebm.requestBooking(testEvent, someUser, someAdditionalInformation);
      // success
    } catch (EventIsFullException e) {
      fail("Expected successful booking as no waiting list bookings.");
    }
    verify(mockedObjects);
  }

  @Test
  void requestBooking_cancelledSpaceAndSomeWaitingList_Success() throws Exception {
    EventBookingManager ebm = this.buildEventBookingManager();
    IsaacEventPageDTO testEvent = prepareIsaacEventPageDtoWithEventDetails(teacherCSTags, 2);

    RegisteredUserDTO firstUserFull = new RegisteredUserDTO();
    firstUserFull.setId(6L);
    firstUserFull.setEmailVerificationStatus(EmailVerificationStatus.VERIFIED);
    firstUserFull.setRole(Role.TEACHER);

    UserSummaryDTO firstUser = prepareUserSummaryDto(firstUserFull.getId(), Role.TEACHER);
    DetailedEventBookingDTO firstBooking = prepareDetailedEventBookingDto(firstUser, BookingStatus.WAITING_LIST);
    EventBookingDTO secondBooking = prepareEventBookingDto(firstUser, BookingStatus.CANCELLED);

    Map<BookingStatus, Map<Role, Long>> placesAvailableMap = generatePlacesAvailableMap();
    placesAvailableMap.get(BookingStatus.CANCELLED).put(Role.TEACHER, 1L);
    placesAvailableMap.get(BookingStatus.WAITING_LIST).put(Role.TEACHER, 1L);
    expect(dummyEventBookingPersistenceManager.getEventBookingStatusCounts(testEvent.getId(), false)).andReturn(
        placesAvailableMap).atLeastOnce();

    expect(dummyEventBookingPersistenceManager.getBookingByEventIdAndUserId(testEvent.getId(), firstUserFull
        .getId())).andReturn(firstBooking).once();

    dummyEventBookingPersistenceManager.lockEventUntilTransactionComplete(dummyTransaction, testEvent.getId());
    expectLastCall().once();
    expect(dummyTransactionManager.getTransaction()).andReturn(dummyTransaction).once();
    dummyTransaction.commit();
    expectLastCall().once();
    dummyTransaction.close();
    expectLastCall().once();

    expect(dummyEventBookingPersistenceManager.createBooking(dummyTransaction, testEvent.getId(), firstUserFull.getId(),
        BookingStatus.CONFIRMED, someAdditionalInformation)).andReturn(secondBooking).atLeastOnce();

    dummyEmailManager.sendTemplatedEmailToUser(anyObject(), anyObject(), anyObject(), anyObject(), anyObject());
    expectLastCall().atLeastOnce();

    expect(dummyEmailManager.getEmailTemplateDTO("email-event-booking-confirmed")).andReturn(new EmailTemplateDTO())
        .atLeastOnce();

    replay(mockedObjects);
    try {
      ebm.requestBooking(testEvent, firstUserFull, someAdditionalInformation);
      // success
    } catch (EventIsFullException e) {
      fail("Expected successful booking as no waiting list bookings.");
    }
    verify(mockedObjects);
  }

  @Test
  void requestBooking_userIsAbleToPromoteBookingReservation_Success() throws Exception {
    EventBookingManager eventBookingManager = this.buildEventBookingManager();
    ReservationTestDefaults testCase = new ReservationTestDefaults();
    testCase.event.setNumberOfPlaces(1);

    RegisteredUserDTO reservedStudent = testCase.student1;
    DetailedEventBookingDTO reservedStudentBooking =
        prepareDetailedEventBookingDto(reservedStudent.getId(), BookingStatus.RESERVED, testCase.event.getId());
    DetailedEventBookingDTO reservedStudentBookingAfterConfirmation =
        prepareDetailedEventBookingDto(reservedStudent.getId(), BookingStatus.CONFIRMED, testCase.event.getId());

    // Expected external calls
    dummyEventBookingPersistenceManager.lockEventUntilTransactionComplete(dummyTransaction, testCase.event.getId());
    expectLastCall().once();
    expect(dummyTransactionManager.getTransaction()).andReturn(dummyTransaction).once();
    dummyTransaction.commit();
    expectLastCall().once();
    dummyTransaction.close();
    expectLastCall().once();

    expect(dummyEventBookingPersistenceManager
        .getBookingByEventIdAndUserId(testCase.event.getId(), reservedStudent.getId()))
        .andReturn(reservedStudentBooking).once();
    // As a reserved booking exists, expect an update to the booking
    expect(dummyEventBookingPersistenceManager
        .updateBookingStatus(eq(dummyTransaction), eq(testCase.event.getId()), eq(reservedStudent.getId()),
            eq(BookingStatus.CONFIRMED), anyObject()))
        .andReturn(reservedStudentBookingAfterConfirmation).once();
    // Send emails
    EmailTemplateDTO emailTemplate = new EmailTemplateDTO();
    expect(dummyEmailManager.getEmailTemplateDTO("email-event-booking-confirmed")).andReturn(emailTemplate).once();
    dummyEmailManager.sendTemplatedEmailToUser(eq(reservedStudent), eq(emailTemplate), anyObject(),
        eq(EmailType.SYSTEM), anyObject());
    expectLastCall().once();

    replay(mockedObjects);
    eventBookingManager.requestBooking(testCase.event, reservedStudent, someAdditionalInformation);
    verify(mockedObjects);
  }

  @Test
  void promoteBooking_spaceDueToCancellation_Success() throws Exception {
    EventBookingManager ebm = this.buildEventBookingManager();
    IsaacEventPageDTO testEvent = prepareIsaacEventPageDtoWithEventDetails(teacherCSTags);

    RegisteredUserDTO someUser = new RegisteredUserDTO();
    someUser.setId(6L);
    someUser.setEmailVerificationStatus(EmailVerificationStatus.VERIFIED);
    someUser.setRole(Role.TEACHER);

    UserSummaryDTO firstUser = prepareUserSummaryDto(6L, Role.TEACHER);
    DetailedEventBookingDTO firstBooking =
        prepareDetailedEventBookingDto(firstUser, BookingStatus.WAITING_LIST, testEvent.getId());
    firstBooking.setAdditionalInformation(someAdditionalInformation);
    DetailedEventBookingDTO secondBooking =
        prepareDetailedEventBookingDto(firstUser, BookingStatus.CANCELLED, testEvent.getId());
    secondBooking.setAdditionalInformation(someAdditionalInformation);

    Map<BookingStatus, Map<Role, Long>> placesAvailableMap = generatePlacesAvailableMap();
    placesAvailableMap.get(BookingStatus.CANCELLED).put(Role.TEACHER, 1L);
    placesAvailableMap.get(BookingStatus.WAITING_LIST).put(Role.TEACHER, 1L);
    expect(dummyEventBookingPersistenceManager.getEventBookingStatusCounts(testEvent.getId(), false)).andReturn(
        placesAvailableMap).atLeastOnce();

    expect(dummyEventBookingPersistenceManager.getBookingByEventIdAndUserId(testEvent.getId(), 6L))
        .andReturn(firstBooking).once();

    dummyEventBookingPersistenceManager.lockEventUntilTransactionComplete(dummyTransaction, testEvent.getId());
    expectLastCall().once();
    expect(dummyTransactionManager.getTransaction()).andReturn(dummyTransaction).once();
    dummyTransaction.commit();
    expectLastCall().once();
    dummyTransaction.close();
    expectLastCall().once();

    expect(
        dummyEventBookingPersistenceManager.updateBookingStatus(dummyTransaction, testEvent.getId(), someUser.getId(),
            BookingStatus.CONFIRMED, someAdditionalInformation)).andReturn(secondBooking).atLeastOnce();

    expect(dummyEmailManager.getEmailTemplateDTO("email-event-booking-waiting-list-promotion-confirmed"))
        .andReturn(new EmailTemplateDTO()).atLeastOnce();

    dummyEmailManager.sendTemplatedEmailToUser(anyObject(), anyObject(), anyObject(), anyObject(), anyObject());
    expectLastCall().atLeastOnce();

    replay(mockedObjects);
    try {
      ebm.promoteToConfirmedBooking(testEvent, someUser);
      // success
    } catch (EventIsFullException e) {
      fail("Expected successful booking as no waiting list bookings.");
    }
    verify(mockedObjects);
  }

  @Test
  void promoteBooking_NoSpace_Failure() throws Exception {
    EventBookingManager ebm = this.buildEventBookingManager();
    IsaacEventPageDTO testEvent = prepareIsaacEventPageDto(teacherCSTags);

    RegisteredUserDTO someUser = new RegisteredUserDTO();
    someUser.setId(6L);
    someUser.setEmailVerificationStatus(EmailVerificationStatus.VERIFIED);
    someUser.setRole(Role.TEACHER);

    UserSummaryDTO firstUser = prepareUserSummaryDto(6L, Role.TEACHER);
    DetailedEventBookingDTO firstBooking =
        prepareDetailedEventBookingDto(firstUser, BookingStatus.WAITING_LIST, testEvent.getId());

    Map<BookingStatus, Map<Role, Long>> placesAvailableMap = generatePlacesAvailableMap();
    placesAvailableMap.get(BookingStatus.CONFIRMED).put(Role.TEACHER, 1L);
    placesAvailableMap.get(BookingStatus.WAITING_LIST).put(Role.TEACHER, 1L);
    expect(dummyEventBookingPersistenceManager.getEventBookingStatusCounts(testEvent.getId(), false)).andReturn(
        placesAvailableMap).atLeastOnce();

    expect(dummyEventBookingPersistenceManager.getBookingByEventIdAndUserId(testEvent.getId(), 6L)).andReturn(
        firstBooking);

    dummyEventBookingPersistenceManager.lockEventUntilTransactionComplete(dummyTransaction, testEvent.getId());
    expectLastCall().once();
    expect(dummyTransactionManager.getTransaction()).andReturn(dummyTransaction).once();
    // No commit expected; exception thrown!
    dummyTransaction.close();
    expectLastCall().once();

    replay(mockedObjects);
    try {
      ebm.promoteToConfirmedBooking(testEvent, someUser);
      fail("Expected failure booking as no space for this event.");
    } catch (EventIsFullException e) {
      // success
    }
    verify(mockedObjects);
  }

  @Test
  void getPlacesAvailable_checkEventCapacity_capacityCalculatedCorrectly() throws Exception {
    // Create a future event and event booking manager
    EventBookingManager ebm = this.buildEventBookingManager();
    int initialNumberOfPlaces = 1000;
    IsaacEventPageDTO testEvent =
        prepareIsaacEventPageDto(ImmutableSet.of("student"), initialNumberOfPlaces, EventStatus.OPEN);

    // Mock the event booking status count result from the event booking persistence manager
    Map<BookingStatus, Map<Role, Long>> placesAvailableMap = generatePlacesAvailableMap();
    // Student places
    placesAvailableMap.get(BookingStatus.CONFIRMED).put(Role.STUDENT, 1L);
    placesAvailableMap.get(BookingStatus.WAITING_LIST).put(Role.STUDENT, 10L);
    placesAvailableMap.get(BookingStatus.CANCELLED).put(Role.STUDENT, 100L);
    // Teacher places
    placesAvailableMap.get(BookingStatus.CONFIRMED).put(Role.TEACHER, 2L);
    placesAvailableMap.get(BookingStatus.WAITING_LIST).put(Role.TEACHER, 20L);
    placesAvailableMap.get(BookingStatus.CANCELLED).put(Role.TEACHER, 200L);

    expect(dummyEventBookingPersistenceManager.getEventBookingStatusCounts(testEvent.getId(), false))
        .andReturn(placesAvailableMap).atLeastOnce();

    // Run the test for a student event
    replay(mockedObjects);
    Long actualPlacesAvailable = ebm.getPlacesAvailable(testEvent);
    Long expectedPlacesAvailable = (long) initialNumberOfPlaces - 1 - 10;
    assertEquals(
        expectedPlacesAvailable, actualPlacesAvailable,
        "STUDENT events should only count confirmed and waiting list student places in availability calculations");
    verify(mockedObjects);
  }

  @Test
  void getEventPage_checkWaitingListOnlyEventCapacity_capacityCalculatedCorrectly() throws
      Exception {
    EventBookingManager ebm = this.buildEventBookingManager();
    IsaacEventPageDTO testEvent = prepareIsaacEventPageDto(studentCSTags, 2, EventStatus.WAITING_LIST_ONLY);

    RegisteredUserDTO someUser = new RegisteredUserDTO();
    someUser.setId(6L);
    someUser.setEmailVerificationStatus(EmailVerificationStatus.VERIFIED);
    someUser.setRole(Role.STUDENT);

    Map<BookingStatus, Map<Role, Long>> placesAvailableMap = generatePlacesAvailableMap();
    placesAvailableMap.get(BookingStatus.CONFIRMED).put(Role.STUDENT, 1L);
    placesAvailableMap.get(BookingStatus.WAITING_LIST).put(Role.STUDENT, 1L);
    expect(dummyEventBookingPersistenceManager.getEventBookingStatusCounts(testEvent.getId(), false)).andReturn(
        placesAvailableMap).atLeastOnce();

    replay(mockedObjects);
    Long placesAvailable = ebm.getPlacesAvailable(testEvent);
    Long expectedPlacesAvailable = 1L;
    assertEquals(expectedPlacesAvailable, placesAvailable,
        "WAITING_LIST_ONLY events should only count confirmed places in availability calculations");
    verify(mockedObjects);
  }

  @Test
  void getEventPage_checkStudentEventReservedBookings_capacityCalculatedCorrectly() throws
      Exception {
    EventBookingManager ebm = this.buildEventBookingManager();
    IsaacEventPageDTO testEvent = prepareIsaacEventPageDto(studentCSTags, 2, EventStatus.OPEN);
    testEvent.setAllowGroupReservations(true);

    // Mocks the counts for the places available calculation from the database
    Map<BookingStatus, Map<Role, Long>> placesAvailableMap = generatePlacesAvailableMap();

    RegisteredUserDTO someUser = new RegisteredUserDTO();
    someUser.setId(6L);
    someUser.setEmailVerificationStatus(EmailVerificationStatus.VERIFIED);
    someUser.setRole(Role.STUDENT);

    placesAvailableMap.get(BookingStatus.RESERVED).put(Role.STUDENT, 1L);
    placesAvailableMap.get(BookingStatus.CONFIRMED).put(Role.STUDENT, 1L);

    expect(dummyEventBookingPersistenceManager.getEventBookingStatusCounts(testEvent.getId(), false)).andReturn(
        placesAvailableMap).atLeastOnce();

    replay(mockedObjects);
    Long placesAvailable = ebm.getPlacesAvailable(testEvent);
    Long expectedPlacesAvailable = 0L;
    assertEquals(expectedPlacesAvailable, placesAvailable,
        "RESERVED bookings should count towards the places available in availability calculations");
    verify(mockedObjects);
  }

  @Test
  void isUserAbleToManageEvent_checkUsersWithDifferentRoles_success() throws Exception {
    EventBookingManager eventBookingManager = this.buildEventBookingManager();

    // Users to test
    RegisteredUserDTO teacher = new RegisteredUserDTO();
    teacher.setId(1L);
    teacher.setRole(Role.TEACHER);

    RegisteredUserDTO unassociatedEventLeader = new RegisteredUserDTO();
    unassociatedEventLeader.setId(2L);
    unassociatedEventLeader.setRole(Role.EVENT_LEADER);

    RegisteredUserDTO associatedEventLeader = new RegisteredUserDTO();
    associatedEventLeader.setId(3L);
    associatedEventLeader.setRole(Role.EVENT_LEADER);

    RegisteredUserDTO eventManager = new RegisteredUserDTO();
    eventManager.setId(4L);
    eventManager.setRole(Role.EVENT_MANAGER);

    RegisteredUserDTO admin = new RegisteredUserDTO();
    admin.setId(5L);
    admin.setRole(Role.ADMIN);

    // Event and associated group
    UserGroupDTO testEventGroup = new UserGroupDTO();
    testEventGroup.setId(0L);
    testEventGroup.setOwnerId(associatedEventLeader.getId());

    AssociationToken testAssociationToken = new AssociationToken();
    testAssociationToken.setToken("EVENT_GROUP_TOKEN");
    testAssociationToken.setOwnerUserId(testEventGroup.getOwnerId());
    testAssociationToken.setGroupId(testEventGroup.getId());

    IsaacEventPageDTO testEvent = new IsaacEventPageDTO();
    testEvent.setId("someEventId");
    testEvent.setIsaacGroupToken(testAssociationToken.getToken());

    // Expected mocked method calls
    expect(dummyUserAssociationManager.lookupTokenDetails(unassociatedEventLeader, testEvent.getIsaacGroupToken()))
        .andReturn(testAssociationToken);
    expect(dummyGroupManager.getGroupById(testAssociationToken.getGroupId()))
        .andReturn(testEventGroup);

    expect(dummyUserAssociationManager.lookupTokenDetails(associatedEventLeader, testEvent.getIsaacGroupToken()))
        .andReturn(testAssociationToken);
    expect(dummyGroupManager.getGroupById(testAssociationToken.getGroupId()))
        .andReturn(testEventGroup);

    replay(mockedObjects);

    // Expected results
    Map<RegisteredUserDTO, Boolean> expectedResults = ImmutableMap.of(
        teacher, false,
        unassociatedEventLeader, false,
        associatedEventLeader, true,
        eventManager, true,
        admin, true
    );

    for (RegisteredUserDTO user : expectedResults.keySet()) {
      assertEquals(expectedResults.get(user),
          eventBookingManager.isUserAbleToManageEvent(user, testEvent),
          String.format("Test case: %s", user.getRole()));
    }
    verify(mockedObjects);
  }

  @Test
  void eventAllowsGroupBookings_checkAllCases_defaultIsFalseAndOtherwiseReportedCorrectly() {
    class TestCase {
      IsaacEventPageDTO eventPageDTO;
      Boolean expected;
      String assertion;
    }

    List<TestCase> testCases = ImmutableList.of(
        new TestCase() {{
          eventPageDTO = new IsaacEventPageDTO();
          expected = false;
          assertion = "The default case should return false";
        }},
        new TestCase() {{
          eventPageDTO = new IsaacEventPageDTO() {{
              setAllowGroupReservations(true);
            }};
          expected = true;
          assertion = "Events which allow group reservations should return true";
        }},
        new TestCase() {{
          eventPageDTO = new IsaacEventPageDTO() {{
              setAllowGroupReservations(false);
            }};
          expected = false;
          assertion = "Events which explicitly disallow group reservations should return false";
        }}
    );

    for (TestCase testCase : testCases) {
      boolean actual = EventBookingManager.eventAllowsGroupBookings(testCase.eventPageDTO);
      assertEquals(testCase.expected, actual, testCase.assertion);
    }
  }

  @Test
  void requestReservations_reserveSpacesWhenThereAreAvailableSpaces_success() throws Exception {
    EventBookingManager eventBookingManager = buildEventBookingManager();
    ReservationTestDefaults testCase = new ReservationTestDefaults();
    List<RegisteredUserDTO> students = ImmutableList.of(testCase.student1, testCase.student2);

    // Make student two have a cancelled booking
    DetailedEventBookingDTO student2sCancelledBooking =
        prepareDetailedEventBookingDto(testCase.student2.getId(), BookingStatus.CANCELLED, testCase.event.getId());

    // Define expected external calls
    dummyEventBookingPersistenceManager.lockEventUntilTransactionComplete(dummyTransaction, testCase.event.getId());
    expectLastCall().once();

    // Check existing bookings
    expect(dummyEventBookingPersistenceManager.getBookingsByEventId(testCase.event.getId())).andReturn(
        ImmutableList.of(student2sCancelledBooking)).once();


    // Make Reservations
    expect(dummyTransactionManager.getTransaction()).andReturn(dummyTransaction).once();

    expect(dummyEventBookingPersistenceManager
        .getBookingByEventIdAndUserId(testCase.event.getId(), testCase.student1.getId()))
        .andReturn(null).once();
    expect(dummyEventBookingPersistenceManager
        .createBooking(eq(dummyTransaction), eq(testCase.event.getId()), eq(testCase.student1.getId()),
            eq(testCase.teacher.getId()), eq(BookingStatus.RESERVED), anyObject()))
        .andReturn(testCase.student1Booking).once();

    expect(dummyEventBookingPersistenceManager
        .getBookingByEventIdAndUserId(testCase.event.getId(), testCase.student2.getId()))
        .andReturn(student2sCancelledBooking).once();
    expect(dummyEventBookingPersistenceManager
        .updateBookingStatus(eq(dummyTransaction), eq(testCase.event.getId()), eq(testCase.student2.getId()),
            eq(testCase.teacher.getId()), eq(BookingStatus.RESERVED), anyObject()))
        .andReturn(testCase.student2Booking).once();

    dummyTransaction.commit();
    expectLastCall().once();
    dummyTransaction.close();
    expectLastCall().once();

    // Send Emails
    expect(dummyEmailManager.getEmailTemplateDTO(("email-event-reservation-requested"))).andReturn(
        testCase.reservationEmail).atLeastOnce();
    expect(dummyEmailManager.getEmailTemplateDTO(("email-event-reservation-recap"))).andReturn(
        testCase.reservationEmail).atLeastOnce();

    expect(dummyUserAccountManager.getUserDTOById(testCase.student1.getId())).andReturn(testCase.student1).times(2);
    dummyEmailManager.sendTemplatedEmailToUser(eq(testCase.student1), eq(testCase.reservationEmail), anyObject(),
        eq(EmailType.SYSTEM));
    expectLastCall().once();

    expect(dummyUserAccountManager.getUserDTOById(testCase.student2.getId())).andReturn(testCase.student2).times(2);
    dummyEmailManager.sendTemplatedEmailToUser(eq(testCase.student2), eq(testCase.reservationEmail), anyObject(),
        eq(EmailType.SYSTEM));
    expectLastCall().once();

    dummyEmailManager.sendTemplatedEmailToUser(eq(testCase.teacher), eq(testCase.reservationEmail), anyObject(),
        eq(EmailType.SYSTEM));
    expectLastCall().once();

    // Run the test for a student event
    replay(mockedObjects);
    List<EventBookingDTO> actualResults =
        eventBookingManager.requestReservations(testCase.event, students, testCase.teacher);
    List<EventBookingDTO> expectedResults = ImmutableList.of(testCase.student1Booking, testCase.student2Booking);
    assertEquals(expectedResults, actualResults, "N results should be returned unaltered");
    verify(mockedObjects);
  }

  @Test
  void requestReservations_reserveSpacesForTwoWhenThereIsOnlyOneAvailableSpace_throwsEventIsFullException()
      throws Exception {
    EventBookingManager eventBookingManager = buildEventBookingManager();
    ReservationTestDefaults testCase = new ReservationTestDefaults();
    testCase.event.setNumberOfPlaces(1);
    List<RegisteredUserDTO> studentsToReserve = ImmutableList.of(testCase.student1, testCase.student2);

    // Define expected external calls
    expect(dummyTransactionManager.getTransaction()).andReturn(dummyTransaction).once();
    dummyEventBookingPersistenceManager.lockEventUntilTransactionComplete(dummyTransaction, testCase.event.getId());
    expectLastCall().atLeastOnce();
    // No commit expected; exception thrown!
    dummyTransaction.close();
    expectLastCall().once();

    expect(dummyEventBookingPersistenceManager.getEventBookingStatusCounts(testCase.event.getId(), false))
        .andReturn(Maps.newHashMap()).once();

    replay(mockedObjects);
    try {
      eventBookingManager.requestReservations(testCase.event, studentsToReserve, testCase.teacher);
      fail("Expected to fail from trying to reserve 2 students onto an event with only one space.");
    } catch (EventIsFullException e) {
      // success
    }
    verify(mockedObjects);
  }

  @Test
  void requestReservations_reserveSpacesForMoreThanAllowed_throwsEventGroupReservationLimitException()
      throws Exception {
    EventBookingManager eventBookingManager = buildEventBookingManager();
    ReservationTestDefaults testCase = new ReservationTestDefaults();

    // 2 reservation limit, 1 previously reserved, try to reserve another 2
    testCase.event.setNumberOfPlaces(999); // big enough not to matter
    testCase.event.setGroupReservationLimit(2);
    List<RegisteredUserDTO> studentsToReserve = ImmutableList.of(testCase.student1, testCase.student2);

    RegisteredUserDTO previouslyReservedStudent = testCase.student3;
    Map<BookingStatus, Map<Role, Long>> previousBookingCounts = generatePlacesAvailableMap();
    previousBookingCounts.put(BookingStatus.CONFIRMED, ImmutableMap.of(Role.STUDENT, 1L));
    DetailedEventBookingDTO existingEventBooking = prepareDetailedEventBookingDto(
        previouslyReservedStudent.getId(), BookingStatus.CONFIRMED, testCase.event.getId());
    existingEventBooking.setReservedById(testCase.teacher.getId());

    // Define expected external calls
    expect(dummyTransactionManager.getTransaction()).andReturn(dummyTransaction).once();
    dummyEventBookingPersistenceManager.lockEventUntilTransactionComplete(dummyTransaction, testCase.event.getId());
    expectLastCall().atLeastOnce();
    // No commit expected; exception thrown!
    dummyTransaction.close();
    expectLastCall().once();

    expect(dummyEventBookingPersistenceManager
        .getEventBookingStatusCounts(testCase.event.getId(), false))
        .andReturn(previousBookingCounts).once();
    expect(dummyEventBookingPersistenceManager
        .getBookingsByEventId(testCase.event.getId()))
        .andReturn(ImmutableList.of(existingEventBooking));

    replay(mockedObjects);
    try {
      eventBookingManager.requestReservations(testCase.event, studentsToReserve, testCase.teacher);
      fail("Expected to fail from trying to reserve 2 students onto an event with only one space.");
    } catch (EventGroupReservationLimitException e) {
      // success
    }
    verify(mockedObjects);
  }

  @Test
  void requestReservations_cancelledReservationsDoNotCountTowardsReservationLimit_success() throws Exception {
    EventBookingManager eventBookingManager = buildEventBookingManager();
    ReservationTestDefaults testCase = new ReservationTestDefaults();

    // 1 reservation limit, 1 previously reserved but cancelled, try to reserve another 1
    testCase.event.setNumberOfPlaces(999); // big enough not to matter
    testCase.event.setGroupReservationLimit(1);

    List<RegisteredUserDTO> students = ImmutableList.of(testCase.student1);

    // Make student two have a cancelled booking
    DetailedEventBookingDTO student2sCancelledReservation =
        prepareDetailedEventBookingDto(testCase.student2.getId(), BookingStatus.CANCELLED, testCase.event.getId());
    student2sCancelledReservation.setReservedById(testCase.teacher.getId());
    Map<BookingStatus, Map<Role, Long>> previousBookingCounts = generatePlacesAvailableMap();
    previousBookingCounts.put(BookingStatus.CANCELLED, ImmutableMap.of(Role.STUDENT, 1L));

    // Define expected external calls
    dummyEventBookingPersistenceManager.lockEventUntilTransactionComplete(dummyTransaction, testCase.event.getId());
    expectLastCall().once();

    // Check existing bookings
    expect(dummyEventBookingPersistenceManager.getBookingsByEventId(testCase.event.getId()))
        .andReturn(ImmutableList.of(student2sCancelledReservation)).once();
    expect(dummyEventBookingPersistenceManager
        .getEventBookingStatusCounts(testCase.event.getId(), false))
        .andReturn(previousBookingCounts).once();

    // Make Reservations
    expect(dummyTransactionManager.getTransaction()).andReturn(dummyTransaction).once();

    expect(dummyEventBookingPersistenceManager
        .getBookingByEventIdAndUserId(testCase.event.getId(), testCase.student1.getId()))
        .andReturn(null).once();
    expect(dummyEventBookingPersistenceManager
        .createBooking(eq(dummyTransaction), eq(testCase.event.getId()), eq(testCase.student1.getId()),
            eq(testCase.teacher.getId()), eq(BookingStatus.RESERVED), anyObject()))
        .andReturn(testCase.student1Booking).once();

    dummyTransaction.commit();
    expectLastCall().once();
    dummyTransaction.close();
    expectLastCall().once();

    // Send Emails
    expect(dummyEmailManager.getEmailTemplateDTO(("email-event-reservation-requested"))).andReturn(
        testCase.reservationEmail).atLeastOnce();
    expect(dummyEmailManager.getEmailTemplateDTO(("email-event-reservation-recap"))).andReturn(
        testCase.reservationEmail).atLeastOnce();

    expect(dummyUserAccountManager.getUserDTOById(testCase.student1.getId())).andReturn(testCase.student1)
        .atLeastOnce();
    dummyEmailManager.sendTemplatedEmailToUser(eq(testCase.student1), eq(testCase.reservationEmail), anyObject(),
        eq(EmailType.SYSTEM));
    expectLastCall().once();
    dummyEmailManager.sendTemplatedEmailToUser(eq(testCase.teacher), eq(testCase.reservationEmail), anyObject(),
        eq(EmailType.SYSTEM));
    expectLastCall().atLeastOnce();

    // Run the test for a student event
    replay(mockedObjects);
    List<EventBookingDTO> actualResults =
        eventBookingManager.requestReservations(testCase.event, students, testCase.teacher);
    List<EventBookingDTO> expectedResults = ImmutableList.of(testCase.student1Booking);
    assertEquals(expectedResults, actualResults,
        "Student 1 should get reserved despite the existing cancelled reservation");
    verify(mockedObjects);
  }

  @Nested
  class CancelBooking {
    @Test
    void cancelBookingPromotesOldestWaitingListEntry()
        throws SegueDatabaseException, ContentManagerException, NoUserException {
      IsaacEventPageDTO testEvent = prepareIsaacEventPageDto(studentCSTags);

      RegisteredUserDTO confirmedUser = new RegisteredUserDTO();
      confirmedUser.setId(2L);
      RegisteredUserDTO waitingListUser = new RegisteredUserDTO();
      waitingListUser.setId(4L);

      DetailedEventBookingDTO additionalWaitingListBooking1 =
          prepareDetailedEventBookingDto(prepareUserSummaryDto(3L), BookingStatus.WAITING_LIST, testEvent.getId());
      additionalWaitingListBooking1.setBookingDate(someFutureDate);
      DetailedEventBookingDTO waitingListBookingToBeUpdated =
          prepareDetailedEventBookingDto(prepareUserSummaryDto(4L), BookingStatus.WAITING_LIST, testEvent.getId());
      waitingListBookingToBeUpdated.setBookingDate(someLessFutureDate);
      DetailedEventBookingDTO additionalWaitingListBooking2 =
          prepareDetailedEventBookingDto(prepareUserSummaryDto(5L), BookingStatus.WAITING_LIST, testEvent.getId());
      additionalWaitingListBooking2.setBookingDate(someMoreFutureDate);
      List<DetailedEventBookingDTO> waitingListBookingsList =
          List.of(additionalWaitingListBooking1, waitingListBookingToBeUpdated, additionalWaitingListBooking2);
      DetailedEventBookingDTO updatedWaitingListBooking =
          prepareDetailedEventBookingDto(prepareUserSummaryDto(4L), BookingStatus.CONFIRMED, testEvent.getId());

      prepareTransactionExpectations(testEvent);
      prepareConfirmedBookingExpectations(testEvent, confirmedUser);
      prepareNonEmptyWaitingListExpectations(testEvent, waitingListBookingToBeUpdated, waitingListBookingsList,
          updatedWaitingListBooking);
      prepareCancellationEmailExpectations("email-event-booking-cancellation-confirmed", testEvent,
          confirmedUser);

      expect(dummyUserAccountManager.getUserDTOById(4L)).andReturn(waitingListUser);
      EmailTemplateDTO bookingPromotionNotificationTemplate = new EmailTemplateDTO();
      expect(dummyEmailManager.getEmailTemplateDTO("email-event-booking-waiting-list-promotion-confirmed")).andReturn(
          bookingPromotionNotificationTemplate);
      dummyEmailManager.sendTemplatedEmailToUser(eq(waitingListUser), eq(bookingPromotionNotificationTemplate),
          eq(Map.of(EMAIL_TEMPLATE_TOKEN_CONTACT_US_URL,
              String.format("https://hostname.com/contact?subject=Event+-++-+%s", urlDate),
              EMAIL_TEMPLATE_TOKEN_AUTHORIZATION_LINK, "https://hostname.com/account?authToken=null",
              EMAIL_TEMPLATE_TOKEN_EVENT_DETAILS, "", EMAIL_TEMPLATE_TOKEN_EVENT, testEvent)), eq(EmailType.SYSTEM),
          EasyMock.<List<EmailAttachment>>anyObject());
      expectLastCall();

      replay(mockedObjects);

      EventBookingManager ebm = buildEventBookingManager();
      try {
        ebm.cancelBooking(testEvent, confirmedUser);
      } catch (SegueDatabaseException | ContentManagerException e) {
        fail("No exception is expected for this test");
      }

      verify(mockedObjects);
    }

    @Test
    void cancelBooking_noUserExceptionShouldBeCaughtIfPromotedUserNotFound()
        throws SegueDatabaseException, ContentManagerException, NoUserException {
      IsaacEventPageDTO testEvent = prepareIsaacEventPageDto(studentCSTags);

      RegisteredUserDTO confirmedUser = new RegisteredUserDTO();
      confirmedUser.setId(2L);

      DetailedEventBookingDTO waitingListBookingToBeUpdated =
          prepareDetailedEventBookingDto(prepareUserSummaryDto(3L), BookingStatus.WAITING_LIST, testEvent.getId());
      waitingListBookingToBeUpdated.setBookingDate(someFutureDate);
      List<DetailedEventBookingDTO> waitingListBookingsList = List.of(waitingListBookingToBeUpdated);
      DetailedEventBookingDTO updatedWaitingListBooking =
          prepareDetailedEventBookingDto(prepareUserSummaryDto(3L), BookingStatus.CONFIRMED, testEvent.getId());

      prepareTransactionExpectations(testEvent);
      prepareConfirmedBookingExpectations(testEvent, confirmedUser);
      prepareNonEmptyWaitingListExpectations(testEvent, waitingListBookingToBeUpdated, waitingListBookingsList,
          updatedWaitingListBooking);
      prepareCancellationEmailExpectations("email-event-booking-cancellation-confirmed", testEvent,
          confirmedUser);

      expect(dummyUserAccountManager.getUserDTOById(3L)).andThrow(new NoUserException("No user found with this ID!"));

      replay(mockedObjects);

      EventBookingManager ebm = buildEventBookingManager();
      try {
        ebm.cancelBooking(testEvent, confirmedUser);
      } catch (SegueDatabaseException | ContentManagerException e) {
        fail("No exception is expected for this test");
      }

      verify(mockedObjects);
    }

    @Test
    void cancelBooking_EventBookingUpdateExceptionShouldBeCaughtIfEmailCouldNotBeConstructed()
        throws SegueDatabaseException, ContentManagerException, NoUserException {
      IsaacEventPageDTO testEvent = prepareIsaacEventPageDto(studentCSTags);

      RegisteredUserDTO confirmedUser = new RegisteredUserDTO();
      confirmedUser.setId(2L);
      RegisteredUserDTO waitingListUser = new RegisteredUserDTO();
      waitingListUser.setId(3L);

      DetailedEventBookingDTO waitingListBookingToBeUpdated =
          prepareDetailedEventBookingDto(prepareUserSummaryDto(3L), BookingStatus.WAITING_LIST, testEvent.getId());
      waitingListBookingToBeUpdated.setBookingDate(someFutureDate);
      List<DetailedEventBookingDTO> waitingListBookingsList = List.of(waitingListBookingToBeUpdated);
      DetailedEventBookingDTO updatedWaitingListBooking =
          prepareDetailedEventBookingDto(prepareUserSummaryDto(3L), BookingStatus.CONFIRMED, testEvent.getId());

      prepareTransactionExpectations(testEvent);
      prepareConfirmedBookingExpectations(testEvent, confirmedUser);
      prepareNonEmptyWaitingListExpectations(testEvent, waitingListBookingToBeUpdated, waitingListBookingsList,
          updatedWaitingListBooking);
      prepareCancellationEmailExpectations("email-event-booking-cancellation-confirmed", testEvent,
          confirmedUser);

      expect(dummyUserAccountManager.getUserDTOById(3L)).andReturn(waitingListUser);
      expect(dummyEmailManager.getEmailTemplateDTO("email-event-booking-waiting-list-promotion-confirmed")).andThrow(
          new ContentManagerException("Content is of incorrect type:notAnEmailTemplateDTO"));

      replay(mockedObjects);

      EventBookingManager ebm = buildEventBookingManager();
      try {
        ebm.cancelBooking(testEvent, confirmedUser);
      } catch (SegueDatabaseException | ContentManagerException e) {
        fail("No exception is expected for this test");
      }

      verify(mockedObjects);
    }

    @Test
    void cancelBookingDoesNotPromoteIfThereAreNoBookingsOnTheWaitingList()
        throws SegueDatabaseException, ContentManagerException {
      IsaacEventPageDTO testEvent = prepareIsaacEventPageDto(studentCSTags);

      RegisteredUserDTO confirmedUser = new RegisteredUserDTO();
      confirmedUser.setId(2L);

      prepareTransactionExpectations(testEvent);
      prepareConfirmedBookingExpectations(testEvent, confirmedUser);
      prepareEmptyWaitingListExpectations(testEvent);
      prepareCancellationEmailExpectations("email-event-booking-cancellation-confirmed", testEvent,
          confirmedUser);

      replay(mockedObjects);

      EventBookingManager ebm = buildEventBookingManager();
      try {
        ebm.cancelBooking(testEvent, confirmedUser);
      } catch (SegueDatabaseException | ContentManagerException e) {
        fail("No exception is expected for this test");
      }

      verify(mockedObjects);
    }

    @Test
    void cancelReservedBookingNotifiesReserver()
        throws SegueDatabaseException, ContentManagerException, NoUserException {
      IsaacEventPageDTO testEvent = prepareIsaacEventPageDto(studentCSTags);

      RegisteredUserDTO reservedUser = new RegisteredUserDTO();
      reservedUser.setId(2L);
      reservedUser.setGivenName("givenName");
      reservedUser.setFamilyName("familyName");
      RegisteredUserDTO reservingUser = new RegisteredUserDTO();
      reservingUser.setId(5L);

      prepareTransactionExpectations(testEvent);
      prepareReservedBookingExpectations(testEvent, reservedUser);
      prepareEmptyWaitingListExpectations(testEvent);
      prepareCancellationEmailExpectations("email-event-reservation-cancellation-confirmed", testEvent,
          reservedUser);

      expect(dummyUserAccountManager.getUserDTOById(5L)).andReturn(reservingUser);
      EmailTemplateDTO bookingCancellationReserverNotificationTemplate = new EmailTemplateDTO();
      expect(
          dummyEmailManager.getEmailTemplateDTO(
              "email_event_reservation_cancellation_reserver_notification")).andReturn(
          bookingCancellationReserverNotificationTemplate);
      dummyEmailManager.sendTemplatedEmailToUser(reservingUser, bookingCancellationReserverNotificationTemplate,
          Map.of(EMAIL_TEMPLATE_TOKEN_CONTACT_US_URL,
              String.format("https://hostname.com/contact?subject=Event+-++-+%s", urlDate),
              EMAIL_TEMPLATE_TOKEN_EVENT_DETAILS, "", EMAIL_TEMPLATE_TOKEN_EVENT, testEvent, "reservedName",
              "givenName familyName"), EmailType.SYSTEM);

      replay(mockedObjects);

      EventBookingManager ebm = buildEventBookingManager();
      try {
        ebm.cancelBooking(testEvent, reservedUser);
      } catch (SegueDatabaseException | ContentManagerException e) {
        fail("No exception is expected for this test");
      }

      verify(mockedObjects);
    }

    @Test
    void cancelBooking_noUserExceptionShouldBeCaughtIfReserverNotFound()
        throws SegueDatabaseException, ContentManagerException, NoUserException {
      // This probably should never happen but check it's handled just in case
      IsaacEventPageDTO testEvent = prepareIsaacEventPageDto(studentCSTags);

      RegisteredUserDTO reservedUser = new RegisteredUserDTO();
      reservedUser.setId(2L);
      reservedUser.setGivenName("givenName");
      reservedUser.setFamilyName("familyName");

      prepareTransactionExpectations(testEvent);
      prepareReservedBookingExpectations(testEvent, reservedUser);
      prepareEmptyWaitingListExpectations(testEvent);
      prepareCancellationEmailExpectations("email-event-reservation-cancellation-confirmed", testEvent,
          reservedUser);

      expect(dummyUserAccountManager.getUserDTOById(5L)).andThrow(new NoUserException("No user found with this ID!"));

      replay(mockedObjects);

      EventBookingManager ebm = buildEventBookingManager();
      try {
        ebm.cancelBooking(testEvent, reservedUser);
      } catch (SegueDatabaseException | ContentManagerException e) {
        fail("No exception is expected for this test");
      }

      verify(mockedObjects);
    }

    private void prepareTransactionExpectations(IsaacEventPageDTO testEvent) throws SegueDatabaseException {
      dummyEventBookingPersistenceManager.lockEventUntilTransactionComplete(dummyTransaction, testEvent.getId());
      expectLastCall().once();
      expect(dummyTransactionManager.getTransaction()).andReturn(dummyTransaction).once();
      dummyTransaction.commit();
      expectLastCall().once();
      dummyTransaction.close();
      expectLastCall().once();
    }

    private void prepareConfirmedBookingExpectations(IsaacEventPageDTO testEvent, RegisteredUserDTO confirmedUser)
        throws SegueDatabaseException {
      DetailedEventBookingDTO confirmedBooking =
          prepareDetailedEventBookingDto(prepareUserSummaryDto(2L), BookingStatus.CONFIRMED, testEvent.getId());
      DetailedEventBookingDTO updatedConfirmedBooking =
          prepareDetailedEventBookingDto(prepareUserSummaryDto(2L), BookingStatus.CANCELLED, testEvent.getId());

      expect(dummyEventBookingPersistenceManager.getBookingByEventIdAndUserId(testEvent.getId(),
          confirmedUser.getId())).andReturn(confirmedBooking);
      expect(dummyEventBookingPersistenceManager.updateBookingStatus(dummyTransaction, testEvent.getId(),
          confirmedUser.getId(), BookingStatus.CANCELLED, null)).andReturn(updatedConfirmedBooking);
    }

    private void prepareReservedBookingExpectations(IsaacEventPageDTO testEvent, RegisteredUserDTO reservedUser)
        throws SegueDatabaseException {
      DetailedEventBookingDTO reservedBooking =
          prepareDetailedEventBookingDto(prepareUserSummaryDto(2L), BookingStatus.RESERVED, testEvent.getId());
      reservedBooking.setReservedById(5L);
      DetailedEventBookingDTO updatedReservedBooking =
          prepareDetailedEventBookingDto(prepareUserSummaryDto(2L), BookingStatus.CANCELLED, testEvent.getId());
      updatedReservedBooking.setReservedById(5L);

      expect(dummyEventBookingPersistenceManager.getBookingByEventIdAndUserId(testEvent.getId(),
          reservedUser.getId())).andReturn(reservedBooking);
      expect(dummyEventBookingPersistenceManager.updateBookingStatus(dummyTransaction, testEvent.getId(),
          reservedUser.getId(), BookingStatus.CANCELLED, null)).andReturn(updatedReservedBooking);
    }

    private void prepareNonEmptyWaitingListExpectations(IsaacEventPageDTO testEvent, DetailedEventBookingDTO waitingListBookingToBeUpdated,
                                                        List<DetailedEventBookingDTO> waitingListBookingsList,
                                                        DetailedEventBookingDTO updatedWaitingListBooking) throws SegueDatabaseException {
      expect(dummyEventBookingPersistenceManager.adminGetBookingsByEventIdAndStatus(testEvent.getId(),
          BookingStatus.WAITING_LIST)).andReturn(waitingListBookingsList);
      expect(dummyEventBookingPersistenceManager.updateBookingStatus(dummyTransaction, testEvent.getId(),
          waitingListBookingToBeUpdated.getUserBooked().getId(), BookingStatus.CONFIRMED,
          waitingListBookingToBeUpdated.getAdditionalInformation())).andReturn(updatedWaitingListBooking);
    }

    private void prepareEmptyWaitingListExpectations(IsaacEventPageDTO testEvent) throws SegueDatabaseException {
      List<DetailedEventBookingDTO> waitingListBookingsList = List.of();
      expect(dummyEventBookingPersistenceManager.adminGetBookingsByEventIdAndStatus(testEvent.getId(),
          BookingStatus.WAITING_LIST)).andReturn(waitingListBookingsList);
    }

    private void prepareCancellationEmailExpectations(String emailTemplateId, IsaacEventPageDTO testEvent,
                                                      RegisteredUserDTO confirmedUser)
        throws ContentManagerException, SegueDatabaseException {
      EmailTemplateDTO cancellationNotificationTemplate = new EmailTemplateDTO();
      expect(dummyEmailManager.getEmailTemplateDTO(emailTemplateId)).andReturn(cancellationNotificationTemplate);
      dummyEmailManager.sendTemplatedEmailToUser(confirmedUser, cancellationNotificationTemplate,
          Map.of(EMAIL_TEMPLATE_TOKEN_CONTACT_US_URL,
              String.format("https://hostname.com/contact?subject=Event+-++-+%s", urlDate),
              EMAIL_TEMPLATE_TOKEN_EVENT_DETAILS, "", EMAIL_TEMPLATE_TOKEN_EVENT, testEvent), EmailType.SYSTEM);
      expectLastCall();
    }
  }

  static class ReservationTestDefaults {
    IsaacEventPageDTO event = new IsaacEventPageDTO() {{
        setId("SomeEventId");
        setDate(EventBookingManagerTest.someFutureDate);
      }};
    RegisteredUserDTO teacher = new RegisteredUserDTO() {{
        setId(10L);
      }};
    RegisteredUserDTO student1 = new RegisteredUserDTO() {{
        setId(1L);
        setEmail("student1");
        setEmailVerificationStatus(EmailVerificationStatus.VERIFIED);
      }};
    UserSummaryDTO student1Summary = prepareUserSummaryDto(1L, EmailVerificationStatus.VERIFIED);
    DetailedEventBookingDTO student1Booking =
        prepareDetailedEventBookingDto(student1Summary, BookingStatus.RESERVED, "SomeEventId");
    RegisteredUserDTO student2 = new RegisteredUserDTO() {{
        setId(2L);
        setEmail("student2");
        setEmailVerificationStatus(EmailVerificationStatus.VERIFIED);
      }};
    UserSummaryDTO student2Summary = prepareUserSummaryDto(2L, EmailVerificationStatus.VERIFIED);
    DetailedEventBookingDTO student2Booking =
        prepareDetailedEventBookingDto(student2Summary, BookingStatus.RESERVED, "SomeEventId");
    RegisteredUserDTO student3 = new RegisteredUserDTO() {{
        setId(2L);
        setEmail("student2");
        setEmailVerificationStatus(EmailVerificationStatus.VERIFIED);
      }};
    EmailTemplateDTO reservationEmail = new EmailTemplateDTO();
  }

  private EventBookingManager buildEventBookingManager() {
    return new EventBookingManager(
        dummyEventBookingPersistenceManager, dummyEmailManager, dummyUserAssociationManager,
        dummyPropertiesLoader, dummyGroupManager, dummyUserAccountManager, dummyTransactionManager);
  }

  private static Map<BookingStatus, Map<Role, Long>> generatePlacesAvailableMap() {
    Map<BookingStatus, Map<Role, Long>> placesAvailableMap = Maps.newHashMap();
    placesAvailableMap.put(BookingStatus.CANCELLED, Maps.newHashMap());
    placesAvailableMap.put(BookingStatus.WAITING_LIST, Maps.newHashMap());
    placesAvailableMap.put(BookingStatus.CONFIRMED, Maps.newHashMap());
    placesAvailableMap.put(BookingStatus.RESERVED, Maps.newHashMap());
    return placesAvailableMap;
  }

  private static DetailedEventBookingDTO prepareDetailedEventBookingDto(
      Long userId, BookingStatus bookingStatus, String eventId) {
    UserSummaryDTO user = prepareUserSummaryDto(userId);
    return prepareDetailedEventBookingDto(user, bookingStatus, eventId);
  }

  private static DetailedEventBookingDTO prepareDetailedEventBookingDto(
      UserSummaryDTO user, BookingStatus bookingStatus, String eventId) {
    DetailedEventBookingDTO booking = prepareDetailedEventBookingDto(user, bookingStatus);
    booking.setEventId(eventId);
    return booking;
  }

  private static DetailedEventBookingDTO prepareDetailedEventBookingDto(
      UserSummaryDTO user, BookingStatus bookingStatus) {
    DetailedEventBookingDTO booking = new DetailedEventBookingDTO();
    booking.setUserBooked(user);
    booking.setBookingStatus(bookingStatus);
    return booking;
  }

  private static EventBookingDTO prepareEventBookingDto(Long userId, BookingStatus bookingStatus, Role userRole) {
    UserSummaryDTO user = prepareUserSummaryDto(userId, userRole);
    return prepareEventBookingDto(user, bookingStatus);
  }

  private static EventBookingDTO prepareEventBookingDto(UserSummaryDTO user, BookingStatus bookingStatus) {
    EventBookingDTO booking = new EventBookingDTO();
    booking.setUserBooked(user);
    booking.setBookingStatus(bookingStatus);
    return booking;
  }

  private static UserSummaryDTO prepareUserSummaryDto(Long userId) {
    UserSummaryDTO user = new UserSummaryDTO();
    user.setId(userId);
    return user;
  }

  private static UserSummaryDTO prepareUserSummaryDto(Long userId, Role userRole) {
    UserSummaryDTO user = prepareUserSummaryDto(userId);
    user.setRole(userRole);
    return user;
  }

  private static UserSummaryDTO prepareUserSummaryDto(Long userId, EmailVerificationStatus emailVerificationStatus) {
    UserSummaryDTO user = prepareUserSummaryDto(userId);
    user.setEmailVerificationStatus(emailVerificationStatus);
    return user;
  }

  private static final Set<String> studentCSTags = ImmutableSet.of("student", "computerscience");
  private static final Set<String> teacherCSTags = ImmutableSet.of("teacher", "computerscience");

  private static IsaacEventPageDTO prepareIsaacEventPageDto(Set<String> tags) {
    return prepareIsaacEventPageDto("someEventId", 1, tags, someFutureDate);
  }

  private static IsaacEventPageDTO prepareIsaacEventPageDto(
      Set<String> tags, Integer numberOfPlaces, EventStatus eventStatus) {
    IsaacEventPageDTO testEvent = prepareIsaacEventPageDto("someEventId", numberOfPlaces, tags, someFutureDate);
    testEvent.setEventStatus(eventStatus);
    return testEvent;
  }

  private static IsaacEventPageDTO prepareIsaacEventPageDto(
      String eventId, Integer numberOfPlaces, Set<String> tags, Date date) {
    IsaacEventPageDTO testEvent = new IsaacEventPageDTO();
    testEvent.setId(eventId);
    testEvent.setNumberOfPlaces(numberOfPlaces);
    testEvent.setTags(tags);
    testEvent.setDate(date);
    return testEvent;
  }

  private static IsaacEventPageDTO prepareIsaacEventPageDtoWithEventDetails(Set<String> tags) {
    IsaacEventPageDTO testEvent = prepareIsaacEventPageDto(tags);
    testEvent.setEmailEventDetails("Some Details");
    return testEvent;
  }

  private static IsaacEventPageDTO prepareIsaacEventPageDtoWithEventDetails(Set<String> tags, Integer numberOfPlaces) {
    IsaacEventPageDTO testEvent = prepareIsaacEventPageDto("someEventId", numberOfPlaces, tags, someFutureDate);
    testEvent.setEmailEventDetails("Some Details");
    return testEvent;
  }
}