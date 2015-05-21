/**
 * Copyright 2014 Stephen Cummins
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.cam.cl.dtg.segue.dos.content;

import java.util.List;
import java.util.Set;

import uk.ac.cam.cl.dtg.segue.dto.content.SeguePageDTO;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Segue Page object.
 *
 */
@DTOMapping(SeguePageDTO.class)
@JsonContentType("page")
public class SeguePage extends Content {
	private String summary;
	
	@JsonCreator
	public SeguePage(@JsonProperty("_id") String _id,
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
			@JsonProperty("published") Boolean published,
			@JsonProperty("tags") Set<String> tags,
			@JsonProperty("level") Integer level) {
		
		super(_id, id, title, subtitle, type, author, encoding,
				canonicalSourceFile, layout, children, value, attribution,
				relatedContent, published, tags, level);

	}

	public SeguePage() { }
	
	/**
	 * Gets the summary.
	 * @return the summary
	 */
	public final String getSummary() {
		return summary;
	}

	/**
	 * Sets the summary.
	 * @param summary the summary to set
	 */
	public final void setSummary(final String summary) {
		this.summary = summary;
	}
}
