/*
 * Copyright 2019 Stephen Cummins
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
package uk.ac.cam.cl.dtg.segue.dto.users;

import com.fasterxml.jackson.annotation.JsonProperty;
import uk.ac.cam.cl.dtg.segue.auth.AuthenticationProvider;

import java.util.List;

/**
 * Data Transfer Object to represent the user authentication settings.
 * 
 */
public class UserAuthenticationSettingsDTO extends AbstractSegueUserDTO {
    private Long id;

    private List<AuthenticationProvider> linkedAccounts;
    private boolean hasSegueAccount;

    /**
     * Create a UserAuthenticationSettings DTO
     *
     * @param id - user id
     * @param linkedAccounts - The list of linked accounts the user has setup
     * @param hasSegueAccount - boolean whether or not they have a segue account
     */
    public UserAuthenticationSettingsDTO(Long id, List<AuthenticationProvider> linkedAccounts, boolean hasSegueAccount) {
        this.id = id;
        this.linkedAccounts = linkedAccounts;
        this.hasSegueAccount = hasSegueAccount;
    }

    /**
     * Default constructor required for Jackson.
     */
    public UserAuthenticationSettingsDTO() {

    }

    /**
     * Gets the id.
     * @return the id
     */
    @JsonProperty("id")
    public Long getId() {
        return id;
    }


    /**
     * Sets the id.
     * @param id the id to set
     */
    @JsonProperty("id")
    public void setId(final Long id) {
        this.id = id;
    }


    /**
     * Gets the linkedAccounts.
     * 
     * @return the linkedAccounts
     */
    public List<AuthenticationProvider> getLinkedAccounts() {
        return linkedAccounts;
    }

    /**
     * Sets the linkedAccounts.
     * 
     * @param linkedAccounts
     *            the linkedAccounts to set
     */
    public void setLinkedAccounts(final List<AuthenticationProvider> linkedAccounts) {
        this.linkedAccounts = linkedAccounts;
    }

    /**
     * Gets the hasSegueAccount.
     * 
     * @return the hasSegueAccount
     */
    public boolean getHasSegueAccount() {
        return hasSegueAccount;
    }

    /**
     * Sets the hasSegueAccount.
     * 
     * @param hasSegueAccount
     *            the hasSegueAccount to set
     */
    public void setHasSegueAccount(final boolean hasSegueAccount) {
        this.hasSegueAccount = hasSegueAccount;
    }
}
