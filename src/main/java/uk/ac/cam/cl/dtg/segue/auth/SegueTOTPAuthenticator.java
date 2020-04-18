/*
 * Copyright 2020 Stephen Cummins
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
package uk.ac.cam.cl.dtg.segue.auth;

import com.google.inject.Inject;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.IncorrectCredentialsProvidedException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoCredentialsAvailableException;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.users.ITOTPDataManager;
import uk.ac.cam.cl.dtg.segue.dos.users.TOTPSharedSecret;
import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;

import java.util.Date;

public class SegueTOTPAuthenticator implements ISecondFactorAuthenticator {
    private final ITOTPDataManager dataManager;
    private final GoogleAuthenticator gAuth;

    @Inject
    public SegueTOTPAuthenticator(final ITOTPDataManager dataManager) {
        this.dataManager = dataManager;
        gAuth = new GoogleAuthenticator();
    }

    @Override
    public TOTPSharedSecret getNewSharedSecret(final RegisteredUserDTO user) {
        final GoogleAuthenticatorKey key = gAuth.createCredentials();

        TOTPSharedSecret toReturn = new TOTPSharedSecret();
        toReturn.setUserId(user.getId());
        toReturn.setSharedSecret(key.getKey());
        toReturn.setCreated(new Date());

        return toReturn;
    }

    @Override
    public boolean has2FAConfigured(final RegisteredUserDTO user) throws SegueDatabaseException {
        return null != dataManager.get2FASharedSecret(user.getId());
    }

    @Override
    public boolean activate2FAForUser(final RegisteredUserDTO user, final String sharedSecret, final Integer verificationCode) throws SegueDatabaseException {
        TOTPSharedSecret toSave = new TOTPSharedSecret();
        toSave.setUserId(user.getId());
        toSave.setSharedSecret(sharedSecret);
        toSave.setCreated(new Date());

        if (gAuth.authorize(sharedSecret, verificationCode)) {
            this.dataManager.save2FASharedSecret(user.getId(), toSave);
            return true;
        }

        return false;
    }

    @Override
    public boolean authenticate2ndFactor(final RegisteredUserDTO user, final Integer verificationCode) throws IncorrectCredentialsProvidedException, NoCredentialsAvailableException, SegueDatabaseException {
        TOTPSharedSecret storedSharedSecret = this.dataManager.get2FASharedSecret(user.getId());

        if (null == storedSharedSecret) {
            throw new NoCredentialsAvailableException("Unable to find 2FA shared secret.");
        }

        if (this.gAuth.authorize(storedSharedSecret.getSharedSecret(), verificationCode)) {
            return true;
        }

        throw new IncorrectCredentialsProvidedException("2FA code provided by the user is not correct");
    }
}
