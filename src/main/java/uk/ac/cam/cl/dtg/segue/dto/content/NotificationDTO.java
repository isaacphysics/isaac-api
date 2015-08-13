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
package uk.ac.cam.cl.dtg.segue.dto.content;

import uk.ac.cam.cl.dtg.segue.dos.content.ExternalReference;

/**
 * @author sac92
 */
public class NotificationDTO extends ContentDTO {
    private ExternalReference externalReference;

    /**
     * @param value for the value field
     */
    public NotificationDTO(final String value) {
        super(value);
    }

    /**
     * 
     */
    public NotificationDTO() {

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
}
