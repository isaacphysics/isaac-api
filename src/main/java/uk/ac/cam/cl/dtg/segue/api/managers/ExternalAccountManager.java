/*
 * Copyright 2021 James Sharkey
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
package uk.ac.cam.cl.dtg.segue.api.managers;

import com.mailjet.client.errors.MailjetClientCommunicationException;
import com.mailjet.client.errors.MailjetException;
import com.mailjet.client.errors.MailjetRateLimitException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.users.IExternalAccountDataManager;
import uk.ac.cam.cl.dtg.isaac.dos.users.EmailVerificationStatus;
import uk.ac.cam.cl.dtg.isaac.dos.users.UserExternalAccountChanges;
import uk.ac.cam.cl.dtg.util.email.MailJetApiClientWrapper;
import uk.ac.cam.cl.dtg.util.email.MailJetSubscriptionAction;

import java.util.List;

public class ExternalAccountManager implements IExternalAccountManager {
    private static final Logger log = LoggerFactory.getLogger(ExternalAccountManager.class);

    private final IExternalAccountDataManager database;
    private final MailJetApiClientWrapper mailjetApi;

    /**
     *  Synchronise account settings, email preferences and verification status with third party providers.
     *
     *  Currently this class is highly specialised for synchronising with MailJet.
     *
     * @param mailjetApi - to enable updates on MailJet
     * @param database - to persist external identifiers and to record sync success.
     */
    public ExternalAccountManager(final MailJetApiClientWrapper mailjetApi, final IExternalAccountDataManager database) {
        this.database = database;
        this.mailjetApi = mailjetApi;
    }

    /**
     *  Synchronise account settings and data with external providers.
     *
     *  Whilst the actions this method takes are mostly idempotent, it should not be run simultaneously with itself.
     *
     * @throws ExternalAccountSynchronisationException on unrecoverable errors with external providers.
     */
    public synchronized void synchroniseChangedUsers() throws ExternalAccountSynchronisationException {
        try {
            List<UserExternalAccountChanges> userRecordsToUpdate = database.getRecentlyChangedRecords();

            for (UserExternalAccountChanges userRecord : userRecordsToUpdate) {

                Long userId = userRecord.getUserId();
                log.debug(String.format("Processing user: %s", userId));
                try {

                    String accountEmail = userRecord.getAccountEmail();
                    boolean accountEmailDeliveryFailed = EmailVerificationStatus.DELIVERY_FAILED.equals(userRecord.getEmailVerificationStatus());
                    String mailjetId = userRecord.getProviderUserId();
                    JSONObject mailjetDetails;

                    if (null != mailjetId) {
                        // If there is a "mailjet_id", get the account from MailJet.
                        mailjetDetails = mailjetApi.getAccountByIdOrEmail(mailjetId);
                        if (userRecord.isDeleted()) {
                            // Case: deleted from Isaac but still on MailJet:
                            //    Expect: "deleted" but non-null "mailjet_id"
                            //    Action: GDPR deletion, null out MailJet ID?, update provider_last_updated
                            log.debug("Case: deletion.");
                            deleteUserFromMailJet(mailjetId, userRecord);
                        } else if (accountEmailDeliveryFailed) {
                            // Case: DELIVERY_FAILED but already on MailJet
                            //    Expect: DELIVERY_FAILED, but non-null "mailjet_id"
                            //    Action: same as deletion? Or just remove from lists for now?
                            log.debug("Case: delivery failed.");
                            mailjetApi.updateUserSubscriptions(mailjetId, MailJetSubscriptionAction.REMOVE, MailJetSubscriptionAction.REMOVE);
                        } else if (!accountEmail.toLowerCase().equals(mailjetDetails.getString("Email"))) {
                            // Case: account email change:
                            //    Expect: non-null "mailjet_id", email in MailJet != email in database
                            //    Action: delete old email, add new user for new email
                            log.debug("Case: account email change.");
                            mailjetApi.permanentlyDeleteAccountById(mailjetId);
                            mailjetId = mailjetApi.addNewUserOrGetUserIfExists(accountEmail);
                            updateUserOnMailJet(mailjetId, userRecord);
                        } else {
                            // Case: changed details/preferences:
                            //    Expect: not deleted, not DELIVERY_FAILED
                            //    Action: update details, update subscriptions, update provider_last_updated
                            log.debug("Case: generic preferences update.");
                            updateUserOnMailJet(mailjetId, userRecord);
                        }
                    } else {
                        if (!accountEmailDeliveryFailed && !userRecord.isDeleted()) {
                            // Case: new to Isaac, not on MailJet:
                            //    Expect: null "mailjet_id", not DELIVERY_FAILED, not deleted
                            //    Action: create MailJet ID, update details, update subscriptions, update provider_last_updated
                            //            This will upload even users who are not subscribed to emails.
                            log.debug("Case: new to Isaac/not yet on MailJet");
                            mailjetId = mailjetApi.addNewUserOrGetUserIfExists(accountEmail);
                            updateUserOnMailJet(mailjetId, userRecord);
                        } else {
                            // Case: not on MailJet, do not upload to Mailjet:
                            //     Expect: either invalid email, deleted, or not subscribed at all so don't upload.
                            log.debug("Case: invalid/incorrect/already-unsubscribed user to skip.");
                            database.updateExternalAccount(userId, null);
                        }
                    }
                    // Iff action done successfully, update the provider_last_updated time:
                    log.debug("Update provider_last_updated.");
                    database.updateProviderLastUpdated(userId);

                } catch (SegueDatabaseException e) {
                    log.error(String.format("Error storing record of MailJet update to user (%s)!", userId));
                } catch (MailjetClientCommunicationException e) {
                    log.error("Failed to talk to MailJet!");
                    throw new ExternalAccountSynchronisationException("Failed to successfully connect to MailJet!");
                } catch (MailjetRateLimitException e) {
                    log.warn("MailJet rate limiting!");
                    throw new ExternalAccountSynchronisationException("MailJet API rate limits exceeded!");
                } catch (MailjetException e) {
                    log.error(e.getMessage());
                    throw new ExternalAccountSynchronisationException(e.getMessage());
                }
            }
        } catch (SegueDatabaseException e) {
            log.error("Database error whilst collecting users whose details have changed!", e);
        }
    }

    private void updateUserOnMailJet(final String mailjetId, final UserExternalAccountChanges userRecord) throws SegueDatabaseException, MailjetException {
        Long userId = userRecord.getUserId();
        mailjetApi.updateUserProperties(mailjetId, userRecord.getGivenName(), userRecord.getRole().toString(), userRecord.getEmailVerificationStatus().toString());

        MailJetSubscriptionAction newsStatus = (userRecord.allowsNewsEmails() != null && userRecord.allowsNewsEmails()) ? MailJetSubscriptionAction.FORCE_SUBSCRIBE : MailJetSubscriptionAction.UNSUBSCRIBE;
        MailJetSubscriptionAction eventsStatus = (userRecord.allowsEventsEmails() != null && userRecord.allowsEventsEmails()) ? MailJetSubscriptionAction.FORCE_SUBSCRIBE : MailJetSubscriptionAction.UNSUBSCRIBE;
        mailjetApi.updateUserSubscriptions(mailjetId, newsStatus, eventsStatus);

        database.updateExternalAccount(userId, mailjetId);
    }

    private void deleteUserFromMailJet(final String mailjetId, final UserExternalAccountChanges userRecord) throws SegueDatabaseException, MailjetException {
        Long userId = userRecord.getUserId();
        mailjetApi.permanentlyDeleteAccountById(mailjetId);
        database.updateExternalAccount(userId, null);
    }
}
