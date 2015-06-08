/**
 * Copyright 2015 Stephen Cummins
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
package uk.ac.cam.cl.dtg.segue.api.monitors;

/**
 * Represents an event that may or may not be considered misuse of a server provided resource.
 * 
 */
public interface IMisuseHandler {
	
	/**
	 * Get the number of times we can see this event before the warning condition is fired.
	 * 
	 * @return the trigger number for a warning. Can be null
	 */
	Integer getSoftThreshold();
	
	/**
	 * Get the number of times we can see this event before a security exception is thrown.
	 * 
	 * @return the trigger threshold for an error. Can be null.
	 */
	Integer getHardThreshold(); 
	
	/**
	 * The accounting interval represents when we reset our internal counter.
	 * 
	 * @return an integer value in seconds. 
	 */
	Integer getAccountingIntervalInSeconds();
	
	/**
	 * Optional method to execute when soft threshold has been reached.
	 * @param message - human readable input to the action - optional
	 */
	void executeSoftThresholdAction(final String message);

	/**
     * Optional method to execute when hard threshold has been reached before exception is thrown.
     * @param message - human readable input to the action - optional
	 */
	void executeHardThresholdAction(final String message);
}
