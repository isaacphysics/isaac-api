package uk.ac.cam.cl.dtg.segue.comm;

import static uk.ac.cam.cl.dtg.segue.api.Constants.HOST_NAME;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.isaac.api.managers.GameManager;
import uk.ac.cam.cl.dtg.isaac.dto.AssignmentDTO;
import uk.ac.cam.cl.dtg.isaac.dto.GameboardDTO;
import uk.ac.cam.cl.dtg.segue.api.managers.ContentVersionController;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dos.users.RegisteredUser;
import uk.ac.cam.cl.dtg.segue.dto.UserGroupDTO;
import uk.ac.cam.cl.dtg.segue.dto.content.ContentBaseDTO;
import uk.ac.cam.cl.dtg.segue.dto.content.ContentDTO;
import uk.ac.cam.cl.dtg.segue.dto.content.SeguePageDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

import com.google.inject.Inject;

/**
 * @author Alistair Stead
 *
 */
public class EmailManager extends AbstractCommunicationQueue<EmailCommunicationMessage> {
    private final PropertiesLoader globalProperties;
    private final ContentVersionController contentVersionController;
    
    private static final Logger log = LoggerFactory.getLogger(EmailManager.class);
    private static final String SIGNATURE = "Isaac Physics Project";
    private static final int MINIMUM_TAG_LENGTH = 4;
    private static final int TRUNCATED_TOKEN_LENGTH = 5;

    /**
     * @param communicator
     *            class we'll use to send the actual email.
     * @param globalProperties
     *            global properties used to get host name
     * @param contentVersionController
     *            content for email templates
     */
    @Inject
    public EmailManager(final EmailCommunicator communicator, final PropertiesLoader globalProperties,
		            final ContentVersionController contentVersionController) {
        super(communicator);
        this.globalProperties = globalProperties;
        this.contentVersionController = contentVersionController;
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
                    template = start + templateProperties.getProperty(strippedTag) + end;
                    offset += templateProperties.getProperty(strippedTag).length() - tag.length();
                } else {
                    throw new IllegalArgumentException("Email template contains tag that was not provided! - " + tag);
                }
            }
        }

        return template;
    }

    /**
     * @param user
     *            - user object used to complete template
     * @throws ContentManagerException
     *             - some content may not have been accessible
     * @throws SegueDatabaseException
     *             - the content was of incorrect type
     */
    public void sendPasswordReset(final RegisteredUser user) throws ContentManagerException, SegueDatabaseException {
        SeguePageDTO segueContent = getSegueDTOEmailTemplate("email-template-password-reset");
        
        if (null == segueContent) {
            log.debug("Password reset message not sent as segue content was null!");
            return;
        }
        
        String hostName = globalProperties.getProperty(HOST_NAME);
        String verificationURL = String.format("https://%s/resetpassword/%s", hostName, user.getResetToken());

        // TODO turn these into constants
        Properties p = new Properties();
        p.put("givenname", user.getGivenName());
        p.put("email", user.getEmail());
        p.put("resetURL", verificationURL);
        p.put("sig", SIGNATURE);
        
        String plainTextMessage = completeTemplateWithProperties(segueContent, p);
        
        SeguePageDTO htmlTemplate = getSegueDTOEmailTemplate("email-template-html");
        
        String htmlMessage = null;
        
        if (null == htmlTemplate) {
            log.debug("HTML email template could not be found!");
        } else {
            Properties htmlTemplateProperties = new Properties();
            htmlTemplateProperties.put("content", plainTextMessage.replace("\n", "<br>"));
            htmlTemplateProperties.put("email", user.getEmail());
            
            htmlMessage = completeTemplateWithProperties(htmlTemplate, htmlTemplateProperties);
        }

        EmailCommunicationMessage e = new EmailCommunicationMessage(user.getEmail(), user.getGivenName(),
                segueContent.getTitle(), plainTextMessage, htmlMessage,  EmailType.SYSTEM,
                globalProperties.getProperty(Constants.REPLY_TO_ADDRESS));

        this.addToQueue(e);
    }

    /**
     * Sends email registration confirmation using email registration template. Assumes that a verification code has
     * been successfully generated.
     * 
     * @param user
     *            - user object used to complete template
     * @throws ContentManagerException
     *             - some content may not have been accessible
     * @throws SegueDatabaseException
     *             - the content was of incorrect type
     */
    public void sendRegistrationConfirmation(final RegisteredUser user) throws ContentManagerException,
            SegueDatabaseException {

        SeguePageDTO segueContent = getSegueDTOEmailTemplate("email-template-registration-confirmation");
        
        if (null == segueContent) {
            log.debug("Email registration confirmation email not sent as segue content was null!");
            return;
        }

        String verificationURL = String.format("https://%s/verifyemail?userid=%s&email=%s&token=%s", 
                globalProperties.getProperty(HOST_NAME), 
                user.getLegacyDbId(),
                user.getEmail(),
                user.getEmailVerificationToken().substring(0, TRUNCATED_TOKEN_LENGTH));

        Properties p = new Properties();
        p.put("givenname", user.getGivenName());
        p.put("email", user.getEmail());
        p.put("verificationURL", verificationURL);
        p.put("sig", SIGNATURE);
        String plainTextMessage = completeTemplateWithProperties(segueContent, p);
        
        SeguePageDTO htmlTemplate = getSegueDTOEmailTemplate("email-template-html");
        
        String htmlMessage = null;
        
        if (null == htmlTemplate) {
            log.debug("HTML email template could not be found!");
        } else {
            Properties htmlTemplateProperties = new Properties();
            htmlTemplateProperties.put("content", plainTextMessage.replace("\n", "<br>"));
            htmlTemplateProperties.put("email", user.getEmail());
            
            htmlMessage = completeTemplateWithProperties(htmlTemplate, htmlTemplateProperties);
        }

        EmailCommunicationMessage e = new EmailCommunicationMessage(user.getEmail(), user.getGivenName(),
                segueContent.getTitle(), plainTextMessage, EmailType.SYSTEM, htmlMessage);
        
        this.addToQueue(e);
    }

    /**
     * Sends email verification using email verification template. Assumes that a verification code has been
     * successfully generated.
     * 
     * @param user
     *            - user object used to complete template
     * @throws ContentManagerException
     *             - some content may not have been accessible
     * @throws SegueDatabaseException
     *             - the content was of incorrect type
     */
    public void sendEmailVerification(final RegisteredUser user) throws ContentManagerException, 
                                                                        SegueDatabaseException {

        SeguePageDTO segueContent = getSegueDTOEmailTemplate("email-template-email-verification");
        
        if (null == segueContent) {
            log.debug("Email verification message not sent as segue content was null!");
            return;
        }

        String verificationURL = String.format("https://%s/verifyemail?userid=%s&email=%s&token=%s", 
                globalProperties.getProperty(HOST_NAME),
                user.getLegacyDbId(),
                user.getEmail(),
                user.getEmailVerificationToken().substring(0, TRUNCATED_TOKEN_LENGTH));

        Properties p = new Properties();
        p.put("givenname", user.getGivenName());
        p.put("email", user.getEmail());
        p.put("verificationURL", verificationURL);
        p.put("sig", SIGNATURE);
        String plainTextMessage = completeTemplateWithProperties(segueContent, p);
        
        SeguePageDTO htmlTemplate = getSegueDTOEmailTemplate("email-template-html");
        
        String htmlMessage = null;
        
        if (null == htmlTemplate) {
            log.debug("HTML email template could not be found!");
        } else {
            Properties htmlTemplateProperties = new Properties();
            htmlTemplateProperties.put("content", plainTextMessage.replace("\n", "<br>"));
            htmlTemplateProperties.put("email", user.getEmail());
            
            htmlMessage = completeTemplateWithProperties(htmlTemplate, htmlTemplateProperties);
        }

        EmailCommunicationMessage e = new EmailCommunicationMessage(user.getEmail(), user.getGivenName(),
                segueContent.getTitle(), plainTextMessage, htmlMessage, EmailType.SYSTEM,
                globalProperties.getProperty(Constants.REPLY_TO_ADDRESS));
        this.addToQueue(e);
    }
    
    /**
     * Sends notification for email change using email_verification_change template. 
     * 
     * @param user
     *            - user object used to complete template
     * @param newUser
     *            - new user object used to complete template          
     * @throws ContentManagerException
     *             - some content may not have been accessible
     * @throws SegueDatabaseException
     *             - the content was of incorrect type
     */
    public void sendEmailVerificationChange(final RegisteredUser user, final RegisteredUser newUser) 
            throws ContentManagerException, 
                                                                                SegueDatabaseException {

        SeguePageDTO segueContent = getSegueDTOEmailTemplate("email-verification-change");
        
        if (null == segueContent) {
            log.debug("Email change message not sent as segue content was null!");
            return;
        }

        Properties p = new Properties();
        p.put("givenname", user.getGivenName());
        p.put("requestedemail", newUser.getEmail());
        p.put("sig", SIGNATURE);
        String plainTextMessage = completeTemplateWithProperties(segueContent, p);
        
        SeguePageDTO htmlTemplate = getSegueDTOEmailTemplate("email-template-html");
        
        String htmlMessage = null;
        
        if (null == htmlTemplate) {
            log.debug("HTML email template could not be found!");
        } else {
            Properties htmlTemplateProperties = new Properties();
            htmlTemplateProperties.put("content", plainTextMessage.replace("\n", "<br>"));
            htmlTemplateProperties.put("email", user.getEmail());

            htmlMessage = completeTemplateWithProperties(htmlTemplate, htmlTemplateProperties);
        }

        EmailCommunicationMessage e = new EmailCommunicationMessage(user.getEmail(), user.getGivenName(),
                segueContent.getTitle(), plainTextMessage, htmlMessage, EmailType.SYSTEM,
                globalProperties.getProperty(Constants.REPLY_TO_ADDRESS));
        this.addToQueue(e);
    }

    /**
     * Sends notification for groups being given an assignment.
     * @param userManager
     * 			  - the userManager
     * @param users
     *            - the group the gameboard is being assigned to
     * @param gameboard
     *            - gameboard that is being assigned to the users
     * @param assignmentOwner
     * 			  - the user that owns the assignment we're sending an email about      
     * @param userAssociationManager
     * 			  - the association manager that allows us to check whether the owner of 
     * 				the assignment has an association with the user      
     * @throws ContentManagerException
     *             - some content may not have been accessible
     * @throws SegueDatabaseException
     *             - the content was of incorrect type
     */
    public void sendGroupAssignment(final List<RegisteredUserDTO> users, 
		    		final GameboardDTO gameboard)
		            throws ContentManagerException, SegueDatabaseException {

        SeguePageDTO segueContent = getSegueDTOEmailTemplate("email-template-group-assignment");

        if (null == segueContent) {
            log.debug("Email change message not sent as segue content was null!");
            return;
        }

        SeguePageDTO htmlTemplate = getSegueDTOEmailTemplate("email-template-html");
        
		String gameboardName = gameboard.getId();
		if (gameboard.getTitle() != null) {
			gameboardName = gameboard.getTitle();
		}

        for (RegisteredUserDTO user : users) {
        	       	
            String gameboardURL = String.format("https://%s/#%s", globalProperties.getProperty(HOST_NAME),
                    gameboard.getId());
            String myAssignmentsURL = String.format("https://%s/assignments",
                    globalProperties.getProperty(HOST_NAME));
            Properties p = new Properties();
            p.put("givenname", user.getGivenName());
            p.put("gameboardURL", gameboardURL);
            p.put("gameboardName", gameboardName);
            p.put("myAssignmentsURL", myAssignmentsURL);
            p.put("sig", SIGNATURE);
            String plainTextMessage = completeTemplateWithProperties(segueContent, p);
            String htmlMessage = null;

            if (null == htmlTemplate) {
                log.debug("HTML email template could not be found!");
            } else {
                Properties htmlTemplateProperties = new Properties();
                htmlTemplateProperties.put("content", plainTextMessage.replace("\n", "<br>"));
                htmlTemplateProperties.put("email", user.getEmail());

                htmlMessage = completeTemplateWithProperties(htmlTemplate, htmlTemplateProperties);
            }

            EmailCommunicationMessage e = new EmailCommunicationMessage(user.getEmail(), user.getGivenName(),
                    segueContent.getTitle(), plainTextMessage, EmailType.ASSIGNMENTS, htmlMessage);
            this.addToQueue(e);
        }
    }

    /**
     * Sends notification for groups being given an assignment.
     * 
     * @param user
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
    public void sendGroupWelcome(final RegisteredUserDTO user, final UserGroupDTO userGroup,
					    		final RegisteredUserDTO groupOwner,
								final List<AssignmentDTO> existingAssignments,
					            final GameManager gameManager)
		            			throws ContentManagerException, SegueDatabaseException {

		SeguePageDTO segueContent = getSegueDTOEmailTemplate("email-template-group-welcome");

        if (null == segueContent) {
            log.debug("Email change message not sent as segue content was null!");
            return;
        }

		SeguePageDTO htmlTemplate = getSegueDTOEmailTemplate("email-template-html");

        String groupOwnerName = "Unknown";
        if (groupOwner != null) {
            groupOwnerName = groupOwner.getGivenName() + " " + groupOwner.getFamilyName();
        }
        
        existingAssignments.sort(new Comparator<AssignmentDTO>() {

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
        }
        else if (existingAssignments != null && existingAssignments.size() == 0) {
            sb.append("No assignments have been set yet.");
        }

        String accountURL = String.format("https://%s/account", globalProperties.getProperty(HOST_NAME));
        Properties p = new Properties();
        p.put("givenname", user.getGivenName());
        p.put("teacherName", groupOwnerName);
        p.put("assignmentsInfo", sb.toString());
        p.put("accountURL", accountURL);
        p.put("sig", SIGNATURE);
        String plainTextMessage = completeTemplateWithProperties(segueContent, p);
        String htmlMessage = null;

        if (null == htmlTemplate) {
            log.debug("HTML email template could not be found!");
        } else {
            Properties htmlTemplateProperties = new Properties();
            htmlTemplateProperties.put("content", plainTextMessage.replace("\n", "<br>"));
            htmlTemplateProperties.put("email", user.getEmail());

            htmlMessage = completeTemplateWithProperties(htmlTemplate, htmlTemplateProperties);
        }

        EmailCommunicationMessage e = new EmailCommunicationMessage(user.getEmail(), user.getGivenName(),
                segueContent.getTitle(), plainTextMessage, EmailType.SYSTEM, htmlMessage);
        this.addToQueue(e);

    }

    /**
     * Sends email verification using email verification template. Assumes that a verification code has been
     * successfully generated.
     * 
     * @param user
     *            - user object used to complete template
     * @param providerString
     *            - the provider
     * @param providerWord
     *            - the provider
     * @throws ContentManagerException
     *             - some content may not have been accessible
     * @throws SegueDatabaseException
     *             - the content was of incorrect type
     */
    public void sendFederatedPasswordReset(final RegisteredUser user, final String providerString,
            final String providerWord) throws ContentManagerException,
            SegueDatabaseException {

        SeguePageDTO segueContent = getSegueDTOEmailTemplate("email-template-federated-password-reset");
        
        if (null == segueContent) {
            log.warn("Federated password reset message not sent as segue content was null!");
            return;
        }

        Properties p = new Properties();
        p.put("givenname", user.getGivenName());
        p.put("providerString", providerString);
        p.put("providerWord", providerWord);
        p.put("sig", SIGNATURE);
        // TODO deal with the potential exception here
        String plainTextMessage = completeTemplateWithProperties(segueContent, p);
        
        SeguePageDTO htmlTemplate = getSegueDTOEmailTemplate("email-template-html");
        
        String htmlMessage = null;
        
        if (null == htmlTemplate) {
            log.debug("HTML email template could not be found!");
        } else {
            Properties htmlTemplateProperties = new Properties();
            htmlTemplateProperties.put("content", plainTextMessage.replace("\n", "<br>"));
            htmlTemplateProperties.put("email", user.getEmail());
            
            htmlMessage = completeTemplateWithProperties(htmlTemplate, htmlTemplateProperties);
        }

        EmailCommunicationMessage e = new EmailCommunicationMessage(user.getEmail(), user.getGivenName(),
                segueContent.getTitle(), plainTextMessage, htmlMessage, EmailType.SYSTEM,
                globalProperties.getProperty(Constants.REPLY_TO_ADDRESS));
        this.addToQueue(e);
    }
    
    /**
     * Returns the SegueDTO we will use as an email template.
     * 
     * @param id  
     *          - the content id of the email template required
     * @return  - the SegueDTO content object
     * @throws SegueDatabaseException 
     *          - error if database cannot be accessed
     * @throws ContentManagerException 
     *          - error if there is a problem accessing content
     */
    private SeguePageDTO getSegueDTOEmailTemplate(final String id) 
            throws SegueDatabaseException, ContentManagerException {
        ContentDTO c = contentVersionController.getContentManager().getContentById(
                contentVersionController.getLiveVersion(), id);

        if (null == c) {
            log.warn(String.format("E-mail template %s does not exist!", id));
            return null;
        }
        
        SeguePageDTO segueContentDTO = null;

        if (c instanceof SeguePageDTO) {
            segueContentDTO = (SeguePageDTO) c;
        } else {
            throw new SegueDatabaseException("Content is of incorrect type:" + c.getType());
        }
        
        return segueContentDTO;
    }
}
