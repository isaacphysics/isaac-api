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
package uk.ac.cam.cl.dtg.segue.etl;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

import com.google.inject.Guice;
import com.google.inject.Injector;
import io.swagger.jaxrs.config.BeanConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

import static uk.ac.cam.cl.dtg.isaac.api.Constants.PROXY_PATH;
import static uk.ac.cam.cl.dtg.segue.api.Constants.HOST_NAME;
import static uk.ac.cam.cl.dtg.segue.api.Constants.SEGUE_APP_VERSION;

/**
 * This class registers the resteasy handlers. The name is important since it is used as a String in
 * HttpServletDispatcherV3
 *
 * @author acr31
 *
 */
public class SegueETLApplicationRegister extends Application {
    private static final Logger log = LoggerFactory.getLogger(SegueETLApplicationRegister.class);
    private Set<Object> singletons;

    private final Injector injector;

    /**
     * Default constructor.
     */
    public SegueETLApplicationRegister() {
        singletons = new HashSet<Object>();
        ETLConfigurationModule etlConfigurationModule = new ETLConfigurationModule();

        injector = Guice.createInjector(etlConfigurationModule);

        setupSwaggerApiAdvertiser();

    }

    @Override
    public final Set<Object> getSingletons() {
        // check to see if we have already registered singletons as we don't want this happening more than once.
        if (singletons.isEmpty()) {

            this.singletons.add(injector.getInstance(ETLFacade.class));

        }
        return this.singletons;
    }

    @Override
    public final Set<Class<?>> getClasses() {
        Set<Class<?>> result = new HashSet<Class<?>>();

        result.add(io.swagger.jaxrs.listing.ApiListingResource.class);
        result.add(io.swagger.jaxrs.listing.SwaggerSerializers.class);

        return result;
    }

    /**
     * Configure and setup Swagger (advertises api endpoints via app_root/swagger.json).
     */
    private void setupSwaggerApiAdvertiser() {
        PropertiesLoader propertiesLoader = injector.getInstance(PropertiesLoader.class);
        String proxyPath = propertiesLoader.getProperty(PROXY_PATH);

        BeanConfig beanConfig = new BeanConfig();
        beanConfig.setVersion(propertiesLoader.getProperty(SEGUE_APP_VERSION));

        if (!proxyPath.equals("")) {
            beanConfig.setBasePath(proxyPath + "/api");
            beanConfig.setHost("localhost:8090");//propertiesLoader.getProperty(HOST_NAME).substring(0,
                    //propertiesLoader.getProperty(HOST_NAME).indexOf('/')));
        } else {
            beanConfig.setBasePath("/api");
            beanConfig.setHost("localhost:8090"/*propertiesLoader.getProperty(HOST_NAME)*/);
        }

        beanConfig.setResourcePackage("uk.ac.cam.cl.dtg.segue.etl");
        beanConfig.setScan(true);
    }

}
