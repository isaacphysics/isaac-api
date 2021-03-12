package uk.ac.cam.cl.dtg.segue.api.managers;

import com.google.inject.Inject;
import com.mailjet.client.ClientOptions;
import com.mailjet.client.MailjetClient;
import com.mailjet.client.MailjetRequest;
import com.mailjet.client.MailjetResponse;
import com.mailjet.client.errors.MailjetClientCommunicationException;
import com.mailjet.client.errors.MailjetClientRequestException;
import com.mailjet.client.errors.MailjetException;
import com.mailjet.client.errors.MailjetRateLimitException;
import com.mailjet.client.resource.Contact;
import com.mailjet.client.resource.ContactManagecontactslists;
import com.mailjet.client.resource.Contactdata;
import com.mailjet.client.resource.Contacts;
import com.mailjet.client.resource.ContactslistImportList;
import org.apache.commons.lang3.Validate;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.users.IExternalAccountDataManager;
import uk.ac.cam.cl.dtg.segue.dos.users.EmailVerificationStatus;
import uk.ac.cam.cl.dtg.segue.dos.users.UserExternalAccountChanges;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

import javax.ws.rs.core.Response;
import java.util.List;

import static uk.ac.cam.cl.dtg.segue.api.Constants.*;

public class ExternalAccountManager {
    private static final Logger log = LoggerFactory.getLogger(ExternalAccountManager.class);

    private enum MailJetSubscriptionAction {
        SUBSCRIBE("addnoforce"),
        FORCE_SUBSCRIBE("addforce"),
        UNSUBSCRIBE("unsub"),
        REMOVE("remove");

        public final String value;

        MailJetSubscriptionAction(final String value) {
            this.value = value;
        }
    }

    private final IExternalAccountDataManager database;
    private final MailjetClient mailjetClient;
    private final String newsListId;
    private final String eventsListId;

    @Inject
    public ExternalAccountManager(final PropertiesLoader properties, IExternalAccountDataManager database) {

        this.database = database;

        ClientOptions options = ClientOptions.builder()
                .apiKey(properties.getProperty(MAILJET_API_KEY))
                .apiSecretKey(properties.getProperty(MAILJET_API_SECRET))
                .build();

        this.mailjetClient = new MailjetClient(options);
        this.newsListId = properties.getProperty(MAILJET_NEWS_LIST_ID);
        this.eventsListId = properties.getProperty(MAILJET_EVENTS_LIST_ID);
    }

    // FIXME - return value / exception throwing on failure?
    public void processRecentlyUpdatedUsers() {
        try {
            List<UserExternalAccountChanges> userRecordsToUpdate = database.getRecentlyChangedRecords();

            for (UserExternalAccountChanges userRecord : userRecordsToUpdate) {

                Long userId = userRecord.getUserId();
                log.debug(String.format("Processing user: %s", userId));
                try {

                    String accountEmail = userRecord.getAccountEmail();
                    String mailjetId = userRecord.getProviderUserId();
                    JSONObject mailjetDetails;

                    if (null != mailjetId) {
                        // If there is a "mailjet_id", get the account from MailJet.
                        mailjetDetails = mailJetAPIGetAccountByIdOrEmail(mailjetId);
                        if (userRecord.isDeleted()) {
                            // Case: deleted from Isaac but still on MailJet:
                            //    Expect: "deleted" but non-null "mailjet_id"
                            //    Action: GDPR deletion, null out MailJet ID?, update provider_last_updated
                            log.debug("Case: deletion.");
                            deleteUserFromMailJet(mailjetId, userRecord);
                        } else if (EmailVerificationStatus.DELIVERY_FAILED.equals(userRecord.getEmailVerificationStatus())) {
                            // Case: DELIVERY_FAILED but already on MailJet
                            //    Expect: DELIVERY_FAILED, but non-null "mailjet_id"
                            //    Action: same as deletion? Or just remove from lists for now?
                            log.debug("Case: delivery failed.");
                            mailJetAPIUpdateUserSubscriptions(mailjetId, MailJetSubscriptionAction.REMOVE, MailJetSubscriptionAction.REMOVE);
                        } else if (!accountEmail.equals(mailjetDetails.getString("Email"))) {
                            // Case: account email change:
                            //    Expect: non-null "mailjet_id", email in MailJet != email in database
                            //    Action: delete old email, add new user for new email
                            log.debug("Case: account email change.");
                            mailJetAPIPermanentlyDeleteAccountById(mailjetId);
                            mailjetId = mailJetAPIAddNewUser(accountEmail);
                            updateUserOnMailJet(mailjetId, userRecord);
                        } else {
                            // Case: changed details/preferences:
                            //    Expect: not deleted, not DELIVERY_FAILED
                            //    Action: update details, update subscriptions, update provider_last_updated
                            log.debug("Case: generic preferences update.");
                            updateUserOnMailJet(mailjetId, userRecord);
                        }
                    } else {
                        if (!EmailVerificationStatus.DELIVERY_FAILED.equals(userRecord.getEmailVerificationStatus())) {
                            // Case: new to Isaac, not on MailJet:
                            //    Expect: null "mailjet_id", not DELIVERY_FAILED
                            //    Action: create MailJet ID, update details, update subscriptions, update provider_last_updated
                            log.debug("Case: new to Isaac/not on MailJet");
                            mailjetId = mailJetAPIAddNewUser(accountEmail);
                            updateUserOnMailJet(mailjetId, userRecord);
                        } else {
                            // Not on MailJet, but invalid email so don't add to MailJet.
                            // Do we need this row? :/
                            log.debug("Case: invalid/incorrect user to skip.");
                            database.updateExternalAccount(userId, null);
                        }
                    }
                    // Iff action done successfully, update the provider_last_updated time:
                    database.updateProviderLastUpdated(userId);

                } catch (SegueDatabaseException e) {
                    log.error(String.format("Error storing record of MailJet update to user (%s)!", userId));
                } catch (MailjetClientCommunicationException e) {
                    // TODO - abort better?
                    log.error("Failed to talk to MailJet!");
                    break;
                } catch (MailjetRateLimitException e) {
                    // TODO - abort better?
                    log.warn("MailJet rate limiting!");
                    break;
                } catch (MailjetException e) {
                    log.error(e.getMessage());
                }
            }
        } catch (SegueDatabaseException e) {
            e.printStackTrace();
        }
    }

    private void updateUserOnMailJet(final String mailjetId, final UserExternalAccountChanges userRecord) throws SegueDatabaseException, MailjetException {
        Long userId = userRecord.getUserId();
        mailJetAPIUpdateUserProperties(mailjetId, userRecord.getGivenName(), userRecord.getRole().toString(), userRecord.getEmailVerificationStatus().toString());

        MailJetSubscriptionAction newsStatus = (userRecord.allowsNewsEmails() != null && userRecord.allowsNewsEmails()) ? MailJetSubscriptionAction.FORCE_SUBSCRIBE : MailJetSubscriptionAction.UNSUBSCRIBE;
        MailJetSubscriptionAction eventsStatus = (userRecord.allowsEventsEmails() != null && userRecord.allowsEventsEmails()) ? MailJetSubscriptionAction.FORCE_SUBSCRIBE : MailJetSubscriptionAction.UNSUBSCRIBE;
        mailJetAPIUpdateUserSubscriptions(mailjetId, newsStatus, eventsStatus);

        database.updateExternalAccount(userId, mailjetId);
    }

    private void deleteUserFromMailJet(final String mailjetId, final UserExternalAccountChanges userRecord) throws SegueDatabaseException, MailjetException {
        Long userId = userRecord.getUserId();
        mailJetAPIPermanentlyDeleteAccountById(mailjetId);
        // Do we need to keep this row? :/
        database.updateExternalAccount(userId, null);
    }

    private JSONObject mailJetAPIGetAccountByIdOrEmail(final String mailjetIdOrEmail) throws MailjetException {
        if (null == mailjetIdOrEmail) {
            return null;
        }
        MailjetRequest request = new MailjetRequest(Contact.resource, mailjetIdOrEmail);
        MailjetResponse response = mailjetClient.get(request);
        JSONArray responseData = response.getData();
        if (response.getTotal() == 1) {
            return responseData.getJSONObject(0);
        }
        return null;
    }

    private void mailJetAPIPermanentlyDeleteAccountById(final String mailjetId) throws MailjetException {
        Validate.notNull(mailjetId);
        MailjetRequest request = new MailjetRequest(Contacts.resource, mailjetId);
        MailjetResponse response = mailjetClient.delete(request);
        int responseStatus = response.getStatus();
        if (!(responseStatus == Response.Status.OK.getStatusCode())) {
            // TODO: Do we want to get any of the data from this request?
            throw new MailjetException("Failed to delete user!");
        }
    }

    private String mailJetAPIAddNewUser(final String email) throws MailjetException {
        if (null == email) {
            return null;
        }
        try {
            MailjetRequest request = new MailjetRequest(Contact.resource).property(Contact.EMAIL, email);
            MailjetResponse response = mailjetClient.post(request);
            // Get MailJet ID out:
            JSONObject responseData = response.getData().getJSONObject(0);
            return Integer.toString(responseData.getInt("ID"));
        } catch (MailjetClientRequestException e) {
            if (e.getMessage().contains("already exists")) {
                // FIXME - we need to test that this response always comes back with "already exists" in the message
                log.warn(String.format("Attempted to create a user with email (%s) that already existed!", email));
                JSONObject existingMailJetAccount = mailJetAPIGetAccountByIdOrEmail(email);
                return Integer.toString(existingMailJetAccount.getInt("ID"));
            } else {
                log.error(String.format("Failed to create user in MailJet with email: %s", email), e);
            }
        } catch (JSONException e) {
            log.error(String.format("Failed to create user in MailJet with email: %s", email), e);
        }
        return null;
    }

    private void mailJetAPIUpdateUserProperties(final String mailjetId, final String firstName, final String role,
                                                   final String email_verification_status) throws MailjetException {
        Validate.notNull(mailjetId);
        MailjetRequest request = new MailjetRequest(Contactdata.resource, mailjetId)
                .property(Contactdata.DATA, new JSONArray()
                        .put(new JSONObject().put("Name", "firstname").put("value", firstName))
                        .put(new JSONObject().put("Name", "role").put("value", role))
                        .put(new JSONObject().put("Name", "verification_status").put("value", email_verification_status))
                );
        MailjetResponse response = mailjetClient.put(request);
        if (response.getTotal() != 1) {
            // TODO: Do we want to get any of the data from this request?
            throw new MailjetException("Failed to update user!" + response.getTotal());
        }
    }

    private void mailJetAPIUpdateUserSubscriptions(final String mailjetId, final MailJetSubscriptionAction newsEmails,
                                                   final MailJetSubscriptionAction eventsEmails) throws MailjetException {
        Validate.notNull(mailjetId);
        MailjetRequest request = new MailjetRequest(ContactManagecontactslists.resource, mailjetId)
                .property(ContactManagecontactslists.CONTACTSLISTS, new JSONArray()
                        .put(new JSONObject()
                                .put(ContactslistImportList.LISTID, newsListId)
                                .put(ContactslistImportList.ACTION, newsEmails.value))
                        .put(new JSONObject()
                                .put(ContactslistImportList.LISTID, eventsListId)
                                .put(ContactslistImportList.ACTION, eventsEmails.value))
                );
        MailjetResponse response = mailjetClient.post(request);
        if (response.getTotal() != 1) {
            // TODO: Do we want to get any of the data from this request?
            throw new MailjetException("Failed to update user subscriptions!" + response.getTotal());
        }
    }
}
