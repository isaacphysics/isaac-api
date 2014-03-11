package uk.ac.cam.cl.dtg.segue.api;

import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebServlet;

import org.jboss.resteasy.plugins.server.servlet.HttpServletDispatcher;

@WebServlet(urlPatterns = { "/segue/api/*" }, 
initParams = { 
		@WebInitParam(name = "javax.ws.rs.Application", value = "uk.ac.cam.cl.dtg.segue.api.SegueApplicationRegister"),
		@WebInitParam(name = "resteasy.servlet.mapping.prefix", value="/segue/api/")
})
public class SegueHttpServletDispatcher extends HttpServletDispatcher {
	private static final long serialVersionUID = 1L;

}
