package uk.ac.cam.cl.dtg.segue.dto.content;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface JsonType {
	String value() default "string";
}
