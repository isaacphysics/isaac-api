package uk.ac.cam.cl.dtg.segue.dao.userBadges.questionBadges;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import uk.ac.cam.cl.dtg.segue.dao.userBadges.IUserBadgePolicy;
import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;

import java.util.Arrays;
import java.util.List;

/**
 * Created by du220 on 30/04/2018.
 */
public abstract class AbstractQuestionsBadgePolicy implements IUserBadgePolicy {

    protected final List<Integer> thresholds = Arrays.asList(1, 5, 10, 15, 25, 50, 75, 100, 150, 200);

    @Override
    public int getLevel(Object state) {

        Integer count = ((ArrayNode) ((ObjectNode) state).get("completeAttempts")).size();

        if (count != 0) {
            for (Integer i = 0; i < thresholds.size(); i++) {
                if (count < thresholds.get(i)) {
                    return thresholds.get(i - 1);
                }
            }

            // if not returned from the loop, return the highest threshold
            return thresholds.get(thresholds.size() - 1);
        } else {
            return 0;
        }
    }
}
