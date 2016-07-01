package uk.ac.cam.cl.dtg.isaac.api.managers;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import uk.ac.cam.cl.dtg.isaac.dao.EventBookingPersistenceManager;
import uk.ac.cam.cl.dtg.isaac.dos.eventbookings.BookingStatus;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacEventPageDTO;
import uk.ac.cam.cl.dtg.isaac.dto.eventbookings.EventBookingDTO;
import uk.ac.cam.cl.dtg.segue.api.managers.GroupManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAccountManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAssociationManager;
import uk.ac.cam.cl.dtg.segue.comm.EmailManager;
import uk.ac.cam.cl.dtg.segue.comm.EmailMustBeVerifiedException;
import uk.ac.cam.cl.dtg.segue.dos.users.EmailVerificationStatus;
import uk.ac.cam.cl.dtg.segue.dos.users.Role;
import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.UserSummaryDTO;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

/**
 * Created by sac92 on 07/05/2016.
 */
public class EventBookingManagerTest {
	private EventBookingPersistenceManager dummyEventBookingPersistenceManager;
	private GroupManager dummyGroupManager;
	private EmailManager dummyEmailManager;
	private UserAccountManager dummyUserManager;
	private UserAssociationManager userAssociationManager;

	/**
	 * Initial configuration of tests.
	 *
	 * @throws Exception
	 *             - test exception
	 */
	@Before
	public final void setUp() throws Exception {
		this.dummyGroupManager = createMock(GroupManager.class);
		this.dummyEmailManager = createMock(EmailManager.class);
		this.dummyUserManager = createMock(UserAccountManager.class);
		this.dummyEventBookingPersistenceManager = createMock(EventBookingPersistenceManager.class);
		this.userAssociationManager = createMock(UserAssociationManager.class);
	}

	@Test
	public void requestBooking_checkTeacherAllowedOnStudentEventDespiteCapacityFull_noExceptionThrown() throws Exception {
		EventBookingManager ebm = new EventBookingManager(dummyEventBookingPersistenceManager,dummyGroupManager,dummyEmailManager,dummyUserManager,userAssociationManager);
		IsaacEventPageDTO testEvent = new IsaacEventPageDTO();
		testEvent.setId("someEventId");
		testEvent.setNumberOfPlaces(1);
		testEvent.setTags(ImmutableSet.of("student", "physics"));

		RegisteredUserDTO someUser = new RegisteredUserDTO();
		someUser.setId(6L);
		someUser.setEmailVerificationStatus(EmailVerificationStatus.VERIFIED);
		someUser.setRole(Role.TEACHER);

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

		expect(dummyEventBookingPersistenceManager.createBooking(testEvent.getId(), someUser.getId(), BookingStatus.CONFIRMED)).andReturn(new EventBookingDTO()).atLeastOnce();

		dummyEventBookingPersistenceManager.releaseDistributedLock(testEvent.getId());
		expectLastCall().atLeastOnce();

		replay(dummyEventBookingPersistenceManager);

		ebm.requestBooking(testEvent, someUser);
	}

	@Test
	public void requestBooking_checkStudentNotAllowedOnStudentEventAsCapacityFull_eventFullExceptionThrown() throws Exception {
		EventBookingManager ebm = new EventBookingManager(dummyEventBookingPersistenceManager,dummyGroupManager,dummyEmailManager,dummyUserManager,userAssociationManager);
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

		expect(dummyEventBookingPersistenceManager.createBooking(testEvent.getId(), someUser.getId(), BookingStatus.CONFIRMED)).andReturn(new EventBookingDTO()).atLeastOnce();

		dummyEventBookingPersistenceManager.releaseDistributedLock(testEvent.getId());
		expectLastCall().atLeastOnce();

		replay(dummyEventBookingPersistenceManager);
		try {
			ebm.requestBooking(testEvent, someUser);
			fail("Expected an EventFullException and one didn't happen.");
		} catch (EventIsFullException e) {
			// success !
		}
	}

	@Test
	public void requestBooking_checkTeacherNotAllowedOnTeacherEventAsCapacityFull_eventFullExceptionThrown() throws Exception {
		EventBookingManager ebm = new EventBookingManager(dummyEventBookingPersistenceManager,dummyGroupManager,dummyEmailManager,dummyUserManager,userAssociationManager);
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

		expect(dummyEventBookingPersistenceManager.createBooking(testEvent.getId(), someUser.getId(), BookingStatus.CONFIRMED)).andReturn(new EventBookingDTO()).atLeastOnce();

		dummyEventBookingPersistenceManager.releaseDistributedLock(testEvent.getId());
		expectLastCall().atLeastOnce();

		replay(dummyEventBookingPersistenceManager);
		try {
			ebm.requestBooking(testEvent, someUser);
			fail("Expected an EventFullException and one didn't happen.");
		} catch (EventIsFullException e) {
			// success !
		}
	}

	@Test
	public void requestBooking_addressNotVerified_addressNotVerifiedExceptionThrown() throws Exception {
		EventBookingManager ebm = new EventBookingManager(dummyEventBookingPersistenceManager,dummyGroupManager,dummyEmailManager,dummyUserManager,userAssociationManager);
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
			ebm.requestBooking(testEvent, someUser);
			fail("Expected an EventFullException and one didn't happen.");
		} catch (EmailMustBeVerifiedException e) {
			// success !
		}
	}

	@Test
	public void requestBooking_expiredBooking_EventExpiredExceptionThrown() throws Exception {
		EventBookingManager ebm = new EventBookingManager(dummyEventBookingPersistenceManager,dummyGroupManager,dummyEmailManager,dummyUserManager,userAssociationManager);
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
			ebm.requestBooking(testEvent, someUser);
			fail("Expected an Event Expiry Exception and one didn't happen.");
		} catch (EventDeadlineException e) {
			// success !
		}
	}

	@Test
	public void requestBooking_cancelledSpaceAndWaitingList_SpaceRemainsFull() throws Exception {
		EventBookingManager ebm = new EventBookingManager(dummyEventBookingPersistenceManager,dummyGroupManager,dummyEmailManager,dummyUserManager,userAssociationManager);
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

		expect(dummyEventBookingPersistenceManager.createBooking(testEvent.getId(), someUser.getId(), BookingStatus.CONFIRMED)).andReturn(new EventBookingDTO()).atLeastOnce();

		dummyEventBookingPersistenceManager.releaseDistributedLock(testEvent.getId());
		expectLastCall().atLeastOnce();

		replay(dummyEventBookingPersistenceManager);
		try {
			ebm.requestBooking(testEvent, someUser);
			fail("Expected an EventFullException and one didn't happen.");
		} catch (EventIsFullException e) {
			// success !
		}
	}

	@Test
	public void requestBooking_cancelledSpaceAndNoWaitingList_Success() throws Exception {
		EventBookingManager ebm = new EventBookingManager(dummyEventBookingPersistenceManager,dummyGroupManager,dummyEmailManager,dummyUserManager,userAssociationManager);
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
		secondBooking.setBookingStatus(BookingStatus.CANCELLED);

		List<EventBookingDTO> currentBookings = Arrays.asList(firstBooking, secondBooking);

		expect(dummyEventBookingPersistenceManager.getBookingByEventId(testEvent.getId())).andReturn(currentBookings);
		expect(dummyEventBookingPersistenceManager.isUserBooked(testEvent.getId(), someUser.getId())).andReturn(false);

		dummyEventBookingPersistenceManager.acquireDistributedLock(testEvent.getId());
		expectLastCall().atLeastOnce();

		expect(dummyEventBookingPersistenceManager.createBooking(testEvent.getId(), someUser.getId(), BookingStatus.CONFIRMED)).andReturn(new EventBookingDTO()).atLeastOnce();

		dummyEventBookingPersistenceManager.releaseDistributedLock(testEvent.getId());
		expectLastCall().atLeastOnce();

		replay(dummyEventBookingPersistenceManager);

		try {
			ebm.requestBooking(testEvent, someUser);
			// success
		} catch (EventIsFullException e) {
			fail("Expected successful booking as no waiting list bookings.");
		}
	}

	@Test
	public void requestBooking_cancelledSpaceAndSomeWaitingList_Success() throws Exception {
		EventBookingManager ebm = new EventBookingManager(dummyEventBookingPersistenceManager,dummyGroupManager,dummyEmailManager,dummyUserManager,userAssociationManager);
		IsaacEventPageDTO testEvent = new IsaacEventPageDTO();
		testEvent.setId("someEventId");
		testEvent.setNumberOfPlaces(2);
		testEvent.setTags(ImmutableSet.of("teacher", "physics"));

		RegisteredUserDTO someUser = new RegisteredUserDTO();
		someUser.setId(6L);
		someUser.setEmailVerificationStatus(EmailVerificationStatus.VERIFIED);
		someUser.setRole(Role.TEACHER);

		EventBookingDTO firstBooking = new EventBookingDTO();
		UserSummaryDTO firstUser = new UserSummaryDTO();
		firstUser.setRole(Role.TEACHER);
		firstBooking.setUserBooked(firstUser);
		firstBooking.setBookingStatus(BookingStatus.WAITING_LIST);

		EventBookingDTO secondBooking = new EventBookingDTO();
		UserSummaryDTO secondUser = new UserSummaryDTO();
		secondUser.setRole(Role.TEACHER);
		secondBooking.setUserBooked(firstUser);
		secondBooking.setBookingStatus(BookingStatus.CANCELLED);

		List<EventBookingDTO> currentBookings = Arrays.asList(firstBooking, secondBooking);

		expect(dummyEventBookingPersistenceManager.getBookingByEventId(testEvent.getId())).andReturn(currentBookings);
		expect(dummyEventBookingPersistenceManager.isUserBooked(testEvent.getId(), someUser.getId())).andReturn(false);

		dummyEventBookingPersistenceManager.acquireDistributedLock(testEvent.getId());
		expectLastCall().atLeastOnce();

		expect(dummyEventBookingPersistenceManager.createBooking(testEvent.getId(), someUser.getId(), BookingStatus.CONFIRMED)).andReturn(new EventBookingDTO()).atLeastOnce();

		dummyEventBookingPersistenceManager.releaseDistributedLock(testEvent.getId());
		expectLastCall().atLeastOnce();

		replay(dummyEventBookingPersistenceManager);

		try {
			ebm.requestBooking(testEvent, someUser);
			// success
		} catch (EventIsFullException e) {
			fail("Expected successful booking as no waiting list bookings.");
		}
	}

	@Test
	public void promoteBooking_spaceDueToCancellation_Success() throws Exception {
		EventBookingManager ebm = new EventBookingManager(dummyEventBookingPersistenceManager,dummyGroupManager,dummyEmailManager,dummyUserManager,userAssociationManager);
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
		secondBooking.setBookingStatus(BookingStatus.CANCELLED);

		List<EventBookingDTO> currentBookings = Arrays.asList(firstBooking, secondBooking);

		expect(dummyEventBookingPersistenceManager.getBookingByEventId(testEvent.getId())).andReturn(currentBookings);
		expect(dummyEventBookingPersistenceManager.isUserBooked(testEvent.getId(), someUser.getId())).andReturn(false);

		expect(dummyEventBookingPersistenceManager.getBookingByEventIdAndUserId(testEvent.getId(), 6L)).andReturn(firstBooking);

		dummyEventBookingPersistenceManager.acquireDistributedLock(testEvent.getId());
		expectLastCall().atLeastOnce();

		expect(dummyEventBookingPersistenceManager.updateBookingStatus(testEvent.getId(), someUser.getId(), BookingStatus.CONFIRMED)).andReturn(new EventBookingDTO()).atLeastOnce();

		dummyEventBookingPersistenceManager.releaseDistributedLock(testEvent.getId());
		expectLastCall().atLeastOnce();

		replay(dummyEventBookingPersistenceManager);

		try {
			ebm.promoteFromWaitingList(testEvent, someUser);
			// success
		} catch (EventIsFullException e) {
			fail("Expected successful booking as no waiting list bookings.");
		}
	}


	@Test
	public void promoteBooking_NoSpace_Failure() throws Exception {
		EventBookingManager ebm = new EventBookingManager(dummyEventBookingPersistenceManager,dummyGroupManager,dummyEmailManager,dummyUserManager,userAssociationManager);
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

		expect(dummyEventBookingPersistenceManager.updateBookingStatus(testEvent.getId(), someUser.getId(), BookingStatus.CONFIRMED)).andReturn(new EventBookingDTO()).atLeastOnce();

		dummyEventBookingPersistenceManager.releaseDistributedLock(testEvent.getId());
		expectLastCall().atLeastOnce();

		replay(dummyEventBookingPersistenceManager);

		try {
			ebm.promoteFromWaitingList(testEvent, someUser);
			fail("Expected failure booking as no space for this event.");
		} catch (EventIsFullException e) {
			// success
		}
	}
}