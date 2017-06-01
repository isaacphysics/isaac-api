/*
 * Copyright 2014 Alistair Stead and Stephen Cummins
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
package uk.ac.cam.cl.dtg.segue.comm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.util.Lists;
import com.google.api.client.util.Maps;
import com.google.api.client.util.Sets;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import org.apache.commons.lang3.Validate;
import org.eclipse.jgit.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import static uk.ac.cam.cl.dtg.segue.api.Constants.CONTENT_VERSION_FIELDNAME;

/**
 * EmailManager
 * Responsible for orchestration of email sending in Segue.
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
     * @throws ContentManagerException if we can't parse the content
     * @throws SegueDatabaseException if we cannot contact the database for logging.
     */
    public void sendTemplatedEmailToUser(final RegisteredUserDTO userDTO, final EmailTemplateDTO emailContentTemplate,
                                                   final Map<String, Object> tokenToValueMapping, final EmailType emailType)
            throws ContentManagerException, SegueDatabaseException {

        // generate properties from hashMap for token replacement process
        Properties propertiesToReplace = new Properties();
        propertiesToReplace.putAll(this.flattenTokenMap(tokenToValueMapping, Maps.newHashMap(), ""));

        // Add all properties in the user DTO (preserving types) so they are available to email templates.
        Map userPropertiesMap = new org.apache.commons.beanutils.BeanMap(userDTO);
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
     * Function that enables contact us messages to be sent to a random email address (not to a known user).
     *
     * @param recipientEmailAddress
     *            - Email Address to send the contact us message to.
     * @param emailValues
     *            - must contain at least contactEmail, replyToName, contactSubject plus any other email tokens to replace.
     * @throws ContentManagerException
     *             - if some content is not found
     * @throws SegueDatabaseException
     *             - if the database cannot be accessed
     */
    public void sendContactUsFormEmail(final String recipientEmailAddress, final Map<String, Object> emailValues)
            throws ContentManagerException, SegueDatabaseException {
        EmailTemplateDTO emailContent = getEmailTemplateDTO("email-contact-us-form");
        emailContent.setReplyToEmailAddress(emailValues.get("contactEmail").toString());
        emailContent.setReplyToName(emailValues.get("replyToName").toString());

        emailContent.setSubject("(Contact Form) " + emailValues.get("contactSubject").toString());

        // generate properties from hashMap for token replacement process
        Properties propertiesToReplace = new Properties();
        propertiesToReplace.putAll(this.flattenTokenMap(emailValues, Maps.newHashMap(), ""));
        propertiesToReplace.put("sig", SIGNATURE);

        EmailCommunicationMessage e = constructMultiPartEmail(null, recipientEmailAddress, emailContent,
                propertiesToReplace,
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
        //TODO: this needs refactoring.
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
                .put(CONTENT_VERSION_FIELDNAME, this.contentManager.getCurrentContentSHA()).build();
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
    private void filterByPreferencesAndAddToQueue(final RegisteredUserDTO userDTO,
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
     * Method to take a random (potentially nested map) and flatten it into something where values can be easily extracted
     * for email templates.
     *
     * Nested fields are addressed as per json objects and separated with the dot operator.
     *
     * @param inputMap - A map of string to random object
     * @param outputMap - the flattened map which is also the returned object
     * @param keyPrefix - the key prefix - used for recursively creating the map key.
     * @return a flattened map for containing strings that can be used in email template replacement.
     */
     public Map<String, String> flattenTokenMap(final Map <String, Object> inputMap, final Map <String, String> outputMap, String keyPrefix) {
        if (null == keyPrefix) {
            keyPrefix = "";
        }

        for(Map.Entry<String, Object> mapEntry : inputMap.entrySet()){
            String valueToStore = "";

            if (mapEntry.getValue() == null) {
                valueToStore = "";

            } else if (mapEntry.getValue() instanceof Map) {
                // if we have a general map we should recurse until we get objects we can do something with.
                this.flattenTokenMap((Map) mapEntry.getValue(), outputMap, keyPrefix + mapEntry.getKey() + ".");

            } else if (mapEntry.getValue() instanceof ContentDTO) {
                Map objectWithJavaTypes = new org.apache.commons.beanutils.BeanMap(mapEntry.getValue());

                // go through and convert any known java types into our preferred string representation
                Map<String, String> temp = this.flattenTokenMap(objectWithJavaTypes,
                        Maps.newHashMap(), keyPrefix + mapEntry.getKey() + ".");
                outputMap.putAll(temp);

                // now convert any java types we haven't defined specific conversions for into the basic Jackson serialisations.
                ObjectMapper om = new ObjectMapper();
                this.flattenTokenMap(om.convertValue(mapEntry.getValue(), HashMap.class),
                        outputMap, keyPrefix + mapEntry.getKey() + ".");

            } else {
                valueToStore = this.emailTokenValueMapper(mapEntry.getValue());
            }

            if (valueToStore != null && !"".equals(valueToStore)) {
                // assume that the first entry into the output map is the correct one
                outputMap.putIfAbsent(keyPrefix + mapEntry.getKey(), valueToStore);
            }
        }

        return outputMap;
    }

    /**
     * helper function to map a value to an email friendly string
     *
     * @param o - object to map
     * @return more sensible string representation or null
     */
    private String emailTokenValueMapper(final Object o) {
        String valueToStore;
        if (o == null) {
            valueToStore = "";
        } else if (o instanceof String) {
            valueToStore = (String) o;
        } else if (o instanceof Date) {
            valueToStore = FULL_DATE_FORMAT.format((Date) o);
        } else if (o instanceof Number || o instanceof Boolean) {
            valueToStore = o.toString();
        } else if (o instanceof Enum){
            valueToStore = ((Enum) o).name();
        } else if (o instanceof ExternalReference) {
            ExternalReference er = (ExternalReference) o;
            valueToStore = String.format("<a href='%s'>%s</a>", er.getUrl(), er.getTitle()) +
                    "\n";
        } else if (o instanceof Collection) {
            List<String> sl = Lists.newArrayList();

            for (Object i : (Collection) o) {
                String s = this.emailTokenValueMapper(i);
                if (s != null) {
                    sl.add(s);
                }
            }

            valueToStore = StringUtils.join(sl, ",");
        } else {
            return null;
        }
        return valueToStore;
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
    public EmailCommunicationMessage constructMultiPartEmail(@Nullable final Long userId, final String userEmail,
            EmailTemplateDTO emailContent, Properties contentProperties, final EmailType emailType)
					throws ContentManagerException, ResourceNotFoundException {
    	Validate.notNull(userEmail);
    	Validate.notEmpty(userEmail);

    	contentProperties.putIfAbsent("sig", SIGNATURE);

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

        EmailTemplateDTO emailTemplateDTO;
        if (c instanceof EmailTemplateDTO) {
            emailTemplateDTO = (EmailTemplateDTO) c;
        } else {
            throw new ContentManagerException("Content is of incorrect type:" + c.getType());
        }

        return emailTemplateDTO;
    }
}