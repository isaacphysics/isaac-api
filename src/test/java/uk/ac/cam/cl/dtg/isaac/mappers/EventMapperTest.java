package uk.ac.cam.cl.dtg.isaac.mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static uk.ac.cam.cl.dtg.CustomAssertions.assertDeepEquals;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.ac.cam.cl.dtg.isaac.dos.ExamBoard;
import uk.ac.cam.cl.dtg.isaac.dos.Stage;
import uk.ac.cam.cl.dtg.isaac.dos.eventbookings.BookingStatus;
import uk.ac.cam.cl.dtg.isaac.dos.users.EmailVerificationStatus;
import uk.ac.cam.cl.dtg.isaac.dos.users.Role;
import uk.ac.cam.cl.dtg.isaac.dos.users.UserContext;
import uk.ac.cam.cl.dtg.isaac.dto.eventbookings.DetailedEventBookingDTO;
import uk.ac.cam.cl.dtg.isaac.dto.eventbookings.EventBookingDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.UserSummaryDTO;

class EventMapperTest {

  private EventMapper eventMapper;
  private static final Instant testDate = Instant.now();
  private static final Instant newTestDate = Instant.now().plus(10000L, ChronoUnit.SECONDS);

  @BeforeEach
  void beforeEach() {
    eventMapper = EventMapper.INSTANCE;
  }

  @Test
  @DisplayName("Test mapping from DetailedEventBookingDTO to EventBookingDTO")
  void testCaseEventMapping() {
    DetailedEventBookingDTO source = prepareDetailedEventBookingDTO();
    EventBookingDTO expected = prepareEventBookingDTO();
    EventBookingDTO result = eventMapper.map(source, EventBookingDTO.class);
    assertEquals(expected.getClass(), result.getClass());
    assertDeepEquals(expected, result);
  }

  @Test
  void mapThrowsUnimplementedMappingExceptionForUnexpectedTarget() {
    DetailedEventBookingDTO source = new DetailedEventBookingDTO();
    Exception exception = assertThrows(
        UnimplementedMappingException.class, () -> eventMapper.map(source, DetailedEventBookingDTO.class));
    assertEquals("Invocation of unimplemented mapping from EventBookingDTO to DetailedEventBookingDTO", exception.getMessage());
  }

  @Test
  void copyEventBookingDTOReturnsNewObjectWithSameProperties() {
    EventBookingDTO source = prepareEventBookingDTO();
    EventBookingDTO actual = eventMapper.copy(source);
    assertEquals(actual.getClass(), source.getClass());
    assertNotSame(source, actual);
    assertDeepEquals(source, actual);
  }

  @Test
  @DisplayName("Testing mapList from DetailedEventBookingDTO to EventBookingDTO")
  void testCaseEventMapListOfDetailedEventBooking() {
    List<DetailedEventBookingDTO> detailedSourceList = prepareDetailedEventBookingDTOList();
    List<EventBookingDTO> expectedList = prepareEventBookingDTOList();
    List<EventBookingDTO> resultList = eventMapper.mapList(detailedSourceList, DetailedEventBookingDTO.class, EventBookingDTO.class);
    assertEquals(detailedSourceList.size(), resultList.size());
    assertEquals(EventBookingDTO.class, resultList.get(0).getClass());
    assertEquals(EventBookingDTO.class, resultList.get(1).getClass());
    assertDeepEquals(expectedList, resultList);
  }

  @Test
  @DisplayName("Testing mapList from EventBookingDTO to EventBookingDTO")
  void testCaseEventMapListCopyEventBooking() {
    List<EventBookingDTO> detailedSourceList = prepareEventBookingDTOList();
    List<EventBookingDTO> resultList = eventMapper.mapList(detailedSourceList, EventBookingDTO.class, EventBookingDTO.class);
    assertEquals(detailedSourceList.size(), resultList.size());
    assertEquals(EventBookingDTO.class, resultList.get(0).getClass());
    assertEquals(EventBookingDTO.class, resultList.get(1).getClass());
    assertNotSame(detailedSourceList.get(0), resultList.get(0));
    assertNotSame(detailedSourceList.get(1), resultList.get(1));
    assertDeepEquals(detailedSourceList, resultList);
  }

  @Test
  void mapListThrowsUnimplementedMappingExceptionForUnexpectedTarget() {
    List<DetailedEventBookingDTO> source = List.of(new DetailedEventBookingDTO());
    Exception exception = assertThrows(UnimplementedMappingException.class, () -> eventMapper.mapList(source, DetailedEventBookingDTO.class, DetailedEventBookingDTO.class));
    assertEquals("Invocation of unimplemented mapping from DetailedEventBookingDTO to DetailedEventBookingDTO", exception.getMessage());
  }

  private static EventBookingDTO prepareEventBookingDTO() {
    return new EventBookingDTO(
        3L,
        prepareUserSummaryDTO(),
        5L,
        "eventID",
        "eventTitle",
        testDate,
        testDate,
        newTestDate,
        BookingStatus.CONFIRMED);
  }

  private static DetailedEventBookingDTO prepareDetailedEventBookingDTO() {
    DetailedEventBookingDTO detailedEvent = new DetailedEventBookingDTO();
    detailedEvent.setBookingId(3L);
    detailedEvent.setUserBooked(prepareUserSummaryDTO());
    detailedEvent.setReservedById(5L);
    detailedEvent.setEventDate(testDate);
    detailedEvent.setEventId("eventID");
    detailedEvent.setEventTitle("eventTitle");
    detailedEvent.setBookingStatus(BookingStatus.CONFIRMED);
    detailedEvent.setBookingDate(testDate);
    detailedEvent.setUpdated(newTestDate);
    detailedEvent.setAdditionalInformation(prepareAdditionalInformation());
    return detailedEvent;
  }

  private List<DetailedEventBookingDTO> prepareDetailedEventBookingDTOList() {
    List<DetailedEventBookingDTO> detailedEventList = new ArrayList<>();
    DetailedEventBookingDTO event1 = new DetailedEventBookingDTO();
    event1.setBookingId(7L);
    event1.setUserBooked(prepareUserSummaryDTO());
    event1.setReservedById(9L);
    event1.setEventDate(testDate);
    event1.setEventId("eventID");
    event1.setEventTitle("eventTitle");
    event1.setBookingStatus(BookingStatus.RESERVED);
    event1.setBookingDate(testDate);
    event1.setUpdated(newTestDate);
    event1.setAdditionalInformation(prepareAdditionalInformation());
    detailedEventList.add(event1);

    DetailedEventBookingDTO event2 = new DetailedEventBookingDTO();
    event2.setBookingId(4L);
    event2.setUserBooked(prepareUserSummaryDTO());
    event2.setReservedById(1L);
    event2.setEventDate(testDate);
    event2.setEventId("eventID");
    event2.setEventTitle("eventTitle");
    event2.setBookingStatus(BookingStatus.RESERVED);
    event2.setBookingDate(testDate);
    event2.setUpdated(newTestDate);
    event2.setAdditionalInformation(prepareAdditionalInformation());
    detailedEventList.add(event2);
    return detailedEventList;
  }

  private List<EventBookingDTO> prepareEventBookingDTOList() {
    List<EventBookingDTO> eventList = new ArrayList<>();
    EventBookingDTO event1 = new EventBookingDTO();
    event1.setBookingId(7L);
    event1.setUserBooked(prepareUserSummaryDTO());
    event1.setReservedById(9L);
    event1.setEventDate(testDate);
    event1.setEventId("eventID");
    event1.setEventTitle("eventTitle");
    event1.setBookingStatus(BookingStatus.RESERVED);
    event1.setBookingDate(testDate);
    event1.setUpdated(newTestDate);
    eventList.add(event1);

    EventBookingDTO event2 = new EventBookingDTO();
    event2.setBookingId(4L);
    event2.setUserBooked(prepareUserSummaryDTO());
    event2.setReservedById(1L);
    event2.setEventDate(testDate);
    event2.setEventId("eventID");
    event2.setEventTitle("eventTitle");
    event2.setBookingStatus(BookingStatus.RESERVED);
    event2.setBookingDate(testDate);
    event2.setUpdated(newTestDate);
    eventList.add(event2);
    return eventList;
  }

  private static Map<String, String> prepareAdditionalInformation() {
    Map<String, String> additionalInformation = new HashMap<>();
    // Add additional information key-value pairs
    additionalInformation.put("key1", "value1");
    additionalInformation.put("key2", "value2");
    return additionalInformation;
  }

  private static UserSummaryDTO prepareUserSummaryDTO() {
    return setUserSummaryDTOCommonFields(new UserSummaryDTO());
  }

  private static <T extends UserSummaryDTO> T setUserSummaryDTOCommonFields(T object) {
    UserContext userContext = new UserContext();
    userContext.setStage(Stage.a_level);
    userContext.setExamBoard(ExamBoard.aqa);

    object.setId(2L);
    object.setGivenName("givenName");
    object.setFamilyName("familyName");
    object.setRole(Role.TEACHER);
    object.setAuthorisedFullAccess(true);
    object.setEmailVerificationStatus(EmailVerificationStatus.VERIFIED);
    object.setTeacherPending(true);
    object.setRegisteredContexts(List.of(userContext));
    return object;
  }
}

