package uk.ac.cam.cl.dtg.isaac.api.managers;

import com.google.api.client.util.Maps;
import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Test;
import uk.ac.cam.cl.dtg.isaac.dao.EventBookingPersistenceManager;
import uk.ac.cam.cl.dtg.isaac.dos.EventStatus;
import uk.ac.cam.cl.dtg.isaac.dos.eventbookings.BookingStatus;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacEventPageDTO;
import uk.ac.cam.cl.dtg.isaac.dto.eventbookings.EventBookingDTO;
import uk.ac.cam.cl.dtg.segue.api.managers.GroupManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAssociationManager;
import uk.ac.cam.cl.dtg.segue.comm.EmailManager;
import uk.ac.cam.cl.dtg.segue.comm.EmailMustBeVerifiedException;
import uk.ac.cam.cl.dtg.segue.comm.EmailType;
import uk.ac.cam.cl.dtg.segue.dos.users.EmailVerificationStatus;
import uk.ac.cam.cl.dtg.segue.dos.users.Role;
import uk.ac.cam.cl.dtg.segue.dto.content.EmailTemplateDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.UserSummaryDTO;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;
import static uk.ac.cam.cl.dtg.segue.api.Constants.*;

/**
 * EventBookingManagerTest.
 */
public class EventBookingManagerTest {
    private EventBookingPersistenceManager dummyEventBookingPersistenceManager;
    private EmailManager dummyEmailManager;
    private UserAssociationManager userAssociationManager;
    private Map<String, String> someAdditionalInformation;
    private PropertiesLoader dummyPropertiesLoader;
    private GroupManager groupManager;

    /**
     * Initial configuration of tests.
     *
     * @throws Exception - test exception
     */
    @Before
    public final void setUp() throws Exception {
        this.dummyEmailManager = createMock(EmailManager.class);
        this.dummyEventBookingPersistenceManager = createMock(EventBookingPersistenceManager.class);
        this.userAssociationManager = createMock(UserAssociationManager.class);
        this.groupManager = createMock(GroupManager.class);
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
        List<EventBookingDTO> currentBookings = Arrays.asList(firstBooking);

        expect(dummyEventBookingPersistenceManager.getBookingByEventId(testEvent.getId())).andReturn(currentBookings);
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

        replay(dummyEventBookingPersistenceManager, dummyPropertiesLoader, dummyEmailManager);

        ebm.requestBooking(testEvent, someUser, someAdditionalInformation);
    }

    @Test
    public void requestBooking_checkStudentNotAllowedOnStudentEventAsCapacityFull_eventFullExceptionThrown() throws
			Exception {
        EventBookingManager ebm = this.buildEventBookingManager();
        IsaacEventPageDTO testEvent = new IsaacEventPageDTO();
        testEvent.setId("someEventId");
        testEvent.setNumberOfPlaces(1);
        testEvent.setTags(ImmutableSet.of("student", "physics"));

        RegisteredUserDTO someUser = new RegisteredUserDTO();
        someUser.setId(6L);
        someUser.setEmailVerificationStatus(EmailVerificationStatus.VERIFIED);
        someUser.setRole(Role.STUDENT);

        EventBookingDTO firstBooking = new EventBookingDTO();
        UserSummaryDTO firstUser = new UserSummaryDTO();
        firstUser.setRole(Role.STUDENT);
        firstBooking.setUserBooked(firstUser);
        firstBooking.setBookingStatus(BookingStatus.CONFIRMED);
        List<EventBookingDTO> currentBookings = Arrays.asList(firstBooking);

        expect(dummyEventBookingPersistenceManager.getBookingByEventId(testEvent.getId())).andReturn(currentBookings);
        expect(dummyEventBookingPersistenceManager.isUserBooked(testEvent.getId(), someUser.getId())).andReturn(false);

        dummyEventBookingPersistenceManager.acquireDistributedLock(testEvent.getId());
        expectLastCall().atLeastOnce();

        expect(dummyEventBookingPersistenceManager.createBooking(testEvent.getId(), someUser.getId(), BookingStatus
				.CONFIRMED, someAdditionalInformation)).andReturn(firstBooking).atLeastOnce();

        dummyEventBookingPersistenceManager.releaseDistributedLock(testEvent.getId());
        expectLastCall().atLeastOnce();

        replay(dummyEventBookingPersistenceManager);
        try {
            ebm.requestBooking(testEvent, someUser, someAdditionalInformation);
            fail("Expected an EventFullException and one didn't happen.");
        } catch (EventIsFullException e) {
            // success !
        }
    }

    @Test
    public void requestBooking_checkTeacherNotAllowedOnTeacherEventAsCapacityFull_eventFullExceptionThrown() throws
			Exception {
        EventBookingManager ebm = this.buildEventBookingManager();
        IsaacEventPageDTO testEvent = new IsaacEventPageDTO();
        testEvent.setId("someEventId");
        testEvent.setNumberOfPlaces(1);
        testEvent.setTags(ImmutableSet.of("teacher", "physics"));

        RegisteredUserDTO someUser = new RegisteredUserDTO();
        someUser.setId(6L);
        someUser.setEmailVerificationStatus(EmailVerificationStatus.VERIFIED);
        someUser.setRole(Role.TEACHER);

        EventBookingDTO firstBooking = new EventBookingDTO();
        UserSummaryDTO firstUser = new UserSummaryDTO();
        firstUser.setRole(Role.TEACHER);
        firstBooking.setUserBooked(firstUser);
        firstBooking.setBookingStatus(BookingStatus.CONFIRMED);
        List<EventBookingDTO> currentBookings = Arrays.asList(firstBooking);

        expect(dummyEventBookingPersistenceManager.getBookingByEventId(testEvent.getId())).andReturn(currentBookings);
        expect(dummyEventBookingPersistenceManager.isUserBooked(testEvent.getId(), someUser.getId())).andReturn(false);

        dummyEventBookingPersistenceManager.acquireDistributedLock(testEvent.getId());
        expectLastCall().atLeastOnce();

        expect(dummyEventBookingPersistenceManager.createBooking(testEvent.getId(), someUser.getId(), BookingStatus
				.CONFIRMED, someAdditionalInformation)).andReturn(new EventBookingDTO()).atLeastOnce();

        dummyEventBookingPersistenceManager.releaseDistributedLock(testEvent.getId());
        expectLastCall().atLeastOnce();

        replay(dummyEventBookingPersistenceManager);
        try {
            ebm.requestBooking(testEvent, someUser, someAdditionalInformation);
            fail("Expected an EventFullException and one didn't happen.");
        } catch (EventIsFullException e) {
            // success !
        }
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

        expect(dummyEventBookingPersistenceManager.isUserBooked(testEvent.getId(), someUser.getId())).andReturn(false);

        replay(dummyEventBookingPersistenceManager);
        try {
            ebm.requestBooking(testEvent, someUser, someAdditionalInformation);
            fail("Expected an Event Expiry Exception and one didn't happen.");
        } catch (EventDeadlineException e) {
            // success !
        }
    }

    @Test
    public void requestBooking_cancelledSpaceAndWaitingList_SpaceRemainsFull() throws Exception {
        EventBookingManager ebm = this.buildEventBookingManager();
        IsaacEventPageDTO testEvent = new IsaacEventPageDTO();
        testEvent.setId("someEventId");
        testEvent.setNumberOfPlaces(1);
        testEvent.setTags(ImmutableSet.of("teacher", "physics"));

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

        List<EventBookingDTO> currentBookings = Arrays.asList(firstBooking, secondBooking);

        expect(dummyEventBookingPersistenceManager.getBookingByEventId(testEvent.getId())).andReturn(currentBookings);
        expect(dummyEventBookingPersistenceManager.isUserBooked(testEvent.getId(), someUser.getId())).andReturn(false);

        dummyEventBookingPersistenceManager.acquireDistributedLock(testEvent.getId());
        expectLastCall().atLeastOnce();

        expect(dummyEventBookingPersistenceManager.createBooking(testEvent.getId(), someUser.getId(), BookingStatus
				.CONFIRMED, someAdditionalInformation)).andReturn(new EventBookingDTO()).atLeastOnce();

        dummyEventBookingPersistenceManager.releaseDistributedLock(testEvent.getId());
        expectLastCall().atLeastOnce();

        replay(dummyEventBookingPersistenceManager);
        try {
            ebm.requestBooking(testEvent, someUser, someAdditionalInformation);
            fail("Expected an EventFullException and one didn't happen.");
        } catch (EventIsFullException e) {
            // success !
        }
    }

    @Test
    public void requestBooking_cancelledSpaceAndNoWaitingList_Success() throws Exception {
        EventBookingManager ebm = this.buildEventBookingManager();
        IsaacEventPageDTO testEvent = new IsaacEventPageDTO();
        testEvent.setId("someEventId");
        testEvent.setNumberOfPlaces(1);
        testEvent.setTags(ImmutableSet.of("teacher", "physics"));
        testEvent.setEmailEventDetails("Some Details");

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

        List<EventBookingDTO> currentBookings = Arrays.asList(secondBooking);

        expect(dummyEventBookingPersistenceManager.getBookingByEventId(testEvent.getId())).andReturn(currentBookings);
        expect(dummyEventBookingPersistenceManager.getBookingByEventIdAndUserId(testEvent.getId(), someUser.getId()))
				.andReturn(null);

        expect(dummyEventBookingPersistenceManager.isUserBooked(testEvent.getId(), someUser.getId())).andReturn(false);

        dummyEventBookingPersistenceManager.acquireDistributedLock(testEvent.getId());
        expectLastCall().atLeastOnce();

        expect(dummyEventBookingPersistenceManager.createBooking(testEvent.getId(), someUser.getId(), BookingStatus
				.CONFIRMED, someAdditionalInformation)).andReturn(secondBooking).atLeastOnce();

        dummyEventBookingPersistenceManager.releaseDistributedLock(testEvent.getId());
        expectLastCall().atLeastOnce();

        expect(dummyEmailManager.getEmailTemplateDTO("email-event-booking-confirmed")).andReturn(new EmailTemplateDTO()).atLeastOnce();

        dummyEmailManager.sendTemplatedEmailToUser(anyObject(), anyObject(), anyObject(), anyObject(), anyObject());
        expectLastCall().atLeastOnce();

        replay(dummyEventBookingPersistenceManager, dummyPropertiesLoader, dummyEmailManager);

        try {
            ebm.requestBooking(testEvent, someUser, someAdditionalInformation);
            // success
        } catch (EventIsFullException e) {
            fail("Expected successful booking as no waiting list bookings.");
        }
    }

    @Test
    public void requestBooking_cancelledSpaceAndSomeWaitingList_Success() throws Exception {
        EventBookingManager ebm = this.buildEventBookingManager();
        IsaacEventPageDTO testEvent = new IsaacEventPageDTO();
        testEvent.setId("someEventId");
        testEvent.setNumberOfPlaces(2);
        testEvent.setTags(ImmutableSet.of("teacher", "physics"));
        testEvent.setEmailEventDetails("Some Details");

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

        expect(dummyEventBookingPersistenceManager.getBookingByEventId(testEvent.getId())).andReturn(currentBookings);
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

        replay(dummyEventBookingPersistenceManager, dummyPropertiesLoader, dummyEmailManager);

        try {
            ebm.requestBooking(testEvent, firstUserFull, someAdditionalInformation);
            // success
        } catch (EventIsFullException e) {
            fail("Expected successful booking as no waiting list bookings.");
        }
    }

    @Test
    public void promoteBooking_spaceDueToCancellation_Success() throws Exception {
        EventBookingManager ebm = this.buildEventBookingManager();
        IsaacEventPageDTO testEvent = new IsaacEventPageDTO();
        testEvent.setId("someEventId");
        testEvent.setNumberOfPlaces(1);
        testEvent.setTags(ImmutableSet.of("teacher", "physics"));
        testEvent.setEmailEventDetails("some details");

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

        List<EventBookingDTO> currentBookings = Arrays.asList(firstBooking, secondBooking);

        expect(dummyEventBookingPersistenceManager.getBookingByEventId(testEvent.getId())).andReturn(currentBookings);
        expect(dummyEventBookingPersistenceManager.isUserBooked(testEvent.getId(), someUser.getId())).andReturn(false);

        expect(dummyEventBookingPersistenceManager.getBookingByEventIdAndUserId(testEvent.getId(), 6L)).andReturn
				(firstBooking);

        dummyEventBookingPersistenceManager.acquireDistributedLock(testEvent.getId());
        expectLastCall().atLeastOnce();

        expect(dummyEventBookingPersistenceManager.updateBookingStatus(testEvent.getId(), someUser.getId(),
				BookingStatus.CONFIRMED, someAdditionalInformation)).andReturn(secondBooking).atLeastOnce();

        dummyEventBookingPersistenceManager.releaseDistributedLock(testEvent.getId());
        expectLastCall().atLeastOnce();

        expect(dummyEmailManager.getEmailTemplateDTO("email-event-booking-waiting-list-promotion-confirmed")).andReturn(new EmailTemplateDTO()).atLeastOnce();

        dummyEmailManager.sendTemplatedEmailToUser(anyObject(), anyObject(), anyObject(), anyObject(), anyObject());
        expectLastCall().atLeastOnce();

        replay(dummyEventBookingPersistenceManager, dummyPropertiesLoader, dummyEmailManager);

        try {
            ebm.promoteFromWaitingListOrCancelled(testEvent, someUser);
            // success
        } catch (EventIsFullException e) {
            fail("Expected successful booking as no waiting list bookings.");
        }
    }

    @Test
    public void promoteBooking_NoSpace_Failure() throws Exception {
        EventBookingManager ebm = this.buildEventBookingManager();
        IsaacEventPageDTO testEvent = new IsaacEventPageDTO();
        testEvent.setId("someEventId");
        testEvent.setNumberOfPlaces(1);
        testEvent.setTags(ImmutableSet.of("teacher", "physics"));

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

        expect(dummyEventBookingPersistenceManager.getBookingByEventId(testEvent.getId())).andReturn(currentBookings);
        expect(dummyEventBookingPersistenceManager.isUserBooked(testEvent.getId(), someUser.getId())).andReturn(false);

        expect(dummyEventBookingPersistenceManager.getBookingByEventIdAndUserId(testEvent.getId(), 6L)).andReturn(firstBooking);

        dummyEventBookingPersistenceManager.acquireDistributedLock(testEvent.getId());
        expectLastCall().atLeastOnce();

        expect(dummyEventBookingPersistenceManager.updateBookingStatus(testEvent.getId(), someUser.getId(), BookingStatus.CONFIRMED, someAdditionalInformation)).andReturn(new EventBookingDTO()).atLeastOnce();

        dummyEventBookingPersistenceManager.releaseDistributedLock(testEvent.getId());
        expectLastCall().atLeastOnce();

        replay(dummyEventBookingPersistenceManager);

        try {
            ebm.promoteFromWaitingListOrCancelled(testEvent, someUser);
            fail("Expected failure booking as no space for this event.");
        } catch (EventIsFullException e) {
            // success
        }
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

        expect(dummyEventBookingPersistenceManager.getBookingByEventId(testEvent.getId())).andReturn(currentBookings);
        expect(dummyEventBookingPersistenceManager.isUserBooked(testEvent.getId(), someUser.getId())).andReturn(false);

        dummyEventBookingPersistenceManager.acquireDistributedLock(testEvent.getId());
        expectLastCall().atLeastOnce();

        expect(dummyEventBookingPersistenceManager.createBooking(testEvent.getId(), someUser.getId(), BookingStatus
                .CONFIRMED, someAdditionalInformation)).andReturn(firstBooking).atLeastOnce();

        dummyEventBookingPersistenceManager.releaseDistributedLock(testEvent.getId());
        expectLastCall().atLeastOnce();

        replay(dummyEventBookingPersistenceManager);
        Integer placesAvailable = ebm.getPlacesAvailable(testEvent);
        Integer expectedPlacesAvailable = 1;
        assertEquals("WAITING_LIST_ONLY events should only count confirmed places in availability calculations",
                placesAvailable, expectedPlacesAvailable);
    }

    private EventBookingManager buildEventBookingManager() {
        return new EventBookingManager(dummyEventBookingPersistenceManager, dummyEmailManager, userAssociationManager, dummyPropertiesLoader, groupManager);
    }
}