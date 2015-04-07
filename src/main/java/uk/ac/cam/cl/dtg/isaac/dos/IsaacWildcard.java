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
	protected String description;
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
	                     @JsonProperty("description") String description,
	                     @JsonProperty("url") String url) {
		super(_id, id, title, subtitle, type, author, encoding,
				canonicalSourceFile, layout, children, value, attribution,
				relatedContent, published, tags, level);

		this.description = description;
		this.url = url;
	}

	/**
	 * Default constructor required for Jackson
	 */
	public IsaacWildcard() {

	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getUrl() {
		// It appears as though sometimes urls are provided with trailing spaces in git... 
		// I do not know why...
		return url.trim();
	}

	public void setUrl(String url) {
		this.url = url;
	}
}
