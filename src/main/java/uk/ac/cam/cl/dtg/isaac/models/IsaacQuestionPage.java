package uk.ac.cam.cl.dtg.isaac.models;

import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import uk.ac.cam.cl.dtg.segue.dto.Content;
import uk.ac.cam.cl.dtg.segue.dto.ContentBase;

public class IsaacQuestionPage extends Content {

	protected String level;
	
	public IsaacQuestionPage(){
		
	}

	@JsonCreator
	public IsaacQuestionPage(@JsonProperty("_id") String _id,
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
			   @JsonProperty("tags") Set<String> tags,
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
			      tags);
		
		this.level = level;
		
	}
	
	public String getLevel() {
		return level;
	}

	public void setLevel(String level) {
		this.level = level;
	}
}
