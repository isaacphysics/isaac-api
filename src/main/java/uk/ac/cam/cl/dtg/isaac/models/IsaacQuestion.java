package uk.ac.cam.cl.dtg.isaac.models;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import uk.ac.cam.cl.dtg.segue.dto.Choice;
import uk.ac.cam.cl.dtg.segue.dto.ChoiceQuestion;
import uk.ac.cam.cl.dtg.segue.dto.ContentBase;

public class IsaacQuestion extends ChoiceQuestion{
	protected String level;
	
	public IsaacQuestion(){
		
	}

	@JsonCreator
	public IsaacQuestion(@JsonProperty("_id") String _id,
		       @JsonProperty("id") String id, 
			   @JsonProperty("title") String title, 
			   @JsonProperty("type") String type, 
			   @JsonProperty("author") String author,
			   @JsonProperty("encoding") String encoding,
			   @JsonProperty("src") String src,
			   @JsonProperty("canonicalSourceFile") String canonicalSourceFile,				   
			   @JsonProperty("layout") String layout,
			   @JsonProperty("contentReferenced") List<ContentBase> children,
			   @JsonProperty("contentLiteral") String value,
			   @JsonProperty("attribution") String attribution,
			   @JsonProperty("relatedContent") List<String> relatedContent,
			   @JsonProperty("published") boolean published,
			   @JsonProperty("answer") ContentBase answer,
			   @JsonProperty("hints") List<ContentBase> hints,
			   @JsonProperty("choices") List<Choice> choices,
			   @JsonProperty("level") String level){
		
		super(_id, 
			      id, 
			      title, 
			      type, 
			      author, 
			      encoding, 
			      src, 
			      canonicalSourceFile,
			      layout, 
			      children, 
			      value, 
			      attribution, 
			      relatedContent, 
			      published,
			      answer,
			      hints,
			      choices);
		this.level = level;
		
	}
	
	public String getLevel() {
		return level;
	}

	public void setLevel(String level) {
		this.level = level;
	}

}
