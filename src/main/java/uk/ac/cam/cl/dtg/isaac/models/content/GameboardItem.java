package uk.ac.cam.cl.dtg.isaac.models.content;

import org.dozer.Mapping;

public abstract class GameboardItem {
	protected String id;
	protected String title;
	@Mapping("subtitle")
	protected String description;
	protected String uri;
	
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
	public String getUri() {
		return uri;
	}
	public void setUri(String url) {
		this.uri = url;
	}
}
