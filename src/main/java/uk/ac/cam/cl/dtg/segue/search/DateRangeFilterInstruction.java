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
package uk.ac.cam.cl.dtg.segue.search;

import java.util.Date;

import javax.annotation.Nullable;

/**
 * DateRangeFilterInstruction.
 * 
 * To be used to filter results based on a date range.
 * 
 * @author sac92
 */
public class DateRangeFilterInstruction extends AbstractFilterInstruction {
	private Date fromDate;
	private Date toDate;

	/**
	 * Create a new date range filter instruction.
	 * 
	 * At least one of the fields should be populated. Otherwise you will get an illegal arguments exception;
	 * 
	 * @param fromDate
	 *            the start date results should match.
	 * @param toDate
	 *            the end date that results can match.
	 */
	public DateRangeFilterInstruction(@Nullable final Date fromDate, @Nullable final Date toDate) {
		this.fromDate = fromDate;
		this.toDate = toDate;
		
		if (null == fromDate && null == toDate) {
			throw new IllegalArgumentException(
					"You must provide either a from date or a to date for this filter to work. "
					+ "Both are currently null");
		}
	}

	/**
	 * Gets the fromDate.
	 * 
	 * @return the fromDate
	 */
	public Date getFromDate() {
		return fromDate;
	}

	/**
	 * Gets the toDate.
	 * 
	 * @return the toDate
	 */
	public Date getToDate() {
		return toDate;
	}
}
