/*
 * Copyright 2014 Stephen Cummins
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.cam.cl.dtg.segue.api.managers;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.google.api.client.util.Lists;
import com.google.api.client.util.Maps;
import com.google.inject.Inject;
import com.google.inject.Injector;
import io.prometheus.client.Histogram;
import ma.glasnost.orika.MapperFacade;
import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacQuestionPage;
import uk.ac.cam.cl.dtg.isaac.dos.LLMFreeTextQuestionValidationResponse;
import uk.ac.cam.cl.dtg.isaac.dos.LightweightQuestionValidationResponse;
import uk.ac.cam.cl.dtg.isaac.dos.QuestionValidationResponse;
import uk.ac.cam.cl.dtg.isaac.dos.TestCase;
import uk.ac.cam.cl.dtg.isaac.dos.TestQuestion;
import uk.ac.cam.cl.dtg.isaac.dos.content.Choice;
import uk.ac.cam.cl.dtg.isaac.dos.content.ChoiceQuestion;
import uk.ac.cam.cl.dtg.isaac.dos.content.Content;
import uk.ac.cam.cl.dtg.isaac.dos.content.DTOMapping;
import uk.ac.cam.cl.dtg.isaac.dos.content.Question;
import uk.ac.cam.cl.dtg.isaac.dos.users.Role;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacItemQuestionDTO;
import uk.ac.cam.cl.dtg.isaac.dto.QuestionValidationResponseDTO;
import uk.ac.cam.cl.dtg.isaac.dto.ResultsWrapper;
import uk.ac.cam.cl.dtg.isaac.dto.SegueErrorResponse;
import uk.ac.cam.cl.dtg.isaac.dto.content.ChoiceDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.ChoiceQuestionDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentBaseDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.InlineRegionDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.QuestionDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.SeguePageDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.AbstractSegueUserDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.AnonymousUserDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.isaac.quiz.IQuestionAttemptManager;
import uk.ac.cam.cl.dtg.isaac.quiz.ISpecifier;
import uk.ac.cam.cl.dtg.isaac.quiz.IValidator;
import uk.ac.cam.cl.dtg.isaac.quiz.SpecifiesWith;
import uk.ac.cam.cl.dtg.isaac.quiz.ValidatesWith;
import uk.ac.cam.cl.dtg.isaac.quiz.ValidatorUnavailableException;
import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.api.Constants.*;
import uk.ac.cam.cl.dtg.segue.api.ErrorResponseWrapper;
import uk.ac.cam.cl.dtg.segue.configuration.SegueGuiceConfigurationModule;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentMapper;
import uk.ac.cam.cl.dtg.segue.dao.content.GitContentManager;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

import static uk.ac.cam.cl.dtg.isaac.api.managers.GameManager.getAllMarkableDOQuestionPartsDFSOrder;
import static uk.ac.cam.cl.dtg.segue.api.monitors.SegueMetrics.VALIDATOR_LATENCY_HISTOGRAM;

/**
 * This class is responsible for validating correct answers using the ValidatesWith annotation when it is applied on to
 * Questions.
 * 
 * It is also responsible for orchestrating question attempt persistence.
 * 
 */
public class QuestionManager {
    private static final Logger log = LoggerFactory.getLogger(QuestionManager.class);

    private final ContentMapper mapper;
    private final IQuestionAttemptManager questionAttemptPersistenceManager;
    private final GitContentManager contentManager;

    /**
     * Create a default Question manager object.
     * 
     * @param mapper
     *            - an auto mapper to allow us to convert to and from QuestionValidationResponseDOs and DTOs.
     * @param questionPersistenceManager - for question attempt persistence.
     */
    @Inject
    public QuestionManager(
            final ContentMapper mapper, final IQuestionAttemptManager questionPersistenceManager,
            final GitContentManager contentManager
    ) {
        this.mapper = mapper;
        this.questionAttemptPersistenceManager = questionPersistenceManager;
        this.contentManager = contentManager;
    }

    /**
     * Validate client answer to recorded answer.
     * 
     * @param question
     *            The question to which the answer must be validated against.
     * @param submittedAnswer
     *            from the client as a DTO for comparison.
     * @return A response containing a QuestionValidationResponse object.
     */
    public final Response validateAnswer(final Question question, final ChoiceDTO submittedAnswer) {
        IValidator validator = locateValidator(question.getClass());

        if (null == validator) {
            log.error("Unable to locate a valid validator for this question " + question.getId());
            return Response.serverError()
                    .entity("Unable to detect question validator for " + "this object. Unable to verify answer")
                    .build();
        }

        Choice answerFromUser = mapper.getAutoMapper().map(submittedAnswer, Choice.class);
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

        return Response.ok(
                mapper.getAutoMapper().map(validateQuestionResponse, QuestionValidationResponseDTO.class)).build();

    }

    /**
     * Reflection to try and determine the associated validator for the question being answered.
     * 
     * @param questionType
     *            - the type of question being answered.
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

            log.debug("Validator for question validation found. Using : "
                    + questionType.getAnnotation(ValidatesWith.class).value());
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
     * Reflection to try and determine the associated specifier for the choice given.
     *
     * @param choiceClass
     *            - the type of choice given.
     * @return a Validator
     */
    @SuppressWarnings("unchecked")
    private ISpecifier locateSpecifier(Class<? extends ChoiceDTO> choiceClass) {
        // check we haven't gone too high up the superclass tree
        if (!ChoiceDTO.class.isAssignableFrom(choiceClass)) {
            return null;
        }

        // Does this class have the correct annotation?
        if (choiceClass.isAnnotationPresent(SpecifiesWith.class)) {

            log.debug("Specifier for specifiation creation found. Using : "
                + choiceClass.getAnnotation(SpecifiesWith.class).value());
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
     * 
     * It will also ensure that any personalisation of questions is affected (e.g. randomised multichoice elements).
     *
     * Note: It will not do anything to related content
     * 
     * @param page
     *            - to augment - this object may be mutated as a result of this method. i.e BestAttempt field set on
     *            question DTOs.
     * @param userId
     *            - to allow us to provide a per user experience of question configuration (random seed).
     * @param usersQuestionAttempts
     *            - as a map of QuestionPageId to Map of QuestionId to QuestionValidationResponseDO
     * @return augmented page - the return result is by convenience as the page provided as a parameter will be mutated.
     */
    public void augmentQuestionObjects(final SeguePageDTO page, final String userId,
            final Map<String, Map<String, List<QuestionValidationResponse>>> usersQuestionAttempts) {

        List<QuestionDTO> questionsToAugment = extractQuestionObjects(page);

        augmentQuestionObjectWithAttemptInformation(page, questionsToAugment, usersQuestionAttempts);

        shuffleChoiceQuestionsChoices(userId, questionsToAugment);
    }

    /**
     * Modify a question objects in a page such that it contains bestAttempt information if we can provide it.
     * 
     * @param page
     *            - the page this object may be mutated as a result of this method. i.e BestAttempt field set on
     *            question DTOs.
     * @param questionsToAugment
     *            - The flattened list of questions which should be augmented.
     * @param usersQuestionAttempts
     *            - as a map of QuestionPageId to Map of QuestionId to QuestionValidationResponseDO
     */
    private void augmentQuestionObjectWithAttemptInformation(final SeguePageDTO page,
            final List<QuestionDTO> questionsToAugment,
            final Map<String, Map<String, List<QuestionValidationResponse>>> usersQuestionAttempts) {

        if (null == usersQuestionAttempts) {
            return;
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
    }



    /**
     * Converts a QuestionValidationResponse into a QuestionValidationResponseDTO.
     * 
     * @param questionValidationResponse
     *            - the thing to convert.
     * @return QuestionValidationResponseDTO
     */
    @SuppressWarnings("unchecked")
    public QuestionValidationResponseDTO convertQuestionValidationResponseToDTO(
            final QuestionValidationResponse questionValidationResponse) {
        // Determine what kind of ValidationResponse to turn it in to.
        DTOMapping dtoMapping = questionValidationResponse.getClass().getAnnotation(DTOMapping.class);
        if (QuestionValidationResponseDTO.class.isAssignableFrom(dtoMapping.value())) {
            return mapper.getAutoMapper().map(questionValidationResponse,
                    (Class<? extends QuestionValidationResponseDTO>) dtoMapping.value());
        } else {
            log.error("Unable to set best attempt as we cannot match the answer to a DTO type.");
            throw new ClassCastException("Unable to cast " + questionValidationResponse.getClass()
                    + " to a QuestionValidationResponse.");
        }
    }
    
    /**
     * Record a question attempt for a given user.
     * @param user - user that made the attempt.
     * @param questionResponse - the outcome of the attempt to be persisted.
     */
    public void recordQuestionAttempt(final AbstractSegueUserDTO user,
            final QuestionValidationResponseDTO questionResponse) throws SegueDatabaseException {
        QuestionValidationResponse questionResponseDO = this.mapper.getAutoMapper().map(questionResponse,
                QuestionValidationResponse.class);

        String questionPageId = extractPageIdFromQuestionId(questionResponse.getQuestionId());
        if (user instanceof RegisteredUserDTO) {
            RegisteredUserDTO registeredUser = (RegisteredUserDTO) user;

            this.questionAttemptPersistenceManager.registerQuestionAttempt(registeredUser.getId(),
                questionPageId, questionResponse.getQuestionId(), questionResponseDO);
            log.debug("Question information recorded for user: " + registeredUser.getId());

        } else if (user instanceof AnonymousUserDTO) {
            AnonymousUserDTO anonymousUserDTO = (AnonymousUserDTO) user;

            this.questionAttemptPersistenceManager.registerAnonymousQuestionAttempt(anonymousUserDTO.getSessionId(),
                questionPageId, questionResponse.getQuestionId(), questionResponseDO);
        } else {
            log.error("Unexpected user type. Unable to record question response");
        }
    }
    
    /** Test a question of a particular type against a series of test cases **/
    public List<TestCase> testQuestion(final String questionType, final TestQuestion testDefinition)
            throws BadRequestException, ValidatorUnavailableException {
        try {
            MapperFacade autoMapper = mapper.getAutoMapper();

            // Create a fake question
            Class<? extends Content> questionClass = mapper.getClassByType(questionType);
            if (null == questionClass || !ChoiceQuestion.class.isAssignableFrom(questionClass)) {
                throw new BadRequestException(String.format("Not a valid questionType (%s)", questionType));
            }
            ChoiceQuestion testQuestion = (ChoiceQuestion) questionClass.getDeclaredConstructor().newInstance();
            testQuestion.setChoices(testDefinition.getUserDefinedChoices());
            IValidator questionValidator = QuestionManager.locateValidator(testQuestion.getClass());
            if (null == questionValidator) {
                throw new ValidatorUnavailableException("Could not find a validator for the question");
            }

            // For each test, check its actual results against the response of the validator on the fake question
            List<TestCase> results = Lists.newArrayList();
            for (TestCase testCase : testDefinition.getTestCases()) {
                Choice inferredChoiceSubclass =
                        autoMapper.map(autoMapper.map(testCase.getAnswer(), ChoiceDTO.class), Choice.class);
                QuestionValidationResponse questionValidationResponse = questionValidator
                        .validateQuestionResponse(testQuestion, inferredChoiceSubclass);
                testCase.setCorrect(questionValidationResponse.isCorrect());
                testCase.setExplanation(questionValidationResponse.getExplanation());
                results.add(testCase);
            }

            return results;
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new BadRequestException(String.format(e.getMessage()));
        }
    }
    
    /**
     * getQuestionAttemptsByUser. This method will return all of the question attempts for a given user as a map.
     * 
     * @param user
     *            - with the session information included.
     * @return map of question attempts (QuestionPageId -> QuestionID -> [QuestionValidationResponse] or an empty map.
     * @throws SegueDatabaseException
     *             - if there is a database error.
     */
    public Map<String, Map<String, List<QuestionValidationResponse>>> getQuestionAttemptsByUser(
            final AbstractSegueUserDTO user) throws SegueDatabaseException {
        Objects.requireNonNull(user);

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
     * getLightweightQuestionAttemptsByUser. This method will return all of the question attempts for a given user as a map.
     *
     * Attempts will not be augmented with the full attempt JSON data.
     *
     * @param user
     *            - with the session information included.
     * @return map of question attempts (QuestionPageId -> QuestionID -> [LightweightQuestionValidationResponse] or an empty map.
     * @throws SegueDatabaseException
     *             - if there is a database error.
     */
    public Map<String, Map<String, List<LightweightQuestionValidationResponse>>> getLightweightQuestionAttemptsByUser(final RegisteredUserDTO user)
            throws SegueDatabaseException {
        return this.questionAttemptPersistenceManager.getLightweightQuestionAttempts(user.getId());
    }


    /**
     * Return all the attempts of a user at a specified page ID prefix.
     *
     * The map returned by this method is in the same format as {@link #getQuestionAttemptsByUser(AbstractSegueUserDTO)}
     * for compatibility.
     *
     * @param user the user of interest
     * @param questionPageId the page ID prefix of interest
     * @return a map QuestionPageID -> (QuestionID -> List[QuestionValidationResponse]).
     * @throws SegueDatabaseException on database error
     */
    public Map<String, Map<String, List<QuestionValidationResponse>>> getQuestionAttemptsByUserForQuestion(
            final RegisteredUserDTO user, final String questionPageId) throws SegueDatabaseException {
        return questionAttemptPersistenceManager.getQuestionAttempts(user.getId(), questionPageId);
    }
    
    /**
     * @param users who we are interested in.
     * @param questionPageIds we want to look up.
     * @return a map of user id to question page id to question_id to list of attempts.
     * @throws SegueDatabaseException if there is a database error.
     */
    public Map<Long, Map<String, Map<String, List<LightweightQuestionValidationResponse>>>> getMatchingLightweightQuestionAttempts(
            final List<RegisteredUserDTO> users, final List<String> questionPageIds) throws SegueDatabaseException {
        List<Long> userIds = Lists.newArrayList();
        for (RegisteredUserDTO user : users) {
            userIds.add(user.getId());
        }

        return this.questionAttemptPersistenceManager.getMatchingLightweightQuestionAttempts(userIds, questionPageIds);
    }

    /**
     *  Helper method for attempts from a single user.
     *
     * @see #getMatchingLightweightQuestionAttempts(List, List)
     *
     * @param user who we are interested in.
     * @param questionPageIds we want to look up.
     * @return a map of user id to question page id to question_id to list of attempts.
     * @throws SegueDatabaseException if there is a database error.
     */
    public Map<String, Map<String, List<LightweightQuestionValidationResponse>>> getMatchingLightweightQuestionAttempts(
            final RegisteredUserDTO user, final List<String> questionPageIds) throws SegueDatabaseException {

        return this.getMatchingLightweightQuestionAttempts(Collections.singletonList(user), questionPageIds)
                .getOrDefault(user.getId(), Collections.emptyMap());
    }
    
    /**
     * mergeAnonymousQuestionAttemptsIntoRegisteredUser.
     * 
     * @param anonymousUser
     *            to look up question attempts
     * @param registeredUser
     *            to merge into.
     * @throws SegueDatabaseException
     *             - if something goes wrong.
     */
    public void mergeAnonymousQuestionAttemptsIntoRegisteredUser(final AnonymousUserDTO anonymousUser,
            final RegisteredUserDTO registeredUser) throws SegueDatabaseException {
        this.questionAttemptPersistenceManager.mergeAnonymousQuestionInformationWithRegisteredUserRecord(
                anonymousUser.getSessionId(), registeredUser.getId());
    }

    /**
     * Count the users by role which have answered questions over the previous time interval
     * @param timeInterval time interval over which to count
     * @return map of counts for each role
     * @throws SegueDatabaseException
     *             - if there is a problem with the database.
     */
    public Map<Role, Long> getAnsweredQuestionRolesOverPrevious(TimeInterval timeInterval)
            throws SegueDatabaseException {
        return this.questionAttemptPersistenceManager.getAnsweredQuestionRolesOverPrevious(timeInterval);
    }

    /**
     * getQuestionAttemptCountsByDate
     *
     * Retrieves a map of days and number of question attempts
     */
    public Map<LocalDate, Long> getUsersQuestionAttemptCountsByDate(final RegisteredUserDTO user,
                                                                    final Date fromDate, final Date toDate,
                                                                    final Boolean perDay) throws SegueDatabaseException {
        Map<Date, Long> questionAttemptCountPerDateByUser = this.questionAttemptPersistenceManager.getQuestionAttemptCountForUserByDateRange(fromDate, toDate, user.getId(), perDay);

        // Convert the normal java dates into useful joda dates and create a new map.
        Map<LocalDate, Long> result = Maps.newHashMap();
        for (Map.Entry<Date, Long> le : questionAttemptCountPerDateByUser.entrySet()) {
            LocalDate localisedDate = new LocalDate(le.getKey());

            if (result.containsKey(localisedDate)) {
                result.put(localisedDate, result.get(localisedDate) + le.getValue());
            } else {
                result.put(localisedDate, le.getValue());
            }
        }
        return result;
    }

    /**
     * Extract all of the question objects, recursively, from some content.
     *
     * @param content
     *            - The contentDTO which may have question objects as children.
     * @return A list of QuestionDTO found in the content.
     */
    public static List<QuestionDTO> extractQuestionObjects(ContentDTO content) {
        return QuestionManager.extractQuestionObjectsRecursively(content,
            new ArrayList<>());
    }

    /**
     * Extract all of the questionObjectsRecursively.
     *
     * See also {@link uk.ac.cam.cl.dtg.isaac.api.managers.GameManager#getAllMarkableQuestionPartsDFSOrder(ContentDTO)}
     * which does basically the same thing.
     *
     * @param toExtract
     *            - The contentDTO which may have question objects as children.
     * @param result
     *            - The initially empty List which will be mutated to contain references to all of the question objects.
     * @return The modified result array.
     */
    private static List<QuestionDTO> extractQuestionObjectsRecursively(final ContentDTO toExtract,
            final List<QuestionDTO> result) {
        if (toExtract instanceof QuestionDTO) {
            // we found a question so add it to the list.
            result.add((QuestionDTO) toExtract);
        }

        if (toExtract instanceof InlineRegionDTO) {
            // extract inline questions
            InlineRegionDTO inlineRegionDTO = (InlineRegionDTO) toExtract;
            result.addAll(inlineRegionDTO.getInlineQuestions());
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

    /**
     * This is a helper method that will shuffle multiple choice questions and item questions
     * based on a user specified seed.
     * 
     * @param seed
     *            - Randomness
     * @param questions
     *            - questions which may have choices to shuffle.
     */
    public void shuffleChoiceQuestionsChoices(final String seed, final List<QuestionDTO> questions) {
        if (null == questions) {
            return;
        }

        // shuffle all choices based on the seed provided, augmented by individual question ID.
        for (QuestionDTO question : questions) {
            if (question instanceof ChoiceQuestionDTO) {
                ChoiceQuestionDTO choiceQuestion = (ChoiceQuestionDTO) question;
                String qSeed = seed + choiceQuestion.getId();

                Boolean randomiseChoices = ((ChoiceQuestionDTO) question).getRandomiseChoices();
                if (randomiseChoices == null || randomiseChoices) {  // Default to randomised if not set.
                    if (choiceQuestion.getChoices() != null) {
                        Collections.shuffle(choiceQuestion.getChoices(), new Random(qSeed.hashCode()));
                    }
                }

                // FIXME: this is an Isaac specific thing in a segue class!
                // Perhaps ItemQuestions could live in Segue, but then what relation should they have to
                // the IsaacQuestionBase class?
                if (question instanceof IsaacItemQuestionDTO) {
                    IsaacItemQuestionDTO itemQuestion = (IsaacItemQuestionDTO) question;
                    Boolean randomiseItems = itemQuestion.getRandomiseItems();
                    if (randomiseItems == null || randomiseItems) {  // Default to randomised if not set.
                        if (itemQuestion.getItems() != null) {
                            Collections.shuffle(itemQuestion.getItems(), new Random(qSeed.hashCode()));
                        }
                    }
                }
            }
        }
    }

    /**
     * Convert an answer into a question specification.
     *
     * @param answer
     *            from the client as a list used for comparison purposes.
     * @return A response containing a QuestionValidationResponse object.
     */
    public final Response generateSpecification(final ChoiceDTO answer) {

        ISpecifier specifier = locateSpecifier(answer.getClass());

        if (null == specifier) {
            log.error("Unable to locate a valid specifier for this choice: " + answer);
            return Response.serverError()
                .entity("Unable to detect question validator for " + "this object. Unable to verify answer")
                .build();
        }

        Choice answerFromUser = mapper.getAutoMapper().map(answer, Choice.class);
        String specification;
        try {
            specification = specifier.createSpecification(answerFromUser);
        } catch (ValidatorUnavailableException e) {
            return SegueErrorResponse.getServiceUnavailableResponse(e.getClass().getSimpleName() + ":"
                + e.getMessage());
        }

        ResultsWrapper<String> results = new ResultsWrapper<>(Collections.singletonList(specification), 1L);

        return Response.ok(
            mapper.getAutoMapper().map(results, ResultsWrapper.class)).build();
    }

    public static String extractPageIdFromQuestionId(String questionId) {
        return questionId.split(Constants.ESCAPED_ID_SEPARATOR)[0];
    }

    public ChoiceDTO convertJsonAnswerToChoice(String jsonAnswer) throws ErrorResponseWrapper {
        ChoiceDTO answerFromClientDTO;
        try {
            // convert submitted JSON into a Choice:
            Choice answerFromClient = mapper.getSharedContentObjectMapper().readValue(jsonAnswer, Choice.class);
            // convert to a DTO so that it strips out any untrusted data.
            answerFromClientDTO = mapper.getAutoMapper().map(answerFromClient, ChoiceDTO.class);
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
}
