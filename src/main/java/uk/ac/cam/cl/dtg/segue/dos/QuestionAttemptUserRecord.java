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
package uk.ac.cam.cl.dtg.segue.dos;

import java.util.List;
import java.util.Map;

import org.mongojack.ObjectId;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.api.client.util.Maps;

/**
 * QuestionAttempts
 * 
 * This object represents a users question attempts record.
 */
public class QuestionAttemptUserRecord {
	@JsonProperty("_id")
	@ObjectId
	private String id;
	private String userId;
	
	// Map of questionPage id -> map of question id -> List of questionAttempts information
	private Map<String, Map<String, List<QuestionValidationResponse>>> questionAttempts;
	
	/**
	 * Default Constructor.
	 */
	public QuestionAttemptUserRecord() {
		questionAttempts = Maps.newHashMap();
	}
	
	/**
	 * Full Constructor.
	 * @param databaseId
	 *            - id for the database to store this record.
	 * @param userId
	 *            - the user which this relates to.
	 * @param questionAttempts
	 *            - the datastructure of the form Map of questionPage id -> map
	 *            of question id -> List of questionAttempts information
	 */
	@JsonCreator
	public QuestionAttemptUserRecord(
			@JsonProperty("_id")
			final String databaseId, 
			@JsonProperty("userId")
			final String userId,
			@JsonProperty("questionAttempts")
			final Map<String, Map<String, List<QuestionValidationResponse>>> questionAttempts) {
		this.id = databaseId;
		this.userId = userId;
		this.questionAttempts = questionAttempts;
	}
	
	/**
	 * Partial Constructor.
	 * @param databaseId
	 *            - id for the database to store this record.
	 * @param userId
	 *            - the user which this relates to.
	 */
	public QuestionAttemptUserRecord(final String databaseId, final String userId) {
		this.id = databaseId;
		this.userId = userId;
		this.questionAttempts = Maps.newHashMap();
	}

	/**
	 * Gets the id.
	 * @return the id
	 */
	@JsonProperty("_id")
	public String getId() {
		return id;
	}

	/**
	 * Gets the userId.
	 * @return the userId
	 */
	public String getUserId() {
		return userId;
	}

	/**
	 * Gets the questionAttempts.
	 * @return the questionAttempts
	 */
	public Map<String, Map<String, List<QuestionValidationResponse>>> getQuestionAttempts() {
		return questionAttempts;
	}
}
