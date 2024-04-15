/**
 * Copyright 2014 Stephen Cummins
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

package uk.ac.cam.cl.dtg.segue.api.managers;

import static java.util.Objects.requireNonNull;
import static uk.ac.cam.cl.dtg.segue.api.monitors.SegueMetrics.VALIDATOR_LATENCY_HISTOGRAM;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.google.api.client.util.Lists;
import com.google.api.client.util.Maps;
import com.google.inject.Inject;
import com.google.inject.Injector;
import io.prometheus.client.Histogram;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.dos.AbstractUserPreferenceManager;
import uk.ac.cam.cl.dtg.isaac.dos.LightweightQuestionValidationResponse;
import uk.ac.cam.cl.dtg.isaac.dos.QuestionValidationResponse;
import uk.ac.cam.cl.dtg.isaac.dos.TestCase;
import uk.ac.cam.cl.dtg.isaac.dos.TestQuestion;
import uk.ac.cam.cl.dtg.isaac.dos.UserPreference;
import uk.ac.cam.cl.dtg.isaac.dos.content.Choice;
import uk.ac.cam.cl.dtg.isaac.dos.content.ChoiceQuestion;
import uk.ac.cam.cl.dtg.isaac.dos.content.Content;
import uk.ac.cam.cl.dtg.isaac.dos.content.Question;
import uk.ac.cam.cl.dtg.isaac.dos.users.Role;
import uk.ac.cam.cl.dtg.isaac.dos.users.UserContext;
import uk.ac.cam.cl.dtg.isaac.dto.GameFilter;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacItemQuestionDTO;
import uk.ac.cam.cl.dtg.isaac.dto.QuestionValidationResponseDTO;
import uk.ac.cam.cl.dtg.isaac.dto.ResultsWrapper;
import uk.ac.cam.cl.dtg.isaac.dto.SegueErrorResponse;
import uk.ac.cam.cl.dtg.isaac.dto.content.ChoiceDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.ChoiceQuestionDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentBaseDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.QuestionDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.SeguePageDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.AbstractSegueUserDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.AnonymousUserDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.isaac.mappers.MainObjectMapper;
import uk.ac.cam.cl.dtg.isaac.quiz.IQuestionAttemptManager;
import uk.ac.cam.cl.dtg.isaac.quiz.ISpecifier;
import uk.ac.cam.cl.dtg.isaac.quiz.IValidator;
import uk.ac.cam.cl.dtg.isaac.quiz.SpecifiesWith;
import uk.ac.cam.cl.dtg.isaac.quiz.ValidatesWith;
import uk.ac.cam.cl.dtg.isaac.quiz.ValidatorUnavailableException;
import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.api.Constants.TimeInterval;
import uk.ac.cam.cl.dtg.segue.api.ErrorResponseWrapper;
import uk.ac.cam.cl.dtg.segue.configuration.SegueGuiceConfigurationModule;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentMapperUtils;
import uk.ac.cam.cl.dtg.util.QueryUtils;

/**
 * This class is responsible for validating correct answers using the ValidatesWith annotation when it is applied on to
 * Questions.
 * <br>
 * It is also responsible for orchestrating question attempt persistence.
 */
public class QuestionManager {
  private static final Logger log = LoggerFactory.getLogger(QuestionManager.class);

  private final ContentMapperUtils mapperUtils;
  private final MainObjectMapper objectMapper;
  private final IQuestionAttemptManager questionAttemptPersistenceManager;
  private final AbstractUserPreferenceManager userPreferenceManager;

  /**
   * Create a default Question manager object.
   *
   * @param mapperUtils                - an auto mapper to allow us to convert to and from QuestionValidationResponseDOs
   *                                   and DTOs.
   * @param questionPersistenceManager - for question attempt persistence.
   * @param userPreferenceManager      - An instance of the Abstract User preference manager to check for user
   *                                   preferences
   */
  @Inject
  public QuestionManager(final ContentMapperUtils mapperUtils, final MainObjectMapper objectMapper,
                         final IQuestionAttemptManager questionPersistenceManager,
                         final AbstractUserPreferenceManager userPreferenceManager) {
    this.mapperUtils = mapperUtils;
    this.objectMapper = objectMapper;
    this.questionAttemptPersistenceManager = questionPersistenceManager;
    this.userPreferenceManager = userPreferenceManager;
  }

  /**
   * Reflection to try and determine the associated validator for the question being answered.
   *
   * @param questionType - the type of question being answered.
   * @return a Validator
   */
  @SuppressWarnings("unchecked")
  private static IValidator locateValidator(final Class<? extends Question> questionType) {
    // check we haven't gone too high up the superclass tree
    if (!Question.class.isAssignableFrom(questionType)) {
      return null;
    }

    // Does this class have the correct annotation?
    if (questionType.isAnnotationPresent(ValidatesWith.class)) {

      log.debug("Validator for question validation found. Using : {}",
          questionType.getAnnotation(ValidatesWith.class).value());
      Injector injector = SegueGuiceConfigurationModule.getGuiceInjector();
      return injector.getInstance(questionType.getAnnotation(ValidatesWith.class).value());

    } else if (questionType.equals(Question.class)) {
      // so if we get here then we haven't found a ValidatesWith class, so
      // we should just give up and return null.
      return null;
    }

    // we will continue our search of the superclasses for the annotation
    return locateValidator((Class<? extends Question>) questionType.getSuperclass());
  }

  /**
   * Extract all of the question objects, recursively, from some content.
   *
   * @param content - The contentDTO which may have question objects as children.
   * @return A list of QuestionDTO found in the content.
   */
  public static List<QuestionDTO> extractQuestionObjects(final ContentDTO content) {
    return QuestionManager.extractQuestionObjectsRecursively(content,
        new ArrayList<>());
  }

  /**
   * Extract all of the questionObjectsRecursively.
   *
   * @param toExtract - The contentDTO which may have question objects as children.
   * @param result    - The initially empty List which will be mutated to contain references to all of the question
   *                  objects.
   * @return The modified result array.
   */
  private static List<QuestionDTO> extractQuestionObjectsRecursively(final ContentDTO toExtract,
                                                                     final List<QuestionDTO> result) {
    if (toExtract instanceof QuestionDTO) {
      // we found a question so add it to the list.
      result.add((QuestionDTO) toExtract);
    }

    if (toExtract.getChildren() != null) {
      // Go through each child in the content object.
      for (ContentBaseDTO child : toExtract.getChildren()) {
        if (child instanceof ContentDTO) {
          // if it is not a question but it can have children then
          // continue recursing.
          ContentDTO childContent = (ContentDTO) child;
          if (childContent.getChildren() != null) {
            QuestionManager.extractQuestionObjectsRecursively(childContent, result);
          }
        }
      }
    }

    return result;
  }

  public static String extractPageIdFromQuestionId(final String questionId) {
    return questionId.split(Constants.ESCAPED_ID_SEPARATOR)[0];
  }

  /**
   * Validate client answer to recorded answer.
   *
   * @param question        The question to which the answer must be validated against.
   * @param submittedAnswer from the client as a DTO for comparison.
   * @return A response containing a QuestionValidationResponse object.
   */
  public final Response validateAnswer(final Question question, final ChoiceDTO submittedAnswer) {
    IValidator validator = locateValidator(question.getClass());

    if (null == validator) {
      log.error("Unable to locate a valid validator for this question {}", question.getId());
      return Response.serverError()
          .entity("Unable to detect question validator for " + "this object. Unable to verify answer")
          .build();
    }

    Choice answerFromUser = objectMapper.mapChoice(submittedAnswer);
    QuestionValidationResponse validateQuestionResponse;
    Histogram.Timer validatorTimer =
        VALIDATOR_LATENCY_HISTOGRAM.labels(validator.getClass().getSimpleName()).startTimer();
    try {
      validateQuestionResponse = validator.validateQuestionResponse(question, answerFromUser);
    } catch (ValidatorUnavailableException e) {
      return SegueErrorResponse.getServiceUnavailableResponse(e.getClass().getSimpleName() + ": "
          + e.getMessage());
    } finally {
      validatorTimer.observeDuration();
    }

    return Response.ok(objectMapper.map(validateQuestionResponse)).build();
  }

  /**
   * Reflection to try and determine the associated specifier for the choice given.
   *
   * @param choiceClass - the type of choice given.
   * @return a Validator
   */
  @SuppressWarnings("unchecked")
  private ISpecifier locateSpecifier(final Class<? extends ChoiceDTO> choiceClass) {
    // check we haven't gone too high up the superclass tree
    if (!ChoiceDTO.class.isAssignableFrom(choiceClass)) {
      return null;
    }

    // Does this class have the correct annotation?
    if (choiceClass.isAnnotationPresent(SpecifiesWith.class)) {

      log.debug("Specifier for specification creation found. Using : {}",
          choiceClass.getAnnotation(SpecifiesWith.class).value());
      Injector injector = SegueGuiceConfigurationModule.getGuiceInjector();
      return injector.getInstance(choiceClass.getAnnotation(SpecifiesWith.class).value());

    } else if (choiceClass.equals(ChoiceDTO.class)) {
      // so if we get here then we haven't found a SpecifiesWith class, so
      // we should just give up and return null.
      return null;
    }

    // we will continue our search of the superclasses for the annotation
    return locateSpecifier((Class<? extends ChoiceDTO>) choiceClass.getSuperclass());
  }

  /**
   * This method will ensure any user question attempt information available is used to augment this question object.
   * <br>
   * It will also ensure that any personalisation of questions is affected (e.g. randomised multichoice elements).
   * <br>
   * Note: It will not do anything to related content
   *
   * @param page                  - to augment - this object may be mutated as a result of this method. i.e. BestAttempt
   *                              field set on question DTOs.
   * @param userId                - to allow us to provide a per-user experience of question configuration (random seed)
   * @param usersQuestionAttempts - as a map of QuestionPageId to Map of QuestionId to QuestionValidationResponseDO
   * @return augmented page - the return result is by convenience as the page provided as a parameter will be mutated.
   */
  public SeguePageDTO augmentQuestionObjects(
      final SeguePageDTO page, final String userId,
      final Map<String, Map<String, List<QuestionValidationResponse>>> usersQuestionAttempts) {

    List<QuestionDTO> questionsToAugment = extractQuestionObjects(page);

    this.augmentQuestionObjectWithAttemptInformation(page, questionsToAugment, usersQuestionAttempts);

    shuffleChoiceQuestionsChoices(userId, questionsToAugment);

    return page;
  }

  /**
   * Modify a question objects in a page such that it contains bestAttempt information if we can provide it.
   *
   * @param page                  - the page this object may be mutated as a result of this method. i.e. BestAttempt
   *                              field set on question DTOs.
   * @param questionsToAugment    - The flattened list of questions which should be augmented.
   * @param usersQuestionAttempts - as a map of QuestionPageId to Map of QuestionId to QuestionValidationResponseDO
   * @return augmented page - the return result is by convenience as the page provided as a parameter will be mutated.
   */
  private SeguePageDTO augmentQuestionObjectWithAttemptInformation(
      final SeguePageDTO page, final List<QuestionDTO> questionsToAugment,
      final Map<String, Map<String, List<QuestionValidationResponse>>> usersQuestionAttempts) {

    if (null == usersQuestionAttempts) {
      return page;
    }

    for (QuestionDTO question : questionsToAugment) {
      if (!usersQuestionAttempts.containsKey(page.getId())) {
        continue;
      }
      if (usersQuestionAttempts.get(page.getId()).get(question.getId()) == null) {
        continue;
      }

      QuestionValidationResponse bestAnswer = null;
      List<QuestionValidationResponse> questionAttempts = usersQuestionAttempts.get(page.getId()).get(
          question.getId());

      // iterate in reverse order to try and find the correct answer.
      for (int i = questionAttempts.size() - 1; i >= 0; i--) {
        QuestionValidationResponse currentResponse = questionAttempts.get(i);

        if (bestAnswer == null) {
          bestAnswer = currentResponse;
        }

        if (questionAttempts.get(i).isCorrect() != null && questionAttempts.get(i).isCorrect()) {
          bestAnswer = currentResponse;
          break;
        }
      }

      question.setBestAttempt(this.convertQuestionValidationResponseToDTO(bestAnswer));

    }
    return page;
  }

  /**
   * Converts a QuestionValidationResponse into a QuestionValidationResponseDTO.
   *
   * @param questionValidationResponse - the thing to convert.
   * @return QuestionValidationResponseDTO
   */
  public QuestionValidationResponseDTO convertQuestionValidationResponseToDTO(
      final QuestionValidationResponse questionValidationResponse) {
    return objectMapper.map(questionValidationResponse);
  }

  /**
   * Record a question attempt for a given user.
   *
   * @param user             - user that made the attempt.
   * @param questionResponse - the outcome of the attempt to be persisted.
   */
  public void recordQuestionAttempt(final AbstractSegueUserDTO user,
                                    final QuestionValidationResponseDTO questionResponse)
      throws SegueDatabaseException {
    QuestionValidationResponse questionResponseDO = this.objectMapper.map(questionResponse);

    String questionPageId = extractPageIdFromQuestionId(questionResponse.getQuestionId());
    if (user instanceof RegisteredUserDTO) {
      RegisteredUserDTO registeredUser = (RegisteredUserDTO) user;

      this.questionAttemptPersistenceManager.registerQuestionAttempt(registeredUser.getId(),
          questionPageId, questionResponse.getQuestionId(), questionResponseDO);
      log.debug("Question information recorded for user: {}", registeredUser.getId());

    } else if (user instanceof AnonymousUserDTO) {
      AnonymousUserDTO anonymousUserDTO = (AnonymousUserDTO) user;

      this.questionAttemptPersistenceManager.registerAnonymousQuestionAttempt(anonymousUserDTO.getSessionId(),
          questionPageId, questionResponse.getQuestionId(), questionResponseDO);
    } else {
      log.error("Unexpected user type. Unable to record question response");
    }
  }

  /**
   * Test a question of a particular type against a series of test cases.
   *
   * @param questionType   - the type of question as a string
   * @param testDefinition - a TestQuestion data structure containing the choices and test cases to use
   * @return a List of TestCases describing the results of the tests
   **/
  public List<TestCase> testQuestion(final String questionType, final TestQuestion testDefinition)
      throws BadRequestException, ValidatorUnavailableException {
    try {
      // Create a fake question
      Class<? extends Content> questionClass = mapperUtils.getClassByType(questionType);
      if (null == questionClass || !ChoiceQuestion.class.isAssignableFrom(questionClass)) {
        throw new BadRequestException(String.format("Not a valid questionType (%s)", questionType));
      }
      ChoiceQuestion testQuestion = (ChoiceQuestion) questionClass.newInstance();
      testQuestion.setChoices(testDefinition.getUserDefinedChoices());
      IValidator questionValidator = QuestionManager.locateValidator(testQuestion.getClass());
      if (null == questionValidator) {
        throw new ValidatorUnavailableException("Could not find a validator for the question");
      }

      // For each test, check its actual results against the response of the validator on the fake question
      List<TestCase> results = Lists.newArrayList();
      for (TestCase testCase : testDefinition.getTestCases()) {
        Choice inferredChoiceSubclass = objectMapper.mapChoice(objectMapper.mapChoice(testCase.getAnswer()));
        QuestionValidationResponse questionValidationResponse = questionValidator
            .validateQuestionResponse(testQuestion, inferredChoiceSubclass);
        testCase.setCorrect(questionValidationResponse.isCorrect());
        testCase.setExplanation(questionValidationResponse.getExplanation());
        results.add(testCase);
      }

      return results;
    } catch (InstantiationException | IllegalAccessException e) {
      throw new BadRequestException(String.format(e.getMessage()));
    }
  }

  /**
   * getQuestionAttemptsByUser. This method will return all of the question attempts for a given user as a map.
   *
   * @param user - with the session information included.
   * @return map of question attempts (QuestionPageId -> QuestionID -> [QuestionValidationResponse] or an empty map.
   * @throws SegueDatabaseException - if there is a database error.
   */
  public Map<String, Map<String, List<QuestionValidationResponse>>> getQuestionAttemptsByUser(
      final AbstractSegueUserDTO user) throws SegueDatabaseException {
    requireNonNull(user);

    if (user instanceof RegisteredUserDTO) {
      RegisteredUserDTO registeredUser = (RegisteredUserDTO) user;

      return this.questionAttemptPersistenceManager.getQuestionAttempts(registeredUser.getId());
    } else {
      AnonymousUserDTO anonymousUser = (AnonymousUserDTO) user;
      // since no user is logged in assume that we want to use any anonymous attempts
      return this.questionAttemptPersistenceManager.getAnonymousQuestionAttempts(anonymousUser.getSessionId());
    }
  }

  /**
   * @param users           who we are interested in.
   * @param questionPageIds we want to look up.
   * @return a map of user id to question page id to question_id to list of attempts.
   * @throws SegueDatabaseException if there is a database error.
   */
  public Map<Long, Map<String, Map<String, List<LightweightQuestionValidationResponse>>>> getMatchingQuestionAttempts(
      final List<RegisteredUserDTO> users, final List<String> questionPageIds) throws SegueDatabaseException {
    List<Long> userIds = Lists.newArrayList();
    for (RegisteredUserDTO user : users) {
      userIds.add(user.getId());
    }

    return this.questionAttemptPersistenceManager.getQuestionAttemptsByUsersAndQuestionPrefix(userIds,
        questionPageIds);
  }

  /**
   * Helper method for attempts from a single user.
   *
   * @param user            who we are interested in.
   * @param questionPageIds we want to look up.
   * @return a map of user id to question page id to question_id to list of attempts.
   * @throws SegueDatabaseException if there is a database error.
   * @see #getMatchingQuestionAttempts(List, List)
   */
  public Map<String, Map<String, List<LightweightQuestionValidationResponse>>> getMatchingQuestionAttempts(
      final RegisteredUserDTO user, final List<String> questionPageIds) throws SegueDatabaseException {

    return this.getMatchingQuestionAttempts(Collections.singletonList(user), questionPageIds).get(user.getId());
  }

  /**
   * mergeAnonymousQuestionAttemptsIntoRegisteredUser.
   *
   * @param anonymousUser  to look up question attempts
   * @param registeredUser to merge into.
   * @throws SegueDatabaseException - if something goes wrong.
   */
  public void mergeAnonymousQuestionAttemptsIntoRegisteredUser(final AnonymousUserDTO anonymousUser,
                                                               final RegisteredUserDTO registeredUser)
      throws SegueDatabaseException {
    this.questionAttemptPersistenceManager.mergeAnonymousQuestionInformationWithRegisteredUserRecord(
        anonymousUser.getSessionId(), registeredUser.getId());
  }

  /**
   * Count the users by role which have answered questions over the previous time interval.
   *
   * @param timeIntervals An array of time ranges (in string format) for which to get the user counts.
   *                      Each time range is used in the SQL query to filter the results.
   * @return map of counts for each role
   * @throws SegueDatabaseException - if there is a problem with the database.
   */
  public Map<TimeInterval, Map<Role, Long>> getAnsweredQuestionRolesOverPrevious(
      final Constants.TimeInterval[] timeIntervals) throws SegueDatabaseException {
    return this.questionAttemptPersistenceManager.getAnsweredQuestionRolesOverPrevious(timeIntervals);
  }

  /**
   * getQuestionAttemptCountsByDate.
   * <br>
   * Retrieves a map of days and number of question attempts
   *
   * @param user     - a User DTO for the user to get question attempt count for
   * @param fromDate - the start of the date range to fetch
   * @param toDate   - the end of the date range to fetch
   * @param perDay   - if true, group count by day, otherwise group count by month
   * @return a Map of LocalDates to attempt counts
   */
  public Map<LocalDate, Long> getUsersQuestionAttemptCountsByDate(final RegisteredUserDTO user,
                                                                  final Instant fromDate, final Instant toDate,
                                                                  final Boolean perDay) throws SegueDatabaseException {
    Map<Instant, Long> questionAttemptCountPerDateByUser = this.questionAttemptPersistenceManager
        .getQuestionAttemptCountForUserByDateRange(fromDate, toDate, user.getId(), perDay);

    // Convert the Instants into localised dates and create a new map.
    Map<LocalDate, Long> result = Maps.newHashMap();
    for (Map.Entry<Instant, Long> le : questionAttemptCountPerDateByUser.entrySet()) {
      LocalDate localisedDate = LocalDate.ofInstant(le.getKey(), ZoneId.systemDefault());

      if (result.containsKey(localisedDate)) {
        result.put(localisedDate, result.get(localisedDate) + le.getValue());
      } else {
        result.put(localisedDate, le.getValue());
      }
    }
    return result;
  }

  /**
   * This is a helper method that will shuffle multiple choice questions and item questions
   * based on a user specified seed.
   *
   * @param seed      - Randomness
   * @param questions - questions which may have choices to shuffle.
   */
  public void shuffleChoiceQuestionsChoices(final String seed, final List<QuestionDTO> questions) {
    if (null == questions) {
      return;
    }

    // shuffle all choices based on the seed provided, augmented by individual question ID.
    for (QuestionDTO question : questions) {
      if (question instanceof ChoiceQuestionDTO) {
        ChoiceQuestionDTO choiceQuestion = (ChoiceQuestionDTO) question;
        String questionSeed = seed + choiceQuestion.getId();

        Boolean randomiseChoices = ((ChoiceQuestionDTO) question).getRandomiseChoices();
        if (randomiseChoices == null || randomiseChoices) {  // Default to randomised if not set.
          if (choiceQuestion.getChoices() != null) {
            Collections.shuffle(choiceQuestion.getChoices(), new Random(questionSeed.hashCode()));
          }
        }

        // FIXME: this is an Isaac specific thing in a segue class!
        //  Perhaps ItemQuestions could live in Segue, but then what relation should they have to
        //  the IsaacQuestionBase class?
        if (question instanceof IsaacItemQuestionDTO) {
          IsaacItemQuestionDTO itemQuestion = (IsaacItemQuestionDTO) question;
          Boolean randomiseItems = itemQuestion.getRandomiseItems();
          if (randomiseItems == null || randomiseItems) {  // Default to randomised if not set.
            if (itemQuestion.getItems() != null) {
              Collections.shuffle(itemQuestion.getItems(), new Random(questionSeed.hashCode()));
            }
          }
        }
      }
    }
  }

  /**
   * Convert an answer into a question specification.
   *
   * @param answer from the client as a list used for comparison purposes.
   * @return A response containing a QuestionValidationResponse object.
   */
  public final Response generateSpecification(final ChoiceDTO answer) {

    ISpecifier specifier = locateSpecifier(answer.getClass());

    if (null == specifier) {
      log.error("Unable to locate a valid specifier for this choice: {}", answer);
      return Response.serverError()
          .entity("Unable to detect question validator for " + "this object. Unable to verify answer")
          .build();
    }

    Choice answerFromUser = objectMapper.mapChoice(answer);
    String specification;
    try {
      specification = specifier.createSpecification(answerFromUser);
    } catch (ValidatorUnavailableException e) {
      return SegueErrorResponse.getServiceUnavailableResponse(e.getClass().getSimpleName() + ":"
          + e.getMessage());
    }

    ResultsWrapper<String> results = new ResultsWrapper<>(Collections.singletonList(specification), 1L);

    return Response.ok(objectMapper.copy(results)).build();
  }

  public ChoiceDTO convertJsonAnswerToChoice(final String jsonAnswer) throws ErrorResponseWrapper {
    ChoiceDTO answerFromClientDTO;
    try {
      // convert submitted JSON into a Choice:
      Choice answerFromClient = mapperUtils.getSharedContentObjectMapper().readValue(jsonAnswer, Choice.class);
      // convert to a DTO so that it strips out any untrusted data.
      answerFromClientDTO = objectMapper.mapChoice(answerFromClient);
    } catch (JsonMappingException | JsonParseException e) {
      log.info("Failed to map to any expected input...", e);
      SegueErrorResponse error = new SegueErrorResponse(Response.Status.NOT_FOUND, "Unable to map response to a "
          + "Choice object so failing with an error", e);
      throw new ErrorResponseWrapper(error);
    } catch (IOException e) {
      SegueErrorResponse error = new SegueErrorResponse(Response.Status.NOT_FOUND, "Unable to map response to a "
          + "Choice object so failing with an error", e);
      log.error(error.getErrorMessage(), e);
      throw new ErrorResponseWrapper(error);
    }
    return answerFromClientDTO;
  }

  public GameFilter createGameFilterForRandomQuestions(final RegisteredUserDTO currentUser, final String subjects) {
    GameFilter gameFilter = new GameFilter();

    UserPreference filterQuestionsPreference = null;
    try {
      filterQuestionsPreference = this.userPreferenceManager.getUserPreference(
          "DISPLAY_SETTING",
          "HIDE_NON_AUDIENCE_CONTENT",
          currentUser.getId());
    } catch (SegueDatabaseException e) {
      log.error("Error while getting user preferences", e);
    }

    if (filterQuestionsPreference != null && filterQuestionsPreference.getPreferenceValue()) {
      var userContexts = currentUser.getRegisteredContexts();

      List<String> subjectsList = QueryUtils.splitCsvStringQueryParam(subjects);
      List<String> stagesList = new ArrayList<>();
      List<String> examBoardsList = new ArrayList<>();

      boolean containsAllStages = userContexts.stream().anyMatch(uc -> "all".equals(uc.getStage().name()));
      boolean containsAllExamBoards = userContexts.stream().anyMatch(uc -> "all".equals(uc.getExamBoard().name()));

      for (UserContext uc : userContexts) {
        if (!containsAllStages) {
          stagesList.add(uc.getStage().name());
        }
        if (!containsAllExamBoards) {
          examBoardsList.add(uc.getExamBoard().name());
        }
      }

      gameFilter = new GameFilter(
          subjectsList,
          null,
          null,
          null,
          null,
          null,
          stagesList,
          null,
          examBoardsList);
    }
    return gameFilter;
  }
}
