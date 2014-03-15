package uk.ac.cam.cl.dtg.segue.auth;

import java.io.IOException;

import uk.ac.cam.cl.dtg.segue.dto.User;

/**
 * Classes that implement this interface must provide some way (via another interface) of 
 * allowing a user to be authenticated. The result of this authentication should be some unique reference 
 * that when provided to this class will allow us to extract user information from the external provider.
 * 
 * The exact specification of the unique reference number is up to the implementing class as long as it can be used
 * to gain information about a specific user.
 *
 */
public interface IFederatedAuthenticator {
	
	/**
	 * Send a request to the Provider's API to retrieve the user's information.
	 * 
	 * @param A reference code that allows the authenticator to identify the user (e.g. OAuth implementations will map this code to external tokens to allow api calls to be made).
	 * @return User's information contained in a User DTO.
	 * @throws NoUserIdException
	 * @throws IOException 
	 */
	public User getUserInfo(String internalProviderReference) throws NoUserIdException, IOException;
	
}