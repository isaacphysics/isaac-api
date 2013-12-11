package uk.ac.cam.cl.dtg.teaching.models;

import org.bson.types.ObjectId;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonType("choice")
public class Choice extends Content {

	protected boolean correct;
	protected String explanation;
	
	@JsonCreator
	public Choice(@JsonProperty("_id") ObjectId _id,
			       @JsonProperty("id") String id, 
				   @JsonProperty("title") String title, 
				   @JsonProperty("type") String type, 
				   @JsonProperty("author") String author,
				   @JsonProperty("encoding") String encoding,
				   @JsonProperty("src") String src,
				   @JsonProperty("layout") String layout,
				   @JsonProperty("contentReferenced") String[] contentReferenced,
				   @JsonProperty("contentLiteral") String contentLiteral,
				   @JsonProperty("attribution") String attribution,
				   @JsonProperty("relatedContent") String[] relatedContent,
				   @JsonProperty("version") int version,
				   @JsonProperty("correct") boolean correct,
				   @JsonProperty("explanation") String explanation) {
		super(_id, 
		      id, 
		      title, 
		      type, 
		      author, 
		      encoding, 
		      src, 
		      layout, 
		      contentReferenced, 
		      contentLiteral, 
		      attribution, 
		      relatedContent, 
		      version);
		
		this.correct = correct;
		this.explanation = explanation;
	}

	public boolean getCorrect() {
		return correct;
	}
	public String getExplanation() {
		return explanation;
	}
}
