package uk.ac.cam.cl.dtg.segue.dto;

import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Choice object
 * The choice object is a specialized form of content and allows the storage of data relating to possible answers to questions. 
 *
 */
@JsonType("question")
public class Question extends Content {

	protected ContentBase answer;
	protected List<ContentBase> hints;
	
	@JsonCreator
	public Question(@JsonProperty("_id") String _id,
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
				   @JsonProperty("version") boolean published,
				   @JsonProperty("tags") Set<String> tags,
				   @JsonProperty("answer") ContentBase answer,
				   @JsonProperty("hints") List<ContentBase> hints) {
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
		      tags);
		
		this.answer = answer;
		this.hints = hints;
	}

	public Question(){
		super();
	}
	
	public ContentBase getAnswer() {
		return answer;
	}

	public List<ContentBase> getHints() {
		return hints;
	}

}
