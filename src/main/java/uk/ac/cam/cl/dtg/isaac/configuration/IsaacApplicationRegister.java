package uk.ac.cam.cl.dtg.isaac.configuration;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

import com.google.inject.Guice;
import com.google.inject.Injector;

import uk.ac.cam.cl.dtg.isaac.api.APIOverviewResource;
import uk.ac.cam.cl.dtg.isaac.api.IsaacController;
import uk.ac.cam.cl.dtg.segue.api.SchoolLookupFacade;
import uk.ac.cam.cl.dtg.segue.api.SegueApiFacade;
import uk.ac.cam.cl.dtg.segue.configuration.SegueGuiceConfigurationModule;

/**
 * This class registers the resteasy handlers. The name is important since it is
 * used as a String in HttpServletDispatcherV3
 * 
 * @author acr31
 * 
 */
public class IsaacApplicationRegister extends Application {
	private Set<Object> singletons;
	
	/**
	 * Default constructor.
	 */
	public IsaacApplicationRegister() {
		singletons = new HashSet<Object>();
	}

	@Override
	public final Set<Object> getSingletons() {
		// Registers segue singleton endpoints as /isaac/segue/api
		Injector injector = Guice.createInjector(
				new IsaacGuiceConfigurationModule(),
				new SegueGuiceConfigurationModule());
		this.singletons.add(injector.getInstance(SegueApiFacade.class));
		return this.singletons;
	}

	@Override
	public final Set<Class<?>> getClasses() {
		Set<Class<?>> result = new HashSet<Class<?>>();
		result.add(IsaacController.class);
		result.add(APIOverviewResource.class);
		result.add(RestEasyJacksonConfiguration.class);
		result.add(SchoolLookupFacade.class);
		return result;
	}
}
