package uk.ac.cam.cl.dtg.teaching;

import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

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
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.mongojack.JacksonDBCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.dtg.clojure.Clojure;
import uk.ac.cam.cl.dtg.clojure.DatomicLogger;
import uk.ac.cam.cl.dtg.clojure.InterestRegistration;
import uk.ac.cam.cl.dtg.teaching.models.Content;
import uk.ac.cam.cl.dtg.teaching.models.ContentInfo;
import uk.ac.cam.cl.dtg.teaching.models.ContentPage;
import uk.ac.cam.cl.dtg.teaching.models.IndexPage;
import uk.ac.cam.cl.dtg.teaching.models.IndexPage.IndexPageItem;
import uk.ac.cam.cl.dtg.teaching.models.TopicPage;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.template.soy.tofu.SoyTofuException;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.MongoClient;
import com.papercut.silken.SilkenServlet;
import com.papercut.silken.TemplateRenderer;


@Path("/")
public class RutherfordController {

	private static final Logger log = LoggerFactory.getLogger(RutherfordController.class);
	private static final DatomicLogger datomicLogger = Clojure.generate(DatomicLogger.class);

	// Map of contentID to detail
	private Map<String, ContentDetail> contentDetails = ContentDetail.load();
	
	// Map of topicPath to detail
	private Map<String, TopicDetail> topicDetails = TopicDetail.load();
	
	private boolean questionNavigationDataLoaded = false;
	
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
	@Path("questions/{question}")
	@Produces("application/json")
	public ContentPage getQuestion(@Context HttpServletRequest req,
			@PathParam("question") String question) {

		if (!questionNavigationDataLoaded)
			loadQuestionNavigationData();
		
		String renderedContent = renderTemplate("rutherford.content."
				+ question, getSoyGlobalMap(req));
		
		return new ContentPage(question, renderedContent, collectEnvironment(), 
				contentDetails.get(question).prevContentId != null ? "/questions/" + contentDetails.get(question).prevContentId : null, 
				"/topics/" + contentDetails.get(question).topic + "/level-" + contentDetails.get(question).level, 
				contentDetails.get(question).nextContentId != null ? "/questions/" + contentDetails.get(question).nextContentId : null);
	}


	@GET
	@Path("concepts/{concept}")
	@Produces("application/json")
	public ContentPage getConcept(@Context HttpServletRequest req,
			@PathParam("concept") String concept) {
		String renderedContent = renderTemplate(
				"rutherford.content." + concept, getSoyGlobalMap(req));
		return new ContentPage(concept, renderedContent, collectEnvironment(), null, null, null);
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

	@POST
	@Path("log")
	@Produces("application/json")
	public ImmutableMap<String, Boolean> postLog(
			@Context HttpServletRequest req,
			@FormParam("sessionId") String sessionId,
			@FormParam("cookieId") String cookieId,
			@FormParam("event") String eventJson) {
		
		try {
			MongoClient mongo = new MongoClient( "localhost" , 27017 );
			
			DB db = mongo.getDB( "rutherford" );
			
			DBCollection coll = db.getCollection("content");
			
			JacksonDBCollection<Content,String> jc = JacksonDBCollection.wrap(coll, Content.class, String.class);
			
			Content ct = new Content(null, "my_obj_id", "The Title", "concept", "Ian", "text", null, "1-col", null, "Hello", "Comes from somewhere", null, 1);
			
			jc.insert(ct);
			
			
			long c = jc.count();
			
			Content r = jc.findOne();
			
			r.src = "Wooooo";
			jc.save(r);
			
			int a = 1;
			a = 7;
			int b = a;
			
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		
		boolean success = datomicLogger.logEvent(sessionId, cookieId, eventJson);

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
		
		InterestRegistration reg = Clojure.generate(InterestRegistration.class);
		
		boolean success = reg.register(name, email, role, school, year, feedback);
		
		String outcome = "success";
		if(!success){
			outcome = "Registration failed: Error registering user.";
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
	

}
