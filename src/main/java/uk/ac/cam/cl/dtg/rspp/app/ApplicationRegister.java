package uk.ac.cam.cl.dtg.rspp.app;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

import uk.ac.cam.cl.dtg.segue.api.SegueApiFacade;

/**
 * This class registers the resteasy handlers. The name is important since it is
 * used as a String in HttpServletDispatcherV3
 * 
 * @author acr31
 * 
 */
public class ApplicationRegister extends Application {

	@Override
	public Set<Class<?>> getClasses() {
		Set<Class<?>> result = new HashSet<Class<?>>();
		result.add(RutherfordController.class);
		result.add(SegueApiFacade.class);
		return result;
	}

}
