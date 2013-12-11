package uk.ac.cam.cl.dtg.teaching.models;

import org.bson.types.ObjectId;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Content extends ContentBase {
	protected ObjectId _id;
	protected String id;
	protected String title;
	protected String type;
	protected String author;
	protected String encoding;
	protected String src;
	protected String layout;
	protected String[] contentReferenced;
	protected String contentLiteral;
	protected String attribution;
	protected String[] relatedContent;
	protected int version;
	
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
	}
	
	@JsonProperty("_id")
	public ObjectId getDbId() {
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
