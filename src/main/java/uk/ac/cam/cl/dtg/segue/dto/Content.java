package uk.ac.cam.cl.dtg.segue.dto;

import java.util.ArrayList;
import java.util.List;

import org.mongojack.ObjectId;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Content Class (Data Transfer Object)
 * This class represents a majority of content types within the Content Management system. It is generalised to encourage reuse as much as is appropriate.
 * This object should be kept as being easily serializable to enable it to be exposed via web views.
 * 
 */
public class Content{
	private String _id;
	protected String id;
	protected String title;
	protected String type;
	protected String author;
	protected String encoding;
	protected String src;
	protected String layout;
	protected List<String> contentReferenced;
	@JsonIgnore
	protected List<Content> contentReferencedList;
	protected String contentLiteral;
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
				   @JsonProperty("layout") String layout,
				   @JsonProperty("contentReferenced") List<String> contentReferenced,
				   @JsonProperty("contentLiteral") String contentLiteral,
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
		this.layout = layout;
		this.contentReferenced = contentReferenced;
		this.contentLiteral = contentLiteral;
		this.attribution = attribution;
		this.relatedContent = relatedContent;
		this.version = version;
		// useful for when we want to augment this POJO
		this.contentReferencedList = new ArrayList<Content>();
	}
	
	/** 
	 * Default constructor required for Jackson
	 */
	public Content(){

		// useful for when we want to augment this POJO
		this.contentReferencedList = new ArrayList<Content>();
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

	public List<String> getContentReferenced() {
		return contentReferenced;
	}

	public void setContentReferenced(List<String> contentReferenced) {
		this.contentReferenced = contentReferenced;
	}

	public String getContentLiteral() {
		return contentLiteral;
	}

	public void setContentLiteral(String contentLiteral) {
		this.contentLiteral = contentLiteral;
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

	public void setRelatedContent(List<String> relatedContent) {
		this.relatedContent = relatedContent;
	}

	public int getVersion() {
		return version;
	}

	public void setVersion(int version) {
		this.version = version;
	}
	
	@JsonIgnore
	public List<Content> getContentReferencedList(){
		return this.contentReferencedList;
	}
	
}
