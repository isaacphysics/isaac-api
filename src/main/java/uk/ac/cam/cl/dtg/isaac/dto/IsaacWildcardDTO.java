package uk.ac.cam.cl.dtg.isaac.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import uk.ac.cam.cl.dtg.segue.dos.content.Image;
import uk.ac.cam.cl.dtg.segue.dos.content.JsonType;
import uk.ac.cam.cl.dtg.segue.dto.content.ContentBaseDTO;
import uk.ac.cam.cl.dtg.segue.dto.content.ContentDTO;
import uk.ac.cam.cl.dtg.segue.dto.content.ContentSummaryDTO;

import java.util.List;
import java.util.Set;

/**
 * Isaac Symbolic Question DO.
 *
 */
@JsonType("isaacWildcard")
public class IsaacWildcardDTO extends ContentDTO {
	protected String url;

	@JsonCreator
	public IsaacWildcardDTO(@JsonProperty("_id") String _id,
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
	                        @JsonProperty("image") Image image,
	                        @JsonProperty("url") String url) {
		super(_id, id, title, subtitle, type, author, encoding,
				canonicalSourceFile, layout, children, value, attribution,
				relatedContent, published, tags, level);

		this.url = url;
	}

	/**
	 * Default constructor required for Jackson
	 */
	public IsaacWildcardDTO() {

	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}
}
