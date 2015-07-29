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

import java.util.List;
import java.util.Set;

import uk.ac.cam.cl.dtg.segue.dto.content.MediaDTO;
import uk.ac.cam.cl.dtg.segue.dto.content.NotificationDTO;

/**
 * @author sac92
 */
@JsonContentType("notification")
@DTOMapping(NotificationDTO.class)
public class Notification extends Content {
    private ExternalReference externalReference;
    
    /**
     * @param _id
     * @param id
     * @param title
     * @param subtitle
     * @param type
     * @param author
     * @param encoding
     * @param canonicalSourceFile
     * @param layout
     * @param children
     * @param value
     * @param attribution
     * @param relatedContent
     * @param published
     * @param tags
     * @param level
     */
    public Notification(final String _id, final String id, final String title, final String subtitle, final String type, final String author,
            final String encoding, final String canonicalSourceFile, final String layout, final List<ContentBase> children, final String value,
            final String attribution, final List<String> relatedContent, final Boolean published, final Set<String> tags, final Integer level) {
        super(_id, id, title, subtitle, type, author, encoding, canonicalSourceFile, layout, children, value,
                attribution, relatedContent, published, tags, level);
    }

    /**
     * @param value
     */
    public Notification(final String value) {
        super(value);
    }

    /**
     * 
     */
    public Notification() {

    }

    /**
     * Gets the externalReference.
     * @return the externalReference
     */
    public ExternalReference getExternalReference() {
        return externalReference;
    }

    /**
     * Sets the externalReference.
     * @param externalReference the externalReference to set
     */
    public void setExternalReference(final ExternalReference externalReference) {
        this.externalReference = externalReference;
    }
}
