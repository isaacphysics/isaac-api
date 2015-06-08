package uk.ac.cam.cl.dtg.segue.comm;

import static uk.ac.cam.cl.dtg.segue.api.Constants.HOST_NAME;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import uk.ac.cam.cl.dtg.segue.api.managers.ContentVersionController;
import uk.ac.cam.cl.dtg.segue.auth.SegueLocalAuthenticator;
import uk.ac.cam.cl.dtg.segue.dao.users.IUserDataManager;
import uk.ac.cam.cl.dtg.segue.dos.content.ContentBase;
import uk.ac.cam.cl.dtg.segue.dos.content.SeguePage;
import uk.ac.cam.cl.dtg.segue.dos.users.RegisteredUser;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

import com.google.inject.Inject;

/**
 * @author Alistair Stead
 *
 */
public class EmailManager extends AbstractCommunicationQueue<EmailCommunicationMessage> {
    private final PropertiesLoader properties;
    private final IUserDataManager userDataManager;
    private final ContentVersionController contentVersionController;

    /**
     * @param communicator
     * 
     * 
     *            class we'll use to send the actual email.
     */
    @Inject
    public EmailManager(final EmailCommunicator communicator, final PropertiesLoader properties,
            final IUserDataManager userDataManager, final ContentVersionController contentVersionController) {
        super(communicator);
        this.properties = properties;
        this.userDataManager = userDataManager;
        this.contentVersionController = contentVersionController;
    }

    /**
     * @param page
     *            SeguePage that contains SeguePage child with template value
     * @param properties
     *            list of properties from which we can fill in the template
     * @return template with completed fields
     */
    private String completeTemplateWithProperties(final SeguePage page, final Properties properties) {

        ArrayList<ContentBase> children = (ArrayList<ContentBase>) page.getChildren();
        if (!(children.size() == 1 && children.get(0) instanceof SeguePage)) {
            throw new IllegalArgumentException("SeguePage does not contain child for email template!");
        }

        String template = ((SeguePage) children.get(0)).getValue();

        Pattern p = Pattern.compile("\\{\\{[A-Za-z]+\\}\\}");
        Matcher m = p.matcher(template);
        int offset = 0;

        while (m.find()) {
            if (template != null && m.start() >= 0 && m.end() <= template.length()) {
                String tag = template.substring(m.start() + offset, m.end() + offset);
                String strippedTag = tag.substring(2, tag.length() - 2);

                // Check all properties required in the page are in the properties list
                if (properties.containsKey(strippedTag)) {
                    String start = template.substring(0, m.start() + offset);
                    String end = template.substring(m.end() + offset, template.length());
                    template = start + properties.getProperty(strippedTag) + end;
                    offset += properties.getProperty(strippedTag).length() - tag.length();
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
     */
    public void sendPasswordReset(RegisteredUser user) {

        SegueLocalAuthenticator auth = new SegueLocalAuthenticator(userDataManager);

        try {
            user = auth.createPasswordResetTokenForUser(user);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        }

        // ContentVersionController

        String hostName = properties.getProperty(HOST_NAME);
        String verificationURL = String.format("https://%s/resetpassword/%s", hostName, user.getResetToken());

        Properties p = new Properties();
        p.put("user", user.getGivenName());
        p.put("email", user.getEmail());
        p.put("verificationURL", verificationURL);

        // SeguePage page = userManager.

        // String message = completeTemplateWithProperties(page, p);
        // EmailCommunicationMessage e = new EmailCommunicationMessage(user.getEmail(), user.getGivenName(),
        // page.getTitle(), message);

        // this.addToQueue(e);
    }

    /**
     * Sends email registration confirmation using email registration template.
     * 
     * @param user
     *            - user object used to complete template
     */
    public void sendRegistrationConfirmation(final RegisteredUser user) {

        // SegueLocalAuthenticator auth = new SegueLocalAuthenticator(userDataManager);
        // try {
        // user = auth.createEmailVerificationTokenForUser(user);
        // } catch (NoSuchAlgorithmException e) {
        // // TODO Auto-generated catch block
        // e.printStackTrace();
        // } catch (InvalidKeySpecException e) {
        // // TODO Auto-generated catch block
        // e.printStackTrace();
        // }

        String verificationURL = getVerificationURL(user.getEmail(), user.getEmailVerificationToken());

        Properties p = new Properties();
        p.put("user", user);
        p.put("email", user.getEmail());
        p.put("verificationURL", verificationURL);
        // String message = EmailTemplateParser.completeTemplateWithProperties(emailTemplate, p);
        // EmailCommunicationMessage e = new EmailCommunicationMessage(emailAddress, user, emailTemplate.getTitle(),
        // message);

        // this.addToQueue(e);
    }

    /**
     * Creates the URL with appropriate arguments for verification.
     * 
     * @param emailAddress
     *            user email address
     * @param hash
     *            - hash generated from email address
     * @return full URL string
     */
    private String getVerificationURL(final String emailAddress, final String hash) {

        String hostName = properties.getProperty(HOST_NAME);

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("https://%s/emailverification?email=%s&hash=%s", hostName, emailAddress, hash));
        return sb.toString();
    }

}
