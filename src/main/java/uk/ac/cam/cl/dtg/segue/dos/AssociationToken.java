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

/**
 * AssociationToken. This allows one user to request permission to view other
 * users details.
 * 
 * This token will be used to make new associations between users.
 */
public class AssociationToken {
	private String token;
	private String teacherId;
	private String labelId;

	/**
	 * AssociationToken - Default Constructor.
	 * 
	 * @param token
	 *            - unique id and token string.
	 * @param teacherId
	 *            - id of user who should be granted permission
	 * @param labelId
	 *            - group / label that users who use this token should be put in
	 *            / labelled.
	 */
	public AssociationToken(final String token, final String teacherId, final String labelId) {
		this.token = token;
		this.teacherId = teacherId;
		this.labelId = labelId;
	}

	/**
	 * Gets the token.
	 * 
	 * @return the token
	 */
	public String getToken() {
		return token;
	}

	/**
	 * Gets the teacher_id.
	 * 
	 * @return the teacher_id
	 */
	public String getTeacherId() {
		return teacherId;
	}

	/**
	 * Gets the label_id.
	 * 
	 * @return the label_id
	 */
	public String getLabelId() {
		return labelId;
	}
}
