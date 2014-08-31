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

/**
 * Media (Abstract) DTO To be used anywhere that a figure should be displayed in
 * the CMS.
 */
public abstract class MediaDTO extends ContentDTO {
	protected String src;
	protected String altText;

	/**
	 * Gets the src.
	 * 
	 * @return the src
	 */
	public final String getSrc() {
		return src;
	}

	/**
	 * Sets the src.
	 * 
	 * @param src
	 *            the src to set
	 */
	public void setSrc(final String src) {
		this.src = src;
	}

	/**
	 * Gets the altText.
	 * 
	 * @return the altText
	 */
	public String getAltText() {
		return altText;
	}

	/**
	 * Sets the altText.
	 * 
	 * @param altText
	 *            the altText to set
	 */
	public void setAltText(final String altText) {
		this.altText = altText;
	}
}
