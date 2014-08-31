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
package uk.ac.cam.cl.dtg.segue.dto.users;

import java.util.List;
import java.util.Map;

import uk.ac.cam.cl.dtg.segue.dos.QuestionValidationResponse;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.api.client.util.Maps;

/**
 * Data Object to represent an anonymous user of the system. 
 * 
 */
public class AnonymousUserDTO extends AbstractSegueUserDTO {
	private String sessionId;
	
	private Map<String, Map<String, List<QuestionValidationResponse>>> temporaryQuestionAttempts;
	
	/**
	 * Default constructor required for Jackson.
	 */
	public AnonymousUserDTO() {
		temporaryQuestionAttempts = Maps.newHashMap();
	}
	
	/**
	 * Full constructor for the AnonymousUser object.
	 * 
	 * @param sessionId
	 *            - Our session Unique ID
	 */
	public AnonymousUserDTO(@JsonProperty("_id") final String sessionId) {
		temporaryQuestionAttempts = Maps.newHashMap();
		this.sessionId = sessionId;
	}

	/**
	 * Gets the sessionId.
	 * @return the sessionId
	 */
	public String getSessionId() {
		return sessionId;
	}

	/**
	 * Sets the sessionId.
	 * @param sessionId the sessionId to set
	 */
	public void setSessionId(final String sessionId) {
		this.sessionId = sessionId;
	}

	/**
	 * Gets the temporaryQuestionAttempts.
	 * @return the temporaryQuestionAttempts
	 */
	public Map<String, Map<String, List<QuestionValidationResponse>>> getTemporaryQuestionAttempts() {
		return temporaryQuestionAttempts;
	}

	/**
	 * Sets the temporaryQuestionAttempts.
	 * @param temporaryQuestionAttempts the temporaryQuestionAttempts to set
	 */
	public void setTemporaryQuestionAttempts(
			final Map<String, Map<String, List<QuestionValidationResponse>>> temporaryQuestionAttempts) {
		this.temporaryQuestionAttempts = temporaryQuestionAttempts;
	}

}
