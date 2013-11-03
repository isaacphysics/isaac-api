package uk.ac.cam.cl.dtg.teaching;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TopicDetail implements Comparable<TopicDetail> {

	private static final Logger log = LoggerFactory
			.getLogger(TopicDetail.class);

	@JsonProperty("TOPIC")
	String topic;

	@JsonProperty("LINKTITLE")
	String linkTitle;

	@JsonProperty("PDF")
	Map<String, String> pdf;

	@JsonProperty("ORDER")
	int order;

	public static Map<String, TopicDetail> load() {
		InputStream is = ContentDetail.class.getClassLoader()
				.getResourceAsStream("topics.json");
		if (is == null) {
			log.error("Failed to find topics.json from context path");
		} else {
			ObjectMapper objectMapper = new ObjectMapper();
			try {
				Map<String, TopicDetail> loaded = objectMapper.readValue(is,
						new TypeReference<Map<String, TopicDetail>>() {
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
		return new HashMap<String,TopicDetail>();
	}

	@Override
	public int compareTo(TopicDetail o) {
		int cmp = new Integer(order).compareTo(o.order);
		if (cmp == 0)
			cmp = linkTitle.compareTo(o.linkTitle);
		if (cmp == 0)
			cmp = topic.compareTo(o.topic);
		return cmp;
	}

}
