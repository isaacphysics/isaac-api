package uk.ac.cam.cl.dtg.isaac.models.content;

import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import uk.ac.cam.cl.dtg.segue.dto.content.Choice;
import uk.ac.cam.cl.dtg.segue.dto.content.ChoiceQuestion;
import uk.ac.cam.cl.dtg.segue.dto.content.ContentBase;
import uk.ac.cam.cl.dtg.segue.dto.content.JsonType;

@JsonType("isaacQuestion")
public class IsaacQuestion extends ChoiceQuestion{

	public IsaacQuestion(){
		
	}
	
	@JsonCreator
	public IsaacQuestion(@JsonProperty("_id") String _id,
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
				   @JsonProperty("choices") List<Choice> choices) {
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
	}

}
