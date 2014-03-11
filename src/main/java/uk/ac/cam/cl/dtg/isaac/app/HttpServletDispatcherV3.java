package uk.ac.cam.cl.dtg.isaac.app;

import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebServlet;

import org.jboss.resteasy.plugins.server.servlet.HttpServletDispatcher;

@WebServlet(urlPatterns = { "/api/*" }, 
initParams = { 
		@WebInitParam(name = "javax.ws.rs.Application", value = "uk.ac.cam.cl.dtg.isaac.app.ApplicationRegister"),
		@WebInitParam(name = "resteasy.servlet.mapping.prefix", value="/api/")
})
public class HttpServletDispatcherV3 extends HttpServletDispatcher {
	private static final long serialVersionUID = 1L;

}
