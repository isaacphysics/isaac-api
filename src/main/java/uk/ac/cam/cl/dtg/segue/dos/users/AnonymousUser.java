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
package uk.ac.cam.cl.dtg.segue.dos.users;

import java.util.Date;
import java.util.List;
import java.util.Map;

import uk.ac.cam.cl.dtg.segue.dos.QuestionValidationResponse;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.api.client.util.Maps;

/**
 * Domain Object to represent an Anonymous user of the system.
 * 
 */
public class AnonymousUser extends AbstractSegueUser {
    private String sessionId;

    private Map<String, Map<String, List<QuestionValidationResponse>>> temporaryQuestionAttempts;

    private Date dateCreated;

    /**
     * Default constructor required for Jackson.
     */
    public AnonymousUser() {
        temporaryQuestionAttempts = Maps.newHashMap();
    }

    /**
     * Full constructor for the AnonymousUser object.
     * 
     * @param sessionId
     *            - Our session Unique ID
     */
    public AnonymousUser(@JsonProperty("_id") final String sessionId) {
        temporaryQuestionAttempts = Maps.newHashMap();
        this.sessionId = sessionId;
    }

    /**
     * Full constructor for the AnonymousUser object.
     * 
     * @param sessionId
     *            - Our session Unique ID
     * @param temporaryQuestionAttempts
     *            - attempts.
     */
    public AnonymousUser(final String sessionId,
            final Map<String, Map<String, List<QuestionValidationResponse>>> temporaryQuestionAttempts) {
        this.temporaryQuestionAttempts = temporaryQuestionAttempts;
        this.sessionId = sessionId;
    }

    /**
     * Gets the sessionId.
     * 
     * @return the sessionId
     */
    public String getSessionId() {
        return sessionId;
    }

    /**
     * Sets the sessionId.
     * 
     * @param sessionId
     *            the sessionId to set
     */
    public void setSessionId(final String sessionId) {
        this.sessionId = sessionId;
    }

    /**
     * Gets the temporaryQuestionAttempts.
     * 
     * @return the temporaryQuestionAttempts
     */
    public Map<String, Map<String, List<QuestionValidationResponse>>> getTemporaryQuestionAttempts() {
        return temporaryQuestionAttempts;
    }

    /**
     * Sets the temporaryQuestionAttempts.
     * 
     * @param temporaryQuestionAttempts
     *            the temporaryQuestionAttempts to set
     */
    public void setTemporaryQuestionAttempts(
            final Map<String, Map<String, List<QuestionValidationResponse>>> temporaryQuestionAttempts) {
        this.temporaryQuestionAttempts = temporaryQuestionAttempts;
    }

    /**
     * Gets the dateCreated.
     * 
     * @return the dateCreated
     */
    public Date getDateCreated() {
        return dateCreated;
    }

    /**
     * Sets the dateCreated.
     * 
     * @param dateCreated
     *            the dateCreated to set
     */
    public void setDateCreated(final Date dateCreated) {
        this.dateCreated = dateCreated;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((dateCreated == null) ? 0 : dateCreated.hashCode());
        result = prime * result + ((sessionId == null) ? 0 : sessionId.hashCode());
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
        if (!(obj instanceof AnonymousUser)) {
            return false;
        }
        AnonymousUser other = (AnonymousUser) obj;
        if (dateCreated == null) {
            if (other.dateCreated != null) {
                return false;
            }
        } else if (!dateCreated.equals(other.dateCreated)) {
            return false;
        }
        if (sessionId == null) {
            if (other.sessionId != null) {
                return false;
            }
        } else if (!sessionId.equals(other.sessionId)) {
            return false;
        }
        return true;
    }

}
