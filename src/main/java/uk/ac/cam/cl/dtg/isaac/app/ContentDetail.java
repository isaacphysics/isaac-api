package uk.ac.cam.cl.dtg.isaac.app;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @deprecated Not sure if we need this now
 *
 */
@JsonIgnoreProperties(ignoreUnknown=true)
public class ContentDetail implements Comparable<ContentDetail> {

	private static final Logger log = LoggerFactory.getLogger(ContentDetail.class);

	@JsonProperty("ID")
	private String id;

	@JsonProperty("TYPE")
	private String type;

	@JsonProperty("TITLE")
	private String title;

	@JsonProperty("TOPIC")
	private String topic;

	@JsonProperty("LEVEL")
	private String level;

	@JsonProperty("VIDEOS")
	private List<String> videoIds;

	@JsonProperty("CONCEPTS")
	private List<String> relatedConceptIds;

	@JsonProperty("QUESTIONS")
	private List<String> relatedQuestionIds;

	@JsonProperty("ORDER")
	private int order;
	
	public String prevContentId = null;
	public String nextContentId = null;

	public static final String TYPE_QUESTION = "question";
	public static final String TYPE_PHYSICS = "physics";
	public static final String TYPE_MATHS = "maths";
	
//	public ContentInfo toContentInfo() {
//		return new ContentInfo(id, type, title, topic, level, 
//				videoIds == null ? ImmutableList.<String> of() : ImmutableList.copyOf(videoIds),
//				relatedConceptIds == null ? ImmutableList.<String> of() : ImmutableList.copyOf(relatedConceptIds),
//				relatedQuestionIds == null ? ImmutableList.<String> of() : ImmutableList.copyOf(relatedQuestionIds));
//	}

	public static Map<String,ContentDetail> load() {
		InputStream is = ContentDetail.class.getClassLoader().getResourceAsStream(
				"resources.json");
		if (is == null) {
			log.error("Failed to find resources.json from context path");
		} else {
			ObjectMapper objectMapper = new ObjectMapper();
			try {
				Map<String, ContentDetail> loaded = objectMapper.readValue(is,
						new TypeReference<Map<String, ContentDetail>>() {
						});
				return Collections.unmodifiableMap(loaded);
			} catch (JsonParseException e) {
				log.error("Failed to parse resources.json", e);
			} catch (JsonMappingException e) {
				log.error("Failed to map resources.json to Java object", e);
			} catch (IOException e) {
				log.error("Unexpected IO exception reading JSON input stream",
						e);
			}
		}
		return new HashMap<String, ContentDetail>();
	}

	@Override
	public int compareTo(ContentDetail o) {
		int cmp = new Integer(getOrder()).compareTo(o.getOrder());
		if (cmp == 0) cmp = getTitle().compareTo(o.getTitle());
		if (cmp == 0) cmp = getId().compareTo(getId());
		return cmp;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getTopic() {
		return topic;
	}

	public void setTopic(String topic) {
		this.topic = topic;
	}

	public String getLevel() {
		return level;
	}

	public void setLevel(String level) {
		this.level = level;
	}

	public List<String> getVideoIds() {
		return videoIds;
	}

	public void setVideoIds(List<String> videoIds) {
		this.videoIds = videoIds;
	}

	public List<String> getRelatedConceptIds() {
		return relatedConceptIds;
	}

	public void setRelatedConceptIds(List<String> relatedConceptIds) {
		this.relatedConceptIds = relatedConceptIds;
	}

	public List<String> getRelatedQuestionIds() {
		return relatedQuestionIds;
	}

	public void setRelatedQuestionIds(List<String> relatedQuestionIds) {
		this.relatedQuestionIds = relatedQuestionIds;
	}

	public int getOrder() {
		return order;
	}

	public void setOrder(int order) {
		this.order = order;
	}
}