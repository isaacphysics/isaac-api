/*
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
package uk.ac.cam.cl.dtg.isaac.api;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import uk.ac.cam.cl.dtg.isaac.IsaacTest;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAccountManager;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserLoggedInException;
import uk.ac.cam.cl.dtg.segue.dto.SegueErrorResponse;
import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.createPartialMock;
import static org.powermock.api.easymock.PowerMock.replay;
import static org.powermock.api.easymock.PowerMock.reset;
import static org.powermock.api.easymock.PowerMock.verifyAll;

/**
 * A test base for testing Facades, specifically targeted around testing facades as different users.
 *
 * A typical test case using this class can look like:
 *
 * <pre>{@code
 *     public void startQuizAttempt() {
 *         QuizAttemptDTO attempt = new QuizAttemptDTO();
 *
 *         forEndpoint(
 *             (assignment) -> () -> quizFacade.startQuizAttempt(requestForCaching, request, assignment.getId()),
 *             with(studentAssignment,
 *                 requiresLogin(),
 *                 as(student,
 *                     prepare(quizAttemptManager, m -> expect(m.fetchOrCreate(studentAssignment, student)).andReturn(attempt)),
 *                     respondsWith(attempt)),
 *                 forbiddenForEveryoneElse()
 *             ),
 *             with(overdueAssignment,
 *                 as(student, failsWith(Status.FORBIDDEN))
 *             )
 *         );
 *     }
 * }</pre>
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({UserAccountManager.class})
@PowerMockIgnore({ "javax.ws.*", "javax.management.*", "javax.script.*" })
abstract public class AbstractFacadeTest extends IsaacTest {
    protected HttpServletRequest request;
    protected UserAccountManager userManager;

    private RegisteredUserDTO specialEveryoneElse = new RegisteredUserDTO();
    private RegisteredUserDTO currentUser = null;

    @Before
    public void abstractFacadeTestSetup() {
        request = createMock(HttpServletRequest.class);
        replay(request);

        userManager = createPartialMock(UserAccountManager.class, "getCurrentRegisteredUser");
    }

    /**
     * Run tests on an endpoint with different parameters.
     *
     * Only supports varying one parameter to the endpoint. Use multiple calls to vary multiple parameters.
     */
    @SafeVarargs
    protected final <T> void forEndpoint(Function<T, Supplier<Response>> endpoint, WithParam<T>... withs) {
        for (WithParam<T> with: withs) {
            forEndpoint(endpoint.apply(with.with), with.testcases.toArray(new Testcase[]{}));
        }
    }

    /**
     * Run tests on an endpoint.
     */
    protected void forEndpoint(Supplier<Response> endpoint, Testcase... testcases) {
        Set<RegisteredUserDTO> users = new HashSet<>(everyone);
        for (Testcase testcase : testcases) {
            try {
                if (testcase.users != null && testcase.users.size() == 1 && testcase.users.get(0) == specialEveryoneElse) {
                    testcase = new Testcase(new ArrayList<>(users), testcase.steps.toArray(new Step[]{}));
                } else {
                    if (testcase.users != null) {
                        users.removeAll(testcase.users);
                    }
                }
                testcase.runOn(endpoint);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Specify the parameter to use with forEndpoint.
     *
     * @see AbstractFacadeTest#forEndpoint(Function, WithParam[])
     */
    protected <T> WithParam<T> with(T value, Testcase... testcases) {
        return new WithParam<>(value, Arrays.asList(testcases));
    }

    /**
     * Specify the user to call the endpoint as.
     */
    protected Testcase as(RegisteredUserDTO user, Step... steps) {
        return new Testcase(user, steps);
    }

    /**
     * Specify a list of users to call the endpoint as.
     */
    protected Testcase as(List<RegisteredUserDTO> users, Step... steps) {
        return new Testcase(users, steps);
    }

    /**
     * Specify steps to run without any call to getCurrentRegisteredUser being made.
     */
    protected Testcase beforeUserCheck(Step... steps) {
        return new Testcase(steps);
    }

    /**
     * Simple check that login is required.
     */
    protected Testcase requiresLogin() {
        return as(noone, failsWith(SegueErrorResponse.getNotLoggedInResponse()));
    }

    /**
     * Run checks for all users that haven't be specifically "as"-ed in this block.
     *
     * Use `everyoneElse(...)` instead of `as(anyOf(thatOtherUser, andTheOtherOne, andTheLastOne), ...)`
     */
    protected Testcase everyoneElse(Step... steps) {
        return new Testcase(specialEveryoneElse, steps);
    }

    /**
     * Add one or more expectations to a mock. The mock is passed in as an argument to the consumer.
     *
     * e.g. <code>prepare(someManager, m -> expect(m.someMethodCall(someArgs)).andReturn(someResult))</code>
     */
    protected <T> PrepareStep<T> prepare(T mock, MockConfigurer<T> preparation) {
        return new PrepareStep<>(mock, preparation);
    }

    /**
     * Get the response from making the call and perform an assertion.
     */
    protected CheckStep check(Consumer<Response> checker) {
        return new CheckStep(checker);
    }

    /**
     * Assert the response code matches the specified response, and if a SegueErrorResponse is provided, check the messages match.
     */
    protected CheckStep failsWith(Response expected) {
        return new CheckStep((response) -> assertErrorResponse(expected, response));
    }

    /**
     * Assert the response fails with the specified status.
     */
    protected CheckStep failsWith(Status status) {
        return new CheckStep((response) -> assertErrorResponse(status, response));
    }

    /**
     * Assert the response is of the expected value.
     */
    protected CheckStep respondsWith(Object expected) {
        return new CheckStep((response) -> assertEquals(expected, response.getEntity()));
    }

    /**
     * Asserts the response has a successful (2XX) status code.
     */
    protected CheckStep succeeds() {
        return new CheckStep((response) -> assertEquals(Status.Family.SUCCESSFUL, response.getStatusInfo().getFamily()));
    }

    /**
     * Get the current user being tested (useful for parameterising preparation).
     */
    protected RegisteredUserDTO currentUser() {
        return currentUser;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Internals start here
    ///////////////////////////////////////////////////////////////////////////

    private void assertErrorResponse(Status expectedStatus, Response actual) {
        assertEquals("Expected status " + expectedStatus.name() + ", got " + actual.getStatusInfo().toEnum().name() + " with details " + extractErrorInfo(actual), expectedStatus.getStatusCode(), actual.getStatus());
    }

    private String extractErrorInfo(Response response) {
        if (response.getEntity() instanceof SegueErrorResponse) {
            SegueErrorResponse error = (SegueErrorResponse) response.getEntity();
            return error.getErrorMessage();
        }
        return response.getEntity().toString();
    }

    private void assertErrorResponse(Response expected, Response actual) {
        assertErrorResponse(expected.getStatusInfo().toEnum(), actual);
        if (expected.getEntity() instanceof SegueErrorResponse) {
            SegueErrorResponse expectedError = (SegueErrorResponse) expected.getEntity();
            assertEquals(expectedError.getErrorMessage(), ((SegueErrorResponse) actual.getEntity()).getErrorMessage());
        }
    }

    interface Step {}

    class CheckStep implements Step {
        private final Consumer<Response> checker;
        CheckStep(Consumer<Response> checker) {
            this.checker = checker;
        }
    }

    class PrepareStep<T> implements Step {
        private final T mock;
        private final MockConfigurer<T> preparation;

        PrepareStep(T mock, MockConfigurer<T> preparation) {
            this.mock = mock;
            this.preparation = preparation;
        }

        public void run() {
            preparation.configure(mock);
        }
    }

    class WithParam<T> {
        final T with;
        final List<Testcase> testcases;

        WithParam(T with, List<Testcase> testcases) {
            this.with = with;
            this.testcases = testcases;
        }
    }

    class Testcase {
        private final List<RegisteredUserDTO> users;
        private final List<Step> steps;

        Testcase(Step[] steps) {
            this.users = null;
            this.steps = Arrays.asList(steps);
        }

        Testcase(RegisteredUserDTO user, Step[] steps) {
            this.users = Collections.singletonList(user);
            this.steps = Arrays.asList(steps);
        }

        Testcase(List<RegisteredUserDTO> users, Step[] steps) {
            this.users = users;
            this.steps = Arrays.asList(steps);
        }

        private void runOn(Supplier<Response> endpoint) {
            if (users == null) {
                runSteps(endpoint);
            } else {
                for (RegisteredUserDTO user : users) {
                    runStepsAs(user, endpoint);
                }
            }
        }

        private void runSteps(Supplier<Response> endpoint) {
            Response response = null;
            for (Step step : steps) {
                if (step instanceof CheckStep) {
                    if (response == null) {
                        response = endpoint.get();
                    }
                    ((CheckStep) step).checker.accept(response);
                } else if (step instanceof PrepareStep) {
                    PrepareStep<?> prepareStep = (PrepareStep) step;
                    reset(prepareStep.mock);
                    if (defaultsMap.containsKey(prepareStep.mock)) {
                        defaultsMap.get(prepareStep.mock).configure(prepareStep.mock);
                    }
                    prepareStep.run();
                    replay(prepareStep.mock);
                }
            }
        }

        private void runStepsAs(RegisteredUserDTO user, Supplier<Response> endpoint) {
            try {
                reset(userManager);
                if (user == null) {
                    expect(userManager.getCurrentRegisteredUser(request)).andThrow(new NoUserLoggedInException());
                } else {
                    expect(userManager.getCurrentRegisteredUser(request)).andReturn(user);
                }
                replay(userManager);
                currentUser = user;

                runSteps(endpoint);

                verifyAll();
            } catch (NoUserLoggedInException e) {
                // Can't happen
                throw new RuntimeException(e);
            }
        }
    }
}
