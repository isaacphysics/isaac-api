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
package uk.ac.cam.cl.dtg.segue.auth.exceptions;

/**
 * An exception to indicate that we suspect CSRF to have happened.
 * 
 * @author Stephen Cummins
 * 
 */
public class CrossSiteRequestForgeryException extends Exception {
	private static final long serialVersionUID = -8542483814754486874L;

	/**
	 * Create a CSRF Exception.
	 * 
	 * @param msg
	 *            - message to attach to the exception.
	 */
	public CrossSiteRequestForgeryException(final String msg) {
		super(msg);
	}
}
