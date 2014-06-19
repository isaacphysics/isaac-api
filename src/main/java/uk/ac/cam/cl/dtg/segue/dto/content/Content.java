package uk.ac.cam.cl.dtg.segue.dto.content;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import uk.ac.cam.cl.dtg.segue.dao.TrimWhitespaceListDeserializer;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * Content Class (Data Transfer Object)
 * This class represents a majority of content types within the Content Management system. It is generalised to encourage reuse as much as is appropriate.
 * This object should be kept as being easily serializable to enable it to be exposed via web views.
 * 
 */
@JsonType("content")
public class Content extends ContentBase{
	protected String title;
	protected String subtitle;
	protected String author;
	protected String encoding;
	protected String layout;
	// this is the actual list of children content objects.
	protected List<ContentBase> children;
	protected String value;
	protected String attribution;
	protected List<String> relatedContent;
	protected boolean published;
	private String level;
	
	@JsonCreator
	public Content(@JsonProperty("_id") String _id,
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
				   @JsonProperty("level") String level) {
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
		this.level = level;
		
		// useful for when we want to augment this POJO
		if(null == this.children)
			this.children = new ArrayList<ContentBase>();
		
		if(null == this.tags)
			this.tags = new HashSet<String>();
		
	}
	
	/** 
	 * Default constructor required for Jackson
	 */
	public Content(){
		// useful for when we want to augment this POJO
		this.children = new ArrayList<ContentBase>();
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getSubtitle() {
		return subtitle;
	}

	public void setSubtitle(String subtitle) {
		this.subtitle = subtitle;
	}

	public String getAuthor() {
		return author;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	public String getEncoding() {
		return encoding;
	}

	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}

	public String getLayout() {
		return layout;
	}

	public void setLayout(String layout) {
		this.layout = layout;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public String getAttribution() {
		return attribution;
	}

	public void setAttribution(String attribution) {
		this.attribution = attribution;
	}

	public List<String> getRelatedContent() {
		return relatedContent;
	}

	@JsonDeserialize(using=TrimWhitespaceListDeserializer.class)
	public void setRelatedContent(List<String> relatedContent) {
		this.relatedContent = relatedContent;
	}

	public boolean getPublished() {
		return published;
	}

	public void setPublished(boolean published) {
		this.published = published;
	}
	
	public List<ContentBase> getChildren(){
		return this.children;
	}
	
	public String getLevel() {
		return level;
	}

	public void setLevel(String level) {
		this.level = level;
	}

	@Override
	public boolean equals(Object o){
		if(null == o || !(o instanceof Content))
			return false;
		
		Content c = (Content) o;
		boolean result = true;
		
		if(this.id != null){
			result = result && this.id.equals(c.getId());
		}
		if(this.title != null){
			result = result && this.title.equals(c.getTitle());
		}
		if(this.value != null){
			result = result && this.value.equals(c.getValue());
		}
			
		return result;
	}
	
	@Override
	public int hashCode(){
		int hashCode = 0;
		
		if(this.id != null)
			hashCode = hashCode + this.id.hashCode();
		
		if(this.title != null)
			hashCode = hashCode + this.title.hashCode();

		if(this.value != null)
			hashCode = hashCode + this.value.hashCode();
		
		return hashCode;
	}
	
}
