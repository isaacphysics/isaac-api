package uk.ac.cam.cl.dtg.isaac.dto;

import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import uk.ac.cam.cl.dtg.segue.dos.content.Image;
import uk.ac.cam.cl.dtg.segue.dos.content.JsonType;
import uk.ac.cam.cl.dtg.segue.dto.content.ContentBaseDTO;
import uk.ac.cam.cl.dtg.segue.dto.content.ContentDTO;
import uk.ac.cam.cl.dtg.segue.dto.content.ContentSummaryDTO;

/**
 * DO for isaac featured profiles.
 *
 */
@JsonType("isaacFeaturedProfile")
public class IsaacFeaturedProfileDTO extends ContentDTO {
	private String emailAddress;
	private Image image;

	@JsonCreator
	public IsaacFeaturedProfileDTO(@JsonProperty("_id") String _id,
			@JsonProperty("id") String id, @JsonProperty("title") String title,
			@JsonProperty("subtitle") String subtitle,
			@JsonProperty("type") String type,
			@JsonProperty("author") String author,
			@JsonProperty("encoding") String encoding,
			@JsonProperty("canonicalSourceFile") String canonicalSourceFile,
			@JsonProperty("layout") String layout,
			@JsonProperty("children") List<ContentBaseDTO> children,
			@JsonProperty("value") String value,
			@JsonProperty("attribution") String attribution,
			@JsonProperty("relatedContent") List<ContentSummaryDTO> relatedContent,
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
	 * Default constructor required for Jackson.
	 */
	public IsaacFeaturedProfileDTO() {

	}

	/**
	 * Gets the emailAddress.
	 * @return the emailAddress
	 */
	public String getEmailAddress() {
		return emailAddress;
	}

	/**
	 * Sets the emailAddress.
	 * @param emailAddress the emailAddress to set
	 */
	public void setEmailAddress(final String emailAddress) {
		this.emailAddress = emailAddress;
	}

	/**
	 * Gets the image.
	 * @return the image
	 */
	public Image getImage() {
		return image;
	}

	/**
	 * Sets the image.
	 * @param image the image to set
	 */
	public void setImage(final Image image) {
		this.image = image;
	}
}
