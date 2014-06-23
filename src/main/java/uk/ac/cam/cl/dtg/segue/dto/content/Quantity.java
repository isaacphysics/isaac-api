package uk.ac.cam.cl.dtg.segue.dto.content;

import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonType("quantity")
public class Quantity extends Choice {
	protected String unit;
	protected boolean requireUnitMatch;
	
	public Quantity(){
		
	}
	
	@JsonCreator
	public Quantity(@JsonProperty("_id") String _id,
			       @JsonProperty("id") String id, 
				   @JsonProperty("title") String title,
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
				   @JsonProperty("level") String level,
				   @JsonProperty("correct") boolean correct,
				   @JsonProperty("explanation") ContentBase explanation,
				   @JsonProperty("units") String unit,
				   @JsonProperty("requiredUnitMatch") boolean requiredUnitMatch) {
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
		      correct,
		      explanation);
		
		this.unit = unit;
		this.requireUnitMatch = requiredUnitMatch;
	}

	public String getUnit() {
		return unit;
	}

	public void setUnit(String unit) {
		this.unit = unit;
	}

	public boolean isRequireUnitMatch() {
		return requireUnitMatch;
	}

	public void setRequireUnitMatch(boolean requireUnitMatch) {
		this.requireUnitMatch = requireUnitMatch;
	}
	
}
