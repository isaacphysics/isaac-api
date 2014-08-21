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
