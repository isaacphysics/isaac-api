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
package uk.ac.cam.cl.dtg.segue.standalone;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

import com.google.inject.Guice;
import com.google.inject.Injector;

import uk.ac.cam.cl.dtg.isaac.api.IsaacController;
import uk.ac.cam.cl.dtg.isaac.configuration.IsaacGuiceConfigurationModule;
import uk.ac.cam.cl.dtg.segue.api.SegueDefaultFacade;
import uk.ac.cam.cl.dtg.segue.configuration.SegueGuiceConfigurationModule;

/**
 * This class registers the resteasy handlers. The name is important since it is used as a String in
 * HttpServletDispatcherV3
 * 
 * Note: this class is not used unless Segue is running in stand alone mode and registering its own rest endpoints.
 */
public class SegueApplicationRegister extends Application {

    private Set<Object> singletons;

    /**
     * Application register for RestEasy Segue.
     */
    public SegueApplicationRegister() {
        singletons = new HashSet<Object>();
    }

    @Override
    public Set<Object> getSingletons() {
        // Registers segue singleton endpoints as /isaac/segue/api
        Injector injector = Guice.createInjector(new IsaacGuiceConfigurationModule(),
                new SegueGuiceConfigurationModule());
        this.singletons.add(injector.getInstance(SegueDefaultFacade.class));
        return this.singletons;
    }

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> result = new HashSet<Class<?>>();
        result.add(IsaacController.class);
        return result;
    }
}
