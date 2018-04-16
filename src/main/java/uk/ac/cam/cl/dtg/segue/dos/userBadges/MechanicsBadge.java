package uk.ac.cam.cl.dtg.segue.dos.userBadges;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import uk.ac.cam.cl.dtg.segue.dos.UserBadgeDefinition;
import uk.ac.cam.cl.dtg.segue.dos.UserBadgeFields;

import java.util.List;
import java.util.Map;

/**
 * Created by du220 on 13/04/2018.
 */
@UserBadgeDefinition(UserBadgeFields.Badge.MECHANICS)
public class MechanicsBadge extends UserBadgeFields {

    @Override
    public int getLevel() {
        return ((Map<String, List<Object>>)state).get("questions").size();
    }

    @Override
    public void initialiseState() {

        state = ImmutableMap.of("questions", Lists.newArrayList());
    }

    @Override
    public void updateState(Object event) {
        ((Map<String, List<Object>>)state).get("questions").add(event);
    }

}
