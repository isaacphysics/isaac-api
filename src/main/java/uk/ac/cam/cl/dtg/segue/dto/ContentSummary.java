package uk.ac.cam.cl.dtg.segue.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO represents high level information about a piece of content
 * 
 * This should be a light weight object used for presenting search results etc.
 *
 */
public class ContentSummary{
	private String id;

	private String title;
	
	private String type;

	private String url;
	
	@JsonCreator
	public ContentSummary(
			@JsonProperty("id") String id, 
			@JsonProperty("title") String title, 
			@JsonProperty("type") String type, 
			@JsonProperty("url") String url) {
		this.id = id;
		this.type = type;
		this.title = title;
		this.url = url;
	}

	public String getId() {
		return id;
	}

	public String getType() {
		return type;
	}

	public String getTitle() {
		return title;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}
}