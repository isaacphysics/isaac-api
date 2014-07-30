package uk.ac.cam.cl.dtg.segue.dos.content;

import java.util.List;
import java.util.Set;

import uk.ac.cam.cl.dtg.segue.dto.content.ChoiceDTO;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Choice object The choice object is a specialized form of content and allows
 * the storage of data relating to possible answers to questions.
 * 
 */
@DTOMapping(ChoiceDTO.class)
@JsonType("choice")
public class Choice extends Content {
	protected boolean correct;
	protected ContentBase explanation;

	/**
	 * Default Constructor required for mappers.
	 */
	public Choice() {
		
	}
	
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

	/**
	 * Gets the correct.
	 * @return the correct
	 */
	public final boolean isCorrect() {
		return correct;
	}

	/**
	 * Sets the correct.
	 * @param correct the correct to set
	 */
	public final void setCorrect(final boolean correct) {
		this.correct = correct;
	}

	/**
	 * Gets the explanation.
	 * @return the explanation
	 */
	public final ContentBase getExplanation() {
		return explanation;
	}

	/**
	 * Sets the explanation.
	 * @param explanation the explanation to set
	 */
	public final void setExplanation(final ContentBase explanation) {
		this.explanation = explanation;
	}
}
