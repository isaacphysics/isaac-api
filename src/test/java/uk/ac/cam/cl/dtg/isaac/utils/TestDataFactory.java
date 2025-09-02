package uk.ac.cam.cl.dtg.isaac.utils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import uk.ac.cam.cl.dtg.isaac.dos.EventStatus;
import uk.ac.cam.cl.dtg.isaac.dos.content.ExternalReference;
import uk.ac.cam.cl.dtg.isaac.dos.eventbookings.BookingStatus;
import uk.ac.cam.cl.dtg.isaac.dos.users.EmailVerificationStatus;
import uk.ac.cam.cl.dtg.isaac.dos.users.Role;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacEventPageDTO;
import uk.ac.cam.cl.dtg.isaac.dto.eventbookings.DetailedEventBookingDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.UserSummaryDTO;

public class TestDataFactory {

  public static DetailedEventBookingDTO createBooking(Long id, String userName, String eventId) {
    DetailedEventBookingDTO dto = new DetailedEventBookingDTO();
    dto.setBookingId(id);
    dto.setUserBooked(createUser(id, userName));
    dto.setEventId(eventId);
    dto.setBookingDate(Instant.now().minus(id, ChronoUnit.HOURS));
    dto.setBookingStatus(BookingStatus.CONFIRMED);
    return dto;
  }

  public static UserSummaryDTO createUser(Long id, String name) {
    UserSummaryDTO dto = new UserSummaryDTO();
    dto.setId(id);
    dto.setGivenName(name);
    dto.setFamilyName("TestUser");
    dto.setEmailVerificationStatus(EmailVerificationStatus.VERIFIED);
    return dto;
  }

  public static RegisteredUserDTO createMockUser(String email, String name) {
    RegisteredUserDTO user = new RegisteredUserDTO();
    user.setId(1L);
    user.setEmail(email);
    user.setGivenName(name);
    user.setRole(Role.STUDENT);
    user.setEmailVerificationStatus(EmailVerificationStatus.VERIFIED);
    return user;
  }

  public static IsaacEventPageDTO createTestEvent(String id, int hoursAgo, List<ExternalReference> postResources) {
    IsaacEventPageDTO event = new IsaacEventPageDTO();
    event.setId(id);
    event.setEventStatus(EventStatus.OPEN);
    event.setDate(Instant.now().minus(hoursAgo, ChronoUnit.HOURS));
    event.setPostResources(postResources);
    event.setEventSurvey("https://survey.example.com");
    event.setEventSurveyTitle("Test Survey");
    return event;
  }

}
