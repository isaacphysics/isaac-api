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
package uk.ac.cam.cl.dtg.segue.api.managers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.dtg.segue.dao.ILogManager;
import uk.ac.cam.cl.dtg.segue.dao.ResourceNotFoundException;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dao.content.IContentManager;
import uk.ac.cam.cl.dtg.segue.dao.schools.SchoolListReader;
import uk.ac.cam.cl.dtg.segue.dao.schools.UnableToIndexSchoolsException;
import uk.ac.cam.cl.dtg.segue.dos.QuestionValidationResponse;
import uk.ac.cam.cl.dtg.segue.dos.users.School;
import uk.ac.cam.cl.dtg.segue.dto.ResultsWrapper;
import uk.ac.cam.cl.dtg.segue.dto.content.ContentDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;
import static com.google.common.collect.Maps.immutableEntry;
import static uk.ac.cam.cl.dtg.isaac.api.Constants.*;
import static uk.ac.cam.cl.dtg.segue.api.Constants.*;

import com.google.api.client.util.Lists;
import com.google.api.client.util.Maps;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

/**
 * StatisticsManager.
 *
 */
public class StatisticsManager {
	private UserManager userManager;
	private ILogManager logManager;
	private SchoolListReader schoolManager;
	private ContentVersionController versionManager;
	private IContentManager contentManager;
	
	private static final Logger log = LoggerFactory.getLogger(StatisticsManager.class);
	
	/**
	 * StatisticsManager.
	 * @param userManager - to query user information
	 * @param logManager - to query Log information
	 * @param schoolManager - to query School information
	 * @param versionManager - to query live version information
	 * @param contentManager - to query content 
	 */
	@Inject
	public StatisticsManager(final UserManager userManager, final ILogManager logManager,
			final SchoolListReader schoolManager, final ContentVersionController versionManager,
			final IContentManager contentManager) {
		this.userManager = userManager;
		this.logManager = logManager;
		this.schoolManager = schoolManager;
		
		this.versionManager = versionManager;
		this.contentManager = contentManager;
	}

	/**
	 * Output general stats.
	 * 
	 * @return ImmutableMap<String, String> (stat name, stat value)
	 * @throws SegueDatabaseException 
	 */
	public ImmutableMap<String, String> outputGeneralStatistics() throws SegueDatabaseException {
		List<RegisteredUserDTO> users = userManager.findUsers(new RegisteredUserDTO());

		ImmutableMap.Builder<String, String> ib = new ImmutableMap.Builder<String, String>();

		List<RegisteredUserDTO> male = Lists.newArrayList();
		List<RegisteredUserDTO> female = Lists.newArrayList();
		List<RegisteredUserDTO> unknownGender = Lists.newArrayList();

		ib.put("totalUsers", "" + users.size());

		List<RegisteredUserDTO> studentOrUnknownRole = Lists.newArrayList();
		List<RegisteredUserDTO> teacherRole = Lists.newArrayList();
		List<RegisteredUserDTO> adminStaffRole = Lists.newArrayList();

		List<RegisteredUserDTO> hasSchool = Lists.newArrayList();
		List<RegisteredUserDTO> hasNoSchool = Lists.newArrayList();
		List<RegisteredUserDTO> hasOtherSchool = Lists.newArrayList();
		
		// build user stats
		for (RegisteredUserDTO user : users) {
			if (user.getGender() == null) {
				unknownGender.add(user);
			} else {
				switch (user.getGender()) {
					case MALE:
						male.add(user);
						break;
					case FEMALE:
						female.add(user);
						break;
					case OTHER:
						unknownGender.add(user);
						break;
					default:
						unknownGender.add(user);
						break;
				}

			}

			if (user.getRole() == null) {
				studentOrUnknownRole.add(user);
			} else {
				switch (user.getRole()) {
					case STUDENT:
						studentOrUnknownRole.add(user);
						break;
					case ADMIN:
						adminStaffRole.add(user);
						break;
					case CONTENT_EDITOR:
						adminStaffRole.add(user);
						break;
					case TEACHER:
						teacherRole.add(user);
						break;
					case TESTER:
						adminStaffRole.add(user);
						break;
					default:
						studentOrUnknownRole.add(user);
						break;
				}
			}
			
			if (user.getSchoolId() == null && user.getSchoolOther() == null) {
				hasNoSchool.add(user);
			} else {
				hasSchool.add(user);
				if (user.getSchoolOther() != null) {
					hasOtherSchool.add(user);
				}
			}
		}

		ib.put("maleUsers", "" + male.size());
		ib.put("femaleUsers", "" + female.size());
		ib.put("unknownGenderUsers", "" + unknownGender.size());

		ib.put("studentUsers", "" + studentOrUnknownRole.size());
		ib.put("teacherUsers", "" + teacherRole.size());
		ib.put("staffUsers", "" + adminStaffRole.size());

		ib.put("viewQuestionEvents", "" + logManager.getLogsByType(VIEW_QUESTION).size());
		ib.put("answeredQuestionEvents", "" + logManager.getLogsByType(ANSWER_QUESTION).size());
		
		ib.put("hasSchool", "" + hasSchool.size());
		ib.put("hasNoSchool", "" + hasNoSchool.size());
		ib.put("hasSchoolOther", "" + hasOtherSchool.size());
		
		Map<String, Date> lastSeenUserMap = this.getLastSeenUserMap();
		
		final int sevenDays = 7;
		final int thirtyDays = 30;
		
		List<RegisteredUserDTO> nonStaffUsers = Lists.newArrayList();
		nonStaffUsers.addAll(teacherRole);
		nonStaffUsers.addAll(studentOrUnknownRole);
		
		ib.put("activeTeachersLastWeek",
				"" + this.getNumberOfUsersActiveForLastNDays(teacherRole, lastSeenUserMap, sevenDays).size());
		ib.put("activeTeachersLastThirtyDays",
				"" + this.getNumberOfUsersActiveForLastNDays(teacherRole, lastSeenUserMap, thirtyDays).size());

		ib.put("activeStudentsLastWeek",
				""
						+ this.getNumberOfUsersActiveForLastNDays(studentOrUnknownRole, lastSeenUserMap,
								sevenDays).size());
		ib.put("activeStudentsLastThirtyDays",
				""
						+ this.getNumberOfUsersActiveForLastNDays(studentOrUnknownRole, lastSeenUserMap,
								thirtyDays).size());

		ib.put("activeUsersLastWeek",
				""
						+ this.getNumberOfUsersActiveForLastNDays(nonStaffUsers, lastSeenUserMap, sevenDays)
								.size());
		ib.put("activeUsersLastThirtyDays",
				""
						+ this.getNumberOfUsersActiveForLastNDays(nonStaffUsers, lastSeenUserMap, thirtyDays)
								.size());

		Map<String, Date> lastSeenUserMapQuestions = this.getLastSeenUserMap(ANSWER_QUESTION);

		ib.put("questionsAnsweredLastWeek",
				""
						+ this.getNumberOfUsersActiveForLastNDays(nonStaffUsers, lastSeenUserMapQuestions,
								sevenDays).size());
		ib.put("questionsAnsweredLastThirtyDays",
				""
						+ this.getNumberOfUsersActiveForLastNDays(nonStaffUsers, lastSeenUserMapQuestions,
								thirtyDays).size());
		
		// questions answered registered

		// questions answered teacher

		// questions answered student

		// questions answered unknown
		return ib.build();
	}

	/**
	 * Get the number of users per school.
	 * @return A map of schools to integers (representing the number of registered users)
	 * @throws UnableToIndexSchoolsException 
	 */
	public Map<School, Integer> getUsersBySchool() throws UnableToIndexSchoolsException {
		List<RegisteredUserDTO> users;
		Map<School, Integer> usersBySchool = Maps.newHashMap();
		
		try {
			users = userManager.findUsers(new RegisteredUserDTO());
			for (RegisteredUserDTO user : users) {
				if (user.getSchoolId() == null) {
					continue;
				}
				
				School s = schoolManager.findSchoolById(user.getSchoolId());
				if (s == null) {
					continue;
				}
				
				if (usersBySchool.containsKey(s)) {
					usersBySchool.put(s, usersBySchool.get(s) + 1); 
				} else {
					usersBySchool.put(s, 1); 
				}
			}
			
		} catch (SegueDatabaseException e) {
			log.error("Segue database error during school frequency calculation", e);
		}
		
		return usersBySchool;
	}
	
	/**
	 * Find all users belonging to a given school.
	 * 
	 * @param schoolId - that we are interested in.
	 * @return list of users.
	 * @throws SegueDatabaseException - if there is a general database error
	 * @throws ResourceNotFoundException - if we cannot locate the school requested.
	 * @throws UnableToIndexSchoolsException - if the school list has not been indexed.
	 */
	public List<RegisteredUserDTO> getUsersBySchoolId(final String schoolId)
		throws ResourceNotFoundException, SegueDatabaseException, UnableToIndexSchoolsException {
		Validate.notBlank(schoolId);
		
		List<RegisteredUserDTO> users = Lists.newArrayList();

		School s = schoolManager.findSchoolById(schoolId);
		
		if (null == s) {
			throw new ResourceNotFoundException("The school with the id provided cannot be found.");
		}
		
		RegisteredUserDTO prototype = new RegisteredUserDTO();
		prototype.setSchoolId(schoolId);
		
		users = userManager.findUsers(prototype);

		return users;
	}
	
	/**
	 * @return a list of userId's to last event timestamp
	 */
	public Map<String, Date> getLastSeenUserMap() {
		return this.getLastSeenUserMap(null);
	}
	
	/**
	 * @param qualifyingLogEvent the string event type that will be looked for.
	 * @return a list of userId's to last event timestamp
	 */
	public Map<String, Date> getLastSeenUserMap(final String qualifyingLogEvent) {
		return this.logManager.getLastAccessForAllUsers(qualifyingLogEvent);
	}
	
	/**
	 * getUserQuestionInformation. Produces a map that contains information
	 * about the total questions attempted, (correct and correct first time)
	 * "totalQuestionsAttempted", "totalCorrect",
	 * "totalCorrectFirstTime","attemptsByTag", questionAttemptsByLevelStats.
	 * 
	 * @param userOfInterest
	 *            - the user you wish to compile statistics for.
	 * @return gets high level statistics about the questions a user has
	 *         completed.
	 * @throws SegueDatabaseException
	 *             - if something went wrong with the database.
	 * @throws ContentManagerException
	 *             - if we are unable to look up the content.
	 */
	public Map<String, Object> getUserQuestionInformation(final RegisteredUserDTO userOfInterest)
		throws SegueDatabaseException, ContentManagerException {
		Validate.notNull(userOfInterest);
		
		// get questions answered correctly.
		int questionsAnsweredCorrectly = 0;
		
		// get total questions attempted
		int totalQuestionsAttempted = 0;
		
		// get total questions answered first time correctly
		int questionsFirstTime = 0;
		
		// question pages stats goes here
//		int totalQuestionPagesAttempted = 0;
//		int totalQuestionPagesCompleted = 0;
		
		Map<String, Map<String, List<QuestionValidationResponse>>> questionAttemptsByUser = userManager
				.getQuestionAttemptsByUser(userOfInterest);
		
		// all relevant question page info
		for (Entry<String, Map<String, List<QuestionValidationResponse>>> questionPage : questionAttemptsByUser
				.entrySet()) {
			// question page
//			totalQuestionPagesAttempted++;

			for (Entry<String, List<QuestionValidationResponse>> question : questionPage.getValue()
					.entrySet()) {
				totalQuestionsAttempted++;

				for (int i = 0; question.getValue().size() > i; i++) {
					// assumption that the order of the list is in chronological
					// order

					QuestionValidationResponse validationResponse = question.getValue().get(i);
					if (validationResponse.isCorrect() && i == 0) {
						questionsFirstTime++;
					}

					if (validationResponse.isCorrect()) {
						questionsAnsweredCorrectly++;
						break;
					}
				}
			}
		}

		// TODO this stuff should be tidied up and put somewhere else
		Map<String, ContentDTO> questionMap = this.getQuestionMap(questionAttemptsByUser.keySet());

		Map<String, Integer> questionAttemptsByTagStats = Maps.newHashMap();
		Map<String, Integer> questionAttemptsByLevelStats = Maps.newHashMap();

		for (Entry<String, Map<String, List<QuestionValidationResponse>>> question : questionAttemptsByUser
				.entrySet()) {
			// add the tags
			if (questionMap.get(question.getKey()) != null) {
				for (String tag : questionMap.get(question.getKey()).getTags()) {
					if (questionAttemptsByTagStats.containsKey(tag)) {
						questionAttemptsByTagStats.put(tag, questionAttemptsByTagStats.get(tag) + 1);
					} else {
						questionAttemptsByTagStats.put(tag, 1);
					}
				}				
			}

			ContentDTO questionContentDTO = questionMap.get(question.getKey());
			
			if (null == questionContentDTO) {
				continue;
			}
			
			String questionLevel = questionContentDTO.getLevel().toString();
			
			if (questionAttemptsByLevelStats.containsKey(questionLevel.toString())) {
				questionAttemptsByLevelStats.put(questionLevel.toString(),
						questionAttemptsByLevelStats.get(questionLevel.toString()) + 1);
			} else {
				questionAttemptsByLevelStats.put(questionLevel.toString(), 1);
			}
		}
		
		ImmutableMap<String, Object> immutableMap = new ImmutableMap.Builder<String, Object>()
				.put("totalQuestionsAttempted", totalQuestionsAttempted)
				.put("totalCorrect", questionsAnsweredCorrectly)
				.put("totalCorrectFirstTime", questionsFirstTime)
				.put("attemptsByTag", questionAttemptsByTagStats)
				.put("attemptsByLevel", questionAttemptsByLevelStats)
				.put("userDetails", this.userManager.convertToUserSummaryObject(userOfInterest)).build();
		
		return immutableMap;
	}
	
	/**
	 * Calculate the number of users from the list provided that meet the
	 * criteria.
	 * 
	 * @param users
	 *            - collection of users to consider.
	 * @param lastSeenUserMap
	 *            - The map of user event data. UserId --> last event date.
	 * @param daysFromToday
	 *            - the number of days from today that should be included in the
	 *            calculation e.g. 7 would be the last week's data.
	 * @return a collection containing the users who meet the criteria
	 */
	public Collection<RegisteredUserDTO> getNumberOfUsersActiveForLastNDays(final Collection<RegisteredUserDTO> users,
			final Map<String, Date> lastSeenUserMap, final int daysFromToday) {
		
		List<RegisteredUserDTO> qualifyingUsers = Lists.newArrayList();
		
		for (RegisteredUserDTO user : users) {
			Date eventDate = lastSeenUserMap.get(user.getDbId());
			Calendar validInclusionTime = Calendar.getInstance(); 
			validInclusionTime.setTime(new Date());
			validInclusionTime.add(Calendar.DATE, -1 * Math.abs(daysFromToday));
			
			if (eventDate != null && eventDate.after(validInclusionTime.getTime())) {
				qualifyingUsers.add(user);
			}
		}
		
		return qualifyingUsers;
	}
	
	/**
	 * Utility method to get a load of question pages by id in one go.
	 * 
	 * @param ids to search for
	 * @return map of id to content object.
	 * @throws ContentManagerException - if something goes wrong.
	 */
	private Map<String, ContentDTO> getQuestionMap(final Collection<String> ids) throws ContentManagerException {
		Map<Map.Entry<BooleanOperator, String>, List<String>> fieldsToMap = Maps.newHashMap();

		fieldsToMap.put(
				immutableEntry(BooleanOperator.OR, ID_FIELDNAME + '.'
						+ UNPROCESSED_SEARCH_FIELD_SUFFIX), new ArrayList<String>(ids));

		fieldsToMap.put(immutableEntry(BooleanOperator.OR, TYPE_FIELDNAME),
				Arrays.asList(QUESTION_TYPE, FAST_TRACK_QUESTION_TYPE));

		// Search for questions that match the ids.
		ResultsWrapper<ContentDTO> findByFieldNames = contentManager.findByFieldNames(
				versionManager.getLiveVersion(), fieldsToMap, 0, ids.size());

		List<ContentDTO> questionsForGameboard = findByFieldNames.getResults();

		Map<String, ContentDTO> questionIdToQuestionMap = Maps.newHashMap();
		for (ContentDTO content : questionsForGameboard) {
			if (content != null) {
				questionIdToQuestionMap.put(content.getId(), content);	
			}
		}
		
		return questionIdToQuestionMap;
	}
}
