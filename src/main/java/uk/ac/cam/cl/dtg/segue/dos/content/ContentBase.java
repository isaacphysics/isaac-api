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
package uk.ac.cam.cl.dtg.segue.dos.content;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import uk.ac.cam.cl.dtg.segue.dao.TrimWhitespaceDeserializer;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Represents any content related data that can be stored by the api
 * 
 * This class is required mainly due to the relatively complex polymorphic type hierarchy that gets serialized and
 * deserialized using a custom serializer (ContentBaseDeserializer).
 */
public abstract class ContentBase {
    protected String id;
    protected String type;
    protected Set<String> tags;
    protected String canonicalSourceFile;
    protected String version;
    protected List<Map<String, List<String>>> audience;
    protected Map<String, List<String>> display;

    public String getId() {
        return id;
    }

    @JsonDeserialize(using = TrimWhitespaceDeserializer.class)
    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getCanonicalSourceFile() {
        return canonicalSourceFile;
    }

    public void setCanonicalSourceFile(String canonicalSourceFile) {
        this.canonicalSourceFile = canonicalSourceFile;
    }

    public Set<String> getTags() {
        return tags;
    }

    public void setTags(Set<String> tags) {
        this.tags = tags;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public List<Map<String, List<String>>> getAudience() {
        return audience;
    }

    public void setAudience(List<Map<String, List<String>>> audience) {
        this.audience = audience;
    }

    public Map<String, List<String>> getDisplay() {
        return display;
    }

    public void setDisplay(Map<String, List<String>> display) {
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
