/**
 * Copyright 2020 Connor Holloway
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
package uk.ac.cam.cl.dtg.segue.dto;

/**
 * The DTO which is used to transfer the response to a MFA challenge.
 */
public class MFAResponseDTO {
    private String mfaVerificationCode;
    private boolean rememberMe;

    /**
     * Default constructor.
     */
    public MFAResponseDTO() {}

    public Boolean getRememberMe() {
        return rememberMe;
    }

    public void setRememberMe(Boolean rememberMe) {
        this.rememberMe = rememberMe;
    }

    public String getMfaVerificationCode() {
        return mfaVerificationCode;
    }

    public void setMfaVerificationCode(String mfaVerificationCode) {
        this.mfaVerificationCode = mfaVerificationCode;
    }
}
