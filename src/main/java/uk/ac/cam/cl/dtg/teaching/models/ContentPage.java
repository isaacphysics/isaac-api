package uk.ac.cam.cl.dtg.teaching.models;

import com.google.common.collect.ImmutableMap;

public class ContentPage {

	private String id;
	private String renderedContent;
	private ImmutableMap<String, ContentInfo> environment;

	public ContentPage(String id, String renderedContent,
			ImmutableMap<String, ContentInfo> environment) {
		super();
		this.id = id;
		this.renderedContent = renderedContent;
		this.environment = environment;
	}

	public String getId() {
		return id;
	}

	public String getRenderedContent() {
		return renderedContent;
	}

	public ImmutableMap<String, ContentInfo> getEnvironment() {
		return environment;
	}

}
