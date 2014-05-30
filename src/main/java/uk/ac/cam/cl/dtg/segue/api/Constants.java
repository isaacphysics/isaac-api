package uk.ac.cam.cl.dtg.segue.api;

public class Constants {

	
	// General Configuration stuff
	/**
	 * Constant representing the key for the HOST_NAME property - Used for establishing the Base URL for fully qualified urls.
	 */
	public static final String HOST_NAME = "HOST_NAME"; 
	
	/**
	 * Constant representing the key for the DEFAULT_LANDING_URL_SUFFIX property - Used for a default location of where authentication provider callbacks should land..
	 */
	public static final String DEFAULT_LANDING_URL_SUFFIX = "DEFAULT_LANDING_URL_SUFFIX"; 	
	
	// GIT stuff
	
	/**
	 * Constant representing the key for the path to Local GIT instance
	 */
	public static final String LOCAL_GIT_DB = "LOCAL_GIT_DB";

	/**
	 * Constant representing the key for the path to the ssh private key for remote git repository  
	 */
	public static final String REMOTE_GIT_SSH_KEY_PATH = "REMOTE_GIT_SSH_KEY_PATH";

	/**
	 * Constant representing the key for the URL to remote git repository for SSH traffic. 
	 */
	public static final String REMOTE_GIT_SSH_URL = "REMOTE_GIT_SSH_URL";

	/**
	 * Constant representing the key for the version id of the content that should be served initially. 
	 */
	public static final String INITIAL_LIVE_VERSION = "INITIAL_LIVE_VERSION";
	
	/**
	 * Constant representing the key for the property id that represents whether segue should track and load all git changes as they arrive on the repository. 
	 */
	public static final String FOLLOW_GIT_VERSION = "FOLLOW_GIT_VERSION";

	// HMAC stuff
	
	/**
	 * Constant representing the key for the date signed property - used in HMAC calculations. 
	 */
	public static final String DATE_SIGNED = "DATE_SIGNED";

	/**
	 * Constant representing the key for the Session id property - used in HMAC calculations. 
	 */
	public static final String SESSION_ID = "SESSION_ID";
	
	/**
	 * Constant representing the key for the HMAC property - used in HMAC calculations. 
	 */
	public static final String HMAC = "HMAC";

	/**
	 * Constant representing the key for the SESSION USER ID - used in HMAC calculations. 
	 */
	public static final String SESSION_USER_ID = "currentUserId";

	/**
	 * Constant representing the key for the HMAC Salt - used in HMAC calculations.
	 */
	public static final String HMAC_SALT = "HMAC_SALT";
	
	// Search stuff
	
	/**
	 * Constant representing the key for the ClusterName - used for Search providers. 
	 */
	public static final String SEARCH_CLUSTER_NAME = "SEARCH_CLUSTER_NAME";

	/**
	 * Constant representing the key for the address of the Search Cluster - used for Search providers. 
	 */
	public static final String SEARCH_CLUSTER_ADDRESS = "SEARCH_CLUSTER_ADDRESS";
	
	/**
	 * Constant representing the key for the port of the Search Cluster - used for Search providers. 
	 */
	public static final String SEARCH_CLUSTER_PORT = "SEARCH_CLUSTER_PORT";
	
	// Enum to represent sort orders
	public enum SortOrder {ASC, DESC};
	
	// Federated Authentication Stuff
	
	/**
	 * Constant representing the key for the GOOGLE client secret location.
	 */
	public static final String GOOGLE_CLIENT_SECRET_LOCATION = "GOOGLE_CLIENT_SECRET_LOCATION";

	/**
	 * Constant representing the key for the GOOGLE OAUTH callback uri.
	 */
	public static final String GOOGLE_CALLBACK_URI = "GOOGLE_CALLBACK_URI";
	
	/**
	 * Constant representing the key for the GOOGLE OAUTH Scopes to be requested.
	 */
	public static final String GOOGLE_OAUTH_SCOPES = "GOOGLE_OAUTH_SCOPES";	
	
	
	/**
	 * Default values
	 */
	
	public static final String DEFAULT_SEARCH_LIMIT = "10";
	
}
