package uk.ac.cam.cl.dtg.segue.dto.content;

import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Choice object The choice object is a specialized form of content and allows
 * the storage of data relating to possible answers to questions.
 * 
 */
public class QuestionDTO extends ContentDTO {

	protected ContentBaseDTO answer;
	protected List<ContentBaseDTO> hints;

	@JsonCreator
	public QuestionDTO(@JsonProperty("_id") String _id,
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
			@JsonProperty("relatedContent") List<ContentSummaryDTO> relatedContent,
			@JsonProperty("version") boolean published,
			@JsonProperty("tags") Set<String> tags,
			@JsonProperty("level") Integer level,
			@JsonProperty("answer") ContentBaseDTO answer,
			@JsonProperty("hints") List<ContentBaseDTO> hints) {
		super(_id, id, title, subtitle, type, author, encoding,
				canonicalSourceFile, layout, children, value, attribution,
				relatedContent, published, tags, level);

		this.answer = answer;
		this.hints = hints;
	}

	public QuestionDTO() {
		super();
	}

	public ContentBaseDTO getAnswer() {
		return answer;
	}

	public List<ContentBaseDTO> getHints() {
		return hints;
	}

	/**
	 * Sets the answer.
	 * @param answer the answer to set
	 */
	public final void setAnswer(ContentBaseDTO answer) {
		this.answer = answer;
	}

	/**
	 * Sets the hints.
	 * @param hints the hints to set
	 */
	public final void setHints(List<ContentBaseDTO> hints) {
		this.hints = hints;
	}

}
