package uk.ac.cam.cl.dtg.segue.dto.content;

import java.util.List;
import java.util.Set;

import uk.ac.cam.cl.dtg.segue.quiz.ChoiceQuestionValidator;
import uk.ac.cam.cl.dtg.segue.quiz.ValidatesWith;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Choice object The choice object is a specialised form of content and allows
 * the storage of data relating to possible answers to questions.
 * 
 */
@ValidatesWith(ChoiceQuestionValidator.class)
public class ChoiceQuestionDTO extends QuestionDTO {

	protected List<ChoiceDTO> choices;

	@JsonCreator
	public ChoiceQuestionDTO(@JsonProperty("_id") String _id,
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
			@JsonProperty("answer") ContentBaseDTO answer,
			@JsonProperty("hints") List<ContentBaseDTO> hints,
			@JsonProperty("choices") List<ChoiceDTO> choices) {
		super(_id, id, title, subtitle, type, author, encoding,
				canonicalSourceFile, layout, children, value, attribution,
				relatedContent, published, tags, level, answer, hints);

		this.choices = choices;
	}

	public ChoiceQuestionDTO() {
		super();
	}

	public List<ChoiceDTO> getChoices() {
		return choices;
	}

	/**
	 * Sets the choices.
	 * @param choices the choices to set
	 */
	public final void setChoices(List<ChoiceDTO> choices) {
		this.choices = choices;
	}
}
