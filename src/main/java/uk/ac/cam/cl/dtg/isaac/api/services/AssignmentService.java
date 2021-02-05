/**
 * Copyright 2021 Raspberry Pi Foundation
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
package uk.ac.cam.cl.dtg.isaac.api.services;

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.dto.IAssignmentLike;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAccountManager;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserException;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.UserSummaryDTO;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class AssignmentService {
    private static final Logger log = LoggerFactory.getLogger(AssignmentService.class);

    private final UserAccountManager userManager;

    @Inject
    public AssignmentService(final UserAccountManager userManager) {
        this.userManager = userManager;
    }

    public <T extends IAssignmentLike> void augmentAssignerSummaries(Collection<T> assignments) throws SegueDatabaseException {
        Map<Long, UserSummaryDTO> userSummaryCache = new HashMap<>();

        for (T assignment: assignments) {
            Long ownerUserId = assignment.getOwnerUserId();
            if (ownerUserId != null) {
                UserSummaryDTO userSummary = userSummaryCache.get(ownerUserId);
                if (userSummary == null) {
                    try {
                        RegisteredUserDTO user = userManager.getUserDTOById(ownerUserId);
                        userSummary = userManager.convertToUserSummaryObject(user);
                        userSummaryCache.put(ownerUserId, userSummary);
                    } catch (NoUserException e) {
                        log.warn("Assignment (" + assignment.getId() + " of class " + assignment.getClass().getSimpleName() + ") exists with owner user ID ("
                            + assignment.getOwnerUserId() + ") that does not exist!");
                    }
                }
                assignment.setAssignerSummary(userSummary);
            }
        }
    }
}
