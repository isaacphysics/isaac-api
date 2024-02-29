package uk.ac.cam.cl.dtg.isaac.mappers;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is used to resolve the rare situation of constructor ambiguity.
 */
@Target(ElementType.CONSTRUCTOR)
@Retention(RetentionPolicy.CLASS)
public @interface Default {

}