package uk.ac.cam.cl.dtg.isaac.models;

import java.util.List;
import uk.ac.cam.cl.dtg.segue.dto.Content;

public class ContentPage {

	private String id;
	private Content contentObject;
	private List<ContentInfo> sidebarContent;

	public ContentPage(String id, 
			Content contentObject,
			List<ContentInfo> sidebarContent) {
		super();
		this.id = id;
		this.contentObject = contentObject;
		this.sidebarContent = sidebarContent;

	}

	public String getId() {
		return id;
	}

	public Content getContentObject() {
		return contentObject;
	}

	public List<ContentInfo> getSidebarContent() {
		return sidebarContent;
	}
}
