package uk.ac.cam.cl.dtg.isaac.models.content;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import uk.ac.cam.cl.dtg.segue.dto.Content;
import uk.ac.cam.cl.dtg.segue.dto.ContentBase;

public class IsaacFeaturedProfile extends Content{
	
	protected String emailAddress;
	
	@JsonCreator
	public IsaacFeaturedProfile(@JsonProperty("_id") String _id,
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
				   @JsonProperty("emailAddress") String emailAddress) {
		this._id = _id;
		this.id = id;
		this.title = title;
		this.subtitle = subtitle;
		this.type = type != null ? type : "string";
		this.author = author;
		this.encoding = encoding;
		this.setCanonicalSourceFile(canonicalSourceFile);
		this.layout = layout;
		this.value = value;
		this.attribution = attribution;
		this.relatedContent = relatedContent;
		this.published = published;
		this.children = children;
		this.tags = tags;
		this.emailAddress = emailAddress;
		
		// useful for when we want to augment this POJO
		if(null == this.children)
			this.children = new ArrayList<ContentBase>();
		
		if(null == this.tags)
			this.tags = new HashSet<String>();
		
	}
	
	/** 
	 * Default constructor required for Jackson
	 */
	public IsaacFeaturedProfile(){
		
	}
}
