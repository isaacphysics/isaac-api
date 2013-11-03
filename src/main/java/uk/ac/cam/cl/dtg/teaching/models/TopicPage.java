package uk.ac.cam.cl.dtg.teaching.models;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class TopicPage {
	private String topicName;
	private String level;
	private ImmutableList<String> conceptIds;
	private ImmutableList<String> questionIds;
	private ImmutableMap<String, ContentInfo> environment;

	public TopicPage(String topicName, String level,
			ImmutableList<String> conceptIds,
			ImmutableList<String> questionIds,
			ImmutableMap<String, ContentInfo> environment) {
		super();
		this.topicName = topicName;
		this.level = level;
		this.conceptIds = conceptIds;
		this.questionIds = questionIds;
		this.environment = environment;
	}

	public String getTopicName() {
		return topicName;
	}

	public String getLevel() {
		return level;
	}

	public ImmutableList<String> getConceptIds() {
		return conceptIds;
	}

	public ImmutableList<String> getQuestionIds() {
		return questionIds;
	}

	public ImmutableMap<String, ContentInfo> getEnvironment() {
		return environment;
	}

}