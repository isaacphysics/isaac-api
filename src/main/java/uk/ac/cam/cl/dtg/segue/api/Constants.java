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
package uk.ac.cam.cl.dtg.segue.api;

/**
 * Utility class to provide common segue-specific constants.
 * 
 */
public final class Constants {
	/**
	 * Name to use to describe the application to external services, e.g. 3rd
	 * party authenticators.
	 */
	public static final String APPLICATION_NAME = "Segue";

	// General Configuration stuff
	/**
	 * Constant representing the key for the HOST_NAME property - Used for
	 * establishing the Base URL for fully qualified urls.
	 */
	public static final String HOST_NAME = "HOST_NAME";

	/**
	 * Constant representing the key for the DEFAULT_LANDING_URL_SUFFIX property
	 * - Used for a default location of where authentication provider callbacks
	 * should land..
	 */
	public static final String DEFAULT_LANDING_URL_SUFFIX = "DEFAULT_LANDING_URL_SUFFIX";

	/**
	 * Constant representing the key for the MAILER_SMTP_SERVER property
	 * - Address of the SMTP server.
	 */
	public static final String MAILER_SMTP_SERVER = "MAILER_SMTP_SERVER";

	/**
	 * Constant representing the key for the MAIL_FROM_ADDRESS property
	 * - Email address to send mail from.
	 */
	public static final String MAIL_FROM_ADDRESS = "MAIL_FROM_ADDRESS";

	/**
	 * Constant representing the key for the maximum number of versions to
	 * cache.
	 */
	public static final String MAX_VERSIONS_TO_CACHE = "MAX_VERSIONS_TO_CACHE";
	
	/**
	 * The path for the csv file containing the list of schools.
	 */
	public static final String SCHOOL_CSV_LIST_PATH = "SCHOOL_CSV_LIST_PATH";

	// GIT stuff
	/**
	 * Constant representing the key for the path to Local GIT instance.
	 */
	public static final String LOCAL_GIT_DB = "LOCAL_GIT_DB";

	/**
	 * Constant representing the key for the path to the ssh private key for
	 * remote git repository.
	 */
	public static final String REMOTE_GIT_SSH_KEY_PATH = "REMOTE_GIT_SSH_KEY_PATH";

	/**
	 * Constant representing the key for the URL to remote git repository for
	 * SSH traffic.
	 */
	public static final String REMOTE_GIT_SSH_URL = "REMOTE_GIT_SSH_URL";

	/**
	 * Constant representing the key for the configuration option indicating
	 * whether or not unpublished content should be visible.
	 */
	public static final String SHOW_ONLY_PUBLISHED_CONTENT = "SHOW_ONLY_PUBLISHED_CONTENT";

	/**
	 * Constant representing the key for the version id of the content that
	 * should be served initially.
	 */
	public static final String INITIAL_LIVE_VERSION = "INITIAL_LIVE_VERSION";

	/**
	 * Constant representing the key for the location for the version config file..
	 */
	public static final String LIVE_VERSION_CONFIG_LOCATION = "LIVE_VERSION_CONFIG_LOCATION";
	
	/**
	 * Constant representing the key for the property id that represents whether
	 * segue should track and load all git changes as they arrive on the
	 * repository.
	 */
	public static final String FOLLOW_GIT_VERSION = "FOLLOW_GIT_VERSION";

	/**
	 * Constant representing the key for the property id that represents whether
	 * segue should track and load all git changes as they arrive on the
	 * repository.
	 */
	public static final String CLEAR_CACHES_ON_APP_START = "CLEAR_CACHES_ON_APP_START";

	/**
	 * Constant representing the segue application version.
	 */
	public static final String SEGUE_APP_VERSION = "SEGUE_APP_VERSION";
	
	public static final String CONTENT_VERSION = "CONTENT_VERSION";

	/**
	 * Constant representing the segue application mode. e.g. either debug or production
	 */
	public static final String SEGUE_APP_ENVIRONMENT = "SEGUE_APP_ENVIRONMENT";

	/**
	 * Enum to describe types of server environment / profile.
	 */
	public enum EnvironmentType { PROD, DEV };
	
	// HMAC stuff
	/**
	 * Constant representing the key for the date signed property - used in HMAC
	 * calculations.
	 */
	public static final String DATE_SIGNED = "DATE_SIGNED";

	/**
	 * Constant representing the key for the Session id property - used in HMAC
	 * calculations.
	 */
	public static final String SESSION_ID = "SESSION_ID";

	/**
	 * Constant representing the key for the HMAC property - used in HMAC
	 * calculations.
	 */
	public static final String HMAC = "HMAC";

	public static final String SESSION_EXPIRY_SECONDS = "SESSION_EXPIRY_SECONDS";
	
	/**
	 * Constant representing the key for the SESSION USER ID - used in HMAC
	 * calculations.
	 */
	public static final String SESSION_USER_ID = "currentUserId";

	/**
	 * Constant representing the key for the HMAC Salt - used in HMAC
	 * calculations.
	 */
	public static final String HMAC_SALT = "HMAC_SALT";

	// Search stuff
	/**
	 * Constant representing the key for the ClusterName - used for Search
	 * providers.
	 */
	public static final String SEARCH_CLUSTER_NAME = "SEARCH_CLUSTER_NAME";

	/**
	 * Constant representing the key for the address of the Search Cluster -
	 * used for Search providers.
	 */
	public static final String SEARCH_CLUSTER_ADDRESS = "SEARCH_CLUSTER_ADDRESS";

	/**
	 * Constant representing the key for the port of the Search Cluster - used
	 * for Search providers.
	 */
	public static final String SEARCH_CLUSTER_PORT = "SEARCH_CLUSTER_PORT";

	/**
	 * Suffix to append to raw fields (minus dot separator) - these are fields
	 * that the search engine should not do any processing on (e.g. no stemming)
	 */
	public static final String UNPROCESSED_SEARCH_FIELD_SUFFIX = "raw";

	/**
	 * Enum to represent sort orders.
	 * 
	 */
	public enum SortOrder {
		ASC, DESC
	};

	/**
	 * Enum to represent search boolean operators.
	 * 
	 */
	public enum BooleanOperator {
		AND, OR
	};
	
	public static final String SCHOOLS_SEARCH_INDEX = "autocomplete_data";
	public static final String SCHOOLS_SEARCH_TYPE = "schools";

	// Federated Authentication Stuff
	public static final String REDIRECT_URL_PARAM_NAME = "auth_redirect";

	/**
	 * This constant will be used to determine if we are expecting a link account request or not.
	 */
	public static final String LINK_ACCOUNT_PARAM_NAME = "LINK_ACCOUNT_PARAM_NAME";

	/**
	 * This constant is used for matching against url params to prevent CSRF.
	 */
	public static final String STATE_PARAM_NAME = "state";

	/**
	 * This constant is used for matching against url params to prevent CSRF.
	 */
	public static final String OAUTH_TOKEN_PARAM_NAME = "oauth_token";

	/**
	 * Constant representing the key for the GOOGLE client secret location.
	 */
	public static final String GOOGLE_CLIENT_SECRET_LOCATION = "GOOGLE_CLIENT_SECRET_LOCATION";

	/**
	 * Constant representing the key for the GOOGLE OAUTH callback uri.
	 */
	public static final String GOOGLE_CALLBACK_URI = "GOOGLE_CALLBACK_URI";

	/**
	 * Constant representing the key for the GOOGLE OAUTH Scopes to be
	 * requested.
	 */
	public static final String GOOGLE_OAUTH_SCOPES = "GOOGLE_OAUTH_SCOPES";

	/**
	 * Constant representing the key for the FACEBOOK OAUTH secret.
	 */
	public static final String FACEBOOK_SECRET = "FACEBOOK_SECRET";

	/**
	 * Constant representing the key for the FACEBOOK OAUTH client id.
	 */
	public static final String FACEBOOK_CLIENT_ID = "FACEBOOK_CLIENT_ID";

	/**
	 * Constant representing the key for the FACEBOOK OAUTH callback uri.
	 */
	public static final String FACEBOOK_CALLBACK_URI = "FACEBOOK_CALLBACK_URI";

	/**
	 * Constant representing the key for the FACEBOOK OAUTH permissions to be
	 * requested.
	 */
	public static final String FACEBOOK_OAUTH_SCOPES = "FACEBOOK_OAUTH_SCOPES";

	/**
	 * Constant representing the key for the TWITTER OAUTH secret.
	 */
	public static final String TWITTER_SECRET = "TWITTER_SECRET";

	/**
	 * Constant representing the key for the TWITTER OAUTH client id.
	 */
	public static final String TWITTER_CLIENT_ID = "TWITTER_CLIENT_ID";

	/**
	 * Constant representing the key for the TWITTER OAUTH callback uri.
	 */
	public static final String TWITTER_CALLBACK_URI = "TWITTER_CALLBACK_URI";
	
	// Local authentication specific stuff
	public static final String LOCAL_AUTH_EMAIL_FIELDNAME = "email";
	public static final String LOCAL_AUTH_PASSWORD_FIELDNAME = "password";
	public static final String LOCAL_AUTH_RESET_TOKEN_FIELDNAME = "resetToken";
	
	// Database properties
	public static final String MONGO_DB_HOSTNAME = "MONGO_DB_HOSTNAME";
	public static final String MONGO_DB_PORT = "MONGO_DB_PORT";
	public static final String SEGUE_DB_NAME = "SEGUE_DB_NAME";
	
	// Logging component
	public static final String LOGGING_ENABLED = "LOGGING_ENABLED";
	public static final String LOG_TABLE_NAME = "loggedEvents";
	public static final String ANSWER_QUESTION = "ANSWER_QUESTION";
	public static final String MERGE_USER = "MERGE_USER";
	public static final String LOG_OUT = "LOG_OUT";
	
	/*
	 * Default values.
	 */
	public static final Integer DEFAULT_START_INDEX = 0;
	public static final String DEFAULT_START_INDEX_AS_STRING = "0";
	
	public static final Integer DEFAULT_RESULTS_LIMIT = 10;
	public static final String DEFAULT_RESULTS_LIMIT_AS_STRING = "10";
	
	// Content model specific stuff
	public static final String ID_FIELDNAME = "id";
	public static final String TITLE_FIELDNAME = "title";
	public static final String TYPE_FIELDNAME = "type";
	public static final String TAGS_FIELDNAME = "tags";
	public static final String VALUE_FIELDNAME = "value";
	public static final String CHILDREN_FIELDNAME = "children";
	public static final String LEVEL_FIELDNAME = "level";
	
	public static final String USER_ID_FKEY_FIELDNAME = "userId";

	public static final String ID_SEPARATOR = "|";
	public static final String ESCAPED_ID_SEPARATOR = "\\" + ID_SEPARATOR;
	
	// User persistence model stuff
	public static final String LINKED_ACCOUNT_USER_DTO_FIELDNAME = "linkedAccounts";
	public static final String LINKED_ACCOUNT_PROVIDER_FIELDNAME = "provider";
	public static final String LINKED_ACCOUNT_LOCAL_USER_ID_FIELDNAME = "localUserId";
	public static final String LINKED_ACCOUNT_PROVIDER_USER_ID_FIELDNAME = "providerUserId";
	public static final String QUESTION_ATTEMPTS_FIELDNAME = "questionAttempts";

	// School List loading - raw data
	public static final String SCHOOL_URN_FIELDNAME = "URN";
	public static final String SCHOOL_ESTABLISHMENT_NUMBER_FIELDNAME = "EstablishmentNumber";
	public static final String SCHOOL_ESTABLISHMENT_NAME_FIELDNAME = "EstablishmentName";
	public static final String SCHOOL_POSTCODE_FIELDNAME = "Postcode";
	
	// School List loading POJO fields
	public static final String SCHOOL_URN_FIELDNAME_POJO = "urn";
	public static final String SCHOOL_ESTABLISHMENT_NUMBER_FIELDNAME_POJO = "establishmentNumber";
	public static final String SCHOOL_ESTABLISHMENT_NAME_FIELDNAME_POJO = "name";
	public static final String SCHOOL_POSTCODE_FIELDNAME_POJO = "postcode";
	
	// cache settings
	public static final String MAX_CONTENT_CACHE_TIME = "MAX_CONTENT_CACHE_TIME";
	public static final int CACHE_FOR_ONE_DAY = 86400;
	public static final int CACHE_FOR_THIRTY_DAY = 2592000;
	public static final int ANONYMOUS_SESSION_DURATION_IN_MINUTES = 40;
	public static final int NEVER_CACHE_WITHOUT_ETAG_CHECK = 0;

	public static final String ANONYMOUS_USER = "ANONYMOUS_USER";

	/**
	 * Redirect response field name.
	 */
	public static final String REDIRECT_URL = "redirectUrl";

	public static final String SEGUE_AUTH_COOKIE = "SEGUE_AUTH_COOKIE";
	
	public static final String DEFAULT_DATE_FORMAT = "EEE MMM dd HH:mm:ss Z yyyy";

	
	/**
	 * Private constructor to prevent this class being created.
	 */
	private Constants() {
		// not allowed to create one of these as it wouldn't make sense.
	}
}
