package uk.ac.cam.cl.dtg.teaching;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;

import org.codehaus.jackson.map.JsonDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import com.papercut.silken.SilkenServlet;
import com.papercut.silken.TemplateRenderer;

import datomic.Entity;
import datomic.Connection;
import datomic.Database;
import datomic.Peer;
import datomic.Util;


@Path("/")
public class RutherfordController {

	private static final Logger log = LoggerFactory.getLogger(RutherfordController.class);

	// Map of contentID to detail
	private Map<String, ContentDetail> contentDetails = ContentDetail.load();
	
	// Map of topicPath to detail
	private Map<String, TopicDetail> topicDetails = TopicDetail.load();

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

		return new TopicPage(topicDetail.title, level, conceptIds, questionIds, environment);
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
		String renderedContent = renderTemplate("rutherford.content."
				+ question, getSoyGlobalMap(req));
		return new ContentPage(question, renderedContent, collectEnvironment());
	}

	@GET
	@Path("concepts/{concept}")
	@Produces("application/json")
	public ContentPage getConcept(@Context HttpServletRequest req,
			@PathParam("concept") String concept) {
		String renderedContent = renderTemplate(
				"rutherford.content." + concept, getSoyGlobalMap(req));
		return new ContentPage(concept, renderedContent, collectEnvironment());
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
				"newSessionId", UUID.randomUUID().toString());
	}

	@POST
	@Path("log")
	@Produces("application/json")
	public ImmutableMap<String, String> postLog(
			@Context HttpServletRequest req,
			@FormParam("sessionId") String sessionId,
			@FormParam("event") String eventJson) {
		
		
		//System.out.println("Log msg from session " + sessionId);
		
		DatomicLogger t = Clojure.generate(DatomicLogger.class);
		t.logEvent(sessionId, eventJson);

		return ImmutableMap.of("result", "success");
	}
	

}
