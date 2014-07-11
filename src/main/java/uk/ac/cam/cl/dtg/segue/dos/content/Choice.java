package uk.ac.cam.cl.dtg.segue.dos.content;

import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Choice object The choice object is a specialized form of content and allows
 * the storage of data relating to possible answers to questions.
 * 
 */
@JsonType("choice")
public class Choice extends Content {
	@JsonIgnore
	protected boolean correct;
	@JsonIgnore
	protected ContentBase explanation;

	@JsonCreator
	public Choice(@JsonProperty("_id") String _id,
			@JsonProperty("id") String id, @JsonProperty("title") String title,
			@JsonProperty("subtitle") String subtitle,
			@JsonProperty("type") String type,
			@JsonProperty("author") String author,
			@JsonProperty("encoding") String encoding,
			@JsonProperty("canonicalSourceFile") String canonicalSourceFile,
			@JsonProperty("layout") String layout,
			@JsonProperty("children") List<ContentBase> children,
			@JsonProperty("value") String value,
			@JsonProperty("attribution") String attribution,
			@JsonProperty("relatedContent") List<String> relatedContent,
			@JsonProperty("published") boolean published,
			@JsonProperty("tags") Set<String> tags,
			@JsonProperty("level") Integer level,
			@JsonProperty("correct") boolean correct,
			@JsonProperty("explanation") ContentBase explanation) {
		super(_id, id, title, subtitle, type, author, encoding,
				canonicalSourceFile, layout, children, value, attribution,
				relatedContent, published, tags, level);

		this.correct = correct;
		this.explanation = explanation;
	}

	public Choice() {
		super();
	}

	@JsonIgnore
	public boolean isCorrect() {
		return correct;
	}

	@JsonIgnore
	public void setCorrect(boolean correct) {
		this.correct = correct;
	}

	@JsonIgnore
	public ContentBase getExplanation() {
		return explanation;
	}

	@JsonIgnore
	public void setExplanation(ContentBase explanation) {
		this.explanation = explanation;
	}

}
