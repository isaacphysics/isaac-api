package uk.ac.cam.cl.dtg.teaching.models;

import com.google.common.collect.ImmutableList;

public class ContentInfo {

	private String id;

	private String type;

	private String linkTitle;

	private String topic;

	private String level;

	private String videoId;

	private ImmutableList<String> relatedConceptIds;

	private ImmutableList<String> relatedQuestionIds;

	public ContentInfo(String id, String type, String linkTitle, String topic,
			String level, String videoId,
			ImmutableList<String> relatedConceptIds,
			ImmutableList<String> relatedQuestionIds) {
		super();
		this.id = id;
		this.type = type;
		this.linkTitle = linkTitle;
		this.topic = topic;
		this.level = level;
		this.videoId = videoId;
		this.relatedConceptIds = relatedConceptIds;
		this.relatedQuestionIds = relatedQuestionIds;
	}

	public String getId() {
		return id;
	}

	public String getType() {
		return type;
	}

	public String getLinkTitle() {
		return linkTitle;
	}

	public String getTopic() {
		return topic;
	}

	public String getLevel() {
		return level;
	}

	public String getVideoId() {
		return videoId;
	}

	public ImmutableList<String> getRelatedConceptIds() {
		return relatedConceptIds;
	}

	public ImmutableList<String> getRelatedQuestionIds() {
		return relatedQuestionIds;
	}

	
}