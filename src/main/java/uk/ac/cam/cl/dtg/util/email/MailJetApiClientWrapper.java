/*
 * Copyright 2021 James Sharkey
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
package uk.ac.cam.cl.dtg.util.email;

import com.google.inject.Inject;
import com.mailjet.client.ClientOptions;
import com.mailjet.client.MailjetClient;
import com.mailjet.client.MailjetRequest;
import com.mailjet.client.MailjetResponse;
import com.mailjet.client.errors.MailjetClientRequestException;
import com.mailjet.client.errors.MailjetException;
import com.mailjet.client.resource.Contact;
import com.mailjet.client.resource.ContactManagecontactslists;
import com.mailjet.client.resource.Contactdata;
import com.mailjet.client.resource.Contacts;
import com.mailjet.client.resource.ContactslistImportList;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class MailJetApiClientWrapper {

    private static final Logger log = LoggerFactory.getLogger(MailJetApiClientWrapper.class);
    private final MailjetClient mailjetClient;
    private final String newsListId;
    private final String eventsListId;
    private final String legalListId;

    /**
     *  Wrapper for MailjetClient class.
     *
     *  @param mailjetApiKey - MailJet API Key
     *  @param mailjetApiSecret - MailJet API Client Secret
     *  @param mailjetNewsListId - MailJet list ID for NEWS_AND_UPDATES
     *  @param mailjetEventsListId - MailJet list ID for EVENTS
     *  @param mailjetLegalListId - MailJet list ID for legal notices (all users)
     */
    @Inject
    public MailJetApiClientWrapper(final String mailjetApiKey, final String mailjetApiSecret,
                                   final String mailjetNewsListId, final String mailjetEventsListId,
                                   final String mailjetLegalListId) {
        ClientOptions options = ClientOptions.builder()
                .apiKey(mailjetApiKey)
                .apiSecretKey(mailjetApiSecret)
                .build();

        this.mailjetClient = new MailjetClient(options);
        this.newsListId = mailjetNewsListId;
        this.eventsListId = mailjetEventsListId;
        this.legalListId = mailjetLegalListId;
    }

    /**
     *  Get user details for an existing MailJet account
     *
     * @param mailjetIdOrEmail - email address or MailJet user ID
     * @return JSONObject of the MailJet user
     * @throws MailjetException  - if underlying MailjetClient throws an exception
     */
    public JSONObject getAccountByIdOrEmail(final String mailjetIdOrEmail) throws MailjetException {
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

    /**
     *  Perform an asynchronous GDPR-compliant deletion of a MailJet user.
     *
     * @param mailjetId - MailJet user ID
     * @throws MailjetException  - if underlying MailjetClient throws an exception
     */
    public void permanentlyDeleteAccountById(final String mailjetId) throws MailjetException {
        Objects.requireNonNull(mailjetId);
        MailjetRequest request = new MailjetRequest(Contacts.resource, mailjetId);
        mailjetClient.delete(request);
    }

    /**
     *  Add a new user to MailJet
     *
     *  If the user already exists, find by email as a fallback to ensure idempotence and better error recovery.
     *
     * @param email - email address
     * @return the MailJet user ID
     * @throws MailjetException  - if underlying MailjetClient throws an exception
     */
    public String addNewUserOrGetUserIfExists(final String email) throws MailjetException {
        if (null == email) {
            return null;
        }
        try {
            MailjetRequest request = new MailjetRequest(Contact.resource).property(Contact.EMAIL, email);
            MailjetResponse response = mailjetClient.post(request);
            // Get MailJet ID out:
            JSONObject responseData = response.getData().getJSONObject(0);
            return Long.toString(responseData.getLong("ID"));
        } catch (MailjetClientRequestException e) {
            if (e.getMessage().contains("already exists")) {
                // FIXME - we need to test that this response always comes back with "already exists" in the message
                log.warn(String.format("Attempted to create a user with email (%s) that already existed!", email));
                JSONObject existingMailJetAccount = getAccountByIdOrEmail(email);
                return Long.toString(existingMailJetAccount.getLong("ID"));
            } else {
                log.error(String.format("Failed to create user in MailJet with email: %s", email), e);
            }
        } catch (JSONException e) {
            log.error(String.format("Failed to create user in MailJet with email: %s", email), e);
        }
        return null;
    }

    /**
     *  Update user details for an existing MailJet account
     *
     * @param mailjetId - MailJet user ID
     * @throws MailjetException  - if underlying MailjetClient throws an exception
     */
    public void updateUserProperties(final String mailjetId, final String firstName, final String role,
                                     final String emailVerificationStatus, final String countryCode,
                                     final String stages) throws MailjetException {
        Objects.requireNonNull(mailjetId);
        MailjetRequest request = new MailjetRequest(Contactdata.resource, mailjetId)
                .property(Contactdata.DATA, new JSONArray()
                        .put(new JSONObject().put("Name", "firstname").put("value", firstName))
                        .put(new JSONObject().put("Name", "role").put("value", role))
                        .put(new JSONObject().put("Name", "verification_status").put("value", emailVerificationStatus))
                        .put(new JSONObject().put("Name", "country").put("value", countryCode))
                        .put(new JSONObject().put("Name", "stages").put("value", stages))
                );
        MailjetResponse response = mailjetClient.put(request);
        if (response.getTotal() != 1) {
            // TODO: Do we want to get any of the data from this request?
            throw new MailjetException("Failed to update user!" + response.getTotal());
        }
    }

    /**
     *  Update user list subscriptions for an existing MailJet account
     *
     * @param mailjetId - MailJet user ID
     * @throws MailjetException  - if underlying MailjetClient throws an exception
     */
    public void updateUserSubscriptions(final String mailjetId, final MailJetSubscriptionAction newsEmails,
                                         final MailJetSubscriptionAction eventsEmails) throws MailjetException {
        Objects.requireNonNull(mailjetId);
        MailjetRequest request = new MailjetRequest(ContactManagecontactslists.resource, mailjetId)
                .property(ContactManagecontactslists.CONTACTSLISTS, new JSONArray()
                        .put(new JSONObject()
                                .put(ContactslistImportList.LISTID, legalListId)
                                .put(ContactslistImportList.ACTION, MailJetSubscriptionAction.FORCE_SUBSCRIBE.value))
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
