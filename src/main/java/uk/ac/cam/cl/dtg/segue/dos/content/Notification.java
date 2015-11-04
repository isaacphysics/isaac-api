/**
 * Copyright 2015 Stephen Cummins
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

import java.util.Date;

import uk.ac.cam.cl.dtg.segue.dto.content.NotificationDTO;

/**
 * Notification. This class represents a special content type which can be used to pop up a modal to users on the site.
 * 
 */
@JsonContentType("notification")
@DTOMapping(NotificationDTO.class)
public class Notification extends Content {
    private ExternalReference externalReference;
    private Date expiry;
    
    /**
     * @param value - of the content
     */
    public Notification(final String value) {
        super(value);
    }

    /**
     * Default constructor.
     */
    public Notification() {

    }

    /**
     * Gets the externalReference.
     * 
     * @return the externalReference
     */
    public ExternalReference getExternalReference() {
        return externalReference;
    }

    /**
     * Sets the externalReference.
     * 
     * @param externalReference
     *            the externalReference to set
     */
    public void setExternalReference(final ExternalReference externalReference) {
        this.externalReference = externalReference;
    }

    /**
     * Gets the expiry.
     * @return the expiry
     */
    public Date getExpiry() {
        return expiry;
    }

    /**
     * Sets the expiry.
     * @param expiry the expiry to set
     */
    public void setExpiry(final Date expiry) {
        this.expiry = expiry;
    }
}
