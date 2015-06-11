package uk.ac.cam.cl.dtg.segue.comm;

import static uk.ac.cam.cl.dtg.segue.api.Constants.HOST_NAME;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.dtg.segue.api.AuthorisationFacade;
import uk.ac.cam.cl.dtg.segue.api.managers.ContentVersionController;
import uk.ac.cam.cl.dtg.segue.auth.SegueLocalAuthenticator;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dao.users.IUserDataManager;
import uk.ac.cam.cl.dtg.segue.dos.users.RegisteredUser;
import uk.ac.cam.cl.dtg.segue.dto.content.ContentBaseDTO;
import uk.ac.cam.cl.dtg.segue.dto.content.ContentDTO;
import uk.ac.cam.cl.dtg.segue.dto.content.SeguePageDTO;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

import com.google.inject.Inject;

/**
 * @author Alistair Stead
 *
 */
public class EmailManager extends AbstractCommunicationQueue<EmailCommunicationMessage> {
    private final PropertiesLoader globalProperties;
    private final IUserDataManager userDataManager;
    private final ContentVersionController contentVersionController;
    private final SegueLocalAuthenticator authenticator;
    private static final Logger log = LoggerFactory.getLogger(AuthorisationFacade.class);
    private final int MINIMUM_TAG_LENGTH = 4;
    private final String sig = "Isaac Physics Project";

    /**
     * @param communicator
     *            class we'll use to send the actual email.
     * @param globalProperties
     *            global properties used to get host name
     * @param userDataManager
     *            data manager used for authentication
     * @param contentVersionController
     *            content for email templates
     */
    @Inject
    public EmailManager(final EmailCommunicator communicator, final PropertiesLoader globalProperties,
            final IUserDataManager userDataManager, final ContentVersionController contentVersionController,
            final SegueLocalAuthenticator authenticator) {
        super(communicator);
        this.globalProperties = globalProperties;
        this.userDataManager = userDataManager;
        this.contentVersionController = contentVersionController;
        this.authenticator = authenticator;
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

        try {
            authenticator.createPasswordResetTokenForUser(user);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        }

        ContentDTO c = contentVersionController.getContentManager().getContentById(
                    contentVersionController.getLiveVersion(), "email-template-password-reset");

        SeguePageDTO segueContent = null;

        if (c instanceof SeguePageDTO) {
            segueContent = (SeguePageDTO) c;
        } else {
            throw new SegueDatabaseException("Content is of incorrect type:" + c.getType());
        }


        String hostName = globalProperties.getProperty(HOST_NAME);
        String verificationURL = String.format("https://%s/resetpassword/%s", hostName, user.getResetToken());

        // TODO turn these into constants
        Properties p = new Properties();
        p.put("givenname", user.getGivenName());
        p.put("email", user.getEmail());
        p.put("resetURL", verificationURL);
        p.put("sig", sig);


        String message = completeTemplateWithProperties(segueContent, p);

        String htmlMessage = "<html><head><meta charset='utf-8'><title>JS Bin</title></head><body>";
        htmlMessage += message;
        htmlMessage += "</body></html>";

        EmailCommunicationMessage e = new EmailCommunicationMessage(user.getEmail(), user.getGivenName(),
                segueContent.getTitle(), message, htmlMessage);

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

        ContentDTO c = contentVersionController.getContentManager().getContentById(
                contentVersionController.getLiveVersion(), "email-template-registration-confirmation");

        SeguePageDTO segueContent = null;

        if (c instanceof SeguePageDTO) {
            segueContent = (SeguePageDTO) c;
        } else {
            throw new SegueDatabaseException("Content is of incorrect type:" + c.getType());
        }

        String verificationURL = String.format("https://%s/verifyemail/%s", globalProperties.getProperty(HOST_NAME),
                user.getEmailVerificationToken());

        Properties p = new Properties();
        p.put("givenname", user.getGivenName());
        p.put("email", user.getEmail());
        p.put("verificationURL", verificationURL);
        p.put("sig", sig);
        String message = completeTemplateWithProperties(segueContent, p);
        EmailCommunicationMessage e = new EmailCommunicationMessage(user.getEmail(), user.getGivenName() + " "
                + user.getFamilyName(), segueContent.getTitle(), message, null);

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
    public void sendEmailVerification(final RegisteredUser user) throws ContentManagerException, SegueDatabaseException {

        ContentDTO c = contentVersionController.getContentManager().getContentById(
                contentVersionController.getLiveVersion(), "email-template-email-verification");

        SeguePageDTO segueContent = null;

        if (c instanceof SeguePageDTO) {
            segueContent = (SeguePageDTO) c;
        } else {
            throw new SegueDatabaseException("Content is of incorrect type:" + c.getType());
        }

        String verificationURL = String.format("https://%s/verifyemail/%s", globalProperties.getProperty(HOST_NAME),
                user.getEmailVerificationToken());

        Properties p = new Properties();
        p.put("givenname", user.getGivenName());
        p.put("email", user.getEmail());
        p.put("verificationURL", verificationURL);
        p.put("sig", sig);
        String message = completeTemplateWithProperties(segueContent, p);

        String htmlMessage = "<!DOCTYPE html><html><head><meta charset='utf-8'><title>JS Bin</title></head><body>";
        htmlMessage += message;
        htmlMessage += "</body></html>";

        EmailCommunicationMessage e = new EmailCommunicationMessage(user.getEmail(), user.getGivenName() + " "
                + user.getFamilyName(), segueContent.getTitle(), message, htmlMessage);

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

        ContentDTO c = contentVersionController.getContentManager().getContentById(
                contentVersionController.getLiveVersion(), "email-template-federated-password-reset");

        SeguePageDTO segueContent = null;

        if (c instanceof SeguePageDTO) {
            segueContent = (SeguePageDTO) c;
        } else {
            throw new SegueDatabaseException("Content is of incorrect type:" + c.getType());
        }

        Properties p = new Properties();
        p.put("givenname", user.getGivenName());
        p.put("providerString", providerString);
        p.put("providerWord", providerWord);
        p.put("sig", sig);
        // TODO deal with the potential exception here
        String message = completeTemplateWithProperties(segueContent, p);
        
        String htmlMessage = "<!DOCTYPE html><html><head><meta charset='utf-8'><title>JS Bin</title></head><body>";
        htmlMessage += message;
        htmlMessage += "</body></html>";
        
        EmailCommunicationMessage e = new EmailCommunicationMessage(user.getEmail(), user.getGivenName() + " "
                + user.getFamilyName(), segueContent.getTitle(), message, htmlMessage);

        this.addToQueue(e);
    }

}
