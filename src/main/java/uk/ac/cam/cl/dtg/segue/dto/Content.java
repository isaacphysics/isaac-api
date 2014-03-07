package uk.ac.cam.cl.dtg.segue.dto;

import java.util.ArrayList;
import java.util.List;

import org.mongojack.ObjectId;

import uk.ac.cam.cl.dtg.rspp.models.JsonType;
import uk.ac.cam.cl.dtg.segue.dao.TrimWhitespaceDeserializer;
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
	private String _id;
	protected String id;
	protected String title;
	protected String type;
	protected String author;
	protected String encoding;
	protected String src;
	protected String layout;
	// this is the actual list of children content objects.
	protected List<ContentBase> children;
	protected String value;
	protected String attribution;
	protected List<String> relatedContent;
	protected int version;
	
	@JsonCreator
	public Content(@JsonProperty("_id") String _id,
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
				   @JsonProperty("version") int version) {
		this._id = _id;
		this.id = id;
		this.title = title;
		this.type = type != null ? type : "string";
		this.author = author;
		this.encoding = encoding;
		this.src = src;
		this.setCanonicalSourceFile(canonicalSourceFile);
		this.layout = layout;
		this.value = value;
		this.attribution = attribution;
		this.relatedContent = relatedContent;
		this.version = version;
		this.children = children;
		
		// useful for when we want to augment this POJO
		if(null == this.children)
			this.children = new ArrayList<ContentBase>();
		
	}
	
	/** 
	 * Default constructor required for Jackson
	 */
	public Content(){

		// useful for when we want to augment this POJO
		this.children = new ArrayList<ContentBase>();
	}
	
	@JsonProperty("_id")
	@ObjectId
	public String getDbId() {
		return _id;
	}
	
	@JsonProperty("_id")
	@ObjectId
	public void setDbId(String _id) {
		this._id = _id;
	}	
	
	public String getId() {
		return id;
	}
	
	@JsonDeserialize(using=TrimWhitespaceDeserializer.class)
	public void setId(String id) {
		this.id = id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
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

	public String getSrc() {
		return src;
	}

	public void setSrc(String src) {
		this.src = src;
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

	public int getVersion() {
		return version;
	}

	public void setVersion(int version) {
		this.version = version;
	}
	
	public List<ContentBase> getChildren(){
		return this.children;
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
