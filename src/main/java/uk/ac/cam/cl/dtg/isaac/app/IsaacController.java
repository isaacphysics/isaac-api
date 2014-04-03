package uk.ac.cam.cl.dtg.isaac.app;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
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
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.dtg.isaac.configuration.SegueConfigurationModule;
import uk.ac.cam.cl.dtg.isaac.models.ContentDetail;
import uk.ac.cam.cl.dtg.isaac.models.ContentInfo;
import uk.ac.cam.cl.dtg.isaac.models.ContentPage;
import uk.ac.cam.cl.dtg.isaac.models.IndexPage;
import uk.ac.cam.cl.dtg.isaac.models.TopicDetail;
import uk.ac.cam.cl.dtg.isaac.models.TopicPage;
import uk.ac.cam.cl.dtg.segue.api.SegueApiFacade;
import uk.ac.cam.cl.dtg.segue.dao.IUserDataManager;
import uk.ac.cam.cl.dtg.segue.database.SeguePersistenceConfigurationModule;
import uk.ac.cam.cl.dtg.segue.dto.Content;
import uk.ac.cam.cl.dtg.segue.dto.Figure;
import uk.ac.cam.cl.dtg.segue.dto.User;
import uk.ac.cam.cl.dtg.util.Mailer;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

import com.google.api.client.util.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
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
public class IsaacController {
	private static final Logger log = LoggerFactory.getLogger(IsaacController.class);
	
	private static SegueApiFacade api = new SegueApiFacade(new SegueConfigurationModule());

	// Map of contentID to detail
	private Map<String, ContentDetail> contentDetails = ContentDetail.load();
	
	// Map of topicPath to detail
	private Map<String, TopicDetail> topicDetails = TopicDetail.load();
	
	/**
	 * Temporary solution to show all content of different types in no particular order. (For dev test)
	 * 
	 * @param req
	 * @return
	 */
	@GET
	@Path("learn")
	@Produces("application/json")
	public Response getTopics(@Context HttpServletRequest req) {		
		
//		test of user registration
//		User user = api.getCurrentUser(req);
//		// example of requiring user to be logged in.
//		if(null == user)
//			return api.authenticationInitialisation(req, "google");
//		else
//			log.info("User Logged in: " + user.getEmail());
			
		
		// get all concepts
		List<ContentInfo> conceptsList = this.extractContentInfo((List<Content>) api.getAllContentByType("concept",0).getEntity(), getSoyGlobalMap(req).get(Constants.PROXY_PATH));
		
		// temporary solution to get all questions as well for testing purposes. 

		//TODO we need to work out a good way of allowing editors to group concepts and questions based on level in a rutherford specific sort of way.
		// This code is just to allow easy access to all api content from one page for testing
		conceptsList.addAll(this.extractContentInfo((List<Content>) api.getAllContentByType("isaacQuestionPage",0).getEntity(), getSoyGlobalMap(req).get(Constants.PROXY_PATH)));
		return Response.ok(new IndexPage(conceptsList)).build();
		
		// get all questions
	}

	/**
	 * Return the list of concepts and questions available for this topic at
	 * this level
	 * @deprecated until we come up with new approach
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
			if (detail.getType().equals(ContentDetail.TYPE_QUESTION) && topic.equals(detail.getTopic()) && level.equals(detail.getLevel())) 
			{
				questionIdBuilder.add(detail.getId());
				
				for (String cid : detail.getRelatedConceptIds())
				{
					if (contentDetails.containsKey(cid))
						linkedConceptIds.add(cid);
				}
			}
		}
		
		conceptIdBuilder.addAll(linkedConceptIds);
		
		ImmutableList<String> questionIds = questionIdBuilder.build();
		
		ImmutableList<String> conceptIds = conceptIdBuilder.build();

		return new TopicPage(topicDetail.getTopic(), topicDetail.getTitle(), level, conceptIds, questionIds, null, topicDetail.getPdf().get(level));
	}

	/**
	 * For now we can circumvent this by just showing all content in the learn page.
	 * 
	 * @deprecated until we come up with new approach
	 * @param topic
	 * @return
	 */
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
		
		ContentPage cp = new ContentPage(c.getId(), c ,this.buildMetaContentmap(getSoyGlobalMap(req).get("proxyPath"), c));			
		return cp;
	}

	@GET
	@Path("questions/{question}")
	@Produces("application/json")
	public ContentPage getQuestion(@Context HttpServletRequest req,
			@PathParam("question") String question) {
		Content c = (Content) api.getContentById(question).getEntity();
		
		ContentPage cp = new ContentPage(c.getId(),c,this.buildMetaContentmap(getSoyGlobalMap(req).get("proxyPath"), c));		
		return cp;
	}
	
	@GET
	@Path("images/{imageId}")
	@Produces("*/*")
	public Response getImages(@Context HttpServletRequest req,
			@PathParam("imageId") String imageId) {
		log.info("test");

		return api.getFileContent(api.getLiveVersion().getEntity().toString(), ((Figure)api.getContentById(imageId).getEntity()).getSrc());
	}
	
	@POST
	@Consumes({"application/x-www-form-urlencoded"})
	@Path("search/full-site/")
	@Produces("application/json")
	public List<ContentInfo> search(@Context HttpServletRequest req, @FormParam("searchString") String searchString) {
		String proxyPath = getSoyGlobalMap(req).get("proxyPath");
		Response searchResponse = api.search(searchString);
		
		List<ContentInfo> summaryOfSearchResults = null;
		if(searchResponse.getEntity() instanceof List<?>){
			summaryOfSearchResults = this.extractContentInfo((List<Content>) searchResponse.getEntity(), proxyPath);
		}
		
		return summaryOfSearchResults;
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

		// TODO split last name and firstname.
		User newUser = new User(null, name, null, email, role, school, year, feedback, new Date());
		
		Injector injector = Guice.createInjector(new SeguePersistenceConfigurationModule());
		IUserDataManager registrationManager = injector.getInstance(IUserDataManager.class);

		//boolean success = registrationManager.register(newUser) != null;
		// TODO this code is going to have to be re-written.
		boolean success = false;
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

		Injector injector = Guice.createInjector(new SeguePersistenceConfigurationModule());
		PropertiesLoader propertiesLoader = injector.getInstance(PropertiesLoader.class);
		
		// construct a new instance of the mailer object
		Mailer contactUsMailer = new Mailer(propertiesLoader.getProperty(Constants.MAILER_SMTP_SERVER), propertiesLoader.getProperty(Constants.MAIL_FROM_ADDRESS));
		
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
			contactUsMailer.sendMail(propertiesLoader.getProperty(Constants.MAIL_RECEIVERS).split(","), email, subject, message.toString());
			log.info("Contact Us - E-mail sent to " + propertiesLoader.getProperty(Constants.MAIL_RECEIVERS) + " " + email + " " + subject + " " + message.toString());
			
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
		Injector injector = Guice.createInjector(new IsaacPersistenceConfigurationModule());
		PropertiesLoader propertiesLoader = injector.getInstance(PropertiesLoader.class);
		
		String proxyPath = propertiesLoader.getProperty(Constants.PROXY_PATH);
		String trackingId = propertiesLoader.getProperty(Constants.ANALYTICS_TRACKING_ID);
		
		if(Strings.isNullOrEmpty(proxyPath)){
			proxyPath = req.getContextPath();
			log.debug("No proxyPath defined in properties file - using default context");
		}
		
		if(Strings.isNullOrEmpty(trackingId)){
			trackingId = "";
		}
		
//		if (req.getLocalAddr().equals("128.232.20.43")) {
//			proxyPath = "/research/dtg/rutherford-staging";
//			trackingId = "UA-45629473-1";
//		} else if (req.getLocalAddr().equals("128.232.20.40")) {
//			proxyPath = "/research/dtg/rutherford";
//			trackingId = "UA-45629473-2";
//		}
		
		ImmutableMap.Builder<String,String> globalMap = ImmutableMap.builder();
		globalMap.put("liveVersion", (String) api.getLiveVersion().getEntity());
		globalMap.put("contextPath", req.getContextPath());
		globalMap.put("proxyPath", proxyPath); // TODO: sort out how we pass this information to the client.
		globalMap.put(Constants.PROXY_PATH, proxyPath); // TODO: remove duplicate
		globalMap.put("analyticsTrackingId", trackingId);
		globalMap.put("newSessionId", UUID.randomUUID().toString());
		globalMap.put("newUserId", UUID.randomUUID().toString());

		// TODO: complete this so that we get proper user information.
		User user = api.getCurrentUser(req);
		if(null != user)
			globalMap.put("usersFirstname", user.getGivenName());
		else
			globalMap.put("usersFirstname", "");
		
		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss dd/M/yyyy");
		String date = sdf.format(api.dateOfVersionChange());
		globalMap.put("dateOfVersionChange", date); //This actually represents the last time we checked and updated the live version number.
		
		return globalMap.build();
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
	
	/**
	 * This method will look at a content objects related content list and return a list of contentInfo objects which can be used for creating links etc.
	 * 
	 * This method returns null if the content object provided has no related Content
	 * 
	 * @param proxyPath - the string prefix for the server being used
	 * @param content - the content object which contains related content
	 * @return
	 */
	private List<ContentInfo> buildMetaContentmap(String proxyPath, Content content){
		if(null == content){
			return null;
		}else if(content.getRelatedContent() == null || content.getRelatedContent().isEmpty()){
			return null;
		}
		
		List<ContentInfo> contentInfoList = new ArrayList<ContentInfo>();
		
		for(String id : content.getRelatedContent()){
			try{
				Content relatedContent = (Content) api.getContentById(id).getEntity();
				
				if(relatedContent == null){
					log.warn("Related content (" + id + ") does not exist in the data store.");
				} else {
					ContentInfo contentInfo = extractContentInfo(relatedContent, proxyPath);
					contentInfoList.add(contentInfo);
				}
			}
			catch(ClassCastException exception){
				exception.printStackTrace();
			}
		}
		
		return contentInfoList;
	}

	/**
	 * This method will extract basic information from a content object so the lighter ContentInfo object can be sent to the client instead.
	 * 
	 * TODO: we should use an automapper to do this in a nicer way.
	 * @param content
	 * @param proxyPath
	 * @return
	 */
	private ContentInfo extractContentInfo(Content content, String proxyPath){
		if (null == content)
			return null;
		
		// TODO fix url parameter to be less horrid
		ContentInfo contentInfo = null;
		if(content.getType().equals("image")){
			contentInfo = new ContentInfo(content.getId(), content.getTitle(), content.getType(), proxyPath + "/isaac/api/images/" + content.getId());
		}
		else if(content.getType().toLowerCase().contains("question")){
			contentInfo = new ContentInfo(content.getId(), content.getTitle(), content.getType(), proxyPath + '/' + "questions/" + content.getId());
		}
		else{
			contentInfo = new ContentInfo(content.getId(), content.getTitle(), content.getType(), proxyPath + '/' + content.getType().toLowerCase() + "s/" + content.getId());
		}
		return contentInfo;
	}

	/**
	 * Utility method to convert a list of content objects into a list of ContentInfo Objects 
	 * @param contentList
	 * @param proxyPath
	 * @return
	 */
	private List<ContentInfo> extractContentInfo(List<Content> contentList, String proxyPath){
		if (null == contentList)
			return null;
		
		List<ContentInfo> listOfContentInfo = new ArrayList<ContentInfo>();
		
		for(Content content : contentList){
			ContentInfo contentInfo = extractContentInfo(content,proxyPath); 
			if(null != contentInfo)
				listOfContentInfo.add(contentInfo);
		}		
		return listOfContentInfo;
	}
}
