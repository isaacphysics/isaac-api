package uk.ac.cam.cl.dtg.isaac.configuration;

import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebServlet;

import org.jboss.resteasy.plugins.server.servlet.HttpServletDispatcher;

@WebServlet(urlPatterns = { "/api/*" }, initParams = {
		@WebInitParam(name = "javax.ws.rs.Application", value = "uk.ac.cam.cl.dtg.isaac.configuration.IsaacApplicationRegister"),
		@WebInitParam(name = "resteasy.servlet.mapping.prefix", value = "/api") })
public class IsaacHttpServletDispatcher extends HttpServletDispatcher {
	private static final long serialVersionUID = 1L;

}
