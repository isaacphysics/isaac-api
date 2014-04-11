package uk.ac.cam.cl.dtg.segue.standalone;

import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebServlet;

import org.jboss.resteasy.plugins.server.servlet.HttpServletDispatcher;

/**
 * Servlet setup for segue when it is running as a standalone web application.
 * 
 * For now this is diabled as we do not need to run it as a standalone system.
 * 
 * TODO: Implement this as a separate project that can be included via maven.
 * TODO: We need to create a custom ISegueConfigurationModule that can be injected into Segue when it is created.
 *
 */
//@WebServlet(urlPatterns = { "/segue/api/*" }, 
//initParams = { 
//		@WebInitParam(name = "javax.ws.rs.Application", value = "uk.ac.cam.cl.dtg.segue.standalone.SegueApplicationRegister"),
//		@WebInitParam(name = "resteasy.servlet.mapping.prefix", value="/segue/api/")
//})
public class SegueHttpServletDispatcher extends HttpServletDispatcher {
	private static final long serialVersionUID = 1L;

}
