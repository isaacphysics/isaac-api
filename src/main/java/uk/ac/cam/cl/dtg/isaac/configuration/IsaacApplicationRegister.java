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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Injector;
import io.swagger.v3.jaxrs2.integration.JaxrsOpenApiContextBuilder;
import io.swagger.v3.jaxrs2.integration.resources.AcceptHeaderOpenApiResource;
import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource;
import io.swagger.v3.oas.integration.OpenApiConfigurationException;
import io.swagger.v3.oas.integration.SwaggerConfiguration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import jakarta.servlet.ServletConfig;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Context;
import uk.ac.cam.cl.dtg.isaac.api.AssignmentFacade;
import uk.ac.cam.cl.dtg.isaac.api.EventsFacade;
import uk.ac.cam.cl.dtg.isaac.api.GameboardsFacade;
import uk.ac.cam.cl.dtg.isaac.api.IsaacController;
import uk.ac.cam.cl.dtg.isaac.api.PagesFacade;
import uk.ac.cam.cl.dtg.isaac.api.QuizFacade;
import uk.ac.cam.cl.dtg.segue.api.*;
import uk.ac.cam.cl.dtg.segue.api.managers.IGroupObserver;
import uk.ac.cam.cl.dtg.segue.api.managers.UserBadgeManager;
import uk.ac.cam.cl.dtg.segue.api.monitors.AuditMonitor;
import uk.ac.cam.cl.dtg.segue.api.monitors.PerformanceMonitor;
import uk.ac.cam.cl.dtg.segue.configuration.SegueGuiceConfigurationModule;
import uk.ac.cam.cl.dtg.segue.scheduler.SegueJobService;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

import java.util.HashSet;
import java.util.Set;

import static uk.ac.cam.cl.dtg.segue.api.Constants.*;

/**
 * This class registers the resteasy handlers. The name is important since it is used as a String in
 * HttpServletDispatcherV3
 * 
 * @author acr31
 * 
 */
public class IsaacApplicationRegister extends Application {
    private final Set<Object> singletons;
    
    private static Injector injector;
    
    /**
     * Default constructor.
     */
    public IsaacApplicationRegister(@Context ServletConfig servletConfig) {
        singletons = new HashSet<>();
        injector = SegueGuiceConfigurationModule.getGuiceInjector();
        
        setupSwaggerApiAdvertiser(servletConfig);

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
            this.singletons.add(injector.getInstance(SessionValidator.class));
            this.singletons.add(injector.getInstance(ExceptionSanitiser.class));

            // initialise observers
            this.singletons.add(injector.getInstance(IGroupObserver.class));
        }

        return this.singletons;
    }

    @Override
    public final Set<Class<?>> getClasses() {
        Set<Class<?>> result = new HashSet<>();
        
        result.add(RestEasyJacksonConfiguration.class);
        result.add(OpenApiResource.class);
        result.add(AcceptHeaderOpenApiResource.class);

        return result;
    }
    
    /**
     * Configure and setup Swagger (advertises api endpoints via app_root/swagger.json).
     */
    private void setupSwaggerApiAdvertiser(final ServletConfig servletConfig) {
        PropertiesLoader propertiesLoader = injector.getInstance(PropertiesLoader.class);
        String hostName = propertiesLoader.getProperty(HOST_NAME);
        String httpScheme;
        if (hostName.contains("localhost")) {
            httpScheme = "http://";
        } else {
            httpScheme = "https://";
        }
        String serverUrl = httpScheme + hostName;

        Info apiInfo = new Info()
                .title("Isaac API")
                .version(SegueGuiceConfigurationModule.getSegueVersion())
                .description("API for the Isaac platform. Automated use may violate our Terms of Service.")
                .contact(new Contact()
                        .name(propertiesLoader.getProperty(MAIL_NAME))
                        .url(String.format("%s/contact", serverUrl))
                        .email(propertiesLoader.getProperty(SERVER_ADMIN_ADDRESS)))
                .termsOfService(String.format("%s/terms", serverUrl));
        OpenAPI openApi = new OpenAPI()
                .info(apiInfo)
                .servers(ImmutableList.of(new Server().description("Isaac API").url("./")));
        SwaggerConfiguration swaggerConfig = new SwaggerConfiguration()
                .openAPI(openApi)
                .sortOutput(true)
                .resourcePackages(ImmutableSet.of("uk.ac.cam.cl.dtg"))
                .prettyPrint(true);

        try {
            new JaxrsOpenApiContextBuilder<>()
                    .servletConfig(servletConfig)
                    .application(this)
                    .openApiConfiguration(swaggerConfig)
                    .buildContext(true);
        } catch (OpenApiConfigurationException e) {
            throw new RuntimeException(e);
        }
    }
}
