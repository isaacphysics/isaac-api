/*
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

import com.google.inject.Guice;
import com.google.inject.Injector;
import io.swagger.jaxrs.config.BeanConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.api.AssignmentFacade;
import uk.ac.cam.cl.dtg.isaac.api.EventsFacade;
import uk.ac.cam.cl.dtg.isaac.api.GameboardsFacade;
import uk.ac.cam.cl.dtg.isaac.api.IsaacController;
import uk.ac.cam.cl.dtg.isaac.api.PagesFacade;
import uk.ac.cam.cl.dtg.isaac.api.QuizFacade;
import uk.ac.cam.cl.dtg.segue.api.*;
import uk.ac.cam.cl.dtg.segue.api.managers.UserBadgeManager;
import uk.ac.cam.cl.dtg.segue.api.monitors.AuditMonitor;
import uk.ac.cam.cl.dtg.segue.api.monitors.PerformanceMonitor;
import uk.ac.cam.cl.dtg.segue.configuration.SegueGuiceConfigurationModule;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentMapper;
import uk.ac.cam.cl.dtg.segue.scheduler.SegueJobService;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

import javax.ws.rs.core.Application;
import java.util.HashSet;
import java.util.Set;

import static uk.ac.cam.cl.dtg.isaac.api.Constants.PROXY_PATH;
import static uk.ac.cam.cl.dtg.segue.api.Constants.HOST_NAME;
import static uk.ac.cam.cl.dtg.segue.api.Constants.SEGUE_APP_VERSION;
import static uk.ac.cam.cl.dtg.segue.api.Constants.SERVER_ADMIN_ADDRESS;

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
        
        setupSwaggerApiAdvertiser();

        // create instance to get it up and running - it is not a rest facade though
        injector.getInstance(SegueJobService.class);
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
            this.singletons.add(injector.getInstance(GlossaryFacade.class));
            
            // initialise isaac specific facades
            this.singletons.add(injector.getInstance(GameboardsFacade.class));
            this.singletons.add(injector.getInstance(IsaacController.class));
            this.singletons.add(injector.getInstance(PagesFacade.class));
            this.singletons.add(injector.getInstance(EventsFacade.class));
            this.singletons.add(injector.getInstance(NotificationFacade.class));
            this.singletons.add(injector.getInstance(EmailFacade.class));
            this.singletons.add(injector.getInstance(UserBadgeManager.class));
            this.singletons.add(injector.getInstance(QuizFacade.class));

            // initialise filters
            this.singletons.add(injector.getInstance(PerformanceMonitor.class));
            this.singletons.add(injector.getInstance(AuditMonitor.class));
        }

        return this.singletons;
    }

    @Override
    public final Set<Class<?>> getClasses() {
        Set<Class<?>> result = new HashSet<>();
        
        result.add(RestEasyJacksonConfiguration.class);

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

        String hostName = propertiesLoader.getProperty(HOST_NAME);
        if (!proxyPath.equals("")) {
            beanConfig.setBasePath(proxyPath + "/api");
            beanConfig.setHost(hostName.substring(0, hostName.indexOf('/')));
        } else {
            beanConfig.setBasePath("/api");    
            beanConfig.setHost(hostName);
        }
        
        beanConfig.setResourcePackage("uk.ac.cam.cl.dtg");
        beanConfig.setTitle("Isaac API");
        beanConfig.setDescription("API for the Isaac platform. Automated use may violate our Terms of Service.");
        beanConfig.setTermsOfServiceUrl("https://" + hostName + "/terms");
        beanConfig.setContact(propertiesLoader.getProperty(SERVER_ADMIN_ADDRESS));
        beanConfig.setPrettyPrint(true);
        beanConfig.setScan(true);        
    }
}
