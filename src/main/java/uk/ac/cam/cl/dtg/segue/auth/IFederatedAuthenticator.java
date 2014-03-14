package uk.ac.cam.cl.dtg.segue.auth;

import java.io.IOException;

import uk.ac.cam.cl.dtg.segue.dto.User;

public interface IFederatedAuthenticator {
	
	public User getUserInfo(String internalProviderReference) throws NoUserIdException, IOException;
	
}