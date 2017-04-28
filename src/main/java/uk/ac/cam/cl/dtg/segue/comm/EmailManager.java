package uk.ac.cam.cl.dtg.segue.comm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.util.Lists;
import com.google.api.client.util.Maps;
import com.google.api.client.util.Sets;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.api.managers.GameManager;
import uk.ac.cam.cl.dtg.isaac.dto.AssignmentDTO;
import uk.ac.cam.cl.dtg.isaac.dto.GameboardDTO;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacEventPageDTO;
import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.dao.ILogManager;
import uk.ac.cam.cl.dtg.segue.dao.ResourceNotFoundException;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dao.content.IContentManager;
import uk.ac.cam.cl.dtg.segue.dos.AbstractEmailPreferenceManager;
import uk.ac.cam.cl.dtg.segue.dos.IEmailPreference;
import uk.ac.cam.cl.dtg.segue.dos.content.ExternalReference;
import uk.ac.cam.cl.dtg.segue.dos.users.EmailVerificationStatus;
import uk.ac.cam.cl.dtg.segue.dto.UserGroupDTO;
import uk.ac.cam.cl.dtg.segue.dto.content.ContentDTO;
import uk.ac.cam.cl.dtg.segue.dto.content.EmailTemplateDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

import javax.annotation.Nullable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static uk.ac.cam.cl.dtg.segue.api.Constants.HOST_NAME;

/**
 * @author Alistair Stead
 *
 */
public class EmailManager extends AbstractCommunicationQueue<EmailCommunicationMessage> {
	private final AbstractEmailPreferenceManager emailPreferenceManager;
    private final PropertiesLoader globalProperties;
    private final IContentManager contentManager;

    private final ILogManager logManager;
    
    private static final Logger log = LoggerFactory.getLogger(EmailManager.class);
    private static final String SIGNATURE = "Isaac Physics Project";
    private static final int MINIMUM_TAG_LENGTH = 4;

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yy HH:mm");
    private static final DateFormat FULL_DATE_FORMAT = new SimpleDateFormat("EEE d MMM yyyy HH:mm a");

    /**
     * @param communicator
     *            class we'll use to send the actual email.
     * @param emailPreferenceManager
     *            email preference manager used to check if users want email.
     * @param globalProperties
     *            global properties used to get host name
     * @param contentManager
     *            content for email templates
     * @param logManager
     *            so we can log e-mail events.
     */
    @Inject
    public EmailManager(final EmailCommunicator communicator, final AbstractEmailPreferenceManager 
		    		emailPreferenceManager, final PropertiesLoader globalProperties,
                        final IContentManager contentManager, final ILogManager logManager) {
        super(communicator);
        this.emailPreferenceManager = emailPreferenceManager;
        this.globalProperties = globalProperties;
        this.contentManager = contentManager;
        this.logManager = logManager;
    }

    /**
     * Send an email to a user based on a content template.
     *
     * @param userDTO - the user to email
     * @param emailContentTemplate - the content template to send to the user.
     * @param tokenToValueMapping - a Map of tokens to values that will be replaced in the email template.
     * @param emailType - the type of email that this is so that it is filtered appropriately based on user email prefs.
     * @throws ContentManagerException
     * @throws SegueDatabaseException
     */
    public void sendTemplatedEmailToUser(final RegisteredUserDTO userDTO, final EmailTemplateDTO emailContentTemplate,
                                                   final Map<String, Object> tokenToValueMapping, final EmailType emailType)
            throws ContentManagerException, SegueDatabaseException {

        // generate properties from hashMap for token replacement process
        Properties propertiesToReplace = new Properties();
        propertiesToReplace.putAll(this.flattenTokenMap(tokenToValueMapping, Maps.newHashMap(), ""));

        // Add all properties in the user DTO so they are available to email templates.
        ObjectMapper om = new ObjectMapper();
        HashMap userPropertiesMap = om.convertValue(userDTO, HashMap.class);
        propertiesToReplace.putAll(this.flattenTokenMap(userPropertiesMap, Maps.newHashMap(), ""));

        // default properties
        //TODO: We should find and replace this in templates as the case is wrong.
        propertiesToReplace.putIfAbsent("givenname", userDTO.getGivenName() == null ? "" : userDTO.getGivenName());
        propertiesToReplace.putIfAbsent("sig", SIGNATURE);

        EmailCommunicationMessage emailCommunicationMessage
                = constructMultiPartEmail(userDTO.getId(), userDTO.getEmail(), emailContentTemplate, propertiesToReplace,
                emailType);

        if (emailType.equals(EmailType.SYSTEM)) {
                addSystemEmailToQueue(emailCommunicationMessage);
        } else {
            this.filterByPreferencesAndAddToQueue(userDTO, emailCommunicationMessage);
        }
    }

    /**
     * Sends notification for groups being given an assignment.
     * @param users
     *            - the group the gameboard is being assigned to
     * @param gameboard
     *            - gameboard that is being assigned to the users    
     * @throws ContentManagerException
     *             - some content may not have been accessible
     * @throws SegueDatabaseException
     *             - the content was of incorrect type
     * @deprecated use {@link #sendTemplatedEmailToUser(RegisteredUserDTO, EmailTemplateDTO, Map, EmailType)} instead
     */
    @Deprecated
    public void sendGroupAssignment(final List<RegisteredUserDTO> users, 
		    		final GameboardDTO gameboard)
		            throws ContentManagerException, SegueDatabaseException {
    	Validate.notNull(users);

        EmailTemplateDTO emailContent = getEmailTemplateDTO("email-template-group-assignment");
   
		String gameboardName = gameboard.getId();
		if (gameboard.getTitle() != null) {
			gameboardName = gameboard.getTitle();
		}

        for (RegisteredUserDTO userDTO : users) {
        	       	
            String gameboardURL = String.format("https://%s/#%s", globalProperties.getProperty(HOST_NAME),
                    gameboard.getId());
            String myAssignmentsURL = String.format("https://%s/assignments",
                    globalProperties.getProperty(HOST_NAME));
            Properties p = new Properties();
            p.put("givenname", userDTO.getGivenName() == null ? "" : userDTO.getGivenName());
            p.put("gameboardURL", gameboardURL);
            p.put("gameboardName", gameboardName);
            p.put("myAssignmentsURL", myAssignmentsURL);
            p.put("sig", SIGNATURE);


            EmailCommunicationMessage e = constructMultiPartEmail(userDTO.getId(), userDTO.getEmail(),
                    emailContent, p, EmailType.ASSIGNMENTS);
            this.filterByPreferencesAndAddToQueue(userDTO, e);
        }
    }

    /**
     * Sends notification that a user is booked onto an event.
     * @param user
     *            - the user to send the welcome email to
     * @param event
     *            - event that the user has been booked on to.
     * @throws ContentManagerException
     *             - some content may not have been accessible
     * @throws SegueDatabaseException
     *             - the content was of incorrect type
     * @deprecated use {@link #sendTemplatedEmailToUser(RegisteredUserDTO, EmailTemplateDTO, Map, EmailType)} instead
     */
    @Deprecated
    public void sendEventWelcomeEmail(final RegisteredUserDTO user,
                                      final IsaacEventPageDTO event)
        throws ContentManagerException, SegueDatabaseException {
        Validate.notNull(user);

        EmailTemplateDTO emailContent = getEmailTemplateDTO("email-event-booking-confirmed-new");

        String myAssignmentsURL = String.format("https://%s/assignments",
            globalProperties.getProperty(HOST_NAME));

        String contactUsURL = String.format("https://%s/contact",
            globalProperties.getProperty(HOST_NAME));

        String myBookedEventsURL = String.format("https://%s/events?show_booked_only=true",
                globalProperties.getProperty(HOST_NAME));

        String authorisationURL = String.format("https://%s/account?authToken=%s",
                globalProperties.getProperty(HOST_NAME), event.getIsaacGroupToken());

        Properties p = new Properties();
        // givenname should be camel case but I have left it to be in line with the others.
        p.put("givenname", user.getGivenName() == null ? "" : user.getGivenName());

        p.put("eventTitle", event.getTitle() == null ? "" : event.getTitle());
        p.put("eventSubtitle", event.getSubtitle() == null ? "" : event.getSubtitle());

        p.put("eventDate", event.getDate() == null ? "" : FULL_DATE_FORMAT.format(event.getDate()));
        p.put("endDate", event.getEndDate() == null ? "" : FULL_DATE_FORMAT.format(event.getEndDate()));
        p.put("prepWorkDeadline", event.getPrepWorkDeadline() == null ? "" : FULL_DATE_FORMAT.format(event.getPrepWorkDeadline()));

        p.put("myAssignmentsURL", myAssignmentsURL == null ? "" : myAssignmentsURL);
        p.put("contactUsURL", contactUsURL == null ? "" : contactUsURL);
        p.put("authorizationLink", authorisationURL == null ? "" : authorisationURL);

        p.put("addressLine1", event.getLocation().getAddress().getAddressLine1() == null
                ? "" : event.getLocation().getAddress().getAddressLine1());

        p.put("addressLine2", event.getLocation().getAddress().getAddressLine2() == null
                ? "" : event.getLocation().getAddress().getAddressLine2());

        p.put("town", event.getLocation().getAddress().getTown() == null
                ? "" : event.getLocation().getAddress().getTown());

        p.put("postalCode", event.getLocation().getAddress().getPostalCode() == null
                ? "" : event.getLocation().getAddress().getPostalCode());

        p.put("myBookedEventsURL", myBookedEventsURL);

        StringBuilder sb = new StringBuilder();
        if (event.getPreResources() != null && event.getPreResources().size() > 0) {
            for (ExternalReference er : event.getPreResources()){
                sb.append(String.format("<a href='%s'>%s</a>", er.getTitle(), er.getUrl()));
                sb.append("\n");
            }
            p.put("preResources", sb.toString());
        } else {
            p.put("preResources", "");
        }

        p.put("emailEventDetails", event.getEmailEventDetails() == null ? "" : event.getEmailEventDetails());

        p.put("sig", SIGNATURE);

        EmailCommunicationMessage e = constructMultiPartEmail(user.getId(), user.getEmail(),
            emailContent, p, EmailType.SYSTEM);

        this.filterByPreferencesAndAddToQueue(user, e);
    }

    /**
     * Sends notification that a user is on the waiting list.
     * @param user
     *            - the user to send the welcome email to
     * @param event
     *            - event that the user has been booked on to.
     * @throws ContentManagerException
     *             - some content may not have been accessible
     * @throws SegueDatabaseException
     *             - the content was of incorrect type
     * @deprecated use {@link #sendTemplatedEmailToUser(RegisteredUserDTO, EmailTemplateDTO, Map, EmailType)} instead
     */
    @Deprecated
    public void sendEventWaitingListEmail(final RegisteredUserDTO user,
                                      final IsaacEventPageDTO event)
        throws ContentManagerException, SegueDatabaseException {
        Validate.notNull(user);

        EmailTemplateDTO emailContent = getEmailTemplateDTO("email-event-waiting-list-addition-notification");

        String contactUsURL = String.format("https://%s/contact",
            globalProperties.getProperty(HOST_NAME));

        Properties p = new Properties();
        // givenname should be camel case but I have left it to be in line with the others.
        p.put("givenname", user.getGivenName() == null ? "" : user.getGivenName());
        p.put("eventTitle", event.getTitle() == null ? "" : event.getTitle());
        p.put("contactUsURL", contactUsURL == null ? "" : contactUsURL);
        p.put("eventDate", event.getDate() == null ? "" : FULL_DATE_FORMAT.format(event.getDate()));

        p.put("sig", SIGNATURE);

        EmailCommunicationMessage e = constructMultiPartEmail(user.getId(), user.getEmail(),
            emailContent, p, EmailType.SYSTEM);
        this.filterByPreferencesAndAddToQueue(user, e);

    }
    /**
     * Sends notification that a user has been promoted from the waiting list and been booked onto an event.
     * @param user
     *            - the user to send the welcome email to
     * @param event
     *            - event that the user has been booked on to.
     * @throws ContentManagerException
     *             - some content may not have been accessible
     * @throws SegueDatabaseException
     *             - the content was of incorrect type
     * @deprecated use {@link #sendTemplatedEmailToUser(RegisteredUserDTO, EmailTemplateDTO, Map, EmailType)} instead
     */
    @Deprecated
    public void sendEventWelcomeEmailForWaitingListPromotion(final RegisteredUserDTO user,
                                      final IsaacEventPageDTO event)
        throws ContentManagerException, SegueDatabaseException {
        Validate.notNull(user);

        EmailTemplateDTO emailContent = getEmailTemplateDTO("email-event-booking-waiting-list-promotion-confirmed");

        String myAssignmentsURL = String.format("https://%s/assignments",
            globalProperties.getProperty(HOST_NAME));

        String contactUsURL = String.format("https://%s/contact",
            globalProperties.getProperty(HOST_NAME));

        String authorisationURL = String.format("https://%s/account?authToken=%s",
            globalProperties.getProperty(HOST_NAME), event.getIsaacGroupToken());

        Properties p = new Properties();
        // givenname should be camel case but I have left it to be in line with the others.
        p.put("givenname", user.getGivenName() == null ? "" : user.getGivenName());
        p.put("eventTitle", event.getTitle() == null ? "" : event.getTitle());
        p.put("eventDate", event.getDate() == null ? "" : FULL_DATE_FORMAT.format(event.getDate()));
        p.put("prepWorkDeadline", event.getPrepWorkDeadline() == null ? "" : FULL_DATE_FORMAT.format(event.getPrepWorkDeadline()));
        p.put("contactUsURL", contactUsURL);
        p.put("myAssignmentsURL", myAssignmentsURL);
        p.put("authorizationLink", event.getIsaacGroupToken() == null ? "" : authorisationURL);

        StringBuilder sb = new StringBuilder();
        if (event.getPreResources() != null && event.getPreResources().size() > 0) {
            for (ExternalReference er : event.getPreResources()){
                sb.append(String.format("<a href='%s'>%s</a>", er.getTitle(), er.getUrl()));
                sb.append("\n");
            }
            p.put("preResources", sb.toString());
        } else {
            p.put("preResources", "");
        }

        p.put("emailEventDetails", event.getEmailEventDetails() == null ? "" : event.getEmailEventDetails());

        p.put("sig", SIGNATURE);

        EmailCommunicationMessage e = constructMultiPartEmail(user.getId(), user.getEmail(),
            emailContent, p, EmailType.EVENTS);
        this.filterByPreferencesAndAddToQueue(user, e);
    }

    /**
     * Sends notification that an event booking has been cancelled.
     * @param user
     *            - the user to send the welcome email to
     * @param event
     *            - event that the user has been booked on to.
     * @throws ContentManagerException
     *             - some content may not have been accessible
     * @throws SegueDatabaseException
     *             - the content was of incorrect type
     * @deprecated use {@link #sendTemplatedEmailToUser(RegisteredUserDTO, EmailTemplateDTO, Map, EmailType)} instead
     */
    @Deprecated
    public void sendEventCancellationEmail(final RegisteredUserDTO user,
                                                             final IsaacEventPageDTO event)
        throws ContentManagerException, SegueDatabaseException {
        Validate.notNull(user);

        EmailTemplateDTO emailContent = getEmailTemplateDTO("email-event-booking-cancellation-confirmed");

        String contactUsURL = String.format("https://%s/contact",
            globalProperties.getProperty(HOST_NAME));

        Properties p = new Properties();
        // givenname should be camel case but I have left it to be in line with the others.
        p.put("givenname", user.getGivenName() == null ? "" : user.getGivenName());
        p.put("eventTitle", event.getTitle() == null ? "" : event.getTitle());
        p.put("eventDate", event.getDate() == null ? "" : FULL_DATE_FORMAT.format(event.getDate()));
        p.put("contactUsURL", contactUsURL);
        p.put("sig", SIGNATURE);

        EmailCommunicationMessage e = constructMultiPartEmail(user.getId(), user.getEmail(),
            emailContent, p, EmailType.SYSTEM);
        this.filterByPreferencesAndAddToQueue(user, e);
    }

    /**
     * Sends notification for groups being given an assignment.
     * 
     * @param userDTO
     *            - the user who has joined the group
     * @param userGroup
     *            - the user group that the user is being assigned to
     * @param gameManager
     * 			  - the game manager we'll use to get the assignments
     * 
     * @param groupOwner
     *            - the owner of the group
     * @param existingAssignments
     *            - the assignments that already exist in the group
     * @throws ContentManagerException
     *             - some content may not have been accessible
     * @throws SegueDatabaseException
     *             - the content was of incorrect type
     * @deprecated use {@link #sendTemplatedEmailToUser(RegisteredUserDTO, EmailTemplateDTO, Map, EmailType)} instead
     */
    @Deprecated
    public void sendGroupWelcome(final RegisteredUserDTO userDTO, final UserGroupDTO userGroup,
					    		final RegisteredUserDTO groupOwner,
								final List<AssignmentDTO> existingAssignments,
					            final GameManager gameManager)
		            			throws ContentManagerException, SegueDatabaseException {
        Validate.notNull(userDTO);

        EmailTemplateDTO emailContent = getEmailTemplateDTO("email-template-group-welcome");

        String groupOwnerName = "Unknown";

        if (groupOwner != null && groupOwner.getFamilyName() != null) {
            groupOwnerName = groupOwner.getFamilyName();
        }

        if (groupOwner != null && groupOwner.getGivenName() != null && !groupOwner.getGivenName().isEmpty()) {
            groupOwnerName = groupOwner.getGivenName().substring(0, 1) + ". " + groupOwnerName;
        }

        if (existingAssignments != null) {
            Collections.sort(existingAssignments, new Comparator<AssignmentDTO>() {

                @Override
                public int compare(final AssignmentDTO o1, final AssignmentDTO o2) {
                    return o1.getCreationDate().compareTo(o2.getCreationDate());
                }

            });
        }
        
        StringBuilder htmlSB = new StringBuilder();
        StringBuilder plainTextSB = new StringBuilder();
        if (existingAssignments != null && existingAssignments.size() > 0) {
            htmlSB.append("Your teacher has assigned the following assignments:<br>");
            plainTextSB.append("Your teacher has assigned the following assignments:\n");
            for (int i = 0; i < existingAssignments.size(); i++) {

                GameboardDTO gameboard = gameManager.getGameboard(existingAssignments.get(i).getGameboardId());

                String gameboardName = existingAssignments.get(i).getGameboardId();
                if (gameboard != null && gameboard.getTitle() != null && !gameboard.getTitle().isEmpty()) {
                	gameboardName = gameboard.getTitle();
                }
                
				String gameboardUrl = String.format("https://%s/#%s",
								globalProperties.getProperty(HOST_NAME),
								existingAssignments.get(i).getGameboardId());

                htmlSB.append(String.format("%d. <a href='%s'>%s</a> (set on %s)<br>", i + 1, gameboardUrl,
                        gameboardName, DATE_FORMAT.format(existingAssignments.get(i).getCreationDate())));

                plainTextSB.append(String.format("%d. %s (set on %s)\n", i + 1, gameboardName,
                    DATE_FORMAT.format(existingAssignments.get(i).getCreationDate())));
            }
        } else if (existingAssignments != null && existingAssignments.size() == 0) {
            htmlSB.append("No assignments have been set yet.<br>");
            plainTextSB.append("No assignments have been set yet.\n");
        }

        String accountURL = String.format("https://%s/account", globalProperties.getProperty(HOST_NAME));
        Properties p = new Properties();
        p.put("givenname", userDTO.getGivenName() == null ? "" : userDTO.getGivenName());
        p.put("teacherName", groupOwnerName == null ? "" : groupOwnerName);
        p.put("accountURL", accountURL);
        p.put("sig", SIGNATURE);
        p.put("assignmentsInfo", plainTextSB.toString());
        p.put("assignmentsInfo_HTML", htmlSB.toString());

        EmailCommunicationMessage e = constructMultiPartEmail(userDTO.getId(), userDTO.getEmail(), emailContent, p,
                        EmailType.SYSTEM);
        this.filterByPreferencesAndAddToQueue(userDTO, e);

    }

    /**
     * @param givenName
     *            - users given name
     * @param familyName
     *            - users family name
     * @param emailAddress
     *            - the email address of the user
     * @param subject
     *            - the subject of the email
     * @param message
     *            - message from user
     * @param recipientEmailAddress
     *            - email address this email is being sent to
     * @param replyToAddress
     *            - the email address we want to be replied to
     * @param replyToName
     *            - the name to use for Reply-To
     * @param userId
     *            - the user ID (if known) of the user
     * @throws ContentManagerException
     *             - if some content is not found
     * @throws SegueDatabaseException
     *             - if the database cannot be accessed
     * @deprecated use {@link #sendTemplatedEmailToUser(RegisteredUserDTO, EmailTemplateDTO, Map, EmailType)} instead
     */
    @Deprecated
    public void sendContactUsFormEmail(final String givenName, final String familyName,
            final String emailAddress, final String subject, final String message,
            final String recipientEmailAddress, final String replyToAddress, final String replyToName,
                                       @Nullable final String userId)
            throws ContentManagerException, SegueDatabaseException {

        EmailTemplateDTO emailContent = getEmailTemplateDTO("email-contact-us-form");
        emailContent.setReplyToEmailAddress(replyToAddress);
        emailContent.setReplyToName(replyToName);
        emailContent.setSubject("(Contact Form) " + subject);

        Properties contentProperties = new Properties();
        contentProperties.put("contactGivenName", givenName == null ? "" : givenName);
        contentProperties.put("contactFamilyName", familyName == null ? "" : familyName);
        contentProperties.put("contactUserId", userId == null ? "" : userId);
        contentProperties.put("contactEmail", emailAddress == null ? "" : emailAddress);
        contentProperties.put("contactSubject", subject == null ? "" : subject);
        contentProperties.put("contactMessage", message == null ? "" : message);
        contentProperties.put("sig", SIGNATURE);

        EmailCommunicationMessage e = constructMultiPartEmail(null, recipientEmailAddress, emailContent,
                contentProperties,
                EmailType.SYSTEM);

        this.addSystemEmailToQueue(e);
    }
    
    /**
	 * @param sendingUser
	 * 				- the user object for the user sending the email
	 * @param contentObjectId
	 * 				- the id of the email template being used
	 * @param allSelectedUsers
	 * 				- the users to send email to
	 * @param emailType
	 * 				- the type of email to send (affects who receives it)
	 * @throws SegueDatabaseException
	 * 				- a segue database exception
	 * @throws ContentManagerException
	 * 				- a content management exception
	 */
    public void sendCustomEmail(final RegisteredUserDTO sendingUser, final String contentObjectId,
            final List<RegisteredUserDTO> allSelectedUsers, final EmailType emailType) throws SegueDatabaseException,
            ContentManagerException {
        Validate.notNull(allSelectedUsers);
        Validate.notNull(contentObjectId);

        EmailTemplateDTO emailContent = getEmailTemplateDTO(contentObjectId);

        Map<Long, Map<EmailType, Boolean>> allUserPreferences = this.emailPreferenceManager
                .getEmailPreferences(allSelectedUsers);

        int numberOfUnfilteredUsers = allSelectedUsers.size();
        Iterator<RegisteredUserDTO> userIterator = allSelectedUsers.iterator();
        while (userIterator.hasNext()) {
            RegisteredUserDTO user = userIterator.next();

            // don't continue if user has preference against this type of email
            if (allUserPreferences.containsKey(user.getId())) {
                Map<EmailType, Boolean> userPreferences = allUserPreferences.get(user.getId());
                if (userPreferences.containsKey(emailType) && !userPreferences.get(emailType)) {
                    userIterator.remove();
                    continue;
                }
            }

            Properties p = new Properties();
            p.put("givenname", user.getGivenName() == null ? "" : user.getGivenName());
            p.put("familyname", user.getFamilyName() == null ? "" : user.getFamilyName());
            p.put("email", user.getEmail());
            p.put("sig", SIGNATURE);

            EmailCommunicationMessage e = constructMultiPartEmail(user.getId(), user.getEmail(), emailContent, p,
                    emailType);

            ImmutableMap<String, Object> eventDetails = new ImmutableMap.Builder<String, Object>()
                    .put("userId", user.getId()).put("email", e.getRecipientAddress()).put("type", emailType)
                    .build();
            logManager.logInternalEvent(user, Constants.SEND_EMAIL, eventDetails);

            // add to the queue without using filterByPreferencesAndAddToQueue as we've already filtered for preferences
            super.addToQueue(e);
        }

        // Create a list of Ids
        ArrayList<Long> ids = Lists.newArrayList();
        for (RegisteredUserDTO userDTO : allSelectedUsers) {
            ids.add(userDTO.getId());
        }

        ImmutableMap<String, Object> eventDetails = new ImmutableMap.Builder<String, Object>().put("userIds", ids)
                .put("contentObjectId", contentObjectId)
                .put("contentVersionId", this.contentManager.getCurrentContentSHA()).build();
        this.logManager.logInternalEvent(sendingUser, "SENT_MASS_EMAIL", eventDetails);
        log.info(String.format("Added %d emails to the queue. %d were filtered.", allSelectedUsers.size(),
                numberOfUnfilteredUsers - allSelectedUsers.size()));
    }
    
    
    /**
     * This method checks the database for the user's email preferences and either adds them to 
     * the queue, or filters them out.
     * 
     * @param userDTO
     * 		- the userDTO used for logging. Must not be null. 
     * @param email
     * 		- the email we want to send. Must be non-null and have an associated non-null user id
     * @throws SegueDatabaseException
     *             - the content was of incorrect type
     */
    public void filterByPreferencesAndAddToQueue(final RegisteredUserDTO userDTO, 
    				final EmailCommunicationMessage email) throws SegueDatabaseException {
    	Validate.notNull(email);
    	Validate.notNull(userDTO);
    	
    	ImmutableMap<String, Object> eventDetails = new ImmutableMap.Builder<String, Object>()
		           .put("userId", userDTO.getId())
		           .put("email", email.getRecipientAddress())
		           .put("type", email.getEmailType())
		           .build();
    	
    	// don't send an email if we know it has failed before
    	if (userDTO.getEmailVerificationStatus() == EmailVerificationStatus.DELIVERY_FAILED) {
            log.info("Email sending abandoned - verification status is DELIVERY_FAILED");
    		return;
    	}
    	
    	// if this is an email type that cannot have a preference, send it and log as appropriate
    	if (!email.getEmailType().isValidEmailPreference()) {
	        logManager.logInternalEvent(userDTO, Constants.SEND_EMAIL, eventDetails);
    		addToQueue(email);
    		return;
    	}

    	try {
			IEmailPreference preference = 
							this.emailPreferenceManager.getEmailPreference(userDTO.getId(), email.getEmailType());
			if (preference != null && preference.getEmailPreferenceStatus()) {
		        logManager.logInternalEvent(userDTO, Constants.SEND_EMAIL, eventDetails);
				addToQueue(email);
			}
		} catch (SegueDatabaseException e1) {
			throw new SegueDatabaseException(String.format("Email of type %s cannot be sent - "
					+ "error accessing preferences in database", email.getEmailType().toString()));
		}
    }
    
    /**
     * This method allows us to send system email without checking for preferences. This should
     * not be used to send email to users
     * 
     * @param email
     * 		- the email we want to send
     * @throws SegueDatabaseException
     *             - the content was of incorrect type
     */
    public void addSystemEmailToQueue(final EmailCommunicationMessage email)
            throws SegueDatabaseException {

        addToQueue(email);
		log.info(String.format("Added system email to the queue with subject: %s", email.getSubject()));
    }
    
    /**
     * This method allows the front end to preview simple email in the browser.
     *
     * @param user
     * 			- the user requesting a preview
     * @return serialised HTML
     * @throws SegueDatabaseException
     * 			- on a database error
     * @throws ContentManagerException
     * 			- on a content error
     * @throws ResourceNotFoundException
     * 			- when the HTML template cannot be found
     * @throws IllegalArgumentException
     * 			- when the HTML template cannot be completed
     */
    public String getHTMLTemplatePreview(final EmailTemplateDTO emailTemplateDTO, final RegisteredUserDTO user)
		    		throws SegueDatabaseException, ContentManagerException, ResourceNotFoundException, 
		    		IllegalArgumentException {    	
        Validate.notNull(emailTemplateDTO);
    	Validate.notNull(user);
        
        ContentDTO htmlTemplate = getContentDTO("email-template-html");

        Properties p = new Properties();
        p.put("givenname", user.getGivenName() == null ? "" : user.getGivenName());
        p.put("familyname", user.getFamilyName() == null ? "" : user.getFamilyName());
        p.put("email", user.getEmail());
        p.put("sig", SIGNATURE);
        
        String compltedHTMLTemplate = completeTemplateWithProperties(emailTemplateDTO.getHtmlContent(), p);

        Properties htmlTemplateProperties = new Properties();
        htmlTemplateProperties.put("content", compltedHTMLTemplate);
        htmlTemplateProperties.put("email", user.getEmail());

        return completeTemplateWithProperties(htmlTemplate.getValue(), htmlTemplateProperties);
        
    }
    
    /**
     * This method allows the front end to preview simple email in the browser.
     *
     * @param user
     * 			- the user requesting a preview
     * @return serialised HTML
     * @throws SegueDatabaseException
     * 			- on a database error
     * @throws ContentManagerException
     * 			- on a content error
     * @throws ResourceNotFoundException
     * 			- when the HTML template cannot be found
     * @throws IllegalArgumentException
     * 			- when the HTML template cannot be completed
     */
    public String getPlainTextTemplatePreview(final EmailTemplateDTO emailTemplateDTO, final RegisteredUserDTO user)
		    		throws SegueDatabaseException, ContentManagerException, ResourceNotFoundException, 
		    		IllegalArgumentException {    	
        Validate.notNull(emailTemplateDTO);
    	Validate.notNull(user);
        
        ContentDTO plainTextTemplate = getContentDTO("email-template-ascii");

        Properties p = new Properties();
        p.put("givenname", user.getGivenName() == null ? "" : user.getGivenName());
        p.put("familyname", user.getFamilyName() == null ? "" : user.getFamilyName());
        p.put("email", user.getEmail());
        p.put("sig", SIGNATURE);
        String plainTextMessage = completeTemplateWithProperties(emailTemplateDTO.getPlainTextContent(), p);
        
        Properties plainTextTemplateProperties = new Properties();
        plainTextTemplateProperties.put("content", plainTextMessage);
        plainTextTemplateProperties.put("email", user.getEmail());

        return completeTemplateWithProperties(plainTextTemplate.getValue(), plainTextTemplateProperties);
        
    }

    /**
     * Method to take a random (potentially nested map) and flatten it into something where values can be easily extracted
     * for email templates.
     *
     * Nested fields are represented with the dot operator.
     *
     * @param inputMap
     * @param outputMap
     * @param keyPrefix
     * @return a flattend map for use in email template replacement.
     */
    public Map<String, String> flattenTokenMap(final Map <String, Object> inputMap, final Map <String, String> outputMap, String keyPrefix) {
        if (null == keyPrefix) {
            keyPrefix = "";
        }

        for(Map.Entry<String, Object> mapEntry : inputMap.entrySet()){
            String valueToStore = "";

            if (mapEntry.getValue() == null) {
                valueToStore = "";
            } else if (mapEntry.getValue() instanceof String) {
                valueToStore = (String) mapEntry.getValue();
            } else if (mapEntry.getValue() instanceof Date) {
                valueToStore = FULL_DATE_FORMAT.format((Date) mapEntry.getValue());
            } else if (mapEntry.getValue() instanceof Number || mapEntry.getValue() instanceof Boolean) {
                valueToStore = mapEntry.getValue().toString();
            } else if (mapEntry.getValue() instanceof Enum){
                valueToStore = ((Enum) mapEntry.getValue()).name();
            } else if (mapEntry.getValue() instanceof Collection) {
                valueToStore = (String)
                        ((Collection) mapEntry.getValue())
                                .stream().map(Object::toString)
                                .collect(Collectors.joining(", "));
            } else if (mapEntry.getValue() instanceof Map) {
                this.flattenTokenMap((Map) mapEntry.getValue(), outputMap, keyPrefix + mapEntry.getKey() + ".");
            } else if (mapEntry.getValue() instanceof ContentDTO) {
                ObjectMapper om = new ObjectMapper();
                this.flattenTokenMap(om.convertValue(mapEntry.getValue(), HashMap.class), outputMap, keyPrefix + mapEntry.getKey() + ".");
            } else if (mapEntry.getValue() instanceof ExternalReference) {
                ExternalReference er = (ExternalReference) mapEntry.getValue();
                StringBuilder sb = new StringBuilder();
                sb.append(String.format("<a href='%s'>%s</a>", er.getTitle(), er.getUrl()));
                sb.append("\n");
                valueToStore = sb.toString();
            } else {
                throw new IllegalArgumentException(
                        String.format("Unable to convert key (%s) value (%s) to string",
                                mapEntry.getKey(), mapEntry.getValue()));
            }

            outputMap.put(keyPrefix + mapEntry.getKey(), valueToStore);
        }
        return outputMap;
    }

    private String completeTemplateWithProperties(final String content, final Properties templateProperties)
            throws IllegalArgumentException {
        return completeTemplateWithProperties(content, templateProperties, false);
    }

    /**
     * Method to parse and replace template elements with the form {{TAG}}.
     *
     * @param templateProperties
     *            list of properties from which we can fill in the template
     * @return template with completed fields
     */
    private String completeTemplateWithProperties(final String content, final Properties templateProperties, final boolean html) {

        // ArrayList<ContentBaseDTO> children = (ArrayList<ContentBaseDTO>) content.getChildren();
        // if (!(children.size() == 1 && children.get(0) instanceof ContentDTO)) {
        // throw new IllegalArgumentException(
        // "Content object does not contain child with which to complete template properties!");
        // }
        //
        // String template = ((ContentDTO) children.get(0)).getValue();

        String template = content;

        Pattern p = Pattern.compile("\\{\\{[A-Za-z0-9.]+\\}\\}");
        Matcher m = p.matcher(template);
        int offset = 0;
        Set<String> unknownTags = Sets.newHashSet();

        while (m.find()) {
            if (m.start() + offset >= 0 && m.end() + offset <= template.length()) {
                String tag = template.substring(m.start() + offset, m.end() + offset);

                if (tag.length() <= MINIMUM_TAG_LENGTH) {
                    log.info("Skipped email template tag with no contents: " + tag);
                    break;
                }

                String strippedTag = tag.substring(2, tag.length() - 2);

                // Check all properties required in the page are in the properties list
                if (html && templateProperties.containsKey(strippedTag + "_HTML")) {
                    String start = template.substring(0, m.start() + offset);
                    String end = template.substring(m.end() + offset, template.length());

                    template = start;
                    if (templateProperties.getProperty(strippedTag + "_HTML") != null) {
                        template += templateProperties.getProperty(strippedTag + "_HTML");
                    }
                    template += end;

                    offset += templateProperties.getProperty(strippedTag + "_HTML").length() - tag.length();
                }
                else if (templateProperties.containsKey(strippedTag)) {
                    String start = template.substring(0, m.start() + offset);
                    String end = template.substring(m.end() + offset, template.length());

                    template = start;
                    if (templateProperties.getProperty(strippedTag) != null) {
                        template += templateProperties.getProperty(strippedTag);
                    }
                    template += end;

                    offset += templateProperties.getProperty(strippedTag).length() - tag.length();
                } else {
                    unknownTags.add(tag);
                }
            }
        }

        if (unknownTags.size() != 0) {
            log.error("Email template contains tags that were not resolved! - " + unknownTags);
            throw new IllegalArgumentException("Email template contains tag that was not provided! - " + unknownTags);
        }

        return template;
    }
    
    /**
     * This method loads the HTML and plain text templates and returns the resulting EmailCommunicationMessage. 
     * 
     * @param userId
     * 		- (nullable) the id of the user the email should be sent to
     * @param userEmail
     * 		- the email of the user 
     * @param emailType
     *      - the type of e-mail being created
     * @return
     * 		- a multi-part EmailCommunicationMessage
     * @throws ContentManagerException
     * 		- if there has been an error accessing content
     * @throws ResourceNotFoundException 
     * 		- if the resource has not been found
     * 	
     */
    private EmailCommunicationMessage constructMultiPartEmail(@Nullable final Long userId, final String userEmail, 
            EmailTemplateDTO emailContent, Properties contentProperties, final EmailType emailType)
					throws ContentManagerException, ResourceNotFoundException {
    	Validate.notNull(userEmail);
    	Validate.notEmpty(userEmail);
    	
        String plainTextContent = completeTemplateWithProperties(emailContent.getPlainTextContent(), contentProperties);
        String HTMLContent = completeTemplateWithProperties(emailContent.getHtmlContent(), contentProperties, true);

        String replyToAddress = emailContent.getReplyToEmailAddress();
        String replyToName = emailContent.getReplyToName();
        if (replyToAddress == null || replyToAddress.isEmpty()) {
            replyToAddress = globalProperties.getProperty(Constants.REPLY_TO_ADDRESS);
            replyToName = globalProperties.getProperty(Constants.MAIL_NAME);
        }

        ContentDTO htmlTemplate = getContentDTO("email-template-html");
        ContentDTO plainTextTemplate = getContentDTO("email-template-ascii");

        Properties htmlTemplateProperties = new Properties();
        htmlTemplateProperties.put("content", HTMLContent);
        htmlTemplateProperties.put("email", userEmail);

        String htmlMessage = completeTemplateWithProperties(htmlTemplate.getValue(), htmlTemplateProperties, true);

        Properties plainTextTemplateProperties = new Properties();
        plainTextTemplateProperties.put("content", plainTextContent);
        plainTextTemplateProperties.put("email", userEmail);

        String plainTextMessage = completeTemplateWithProperties(plainTextTemplate.getValue(),
                plainTextTemplateProperties);

        return new EmailCommunicationMessage(userId, userEmail, emailContent.getSubject(),
                plainTextMessage,
                htmlMessage, emailType, replyToAddress, replyToName);

    }

    /**
     * Returns the SegueDTO we will use as an email template.
     * 
     * @param id
     *            - the content id of the email template required
     * @return - the SegueDTO content object
     * @throws ContentManagerException
     *             - error if there is a problem accessing content
     * @throws ResourceNotFoundException
     *             - error if the content is not of the right type
     */
    private ContentDTO getContentDTO(final String id)
            throws ContentManagerException, ResourceNotFoundException {
    	
        ContentDTO c = this.contentManager.getContentById(
                this.contentManager.getCurrentContentSHA(), id);

        if (null == c) {
            throw new ResourceNotFoundException(String.format("E-mail template %s does not exist!", id));
        }

        return c;
    }

    /**
     * Returns the EmailTemplateDTO we will use as an email template.
     * 
     * @param id
     *            - the content id of the email template required
     * @return - the SegueDTO content object
     * @throws ContentManagerException
     *             - error if there is a problem accessing content
     * @throws ResourceNotFoundException
     *             - error if the content is not of the right type
     */
    public EmailTemplateDTO getEmailTemplateDTO(final String id) throws ContentManagerException,
            ResourceNotFoundException {

        ContentDTO c = this.contentManager.getContentById(
                this.contentManager.getCurrentContentSHA(), id);

        if (null == c) {
            throw new ResourceNotFoundException(String.format("E-mail template %s does not exist!", id));
        }

        EmailTemplateDTO emailTemplateDTO = null;

        if (c instanceof EmailTemplateDTO) {
            emailTemplateDTO = (EmailTemplateDTO) c;
        } else {
            throw new ContentManagerException("Content is of incorrect type:" + c.getType());
        }

        return emailTemplateDTO;
    }
}
