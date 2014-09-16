/**
 * Copyright 2014 Stephen Cummins
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.cam.cl.dtg.isaac.configuration;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

import com.google.inject.Guice;
import com.google.inject.Injector;

import uk.ac.cam.cl.dtg.isaac.api.APIOverviewResource;
import uk.ac.cam.cl.dtg.isaac.api.IsaacController;
import uk.ac.cam.cl.dtg.segue.api.AdminFacade;
import uk.ac.cam.cl.dtg.segue.api.AuthenticationFacade;
import uk.ac.cam.cl.dtg.segue.api.MathsRenderingServiceFacade;
import uk.ac.cam.cl.dtg.segue.api.SchoolLookupServiceFacade;
import uk.ac.cam.cl.dtg.segue.api.SegueApiFacade;
import uk.ac.cam.cl.dtg.segue.api.UsersFacade;
import uk.ac.cam.cl.dtg.segue.configuration.SchoolLookupConfigurationModule;
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
		// check to see if we have already registered singletons as we don't want this happening more than once.
		if (singletons.isEmpty()) {
			// Registers segue singleton endpoints as /isaac/segue/api
			Injector injector = Guice.createInjector(
					new SchoolLookupConfigurationModule(),
					new IsaacGuiceConfigurationModule(),
					new SegueGuiceConfigurationModule());
			
			// invoke optional service initialisation
			this.singletons.add(injector.getInstance(SchoolLookupServiceFacade.class));
			this.singletons.add(injector.getInstance(MathsRenderingServiceFacade.class));			
			
			// initialise segue framework. 
			this.singletons.add(injector.getInstance(SegueApiFacade.class));
			this.singletons.add(injector.getInstance(UsersFacade.class));
			this.singletons.add(injector.getInstance(AuthenticationFacade.class));
			this.singletons.add(injector.getInstance(AdminFacade.class));
		}

		return this.singletons;
	}

	@Override
	public final Set<Class<?>> getClasses() {
		Set<Class<?>> result = new HashSet<Class<?>>();
		result.add(IsaacController.class);
		result.add(APIOverviewResource.class);
		result.add(RestEasyJacksonConfiguration.class);
		return result;
	}
}
