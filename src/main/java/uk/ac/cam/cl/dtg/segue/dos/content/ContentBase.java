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

import java.util.Set;

import org.mongojack.ObjectId;

import uk.ac.cam.cl.dtg.segue.dao.TrimWhitespaceDeserializer;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * Represents any content related data that can be stored by the api
 * 
 * This class is required mainly due to the relatively complex polymorphic type hierarchy that gets serialized and
 * deserialized using a custom serializer (ContentBaseDeserializer).
 */
public abstract class ContentBase {
    // this is a legacy field used for mongodb indexing
    protected String _id;
    protected String id;
    protected String type;
    protected Set<String> tags;
    protected String canonicalSourceFile;
    protected String version;

    @JsonProperty("_id")
    @ObjectId
    public String get_id() {
        return _id;
    }

    @JsonProperty("_id")
    @ObjectId
    public void set_id(String _id) {
        this._id = _id;
    }

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

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("Content Object ID: " + this.id);
        sb.append(" Type: " + this.type);
        sb.append(" Source File: " + this.canonicalSourceFile);

        return sb.toString();
    }
}
