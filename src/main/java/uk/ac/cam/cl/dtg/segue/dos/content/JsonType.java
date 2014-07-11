package uk.ac.cam.cl.dtg.segue.dos.content;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface JsonType {
	String value() default "string";
}
