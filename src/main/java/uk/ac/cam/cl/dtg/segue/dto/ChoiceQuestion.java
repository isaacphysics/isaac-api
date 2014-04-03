package uk.ac.cam.cl.dtg.segue.dto;

import java.util.List;
import java.util.Set;

import uk.ac.cam.cl.dtg.isaac.models.JsonType;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Choice object
 * The choice object is a specialised form of content and allows the storage of data relating to possible answers to questions. 
 *
 */
@JsonType("choiceQuestion")
public class ChoiceQuestion extends Question {
	
	protected List<Choice> choices;
	
	@JsonCreator
	public ChoiceQuestion(@JsonProperty("_id") String _id,
			       @JsonProperty("id") String id, 
				   @JsonProperty("title") String title, 
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
				   @JsonProperty("answer") ContentBase answer,
				   @JsonProperty("hints") List<ContentBase> hints,
				   @JsonProperty("choices") List<Choice> choices) {
		super(_id, 
		      id, 
		      title, 
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
		      answer,
		      hints);
		
		this.choices = choices;
	}

	public ChoiceQuestion(){
		super();
	}

	public List<Choice> getChoices() {
		return choices;
	}
}
