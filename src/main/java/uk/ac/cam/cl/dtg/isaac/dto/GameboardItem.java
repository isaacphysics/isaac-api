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
package uk.ac.cam.cl.dtg.isaac.dto;

import java.util.List;

import uk.ac.cam.cl.dtg.isaac.api.Constants.GameboardItemState;

/**
 * DTO that provides high level information for Isaac Questions.
 * 
 * Used for gameboards to represent cut down versions of questions
 */
public class GameboardItem {
    private String id;
    private String title;
    private String description;
    private String uri;
    private List<String> tags;

    private Integer level;
    private GameboardItemState state;

    /**
     * Gets the id.
     * 
     * @return the id
     */
    public final String getId() {
        return id;
    }

    /**
     * Sets the id.
     * 
     * @param id
     *            the id to set
     */
    public final void setId(final String id) {
        this.id = id;
    }

    /**
     * Gets the title.
     * 
     * @return the title
     */
    public final String getTitle() {
        return title;
    }

    /**
     * Sets the title.
     * 
     * @param title
     *            the title to set
     */
    public final void setTitle(final String title) {
        this.title = title;
    }

    /**
     * Gets the description.
     * 
     * @return the description
     */
    public final String getDescription() {
        return description;
    }

    /**
     * Sets the description.
     * 
     * @param description
     *            the description to set
     */
    public final void setDescription(final String description) {
        this.description = description;
    }

    /**
     * Gets the uri.
     * 
     * @return the uri
     */
    public final String getUri() {
        return uri;
    }

    /**
     * Sets the uri.
     * 
     * @param uri
     *            the uri to set
     */
    public final void setUri(final String uri) {
        this.uri = uri;
    }

    /**
     * Gets the tags.
     * 
     * @return the tags
     */
    public final List<String> getTags() {
        return tags;
    }

    /**
     * Sets the tags.
     * 
     * @param tags
     *            the tags to set
     */
    public final void setTags(final List<String> tags) {
        this.tags = tags;
    }

    /**
     * Gets the level.
     * 
     * @return the level
     */
    public final Integer getLevel() {
        return level;
    }

    /**
     * Sets the level.
     * 
     * @param level
     *            the level to set
     */
    public final void setLevel(final Integer level) {
        this.level = level;
    }

    /**
     * Gets the state.
     * 
     * @return the state
     */
    public final GameboardItemState getState() {
        return state;
    }

    /**
     * Sets the state.
     * 
     * @param state
     *            the state to set
     */
    public final void setState(final GameboardItemState state) {
        this.state = state;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof GameboardItem)) {
            return false;
        }
        GameboardItem other = (GameboardItem) obj;
        if (id == null) {
            if (other.id != null) {
                return false;
            }
        } else if (!id.equals(other.id)) {
            return false;
        }
        return true;
    }
}
