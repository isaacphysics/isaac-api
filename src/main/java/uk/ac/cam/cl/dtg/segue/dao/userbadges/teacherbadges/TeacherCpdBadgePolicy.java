package uk.ac.cam.cl.dtg.segue.dao.userbadges.teacherbadges;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.util.Iterator;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.api.managers.EventBookingManager;
import uk.ac.cam.cl.dtg.isaac.dos.eventbookings.BookingStatus;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacEventPageDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dao.content.GitContentManager;
import uk.ac.cam.cl.dtg.segue.dao.userbadges.IUserBadgePolicy;

public class TeacherCpdBadgePolicy implements IUserBadgePolicy {
  private static final Logger log = LoggerFactory.getLogger(TeacherCpdBadgePolicy.class);

  private final EventBookingManager bookingManager;
  private final GitContentManager contentManager;

  public TeacherCpdBadgePolicy(final EventBookingManager bookingManager, final GitContentManager contentManager) {
    this.bookingManager = bookingManager;
    this.contentManager = contentManager;
  }

  @Override
  public int getLevel(final JsonNode state) {
    return state.get("cpdEvents").size();
  }

  @Override
  public JsonNode initialiseState(final RegisteredUserDTO user) {

    ArrayNode events = JsonNodeFactory.instance.arrayNode();

    try {
      Map<String, BookingStatus> userBookings = bookingManager.getAllEventStatesForUser(user.getId());

      for (String eventId : userBookings.keySet()) {

        if (!BookingStatus.ATTENDED.equals(userBookings.get(eventId))) {
          continue;
        }

        ContentDTO content = getContentDetails(eventId);
        if (content instanceof IsaacEventPageDTO) {

          if (content.getTags().contains("teacher")) {
            events.add(content.getId());
          }
        }
      }

    } catch (SegueDatabaseException | ContentManagerException e) {
      log.error("Error initialising state", e);
    }

    return JsonNodeFactory.instance.objectNode().set("cpdEvents", events);
  }

  @Override
  public JsonNode updateState(final JsonNode state, final String event) {

    Iterator<JsonNode> iter = state.get("cpdEvents").elements();

    while (iter.hasNext()) {
      if (iter.next().asText().equals(event)) {
        return state;
      }
    }

    ((ArrayNode) state.get("cpdEvents")).add(event);
    return state;
  }


  /**
   * Get the ContentDTO object (presumably an event page) associated with an event id.
   *
   * @param eventId - the event id to search for
   * @return the ContentDTO associated with the id
   * @throws ContentManagerException if the subsequent method(s) provide neither a search term nor filter instructions
   */
  private ContentDTO getContentDetails(final String eventId) throws ContentManagerException {
    return this.contentManager.getContentById(eventId);
  }
}
