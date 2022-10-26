/*
 * Copyright 2022 Chris Purdy
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.util.Sets;
import com.mailgun.api.v3.MailgunMessagesApi;
import com.mailgun.client.MailgunClient;
import com.mailgun.model.message.Message;
import com.mailgun.model.message.MessageResponse;
import org.apache.commons.text.StringEscapeUtils;
import org.elasticsearch.common.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.dos.AbstractUserPreferenceManager;
import uk.ac.cam.cl.dtg.isaac.dos.UserPreference;
import uk.ac.cam.cl.dtg.isaac.dos.users.EmailVerificationStatus;
import uk.ac.cam.cl.dtg.isaac.dto.content.EmailTemplateDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;
import static uk.ac.cam.cl.dtg.segue.api.Constants.MAIL_FROM_ADDRESS;
import static uk.ac.cam.cl.dtg.segue.api.Constants.MAILGUN_SECRET_KEY;
import static uk.ac.cam.cl.dtg.segue.api.Constants.MAILGUN_SANDBOX_URL;
import static uk.ac.cam.cl.dtg.segue.api.Constants.SegueUserPreferences;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MailGunEmailManager {

    private final Map<String, String> globalStringTokens;
    private static final Logger log = LoggerFactory.getLogger(EmailManager.class);
    private static final int MINIMUM_TAG_LENGTH = 4;

    private final MailgunMessagesApi mailgunMessagesApi;
    private final ObjectMapper objectMapper;
    private final AbstractUserPreferenceManager userPreferenceManager;
    private final PropertiesLoader globalProperties;

    @Inject
    public MailGunEmailManager(final ObjectMapper objectMapper, final Map<String, String> globalStringTokens, final PropertiesLoader globalProperties, final AbstractUserPreferenceManager userPreferenceManager) {
        this.objectMapper = objectMapper;
        this.globalStringTokens = globalStringTokens;
        this.userPreferenceManager = userPreferenceManager;
        this.globalProperties = globalProperties;
        this.mailgunMessagesApi = MailgunClient.config(globalProperties.getProperty(MAILGUN_SECRET_KEY)).createApi(MailgunMessagesApi.class);
    }

    public MessageResponse sendBatchEmails(final Collection<RegisteredUserDTO> userDTOs, final EmailTemplateDTO emailContentTemplate,
                                final Map<String, String> templateVariables, final EmailType emailType, final @Nullable Map<Long, Map<String, Object>> recipientVariablesOrNull)
            throws ContentManagerException, SegueDatabaseException, JsonProcessingException {

        Map<Long, RegisteredUserDTO> userMap = userDTOs.stream()
                // Filter users by their email preferences and email verification status
                .filter(user -> {
                    // FIXME - should system emails ignore this setting to avoid abuse?
                    if (user.getEmailVerificationStatus() == EmailVerificationStatus.DELIVERY_FAILED) {
                        return false;
                    }
                    try {
                        UserPreference preference = userPreferenceManager.getUserPreference(SegueUserPreferences.EMAIL_PREFERENCE.name(), emailType.name(), user.getId());
                        // If no preference is present, do not send email to this user.
                        return preference != null && preference.getPreferenceValue();
                    } catch (SegueDatabaseException e) {
                        // Assume the worst and do not send the email to this user in the case of a DB issue
                    }
                    return false;
                })
                .collect(
                    Collectors.toMap(
                        RegisteredUserDTO::getId,
                        Function.identity()
                    )
                );

        Map<Long, Map<String, Object>> recipientVariables = null != recipientVariablesOrNull ? recipientVariablesOrNull : Map.of();

        Map<String, Map<String, Object>> recipientVariablesWithUserProps = userMap
                .entrySet().stream()
                .collect(Collectors.toMap(
                        u -> u.getValue().getEmail(),
                        u -> {
                            Map<String, Object> userProps = objectMapper.convertValue(u.getValue(), objectMapper.getTypeFactory().constructMapType(HashMap.class, String.class, Object.class));
                            return Stream.concat(
                                    userProps.entrySet().stream().filter(e -> null != e.getValue()),
                                    recipientVariables.getOrDefault(u.getKey(), Map.of()).entrySet().stream()
                            ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                        }
                    )
                );

        Map<String, String> allTemplateVariables = Stream.concat(
                this.globalStringTokens.entrySet().stream(),
                templateVariables.entrySet().stream()
        ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        sanitizeEmailParameters(allTemplateVariables);

        String fromAddress = emailContentTemplate.getOverrideFromAddress();
        if (null == fromAddress || fromAddress.isEmpty()) {
            fromAddress = globalProperties.getProperty(MAIL_FROM_ADDRESS);
        }
        // FIXME remove - this is for sandboxing
        fromAddress = "Isaac Physics <mailgun@" + globalProperties.getProperty(MAILGUN_SANDBOX_URL) + ">";
        String plainTextContent = this.completeTemplateWithProperties(this.escapeRecipientVariables(emailContentTemplate.getPlainTextContent(), recipientVariablesWithUserProps), allTemplateVariables, false);
        String htmlContent = this.completeTemplateWithProperties(this.escapeRecipientVariables(emailContentTemplate.getHtmlContent(), recipientVariablesWithUserProps), allTemplateVariables, true);

        Message message = Message.builder()
                .from(fromAddress)
                .to(new ArrayList<>(recipientVariablesWithUserProps.keySet()))
                .subject(emailContentTemplate.getSubject())
                .text(plainTextContent)
                .html(htmlContent)
                .recipientVariables(recipientVariablesWithUserProps.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))
                .build();

        return mailgunMessagesApi.sendMessage(globalProperties.getProperty(MAILGUN_SANDBOX_URL), message);
    }

    private String escapeRecipientVariables(final String content, final Map<String, Map<String, Object>> recipientVariables) {
        return recipientVariables
                .values().stream().map(Map::keySet).flatMap(Set::stream)
                .reduce(content, (acc, k) -> acc.replaceAll("\\{\\{" + k + "\\}\\}", "%recipient." + k + "%"));
    }

    /**
     * Method to parse and replace template elements with the form {{TAG}}.
     *
     * @param templateProperties
     *            list of properties from which we can fill in the template
     * @return template with completed fields
     */
    private String completeTemplateWithProperties(final String content, final Map<String, String> templateProperties, final boolean html) {
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
                    if (templateProperties.get(strippedTag + "_HTML") != null) {
                        template += templateProperties.get(strippedTag + "_HTML");
                    }
                    template += end;

                    offset += templateProperties.get(strippedTag + "_HTML").length() - tag.length();
                } else if (templateProperties.containsKey(strippedTag)) {
                    String start = template.substring(0, m.start() + offset);
                    String end = template.substring(m.end() + offset, template.length());

                    template = start;
                    if (templateProperties.get(strippedTag) != null) {
                        template += templateProperties.get(strippedTag);
                    }
                    template += end;

                    offset += templateProperties.get(strippedTag).length() - tag.length();
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
     * Escape any HTML present in the email before sending. MUTATES ARGUMENT.
     *
     * @param emailParameters - the parameters to be inserted into the email
     */
    public static void sanitizeEmailParameters(final Map<String, String> emailParameters) {
        for (Map.Entry<String, String> entry : emailParameters.entrySet()) {
            String key = entry.getKey();
            if (!(key.startsWith("event.") || key.endsWith("URL") || key.endsWith("_HTML"))) {
                emailParameters.put(entry.getKey(), StringEscapeUtils.escapeHtml4(entry.getValue()));
            }
        }
    }

}
