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
package uk.ac.cam.cl.dtg.segue.dto.content;

import java.util.ArrayList;
import java.util.List;

/**
 * This DTO represents high level information about a piece of content
 * 
 * This should be a light weight object used for presenting search results etc.
 * 
 */
public class ContentSummaryDTO {
	private String id;
	private String title;
	private String summary;
	private String type;
	private List<String> tags;
	private String url;

	/**
	 *  Private constructor required for Dozer.
	 */
	public ContentSummaryDTO() {
		tags = new ArrayList<String>();
	}

	/**
	 * Full constructor.
	 * @param id - id
	 * @param title - title
	 * @param type - type
	 * @param tags - tags
	 * @param url - url
	 */
	public ContentSummaryDTO(final String id, final String title, final String type,
			final List<String> tags, final String url) {
		this.id = id;
		this.type = type;
		this.tags = tags;
		this.title = title;
		this.url = url;
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
	public void setId(final String id) {
		this.id = id;
	}

	/**
	 * Gets the title.
	 * @return the title
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * Sets the title.
	 * @param title the title to set
	 */
	public void setTitle(final String title) {
		this.title = title;
	}

	/**
	 * Gets the summary.
	 * @return the summary
	 */
	public String getSummary() {
		return summary;
	}

	/**
	 * Sets the summary.
	 * @param summary the summary to set
	 */
	public void setSummary(final String summary) {
		this.summary = summary;
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
	public List<String> getTags() {
		return tags;
	}

	/**
	 * Sets the tags.
	 * @param tags the tags to set
	 */
	public void setTags(final List<String> tags) {
		this.tags = tags;
	}

	/**
	 * Gets the url.
	 * @return the url
	 */
	public String getUrl() {
		return url;
	}

	/**
	 * Sets the url.
	 * @param url the url to set
	 */
	public void setUrl(final String url) {
		this.url = url;
	}
}