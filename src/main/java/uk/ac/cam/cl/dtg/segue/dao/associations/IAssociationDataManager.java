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
package uk.ac.cam.cl.dtg.segue.dao.associations;

/**
 * Interface for data manager classes that deal with user association data.
 *
 */
public interface IAssociationDataManager {
	String generateToken(String userIdRequestingToken, String associatedGroupId);
	
	String generateToken(String userIdRequestingToken);
	
	void createAssociation(String token, String userIdGrantingAccess);
	
	void deleteAssociation(String userIdWithAccess, String userIdRevokingAccess);
	
	boolean hasValidAssociation(String userIdWithAccess, String userIdRevokingAccess);
}
