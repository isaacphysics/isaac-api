package uk.ac.cam.cl.dtg.segue.dto.content;

import java.util.List;
import java.util.Set;

import uk.ac.cam.cl.dtg.segue.dos.content.ContentBase;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Choice object The choice object is a specialized form of content and allows
 * the storage of data relating to possible answers to questions.
 * 
 */
public class ChoiceDTO extends ContentDTO {
	@JsonIgnore
	protected boolean correct;
	@JsonIgnore
	protected ContentBaseDTO explanation;

	@JsonCreator
	public ChoiceDTO(@JsonProperty("_id") String _id,
			@JsonProperty("id") String id, @JsonProperty("title") String title,
			@JsonProperty("subtitle") String subtitle,
			@JsonProperty("type") String type,
			@JsonProperty("author") String author,
			@JsonProperty("encoding") String encoding,
			@JsonProperty("canonicalSourceFile") String canonicalSourceFile,
			@JsonProperty("layout") String layout,
			@JsonProperty("children") List<ContentBaseDTO> children,
			@JsonProperty("value") String value,
			@JsonProperty("attribution") String attribution,
			@JsonProperty("relatedContent") List<String> relatedContent,
			@JsonProperty("published") boolean published,
			@JsonProperty("tags") Set<String> tags,
			@JsonProperty("level") Integer level,
			@JsonProperty("correct") boolean correct,
			@JsonProperty("explanation") ContentBaseDTO explanation) {
		super(_id, id, title, subtitle, type, author, encoding,
				canonicalSourceFile, layout, children, value, attribution,
				relatedContent, published, tags, level);

		this.correct = correct;
		this.explanation = explanation;
	}

	public ChoiceDTO() {
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
	public ContentBaseDTO getExplanation() {
		return explanation;
	}

	@JsonIgnore
	public void setExplanation(ContentBaseDTO explanation) {
		this.explanation = explanation;
	}

}
