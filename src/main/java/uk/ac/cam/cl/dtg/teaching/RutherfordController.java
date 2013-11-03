package uk.ac.cam.cl.dtg.teaching;

import java.util.Locale;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;

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

@Path("/")
public class RutherfordController {

	private static final Logger log = LoggerFactory
			.getLogger(RutherfordController.class);

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
				// see whether there is any concepts or questions for this
				boolean found = false;
				for(ContentDetail d : contentDetails.values()) {
					if (d.topic.equals(t.topic) && d.level.equals(e.getKey())) {
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
	public TopicPage getTopicWithLevel(@PathParam("topic") String topic,
			@PathParam("level") String level) {

		TopicDetail topicDetail = topicDetails.get(topic);

		ImmutableList.Builder<String> conceptIdBuilder = ImmutableList
				.builder();
		ImmutableList.Builder<String> questionIdBuilder = ImmutableList
				.builder();

		SortedSet<ContentDetail> values = new TreeSet<ContentDetail>(
				contentDetails.values());
		for (ContentDetail detail : values) {
			if (topic.equals(detail.topic) && level.equals(detail.level)) {
				if (ContentDetail.TYPE_CONCEPT.equals(detail.type)) {
					conceptIdBuilder.add(detail.id);
				} else if (ContentDetail.TYPE_QUESTION.equals(detail.type)) {
					questionIdBuilder.add(detail.id);
				}
			}
		}

		ImmutableList<String> conceptIds = conceptIdBuilder.build();
		ImmutableList<String> questionIds = questionIdBuilder.build();

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
		String renderedContent = renderTemplate("rutherford.questions."
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

	private String renderTemplate(String templateName,
			ImmutableMap<String, String> globalMap) {
		TemplateRenderer renderer = SilkenServlet.getTemplateRenderer();

		String cContent = "";
		try {
			cContent = renderer.render(templateName, null, globalMap,
					Locale.ENGLISH);
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

	public static ImmutableMap<String, String> getSoyGlobalMap(
			HttpServletRequest req) {
		String proxyPath;
		if (req.getLocalAddr().equals("128.232.20.43")) {
			proxyPath = "/research/dtg/rutherford-staging";
		} else if (req.getLocalAddr().equals("128.232.20.40")) {
			proxyPath = "/research/dtg/rutherford";
		} else {
			proxyPath = req.getContextPath();
		}
		return ImmutableMap.of("contextPath", req.getContextPath(),
				"proxyPath", proxyPath);
	}
}
