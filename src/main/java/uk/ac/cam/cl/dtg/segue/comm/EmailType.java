/**
 * Copyright 2015 Alistair Stead
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

package uk.ac.cam.cl.dtg.segue.comm;

/**
 * The types of email we can send. 
 *
 * @author Alistair Stead
 *
 */
public enum EmailType {
  ADMIN(0),
  SYSTEM(1),
  ASSIGNMENTS(2),
  NEWS_AND_UPDATES(3),
  EVENTS(4);

  private final int priority;

  EmailType(final int priority) {
    this.priority = priority;
  }

  /**
   * Get the integer value corresponding to the priority of the email type.
   *
   * @return integer representation of priority
   */
  public int getPriority() {
    return switch (this) {
      case ADMIN, SYSTEM, ASSIGNMENTS, NEWS_AND_UPDATES, EVENTS -> this.priority;
      default -> Integer.MAX_VALUE;
    };
  }

  /**
   * Check whether the email type can be controlled through preferences. Admin and System emails are always valid,
   * while other types can be disabled by users.
   *
   * @return boolean giving the validity of email type as email preference
   */
  public boolean isValidEmailPreference() {
    return switch (this) {
      case ADMIN, SYSTEM -> false;
      case ASSIGNMENTS, NEWS_AND_UPDATES, EVENTS -> true;
      default -> false;
    };
  }


}
