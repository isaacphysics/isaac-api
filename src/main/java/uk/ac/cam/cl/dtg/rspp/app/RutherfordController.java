package uk.ac.cam.cl.dtg.rspp.app;

import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
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

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.dtg.rspp.models.ContentInfo;
import uk.ac.cam.cl.dtg.rspp.models.ContentPage;
import uk.ac.cam.cl.dtg.rspp.models.IndexPage;
import uk.ac.cam.cl.dtg.rspp.models.TopicPage;
import uk.ac.cam.cl.dtg.rspp.models.IndexPage.IndexPageItem;
import uk.ac.cam.cl.dtg.segue.api.SegueApiFacade;
import uk.ac.cam.cl.dtg.segue.dao.IRegistrationManager;
import uk.ac.cam.cl.dtg.segue.database.PersistenceConfigurationModule;
import uk.ac.cam.cl.dtg.segue.dto.Content;
import uk.ac.cam.cl.dtg.segue.dto.User;
import uk.ac.cam.cl.dtg.util.Mailer;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.template.soy.tofu.SoyTofuException;
import com.papercut.silken.SilkenServlet;
import com.papercut.silken.TemplateRenderer;

/**
 * Rutherford Controller
 * 
 * This class specifically caters for the Rutherford physics server and is expected to provide extended functionality to the Segue api for use only on the Rutherford site.
 * 
 */
@Path("/")
public class RutherfordController {

	private static final Logger log = LoggerFactory.getLogger(RutherfordController.class);
	
	private static final SegueApiFacade api = new SegueApiFacade();

	// Map of contentID to detail
	private Map<String, ContentDetail> contentDetails = ContentDetail.load();
	
	// Map of topicPath to detail
	private Map<String, TopicDetail> topicDetails = TopicDetail.load();
	
	private boolean questionNavigationDataLoaded = false;
	
	// TODO: Move to a configuration file
	private static final String MailerSmtpServer = "ppsw.cam.ac.uk";
	private static final String MailerFromAddress = "cl-rutherford@lists.cam.ac.uk";
	private static final String[] recipients = {"dst28@cam.ac.uk"};
	
	// I apologise for this function.
	private void loadQuestionNavigationData()
	{
		SortedSet<ContentDetail> content = new TreeSet<ContentDetail>(contentDetails.values());

		for(TopicDetail t : topicDetails.values())
		{
			for (int level = 1; level <= 6; level++)
			{
				ContentDetail prevQ = null;
				for (ContentDetail q : content) 
				{
					if (q.type.equals(ContentDetail.TYPE_QUESTION) && t.topic.equals(q.topic) && ((Integer)level).toString().equals(q.level)) 
					{
						if (prevQ != null)
						{
							q.prevContentId = prevQ.id;
							prevQ.nextContentId = q.id;
						}
						prevQ = q;
					}
				}
			}
		}
		questionNavigationDataLoaded = true;
	}

	@GET
	@Path("learn")
	@Produces("application/json")
	public IndexPage getTopics() {
		ImmutableList.Builder<IndexPageItem> builder = ImmutableList.builder();
		SortedSet<TopicDetail> values = new TreeSet<TopicDetail>(
				topicDetails.values());
		for (TopicDetail t : values) {
			for (Map.Entry<String, String> e : t.pdf.entrySet()) {
				// see whether there are any questions for this
				boolean found = false;
				for(ContentDetail d : contentDetails.values()) {
					if (d.type.equals(ContentDetail.TYPE_QUESTION)  // This is a question
							&& d.topic.equals(t.topic)         // and the question belongs to this topic
							&& d.level.equals(e.getKey()))     // and the level is correct
					{
						found = true;
						break;
					}
				}
				builder.add(new IndexPageItem(t.title, e.getKey(), t.topic,
						e.getValue(),found));
			}
		}
		return new IndexPage(builder.build());
	}

	/**
	 * Return the list of concepts and questions available for this topic at
	 * this level
	 * 
	 * @param topic
	 * @param level
	 * @return
	 */
	@GET
	@Path("topics/{topic:.*}/level-{level}")
	@Produces("application/json")
	public TopicPage getTopicWithLevel(@PathParam("topic") String topic, @PathParam("level") String level) {

		TopicDetail topicDetail = topicDetails.get(topic);

		ImmutableList.Builder<String> conceptIdBuilder = ImmutableList.builder();
		ImmutableList.Builder<String> questionIdBuilder = ImmutableList.builder();
		
		HashSet<String> linkedConceptIds = new HashSet<String>();

		SortedSet<ContentDetail> values = new TreeSet<ContentDetail>(contentDetails.values());

		// Find all the questions for this topic.
		for (ContentDetail detail : values) 
		{
			if (detail.type.equals(ContentDetail.TYPE_QUESTION) && topic.equals(detail.topic) && level.equals(detail.level)) 
			{
				questionIdBuilder.add(detail.id);
				
				for (String cid : detail.relatedConceptIds)
				{
					if (contentDetails.containsKey(cid))
						linkedConceptIds.add(cid);
				}
			}
		}
		
		conceptIdBuilder.addAll(linkedConceptIds);
		
		ImmutableList<String> questionIds = questionIdBuilder.build();
		
		ImmutableList<String> conceptIds = conceptIdBuilder.build();

		ImmutableMap<String, ContentInfo> environment = collectEnvironment();

		return new TopicPage(topicDetail.topic, topicDetail.title, level, conceptIds, questionIds, environment, topicDetail.pdf.get(level));
	}

	@GET
	@Path("topics/{topic}")
	@Produces("application/json")
	public TopicPage getTopic(@PathParam("topic") String topic) {
		return getTopicWithLevel(topic, "1");
	}
	
	@GET
	@Path("concepts/{concept}")
	@Produces("application/json")
	public ContentPage getConcept(@Context HttpServletRequest req,
			@PathParam("concept") String concept) {
		
		Content c = (Content) api.getContentById(concept).getEntity();
		ContentPage cp = new ContentPage(c.getId(),c,collectEnvironment(), null, null, null);	
		
		return cp;
	}

	@GET
	@Path("questions/{question}")
	@Produces("application/json")
	public ContentPage getQuestion(@Context HttpServletRequest req,
			@PathParam("question") String question) {
		
		Content c = (Content) api.getContentById(question).getEntity();
		
		ContentPage cp = new ContentPage(c.getId(),c,collectEnvironment(), null, null, null);		
		return cp;
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
		Mailer contactUsMailer = new Mailer(RutherfordController.MailerSmtpServer,RutherfordController.MailerFromAddress);
		
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
	
	public static ImmutableMap<String, String> getSoyGlobalMap(HttpServletRequest req) {
		String proxyPath;
		String trackingId;
		if (req.getLocalAddr().equals("128.232.20.43")) {
			proxyPath = "/research/dtg/rutherford-staging";
			trackingId = "UA-45629473-1";
		} else if (req.getLocalAddr().equals("128.232.20.40")) {
			proxyPath = "/research/dtg/rutherford";
			trackingId = "UA-45629473-2";
		} else {
			proxyPath = req.getContextPath();
			trackingId = "";
		}
		return ImmutableMap.of("contextPath", req.getContextPath(),
				"proxyPath", proxyPath,
				"analyticsTrackingId", trackingId,
				"newSessionId", UUID.randomUUID().toString(),
				"newUserId", UUID.randomUUID().toString());
	}
	
	private String renderTemplate(String templateName, ImmutableMap<String, String> globalMap) {
		TemplateRenderer renderer = SilkenServlet.getTemplateRenderer();

		String cContent = "";
		try {
			cContent = renderer.render(templateName, null, globalMap, Locale.ENGLISH);
		} catch (SoyTofuException e) {
			cContent = "<i>No content available.</i>";
			log.error("Error applying soy template", e);
		}
		return cContent;
	}

	private ImmutableMap<String, ContentInfo> collectEnvironment() {
		// for the moment just return everything in the environment - when we
		// get to lots of things change this to only give the relevant bits
		ImmutableMap<String, ContentInfo> environment = ImmutableMap
				.copyOf(Maps.transformValues(contentDetails,
						new Function<ContentDetail, ContentInfo>() {
							@Override
							public ContentInfo apply(ContentDetail input) {
								return input.toContentInfo();
							}
						}));
		return environment;
	}
}
