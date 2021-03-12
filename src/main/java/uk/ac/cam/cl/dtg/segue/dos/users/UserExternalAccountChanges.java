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
package uk.ac.cam.cl.dtg.segue.dos.users;

/**
 *  A class to represent the state of a user that needs synchronising with an external provider.
 */
public class UserExternalAccountChanges {

    private Long userId;
    private String providerUserId;
    private String accountEmail;
    private Role role;
    private String givenName;
    private Boolean deleted;
    private EmailVerificationStatus emailVerificationStatus;
    private Boolean allowsNewsEmails;
    private Boolean allowsEventsEmails;

    public UserExternalAccountChanges(Long userId, String providerUserId, String accountEmail, Role role,
                                      String givenName, Boolean deleted, EmailVerificationStatus emailVerificationStatus,
                                      Boolean allowsNewsEmails, Boolean allowsEventsEmails) {
        this.userId = userId;
        this.providerUserId = providerUserId;
        this.accountEmail = accountEmail;
        this.role = role;
        this.givenName = givenName;
        this.deleted = deleted;
        this.emailVerificationStatus = emailVerificationStatus;
        this.allowsNewsEmails = allowsNewsEmails;
        this.allowsEventsEmails = allowsEventsEmails;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getProviderUserId() {
        return providerUserId;
    }

    public void setProviderUserId(String providerUserId) {
        this.providerUserId = providerUserId;
    }

    public String getAccountEmail() {
        return accountEmail;
    }

    public void setAccountEmail(String accountEmail) {
        this.accountEmail = accountEmail;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getGivenName() {
        return givenName;
    }

    public void setGivenName(String givenName) {
        this.givenName = givenName;
    }

    public Boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }

    public EmailVerificationStatus getEmailVerificationStatus() {
        return emailVerificationStatus;
    }

    public void setEmailVerificationStatus(EmailVerificationStatus emailVerificationStatus) {
        this.emailVerificationStatus = emailVerificationStatus;
    }

    public Boolean allowsNewsEmails() {
        return allowsNewsEmails;
    }

    public void setAllowsNewsEmails(Boolean allowsNewsEmails) {
        this.allowsNewsEmails = allowsNewsEmails;
    }

    public Boolean allowsEventsEmails() {
        return allowsEventsEmails;
    }

    public void setAllowsEventsEmails(Boolean allowsEventsEmails) {
        this.allowsEventsEmails = allowsEventsEmails;
    }

}
