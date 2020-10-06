/**
 * Copyright 2020 Connor Holloway
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


import uk.ac.cam.cl.dtg.segue.dos.content.Content;
import uk.ac.cam.cl.dtg.segue.dos.content.DTOMapping;
import uk.ac.cam.cl.dtg.segue.dos.content.Image;
import uk.ac.cam.cl.dtg.segue.dos.content.JsonContentType;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacCardDTO;

/**
 * A card with optional picture and link.
 */
@DTOMapping(IsaacCardDTO.class)
@JsonContentType("isaacCard")
public class IsaacCard extends Content {
    private Image image;
    private String clickUrl;
    private boolean disabled;
    private boolean verticalContent;

    /**
     * Get the image.
     * @return the image.
     */
    public Image getImage() {
        return image;
    }

    /**
     * Set the image.
     * @param image the image to set.
     */
    public void setImage(Image image) {
        this.image = image;
    }

    /**
     * Get the click url.
     * @return the click url.
     */
    public String getClickUrl() {
        return clickUrl;
    }

    /**
     * Set the click url.
     * @param clickUrl the click url to set.
     */
    public void setClickUrl(String clickUrl) {
        this.clickUrl = clickUrl;
    }

    /**
     * Return if the card is disabled.
     * @return if is disabled.
     */
    public boolean isDisabled() {
        return disabled;
    }

    /**
     * Set if the card is disabled.
     * @param disabled the disabled state to set to.
     */
    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    /**
     * Return if the card should be vertically displayed.
     * @return if is verticalContent.
     */
    public boolean isVerticalContent() {
        return verticalContent;
    }

    /**
     * Set if the card is vertical content.
     * @param verticalContent vertical content state to set to.
     */
    public void setVerticalContent(boolean verticalContent) {
        this.verticalContent = verticalContent;
    }
}
