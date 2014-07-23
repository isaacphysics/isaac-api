package uk.ac.cam.cl.dtg.segue.auth;

/**
 * Enum to represent the different authentication providers Segue supports.
 * 
 * This is used for identification purposes and to match api requests based on
 * the string values of the enum.
 * 
 * (I.e. if you want to authenticate against the GOOGLE provider you may pass
 * the string google to the api and this could be matched against the enum value
 * (ignoring case).
 */
public enum AuthenticationProvider {
	GOOGLE, FACEBOOK, TWITTER, RAVEN, TEST;
};
