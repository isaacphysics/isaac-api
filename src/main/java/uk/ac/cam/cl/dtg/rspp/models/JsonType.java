package uk.ac.cam.cl.dtg.rspp.models;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface JsonType {
	String value() default "string";
}
