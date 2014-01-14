package uk.ac.cam.cl.dtg.segue.api;

import java.io.IOException;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.dtg.rspp.app.ContentDetail;
import uk.ac.cam.cl.dtg.rspp.app.TopicDetail;
import uk.ac.cam.cl.dtg.rspp.models.ContentInfo;
import uk.ac.cam.cl.dtg.segue.dao.ContentMapper;
import uk.ac.cam.cl.dtg.segue.dao.IContentManager;
import uk.ac.cam.cl.dtg.segue.dao.ILogManager;
import uk.ac.cam.cl.dtg.segue.dao.IRegistrationManager;
import uk.ac.cam.cl.dtg.segue.database.PersistenceConfigurationModule;
import uk.ac.cam.cl.dtg.segue.dto.Content;
import uk.ac.cam.cl.dtg.segue.dto.User;
import uk.ac.cam.cl.dtg.segue.models.DataView;
import uk.ac.cam.cl.dtg.util.Mailer;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.inject.Guice;
import com.google.inject.Injector;


@Path("/")
public class SegueApiFacade {
	private static final Logger log = LoggerFactory.getLogger(SegueApiFacade.class);
	
	// TODO: Move to a configuration file
	private static final String MailerSmtpServer = "ppsw.cam.ac.uk";
	private static final String MailerFromAddress = "cl-rutherford@lists.cam.ac.uk";
	private static final String[] recipients = {"dst28@cam.ac.uk"};

	@POST
	@Path("log")
	@Produces("application/json")
	public ImmutableMap<String, Boolean> postLog(
			@Context HttpServletRequest req,
			@FormParam("sessionId") String sessionId,
			@FormParam("cookieId") String cookieId,
			@FormParam("event") String eventJSON) {
		
		Injector injector = Guice.createInjector(new PersistenceConfigurationModule());
		ILogManager logPersistenceManager = injector.getInstance(ILogManager.class);

		boolean success = logPersistenceManager.log(sessionId, cookieId, eventJSON);

		return ImmutableMap.of("success", success);
	}
	
	@POST
	@Consumes({"application/x-www-form-urlencoded"})
	@Path("contact-us/register-interest")
	public ImmutableMap<String, String> postRegisterInterest(
			@FormParam("name") String name,
			@FormParam("email") String email,
			@FormParam("role") String role,
			@FormParam("school") String school,
			@FormParam("year") String year,
			@FormParam("feedback") String feedbackAgreement) {
		
		boolean feedback = false;
		
		if(null != feedbackAgreement){
			feedback = true;
		}

		
		StringBuilder sb = new StringBuilder();
		sb.append("Name: " + name);
		sb.append(" email: " + email);
		sb.append(" role: " + role);
		sb.append(" school: " + school);
		sb.append(" year: " + year);
		sb.append(" feedback: " + new Boolean(feedback).toString());
		
		log.info("Register Interest details: " + sb.toString());
		
		User newUser = new User(null, name, email, role, school, year, feedback, new Date());
		
		Injector injector = Guice.createInjector(new PersistenceConfigurationModule());
		IRegistrationManager registrationManager = injector.getInstance(IRegistrationManager.class);

		boolean success = registrationManager.register(newUser);
		
		String outcome = "success";
		if(!success){
			outcome = "Registration failed: Error registering user.";
			log.error("Error in registering interest for user " + name + " " + email);
		}
		
		return ImmutableMap.of("result", outcome);
	}
	
	@POST
	@Consumes({"application/x-www-form-urlencoded"})
	@Path("contact-us/sendContactUsMessage")
	public ImmutableMap<String,String> postContactUsMessage(
			@FormParam("full-name") String fullName,
			@FormParam("email") String email,
			@FormParam("subject") String subject,
			@FormParam("message-text") String messageText,
			@Context HttpServletRequest request){

		// construct a new instance of the mailer object
		Mailer contactUsMailer = new Mailer(SegueApiFacade.MailerSmtpServer,SegueApiFacade.MailerFromAddress);
		
		if (StringUtils.isBlank(fullName) && StringUtils.isBlank(email) && StringUtils.isBlank(subject) && StringUtils.isBlank(messageText)){
			log.debug("Contact us required field validation error ");
			return ImmutableMap.of("result", "message not sent - Missing required field - Validation Error");			
		}
		
		// Get IpAddress of client
		String ipAddress = request.getHeader("X-FORWARDED-FOR");
		
		if (ipAddress == null) {
			ipAddress = request.getRemoteAddr();
		}

		// Construct message
		StringBuilder message = new StringBuilder();
		message.append("- Sender Details - " + "\n");
		message.append("From: " + fullName + "\n");
		message.append("E-mail: " + email + "\n");
		message.append("IP address: " + ipAddress + "\n");
		message.append("Message Subject: " + subject + "\n");
		message.append("- Message - " + "\n");
		message.append(messageText);
		
		try {
			// attempt to send the message via the smtp server
			contactUsMailer.sendMail(recipients, email, subject, message.toString());
			log.info("Contact Us - E-mail sent to " + recipients + " " + email + " " + subject + " " + message.toString());
			
		} catch (AddressException e) {				
			log.warn("E-mail Address validation error " + e.toString());
			return ImmutableMap.of(
					"result", "message not sent - E-mail address malformed - Validation Error \n " + e.toString());		
			
		} catch (MessagingException e) {
			log.error("Messaging error " + e.toString());
			return ImmutableMap.of(
					"result", "message not sent - Unknown Messaging error\n " + e.toString());	
		}
		
		return ImmutableMap.of("result", "success");
	}
	

	@POST
	@Produces("application/json")
	@Path("content/save")
	public Response contentSave(@FormParam("doc") String docJson) {
		Injector injector = Guice.createInjector(new PersistenceConfigurationModule());
		IContentManager contentPersistenceManager = injector.getInstance(IContentManager.class);

		ContentMapper mapper = injector.getInstance(ContentMapper.class);
		
		log.info("INSERTING DOC: " + docJson);
		
		String newId = null;
		try {			
			Content cnt = mapper.load(docJson);
			
			newId = contentPersistenceManager.save(cnt);			
		} catch (JsonParseException e) {
			e.printStackTrace();
			return Response.serverError().entity(ImmutableMap.of("error", e.toString())).build();
		} catch (JsonMappingException e) {
			e.printStackTrace();
			return Response.serverError().entity(ImmutableMap.of("error", e.toString())).build();
		} catch (IOException e) {
			e.printStackTrace();
			return Response.serverError().entity(ImmutableMap.of("error", e.toString())).build();
		}
		
		if (newId != null)
			return Response.ok().entity(ImmutableMap.of("result", "success", "newId", newId)).build();
		else
			return Response.serverError().entity(ImmutableMap.of("error", "No new Id was assigned by the database")).build();
	}
	
	/**
	 * GetContentBy Id from the database
	 * 
	 * Currently this method will return a single Json Object containing all of the fields available to the object retrieved from the database.
	 * 
	 * @param id
	 * @return Response object containing the serialized content object. (with no levels of recursion into the content)
	 */
	@GET
	@Produces("application/json")
	@Path("content/get/{id}")
	public Response getContentById(@PathParam("id") String id) {		
		Injector injector = Guice.createInjector(new PersistenceConfigurationModule());
		IContentManager contentPersistenceManager = injector.getInstance(IContentManager.class);
		
		Content c = null;
		
		// Deserialize object into POJO of specified type, providing one exists. 
		try{
			log.info("RETRIEVING DOC: " + id);
			c = contentPersistenceManager.getById(id);
		}
		catch(IllegalArgumentException e){
			log.error("Unable to map content object.", e);
			return Response.serverError().entity(e).build();
		}
		
		return Response.ok().entity(c).build();
	}
}
