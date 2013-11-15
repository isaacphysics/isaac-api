package uk.ac.cam.cl.dtg.teaching.models;

import com.google.common.collect.ImmutableMap;

public class ContentPage {

	private String id;
	private String renderedContent;
	private ImmutableMap<String, ContentInfo> environment;
	private String nextContentUri;
	private String prevContentUri;
	private String upContentUri;

	public ContentPage(String id, 
			String renderedContent,
			ImmutableMap<String, ContentInfo> environment,
			String prevContentUri,
			String upContentUri,
			String nextContentUri) {
		super();
		this.id = id;
		this.renderedContent = renderedContent;
		this.environment = environment;
		this.nextContentUri = nextContentUri;
		this.prevContentUri = prevContentUri;
		this.upContentUri = upContentUri;
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
	
	public String getNextContentUri() {
		return nextContentUri;
	}
	
	public String getPrevContentUri() {
		return prevContentUri;
	}

	public String getUpContentUri() {
		return upContentUri;
	}
}
