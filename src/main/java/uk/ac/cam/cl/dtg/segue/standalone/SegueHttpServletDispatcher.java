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

import org.jboss.resteasy.plugins.server.servlet.HttpServletDispatcher;

/**
 * Servlet setup for segue when it is running as a standalone web application.
 * 
 * For now this is diabled as we do not need to run it as a standalone system.
 * 
 * TODO: Implement this as a separate project that can be included via maven.
 */
// @WebServlet(urlPatterns = { "/segue/api/*" },
// initParams = {
// @WebInitParam(name = "javax.ws.rs.Application", value =
// "uk.ac.cam.cl.dtg.segue.standalone.SegueApplicationRegister"),
// @WebInitParam(name = "resteasy.servlet.mapping.prefix", value="/segue/api/")
// })
public class SegueHttpServletDispatcher extends HttpServletDispatcher {
    private static final long serialVersionUID = 1L;

}
