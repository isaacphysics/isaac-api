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

import uk.ac.cam.cl.dtg.segue.dto.content.ImageDTO;

/**
 * Image is in any picture.
 */
@DTOMapping(ImageDTO.class)
@JsonContentType("image")
public class Image extends Media {
    private String clickUrl;
    private String clickTarget;

    /**
     * Default constructor required for mapping purposes.
     */
    public Image() {

    }

    /**
     * Gets the clickUrl.
     * 
     * @return the clickUrl
     */
    public String getClickUrl() {
        return clickUrl;
    }

    /**
     * Sets the clickUrl.
     * 
     * @param clickUrl
     *            the clickUrl to set
     */
    public void setClickUrl(final String clickUrl) {
        this.clickUrl = clickUrl;
    }

    /**
     * Gets the clickTarget.
     * 
     * @return the clickTarget
     */
    public String getClickTarget() {
        return clickTarget;
    }

    /**
     * Sets the clickTarget.
     * 
     * @param clickTarget
     *            the clickTarget to set
     */
    public void setClickTarget(final String clickTarget) {
        this.clickTarget = clickTarget;
    }
}
