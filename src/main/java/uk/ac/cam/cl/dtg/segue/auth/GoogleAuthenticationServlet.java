package uk.ac.cam.cl.dtg.segue.auth;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.dtg.segue.dto.User;

/**
 * This class is to allow testing of the google oauth code.
 * 
 * It is probably going to be removed soon.
 * 
 * @author sac92
 * @deprecated
 */
@Path("/old/")
public class GoogleAuthenticationServlet {

	private static final Logger log = LoggerFactory.getLogger(GoogleAuthenticationServlet.class);
	final GoogleAuthenticator helper = new GoogleAuthenticator();
	
	@GET
	@Path("auth/google/authenticate")
	public void googleTest(@Context HttpServletRequest request, @Context HttpServletResponse response) throws IOException{
		String url = helper.getAuthorizationUrl();
		
		request.getSession().setAttribute("state", helper.getAntiForgeryStateToken());
		
		log.info("Redirecting for client authentication: " + url);
		response.sendRedirect(url);
	}

	@GET
	@Path("auth/google/callback")
	public Response authCallback(@Context HttpServletRequest request, @Context HttpServletResponse response) throws Exception{		  

		StringBuffer fullUrlBuf = request.getRequestURL();

		// Ensure that there is no request forgery going on, and that the user
		// sending us this connect request is the user that was supposed to.
		if (!request.getParameter("state").equals(request.getSession().getAttribute("state"))) {
			log.error("Invalid state parameter - Google said: " + request.getParameter("state") + " Session said: " + request.getSession().getAttribute("state"));
			return Response.status(401).build();
		}
		else
		{
			log.error("State parameter matches - Google said: " + request.getParameter("state") + " Session said: " + request.getSession().getAttribute("state"));
		}

		if (request.getQueryString() != null) {
			fullUrlBuf.append('?').append(request.getQueryString());
		}

		log.info("requestURL is: " + fullUrlBuf);

		String authCode = helper.extractAuthCode(fullUrlBuf.toString());

		if (authCode == null) {
			log.info("User denied access to our app.");
		} else {	      
			log.info("User granted access to our app : oauth access code is: " + authCode );

			request.getSession().setAttribute("code", authCode);
			String internalReference = helper.exchangeCode(authCode);
			log.info(request.getSession().getId());
			
			//get user info
			User u = helper.getUserInfo(internalReference);
			log.info("User with name " + u.getEmail() + " retrieved");
		}
		return Response.ok().build();
	}
}
