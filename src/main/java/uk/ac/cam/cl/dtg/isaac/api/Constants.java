package uk.ac.cam.cl.dtg.isaac.api;

/**
 * Utility class to provide common isaac-specific constants.
 *
 */
public final class Constants {
	public static final String MAILER_SMTP_SERVER = "MAILER_SMTP_SERVER";
	public static final String MAIL_FROM_ADDRESS = "MAIL_FROM_ADDRESS";
	public static final String MAIL_RECEIVERS = "MAIL_RECEIVERS";
	public static final String PROXY_PATH = "PROXY_PATH";
	public static final String ANALYTICS_TRACKING_ID = "ANALYTICS_TRACKING_ID";

	/*
	 * Subject specific constants.
	 */
	public static final String CONCEPT_TYPE = "isaacConceptPage";
	public static final String QUESTION_TYPE = "isaacQuestionPage";
	public static final String WILDCARD_TYPE = "isaacWildcard";
	public static final String PAGE_FRAGMENT_TYPE = "isaacPageFragment";
	public static final String PAGE_TYPE = "page";

	public static final String RELATED_CONTENT_FIELDNAME = "relatedContent";

	public static final String GAMEBOARD_COLLECTION_NAME = "gameboards";
	public static final String USERS_GAMEBOARD_COLLECTION_NAME = "UsersToGameboards";
	
	/**
	 * Game specific variables.
	 */
	public static final int GAME_BOARD_SIZE = 10;
	
	/**
	 * GameboardItemState
	 * Represents the potential states of a gameboard item.
	 */
	public enum GameboardItemState { COMPLETED, IN_PROGRESS, NOT_ATTEMPTED, TRY_AGAIN }
	
	/**
	 * GameboardState
	 * Represents the potential states of a gameboard.
	 */
	public enum GameboardState { COMPLETED, IN_PROGRESS, NOT_ATTEMPTED }
	// GameboardDTO field names
	public static final String CREATED_DATE_FIELDNAME = "created";
	public static final String VISITED_DATE_FIELDNAME = "lastVisited";
	
	/**
	 * Private constructor to prevent this class being created.
	 */
	private Constants() {
		// not allowed to create one of these as it wouldn't make sense.
	}
}
