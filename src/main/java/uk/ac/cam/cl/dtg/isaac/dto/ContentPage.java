package uk.ac.cam.cl.dtg.isaac.dto;

import java.util.List;

import uk.ac.cam.cl.dtg.segue.dto.content.Content;
import uk.ac.cam.cl.dtg.segue.dto.content.ContentSummary;

/**
 * 
 * @deprecated this is an old class for the rutherford prototype.
 */
@Deprecated
public class ContentPage {
	private String id;
	private Content contentObject;
	private List<ContentSummary> sidebarContent;

	/**
	 * Create a new content page.
	 * @param id - 
	 * @param contentObject - 
	 * @param sidebarContent - 
	 */
	public ContentPage(final String id, final Content contentObject,
			final List<ContentSummary> sidebarContent) {
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

	public List<ContentSummary> getSidebarContent() {
		return sidebarContent;
	}
}
