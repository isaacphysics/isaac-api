package uk.ac.cam.cl.dtg.segue.dto;

import java.util.Set;

import org.mongojack.ObjectId;

import uk.ac.cam.cl.dtg.segue.dao.TrimWhitespaceDeserializer;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * Represents any content related data that can be stored by the api
 *
 * This class is required mainly due to the relatively complex polymorphic type hierarchy that gets serialized and deserialized using a custom serializer (ContentBaseDeserializer). 
 */
public abstract class ContentBase {

	// this is a legacy field used for mongodb indexing
	protected String _id;
	protected String id;
	protected String type;
	protected Set<String> tags;
	protected String canonicalSourceFile;

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
	
	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}
	
	public String getCanonicalSourceFile() {
		return canonicalSourceFile;
	}

	public void setCanonicalSourceFile(String canonicalSourceFile) {
		this.canonicalSourceFile = canonicalSourceFile;
	}

	public Set<String> getTags() {
		return tags;
	}

	public void setTags(Set<String> tags) {
		this.tags = tags;
	}
}
