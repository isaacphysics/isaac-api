package uk.ac.cam.cl.dtg.isaac.dao;

import static java.time.Instant.now;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.ac.cam.cl.dtg.CustomAssertions.assertDeepEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.ac.cam.cl.dtg.isaac.dos.eventbookings.BookingStatus;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacEventPageDTO;
import uk.ac.cam.cl.dtg.isaac.dto.eventbookings.DetailedEventBookingDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.UserSummaryWithEmailAddressAndGenderDTO;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAccountManager;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserException;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dao.content.GitContentManager;
import uk.ac.cam.cl.dtg.segue.database.PostgresSqlDb;

class EventBookingPersistenceManagerTest {

  private PostgresSqlDb mockEventsDatabase;
  private UserAccountManager mockAccountManager;
  private GitContentManager mockContentManager;
  private ObjectMapper mockObjectMapper;
  private EventBookingPersistenceManager eventBookingPersistenceManager;

  @BeforeEach
  public void setUp() {
    mockEventsDatabase = createMock(PostgresSqlDb.class);
    mockAccountManager = createMock(UserAccountManager.class);
    mockContentManager = createMock(GitContentManager.class);
    mockObjectMapper = createMock(ObjectMapper.class);
    eventBookingPersistenceManager = new EventBookingPersistenceManager(mockEventsDatabase, mockAccountManager, mockContentManager, mockObjectMapper);
  }

  @Test
  void testAdminGetBookingsByEventIds()
      throws SegueDatabaseException, ContentManagerException, SQLException, NoUserException {
    // Given
    List<String> eventIds = List.of("event1", "event2");
    String expectedQuery = "SELECT event_bookings.* FROM event_bookings JOIN users ON users.id=user_id WHERE event_id IN (?, ?) AND NOT users.deleted";
    Instant now = now();

    IsaacEventPageDTO eventPage1 = new IsaacEventPageDTO();
    IsaacEventPageDTO eventPage2 = new IsaacEventPageDTO();
    RegisteredUserDTO bookedUser1 = new RegisteredUserDTO();
    RegisteredUserDTO bookedUser2 = new RegisteredUserDTO();
    UserSummaryWithEmailAddressAndGenderDTO bookedUserSummary1 = new UserSummaryWithEmailAddressAndGenderDTO();
    UserSummaryWithEmailAddressAndGenderDTO bookedUserSummary2 = new UserSummaryWithEmailAddressAndGenderDTO();

    expect(mockContentManager.getContentById("event1")).andReturn(eventPage1);
    expect(mockContentManager.getContentById("event2")).andReturn(eventPage2);

    Connection dummyConnection = createMock(Connection.class);
    PreparedStatement dummyPreparedStatement = createMock(PreparedStatement.class);
    ResultSet dummyResultSet = createMock(ResultSet.class);

    expect(mockEventsDatabase.getDatabaseConnection()).andReturn(dummyConnection);
    expect(dummyConnection.prepareStatement(expectedQuery)).andReturn(dummyPreparedStatement);
    dummyPreparedStatement.setString(1, "event1");
    dummyPreparedStatement.setString(2, "event2");
    expect(dummyPreparedStatement.executeQuery()).andReturn(dummyResultSet);

    expect(dummyResultSet.next()).andReturn(true);
    prepareEventBookingResultSet(dummyResultSet, 1L, "event1", now);

    expect(dummyResultSet.next()).andReturn(true);
    prepareEventBookingResultSet(dummyResultSet, 2L, "event2", now);

    expect(dummyResultSet.next()).andReturn(false);
    dummyResultSet.close();
    dummyPreparedStatement.close();
    dummyConnection.close();

    expect(mockAccountManager.getUserDTOById(1L, true)).andReturn(bookedUser1);
    expect(mockAccountManager.convertToUserSummary(bookedUser1, UserSummaryWithEmailAddressAndGenderDTO.class)).andReturn(bookedUserSummary1);

    expect(mockAccountManager.getUserDTOById(2L, true)).andReturn(bookedUser2);
    expect(mockAccountManager.convertToUserSummary(bookedUser2, UserSummaryWithEmailAddressAndGenderDTO.class)).andReturn(bookedUserSummary2);

    Object[] mockedObjects = {mockEventsDatabase, mockAccountManager, mockContentManager, mockObjectMapper,
        dummyConnection, dummyPreparedStatement, dummyResultSet};
    replay(mockedObjects);

    // When
    Map<String, List<DetailedEventBookingDTO>> result = eventBookingPersistenceManager.adminGetBookingsByEventIds(eventIds);

    // Then
    DetailedEventBookingDTO expectedBooking1 = prepareDetailedEventBookingDTO(1L, now);
    DetailedEventBookingDTO expectedBooking2 = prepareDetailedEventBookingDTO(2L, now);
    assertEquals(2, result.size());
    assertDeepEquals(List.of(expectedBooking1), result.get("event1"));
    assertDeepEquals(List.of(expectedBooking2), result.get("event2"));

    verify(mockedObjects);
  }

  @NotNull
  private static DetailedEventBookingDTO prepareDetailedEventBookingDTO(long bookingId, Instant createAndUpdateTime) {
    DetailedEventBookingDTO expectedBooking1 = new DetailedEventBookingDTO();
    expectedBooking1.setBookingId(bookingId);
    expectedBooking1.setUserBooked(new UserSummaryWithEmailAddressAndGenderDTO());
    expectedBooking1.setReservedById(7L);
    expectedBooking1.setBookingStatus(BookingStatus.CONFIRMED);
    expectedBooking1.setBookingDate(createAndUpdateTime);
    expectedBooking1.setUpdated(createAndUpdateTime);
    return expectedBooking1;
  }

  private static void prepareEventBookingResultSet(ResultSet dummyResultSet, long eventId, String event, Instant createAndUpdateTime)
      throws SQLException {
    expect(dummyResultSet.getLong("id")).andReturn(eventId);
    expect(dummyResultSet.getLong("user_id")).andReturn(eventId);
    expect(dummyResultSet.getLong("reserved_by")).andReturn(7L);
    expect(dummyResultSet.getString("event_id")).andReturn(event);
    expect(dummyResultSet.getString("status")).andReturn("CONFIRMED");
    expect(dummyResultSet.getTimestamp("created")).andReturn(Timestamp.from(createAndUpdateTime));
    expect(dummyResultSet.getTimestamp("updated")).andReturn(Timestamp.from(createAndUpdateTime));
    expect(dummyResultSet.getObject("additional_booking_information")).andReturn(null);
  }
}

