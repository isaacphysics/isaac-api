package uk.ac.cam.cl.dtg.rspp.models;

import com.google.common.collect.ImmutableList;

public class ContentInfo {

	private String id;

	private String type;

	private String title;

	private String topic;

	private String level;

	private ImmutableList<String> videoIds;

	private ImmutableList<String> relatedConceptIds;

	private ImmutableList<String> relatedQuestionIds;

	public ContentInfo(String id, String type, String title, String topic,
			String level, ImmutableList<String> videoIds,
			ImmutableList<String> relatedConceptIds,
			ImmutableList<String> relatedQuestionIds) {
		super();
		this.id = id;
		this.type = type;
		this.title = title;
		this.topic = topic;
		this.level = level;
		this.videoIds = videoIds;
		this.relatedConceptIds = relatedConceptIds;
		this.relatedQuestionIds = relatedQuestionIds;
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

	public String getTopic() {
		return topic;
	}

	public String getLevel() {
		return level;
	}

	public ImmutableList<String> getVideoIds() {
		return videoIds;
	}

	public ImmutableList<String> getRelatedConceptIds() {
		return relatedConceptIds;
	}

	public ImmutableList<String> getRelatedQuestionIds() {
		return relatedQuestionIds;
	}

	
}