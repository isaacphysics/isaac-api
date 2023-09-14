/**
 * Copyright 2021 Raspberry Pi Foundation
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

package uk.ac.cam.cl.dtg.isaac.api.services;

import com.google.inject.Inject;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.dto.IAssignmentLike;
import uk.ac.cam.cl.dtg.isaac.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.UserSummaryDTO;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAccountManager;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserException;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;

public class AssignmentService {
  private static final Logger log = LoggerFactory.getLogger(AssignmentService.class);

  private final UserAccountManager userManager;

  @Inject
  public AssignmentService(final UserAccountManager userManager) {
    this.userManager = userManager;
  }

  public <T extends IAssignmentLike> void augmentAssignerSummaries(final Collection<T> assignments)
      throws SegueDatabaseException {
    Map<Long, UserSummaryDTO> userSummaryCache = new HashMap<>();

    // Iterating over the owner IDs allows us to cache "no user found" errors without querying database each time:
    List<Long> ownerUserIds =
        assignments.stream().map(IAssignmentLike::getOwnerUserId).distinct().collect(Collectors.toList());
    for (Long ownerUserId : ownerUserIds) {
      try {
        RegisteredUserDTO user = userManager.getUserDTOById(ownerUserId);
        UserSummaryDTO userSummary = userManager.convertToUserSummaryObject(user);
        userSummaryCache.put(ownerUserId, userSummary);
      } catch (NoUserException e) {
        log.debug(String.format("Assignments exist with owner user ID (%s) that does not exist!", ownerUserId));
      }
    }

    for (T assignment : assignments) {
      Long ownerUserId = assignment.getOwnerUserId();
      if (ownerUserId != null) {
        assignment.setAssignerSummary(userSummaryCache.get(ownerUserId));
      }
    }
  }
}
