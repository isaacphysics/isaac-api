package uk.ac.cam.cl.dtg.segue.dao.userBadges.teacherBadges;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import uk.ac.cam.cl.dtg.isaac.api.managers.EventBookingManager;
import uk.ac.cam.cl.dtg.isaac.dos.eventbookings.BookingStatus;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacEventPageDTO;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dao.content.GitContentManager;
import uk.ac.cam.cl.dtg.segue.dao.userBadges.IUserBadgePolicy;
import uk.ac.cam.cl.dtg.isaac.dos.ITransaction;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.RegisteredUserDTO;

import java.util.Iterator;
import java.util.Map;

/**
 * Created by du220 on 02/05/2018.
 */
public class TeacherCpdBadgePolicy implements IUserBadgePolicy {

    private final EventBookingManager bookingManager;
    private final GitContentManager contentManager;
    private final String contentIndex;

    public TeacherCpdBadgePolicy(final EventBookingManager bookingManager,
                                 final GitContentManager contentManager,
                                 final String contentIndex) {
        this.bookingManager = bookingManager;
        this.contentManager = contentManager;
        this.contentIndex = contentIndex;
    }

    @Override
    public int getLevel(final JsonNode state) {
        return state.get("cpdEvents").size();
    }

    @Override
    public JsonNode initialiseState(final RegisteredUserDTO user, final ITransaction transaction) {

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
            e.printStackTrace();
        }

        return JsonNodeFactory.instance.objectNode().set("cpdEvents", events);
    }

    @Override
    public JsonNode updateState(final RegisteredUserDTO user, final JsonNode state, final String event) {

        Iterator<JsonNode> iter = ((ArrayNode) state.get("cpdEvents")).elements();

        while (iter.hasNext()) {
            if (iter.next().asText().equals(event)) {
                return state;
            }
        }

        ((ArrayNode) state.get("cpdEvents")).add(event);
        return state;
    }


    /**
     *
     * @param eventId - the event id to search for
     * @return the ContentDTO associated with the id
     * @throws ContentManagerException if the subsequent method(s) provide neither a search term nor filter instructions
     */
    private ContentDTO getContentDetails(final String eventId) throws ContentManagerException {
        return this.contentManager.getContentById(eventId);
    }
}
