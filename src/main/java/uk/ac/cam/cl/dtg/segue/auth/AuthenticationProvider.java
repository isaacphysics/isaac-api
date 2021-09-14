/**
 * Copyright 2014 Stephen Cummins
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

/**
 * Enum to represent the different authentication providers Segue supports.
 * 
 * This is used for identification purposes and to match api requests based on
 * the string values of the enum.
 * 
 * (I.e. if you want to authenticate against the GOOGLE provider you may pass
 * the string google to the api and this could be matched against the enum value
 * (ignoring case).
 */
public enum AuthenticationProvider {
	GOOGLE, FACEBOOK, TWITTER, RAVEN, TEST, SEGUE
}
