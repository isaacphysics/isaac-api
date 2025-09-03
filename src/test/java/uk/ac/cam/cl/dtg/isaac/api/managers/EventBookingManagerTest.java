package uk.ac.cam.cl.dtg.isaac.api.managers;

import static java.time.ZoneOffset.UTC;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.easymock.EasyMock;
import org.easymock.IArgumentMatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import uk.ac.cam.cl.dtg.isaac.api.exceptions.DuplicateBookingException;
import uk.ac.cam.cl.dtg.isaac.api.exceptions.EventDeadlineException;
import uk.ac.cam.cl.dtg.isaac.api.exceptions.EventGroupReservationLimitException;
import uk.ac.cam.cl.dtg.isaac.api.exceptions.EventIsCancelledException;
import uk.ac.cam.cl.dtg.isaac.api.exceptions.EventIsFullException;
import uk.ac.cam.cl.dtg.isaac.api.exceptions.EventIsNotFullException;
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
  private static final Instant someFutureDate = Instant.now().plus(7L, ChronoUnit.DAYS);
  private static final Instant someLessFutureDate = Instant.now().plus(6L, ChronoUnit.DAYS);
  private static final Instant someMoreFutureDate = Instant.now().plus(8L, ChronoUnit.DAYS);
  DateTimeFormatter urlDateFormatter = DateTimeFormatter.ofPattern("dd'%2F'MM'%2F'yyyy").withZone(UTC);
  String urlDate = urlDateFormatter.format(someFutureDate);
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

  @Nested
  class RequestBooking {
    @ParameterizedTest
    @EnumSource(value = Role.class, names = {"STUDENT", "TUTOR"}, mode = EnumSource.Mode.EXCLUDE)
    void requestBooking_checkNonStudentRolesAllowedOnStudentEventDespiteCapacityFull_noExceptionThrown(Role role) throws
        Exception {
      EventBookingManager ebm = buildEventBookingManager();
      IsaacEventPageDTO testEvent = prepareIsaacEventPageDtoWithEventDetails(studentCSTags);

      RegisteredUserDTO someUser = new RegisteredUserDTO();
      someUser.setId(6L);
      someUser.setEmailVerificationStatus(EmailVerificationStatus.VERIFIED);
      someUser.setRole(role);

      Map<BookingStatus, Map<Role, Integer>> placesAvailableMap = generatePlacesAvailableMap();
      placesAvailableMap.get(BookingStatus.CONFIRMED).put(Role.STUDENT, 1);
      expect(dummyEventBookingPersistenceManager.getEventBookingStatusCounts(testEvent.getId(), false)).andReturn(
          placesAvailableMap).atLeastOnce();

      expect(dummyEventBookingPersistenceManager.getBookingByEventIdAndUserId(testEvent.getId(), someUser.getId()))
          .andReturn(null).once();

      prepareCommonTransactionExpectations(testEvent);

      EventBookingDTO newBooking = prepareEventBookingDto(someUser.getId(), BookingStatus.CONFIRMED,
          someUser.getRole());
      expect(dummyEventBookingPersistenceManager.createBooking(dummyTransaction, testEvent.getId(), someUser.getId(),
          BookingStatus.CONFIRMED, someAdditionalInformation)).andReturn(newBooking).atLeastOnce();

      EmailTemplateDTO emailTemplate = new EmailTemplateDTO();
      expect(dummyEmailManager.getEmailTemplateDTO("email-event-booking-confirmed")).andReturn(emailTemplate)
          .atLeastOnce();
      dummyEmailManager.sendTemplatedEmailToUser(eq(someUser), eq(emailTemplate), anyObject(), eq(EmailType.SYSTEM),
          anyObject());
      expectLastCall().atLeastOnce();

      replay(mockedObjects);
      ebm.requestBooking(testEvent, someUser, someAdditionalInformation);
      verify(mockedObjects);
    }

    @ParameterizedTest
    @EnumSource(value = Role.class, names = {"STUDENT", "TUTOR"}, mode = EnumSource.Mode.EXCLUDE)
    void requestBooking_checkNonStudentRolesAllowedOnStudentEventDespiteCapacityFull_withWaitingList_noExceptionThrown(
        Role role) throws Exception {
      EventBookingManager ebm = buildEventBookingManager();
      IsaacEventPageDTO testEvent = prepareIsaacEventPageDtoWithEventDetails(studentCSTags);

      RegisteredUserDTO someUser = new RegisteredUserDTO();
      someUser.setId(6L);
      someUser.setEmailVerificationStatus(EmailVerificationStatus.VERIFIED);
      someUser.setRole(role);

      Map<BookingStatus, Map<Role, Integer>> placesAvailableMap = generatePlacesAvailableMap();
      placesAvailableMap.get(BookingStatus.CONFIRMED).put(Role.STUDENT, 1);
      placesAvailableMap.get(BookingStatus.WAITING_LIST).put(Role.STUDENT, 3);
      expect(dummyEventBookingPersistenceManager.getEventBookingStatusCounts(testEvent.getId(), false)).andReturn(
          placesAvailableMap).atLeastOnce();

      expect(dummyEventBookingPersistenceManager.getBookingByEventIdAndUserId(testEvent.getId(), someUser.getId()))
          .andReturn(null).once();

      prepareCommonTransactionExpectations(testEvent);

      EventBookingDTO newBooking = prepareEventBookingDto(someUser.getId(), BookingStatus.CONFIRMED,
                someUser.getRole());
      expect(
          dummyEventBookingPersistenceManager.createBooking(dummyTransaction, testEvent.getId(), someUser.getId(),
              BookingStatus.CONFIRMED, someAdditionalInformation)).andReturn(newBooking).atLeastOnce();

      EmailTemplateDTO emailTemplate = new EmailTemplateDTO();
      expect(dummyEmailManager.getEmailTemplateDTO("email-event-booking-confirmed")).andReturn(emailTemplate)
          .atLeastOnce();

      dummyEmailManager.sendTemplatedEmailToUser(eq(someUser), eq(emailTemplate), anyObject(), eq(EmailType.SYSTEM),
          anyObject());
      expectLastCall().atLeastOnce();

      replay(mockedObjects);
      ebm.requestBooking(testEvent, someUser, someAdditionalInformation);
      verify(mockedObjects);
    }

    @Test
    void requestBooking_checkStudentNotAllowedOnStudentEventAsCapacityFull_eventFullExceptionThrown() throws
        Exception {
      EventBookingManager ebm = buildEventBookingManager();
      IsaacEventPageDTO testEvent = prepareIsaacEventPageDto(studentCSTags);

      RegisteredUserDTO someUser = new RegisteredUserDTO();
      someUser.setId(6L);
      someUser.setEmailVerificationStatus(EmailVerificationStatus.VERIFIED);
      someUser.setRole(Role.STUDENT);

      Map<BookingStatus, Map<Role, Integer>> placesAvailableMap = generatePlacesAvailableMap();
      placesAvailableMap.get(BookingStatus.CONFIRMED).put(Role.STUDENT, 1);
      expect(dummyEventBookingPersistenceManager.getEventBookingStatusCounts(testEvent.getId(), false)).andReturn(
          placesAvailableMap).atLeastOnce();

      expect(dummyEventBookingPersistenceManager.getBookingByEventIdAndUserId(testEvent.getId(),
          someUser.getId())).andReturn(null).once();

      prepareCommonTransactionExpectations(testEvent);

      replay(mockedObjects);
      assertThrows(EventIsFullException.class,
          () -> ebm.requestBooking(testEvent, someUser, someAdditionalInformation));
      verify(dummyEventBookingPersistenceManager);
    }

    @Test
    void requestBooking_checkTeacherNotAllowedOnTeacherEventAsCapacityFull_eventFullExceptionThrown() throws
        Exception {
      EventBookingManager ebm = buildEventBookingManager();
      IsaacEventPageDTO testEvent = prepareIsaacEventPageDto(teacherCSTags);

      RegisteredUserDTO someUser = new RegisteredUserDTO();
      someUser.setId(6L);
      someUser.setEmailVerificationStatus(EmailVerificationStatus.VERIFIED);
      someUser.setRole(Role.TEACHER);

      Map<BookingStatus, Map<Role, Integer>> placesAvailableMap = generatePlacesAvailableMap();
      placesAvailableMap.get(BookingStatus.CONFIRMED).put(Role.TEACHER, 1);
      expect(dummyEventBookingPersistenceManager.getEventBookingStatusCounts(testEvent.getId(), false)).andReturn(
          placesAvailableMap).atLeastOnce();

      expect(dummyEventBookingPersistenceManager.getBookingByEventIdAndUserId(testEvent.getId(),
          someUser.getId())).andReturn(null).once();

      prepareCommonTransactionExpectations(testEvent);

      replay(mockedObjects);
      assertThrows(EventIsFullException.class,
          () -> ebm.requestBooking(testEvent, someUser, someAdditionalInformation));
      verify(dummyEventBookingPersistenceManager);
    }

    @Test
    void requestBooking_addressNotVerified_addressNotVerifiedExceptionThrown() {
      EventBookingManager ebm = buildEventBookingManager();
      IsaacEventPageDTO testEvent = prepareIsaacEventPageDto(studentCSTags);

      RegisteredUserDTO someUser = new RegisteredUserDTO();
      someUser.setId(6L);
      someUser.setEmailVerificationStatus(EmailVerificationStatus.NOT_VERIFIED);
      someUser.setRole(Role.STUDENT);

      replay(mockedObjects);
      assertThrows(EmailMustBeVerifiedException.class,
          () -> ebm.requestBooking(testEvent, someUser, someAdditionalInformation));
      verify(dummyEventBookingPersistenceManager);
    }

    @Test
    void requestBooking_expiredBooking_EventExpiredExceptionThrown() {
      EventBookingManager ebm = buildEventBookingManager();
      IsaacEventPageDTO testEvent = prepareIsaacEventPageDto(studentCSTags);

      // old deadline
      Instant old = Instant.ofEpochMilli(958074310000L);
      testEvent.setBookingDeadline(old);

      RegisteredUserDTO someUser = new RegisteredUserDTO();
      someUser.setId(6L);
      someUser.setEmailVerificationStatus(EmailVerificationStatus.NOT_VERIFIED);
      someUser.setRole(Role.STUDENT);

      replay(mockedObjects);
      assertThrows(EventDeadlineException.class,
          () -> ebm.requestBooking(testEvent, someUser, someAdditionalInformation));
      verify(dummyEventBookingPersistenceManager);
    }

    @Test
    void requestBooking_cancelledSpaceAndWaitingList_SpaceRemainsFull() throws Exception {
      EventBookingManager ebm = buildEventBookingManager();
      IsaacEventPageDTO testEvent = prepareIsaacEventPageDto(teacherCSTags);

      RegisteredUserDTO someUser = new RegisteredUserDTO();
      someUser.setId(6L);
      someUser.setEmailVerificationStatus(EmailVerificationStatus.VERIFIED);
      someUser.setRole(Role.TEACHER);

      Map<BookingStatus, Map<Role, Integer>> placesAvailableMap = generatePlacesAvailableMap();
      placesAvailableMap.get(BookingStatus.WAITING_LIST).put(Role.TEACHER, 1);
      placesAvailableMap.get(BookingStatus.CANCELLED).put(Role.TEACHER, 1);
      expect(dummyEventBookingPersistenceManager.getEventBookingStatusCounts(testEvent.getId(), false)).andReturn(
          placesAvailableMap).atLeastOnce();

      expect(dummyEventBookingPersistenceManager.getBookingByEventIdAndUserId(testEvent.getId(),
          someUser.getId())).andReturn(null)
          .once();

      prepareCommonTransactionExpectations(testEvent);

      replay(mockedObjects);
      assertThrows(EventIsFullException.class,
          () -> ebm.requestBooking(testEvent, someUser, someAdditionalInformation));
      verify(dummyEventBookingPersistenceManager);
    }

    @Test
    void requestBooking_cancelledSpaceAndNoWaitingList_Success() throws Exception {
      EventBookingManager ebm = buildEventBookingManager();
      IsaacEventPageDTO testEvent = prepareIsaacEventPageDtoWithEventDetails(studentCSTags);

      RegisteredUserDTO someUser = new RegisteredUserDTO();
      someUser.setId(6L);
      someUser.setEmailVerificationStatus(EmailVerificationStatus.VERIFIED);
      someUser.setRole(Role.TEACHER);

      Map<BookingStatus, Map<Role, Integer>> placesAvailableMap = generatePlacesAvailableMap();
      placesAvailableMap.get(BookingStatus.CANCELLED).put(Role.TEACHER, 1);
      expect(dummyEventBookingPersistenceManager.getEventBookingStatusCounts(testEvent.getId(), false)).andReturn(
          placesAvailableMap).atLeastOnce();

      expect(dummyEventBookingPersistenceManager.getBookingByEventIdAndUserId(testEvent.getId(), someUser.getId()))
          .andReturn(null).once();

      prepareCommonTransactionExpectations(testEvent);

      EventBookingDTO newBooking =
          prepareEventBookingDto(someUser.getId(), BookingStatus.CONFIRMED, someUser.getRole());
      expect(dummyEventBookingPersistenceManager.createBooking(dummyTransaction, testEvent.getId(), someUser.getId(),
          BookingStatus.CONFIRMED, someAdditionalInformation)).andReturn(newBooking).atLeastOnce();

      EmailTemplateDTO emailTemplate = new EmailTemplateDTO();
      expect(dummyEmailManager.getEmailTemplateDTO("email-event-booking-confirmed")).andReturn(emailTemplate)
          .atLeastOnce();

      dummyEmailManager.sendTemplatedEmailToUser(eq(someUser), eq(emailTemplate), anyObject(), eq(EmailType.SYSTEM),
          anyObject());
      expectLastCall().atLeastOnce();

      replay(mockedObjects);
      ebm.requestBooking(testEvent, someUser, someAdditionalInformation);
      verify(mockedObjects);
    }

    @Test
    void requestBooking_cancelledSpaceAndSomeWaitingList_Success() throws Exception {
      EventBookingManager ebm = buildEventBookingManager();
      IsaacEventPageDTO testEvent = prepareIsaacEventPageDtoWithEventDetails(teacherCSTags, 2);

      RegisteredUserDTO someUser = new RegisteredUserDTO();
      someUser.setId(6L);
      someUser.setEmailVerificationStatus(EmailVerificationStatus.VERIFIED);
      someUser.setRole(Role.TEACHER);

      Map<BookingStatus, Map<Role, Integer>> placesAvailableMap = generatePlacesAvailableMap();
      placesAvailableMap.get(BookingStatus.CANCELLED).put(Role.TEACHER, 1);
      placesAvailableMap.get(BookingStatus.WAITING_LIST).put(Role.TEACHER, 1);
      expect(dummyEventBookingPersistenceManager.getEventBookingStatusCounts(testEvent.getId(), false)).andReturn(
          placesAvailableMap).atLeastOnce();

      expect(dummyEventBookingPersistenceManager.getBookingByEventIdAndUserId(testEvent.getId(), someUser
          .getId())).andReturn(null).once();

      prepareCommonTransactionExpectations(testEvent);

      EventBookingDTO newBooking =
          prepareEventBookingDto(someUser.getId(), BookingStatus.CONFIRMED, someUser.getRole());
      expect(dummyEventBookingPersistenceManager.createBooking(dummyTransaction, testEvent.getId(), someUser.getId(),
          BookingStatus.CONFIRMED, someAdditionalInformation)).andReturn(newBooking).atLeastOnce();

      dummyEmailManager.sendTemplatedEmailToUser(anyObject(), anyObject(), anyObject(), anyObject(), anyObject());
      expectLastCall().atLeastOnce();

      expect(dummyEmailManager.getEmailTemplateDTO("email-event-booking-confirmed")).andReturn(new EmailTemplateDTO())
          .atLeastOnce();

      replay(mockedObjects);
      ebm.requestBooking(testEvent, someUser, someAdditionalInformation);
      verify(mockedObjects);
    }

    @Test
    void requestBooking_userIsAbleToPromoteBookingReservation_Success() throws Exception {
      EventBookingManager eventBookingManager = buildEventBookingManager();
      ReservationTestDefaults testCase = new ReservationTestDefaults();
      testCase.event.setNumberOfPlaces(1);

      RegisteredUserDTO reservedStudent = testCase.student1;
      DetailedEventBookingDTO reservedStudentBooking =
          prepareDetailedEventBookingDto(reservedStudent.getId(), BookingStatus.RESERVED, testCase.event.getId());
      DetailedEventBookingDTO reservedStudentBookingAfterConfirmation =
          prepareDetailedEventBookingDto(reservedStudent.getId(), BookingStatus.CONFIRMED, testCase.event.getId());

      // Expected external calls
      prepareCommonTransactionExpectations(testCase.event);

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
    void requestBooking_userCanRebookCancelledBookingIfSpaceAvailable_succeeds() throws
        Exception {
      EventBookingManager ebm = buildEventBookingManager();
      IsaacEventPageDTO testEvent = prepareIsaacEventPageDtoWithEventDetails(studentCSTags);

      RegisteredUserDTO someUser = new RegisteredUserDTO();
      someUser.setId(6L);
      someUser.setEmailVerificationStatus(EmailVerificationStatus.VERIFIED);
      someUser.setRole(Role.STUDENT);

      Map<BookingStatus, Map<Role, Integer>> placesAvailableMap = generatePlacesAvailableMap();
      placesAvailableMap.get(BookingStatus.CANCELLED).put(Role.STUDENT, 6);
      expect(dummyEventBookingPersistenceManager.getEventBookingStatusCounts(testEvent.getId(), false)).andReturn(
          placesAvailableMap).atLeastOnce();

      DetailedEventBookingDTO cancelledBooking =
          prepareDetailedEventBookingDto(someUser.getId(), BookingStatus.CANCELLED,
              testEvent.getId());
      expect(dummyEventBookingPersistenceManager.getBookingByEventIdAndUserId(testEvent.getId(), someUser.getId()))
          .andReturn(cancelledBooking).once();

      prepareCommonTransactionExpectations(testEvent);

      DetailedEventBookingDTO updatedBooking = prepareDetailedEventBookingDto(someUser.getId(), BookingStatus.CONFIRMED,
          testEvent.getId());
      expect(
          dummyEventBookingPersistenceManager.updateBookingStatus(dummyTransaction, testEvent.getId(), someUser.getId(),
              BookingStatus.CONFIRMED, someAdditionalInformation)).andReturn(updatedBooking).atLeastOnce();

      EmailTemplateDTO emailTemplate = new EmailTemplateDTO();
      expect(dummyEmailManager.getEmailTemplateDTO("email-event-booking-confirmed")).andReturn(emailTemplate)
          .atLeastOnce();

      dummyEmailManager.sendTemplatedEmailToUser(eq(someUser), eq(emailTemplate), anyObject(), eq(EmailType.SYSTEM),
          anyObject());
      expectLastCall().atLeastOnce();

      replay(mockedObjects);
      ebm.requestBooking(testEvent, someUser, someAdditionalInformation);
      verify(mockedObjects);
    }

    @Test
    void requestBooking_userCanPromoteWaitingListBookingIfSpaceAvailable_succeeds() throws
        Exception {
      EventBookingManager ebm = buildEventBookingManager();
      IsaacEventPageDTO testEvent = prepareIsaacEventPageDtoWithEventDetails(studentCSTags);

      RegisteredUserDTO someUser = new RegisteredUserDTO();
      someUser.setId(6L);
      someUser.setEmailVerificationStatus(EmailVerificationStatus.VERIFIED);
      someUser.setRole(Role.STUDENT);

      Map<BookingStatus, Map<Role, Integer>> placesAvailableMap = generatePlacesAvailableMap();
      placesAvailableMap.get(BookingStatus.WAITING_LIST).put(Role.STUDENT, 6);
      expect(dummyEventBookingPersistenceManager.getEventBookingStatusCounts(testEvent.getId(), false)).andReturn(
          placesAvailableMap).atLeastOnce();

      DetailedEventBookingDTO waitingListBooking =
          prepareDetailedEventBookingDto(someUser.getId(), BookingStatus.WAITING_LIST, testEvent.getId());
      expect(dummyEventBookingPersistenceManager.getBookingByEventIdAndUserId(testEvent.getId(),
          someUser.getId())).andReturn(waitingListBooking).once();

      prepareCommonTransactionExpectations(testEvent);

      DetailedEventBookingDTO updatedBooking = prepareDetailedEventBookingDto(someUser.getId(), BookingStatus.CONFIRMED,
          testEvent.getId());
      expect(
          dummyEventBookingPersistenceManager.updateBookingStatus(dummyTransaction, testEvent.getId(), someUser.getId(),
              BookingStatus.CONFIRMED, someAdditionalInformation)).andReturn(updatedBooking).atLeastOnce();

      EmailTemplateDTO emailTemplate = new EmailTemplateDTO();
      expect(dummyEmailManager.getEmailTemplateDTO("email-event-booking-confirmed")).andReturn(emailTemplate)
          .atLeastOnce();

      dummyEmailManager.sendTemplatedEmailToUser(eq(someUser), eq(emailTemplate), anyObject(), eq(EmailType.SYSTEM),
          anyObject());
      expectLastCall().atLeastOnce();

      replay(mockedObjects);
      ebm.requestBooking(testEvent, someUser, someAdditionalInformation);
      verify(mockedObjects);
    }
  }

  @Nested
  class RequestWaitingList {
    @Test
    void requestWaitingList_checkTeacherAllowedOnFullEventWithEmptyWaitingList_noExceptionThrown() throws
        Exception {
      EventBookingManager ebm = buildEventBookingManager();
      IsaacEventPageDTO testEvent = prepareIsaacEventPageDtoWithEventDetails(studentCSTags);
      testEvent.setNumberOfPlaces(1);

      RegisteredUserDTO someUser = new RegisteredUserDTO();
      someUser.setId(6L);
      someUser.setEmailVerificationStatus(EmailVerificationStatus.VERIFIED);
      someUser.setRole(Role.TEACHER);

      DetailedEventBookingDTO firstBooking =
          prepareDetailedEventBookingDto(someUser.getId(), BookingStatus.WAITING_LIST, testEvent.getId());

      Map<BookingStatus, Map<Role, Integer>> placesAvailableMap = generatePlacesAvailableMap();
      placesAvailableMap.get(BookingStatus.CONFIRMED).put(Role.STUDENT, 1);
      expect(dummyEventBookingPersistenceManager.getEventBookingStatusCounts(testEvent.getId(), false)).andReturn(
          placesAvailableMap).atLeastOnce();

      expect(dummyEventBookingPersistenceManager.getBookingByEventIdAndUserId(testEvent.getId(), someUser.getId()))
          .andReturn(null).once();

      prepareCommonTransactionExpectations(testEvent);

      expect(
          dummyEventBookingPersistenceManager.createBooking(dummyTransaction, testEvent.getId(), someUser.getId(), null,
              BookingStatus.WAITING_LIST, someAdditionalInformation)).andReturn(firstBooking).atLeastOnce();

      EmailTemplateDTO emailTemplate = new EmailTemplateDTO();
      expect(dummyEmailManager.getEmailTemplateDTO("email-event-waiting-list-addition-notification")).andReturn(
          emailTemplate).atLeastOnce();

      dummyEmailManager.sendTemplatedEmailToUser(eq(someUser), eq(emailTemplate), anyObject(), eq(EmailType.SYSTEM));
      expectLastCall().atLeastOnce();

      replay(mockedObjects);
      ebm.requestWaitingListBooking(testEvent, someUser, someAdditionalInformation);
      verify(mockedObjects);
    }

    @Test
    void requestWaitingList_checkTeacherAllowedOnFullEventWithExistingWaitingList_noExceptionThrown() throws
        Exception {
      EventBookingManager ebm = buildEventBookingManager();
      IsaacEventPageDTO testEvent = prepareIsaacEventPageDtoWithEventDetails(studentCSTags);
      testEvent.setNumberOfPlaces(1);

      RegisteredUserDTO someUser = new RegisteredUserDTO();
      someUser.setId(6L);
      someUser.setEmailVerificationStatus(EmailVerificationStatus.VERIFIED);
      someUser.setRole(Role.TEACHER);

      DetailedEventBookingDTO firstBooking =
          prepareDetailedEventBookingDto(someUser.getId(), BookingStatus.WAITING_LIST, testEvent.getId());

      Map<BookingStatus, Map<Role, Integer>> placesAvailableMap = generatePlacesAvailableMap();
      placesAvailableMap.get(BookingStatus.CONFIRMED).put(Role.STUDENT, 1);
      placesAvailableMap.get(BookingStatus.WAITING_LIST).put(Role.STUDENT, 3);
      expect(dummyEventBookingPersistenceManager.getEventBookingStatusCounts(testEvent.getId(), false)).andReturn(
          placesAvailableMap).atLeastOnce();

      expect(dummyEventBookingPersistenceManager.getBookingByEventIdAndUserId(testEvent.getId(), someUser.getId()))
          .andReturn(null).once();

      prepareCommonTransactionExpectations(testEvent);

      expect(
          dummyEventBookingPersistenceManager.createBooking(dummyTransaction, testEvent.getId(), someUser.getId(), null,
              BookingStatus.WAITING_LIST, someAdditionalInformation)).andReturn(firstBooking).atLeastOnce();

      EmailTemplateDTO emailTemplate = new EmailTemplateDTO();
      expect(dummyEmailManager.getEmailTemplateDTO("email-event-waiting-list-addition-notification")).andReturn(
          emailTemplate).atLeastOnce();

      dummyEmailManager.sendTemplatedEmailToUser(eq(someUser), eq(emailTemplate), anyObject(), eq(EmailType.SYSTEM));
      expectLastCall().atLeastOnce();

      replay(mockedObjects);
      ebm.requestWaitingListBooking(testEvent, someUser, someAdditionalInformation);
      verify(mockedObjects);
    }

    @Test
    void requestWaitingList_checkStudentAllowedOnFullEventWithEmptyWaitingList_noExceptionThrown() throws
        Exception {
      EventBookingManager ebm = buildEventBookingManager();
      IsaacEventPageDTO testEvent = prepareIsaacEventPageDtoWithEventDetails(studentCSTags);
      testEvent.setNumberOfPlaces(1);

      RegisteredUserDTO someUser = new RegisteredUserDTO();
      someUser.setId(6L);
      someUser.setEmailVerificationStatus(EmailVerificationStatus.VERIFIED);
      someUser.setRole(Role.STUDENT);

      DetailedEventBookingDTO firstBooking =
          prepareDetailedEventBookingDto(someUser.getId(), BookingStatus.WAITING_LIST, testEvent.getId());

      Map<BookingStatus, Map<Role, Integer>> placesAvailableMap = generatePlacesAvailableMap();
      placesAvailableMap.get(BookingStatus.CONFIRMED).put(Role.STUDENT, 1);
      expect(dummyEventBookingPersistenceManager.getEventBookingStatusCounts(testEvent.getId(), false)).andReturn(
          placesAvailableMap).atLeastOnce();

      expect(dummyEventBookingPersistenceManager.getBookingByEventIdAndUserId(testEvent.getId(), someUser.getId()))
          .andReturn(null).once();

      prepareCommonTransactionExpectations(testEvent);

      expect(
          dummyEventBookingPersistenceManager.createBooking(dummyTransaction, testEvent.getId(), someUser.getId(), null,
              BookingStatus.WAITING_LIST, someAdditionalInformation)).andReturn(firstBooking).atLeastOnce();

      EmailTemplateDTO emailTemplate = new EmailTemplateDTO();
      expect(dummyEmailManager.getEmailTemplateDTO("email-event-waiting-list-addition-notification")).andReturn(
          emailTemplate).atLeastOnce();

      dummyEmailManager.sendTemplatedEmailToUser(eq(someUser), eq(emailTemplate), anyObject(), eq(EmailType.SYSTEM));
      expectLastCall().atLeastOnce();

      replay(mockedObjects);
      ebm.requestWaitingListBooking(testEvent, someUser, someAdditionalInformation);
      verify(mockedObjects);
    }

    @Test
    void requestWaitingList_checkStudentAllowedOnFullEventWithExistingWaitingList_noExceptionThrown() throws
        Exception {
      EventBookingManager ebm = buildEventBookingManager();
      IsaacEventPageDTO testEvent = prepareIsaacEventPageDtoWithEventDetails(studentCSTags);
      testEvent.setNumberOfPlaces(1);

      RegisteredUserDTO someUser = new RegisteredUserDTO();
      someUser.setId(6L);
      someUser.setEmailVerificationStatus(EmailVerificationStatus.VERIFIED);
      someUser.setRole(Role.STUDENT);

      DetailedEventBookingDTO firstBooking =
          prepareDetailedEventBookingDto(someUser.getId(), BookingStatus.WAITING_LIST, testEvent.getId());

      Map<BookingStatus, Map<Role, Integer>> placesAvailableMap = generatePlacesAvailableMap();
      placesAvailableMap.get(BookingStatus.CONFIRMED).put(Role.STUDENT, 1);
      placesAvailableMap.get(BookingStatus.WAITING_LIST).put(Role.STUDENT, 3);
      expect(dummyEventBookingPersistenceManager.getEventBookingStatusCounts(testEvent.getId(), false)).andReturn(
          placesAvailableMap).atLeastOnce();

      expect(dummyEventBookingPersistenceManager.getBookingByEventIdAndUserId(testEvent.getId(), someUser.getId()))
          .andReturn(null).once();

      prepareCommonTransactionExpectations(testEvent);

      expect(
          dummyEventBookingPersistenceManager.createBooking(dummyTransaction, testEvent.getId(), someUser.getId(), null,
              BookingStatus.WAITING_LIST, someAdditionalInformation)).andReturn(firstBooking).atLeastOnce();

      EmailTemplateDTO emailTemplate = new EmailTemplateDTO();
      expect(dummyEmailManager.getEmailTemplateDTO("email-event-waiting-list-addition-notification")).andReturn(
          emailTemplate).atLeastOnce();

      dummyEmailManager.sendTemplatedEmailToUser(eq(someUser), eq(emailTemplate), anyObject(), eq(EmailType.SYSTEM));
      expectLastCall().atLeastOnce();

      replay(mockedObjects);
      ebm.requestWaitingListBooking(testEvent, someUser, someAdditionalInformation);
      verify(mockedObjects);
    }

    @Test
    void requestWaitingList_checkRequestOnEventWithOpenSpaces_throwsEventIsNotFullException() throws
        Exception {
      EventBookingManager ebm = buildEventBookingManager();
      IsaacEventPageDTO testEvent = prepareIsaacEventPageDtoWithEventDetails(studentCSTags);
      testEvent.setNumberOfPlaces(10);

      RegisteredUserDTO someUser = new RegisteredUserDTO();
      someUser.setId(6L);
      someUser.setEmailVerificationStatus(EmailVerificationStatus.VERIFIED);
      someUser.setRole(Role.TEACHER);

      Map<BookingStatus, Map<Role, Integer>> placesAvailableMap = generatePlacesAvailableMap();
      placesAvailableMap.get(BookingStatus.CONFIRMED).put(Role.STUDENT, 1);
      expect(dummyEventBookingPersistenceManager.getEventBookingStatusCounts(testEvent.getId(), false)).andReturn(
          placesAvailableMap).atLeastOnce();

      dummyEventBookingPersistenceManager.lockEventUntilTransactionComplete(dummyTransaction, testEvent.getId());
      expectLastCall().once();
      expect(dummyTransactionManager.getTransaction()).andReturn(dummyTransaction).once();
      dummyTransaction.close();
      expectLastCall().once();

      replay(mockedObjects);
      assertThrows(EventIsNotFullException.class,
          () -> ebm.requestWaitingListBooking(testEvent, someUser, someAdditionalInformation));
      verify(mockedObjects);
    }

    @Test
    void requestWaitingList_checkRequestOnEventWhenAlreadyBooked_throwsDuplicateBookingException() throws
        Exception {
      EventBookingManager ebm = buildEventBookingManager();
      IsaacEventPageDTO testEvent = prepareIsaacEventPageDtoWithEventDetails(studentCSTags);
      testEvent.setNumberOfPlaces(1);

      RegisteredUserDTO someUser = new RegisteredUserDTO();
      someUser.setId(6L);
      someUser.setEmailVerificationStatus(EmailVerificationStatus.VERIFIED);
      someUser.setRole(Role.TEACHER);

      DetailedEventBookingDTO existingBooking =
          prepareDetailedEventBookingDto(someUser.getId(), BookingStatus.CONFIRMED, testEvent.getId());

      Map<BookingStatus, Map<Role, Integer>> placesAvailableMap = generatePlacesAvailableMap();
      placesAvailableMap.get(BookingStatus.CONFIRMED).put(Role.STUDENT, 1);
      placesAvailableMap.get(BookingStatus.CONFIRMED).put(Role.TEACHER, 6);
      expect(dummyEventBookingPersistenceManager.getEventBookingStatusCounts(testEvent.getId(), false)).andReturn(
          placesAvailableMap).atLeastOnce();

      expect(dummyEventBookingPersistenceManager.getBookingByEventIdAndUserId(testEvent.getId(), someUser.getId()))
          .andReturn(existingBooking).once();

      dummyEventBookingPersistenceManager.lockEventUntilTransactionComplete(dummyTransaction, testEvent.getId());
      expectLastCall().once();
      expect(dummyTransactionManager.getTransaction()).andReturn(dummyTransaction).once();
      dummyTransaction.close();
      expectLastCall().once();

      replay(mockedObjects);
      assertThrows(DuplicateBookingException.class,
          () -> ebm.requestWaitingListBooking(testEvent, someUser, someAdditionalInformation));
      verify(mockedObjects);
    }

    @Test
    void requestWaitingList_checkRequestOnEventThatIsCancelled_throwsEventIsCancelledException() {
      EventBookingManager ebm = buildEventBookingManager();
      IsaacEventPageDTO testEvent = prepareIsaacEventPageDtoWithEventDetails(studentCSTags);
      testEvent.setNumberOfPlaces(1);
      testEvent.setEventStatus(EventStatus.CANCELLED);

      RegisteredUserDTO someUser = new RegisteredUserDTO();
      someUser.setId(6L);
      someUser.setEmailVerificationStatus(EmailVerificationStatus.VERIFIED);
      someUser.setRole(Role.TEACHER);

      replay(mockedObjects);
      assertThrows(EventIsCancelledException.class,
          () -> ebm.requestWaitingListBooking(testEvent, someUser, someAdditionalInformation));
      verify(mockedObjects);
    }

    @Test
    void requestWaitingList_checkRequestOnEventThatIsInThePast_throwsEventDeadlineException() {
      EventBookingManager ebm = buildEventBookingManager();
      IsaacEventPageDTO testEvent = prepareIsaacEventPageDtoWithEventDetails(studentCSTags);
      testEvent.setNumberOfPlaces(1);
      testEvent.setDate(Instant.now().minus(1L, ChronoUnit.HOURS));

      RegisteredUserDTO someUser = new RegisteredUserDTO();
      someUser.setId(6L);
      someUser.setEmailVerificationStatus(EmailVerificationStatus.VERIFIED);
      someUser.setRole(Role.TEACHER);

      replay(mockedObjects);
      assertThrows(EventDeadlineException.class,
          () -> ebm.requestWaitingListBooking(testEvent, someUser, someAdditionalInformation));
      verify(mockedObjects);
    }
  }

  @Nested
  class PromoteBooking {
    @Test
    void promoteBooking_spaceDueToCancellation_Success() throws Exception {
      EventBookingManager ebm = buildEventBookingManager();
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

      Map<BookingStatus, Map<Role, Integer>> placesAvailableMap = generatePlacesAvailableMap();
      placesAvailableMap.get(BookingStatus.CANCELLED).put(Role.TEACHER, 1);
      placesAvailableMap.get(BookingStatus.WAITING_LIST).put(Role.TEACHER, 1);
      expect(dummyEventBookingPersistenceManager.getEventBookingStatusCounts(testEvent.getId(), false)).andReturn(
          placesAvailableMap).atLeastOnce();

      expect(dummyEventBookingPersistenceManager.getBookingByEventIdAndUserId(testEvent.getId(), 6L))
          .andReturn(firstBooking).once();

      prepareCommonTransactionExpectations(testEvent);

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
      EventBookingManager ebm = buildEventBookingManager();
      IsaacEventPageDTO testEvent = prepareIsaacEventPageDto(teacherCSTags);

      RegisteredUserDTO someUser = new RegisteredUserDTO();
      someUser.setId(6L);
      someUser.setEmailVerificationStatus(EmailVerificationStatus.VERIFIED);
      someUser.setRole(Role.TEACHER);

      UserSummaryDTO firstUser = prepareUserSummaryDto(6L, Role.TEACHER);
      DetailedEventBookingDTO firstBooking =
          prepareDetailedEventBookingDto(firstUser, BookingStatus.WAITING_LIST, testEvent.getId());

      Map<BookingStatus, Map<Role, Integer>> placesAvailableMap = generatePlacesAvailableMap();
      placesAvailableMap.get(BookingStatus.CONFIRMED).put(Role.TEACHER, 1);
      placesAvailableMap.get(BookingStatus.WAITING_LIST).put(Role.TEACHER, 1);
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
  }

  @Nested
  class RequestReservation {
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
              eq(testCase.teacher.getId()), eq(BookingStatus.RESERVED), reservationCloseDateMatcher()))
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
      Map<BookingStatus, Map<Role, Integer>> previousBookingCounts = generatePlacesAvailableMap();
      previousBookingCounts.put(BookingStatus.CONFIRMED, ImmutableMap.of(Role.STUDENT, 1));
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
      Map<BookingStatus, Map<Role, Integer>> previousBookingCounts = generatePlacesAvailableMap();
      previousBookingCounts.put(BookingStatus.CANCELLED, ImmutableMap.of(Role.STUDENT, 1));

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
    void cancelConfirmedBookingTriggersWaitingListPromotion()
        throws SegueDatabaseException, ContentManagerException, NoUserException {
      IsaacEventPageDTO testEvent = prepareIsaacEventPageDto(studentCSTags);

      RegisteredUserDTO confirmedUser = new RegisteredUserDTO();
      confirmedUser.setId(2L);
      RegisteredUserDTO waitingListUser = new RegisteredUserDTO();
      waitingListUser.setId(4L);

      DetailedEventBookingDTO waitingListBookingToBeUpdated =
          prepareDetailedEventBookingDto(prepareUserSummaryDto(4L), BookingStatus.WAITING_LIST, testEvent.getId());
      waitingListBookingToBeUpdated.setBookingDate(someFutureDate);
      List<DetailedEventBookingDTO> waitingListBookingsList = List.of(waitingListBookingToBeUpdated);
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
    void cancelReservedBookingTriggersWaitingListPromotion()
        throws SegueDatabaseException, ContentManagerException, NoUserException {
      IsaacEventPageDTO testEvent = prepareIsaacEventPageDto(studentCSTags);

      RegisteredUserDTO reservedUser = new RegisteredUserDTO();
      reservedUser.setId(2L);
      reservedUser.setGivenName("givenName");
      reservedUser.setFamilyName("familyName");
      RegisteredUserDTO waitingListUser = new RegisteredUserDTO();
      waitingListUser.setId(4L);
      RegisteredUserDTO reservingUser = new RegisteredUserDTO();
      reservingUser.setId(5L);

      DetailedEventBookingDTO waitingListBookingToBeUpdated =
          prepareDetailedEventBookingDto(prepareUserSummaryDto(4L), BookingStatus.WAITING_LIST, testEvent.getId());
      waitingListBookingToBeUpdated.setBookingDate(someFutureDate);
      List<DetailedEventBookingDTO> waitingListBookingsList = List.of(waitingListBookingToBeUpdated);
      DetailedEventBookingDTO updatedWaitingListBooking =
          prepareDetailedEventBookingDto(prepareUserSummaryDto(4L), BookingStatus.CONFIRMED, testEvent.getId());

      prepareTransactionExpectations(testEvent);
      prepareReservedBookingExpectations(testEvent, reservedUser);
      prepareNonEmptyWaitingListExpectations(testEvent, waitingListBookingToBeUpdated, waitingListBookingsList,
          updatedWaitingListBooking);
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
      expectLastCall();

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
        ebm.cancelBooking(testEvent, reservedUser);
      } catch (SegueDatabaseException | ContentManagerException e) {
        fail("No exception is expected for this test");
      }

      verify(mockedObjects);
    }

    @Test
    void cancelWaitingListBookingDoesNotTriggerWaitingListPromotion()
        throws SegueDatabaseException, ContentManagerException {
      IsaacEventPageDTO testEvent = prepareIsaacEventPageDto(studentCSTags);

      RegisteredUserDTO waitingListUserToCancelBooking = new RegisteredUserDTO();
      waitingListUserToCancelBooking.setId(2L);

      DetailedEventBookingDTO waitingListBookingToBeCancelled =
          prepareDetailedEventBookingDto(prepareUserSummaryDto(2L), BookingStatus.WAITING_LIST, testEvent.getId());
      DetailedEventBookingDTO updatedWaitingListBooking =
          prepareDetailedEventBookingDto(prepareUserSummaryDto(2L), BookingStatus.CANCELLED, testEvent.getId());

      prepareTransactionExpectations(testEvent);

      expect(dummyEventBookingPersistenceManager.getBookingByEventIdAndUserId(testEvent.getId(),
          waitingListUserToCancelBooking.getId())).andReturn(waitingListBookingToBeCancelled);
      expect(dummyEventBookingPersistenceManager.updateBookingStatus(dummyTransaction, testEvent.getId(),
          waitingListUserToCancelBooking.getId(), BookingStatus.CANCELLED, null)).andReturn(updatedWaitingListBooking);

      prepareCancellationEmailExpectations("email-event-booking-cancellation-confirmed", testEvent,
          waitingListUserToCancelBooking);

      replay(mockedObjects);

      EventBookingManager ebm = buildEventBookingManager();
      try {
        ebm.cancelBooking(testEvent, waitingListUserToCancelBooking);
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
      expectLastCall();

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
      prepareCommonTransactionExpectations(testEvent);
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

  @Nested
  class CapacityChecks {
    @Test
    void getPlacesAvailable_ifNumberOfPlacesIsNull_returnsNull() throws SegueDatabaseException {
      EventBookingManager eventBookingManager = buildEventBookingManager();

      IsaacEventPageDTO testEvent = new IsaacEventPageDTO();
      testEvent.setNumberOfPlaces(null);

      replay(mockedObjects);

      Integer remainingPlacesAvailable = eventBookingManager.getPlacesAvailable(testEvent);
      assertNull(remainingPlacesAvailable);
      verify(mockedObjects);
    }

    @ParameterizedTest(name = "{index} {3}")
    @MethodSource
    void getPlacesAvailable_returnsCorrectCount(IsaacEventPageDTO testEvent, Integer expectedPlacesAvailable, Map<BookingStatus, Map<Role, Integer>> bookingStatusMap, String description)
        throws SegueDatabaseException {
      EventBookingManager eventBookingManager = buildEventBookingManager();

      expect(dummyEventBookingPersistenceManager.getEventBookingStatusCounts(testEvent.getId(), false)).andReturn(
          bookingStatusMap);
      replay(mockedObjects);

      Integer remainingPlacesAvailable = eventBookingManager.getPlacesAvailable(testEvent);
      assertEquals(expectedPlacesAvailable, remainingPlacesAvailable, description);
      verify(mockedObjects);
    }

    private static Stream<Arguments> getPlacesAvailable_returnsCorrectCount() {
      return Stream.of(
          Arguments.of(prepareIsaacEventPageDto(studentCSTags, 500, EventStatus.WAITING_LIST_ONLY), 499,
              testBookingStatusMap, "WAITING_LIST_ONLY student events should count confirmed student bookings"),
          Arguments.of(prepareIsaacEventPageDto(studentCSTags, 500, EventStatus.OPEN), 389, testBookingStatusMap,
              "OPEN student events should count student bookings that are confirmed, reserved or on the waiting list"),
          Arguments.of(prepareIsaacEventPageDto(teacherCSTags, 500, EventStatus.WAITING_LIST_ONLY), 485,
              testBookingStatusMap, "WAITING_LIST_ONLY standard events should count confirmed bookings for all roles"),
          Arguments.of(prepareIsaacEventPageDto(teacherCSTags, 500, EventStatus.OPEN), 155, testBookingStatusMap,
              "OPEN standard events should count bookings that are confirmed, reserved or on the waiting list for all roles"),
          Arguments.of(prepareIsaacEventPageDto(studentCSTags, 10, EventStatus.OPEN), 0, smallStudentBookingStatusMap,
              "Student events should return a minimum remaining places available of zero"),
          Arguments.of(prepareIsaacEventPageDto(teacherCSTags, 10, EventStatus.OPEN), 0, smallTeacherBookingStatusMap,
              "Standard events should return a minimum remaining places available of zero"),
          Arguments.of(prepareIsaacEventPageDto(studentCSTags, 10, EventStatus.WAITING_LIST_ONLY), 10, Map.of(),
              "WAITING_LIST_ONLY student events should handle an empty map"),
          Arguments.of(prepareIsaacEventPageDto(studentCSTags, 10, EventStatus.OPEN), 10, Map.of(),
              "OPEN student events should handle an empty map"),
          Arguments.of(prepareIsaacEventPageDto(teacherCSTags, 10, EventStatus.WAITING_LIST_ONLY), 10, Map.of(),
              "WAITING_LIST_ONLY standard events should handle an empty map"),
          Arguments.of(prepareIsaacEventPageDto(teacherCSTags, 10, EventStatus.OPEN), 10, Map.of(),
              "OPEN standard events should handle an empty map")
      );
    }

    private static final Map<BookingStatus, Map<Role, Integer>> testBookingStatusMap = Map.of(
        BookingStatus.CONFIRMED, Map.of(
            Role.STUDENT, 1, Role.TEACHER, 2, Role.TUTOR, 4, Role.EVENT_LEADER, 8
        ),
        BookingStatus.RESERVED, Map.of(
            Role.STUDENT, 10, Role.TEACHER, 20
        ),
        BookingStatus.WAITING_LIST, Map.of(
            Role.STUDENT, 100, Role.TEACHER, 200
        ),
        BookingStatus.CANCELLED, Map.of(
            Role.STUDENT, 1000
        )
    );

    private static final Map<BookingStatus, Map<Role, Integer>> smallStudentBookingStatusMap =
        Map.of(BookingStatus.CONFIRMED, Map.of(Role.STUDENT, 15), BookingStatus.WAITING_LIST,
            Map.of(Role.STUDENT, 5));

    private static final Map<BookingStatus, Map<Role, Integer>> smallTeacherBookingStatusMap =
        Map.of(BookingStatus.CONFIRMED, Map.of(Role.TEACHER, 15), BookingStatus.WAITING_LIST,
            Map.of(Role.TEACHER, 5));

  }

  @Test
  void isUserAbleToManageEvent_checkUsersWithDifferentRoles_success() throws Exception {
    EventBookingManager eventBookingManager = buildEventBookingManager();

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
          eventPageDTO = new IsaacEventPageDTO() {
            @Override
            public Boolean isCompetitionEvent() {
              return super.isCompetitionEvent();
            }

            {
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

  private void prepareCommonTransactionExpectations(IsaacEventPageDTO testEvent) throws SegueDatabaseException {
    dummyEventBookingPersistenceManager.lockEventUntilTransactionComplete(dummyTransaction, testEvent.getId());
    expectLastCall().once();
    expect(dummyTransactionManager.getTransaction()).andReturn(dummyTransaction).once();
    dummyTransaction.commit();
    expectLastCall().once();
    dummyTransaction.close();
    expectLastCall().once();
  }

  private static Map<BookingStatus, Map<Role, Integer>> generatePlacesAvailableMap() {
    Map<BookingStatus, Map<Role, Integer>> placesAvailableMap = Maps.newHashMap();
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
      String eventId, Integer numberOfPlaces, Set<String> tags, Instant date) {
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

  private static Map<String, String> reservationCloseDateMatcher() {
    EasyMock.reportMatcher(new IArgumentMatcher() {

      @Override
      public boolean matches(Object argument) {
        if (argument instanceof Map<?, ?> map) {
          Object reservationClosedDate = map.get("reservationCloseDate");

          if (reservationClosedDate instanceof String) {
            LocalDate comparisonDate = LocalDate.parse((String) reservationClosedDate, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX"));
            Instant expectedDate = Instant.now().plus(3, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS);
            comparisonDate.atStartOfDay(ZoneId.of("Europe/London"));
            Instant actualDate = ZonedDateTime.parse((String) reservationClosedDate).toInstant().truncatedTo(ChronoUnit.DAYS);
            return expectedDate.equals(actualDate);
          }
        }
        return false;
      }

      @Override
      public void appendTo(StringBuffer buffer) {
        buffer.append("Expected close date wrong: ");
      }
    });
    return null;
  }
}