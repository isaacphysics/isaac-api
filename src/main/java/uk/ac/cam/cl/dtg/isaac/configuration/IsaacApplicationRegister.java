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

import io.swagger.jaxrs.config.BeanConfig;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.dtg.isaac.api.AssignmentFacade;
import uk.ac.cam.cl.dtg.isaac.api.EventsFacade;
import uk.ac.cam.cl.dtg.isaac.api.GameboardsFacade;
import uk.ac.cam.cl.dtg.isaac.api.IsaacController;
import uk.ac.cam.cl.dtg.isaac.api.PagesFacade;
import uk.ac.cam.cl.dtg.segue.api.*;
import uk.ac.cam.cl.dtg.segue.api.monitors.IMetricsExporter;
import uk.ac.cam.cl.dtg.segue.api.monitors.PerformanceMonitor;
import uk.ac.cam.cl.dtg.segue.configuration.SegueGuiceConfigurationModule;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentMapper;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

import com.google.inject.Guice;
import com.google.inject.Injector;

import static uk.ac.cam.cl.dtg.isaac.api.Constants.*;
import static uk.ac.cam.cl.dtg.segue.api.Constants.*;

/**
 * This class registers the resteasy handlers. The name is important since it is used as a String in
 * HttpServletDispatcherV3
 * 
 * @author acr31
 * 
 */
public class IsaacApplicationRegister extends Application {
    private static final Logger log = LoggerFactory.getLogger(IsaacApplicationRegister.class);
    private Set<Object> singletons;
    
    public static Injector injector;
    
    /**
     * Default constructor.
     */
    public IsaacApplicationRegister() {
        singletons = new HashSet<Object>();
        SegueGuiceConfigurationModule segueGuiceConfigurationModule = new SegueGuiceConfigurationModule();
        IsaacGuiceConfigurationModule isaacGuiceConfigurationModule = new IsaacGuiceConfigurationModule();
        
        injector = Guice.createInjector(isaacGuiceConfigurationModule, segueGuiceConfigurationModule);
        
        SegueConfigurationModule segueConfigurationModule = injector.getInstance(SegueConfigurationModule.class);
        ContentMapper mapper = injector.getInstance(ContentMapper.class);
        if (segueConfigurationModule != null) {
            // register the isaac specific data types.
            log.info("Registering isaac specific datatypes with the segue content mapper.");
            mapper.registerJsonTypes(segueConfigurationModule.getContentDataTransferObjectMap());
        }
        
        setupMetricsExporter();
        setupSwaggerApiAdvertiser();
    }

    @Override
    public final Set<Object> getSingletons() {
        // check to see if we have already registered singletons as we don't want this happening more than once.
        if (singletons.isEmpty()) {
            // Registers segue singleton endpoints as /isaac/segue/api
            // invoke optional service initialisation
            this.singletons.add(injector.getInstance(SchoolLookupServiceFacade.class));

            // initialise segue framework.
            this.singletons.add(injector.getInstance(SegueContentFacade.class));
            this.singletons.add(injector.getInstance(InfoFacade.class));
            this.singletons.add(injector.getInstance(ContactFacade.class));
            this.singletons.add(injector.getInstance(QuestionFacade.class));
            this.singletons.add(injector.getInstance(LogEventFacade.class));
            this.singletons.add(injector.getInstance(SegueDefaultFacade.class));
            this.singletons.add(injector.getInstance(UsersFacade.class));
            this.singletons.add(injector.getInstance(AuthenticationFacade.class));
            this.singletons.add(injector.getInstance(AdminFacade.class));
            this.singletons.add(injector.getInstance(AuthorisationFacade.class));
            this.singletons.add(injector.getInstance(AssignmentFacade.class));
            this.singletons.add(injector.getInstance(GroupsFacade.class));
            
            // initialise isaac specific facades
            this.singletons.add(injector.getInstance(GameboardsFacade.class));
            this.singletons.add(injector.getInstance(IsaacController.class));
            this.singletons.add(injector.getInstance(PagesFacade.class));
            this.singletons.add(injector.getInstance(EventsFacade.class));
            this.singletons.add(injector.getInstance(NotificationFacade.class));
            this.singletons.add(injector.getInstance(EmailFacade.class));
        }

        return this.singletons;
    }

    @Override
    public final Set<Class<?>> getClasses() {
        Set<Class<?>> result = new HashSet<Class<?>>();
        
        result.add(RestEasyJacksonConfiguration.class);
        result.add(PerformanceMonitor.class);
        
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
            beanConfig.setHost(propertiesLoader.getProperty(HOST_NAME).substring(0,
                    propertiesLoader.getProperty(HOST_NAME).indexOf('/')));
        } else {
            beanConfig.setBasePath("/api");    
            beanConfig.setHost(propertiesLoader.getProperty(HOST_NAME));
        }
        
        beanConfig.setResourcePackage("uk.ac.cam.cl.dtg");
        beanConfig.setScan(true);        
    }

    /**
     * Set-up metrics exporter. Only needs to be created once per process.
     */
    private void setupMetricsExporter() {
        injector.getInstance(IMetricsExporter.class);
    }
}
