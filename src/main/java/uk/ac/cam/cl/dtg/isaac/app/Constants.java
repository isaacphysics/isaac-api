package uk.ac.cam.cl.dtg.isaac.app;

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

	public static final String RELATED_CONTENT_FIELDNAME = "relatedContent";

	/**
	 * Game specific variables.
	 */
	public static final int GAME_BOARD_SIZE = 10;
	
	/**
	 * Private constructor to prevent this class being created.
	 */
	private Constants() {
		// not allowed to create one of these as it wouldn't make sense.
	}
}
