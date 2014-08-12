package uk.ac.cam.cl.dtg.segue.quiz;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used to bind a question type to an 
 * {@link IValidator}.
 * @author Stephen Cummins
 *
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidatesWith {
	
	/**
	 * The {@link IValidator} class that this Question relates to.
	 * @return
	 */
	Class<? extends IValidator> value();
}
