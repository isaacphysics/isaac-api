package uk.ac.cam.cl.dtg.segue.comm;

import static uk.ac.cam.cl.dtg.segue.api.Constants.HOST_NAME;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import org.elasticsearch.common.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.isaac.api.managers.GameManager;
import uk.ac.cam.cl.dtg.isaac.dto.AssignmentDTO;
import uk.ac.cam.cl.dtg.isaac.dto.GameboardDTO;
import uk.ac.cam.cl.dtg.segue.api.managers.ContentVersionController;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserException;
import uk.ac.cam.cl.dtg.segue.dao.ILogManager;
import uk.ac.cam.cl.dtg.segue.dao.ResourceNotFoundException;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dos.AbstractEmailPreferenceManager;
import uk.ac.cam.cl.dtg.segue.dos.IEmailPreference;
import uk.ac.cam.cl.dtg.segue.dos.users.EmailVerificationStatus;
import uk.ac.cam.cl.dtg.segue.dos.users.RegisteredUser;
import uk.ac.cam.cl.dtg.segue.dto.UserGroupDTO;
import uk.ac.cam.cl.dtg.segue.dto.content.ContentBaseDTO;
import uk.ac.cam.cl.dtg.segue.dto.content.ContentDTO;
import uk.ac.cam.cl.dtg.segue.dto.content.SeguePageDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

import com.google.api.client.util.Lists;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

/**
 * @author Alistair Stead
 *
 */
public class EmailManager extends AbstractCommunicationQueue<EmailCommunicationMessage> {
	private final AbstractEmailPreferenceManager emailPreferenceManager;
    private final PropertiesLoader globalProperties;
    private final ContentVersionController contentVersionController;
    private final ILogManager logManager;
    
    private static final Logger log = LoggerFactory.getLogger(EmailManager.class);
    private static final String SIGNATURE = "Isaac Physics Project";
    private static final int MINIMUM_TAG_LENGTH = 4;
    private static final int TRUNCATED_TOKEN_LENGTH = 5;

    /**
     * @param communicator
     *            class we'll use to send the actual email.
     * @param emailPreferenceManager
     *            email preference manager used to check if users want email.
     * @param globalProperties
     *            global properties used to get host name
     * @param contentVersionController
     *            content for email templates
     * @param logManager
     *            so we can log e-mail events.
     */
    @Inject
    public EmailManager(final EmailCommunicator communicator, final AbstractEmailPreferenceManager 
		    		emailPreferenceManager, final PropertiesLoader globalProperties, 
		    		final ContentVersionController contentVersionController, final ILogManager logManager) {
        super(communicator, logManager);
        this.emailPreferenceManager = emailPreferenceManager;
        this.globalProperties = globalProperties;
        this.contentVersionController = contentVersionController;
        this.logManager = logManager;
    }

    

    /**
     * @param userDTO
     *            - user object used to complete template
     * @param resetToken
     * 			  - the reset token
     * @throws ContentManagerException
     *             - some content may not have been accessible
     * @throws SegueDatabaseException
     *             - the content was of incorrect type
     * @throws NoUserException 
     * 			   - when no matching user is found
     */
    public void sendPasswordReset(final RegisteredUserDTO userDTO, final String resetToken) 
    				throws ContentManagerException, SegueDatabaseException, NoUserException {
    	Validate.notNull(userDTO);
    	SeguePageDTO segueContent = getSegueDTOEmailTemplate("email-template-password-reset");
        
        String hostName = globalProperties.getProperty(HOST_NAME);
        String verificationURL = String.format("https://%s/resetpassword/%s", hostName, resetToken);

        Properties p = new Properties();
        p.put("givenname", userDTO.getGivenName() == null ? "" : userDTO.getGivenName());
        p.put("email", userDTO.getEmail());
        p.put("resetURL", verificationURL);
        p.put("sig", SIGNATURE);
        
        String content = completeTemplateWithProperties(segueContent, p);
        
        EmailCommunicationMessage e = constructMultiPartEmail(userDTO.getId(), userDTO.getEmail(), 
        				content, segueContent.getTitle(), globalProperties.getProperty(Constants.REPLY_TO_ADDRESS),
                        EmailType.SYSTEM);

        this.filterByPreferencesAndAddToQueue(userDTO, e);
    }

    /**
     * Sends email registration confirmation using email registration template. Assumes that a verification code has
     * been successfully generated.
     * 
     * @param userDTO
     *            - user object used to complete template
     * @param emailVerificationToken
     * 			  - the email verification token
     * @throws ContentManagerException
     *             - some content may not have been accessible
     * @throws SegueDatabaseException
     *             - the content was of incorrect type
     * @throws NoUserException 
     * 			   - when no matching user is found 
     */
    public void sendRegistrationConfirmation(final RegisteredUserDTO userDTO, final String emailVerificationToken) 
    				throws ContentManagerException, SegueDatabaseException, NoUserException {
    	Validate.notNull(userDTO);
        SeguePageDTO segueContent = getSegueDTOEmailTemplate("email-template-registration-confirmation");
        
        String verificationURL = String.format("https://%s/verifyemail?userid=%s&email=%s&token=%s", 
                globalProperties.getProperty(HOST_NAME), 
                userDTO.getLegacyDbId(),
                userDTO.getEmail(),
                emailVerificationToken.substring(0, TRUNCATED_TOKEN_LENGTH));

        Properties p = new Properties();
        p.put("givenname", userDTO.getGivenName() == null ? "" : userDTO.getGivenName());
        p.put("email", userDTO.getEmail());
        p.put("verificationURL", verificationURL);
        p.put("sig", SIGNATURE);
        String content = completeTemplateWithProperties(segueContent, p);
        
        EmailCommunicationMessage e = constructMultiPartEmail(userDTO.getId(), userDTO.getEmail(), 
        				content, segueContent.getTitle(), globalProperties.getProperty(Constants.REPLY_TO_ADDRESS),
                        EmailType.SYSTEM);
        
        this.filterByPreferencesAndAddToQueue(userDTO, e);
    }
    
	/**
     * Sends email registration confirmation using email registration template. 
     * 
     * @param userDTO
     *            - user object used to complete template
     * @throws ContentManagerException
     *             - some content may not have been accessible
     * @throws SegueDatabaseException
     *             - the content was of incorrect type
     * @throws NoUserException 
     * 			   - when no matching user is found 
	 */
	public void sendFederatedRegistrationConfirmation(final RegisteredUserDTO userDTO) 
					throws ContentManagerException, SegueDatabaseException {
    	Validate.notNull(userDTO);
        SeguePageDTO segueContent = getSegueDTOEmailTemplate("email-template-registration-confirmation-federated");
        
        Properties p = new Properties();
        p.put("givenname", userDTO.getGivenName() == null ? "" : userDTO.getGivenName());
        p.put("email", userDTO.getEmail());
        p.put("sig", SIGNATURE);
        String content = completeTemplateWithProperties(segueContent, p);
        
        EmailCommunicationMessage e = constructMultiPartEmail(userDTO.getId(), userDTO.getEmail(), 
        				content, segueContent.getTitle(), globalProperties.getProperty(Constants.REPLY_TO_ADDRESS),
                        EmailType.SYSTEM);
        
        this.filterByPreferencesAndAddToQueue(userDTO, e);
	}

    /**
     * Sends email verification using email verification template. Assumes that a verification code has been
     * successfully generated.
     * 
     * @param userDTO
     *            - user object used to complete template
     * @param emailVerificationToken
     *            - the user's verification token
     * @throws ContentManagerException
     *             - some content may not have been accessible
     * @throws SegueDatabaseException
     *             - the content was of incorrect type
     * @throws NoUserException 
     * 			   - when no matching user is found
     */
    public void sendEmailVerification(final RegisteredUserDTO userDTO, final String emailVerificationToken) 
    					throws ContentManagerException, SegueDatabaseException, NoUserException {
    	Validate.notNull(userDTO);
        SeguePageDTO segueContent = getSegueDTOEmailTemplate("email-template-email-verification");


        String verificationURL = String.format("https://%s/verifyemail?userid=%s&email=%s&token=%s", 
                globalProperties.getProperty(HOST_NAME),
                userDTO.getLegacyDbId(),
                userDTO.getEmail(),
                emailVerificationToken.substring(0, TRUNCATED_TOKEN_LENGTH));

        Properties p = new Properties();
        p.put("givenname", userDTO.getGivenName() == null ? "" : userDTO.getGivenName());
        p.put("email", userDTO.getEmail());
        p.put("verificationURL", verificationURL);
        p.put("sig", SIGNATURE);
        String content = completeTemplateWithProperties(segueContent, p);
        
        EmailCommunicationMessage e = constructMultiPartEmail(userDTO.getId(), userDTO.getEmail(), 
        				content, segueContent.getTitle(), globalProperties.getProperty(Constants.REPLY_TO_ADDRESS),
                        EmailType.SYSTEM);
        this.filterByPreferencesAndAddToQueue(userDTO, e);
    }
    
    /**
     * Sends notification for email change using email_verification_change template. 
     * 
     * @param userDTO
     *            - user object used to complete template
     * @param newUser
     *            - new user object used to complete template          
     * @throws ContentManagerException
     *             - some content may not have been accessible
     * @throws SegueDatabaseException
     *             - the content was of incorrect type
     * @throws NoUserException 
     * 				- when no matching user was found
     */
    public void sendEmailVerificationChange(final RegisteredUserDTO userDTO, final RegisteredUser newUser) 
            throws ContentManagerException, SegueDatabaseException, NoUserException {
    	Validate.notNull(userDTO);
    	Validate.notNull(newUser);
        SeguePageDTO segueContent = getSegueDTOEmailTemplate("email-verification-change");
        
        Properties p = new Properties();
        p.put("givenname", userDTO.getGivenName() == null ? "" : userDTO.getGivenName());
        p.put("requestedemail", newUser.getEmail());
        p.put("sig", SIGNATURE);
        String content = completeTemplateWithProperties(segueContent, p);
        
        EmailCommunicationMessage e = constructMultiPartEmail(userDTO.getId(), userDTO.getEmail(), 
        				content, segueContent.getTitle(), globalProperties.getProperty(Constants.REPLY_TO_ADDRESS),
                        EmailType.SYSTEM);
        this.filterByPreferencesAndAddToQueue(userDTO, e);
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
     */
    public void sendGroupAssignment(final List<RegisteredUserDTO> users, 
		    		final GameboardDTO gameboard)
		            throws ContentManagerException, SegueDatabaseException {
    	Validate.notNull(users);

        SeguePageDTO segueContent = getSegueDTOEmailTemplate("email-template-group-assignment");
   
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
            String content = completeTemplateWithProperties(segueContent, p);



            EmailCommunicationMessage e = constructMultiPartEmail(userDTO.getId(), userDTO.getEmail(), 
            				content, segueContent.getTitle(), globalProperties.getProperty(Constants.REPLY_TO_ADDRESS),
                            EmailType.ASSIGNMENTS);
            this.filterByPreferencesAndAddToQueue(userDTO, e);
        }
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
     */
    public void sendGroupWelcome(final RegisteredUserDTO userDTO, final UserGroupDTO userGroup,
					    		final RegisteredUserDTO groupOwner,
								final List<AssignmentDTO> existingAssignments,
					            final GameManager gameManager)
		            			throws ContentManagerException, SegueDatabaseException {
    	Validate.notNull(userDTO);

		SeguePageDTO segueContent = getSegueDTOEmailTemplate("email-template-group-welcome");

        String groupOwnerName = "Unknown";
        if (groupOwner != null && groupOwner.getGivenName() != null && groupOwner.getFamilyName() != null) {
            groupOwnerName = groupOwner.getGivenName() + " " + groupOwner.getFamilyName();
        }
        
        Collections.sort(existingAssignments, new Comparator<AssignmentDTO>() {

            @Override
            public int compare(final AssignmentDTO o1, final AssignmentDTO o2) {
                return o1.getCreationDate().compareTo(o2.getCreationDate());
            }
            
        });
        
        StringBuilder sb = new StringBuilder();
        if (existingAssignments != null && existingAssignments.size() > 0) {
            sb.append("Your teacher has assigned the following assignments:\n");
            for (int i = 0; i < existingAssignments.size(); i++) {
                DateFormat df = new SimpleDateFormat("dd/MM/yy HH:mm");
                GameboardDTO gameboard = gameManager.getGameboard(existingAssignments.get(i).getGameboardId());

                String gameboardName = existingAssignments.get(i).getGameboardId();
                if (gameboard != null) {
                	gameboardName = gameboard.getTitle();
                }
                
				String gameboardUrl = String.format("https://%s/#%s",
								globalProperties.getProperty(HOST_NAME),
								existingAssignments.get(i).getGameboardId());

				sb.append(String.format(
								"%d. <a href='%s'>%s</a> (set on %s)\n",
								i + 1,
								gameboardUrl,
		                        gameboardName,
		                        df.format(existingAssignments.get(i).getCreationDate())));
            }
        } else if (existingAssignments != null && existingAssignments.size() == 0) {
            sb.append("No assignments have been set yet.");
        }

        String accountURL = String.format("https://%s/account", globalProperties.getProperty(HOST_NAME));
        Properties p = new Properties();
        p.put("givenname", userDTO.getGivenName() == null ? "" : userDTO.getGivenName());
        p.put("teacherName", groupOwnerName == null);
        p.put("assignmentsInfo", sb.toString());
        p.put("accountURL", accountURL);
        p.put("sig", SIGNATURE);
        String content = completeTemplateWithProperties(segueContent, p);

        

        EmailCommunicationMessage e = constructMultiPartEmail(userDTO.getId(), userDTO.getEmail(), 
        				content, segueContent.getTitle(), globalProperties.getProperty(Constants.REPLY_TO_ADDRESS),
                        EmailType.SYSTEM);
        this.filterByPreferencesAndAddToQueue(userDTO, e);

    }

    /**
     * Sends email verification using email verification template. Assumes that a verification code has been
     * successfully generated.
     * 
     * @param userDTO
     *            - user object used to complete template
     * @param providerString
     *            - the provider
     * @param providerWord
     *            - the provider
     * @throws ContentManagerException
     *             - some content may not have been accessible
     * @throws SegueDatabaseException
     *             - the content was of incorrect type
     * @throws NoUserException 
     * 				- if no user DTO could be found
     */
    public void sendFederatedPasswordReset(final RegisteredUserDTO userDTO, final String providerString,
            final String providerWord) throws ContentManagerException, SegueDatabaseException, NoUserException {
    	Validate.notNull(userDTO);
        Validate.notNull(providerString);
        Validate.notNull(providerWord);

        SeguePageDTO segueContent = getSegueDTOEmailTemplate("email-template-federated-password-reset");

        Properties contentProperties = new Properties();
        contentProperties.put("givenname", userDTO.getGivenName() == null ? "" : userDTO.getGivenName());
        contentProperties.put("providerString", providerString);
        contentProperties.put("providerWord", providerWord);
        contentProperties.put("sig", SIGNATURE);

        String content = completeTemplateWithProperties(segueContent, contentProperties);      

        EmailCommunicationMessage e = constructMultiPartEmail(userDTO.getId(), userDTO.getEmail(), 
        				content, segueContent.getTitle(), globalProperties.getProperty(Constants.REPLY_TO_ADDRESS),
                        EmailType.SYSTEM);
        this.filterByPreferencesAndAddToQueue(userDTO, e);
        
    }
    

    /**
     * @param subject
     *            - the subject of the email
     * @param contactFormMessage
     *            - the message from the contact form
     * @param recipientEmailAddress
     *            - the email address it is being sent to
     * @param replyToAddress
     *            - the email address we want to reply to
     * @throws ContentManagerException
     *             - if some content is not found
     * @throws SegueDatabaseException
     *             - if the database cannot be accessed
     */
    public void sendContactUsFormEmail(final String subject, final String contactFormMessage,
            final String recipientEmailAddress, final String replyToAddress) throws ContentManagerException,
            SegueDatabaseException {

        EmailCommunicationMessage e = constructMultiPartEmail(null, recipientEmailAddress, contactFormMessage, subject,
                replyToAddress, EmailType.SYSTEM);
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

        SeguePageDTO segueContent = getSegueDTOEmailTemplate(contentObjectId);

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
            String content = completeTemplateWithProperties(segueContent, p);

            EmailCommunicationMessage e = constructMultiPartEmail(user.getId(), user.getEmail(), content,
                    segueContent.getTitle(), globalProperties.getProperty(Constants.REPLY_TO_ADDRESS), emailType);

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
                .put("contentVersionId", this.contentVersionController.getLiveVersion()).build();
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
     * @param segueContent
     * 			- the email template 
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
    public String getHTMLTemplatePreview(final SeguePageDTO segueContent, final RegisteredUserDTO user) 
		    		throws SegueDatabaseException, ContentManagerException, ResourceNotFoundException, 
		    		IllegalArgumentException {    	
        Validate.notNull(segueContent);
    	Validate.notNull(user);
        
        SeguePageDTO htmlTemplate = getSegueDTOEmailTemplate("email-template-html");

        Properties p = new Properties();
        p.put("givenname", user.getGivenName() == null ? "" : user.getGivenName());
        p.put("familyname", user.getFamilyName() == null ? "" : user.getFamilyName());
        p.put("email", user.getEmail());
        p.put("sig", SIGNATURE);
        String plainTextMessage = completeTemplateWithProperties(segueContent, p);
        
        Properties htmlTemplateProperties = new Properties();
        htmlTemplateProperties.put("content", plainTextMessage.replace("\n", "<br>"));
        htmlTemplateProperties.put("email", user.getEmail());

        return completeTemplateWithProperties(htmlTemplate, htmlTemplateProperties);
        
    }
    
    /**
     * This method allows the front end to preview simple email in the browser.
     * 
     * @param segueContent
     * 			- the email template 
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
    public String getPlainTextTemplatePreview(final SeguePageDTO segueContent, final RegisteredUserDTO user) 
		    		throws SegueDatabaseException, ContentManagerException, ResourceNotFoundException, 
		    		IllegalArgumentException {    	
        Validate.notNull(segueContent);
    	Validate.notNull(user);
        
        SeguePageDTO plainTextTemplate = getSegueDTOEmailTemplate("email-template-ascii");

        Properties p = new Properties();
        p.put("givenname", user.getGivenName() == null ? "" : user.getGivenName());
        p.put("familyname", user.getFamilyName() == null ? "" : user.getFamilyName());
        p.put("email", user.getEmail());
        p.put("sig", SIGNATURE);
        String plainTextMessage = completeTemplateWithProperties(segueContent, p);
        
        Properties plainTextTemplateProperties = new Properties();
        plainTextTemplateProperties.put("content", plainTextMessage);
        plainTextTemplateProperties.put("email", user.getEmail());

        return completeTemplateWithProperties(plainTextTemplate, plainTextTemplateProperties);
        
    }
   
    
    /**
     * Method to parse and replace template elements with the form {{TAG}}.
     * 
     * @param page
     *            SeguePage that contains SeguePage child with template value
     * @param templateProperties
     *            list of properties from which we can fill in the template
     * @return template with completed fields
     * @throws IllegalArgumentException
     *             - exception when the provided page object is incorrect
     */
    private String completeTemplateWithProperties(final SeguePageDTO page, final Properties templateProperties)
            throws IllegalArgumentException {

        ArrayList<ContentBaseDTO> children = (ArrayList<ContentBaseDTO>) page.getChildren();
        if (!(children.size() == 1 && children.get(0) instanceof ContentDTO)) {
            throw new IllegalArgumentException("SeguePage does not contain child for email template!");
        }

        String template = ((ContentDTO) children.get(0)).getValue();

        Pattern p = Pattern.compile("\\{\\{[A-Za-z]+\\}\\}");
        Matcher m = p.matcher(template);
        int offset = 0;

        while (m.find()) {
            if (template != null && m.start() + offset >= 0 && m.end() + offset <= template.length()) {
                String tag = template.substring(m.start() + offset, m.end() + offset);

                if (tag.length() <= MINIMUM_TAG_LENGTH) {
                    log.info("Skipped email template tag with no contents: " + tag);
                    break;
                }

                String strippedTag = tag.substring(2, tag.length() - 2);

                // Check all properties required in the page are in the properties list
                if (templateProperties.containsKey(strippedTag)) {
                    String start = template.substring(0, m.start() + offset);
                    String end = template.substring(m.end() + offset, template.length());

                    template = start;
                    if (templateProperties.getProperty(strippedTag) != null) {
                        template += templateProperties.getProperty(strippedTag);
                    }
                    template += end;

                    offset += templateProperties.getProperty(strippedTag).length() - tag.length();
                } else {
                    throw new IllegalArgumentException("Email template contains tag that was not provided! - " + tag);
                }
            }
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
     * @param content
     * 		- the text in the email
     * @param subject
     * 		- the subject of the email
     * @param replyToAddress
     * 		- the reply-to address of the email - needed for contact form
     * @return
     * 		- a multi-part EmailCommunicationMessage
     * @throws ContentManagerException
     * 		- if there has been an error accessing content
     * @throws ResourceNotFoundException 
     * 		- if the resource has not been found
     * 	
     */
    private EmailCommunicationMessage constructMultiPartEmail(@Nullable final Long userId, final String userEmail, 
    				final String content, final String subject, final String replyToAddress, final EmailType emailType)
					throws ContentManagerException, ResourceNotFoundException {
    	Validate.notNull(userEmail);
    	Validate.notEmpty(userEmail);
    	
        SeguePageDTO htmlTemplate = getSegueDTOEmailTemplate("email-template-html");
        SeguePageDTO plainTextTemplate = getSegueDTOEmailTemplate("email-template-ascii");
        
        // Remove bad things for HTML
        String htmlContent = content;
        htmlContent = htmlContent.replace("\n", "<br>");
        
        Properties htmlTemplateProperties = new Properties();
        htmlTemplateProperties.put("content", htmlContent); 
        htmlTemplateProperties.put("email", userEmail);

        String htmlMessage = completeTemplateWithProperties(htmlTemplate, htmlTemplateProperties);
        
        // Remove bad things for plain text
        String plainTextContent = content;
        //plainTextContent = plainTextContent.replace("<span>", ""); //TODO finalise this with James/Steve
        
        Properties plainTextTemplateProperties = new Properties();
        plainTextTemplateProperties.put("content", plainTextContent); 
        plainTextTemplateProperties.put("email", userEmail);

        String plainTextMessage = completeTemplateWithProperties(plainTextTemplate, plainTextTemplateProperties);

        EmailCommunicationMessage e = new EmailCommunicationMessage(userId, userEmail, subject, plainTextMessage,
                htmlMessage, emailType, replyToAddress);

        return e;
    }
    
    /**
     * Returns the SegueDTO we will use as an email template.
     * 
     * @param id  
     *          - the content id of the email template required
     * @return  - the SegueDTO content object
     * @throws ContentManagerException 
     *          - error if there is a problem accessing content
     * @throws ResourceNotFoundException 
     *          - error if the content is not of the right type
     */
    private SeguePageDTO getSegueDTOEmailTemplate(final String id) 
            throws ContentManagerException, ResourceNotFoundException {
    	
        ContentDTO c = contentVersionController.getContentManager().getContentById(
                contentVersionController.getLiveVersion(), id);

        if (null == c) {
            throw new ResourceNotFoundException(String.format("E-mail template %s does not exist!", id));
        }
        
        SeguePageDTO segueContentDTO = null;

        if (c instanceof SeguePageDTO) {
            segueContentDTO = (SeguePageDTO) c;
        } else {
            throw new ContentManagerException("Content is of incorrect type:" + c.getType());
        }
        
        return segueContentDTO;
    }

	
}
