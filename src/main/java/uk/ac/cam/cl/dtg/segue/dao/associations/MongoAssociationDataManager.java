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

import java.util.UUID;

import org.apache.commons.codec.binary.Base64;

import uk.ac.cam.cl.dtg.segue.database.MongoDb;

import com.google.inject.Inject;
import com.mongodb.DB;

/**
 * MongoAssociationDataManager.
 * 
 */
public class MongoAssociationDataManager implements IAssociationDataManager {
	public final DB database;

	private final int tokenLength = 8;

	/**
	 * PostgresAssociationDataManager.
	 * 
	 * @param database
	 *            - preconfigured connection
	 */
	@Inject
	public MongoAssociationDataManager(final MongoDb database) {
		this.database = database.getDB();
	}

	@Override
	public String generateToken(final String userIdRequestingToken, final String associatedGroupId) {
		// create some kind of token
		String token = new String(Base64.encodeBase64(UUID.randomUUID().toString().getBytes())).replace("=",
				"").substring(0, tokenLength);

		// TODO persist the token
		
		return token;
	}

	@Override
	public String generateToken(final String userIdRequestingToken) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void createAssociation(final String token, final String userIdGrantingAccess) {
		// TODO Auto-generated method stub

	}

	@Override
	public void deleteAssociation(final String userIdWithAccess, final String userIdRevokingAccess) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean hasValidAssociation(final String userIdWithAccess, final String userIdRevokingAccess) {
		// TODO Auto-generated method stub
		return false;
	}
}
