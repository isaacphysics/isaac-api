package uk.ac.cam.cl.dtg.isaac.dos;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import uk.ac.cam.cl.dtg.segue.api.managers.UserBadgeManager;

/**
 * Created by du220 on 13/04/2018.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface UserBadgeDefinition {

  UserBadgeManager.Badge value();

}
