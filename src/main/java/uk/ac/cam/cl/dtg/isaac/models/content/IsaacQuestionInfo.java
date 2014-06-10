package uk.ac.cam.cl.dtg.isaac.models.content;

import org.dozer.Mapping;

/**
 * DTO that provides high level information for Isaac Questions
 * 
 * Used for gameboards and for related questions links
 */
public class IsaacQuestionInfo {
	
	private String id;
	private String title;
	@Mapping("subtitle")
	private String description;
	private String level;
	private String uri;
	private String state;
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getLevel() {
		return level;
	}
	public void setLevel(String level) {
		this.level = level;
	}
	public String getUri() {
		return uri;
	}
	public void setUri(String url) {
		this.uri = url;
	}
	public String getState() {
		return state;
	}
	public void setState(String state) {
		this.state = state;
	}
	
}
