/**
 * Copyright 2015 Stephen Cummins
 * <br>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * <br>
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * <br>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.ac.cam.cl.dtg.segue.search;

import jakarta.annotation.Nullable;
import java.time.Instant;

/**
 * DateRangeFilterInstruction.
 * <br>
 * To be used to filter results based on a date range.
 *
 * @author sac92
 */
public class DateRangeFilterInstruction extends AbstractFilterInstruction {
  private Instant fromDate;
  private Instant toDate;

  /**
   * Create a new date range filter instruction.
   * <br>
   * At least one of the fields should be populated. Otherwise you will get an illegal arguments exception;
   *
   * @param fromDate
   *            the start date results should match.
   * @param toDate
   *            the end date that results can match.
   */
  public DateRangeFilterInstruction(@Nullable final Instant fromDate, @Nullable final Instant toDate) {
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
  public final Instant getFromDate() {
    return fromDate;
  }

  /**
   * Gets the toDate.
   *
   * @return the toDate
   */
  public final Instant getToDate() {
    return toDate;
  }
}
