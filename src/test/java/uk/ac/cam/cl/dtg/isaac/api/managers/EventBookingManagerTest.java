package uk.ac.cam.cl.dtg.isaac.api.managers;

import com.google.api.client.util.Maps;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Test;
import uk.ac.cam.cl.dtg.isaac.dao.EventBookingPersistenceManager;
import uk.ac.cam.cl.dtg.isaac.dos.EventStatus;
import uk.ac.cam.cl.dtg.isaac.dos.eventbookings.BookingStatus;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacEventPageDTO;
import uk.ac.cam.cl.dtg.isaac.dto.eventbookings.EventBookingDTO;
import uk.ac.cam.cl.dtg.segue.api.managers.GroupManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAccountManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAssociationManager;
import uk.ac.cam.cl.dtg.segue.comm.EmailManager;
import uk.ac.cam.cl.dtg.segue.comm.EmailMustBeVerifiedException;
import uk.ac.cam.cl.dtg.segue.comm.EmailType;
import uk.ac.cam.cl.dtg.segue.dos.AssociationToken;
import uk.ac.cam.cl.dtg.segue.dos.users.EmailVerificationStatus;
import uk.ac.cam.cl.dtg.segue.dos.users.Role;
import uk.ac.cam.cl.dtg.segue.dto.UserGroupDTO;
import uk.ac.cam.cl.dtg.segue.dto.content.EmailTemplateDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.UserSummaryDTO;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.easymock.EasyMock.anyLong;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static uk.ac.cam.cl.dtg.segue.api.Constants.EVENT_ADMIN_EMAIL;
import static uk.ac.cam.cl.dtg.segue.api.Constants.EVENT_ICAL_UID_DOMAIN;
import static uk.ac.cam.cl.dtg.segue.api.Constants.HOST_NAME;
import static uk.ac.cam.cl.dtg.segue.api.Constants.MAIL_NAME;

/**
 * EventBookingManagerTest.
 */
public class EventBookingManagerTest {
    static private Date someFutureDate = new Date(System.currentTimeMillis() + 7*24*60*60*1000);
    private EventBookingPersistenceManager dummyEventBookingPersistenceManager;
    private EmailManager dummyEmailManager;
    private UserAssociationManager dummyUserAssociationManager;
    private Map<String, String> someAdditionalInformation;
    private PropertiesLoader dummyPropertiesLoader;
    private GroupManager dummyGroupManager;
    private UserAccountManager dummyUserAccountManager;

    /**
     * Initial configuration of tests.
     */
    @Before
    public final void setUp() {
        this.dummyEmailManager = createMock(EmailManager.class);
        this.dummyEventBookingPersistenceManager = createMock(EventBookingPersistenceManager.class);
        this.dummyUserAssociationManager = createMock(UserAssociationManager.class);
        this.dummyGroupManager = createMock(GroupManager.class);
        this.dummyUserAccountManager = createMock(UserAccountManager.class);
        this.dummyPropertiesLoader = createMock(PropertiesLoader.class);
        expect(this.dummyPropertiesLoader.getProperty(HOST_NAME)).andReturn("hostname.com").anyTimes();
        expect(this.dummyPropertiesLoader.getProperty(MAIL_NAME)).andReturn("Isaac Physics").anyTimes();
        expect(this.dummyPropertiesLoader.getProperty(EVENT_ADMIN_EMAIL)).andReturn("admin@hostname.com").anyTimes();
        expect(this.dummyPropertiesLoader.getProperty(EVENT_ICAL_UID_DOMAIN)).andReturn("hostname.com").anyTimes();
        this.someAdditionalInformation = Maps.newHashMap();
    }

    @Test
    public void requestBooking_checkTeacherAllowedOnStudentEventDespiteCapacityFull_noExceptionThrown() throws
            Exception {
        EventBookingManager ebm = this.buildEventBookingManager();
        IsaacEventPageDTO testEvent = new IsaacEventPageDTO();
        testEvent.setId("someEventId");
        testEvent.setNumberOfPlaces(1);
        testEvent.setTags(ImmutableSet.of("student", "physics"));
        testEvent.setEmailEventDetails("Some Details");
        testEvent.setDate(someFutureDate);

        RegisteredUserDTO someUser = new RegisteredUserDTO();
        someUser.setId(6L);
        someUser.setEmailVerificationStatus(EmailVerificationStatus.VERIFIED);
        someUser.setRole(Role.TEACHER);

        EventBookingDTO firstBooking = new EventBookingDTO();
        UserSummaryDTO firstUser = new UserSummaryDTO();
        firstUser.setRole(Role.TEACHER);
        firstUser.setId(someUser.getId());
        firstBooking.setUserBooked(firstUser);
        firstBooking.setBookingStatus(BookingStatus.CONFIRMED);

        Map<BookingStatus, Map<Role, Long>> placesAvailableMap = generatePlacesAvailableMap();
        placesAvailableMap.get(BookingStatus.CONFIRMED).put(Role.TEACHER, 1L);
        expect(dummyEventBookingPersistenceManager.getEventBookingStatusCounts(testEvent.getId(), false)).andReturn(placesAvailableMap).atLeastOnce();

        expect(dummyEventBookingPersistenceManager.getBookingByEventIdAndUserId(testEvent.getId(), someUser.getId()))
				.andReturn(firstBooking);
        expect(dummyEventBookingPersistenceManager.isUserBooked(testEvent.getId(), someUser.getId())).andReturn(false);

        dummyEventBookingPersistenceManager.acquireDistributedLock(testEvent.getId());
        expectLastCall().atLeastOnce();

        expect(dummyEventBookingPersistenceManager.createBooking(testEvent.getId(), someUser.getId(), BookingStatus
				.CONFIRMED, someAdditionalInformation)).andReturn(firstBooking).atLeastOnce();

        dummyEventBookingPersistenceManager.releaseDistributedLock(testEvent.getId());
        expectLastCall().atLeastOnce();

        expect(dummyEmailManager.getEmailTemplateDTO("email-event-booking-confirmed")).andReturn(new EmailTemplateDTO()).atLeastOnce();

        dummyEmailManager.sendTemplatedEmailToUser(anyObject(), anyObject(), anyObject(), anyObject(), anyObject());
        expectLastCall().atLeastOnce();

        Object[] mockedObjects = {dummyEventBookingPersistenceManager, dummyPropertiesLoader, dummyEmailManager};
        replay(mockedObjects);
        ebm.requestBooking(testEvent, someUser, someAdditionalInformation);
        verify(mockedObjects);
    }

    @Test
    public void requestBooking_checkStudentNotAllowedOnStudentEventAsCapacityFull_eventFullExceptionThrown() throws
			Exception {
        EventBookingManager ebm = this.buildEventBookingManager();
        IsaacEventPageDTO testEvent = new IsaacEventPageDTO();
        testEvent.setId("someEventId");
        testEvent.setNumberOfPlaces(1);
        testEvent.setTags(ImmutableSet.of("student", "physics"));
        testEvent.setDate(someFutureDate);

        RegisteredUserDTO someUser = new RegisteredUserDTO();
        someUser.setId(6L);
        someUser.setEmailVerificationStatus(EmailVerificationStatus.VERIFIED);
        someUser.setRole(Role.STUDENT);

        EventBookingDTO firstBooking = new EventBookingDTO();
        UserSummaryDTO firstUser = new UserSummaryDTO();
        firstUser.setRole(Role.STUDENT);
        firstBooking.setUserBooked(firstUser);
        firstBooking.setBookingStatus(BookingStatus.CONFIRMED);

        Map<BookingStatus, Map<Role, Long>> placesAvailableMap = generatePlacesAvailableMap();
        placesAvailableMap.get(BookingStatus.CONFIRMED).put(Role.STUDENT, 1L);
        expect(dummyEventBookingPersistenceManager.getEventBookingStatusCounts(testEvent.getId(), false)).andReturn(placesAvailableMap).atLeastOnce();

        expect(dummyEventBookingPersistenceManager.isUserBooked(testEvent.getId(), someUser.getId())).andReturn(false);
        expect(dummyEventBookingPersistenceManager.getBookingByEventIdAndUserId(testEvent.getId(), someUser.getId())).andReturn(null);

        dummyEventBookingPersistenceManager.acquireDistributedLock(testEvent.getId());
        expectLastCall().atLeastOnce();
        dummyEventBookingPersistenceManager.releaseDistributedLock(testEvent.getId());
        expectLastCall().atLeastOnce();

        replay(dummyEventBookingPersistenceManager);
        try {
            ebm.requestBooking(testEvent, someUser, someAdditionalInformation);
            fail("Expected an EventFullException and one didn't happen.");
        } catch (EventIsFullException e) {
            // success !
        }
        verify(dummyEventBookingPersistenceManager);
    }

    @Test
    public void requestBooking_checkTeacherNotAllowedOnTeacherEventAsCapacityFull_eventFullExceptionThrown() throws
			Exception {
        EventBookingManager ebm = this.buildEventBookingManager();
        IsaacEventPageDTO testEvent = new IsaacEventPageDTO();
        testEvent.setId("someEventId");
        testEvent.setNumberOfPlaces(1);
        testEvent.setTags(ImmutableSet.of("teacher", "physics"));
        testEvent.setDate(someFutureDate);

        RegisteredUserDTO someUser = new RegisteredUserDTO();
        someUser.setId(6L);
        someUser.setEmailVerificationStatus(EmailVerificationStatus.VERIFIED);
        someUser.setRole(Role.TEACHER);

        EventBookingDTO firstBooking = new EventBookingDTO();
        UserSummaryDTO firstUser = new UserSummaryDTO();
        firstUser.setRole(Role.TEACHER);
        firstBooking.setUserBooked(firstUser);
        firstBooking.setBookingStatus(BookingStatus.CONFIRMED);

        Map<BookingStatus, Map<Role, Long>> placesAvailableMap = generatePlacesAvailableMap();
        placesAvailableMap.get(BookingStatus.CONFIRMED).put(Role.TEACHER, 1L);
        expect(dummyEventBookingPersistenceManager.getEventBookingStatusCounts(testEvent.getId(), false)).andReturn(placesAvailableMap).atLeastOnce();

        expect(dummyEventBookingPersistenceManager.isUserBooked(testEvent.getId(), someUser.getId())).andReturn(false);
        expect(dummyEventBookingPersistenceManager.getBookingByEventIdAndUserId(testEvent.getId(), someUser.getId())).andReturn(null);

        dummyEventBookingPersistenceManager.acquireDistributedLock(testEvent.getId());
        expectLastCall().atLeastOnce();
        dummyEventBookingPersistenceManager.releaseDistributedLock(testEvent.getId());
        expectLastCall().atLeastOnce();

        replay(dummyEventBookingPersistenceManager);
        try {
            ebm.requestBooking(testEvent, someUser, someAdditionalInformation);
            fail("Expected an EventFullException and one didn't happen.");
        } catch (EventIsFullException e) {
            // success !
        }
        verify(dummyEventBookingPersistenceManager);
    }

    @Test
    public void requestBooking_addressNotVerified_addressNotVerifiedExceptionThrown() throws Exception {
        EventBookingManager ebm = this.buildEventBookingManager();
        IsaacEventPageDTO testEvent = new IsaacEventPageDTO();
        testEvent.setId("someEventId");
        testEvent.setNumberOfPlaces(1);
        testEvent.setTags(ImmutableSet.of("student", "physics"));

        RegisteredUserDTO someUser = new RegisteredUserDTO();
        someUser.setId(6L);
        someUser.setEmailVerificationStatus(EmailVerificationStatus.NOT_VERIFIED);
        someUser.setRole(Role.STUDENT);

        expect(dummyEventBookingPersistenceManager.isUserBooked(testEvent.getId(), someUser.getId())).andReturn(false);

        replay(dummyEventBookingPersistenceManager);
        try {
            ebm.requestBooking(testEvent, someUser, someAdditionalInformation);
            fail("Expected an EventFullException and one didn't happen.");
        } catch (EmailMustBeVerifiedException e) {
            // success !
        }
        verify(dummyEventBookingPersistenceManager);
    }

    @Test
    public void requestBooking_expiredBooking_EventExpiredExceptionThrown() throws Exception {
        EventBookingManager ebm = this.buildEventBookingManager();
        IsaacEventPageDTO testEvent = new IsaacEventPageDTO();
        testEvent.setId("someEventId");
        testEvent.setNumberOfPlaces(1);

        // old deadline
        Date old = new Date();
        old.setTime(958074310000L);

        testEvent.setBookingDeadline(old);
        testEvent.setTags(ImmutableSet.of("student", "physics"));

        RegisteredUserDTO someUser = new RegisteredUserDTO();
        someUser.setId(6L);
        someUser.setEmailVerificationStatus(EmailVerificationStatus.NOT_VERIFIED);
        someUser.setRole(Role.STUDENT);

        replay(dummyEventBookingPersistenceManager);
        try {
            ebm.requestBooking(testEvent, someUser, someAdditionalInformation);
            fail("Expected an Event Expiry Exception and one didn't happen.");
        } catch (EventDeadlineException e) {
            // success !
        }
        verify(dummyEventBookingPersistenceManager);
    }

    @Test
    public void requestBooking_cancelledSpaceAndWaitingList_SpaceRemainsFull() throws Exception {
        EventBookingManager ebm = this.buildEventBookingManager();
        IsaacEventPageDTO testEvent = new IsaacEventPageDTO();
        testEvent.setId("someEventId");
        testEvent.setNumberOfPlaces(1);
        testEvent.setTags(ImmutableSet.of("teacher", "physics"));
        testEvent.setDate(someFutureDate);

        RegisteredUserDTO someUser = new RegisteredUserDTO();
        someUser.setId(6L);
        someUser.setEmailVerificationStatus(EmailVerificationStatus.VERIFIED);
        someUser.setRole(Role.TEACHER);

        EventBookingDTO firstBooking = new EventBookingDTO();
        UserSummaryDTO firstUser = new UserSummaryDTO();
        firstUser.setRole(Role.TEACHER);
        firstBooking.setUserBooked(firstUser);
        firstBooking.setBookingStatus(BookingStatus.CANCELLED);

        EventBookingDTO secondBooking = new EventBookingDTO();
        UserSummaryDTO secondUser = new UserSummaryDTO();
        secondUser.setRole(Role.TEACHER);
        secondBooking.setUserBooked(firstUser);
        secondBooking.setBookingStatus(BookingStatus.WAITING_LIST);

        Map<BookingStatus, Map<Role, Long>> placesAvailableMap = generatePlacesAvailableMap();
        placesAvailableMap.get(BookingStatus.WAITING_LIST).put(Role.TEACHER, 1L);
        placesAvailableMap.get(BookingStatus.CANCELLED).put(Role.TEACHER, 1L);
        expect(dummyEventBookingPersistenceManager.getEventBookingStatusCounts(testEvent.getId(), false)).andReturn(placesAvailableMap).atLeastOnce();

        List<EventBookingDTO> currentBookings = Arrays.asList(firstBooking, secondBooking);

        expect(dummyEventBookingPersistenceManager.isUserBooked(testEvent.getId(), someUser.getId())).andReturn(false);
        expect(dummyEventBookingPersistenceManager.getBookingByEventIdAndUserId(testEvent.getId(), someUser.getId())).andReturn(null);

        dummyEventBookingPersistenceManager.acquireDistributedLock(testEvent.getId());
        expectLastCall().atLeastOnce();
        dummyEventBookingPersistenceManager.releaseDistributedLock(testEvent.getId());
        expectLastCall().atLeastOnce();

        replay(dummyEventBookingPersistenceManager);
        try {
            ebm.requestBooking(testEvent, someUser, someAdditionalInformation);
            fail("Expected an EventFullException and one didn't happen.");
        } catch (EventIsFullException e) {
            // success !
        }
        verify(dummyEventBookingPersistenceManager);
    }

    @Test
    public void requestBooking_cancelledSpaceAndNoWaitingList_Success() throws Exception {
        EventBookingManager ebm = this.buildEventBookingManager();
        IsaacEventPageDTO testEvent = new IsaacEventPageDTO();
        testEvent.setId("someEventId");
        testEvent.setNumberOfPlaces(1);
        testEvent.setTags(ImmutableSet.of("teacher", "physics"));
        testEvent.setEmailEventDetails("Some Details");
        testEvent.setDate(someFutureDate);

        RegisteredUserDTO someUser = new RegisteredUserDTO();
        someUser.setId(6L);
        someUser.setEmailVerificationStatus(EmailVerificationStatus.VERIFIED);
        someUser.setRole(Role.TEACHER);

        EventBookingDTO secondBooking = new EventBookingDTO();
        UserSummaryDTO secondUser = new UserSummaryDTO();
        secondUser.setRole(Role.TEACHER);
        secondUser.setId(7L);
        secondBooking.setUserBooked(secondUser);
        secondBooking.setBookingStatus(BookingStatus.CANCELLED);

        Map<BookingStatus, Map<Role, Long>> placesAvailableMap = generatePlacesAvailableMap();
        placesAvailableMap.get(BookingStatus.CANCELLED).put(Role.TEACHER, 1L);
        expect(dummyEventBookingPersistenceManager.getEventBookingStatusCounts(testEvent.getId(), false)).andReturn(placesAvailableMap).atLeastOnce();

        List<EventBookingDTO> currentBookings = Arrays.asList(secondBooking);

        expect(dummyEventBookingPersistenceManager.getBookingByEventIdAndUserId(testEvent.getId(), someUser.getId()))
				.andReturn(null);

        expect(dummyEventBookingPersistenceManager.isUserBooked(testEvent.getId(), someUser.getId())).andReturn(false);

        dummyEventBookingPersistenceManager.acquireDistributedLock(testEvent.getId());
        expectLastCall().atLeastOnce();

        expect(dummyEventBookingPersistenceManager.createBooking(testEvent.getId(), someUser.getId(),
                BookingStatus.CONFIRMED, someAdditionalInformation)).andReturn(secondBooking).atLeastOnce();

        dummyEventBookingPersistenceManager.releaseDistributedLock(testEvent.getId());
        expectLastCall().atLeastOnce();

        expect(dummyEmailManager.getEmailTemplateDTO("email-event-booking-confirmed")).andReturn(new EmailTemplateDTO()).atLeastOnce();

        dummyEmailManager.sendTemplatedEmailToUser(anyObject(), anyObject(), anyObject(), anyObject(), anyObject());
        expectLastCall().atLeastOnce();

        Object[] mockedObjects = {dummyEventBookingPersistenceManager, dummyPropertiesLoader, dummyEmailManager};
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
    public void requestBooking_cancelledSpaceAndSomeWaitingList_Success() throws Exception {
        EventBookingManager ebm = this.buildEventBookingManager();
        IsaacEventPageDTO testEvent = new IsaacEventPageDTO();
        testEvent.setId("someEventId");
        testEvent.setNumberOfPlaces(2);
        testEvent.setTags(ImmutableSet.of("teacher", "physics"));
        testEvent.setEmailEventDetails("Some Details");
        testEvent.setDate(someFutureDate);

        RegisteredUserDTO firstUserFull = new RegisteredUserDTO();
        firstUserFull.setId(6L);
        firstUserFull.setEmailVerificationStatus(EmailVerificationStatus.VERIFIED);
        firstUserFull.setRole(Role.TEACHER);

        EventBookingDTO firstBooking = new EventBookingDTO();
        UserSummaryDTO firstUser = new UserSummaryDTO();
        firstUser.setId(firstUserFull.getId());
        firstUser.setRole(Role.TEACHER);

        firstBooking.setUserBooked(firstUser);
        firstBooking.setBookingStatus(BookingStatus.WAITING_LIST);

        EventBookingDTO secondBooking = new EventBookingDTO();
        UserSummaryDTO secondUser = new UserSummaryDTO();
        secondUser.setRole(Role.TEACHER);
        secondUser.setId(7L);
        secondBooking.setUserBooked(firstUser);
        secondBooking.setBookingStatus(BookingStatus.CANCELLED);

        List<EventBookingDTO> currentBookings = Arrays.asList(firstBooking, secondBooking);

        Map<BookingStatus, Map<Role, Long>> placesAvailableMap = generatePlacesAvailableMap();
        placesAvailableMap.get(BookingStatus.CANCELLED).put(Role.TEACHER, 1L);
        placesAvailableMap.get(BookingStatus.WAITING_LIST).put(Role.TEACHER, 1L);
        expect(dummyEventBookingPersistenceManager.getEventBookingStatusCounts(testEvent.getId(), false)).andReturn(placesAvailableMap).atLeastOnce();

        expect(dummyEventBookingPersistenceManager.getBookingByEventIdAndUserId(testEvent.getId(), firstUserFull
				.getId())).andReturn(firstBooking);
        expect(dummyEventBookingPersistenceManager.isUserBooked(testEvent.getId(), firstUserFull.getId())).andReturn
				(false);

        dummyEventBookingPersistenceManager.acquireDistributedLock(testEvent.getId());
        expectLastCall().atLeastOnce();

        expect(dummyEventBookingPersistenceManager.createBooking(testEvent.getId(), firstUserFull.getId(),
				BookingStatus.CONFIRMED, someAdditionalInformation)).andReturn(secondBooking).atLeastOnce();

        dummyEventBookingPersistenceManager.releaseDistributedLock(testEvent.getId());
        expectLastCall().atLeastOnce();

        dummyEmailManager.sendTemplatedEmailToUser(anyObject(), anyObject(), anyObject(), anyObject(), anyObject());
        expectLastCall().atLeastOnce();

        expect(dummyEmailManager.getEmailTemplateDTO("email-event-booking-confirmed")).andReturn(new EmailTemplateDTO()).atLeastOnce();

        Object[] mockedObjects = {dummyEventBookingPersistenceManager, dummyPropertiesLoader, dummyEmailManager};
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
    public void requestBooking_userIsAbleToPromoteBookingReservation_Success() throws Exception {
        EventBookingManager eventBookingManager = this.buildEventBookingManager();
        ReservationTestDefaults testCase = new ReservationTestDefaults();
        testCase.event.setNumberOfPlaces(1);

        RegisteredUserDTO reservedStudent = testCase.student1;
        EventBookingDTO reservedStudentBooking = new EventBookingDTO() {{
            setEventId(testCase.event.getId()); setBookingStatus(BookingStatus.RESERVED);
            setUserBooked(new UserSummaryDTO() {{setId(reservedStudent.getId());}});
        }};
        EventBookingDTO reservedStudentBookingAfterConfirmation = new EventBookingDTO() {{
            setEventId(testCase.event.getId()); setBookingStatus(BookingStatus.CONFIRMED);
            setUserBooked(new UserSummaryDTO() {{setId(reservedStudent.getId());}});
        }};

        // Expected external calls
        expect(dummyEventBookingPersistenceManager.isUserBooked(testCase.event.getId(), reservedStudent.getId())).andReturn(false).once();

        dummyEventBookingPersistenceManager.acquireDistributedLock(testCase.event.getId());
        expectLastCall().once();

        expect(dummyEventBookingPersistenceManager
                .getBookingByEventIdAndUserId(testCase.event.getId(), reservedStudent.getId()))
                .andReturn(reservedStudentBooking).once();
        // As a reserved booking exists, expect an update to the booking
        expect(dummyEventBookingPersistenceManager
                .updateBookingStatus(eq(testCase.event.getId()), eq(reservedStudent.getId()), eq(BookingStatus.CONFIRMED), anyObject()))
                .andReturn(reservedStudentBookingAfterConfirmation).once();
        // Send emails
        EmailTemplateDTO emailTemplate = new EmailTemplateDTO();
        expect(dummyEmailManager.getEmailTemplateDTO("email-event-booking-confirmed")).andReturn(emailTemplate).once();
        dummyEmailManager.sendTemplatedEmailToUser(eq(reservedStudent), eq(emailTemplate), anyObject(), eq(EmailType.SYSTEM), anyObject());
        expectLastCall().once();

        dummyEventBookingPersistenceManager.releaseDistributedLock(testCase.event.getId());
        expectLastCall().once();


        Object[] mockedObjects = {dummyEventBookingPersistenceManager, dummyPropertiesLoader, dummyEmailManager};
        replay(mockedObjects);
        eventBookingManager.requestBooking(testCase.event, reservedStudent, someAdditionalInformation);
        verify(mockedObjects);
    }

    @Test
    public void promoteBooking_spaceDueToCancellation_Success() throws Exception {
        EventBookingManager ebm = this.buildEventBookingManager();
        IsaacEventPageDTO testEvent = new IsaacEventPageDTO();
        testEvent.setId("someEventId");
        testEvent.setNumberOfPlaces(1);
        testEvent.setTags(ImmutableSet.of("teacher", "physics"));
        testEvent.setEmailEventDetails("some details");
        testEvent.setDate(someFutureDate);

        RegisteredUserDTO someUser = new RegisteredUserDTO();
        someUser.setId(6L);
        someUser.setEmailVerificationStatus(EmailVerificationStatus.VERIFIED);
        someUser.setRole(Role.TEACHER);

        EventBookingDTO firstBooking = new EventBookingDTO();
        UserSummaryDTO firstUser = new UserSummaryDTO();
        firstBooking.setEventId(testEvent.getId());
        firstUser.setId(6L);
        firstUser.setRole(Role.TEACHER);
        firstBooking.setUserBooked(firstUser);
        firstBooking.setBookingStatus(BookingStatus.WAITING_LIST);
        firstBooking.setAdditionalInformation(someAdditionalInformation);

        EventBookingDTO secondBooking = new EventBookingDTO();
        UserSummaryDTO secondUser = new UserSummaryDTO();
        secondUser.setId(2L);
        secondUser.setRole(Role.TEACHER);
        secondBooking.setEventId(testEvent.getId());
        secondBooking.setUserBooked(firstUser);
        secondBooking.setBookingStatus(BookingStatus.CANCELLED);
        secondBooking.setAdditionalInformation(someAdditionalInformation);

        Map<BookingStatus, Map<Role, Long>> placesAvailableMap = generatePlacesAvailableMap();
        placesAvailableMap.get(BookingStatus.CANCELLED).put(Role.TEACHER, 1L);
        placesAvailableMap.get(BookingStatus.WAITING_LIST).put(Role.TEACHER, 1L);
        expect(dummyEventBookingPersistenceManager.getEventBookingStatusCounts(testEvent.getId(), false)).andReturn(placesAvailableMap).atLeastOnce();

        List<EventBookingDTO> currentBookings = Arrays.asList(firstBooking, secondBooking);

        expect(dummyEventBookingPersistenceManager.isUserBooked(testEvent.getId(), someUser.getId())).andReturn(false);

        expect(dummyEventBookingPersistenceManager.getBookingByEventIdAndUserId(testEvent.getId(), 6L))
                .andReturn(firstBooking).atLeastOnce();

        dummyEventBookingPersistenceManager.acquireDistributedLock(testEvent.getId());
        expectLastCall().atLeastOnce();

        expect(dummyEventBookingPersistenceManager.updateBookingStatus(testEvent.getId(), someUser.getId(),
				BookingStatus.CONFIRMED, someAdditionalInformation)).andReturn(secondBooking).atLeastOnce();

        dummyEventBookingPersistenceManager.releaseDistributedLock(testEvent.getId());
        expectLastCall().atLeastOnce();

        expect(dummyEmailManager.getEmailTemplateDTO("email-event-booking-waiting-list-promotion-confirmed"))
                .andReturn(new EmailTemplateDTO()).atLeastOnce();

        dummyEmailManager.sendTemplatedEmailToUser(anyObject(), anyObject(), anyObject(), anyObject(), anyObject());
        expectLastCall().atLeastOnce();

        Object[] mockedObjects = {dummyEventBookingPersistenceManager, dummyPropertiesLoader, dummyEmailManager};
        replay(mockedObjects);
        try {
            ebm.promoteFromWaitingListOrCancelled(testEvent, someUser);
            // success
        } catch (EventIsFullException e) {
            fail("Expected successful booking as no waiting list bookings.");
        }
        verify(mockedObjects);
    }

    @Test
    public void promoteBooking_NoSpace_Failure() throws Exception {
        EventBookingManager ebm = this.buildEventBookingManager();
        IsaacEventPageDTO testEvent = new IsaacEventPageDTO();
        testEvent.setId("someEventId");
        testEvent.setNumberOfPlaces(1);
        testEvent.setTags(ImmutableSet.of("teacher", "physics"));
        testEvent.setDate(someFutureDate);

        RegisteredUserDTO someUser = new RegisteredUserDTO();
        someUser.setId(6L);
        someUser.setEmailVerificationStatus(EmailVerificationStatus.VERIFIED);
        someUser.setRole(Role.TEACHER);

        EventBookingDTO firstBooking = new EventBookingDTO();
        UserSummaryDTO firstUser = new UserSummaryDTO();
        firstBooking.setEventId(testEvent.getId());
        firstUser.setId(6L);
        firstUser.setRole(Role.TEACHER);
        firstBooking.setUserBooked(firstUser);
        firstBooking.setBookingStatus(BookingStatus.WAITING_LIST);

        EventBookingDTO secondBooking = new EventBookingDTO();
        UserSummaryDTO secondUser = new UserSummaryDTO();
        secondUser.setId(2L);
        secondUser.setRole(Role.TEACHER);
        secondBooking.setEventId(testEvent.getId());
        secondBooking.setUserBooked(firstUser);
        secondBooking.setBookingStatus(BookingStatus.CONFIRMED);

        List<EventBookingDTO> currentBookings = Arrays.asList(firstBooking, secondBooking);

        Map<BookingStatus, Map<Role, Long>> placesAvailableMap = generatePlacesAvailableMap();
        placesAvailableMap.get(BookingStatus.CONFIRMED).put(Role.TEACHER, 1L);
        placesAvailableMap.get(BookingStatus.WAITING_LIST).put(Role.TEACHER, 1L);
        expect(dummyEventBookingPersistenceManager.getEventBookingStatusCounts(testEvent.getId(), false)).andReturn(placesAvailableMap).atLeastOnce();

        expect(dummyEventBookingPersistenceManager.isUserBooked(testEvent.getId(), someUser.getId())).andReturn(false);

        expect(dummyEventBookingPersistenceManager.getBookingByEventIdAndUserId(testEvent.getId(), 6L)).andReturn(firstBooking);

        dummyEventBookingPersistenceManager.acquireDistributedLock(testEvent.getId());
        expectLastCall().atLeastOnce();
        dummyEventBookingPersistenceManager.releaseDistributedLock(testEvent.getId());
        expectLastCall().atLeastOnce();

        replay(dummyEventBookingPersistenceManager);
        try {
            ebm.promoteFromWaitingListOrCancelled(testEvent, someUser);
            fail("Expected failure booking as no space for this event.");
        } catch (EventIsFullException e) {
            // success
        }
        verify(dummyEventBookingPersistenceManager);
    }

    @Test
    public void getPlacesAvailable_checkEventCapacity_capacityCalculatedCorrectly() throws Exception {
        // Create a future event and event booking manager
        EventBookingManager ebm = this.buildEventBookingManager();
        int initialNumberOfPlaces = 1000;
        IsaacEventPageDTO testEvent = new IsaacEventPageDTO() {{
            setId("someEventId");
            setNumberOfPlaces(initialNumberOfPlaces);
            setTags(ImmutableSet.of("student"));
            setEventStatus(EventStatus.OPEN);
            setDate(someFutureDate);
        }};

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
        replay(dummyEventBookingPersistenceManager);
        Long actualPlacesAvailable = ebm.getPlacesAvailable(testEvent);
        Long expectedPlacesAvailable = (long)initialNumberOfPlaces - 1 - 10;
        assertEquals("STUDENT events should only count confirmed and waiting list student places in availability calculations",
                expectedPlacesAvailable, actualPlacesAvailable);
        verify(dummyEventBookingPersistenceManager);
    }

    @Test
    public void getEventPage_checkWaitingListOnlyEventCapacity_capacityCalculatedCorrectly() throws
            Exception {
        EventBookingManager ebm = this.buildEventBookingManager();
        IsaacEventPageDTO testEvent = new IsaacEventPageDTO();
        testEvent.setId("someEventId");
        testEvent.setNumberOfPlaces(2);
        testEvent.setTags(ImmutableSet.of("student", "physics"));
        testEvent.setEventStatus(EventStatus.WAITING_LIST_ONLY);
        testEvent.setDate(someFutureDate);

        RegisteredUserDTO someUser = new RegisteredUserDTO();
        someUser.setId(6L);
        someUser.setEmailVerificationStatus(EmailVerificationStatus.VERIFIED);
        someUser.setRole(Role.STUDENT);

        EventBookingDTO firstBooking = new EventBookingDTO();
        UserSummaryDTO firstUser = new UserSummaryDTO();
        firstUser.setRole(Role.STUDENT);
        firstBooking.setUserBooked(firstUser);
        firstBooking.setBookingStatus(BookingStatus.CONFIRMED);

        EventBookingDTO secondBooking = new EventBookingDTO();
        UserSummaryDTO secondUser = new UserSummaryDTO();
        secondUser.setRole(Role.STUDENT);
        secondBooking.setUserBooked(secondUser);
        secondBooking.setBookingStatus(BookingStatus.WAITING_LIST);

        List<EventBookingDTO> currentBookings = Arrays.asList(firstBooking, secondBooking);

        Map<BookingStatus, Map<Role, Long>> placesAvailableMap = generatePlacesAvailableMap();
        placesAvailableMap.get(BookingStatus.CONFIRMED).put(Role.STUDENT, 1L);
        placesAvailableMap.get(BookingStatus.WAITING_LIST).put(Role.STUDENT, 1L);
        expect(dummyEventBookingPersistenceManager.getEventBookingStatusCounts(testEvent.getId(), false)).andReturn(placesAvailableMap).atLeastOnce();

        replay(dummyEventBookingPersistenceManager);
        Long placesAvailable = ebm.getPlacesAvailable(testEvent);
        Long expectedPlacesAvailable = 1L;
        assertEquals("WAITING_LIST_ONLY events should only count confirmed places in availability calculations",
                placesAvailable, expectedPlacesAvailable);
        verify(dummyEventBookingPersistenceManager);
    }

    @Test
    public void getEventPage_checkStudentEventReservedBookings_capacityCalculatedCorrectly() throws
            Exception {
        EventBookingManager ebm = this.buildEventBookingManager();
        IsaacEventPageDTO testEvent = new IsaacEventPageDTO();
        testEvent.setId("someEventId");
        testEvent.setNumberOfPlaces(2);
        testEvent.setTags(ImmutableSet.of("student", "physics"));
        testEvent.setEventStatus(EventStatus.OPEN);
        testEvent.setDate(new Date(System.currentTimeMillis()+24*60*60*1000)); // future dated
        testEvent.setAllowGroupReservations(true);

        // Mocks the counts for the places available calculation from the database
        // TODO we should make this a helper method in the test really
        Map<BookingStatus, Map<Role, Long>> placesAvailableMap = generatePlacesAvailableMap();

        RegisteredUserDTO someUser = new RegisteredUserDTO();
        someUser.setId(6L);
        someUser.setEmailVerificationStatus(EmailVerificationStatus.VERIFIED);
        someUser.setRole(Role.STUDENT);

        EventBookingDTO firstBooking = new EventBookingDTO();
        UserSummaryDTO firstUser = new UserSummaryDTO();
        firstUser.setRole(Role.STUDENT);
        firstBooking.setUserBooked(firstUser);
        firstBooking.setBookingStatus(BookingStatus.RESERVED);
        placesAvailableMap.get(BookingStatus.RESERVED).put(Role.STUDENT, 1L);

        EventBookingDTO secondBooking = new EventBookingDTO();
        UserSummaryDTO secondUser = new UserSummaryDTO();
        secondUser.setRole(Role.STUDENT);
        secondBooking.setUserBooked(secondUser);
        secondBooking.setBookingStatus(BookingStatus.CONFIRMED);
        placesAvailableMap.get(BookingStatus.CONFIRMED).put(Role.STUDENT, 1L);

        expect(dummyEventBookingPersistenceManager.getEventBookingStatusCounts(testEvent.getId(), false)).andReturn(placesAvailableMap).atLeastOnce();

        replay(dummyEventBookingPersistenceManager);
        Long placesAvailable = ebm.getPlacesAvailable(testEvent);
        Long expectedPlacesAvailable = 0L;
        assertEquals("RESERVED bookings should count towards the places available in availability calculations",
                expectedPlacesAvailable, placesAvailable);
        verify(dummyEventBookingPersistenceManager);
    }

    @Test
    public void isUserAbleToManageEvent_checkUsersWithDifferentRoles_success() throws Exception {
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

        Object[] mockedObjects = {dummyUserAssociationManager, dummyGroupManager};
        replay(mockedObjects);

        // Expected results
        Map<RegisteredUserDTO, Boolean> expectedResults =  ImmutableMap.of(
            teacher, false,
            unassociatedEventLeader, false,
            associatedEventLeader, true,
            eventManager, true,
            admin, true
        );

        for (RegisteredUserDTO user : expectedResults.keySet()) {
            assertEquals(String.format("Test case: %s", user.getRole()),
                    expectedResults.get(user),
                    eventBookingManager.isUserAbleToManageEvent(user, testEvent));
        }
        verify(mockedObjects);
    }

    @Test
    public void eventAllowsGroupBookings_checkAllCases_defaultIsFalseAndOtherwiseReportedCorrectly() {
        class TestCase {IsaacEventPageDTO eventPageDTO; Boolean expected; String assertion;}

        List<TestCase> testCases = ImmutableList.of(
            new TestCase() {{
                eventPageDTO = new IsaacEventPageDTO();
                expected = false;
                assertion = "The default case should return false";
            }},
            new TestCase() {{
                eventPageDTO = new IsaacEventPageDTO() {{setAllowGroupReservations(true);}};
                expected = true;
                assertion = "Events which allow group reservations should return true";
            }},
            new TestCase() {{
                eventPageDTO = new IsaacEventPageDTO() {{setAllowGroupReservations(false);}};
                expected = false;
                assertion = "Events which explicitly disallow group reservations should return false";
            }}
        );

        for (TestCase testCase : testCases) {
            boolean actual = EventBookingManager.eventAllowsGroupBookings(testCase.eventPageDTO);
            assertEquals(testCase.assertion, testCase.expected, actual);
        }
    }

    @Test
    public void requestReservations_reserveSpacesWhenThereAreAvailableSpaces_success() throws Exception {
        EventBookingManager eventBookingManager = buildEventBookingManager();
        ReservationTestDefaults testCase = new ReservationTestDefaults();
        List<RegisteredUserDTO> students = ImmutableList.of(testCase.student1, testCase.student2);

        // Make student two have a cancelled booking
        EventBookingDTO student2sCancelledBooking = new EventBookingDTO() {{
            setBookingStatus(BookingStatus.CANCELLED); setEventId(testCase.event.getId());
            setUserBooked(new UserSummaryDTO() {{setId(testCase.student2.getId());}});
        }};

        // Define expected external calls
        expect(dummyEventBookingPersistenceManager.isUserReserved(anyString(), anyLong())).andReturn(false).atLeastOnce();
        dummyEventBookingPersistenceManager.acquireDistributedLock(testCase.event.getId());
        expectLastCall().atLeastOnce();

        expect(dummyEventBookingPersistenceManager
                .getBookingByEventId(testCase.event.getId()))
                .andReturn(ImmutableList.of(student2sCancelledBooking)).once();

        // Make Reservations
        expect(dummyEventBookingPersistenceManager
                .getBookingByEventIdAndUserId(testCase.event.getId(), testCase.student1.getId()))
                .andReturn(null).once();
        expect(dummyEventBookingPersistenceManager
                .createBooking(eq(testCase.event.getId()), eq(testCase.student1.getId()), eq(testCase.teacher.getId()), eq(BookingStatus.RESERVED), anyObject()))
                .andReturn(testCase.student1Booking).once();
        expect(dummyEmailManager
                .getEmailTemplateDTO(("email-event-reservation-requested")))
                .andReturn(testCase.reservationEmail).once();
        dummyEmailManager.sendTemplatedEmailToUser(eq(testCase.student1), eq(testCase.reservationEmail), anyObject(), eq(EmailType.SYSTEM));
        expectLastCall().once();

        expect(dummyEventBookingPersistenceManager
                .getBookingByEventIdAndUserId(testCase.event.getId(), testCase.student2.getId()))
                .andReturn(student2sCancelledBooking).once();
        expect(dummyEventBookingPersistenceManager
                .updateBookingStatus(eq(testCase.event.getId()), eq(testCase.student2.getId()), eq(BookingStatus.RESERVED), anyObject()))
                .andReturn(testCase.student2Booking).once();
        expect(dummyEmailManager
                .getEmailTemplateDTO(("email-event-reservation-requested")))
                .andReturn(testCase.reservationEmail).once();
        dummyEmailManager.sendTemplatedEmailToUser(eq(testCase.student2), eq(testCase.reservationEmail), anyObject(), eq(EmailType.SYSTEM));
        expectLastCall().once();

        dummyEventBookingPersistenceManager.releaseDistributedLock(testCase.event.getId());
        expectLastCall().atLeastOnce();

        // Run the test for a student event
        Object[] mockedObjects = {dummyEventBookingPersistenceManager, dummyEmailManager, dummyPropertiesLoader};
        replay(mockedObjects);
        List<EventBookingDTO> actualResults = eventBookingManager.requestReservations(testCase.event, students, testCase.teacher);
        List<EventBookingDTO> expectedResults = ImmutableList.of(testCase.student1Booking, testCase.student2Booking);
        assertEquals("N results should be returned unaltered", expectedResults, actualResults);
        verify(mockedObjects);
    }

    @Test
    public void requestReservations_reserveSpacesForTwoWhenThereIsOnlyOneAvailableSpace_throwsEventIsFullException()
            throws Exception {
        EventBookingManager eventBookingManager = buildEventBookingManager();
        ReservationTestDefaults testCase = new ReservationTestDefaults();
        testCase.event.setNumberOfPlaces(1);
        List<RegisteredUserDTO> studentsToReserve = ImmutableList.of(testCase.student1, testCase.student2);

        // Define expected external calls
        expect(dummyEventBookingPersistenceManager.isUserReserved(anyString(), anyLong())).andReturn(false).atLeastOnce();
        dummyEventBookingPersistenceManager.acquireDistributedLock(testCase.event.getId());
        expectLastCall().atLeastOnce();

        expect(dummyEventBookingPersistenceManager.getEventBookingStatusCounts(testCase.event.getId(), false))
                .andReturn(Maps.newHashMap()).once();
        dummyEventBookingPersistenceManager.releaseDistributedLock(testCase.event.getId());
        expectLastCall().atLeastOnce();

        replay(dummyEventBookingPersistenceManager);
        try {
            eventBookingManager.requestReservations(testCase.event, studentsToReserve, testCase.teacher);
            fail("Expected to fail from trying to reserve 2 students onto an event with only one space.");
        } catch (EventIsFullException e) {
            // success
        }
        verify(dummyEventBookingPersistenceManager);
    }

    @Test
    public void requestReservations_reserveSpacesForMoreThanAllowed_throwsEventGroupReservationLimitException()
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
        EventBookingDTO existingEventBooking = new EventBookingDTO() {{
            setEventId(testCase.event.getId()); setBookingStatus(BookingStatus.CONFIRMED);
            setReservedById(testCase.teacher.getId());
            setUserBooked(new UserSummaryDTO() {{setId(previouslyReservedStudent.getId());}});
        }};

        // Define expected external calls
        expect(dummyEventBookingPersistenceManager.isUserReserved(anyString(), anyLong())).andReturn(false).atLeastOnce();
        dummyEventBookingPersistenceManager.acquireDistributedLock(testCase.event.getId());
        expectLastCall().atLeastOnce();
        expect(dummyEventBookingPersistenceManager
                .getEventBookingStatusCounts(testCase.event.getId(), false))
                .andReturn(previousBookingCounts).once();
        expect(dummyEventBookingPersistenceManager
                .getBookingByEventId(testCase.event.getId()))
                .andReturn(ImmutableList.of(existingEventBooking));
        dummyEventBookingPersistenceManager.releaseDistributedLock(testCase.event.getId());
        expectLastCall().atLeastOnce();

        replay(dummyEventBookingPersistenceManager);
        try {
            eventBookingManager.requestReservations(testCase.event, studentsToReserve, testCase.teacher);
            fail("Expected to fail from trying to reserve 2 students onto an event with only one space.");
        } catch (EventGroupReservationLimitException e) {
            // success
        }
        verify(dummyEventBookingPersistenceManager);
    }

    static class ReservationTestDefaults {
        IsaacEventPageDTO event = new IsaacEventPageDTO() {{
            setId("SomeEventId");
            setDate(EventBookingManagerTest.someFutureDate);
        }};
        RegisteredUserDTO teacher = new RegisteredUserDTO() {{setId(10L);}};
        RegisteredUserDTO student1 = new RegisteredUserDTO() {{
            setId(1L); setEmail("student1"); setEmailVerificationStatus(EmailVerificationStatus.VERIFIED);
        }};
        EventBookingDTO student1Booking = new EventBookingDTO();
        RegisteredUserDTO student2 = new RegisteredUserDTO() {{
            setId(2L); setEmail("student2"); setEmailVerificationStatus(EmailVerificationStatus.VERIFIED);
        }};
        EventBookingDTO student2Booking = new EventBookingDTO();
        RegisteredUserDTO student3 = new RegisteredUserDTO() {{
            setId(2L); setEmail("student2"); setEmailVerificationStatus(EmailVerificationStatus.VERIFIED);
        }};
        EmailTemplateDTO reservationEmail = new EmailTemplateDTO();
    }

    private EventBookingManager buildEventBookingManager() {
        return new EventBookingManager(dummyEventBookingPersistenceManager, dummyEmailManager, dummyUserAssociationManager, dummyPropertiesLoader, dummyGroupManager, dummyUserAccountManager);
    }

    static private Map<BookingStatus, Map<Role, Long>> generatePlacesAvailableMap() {
        Map<BookingStatus, Map<Role, Long>> placesAvailableMap = Maps.newHashMap();
        placesAvailableMap.put(BookingStatus.CANCELLED, Maps.newHashMap());
        placesAvailableMap.put(BookingStatus.WAITING_LIST, Maps.newHashMap());
        placesAvailableMap.put(BookingStatus.CONFIRMED, Maps.newHashMap());
        placesAvailableMap.put(BookingStatus.RESERVED, Maps.newHashMap());
        return placesAvailableMap;
    }
}