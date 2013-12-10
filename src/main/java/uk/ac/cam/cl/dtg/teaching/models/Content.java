package uk.ac.cam.cl.dtg.teaching.models;

import org.bson.types.ObjectId;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Content {
	private ObjectId _id;
	private String id;
	private String title;
	private String type;
	private String author;
	private String encoding;
	public String src;
	private String layout;
	private String[] contentReferenced;
	private String contentLiteral;
	private String attribution;
	private String[] relatedContent;
	private int version;
	
	@JsonCreator
	public Content(@JsonProperty("_id") ObjectId _id,
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
				   @JsonProperty("version") int version) {
		this._id = _id;
		this.id = id;
		this.title = title;
		this.type = type;
		this.author = author;
		this.encoding = encoding;
		this.src = src;
		this.layout = layout;
		this.contentReferenced = contentReferenced;
		this.contentLiteral = contentLiteral;
		this.attribution = attribution;
		this.relatedContent = relatedContent;
		this.version = version;
	}
	

	public ObjectId get_Id() {
		return _id;
	}
	public String getId() {
		return id;
	}
	public String getTitle() {
		return title;
	}
	public String getType() {
		return type;
	}
	public String getAuthor() {
		return author;
	}
	public String getEncoding() {
		return encoding;
	}
	public String getSrc() {
		return src;
	}
	public String getLayout() {
		return layout;
	}
	public String[] getContentReferenced() {
		return contentReferenced;
	}
	public String getContentLiteral() {
		return contentLiteral;
	}
	public String getAttribution() {
		return attribution;
	}
	public String[] getRelatedContent() {
		return relatedContent;
	}
	public int getVersion() {
		return version;
	}
}
