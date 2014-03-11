package uk.ac.cam.cl.dtg.isaac.models;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class TopicPage {

	private String topicTitle;
	private String level;
	private ImmutableList<String> conceptIds;
	private ImmutableList<String> questionIds;
	private ImmutableMap<String, ContentInfo> environment;
	private String topicPdf;
	private String topicId;

	public TopicPage(String topicId, String topicTitle, String level,
			ImmutableList<String> conceptIds,
			ImmutableList<String> questionIds,
			ImmutableMap<String, ContentInfo> environment,
			String topicPdf) {
		super();
		this.topicTitle = topicTitle;
		this.level = level;
		this.conceptIds = conceptIds;
		this.questionIds = questionIds;
		this.environment = environment;
		this.topicPdf = topicPdf;
		this.topicId = topicId;
	}

	public String getTopicTitle() {
		return topicTitle;
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
	
	public String getTopicPdf() {
		return topicPdf;
	}

	public String getTopicId() {
		return topicId;
	}
}