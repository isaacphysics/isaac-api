/*
 * Copyright 2022 Chris Purdy
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.cam.cl.dtg.segue.comm;

import com.google.api.client.util.Lists;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.mailgun.api.v3.MailgunMessagesApi;
import com.mailgun.client.MailgunClient;
import com.mailgun.model.message.Message;
import com.mailgun.model.message.MessageResponse;
import feign.FeignException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.dos.AbstractUserPreferenceManager;
import uk.ac.cam.cl.dtg.isaac.dos.UserPreference;
import uk.ac.cam.cl.dtg.isaac.dos.users.EmailVerificationStatus;
import uk.ac.cam.cl.dtg.isaac.dto.content.EmailTemplateDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.util.AbstractConfigLoader;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;

import static com.mailgun.util.Constants.EU_REGION_BASE_URL;
import static uk.ac.cam.cl.dtg.segue.api.Constants.*;
import static uk.ac.cam.cl.dtg.segue.api.monitors.SegueMetrics.QUEUED_EMAIL;

public class MailGunEmailManager {

    private final Map<String, String> globalStringTokens;
    private static final Logger log = LoggerFactory.getLogger(EmailManager.class);

    private MailgunMessagesApi mailgunMessagesApi;
    private final AbstractUserPreferenceManager userPreferenceManager;
    private final AbstractConfigLoader globalProperties;
    private final ExecutorService executor;

    @Inject
    public MailGunEmailManager(final Map<String, String> globalStringTokens, final AbstractConfigLoader globalProperties, final AbstractUserPreferenceManager userPreferenceManager) {
        this.globalStringTokens = globalStringTokens;
        this.userPreferenceManager = userPreferenceManager;
        this.globalProperties = globalProperties;
        this.executor = Executors.newFixedThreadPool(1);
    }

    private void createMessagesApiIfNeeded() {
        if (null == this.mailgunMessagesApi) {
            log.info("Creating singleton MailgunMessagesApi object.");
            this.mailgunMessagesApi = MailgunClient
                    .config(EU_REGION_BASE_URL, globalProperties.getProperty(MAILGUN_SECRET_KEY))
                    .logLevel(feign.Logger.Level.NONE)
                    .createApi(MailgunMessagesApi.class);
        }
    }

    public Future<Optional<MessageResponse>> sendBatchEmails(final Collection<RegisteredUserDTO> userDTOs, final EmailTemplateDTO emailContentTemplate,
                                                             final EmailType emailType,
                                                             @Nullable final Map<String, Object> templateVariablesOrNull,
                                                             @Nullable final Map<Long, Map<String, Object>> userVariablesOrNull) {

        // Lazily construct the MailGun messages API
        this.createMessagesApiIfNeeded();

        // Ensure all relevant email variable maps are not null
        Map<Long, Map<String, Object>> userVariables = null != userVariablesOrNull ? userVariablesOrNull : Map.of();
        Map<String, Object> templateVariables = new HashMap<>(null != templateVariablesOrNull ? templateVariablesOrNull : Map.of());
        // Combine local and global template variables (non-recipient ones)
        if (null != this.globalStringTokens) {
            templateVariables.putAll(this.globalStringTokens);
        }

        // This expression:
        //  - filters users by their email preferences and email verification status
        //  - for each user U, extracts the name and email of U, and adds them to the (possibly empty) map of
        //    `userVariables.get(U.getId())`
        //  - creates a new map with emails as keys and the updated `userVariables` maps as values
        Map<String, Object> recipientVariables = userDTOs.stream()
                .filter(user -> {
                    // FIXME - should system emails ignore this setting to avoid abuse?
                    if (user.getEmailVerificationStatus() == EmailVerificationStatus.DELIVERY_FAILED) {
                        return false;
                    }
                    try {
                        UserPreference preference = userPreferenceManager.getUserPreference(
                                SegueUserPreferences.EMAIL_PREFERENCE.name(), emailType.name(), user.getId());
                        // If no preference is present, do not send email to this user.
                        return preference != null && preference.getPreferenceValue();
                    } catch (SegueDatabaseException e) {
                        // Assume the worst and do not send the email to this user in the case of a DB issue
                    }
                    return false;
                }).collect(
                    // Produce a map from email -> user variables (variables to populate the MailGun template with)
                    Collectors.toMap(
                        RegisteredUserDTO::getEmail,
                        // This just concatenates the user email and name onto the (possibly) existing user variables,
                        // so that the name and email can always be referred to in MailGun email templates.
                        u -> Stream.of(
                                ImmutableMap.of(
                                "email", u.getEmail(),
                                "givenName", u.getGivenName()
                                ),
                                userVariables.getOrDefault(u.getId(), ImmutableMap.of())
                        ).flatMap(m -> m.entrySet().stream())
                        // Turn the above stream of (key, value) pairs (the user variables) into a map, discarding
                        // entries with duplicate keys (that's what the third argument is for), which shouldn't happen
                        // anyway since users have unique emails
                        .collect(Collectors.toMap(Entry::getKey, Entry::getValue, (e1, e2) -> e1))
                    )
                );

        String fromAddress = StringUtils.defaultIfBlank(
                emailContentTemplate.getOverrideFromAddress(),
                globalProperties.getProperty(Constants.MAILGUN_FROM_ADDRESS)
        );
        String fromName = StringUtils.defaultIfBlank(
                emailContentTemplate.getOverrideFromName(),
                globalProperties.getProperty(Constants.MAIL_NAME)
        );
        String replyToAddress = StringUtils.defaultIfBlank(
                emailContentTemplate.getReplyToEmailAddress(),
                globalProperties.getProperty(Constants.REPLY_TO_ADDRESS)
        );
        String replyToName = StringUtils.defaultIfBlank(
                emailContentTemplate.getReplyToName(),
                globalProperties.getProperty(Constants.MAIL_NAME)
        );
        String replyTo = replyToName + " <" + replyToAddress + "> ";
        String from = fromName + " <" + fromAddress + "> ";

        List<String> usersToSendTo = Lists.newArrayList(recipientVariables.keySet());
        if (usersToSendTo.isEmpty()) {
            if (!userDTOs.isEmpty()) {
                log.warn(String.format("No eligible recipients from batch of %s %s emails.", userDTOs.size(), emailType.name()));
            }
            return CompletableFuture.completedFuture(Optional.empty());
        }

        Message message = Message.builder()
                .from(from)
                .replyTo(replyTo)
                .to(usersToSendTo)
                .template(emailContentTemplate.getId())  // We use the same template IDs in MailGun!
                .subject(emailContentTemplate.getSubject())
                .mailgunVariables(templateVariables)
                .recipientVariables(recipientVariables)
                .build();

        QUEUED_EMAIL.labels(emailType.name(), "mailgun-api").inc(usersToSendTo.size());

        return executor.submit(() -> {
            try {
                return Optional.of(mailgunMessagesApi.sendMessage(globalProperties.getProperty(MAILGUN_DOMAIN), message));
            } catch (FeignException e) {
                log.error("Failed to send email to {} users via the MailGun API:", userDTOs.size(), e);
                return Optional.empty();
            }
        });
    }

}
