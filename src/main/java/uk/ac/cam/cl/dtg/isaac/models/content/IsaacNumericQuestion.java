package uk.ac.cam.cl.dtg.isaac.models.content;

import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import uk.ac.cam.cl.dtg.isaac.quiz.IsaacNumericValidator;
import uk.ac.cam.cl.dtg.segue.dto.content.Choice;
import uk.ac.cam.cl.dtg.segue.dto.content.ContentBase;
import uk.ac.cam.cl.dtg.segue.dto.content.JsonType;
import uk.ac.cam.cl.dtg.segue.dto.content.Question;
import uk.ac.cam.cl.dtg.segue.dto.content.TextInputQuestion;
import uk.ac.cam.cl.dtg.segue.quiz.IMultiFieldQuestion;
import uk.ac.cam.cl.dtg.segue.quiz.ValidatesWith;

@JsonType("isaacNumericQuestion")
@ValidatesWith(IsaacNumericValidator.class)
public class IsaacNumericQuestion extends IsaacQuestion implements IMultiFieldQuestion{
	@JsonIgnore
	private List<TextInputQuestion> fields;
	
	@JsonCreator
	public IsaacNumericQuestion(@JsonProperty("_id") String _id,
			       @JsonProperty("id") String id, 
				   @JsonProperty("title") String title, 
				   @JsonProperty("subtitle") String subtitle,
				   @JsonProperty("type") String type, 
				   @JsonProperty("author") String author,
				   @JsonProperty("encoding") String encoding,
				   @JsonProperty("canonicalSourceFile") String canonicalSourceFile,				   
				   @JsonProperty("layout") String layout,
				   @JsonProperty("contentReferenced") List<ContentBase> children,
				   @JsonProperty("value") String value,
				   @JsonProperty("attribution") String attribution,
				   @JsonProperty("relatedContent") List<String> relatedContent,
				   @JsonProperty("published") boolean published,
				   @JsonProperty("tags") Set<String> tags,
				   @JsonProperty("level") String level,
				   @JsonProperty("answer") ContentBase answer,
				   @JsonProperty("hints") List<ContentBase> hints,
				   @JsonProperty("choices") List<Choice> choices,
				   @JsonProperty("fields") List<TextInputQuestion> fields) {
		super(_id, 
		      id, 
		      title, 
		      subtitle,
		      type, 
		      author, 
		      encoding, 
		      canonicalSourceFile,
		      layout, 
		      children, 
		      value, 
		      attribution, 
		      relatedContent, 
		      published,
		      tags,
		      level,
		      answer,
		      hints,
		      choices);
		this.fields = fields; 
	}
	
	public IsaacNumericQuestion(){
		super();
	}
	
	@Override
	public List<? extends Question> getFields() {
		return fields;
	}
}
