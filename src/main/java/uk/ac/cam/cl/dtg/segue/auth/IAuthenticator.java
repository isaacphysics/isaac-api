package uk.ac.cam.cl.dtg.segue.auth;

/**
 * Parent interface for all authenticators both federated and local in the Segue
 * CMS.
 * 
 */
public interface IAuthenticator {
	
	/**
	 * Returns a string representation of the providers name.
	 * 
	 * @return AuthenticationProvider
	 */
	AuthenticationProvider getAuthenticationProvider();
}
