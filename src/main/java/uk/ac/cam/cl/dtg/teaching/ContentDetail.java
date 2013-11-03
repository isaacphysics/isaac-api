package uk.ac.cam.cl.dtg.teaching;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.dtg.teaching.models.ContentInfo;

import com.google.common.collect.ImmutableList;

public class ContentDetail implements Comparable<ContentDetail> {

	private static final Logger log = LoggerFactory.getLogger(ContentDetail.class);

	@JsonProperty("ID")
	String id;

	@JsonProperty("TYPE")
	String type;

	@JsonProperty("TITLE")
	String title;

	@JsonProperty("TOPIC")
	String topic;

	@JsonProperty("LEVEL")
	String level;

	@JsonProperty("VIDEO")
	String videoId;

	@JsonProperty("CONCEPTS")
	List<String> relatedConceptIds;

	@JsonProperty("QUESTIONS")
	List<String> relatedQuestionIds;

	@JsonProperty("ORDER")
	int order;

	public static final String TYPE_QUESTION = "question";
	public static final String TYPE_CONCEPT = "concept";

	public ContentInfo toContentInfo() {
		return new ContentInfo(id, type, title, topic, level, videoId,
				relatedConceptIds == null ? ImmutableList.<String> of()
						: ImmutableList.copyOf(relatedConceptIds),
				relatedQuestionIds == null ? ImmutableList.<String> of()
						: ImmutableList.copyOf(relatedQuestionIds));
	}

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
		int cmp = new Integer(order).compareTo(o.order);
		if (cmp == 0) cmp = title.compareTo(o.title);
		if (cmp == 0) cmp = id.compareTo(id);
		return cmp;
	}
}