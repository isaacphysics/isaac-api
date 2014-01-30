package uk.ac.cam.cl.dtg.rspp.models;

import uk.ac.cam.cl.dtg.segue.dto.Content;

import com.google.common.collect.ImmutableMap;

public class ContentPage {

	private String id;
	private Content contentObject;
	private ImmutableMap<String, ContentInfo> environment;
	private String nextContentUri;
	private String prevContentUri;
	private String upContentUri;

	public ContentPage(String id, 
			Content contentObject,
			ImmutableMap<String, ContentInfo> environment,
			String prevContentUri,
			String upContentUri,
			String nextContentUri) {
		super();
		this.id = id;
		this.contentObject = contentObject;
		this.environment = environment;
		this.nextContentUri = nextContentUri;
		this.prevContentUri = prevContentUri;
		this.upContentUri = upContentUri;
	}

	public String getId() {
		return id;
	}

	public Content getContentObject() {
		return contentObject;
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
