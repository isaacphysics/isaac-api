package uk.ac.cam.cl.dtg.segue.dto;

import java.util.List;

import org.bson.types.ObjectId;

import uk.ac.cam.cl.dtg.rspp.models.JsonType;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Choice object
 * The choice object is a specialized form of content and allows the storage of data relating to possible answers to questions. 
 *
 */
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
				   @JsonProperty("contentReferenced") List<String> contentReferenced,
				   @JsonProperty("contentLiteral") String contentLiteral,
				   @JsonProperty("attribution") String attribution,
				   @JsonProperty("relatedContent") List<String> relatedContent,
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

	public Choice(){
		super();
	}
	
	public boolean isCorrect() {
		return correct;
	}

	public void setCorrect(boolean correct) {
		this.correct = correct;
	}

	public String getExplanation() {
		return explanation;
	}

	public void setExplanation(String explanation) {
		this.explanation = explanation;
	}


}
