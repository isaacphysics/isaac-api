package uk.ac.cam.cl.dtg.isaac.dto;

import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import uk.ac.cam.cl.dtg.segue.dos.content.Content;
import uk.ac.cam.cl.dtg.segue.dos.content.ContentBase;
import uk.ac.cam.cl.dtg.segue.dos.content.Image;
import uk.ac.cam.cl.dtg.segue.dos.content.JsonType;

@JsonType("isaacFeaturedProfile")
public class IsaacFeaturedProfile extends Content {

	protected String emailAddress;
	protected Image image;

	@JsonCreator
	public IsaacFeaturedProfile(@JsonProperty("_id") String _id,
			@JsonProperty("id") String id, @JsonProperty("title") String title,
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
			@JsonProperty("version") boolean published,
			@JsonProperty("tags") Set<String> tags,
			@JsonProperty("level") Integer level,
			@JsonProperty("src") String src,
			@JsonProperty("altText") String altText,
			@JsonProperty("emailAddress") String emailAddress,
			@JsonProperty("image") Image image) {
		super(_id, id, title, subtitle, type, author, encoding,
				canonicalSourceFile, layout, children, value, attribution,
				relatedContent, published, tags, level);

		this.emailAddress = emailAddress;
		this.image = image;
	}

	/**
	 * Default constructor required for Jackson
	 */
	public IsaacFeaturedProfile() {

	}

	public String getEmailAddress() {
		return emailAddress;
	}

	public void setEmailAddress(String emailAddress) {
		this.emailAddress = emailAddress;
	}

	public Image getImage() {
		return image;
	}

	public void setImage(Image image) {
		this.image = image;
	}
}
