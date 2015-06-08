/**
 * Copyright 2014 Andrew Rice
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

import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebServlet;

import org.jboss.resteasy.plugins.server.servlet.HttpServletDispatcher;

/**
 * Isaac servlet dispatcher - replaces web.xml routes.
 *
 */
@WebServlet(urlPatterns = { "/api/*" }, initParams = {
        @WebInitParam(name = "javax.ws.rs.Application",
                value = "uk.ac.cam.cl.dtg.isaac.configuration.IsaacApplicationRegister"),
        @WebInitParam(name = "resteasy.servlet.mapping.prefix", value = "/api") })
public class IsaacHttpServletDispatcher extends HttpServletDispatcher {
    private static final long serialVersionUID = -4757864378012588474L;

}
