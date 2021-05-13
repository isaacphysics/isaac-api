package uk.ac.cam.cl.dtg.segue.dao.userBadges.teacherBadges;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import uk.ac.cam.cl.dtg.isaac.api.managers.EventBookingManager;
import uk.ac.cam.cl.dtg.isaac.dos.eventbookings.BookingStatus;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacEventPageDTO;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dao.content.IContentManager;
import uk.ac.cam.cl.dtg.segue.dao.userBadges.IUserBadgePolicy;
import uk.ac.cam.cl.dtg.segue.dos.ITransaction;
import uk.ac.cam.cl.dtg.segue.dto.content.ContentDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;

import java.util.Iterator;
import java.util.Map;

/**
 * Created by du220 on 02/05/2018.
 */
public class TeacherCpdBadgePolicy implements IUserBadgePolicy {

    private final EventBookingManager bookingManager;
    private final IContentManager contentManager;
    private final String contentIndex;

    public TeacherCpdBadgePolicy(EventBookingManager bookingManager,
                                 IContentManager contentManager,
                                 String contentIndex) {
        this.bookingManager = bookingManager;
        this.contentManager = contentManager;
        this.contentIndex = contentIndex;
    }

    @Override
    public int getLevel(JsonNode state) {
        return state.get("cpdEvents").size();
    }

    @Override
    public JsonNode initialiseState(RegisteredUserDTO user, ITransaction transaction) {

        ArrayNode events = JsonNodeFactory.instance.arrayNode();

        try {
            Map<String, BookingStatus> userBookings = bookingManager.getAllEventStatesForUser(user.getId());

            for (String eventId : userBookings.keySet()) {

                if (!BookingStatus.ATTENDED.equals(userBookings.get(eventId))) {
                    continue;
                }

                ContentDTO content = getcontentDetails(eventId);
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
    public JsonNode updateState(RegisteredUserDTO user, JsonNode state, String event) {

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
     * @param eventId
     * @return
     * @throws ContentManagerException
     */
    private ContentDTO getcontentDetails(String eventId) throws ContentManagerException {
        return this.contentManager.getContentById(this.contentIndex, eventId);
    }
}
