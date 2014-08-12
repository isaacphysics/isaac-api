package uk.ac.cam.cl.dtg.segue.dto.content;

import java.util.Set;

import org.mongojack.ObjectId;

import uk.ac.cam.cl.dtg.segue.dao.TrimWhitespaceDeserializer;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.api.client.util.Sets;

/**
 * Represents any content related data that can be stored by the api.
 * 
 * This class is required mainly due to the relatively complex polymorphic type
 * hierarchy that gets serialized and deserialized using a custom serializer
 * (ContentBaseDeserializer).
 */
public abstract class ContentBaseDTO {

	// this is a field used for mongodb indexing
	protected String _id;
	protected String id;
	protected String type;
	protected Set<String> tags;
	protected String canonicalSourceFile;
	protected String version;
	
	/**
	 * Default constructor.
	 */
	public ContentBaseDTO() {
		this.tags = Sets.newHashSet();
	}
	
	/**
	 * Gets the _id.
	 * @return the _id
	 */
	@JsonProperty("_id")
	@ObjectId
	public String get_id() {
		return _id;
	}

	/**
	 * Sets the _id.
	 * @param _id the _id to set
	 */
	@JsonProperty("_id")
	@ObjectId
	public void set_id(final String _id) {
		this._id = _id;
	}

	/**
	 * Gets the id.
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	/**
	 * Sets the id.
	 * @param id the id to set
	 */
	@JsonDeserialize(using = TrimWhitespaceDeserializer.class)
	public void setId(final String id) {
		this.id = id;
	}

	/**
	 * Gets the type.
	 * @return the type
	 */
	public String getType() {
		return type;
	}

	/**
	 * Sets the type.
	 * @param type the type to set
	 */
	public void setType(final String type) {
		this.type = type;
	}

	/**
	 * Gets the tags.
	 * @return the tags
	 */
	public Set<String> getTags() {
		return tags;
	}

	/**
	 * Sets the tags.
	 * @param tags the tags to set
	 */
	public void setTags(final Set<String> tags) {
		this.tags = tags;
	}

	/**
	 * Gets the canonicalSourceFile.
	 * @return the canonicalSourceFile
	 */
	public String getCanonicalSourceFile() {
		return canonicalSourceFile;
	}

	/**
	 * Sets the canonicalSourceFile.
	 * @param canonicalSourceFile the canonicalSourceFile to set
	 */
	public void setCanonicalSourceFile(final String canonicalSourceFile) {
		this.canonicalSourceFile = canonicalSourceFile;
	}

	/**
	 * Gets the version.
	 * @return the version
	 */
	public String getVersion() {
		return version;
	}

	/**
	 * Sets the version.
	 * @param version the version to set
	 */
	public void setVersion(final String version) {
		this.version = version;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		sb.append("Content Object ID: " + this.id);
		sb.append(" Type: " + this.type);
		sb.append(" Source File: " + this.canonicalSourceFile);

		return sb.toString();
	}
}
