package uk.ac.cam.cl.dtg.rspp.app;

import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebServlet;

import com.papercut.silken.SilkenServlet;

/**
 * A simple subclass of the SilkenServlet just so we have a chance to insert the
 * Servlet 3.0 annotations we need
 * 
 * @author acr31
 * 
 */
@WebServlet(//
name = "soy",//
urlPatterns = { "/soy/*" },//
loadOnStartup = 1,//
initParams = { @WebInitParam(name = "disableCaching", value = "true") }//
)
public class SilkenServletApi3 extends SilkenServlet {
	private static final long serialVersionUID = 1L; 
}
