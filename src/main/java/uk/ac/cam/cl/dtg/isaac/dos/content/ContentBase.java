/*
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
package uk.ac.cam.cl.dtg.isaac.dos.content;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import uk.ac.cam.cl.dtg.segue.dao.TrimWhitespaceDeserializer;
import uk.ac.cam.cl.dtg.isaac.dos.AudienceContext;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Represents any content related data that can be stored by the api
 * <p>
 * This class is required mainly due to the relatively complex polymorphic type hierarchy that gets serialized and
 * deserialized using a custom serializer (ContentBaseDeserializer).
 */
public abstract class ContentBase {
    private String id;
    private String type;
    private Set<String> tags;
    private String canonicalSourceFile;
    private String version;
    private List<AudienceContext> audience;
    private Map<String, List<String>> display;

    public String getId() {
        return id;
    }

    @JsonDeserialize(using = TrimWhitespaceDeserializer.class)
    public void setId(final String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(final String type) {
        this.type = type;
    }

    public String getCanonicalSourceFile() {
        return canonicalSourceFile;
    }

    public void setCanonicalSourceFile(final String canonicalSourceFile) {
        this.canonicalSourceFile = canonicalSourceFile;
    }

    public Set<String> getTags() {
        return tags;
    }

    public void setTags(final Set<String> tags) {
        this.tags = tags;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(final String version) {
        this.version = version;
    }

    public List<AudienceContext> getAudience() {
        return audience;
    }

    public void setAudience(final List<AudienceContext> audience) {
        this.audience = audience;
    }

    public Map<String, List<String>> getDisplay() {
        return display;
    }

    public void setDisplay(final Map<String, List<String>> display) {
        this.display = display;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("Content Object ID: " + this.id);
        sb.append(" Type: " + this.type);
        sb.append(" Source File: " + this.canonicalSourceFile);

        return sb.toString();
    }
}
