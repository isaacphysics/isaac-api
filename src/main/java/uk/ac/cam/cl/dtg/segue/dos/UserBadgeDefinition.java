package uk.ac.cam.cl.dtg.segue.dos;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by du220 on 13/04/2018.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface UserBadgeDefinition {

    UserBadgeFields.Badge value();

}
