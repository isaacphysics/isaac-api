package uk.ac.cam.cl.dtg.isaac.dos;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacSymbolicQuestionDTO;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacWildcardDTO;
import uk.ac.cam.cl.dtg.segue.dos.content.Content;
import uk.ac.cam.cl.dtg.segue.dos.content.ContentBase;
import uk.ac.cam.cl.dtg.segue.dos.content.DTOMapping;
import uk.ac.cam.cl.dtg.segue.dos.content.JsonType;

import java.util.List;
import java.util.Set;

@JsonType("isaacWildcard")
@DTOMapping(IsaacWildcardDTO.class)
public class IsaacWildcard extends Content {
	protected String url;


	@JsonCreator
	public IsaacWildcard(@JsonProperty("_id") String _id,
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
	                     @JsonProperty("published") boolean published,
	                     @JsonProperty("tags") Set<String> tags,
	                     @JsonProperty("level") Integer level,
	                     @JsonProperty("url") String url) {
		super(_id, id, title, subtitle, type, author, encoding,
				canonicalSourceFile, layout, children, value, attribution,
				relatedContent, published, tags, level);

		this.url = url;
	}

	/**
	 * Default constructor required for Jackson
	 */
	public IsaacWildcard() {

	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}
}
