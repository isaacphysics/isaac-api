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
package uk.ac.cam.cl.dtg.isaac.dto;

import uk.ac.cam.cl.dtg.isaac.dto.content.ContentDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.ImageDTO;

/**
 * DTO to represent a card.
 */
public class IsaacCardDTO extends ContentDTO {
    private ImageDTO image;
    private String clickUrl;
    private String buttonText;
    private boolean disabled;
    private boolean verticalContent;

    /**
     * Default constructor required for mapping.
     */
    public IsaacCardDTO() {}

    /**
     * Get the image.
     * @return the image.
     */
    public ImageDTO getImage() {
        return image;
    }

    /**
     * Set the image.
     * @param image the image to set.
     */
    public void setImage(ImageDTO image) {
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
     * Get the button text.
     * @return the button text.
     */
    public String getButtonText() {
        return buttonText;
    }

    /**
     * Set the button text.
     * @param buttonText the button text to set.
     */
    public void setButtonText(final String buttonText) {
        this.buttonText = buttonText;
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
