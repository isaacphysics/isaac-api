package uk.ac.cam.cl.dtg.segue.dto.content;

import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class QuantityDTO extends ChoiceDTO {
	private String units;
	private boolean requireUnitsMatch;

	/**
	 * Default constructor required for mapping.
	 */
	public QuantityDTO() {

	}

	@JsonCreator
	public QuantityDTO(@JsonProperty("_id") String _id,
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
			@JsonProperty("relatedContent") List<String> relatedContent,
			@JsonProperty("published") boolean published,
			@JsonProperty("tags") Set<String> tags,
			@JsonProperty("level") Integer level,
			@JsonProperty("correct") boolean correct,
			@JsonProperty("explanation") ContentBaseDTO explanation,
			@JsonProperty("units") String units,
			@JsonProperty("requiredUnitMatch") boolean requiredUnitsMatch) {
		super(_id, id, title, subtitle, type, author, encoding,
				canonicalSourceFile, layout, children, value, attribution,
				relatedContent, published, tags, level, correct, explanation);

		this.units = units;
		this.requireUnitsMatch = requiredUnitsMatch;
	}

	public String getUnits() {
		return units;
	}

	public void setUnits(String unit) {
		this.units = unit;
	}

	public boolean isRequireUnitsMatch() {
		return requireUnitsMatch;
	}

	public void setRequireUnitsMatch(boolean requireUnitsMatch) {
		this.requireUnitsMatch = requireUnitsMatch;
	}
}
