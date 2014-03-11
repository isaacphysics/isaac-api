package uk.ac.cam.cl.dtg.isaac.app;

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
	private String topic;

	@JsonProperty("TITLE")
	private String title;

	@JsonProperty("PDF")
	private Map<String, String> pdf;

	@JsonProperty("ORDER")
	private int order;

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
		int cmp = new Integer(getOrder()).compareTo(o.getOrder());
		if (cmp == 0)
			cmp = getTitle().compareTo(o.getTitle());
		if (cmp == 0)
			cmp = getTopic().compareTo(o.getTopic());
		return cmp;
	}

	public String getTopic() {
		return topic;
	}

	public void setTopic(String topic) {
		this.topic = topic;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public Map<String, String> getPdf() {
		return pdf;
	}

	public void setPdf(Map<String, String> pdf) {
		this.pdf = pdf;
	}

	public int getOrder() {
		return order;
	}

	public void setOrder(int order) {
		this.order = order;
	}

}
