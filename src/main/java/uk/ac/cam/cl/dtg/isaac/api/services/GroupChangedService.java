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

import static java.util.Objects.requireNonNull;
import static uk.ac.cam.cl.dtg.util.NameFormatter.getFilteredGroupNameFromGroup;
import static uk.ac.cam.cl.dtg.util.NameFormatter.getTeacherNameFromUser;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.api.managers.AssignmentManager;
import uk.ac.cam.cl.dtg.isaac.api.managers.QuizAssignmentManager;
import uk.ac.cam.cl.dtg.isaac.dto.AssignmentDTO;
import uk.ac.cam.cl.dtg.isaac.dto.IAssignmentLike;
import uk.ac.cam.cl.dtg.isaac.dto.QuizAssignmentDTO;
import uk.ac.cam.cl.dtg.isaac.dto.UserGroupDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.UserSummaryWithEmailAddressDTO;
import uk.ac.cam.cl.dtg.segue.api.managers.GroupManager;
import uk.ac.cam.cl.dtg.segue.api.managers.IGroupObserver;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAccountManager;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserException;
import uk.ac.cam.cl.dtg.segue.comm.EmailManager;
import uk.ac.cam.cl.dtg.segue.comm.EmailType;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.util.NameFormatter;

public class GroupChangedService implements IGroupObserver {
  private static final Logger log = LoggerFactory.getLogger(GroupChangedService.class);

  private final EmailManager emailManager;
  private final UserAccountManager userManager;
  private final AssignmentManager assignmentManager;
  private final QuizAssignmentManager quizAssignmentManager;

  @Inject
  public GroupChangedService(final EmailManager emailManager, final GroupManager groupManager,
                             final UserAccountManager userManager, final AssignmentManager assignmentManager,
                             final QuizAssignmentManager quizAssignmentManager) {
    this.emailManager = emailManager;
    this.userManager = userManager;
    this.assignmentManager = assignmentManager;
    this.quizAssignmentManager = quizAssignmentManager;

    log.info("Registering GroupChangeService ({})", this);

    groupManager.registerInterestInGroups(this);
  }

  @Override
  public void onGroupMembershipRemoved(final UserGroupDTO group, final RegisteredUserDTO user) {
    // Do nothing
  }

  @Override
  public void onMemberAddedToGroup(final UserGroupDTO group, final RegisteredUserDTO user) {
    requireNonNull(group);
    requireNonNull(user);

    // Try to email user to let them know
    try {
      emailManager.sendTemplatedEmailToUser(user,
          emailManager.getEmailTemplateDTO("email-template-group-welcome"),
          this.prepareGroupWelcomeEmailTokenMap(user, group),
          EmailType.SYSTEM);
    } catch (ContentManagerException e) {
      log.error("Could not send group welcome email due to a content error", e);
    } catch (SegueDatabaseException e) {
      log.error("Unable to send group welcome e-mail due to a database error. Failing silently.", e);
    }
  }

  /**
   * Helper to build up the list of tokens for addToGroup email.
   *
   * @param userDTO   - identity of user
   * @param userGroup - group being added to.
   * @return a map of string to string, with some values that may want to be shown in the email.
   * @throws SegueDatabaseException if we can't get the gameboard details.
   */
  private Map<String, Object> prepareGroupWelcomeEmailTokenMap(final RegisteredUserDTO userDTO,
                                                               final UserGroupDTO userGroup)
      throws SegueDatabaseException, ContentManagerException {
    requireNonNull(userDTO);

    UserSummaryWithEmailAddressDTO groupOwner = userGroup.getOwnerSummary();
    String groupOwnerName = getTeacherNameFromUser(groupOwner);

    String teacherInfo;
    if (!userGroup.getAdditionalManagers().isEmpty()) {
      teacherInfo = String.format("your teachers %s and %s",
          userGroup.getAdditionalManagers().stream().map(NameFormatter::getTeacherNameFromUser)
              .collect(Collectors.joining(", ")),
          groupOwnerName);
    } else {
      teacherInfo = String.format("your teacher %s", groupOwnerName);
    }

    String groupName = getFilteredGroupNameFromGroup(userGroup);

    StringBuilder htmlSB = new StringBuilder();
    StringBuilder plainTextSB = new StringBuilder();

    formatGroupAssignmentsInfo(userGroup, htmlSB, plainTextSB);

    return new ImmutableMap.Builder<String, Object>()
        .put("teacherName", groupOwnerName)
        .put("teacherInfo", teacherInfo)
        .put("groupName", groupName)
        .put("assignmentsInfo", plainTextSB.toString())
        .put("assignmentsInfo_HTML", htmlSB.toString())
        .build();
  }

  private void formatGroupAssignmentsInfo(final UserGroupDTO userGroup, final StringBuilder htmlSB,
                                          final StringBuilder plainTextSB)
      throws SegueDatabaseException, ContentManagerException {
    final List<AssignmentDTO> existingAssignments =
        this.assignmentManager.getAllAssignmentsForSpecificGroups(Collections.singletonList(userGroup), false);

    formatAssignmentLikeList(htmlSB, plainTextSB, existingAssignments, "assignments", assignmentManager);

    final List<QuizAssignmentDTO> existingQuizzes =
        this.quizAssignmentManager.getActiveAssignmentsForGroups(Collections.singletonList(userGroup));

    if (existingQuizzes != null && !existingQuizzes.isEmpty()) {
      htmlSB.append("<br>");
      plainTextSB.append("\n");
      formatAssignmentLikeList(htmlSB, plainTextSB, existingQuizzes, "tests", quizAssignmentManager);
    }
  }

  private <A extends IAssignmentLike> void formatAssignmentLikeList(
      final StringBuilder htmlSB, final StringBuilder plainTextSB, final List<A> existingAssignments,
      final String typeOfAssignment, final IAssignmentLike.Details<A> assignmentDetailsService)
      throws SegueDatabaseException, ContentManagerException {
    if (existingAssignments != null) {
      existingAssignments.sort(Comparator.comparing(IAssignmentLike::getCreationDate));
    }

    final DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
    if (existingAssignments != null && existingAssignments.size() > 0) {
      htmlSB.append("Your teacher has assigned the following " + typeOfAssignment + ":<br>");
      plainTextSB.append("Your teacher has assigned the following " + typeOfAssignment + ":\n");

      for (int i = 0; i < existingAssignments.size(); i++) {
        A existingAssignment = existingAssignments.get(i);
        Date assignmentStartDate = null;
        if (existingAssignment instanceof AssignmentDTO) {
          assignmentStartDate = ((AssignmentDTO) existingAssignment).getScheduledStartDate();
        }
        if (assignmentStartDate == null) {
          assignmentStartDate = existingAssignment.getCreationDate();
        }
        String name = assignmentDetailsService.getAssignmentLikeName(existingAssignment);
        String url = assignmentDetailsService.getAssignmentLikeUrl(existingAssignment);

        String dueDate = "";
        if (existingAssignment.getDueDate() != null) {
          dueDate = String.format(", due on %s", dateFormat.format(existingAssignment.getDueDate()));
        }

        htmlSB.append(String.format("%d. <a href='%s'>%s</a> (set on %s%s)<br>", i + 1, url,
            name, dateFormat.format(assignmentStartDate), dueDate));

        plainTextSB.append(String.format("%d. %s (set on %s%s)\n", i + 1, name,
            dateFormat.format(assignmentStartDate), dueDate));
      }
    } else if (existingAssignments != null) {
      htmlSB.append("No " + typeOfAssignment + " have been set yet.<br>");
      plainTextSB.append("No " + typeOfAssignment + " have been set yet.\n");
    }
  }


  @Override
  public void onAdditionalManagerAddedToGroup(final UserGroupDTO group, final RegisteredUserDTO additionalManagerUser) {
    requireNonNull(group);
    requireNonNull(additionalManagerUser);

    // Try to email user to let them know:
    try {
      RegisteredUserDTO groupOwner = this.userManager.getUserDTOById(group.getOwnerId());

      String groupOwnerName = getTeacherNameFromUser(groupOwner);
      String groupOwnerEmail = "Unknown";
      if (groupOwner != null && groupOwner.getEmail() != null && !groupOwner.getEmail().isEmpty()) {
        groupOwnerEmail = groupOwner.getEmail();
      }
      String groupName = "Unknown";
      if (group.getGroupName() != null && !group.getGroupName().isEmpty()) {
        groupName = group.getGroupName();
      }

      Map<String, Object> emailProperties = new ImmutableMap.Builder<String, Object>()
          .put("ownerName", groupOwnerName)
          .put("ownerEmail", groupOwnerEmail)
          .put("groupName", groupName)
          .build();
      emailManager.sendTemplatedEmailToUser(additionalManagerUser,
          emailManager.getEmailTemplateDTO("email-template-group-additional-manager-welcome"),
          emailProperties,
          EmailType.SYSTEM);

    } catch (ContentManagerException e) {
      log.info("Could not send group additional manager email ", e);
    } catch (NoUserException e) {
      log.info(String.format("Could not find owner user object of group %s", group.getId()), e);
    } catch (SegueDatabaseException e) {
      log.error("Unable to send group additional manager e-mail due to a database error. Failing silently.", e);
    }
  }

  @Override
  public void onAdditionalManagerPromotedToOwner(final UserGroupDTO group, final RegisteredUserDTO newOwner) {
    requireNonNull(group);
    requireNonNull(newOwner);

    String groupName = "Unknown";
    if (group.getGroupName() != null && !group.getGroupName().isEmpty()) {
      groupName = group.getGroupName();
    }

    Map<String, Object> emailProperties = new ImmutableMap.Builder<String, Object>()
        .put("groupName", groupName)
        .build();

    try {
      emailManager.sendTemplatedEmailToUser(newOwner,
          emailManager.getEmailTemplateDTO("email-template-group-manager-promoted"),
          emailProperties,
          EmailType.SYSTEM);
    } catch (ContentManagerException e) {
      log.info("Could not send group additional manager promotion to owner email ", e);
    } catch (SegueDatabaseException e) {
      log.error("Unable to send group additional manager promotion to owner e-mail due to a database error."
              + " Failing silently.", e);
    }
  }

  @Override
  public void onAdditionalManagerPrivilegesChanged(final UserGroupDTO group) {
    requireNonNull(group);

    try {
      RegisteredUserDTO groupOwner = this.userManager.getUserDTOById(group.getOwnerId());

      String groupOwnerName = getTeacherNameFromUser(groupOwner);
      String groupOwnerEmail = "Unknown";
      if (groupOwner != null && groupOwner.getEmail() != null && !groupOwner.getEmail().isEmpty()) {
        groupOwnerEmail = groupOwner.getEmail();
      }
      String groupName = "Unknown";
      if (group.getGroupName() != null && !group.getGroupName().isEmpty()) {
        groupName = group.getGroupName();
      }
      String nowOrNoLonger;
      if (group.isAdditionalManagerPrivileges()) {
        nowOrNoLonger = "now";
      } else {
        nowOrNoLonger = "no longer";
      }

      Map<String, Object> emailProperties = new ImmutableMap.Builder<String, Object>()
          .put("ownerName", groupOwnerName)
          .put("ownerEmail", groupOwnerEmail)
          .put("groupName", groupName)
          .put("nowOrNoLonger", nowOrNoLonger)
          .build();
      for (Long additionalManagerId : group.getAdditionalManagersUserIds()) {
        RegisteredUserDTO additionalManagerUser = userManager.getUserDTOById(additionalManagerId);
        emailManager.sendTemplatedEmailToUser(additionalManagerUser,
            emailManager.getEmailTemplateDTO("email-template-group-manager-privileges"),
            emailProperties,
            EmailType.SYSTEM);
      }
    } catch (ContentManagerException e) {
      log.info("Could not send group additional manager privileges modified email ", e);
    } catch (NoUserException e) {
      log.info(String.format("Could not find owner user object of group %s", group.getId()), e);
    } catch (SegueDatabaseException e) {
      log.error("Unable to send group additional manager privileges modified e-mail due to a database error."
              + " Failing silently.", e);
    }
  }
}
