/*
 * Copyright 2019 Meurig Thomas
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
package uk.ac.cam.cl.dtg.isaac.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.util.Lists;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.jboss.resteasy.annotations.GZIP;
import org.jboss.resteasy.annotations.jaxrs.QueryParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.dto.TestCaseDTO;
import uk.ac.cam.cl.dtg.isaac.dto.QuestionTestDTO;
import uk.ac.cam.cl.dtg.segue.api.managers.QuestionManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAccountManager;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserLoggedInException;
import uk.ac.cam.cl.dtg.segue.dao.ILogManager;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentMapper;
import uk.ac.cam.cl.dtg.segue.dos.QuestionValidationResponse;
import uk.ac.cam.cl.dtg.segue.dos.content.ChoiceQuestion;
import uk.ac.cam.cl.dtg.segue.dos.content.Content;
import uk.ac.cam.cl.dtg.segue.dto.SegueErrorResponse;
import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.segue.quiz.IValidator;
import uk.ac.cam.cl.dtg.segue.quiz.ValidatorUnavailableException;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.List;


/**
 * A facade to define the endpoint(s) needed to help with the building of questions.
 */
@Path("/builder")
@Api(value = "/builder")
public class BuilderFacade extends AbstractIsaacFacade {
    private static final Logger log = LoggerFactory.getLogger(PagesFacade.class);

    private final UserAccountManager userManager;
    private final ContentMapper mapper;

    @Inject
    public BuilderFacade(final PropertiesLoader propertiesLoader, final ILogManager logManager,
                         final UserAccountManager userManager, final ContentMapper mapper) {
        super(propertiesLoader, logManager);
        this.userManager = userManager;
        this.mapper = mapper;
    }

    /**
     * A generic question tester where a fake question is created form received choices and evaluated against a series
     * of example student answers
     * @param request - the incoming request
     * @param response - the outgoing response
     * @param questionType - the type of question to construct from the available choices in testJson
     * @param testJson - a JSON structure to represent the possible choices and
     * @return a list of test cases matching those that were sent to the endpoint augmented with the validator's results
     */
    @POST
    @Path("/test_question")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    @ApiOperation(value = "Test a list of choices with some expected answer values.")
    public Response testQuestion(@Context final HttpServletRequest request, @Context final HttpServletResponse response,
                                 @QueryParam("type") final String questionType, final String testJson) {
        try {
            // User authorisation
            RegisteredUserDTO currentUser = userManager.getCurrentRegisteredUser(request);
            if (!isUserStaff(userManager, currentUser)) {
                return SegueErrorResponse.getIncorrectRoleResponse();
            }

            ObjectMapper sharedContentMapper = mapper.getSharedContentObjectMapper();
            QuestionTestDTO testDefinition = sharedContentMapper.readValue(testJson, QuestionTestDTO.class);

            // Create a fake question
            Class<? extends Content> questionClass = mapper.getClassByType(questionType);
            if (null == questionClass || !ChoiceQuestion.class.isAssignableFrom(questionClass)) {
                throw new BadRequestException(String.format("Not a valid questionType (%s)", questionType));
            }
            ChoiceQuestion testQuestion = (ChoiceQuestion) questionClass.newInstance();
            testQuestion.setChoices(testDefinition.getQuestionChoices());

            IValidator questionValidator = QuestionManager.locateValidator(testQuestion.getClass());
            if (null == questionValidator) {
                throw new ValidatorUnavailableException("Could not find a validator for the question");
            }

            // For each test, check its actual results against the response of the validator on the fake question
            List<TestCaseDTO> results = Lists.newArrayList();
            for (TestCaseDTO testCase : testDefinition.getTestCases()) {
                QuestionValidationResponse questionValidationResponse =
                        questionValidator.validateQuestionResponse(testQuestion, testCase.getChoice());
                testCase.setActual(questionValidationResponse.isCorrect());
                testCase.setExplanation(questionValidationResponse.getExplanation());
                results.add(testCase);
            }

            return Response.ok(results).build();

        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        } catch (ValidatorUnavailableException | IOException e) {
            return SegueErrorResponse.getServiceUnavailableResponse(e.getMessage());
        } catch (InstantiationException | IllegalAccessException e) {
            return SegueErrorResponse.getBadRequestResponse(e.getMessage());
        }
    }
}
