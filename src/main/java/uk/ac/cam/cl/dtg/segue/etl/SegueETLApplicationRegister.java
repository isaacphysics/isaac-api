/**
 * Copyright 2014 Stephen Cummins
 * <br>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * <br>
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * <br>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.ac.cam.cl.dtg.segue.etl;

import com.google.inject.Injector;
import io.swagger.v3.jaxrs2.integration.resources.AcceptHeaderOpenApiResource;
import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource;
import jakarta.ws.rs.core.Application;
import java.util.HashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class registers the resteasy handlers. The name is important since it is used as a String in
 * HttpServletDispatcherV3
 *
 * @author acr31
 */
public class SegueETLApplicationRegister extends Application {
  private static final Logger log = LoggerFactory.getLogger(SegueETLApplicationRegister.class);
  private Set<Object> singletons;

  private final Injector injector;

  /**
   * Default constructor.
   */
  public SegueETLApplicationRegister() {
    singletons = new HashSet<>();
    injector = ETLConfigurationModule.getGuiceInjector();

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

    result.add(OpenApiResource.class);
    result.add(AcceptHeaderOpenApiResource.class);

    return result;
  }

  /**
   * Configure and setup Swagger (advertises api endpoints via app_root/swagger.json).
   */
  private void setupSwaggerApiAdvertiser() {
    // TODO: is this worth it?
  }

}
