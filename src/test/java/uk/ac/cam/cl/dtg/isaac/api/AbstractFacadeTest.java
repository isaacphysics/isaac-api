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

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import uk.ac.cam.cl.dtg.isaac.IsaacTest;
import uk.ac.cam.cl.dtg.isaac.dto.QuizAssignmentDTO;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
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
 * ```
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
 * ```
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({UserAccountManager.class})
@PowerMockIgnore({ "javax.ws.*", "javax.management.*", "javax.script.*" })
abstract public class AbstractFacadeTest extends IsaacTest {
    protected HttpServletRequest request;
    protected UserAccountManager userManager;
    protected ImmutableList<QuizAssignmentDTO> allStudentAssignments;

    private Map<Object, Consumer> defaultsMap = new HashMap<>();
    private RegisteredUserDTO specialEveryoneElse = new RegisteredUserDTO();
    private RegisteredUserDTO currentUser = null;

    @Before
    public void facadeTestSetup() {
        request = createMock(HttpServletRequest.class);
        replay(request);

        userManager = createPartialMock(UserAccountManager.class, "getCurrentRegisteredUser");
    }

    /**
     * If a mock needs to have some expectations as well as stub returns, put the stub return setup in a call to this function.
     */
    protected <T, E extends Exception> void registerDefaultsFor(T mock, SickConsumer<T, E> defaults) {
        Consumer<T> safeConsumer = (t) -> {
            try {
                defaults.accept(t);
            } catch (Exception e) {
                fail("Got unexpected exception: " + e.toString());
            }
        };
        defaultsMap.put(mock, safeConsumer);
        safeConsumer.accept(mock);
    }

    /**
     * Run tests on an endpoint with different parameters.
     *
     * Only supports varying one parameter to the endpoint. Use multiple calls to vary multiple parameters.
     */
    @SafeVarargs
    protected final <E extends Exception, T> void forEndpoint(Function<T, Supplier<Response>> endpoint, WithParam<E, T>... withs) {
        for (WithParam<E, T> with: withs) {
            forEndpoint(endpoint.apply(with.with), with.checks.toArray(new Check[]{}));
        }
    }

    /**
     * Run tests on an endpoint.
     */
    @SafeVarargs
    protected final <E extends Exception> void forEndpoint(Supplier<Response> endpoint, Check<E>... checks) {
        Set<RegisteredUserDTO> users = new HashSet<>(everyone);
        for (Check<E> check: checks) {
            try {
                if (check.users != null && check.users.size() == 1 && check.users.get(0) == specialEveryoneElse) {
                    check = new Check<E>(new ArrayList<>(users), check.steps.toArray(new Step[]{}));
                } else {
                    if (check.users != null) {
                        users.removeAll(check.users);
                    }
                }
                check.runOn(endpoint);
            } catch (Exception e) {
                fail("Got unexpected exception: " + e.toString());
            }
        }
    }

    /**
     * Specify the parameter to use with forEndpoint.
     *
     * @see AbstractFacadeTest#forEndpoint(Function, WithParam[])
     */
    @SafeVarargs
    protected final <E extends Exception, T> WithParam<E, T> with(T value, Check<E>... checks) {
        return new WithParam<>(value, Arrays.asList(checks));
    }

    /**
     * Specify the user to call the endpoint as.
     */
    @SafeVarargs
    protected final <E extends Exception> Check<E> as(RegisteredUserDTO user, Step<E>... steps) {
        return new Check<>(user, steps);
    }

    /**
     * Specify a list of users to call the endpoint as.
     */
    @SafeVarargs
    protected final <E extends Exception> Check<E> as(List<RegisteredUserDTO> users, Step<E>... steps) {
        return new Check<>(users, steps);
    }

    /**
     * Specify steps to run without any call to getCurrentRegisteredUser being made.
     */
    @SafeVarargs
    protected final <E extends Exception> Check<E> beforeUserCheck(Step<E>... steps) {
        return new Check<>(steps);
    }

    /**
     * Simple check that login is required.
     */
    protected Check requiresLogin() {
        return as(noone, failsWith(SegueErrorResponse.getNotLoggedInResponse()));
    }

    /**
     * Run checks for all users that haven't be specifically "as"-ed in this block.
     *
     * Use `everyoneElse(...)` instead of `as(anyOf(thatOtherUser, andTheOtherOne, andTheLastOne), ...)`
     */
    @SafeVarargs
    protected final <E extends Exception> Check<E> everyoneElse(Step<E>... steps) {
        return new Check<>(specialEveryoneElse, steps);
    }

    /**
     * Add one or more expectations to a mock. The mock is passed in as an argument to the consumer.
     *
     * e.g. `prepare(someManager, m -> expect(m.someMethodCall(someArgs)).andReturn(someResult))`
     */
    protected <E extends Exception, T> PrepareStep<E, T> prepare(T mock, SickConsumer<T, E> preparation) {
        return new PrepareStep<>(mock, preparation);
    }

    /**
     * Get the response from making the call and perform an assertion.
     */
    protected <E extends Exception> CheckStep<E> check(Consumer<Response> checker) {
        return new CheckStep<>(checker);
    }

    /**
     * Assert the response code matches the specified response, and if a SegueErrorResponse is provided, check the messages match.
     */
    protected <E extends Exception> CheckStep<E> failsWith(Response expected) {
        return new CheckStep<>((response) -> assertErrorResponse(expected, response));
    }

    /**
     * Assert the response fails with the specified status.
     */
    protected <E extends Exception> CheckStep<E> failsWith(Status status) {
        return new CheckStep<>((response) -> assertErrorResponse(status, response));
    }

    /**
     * Assert the response is of the expected value.
     */
    protected <E extends Exception> CheckStep<E> respondsWith(Object expected) {
        return new CheckStep<>((response) -> assertEquals(expected, response.getEntity()));
    }

    /**
     * Asserts the response has a successful (2XX) status code.
     */
    protected <E extends Exception> CheckStep<E> succeeds() {
        return new CheckStep<>((response) -> assertEquals(Status.Family.SUCCESSFUL, response.getStatusInfo().getFamily()));
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
        assertEquals(expectedStatus.getStatusCode(), actual.getStatus());
    }

    private void assertErrorResponse(Response expected, Response actual) {
        assertEquals(expected.getStatus(), actual.getStatus());
        if (expected.getEntity() instanceof SegueErrorResponse) {
            SegueErrorResponse expectedError = (SegueErrorResponse) expected.getEntity();
            assertEquals(expectedError.getErrorMessage(), ((SegueErrorResponse) actual.getEntity()).getErrorMessage());
        }
    }

    interface Step<E extends Exception> {}

    /**
     * SickConsumer, because it is a Consumer that can throw.
     *
     * @param <T> What it consumes.
     * @param <E> What exception type it can throw.
     */
    @FunctionalInterface
    public interface SickConsumer<T, E extends Exception> {
        void accept(T t) throws E;
    }

    interface Task<E extends Exception> {
        void run() throws E;
    }

    class CheckStep<E extends Exception> implements Step<E> {
        private final Consumer<Response> checker;
        CheckStep(Consumer<Response> checker) {
            this.checker = checker;
        }
    }

    class PrepareStep<E extends Exception, T> implements Step<E> {
        private final T mock;
        private final SickConsumer<T, E> preparation;

        PrepareStep(T mock, SickConsumer<T, E> preparation) {
            this.mock = mock;
            this.preparation = preparation;
        }

        public void run() throws E {
            preparation.accept(mock);
        }
    }

    class WithParam<E extends Exception, T> {
        final T with;
        final List<QuizFacadeTest.Check<E>> checks;

        WithParam(T with, List<QuizFacadeTest.Check<E>> checks) {
            this.with = with;
            this.checks = checks;
        }
    }

    class Check<E extends Exception> {
        private final List<RegisteredUserDTO> users;
        private final List<Step<E>> steps;

        Check(Step[] steps) {
            this.users = null;
            this.steps = Arrays.asList(steps);
        }

        Check(RegisteredUserDTO user, Step[] steps) {
            this.users = Collections.singletonList(user);
            this.steps = Arrays.asList(steps);
        }

        Check(List<RegisteredUserDTO> users, Step[] steps) {
            this.users = users;
            this.steps = Arrays.asList(steps);
        }

        private void runOn(Supplier<Response> endpoint) throws E {
            if (users == null) {
                performSteps(endpoint).run();
            } else {
                for (RegisteredUserDTO user : users) {
                    runTaskAs(user, performSteps(endpoint));
                }
            }
        }

        private Task<E> performSteps(Supplier<Response> endpoint) {
            return () -> {
                Response response = null;
                for (Step step : steps) {
                    if (step instanceof CheckStep) {
                        if (response == null) {
                            response = endpoint.get();
                        }
                        ((CheckStep<E>) step).checker.accept(response);
                    } else if (step instanceof PrepareStep) {
                        PrepareStep<E, ?> prepareStep = (PrepareStep) step;
                        reset(prepareStep.mock);
                        if (defaultsMap.containsKey(prepareStep.mock)) {
                            defaultsMap.get(prepareStep.mock).accept(prepareStep.mock);
                        }
                        prepareStep.run();
                        replay(prepareStep.mock);
                    }
                }
            };
        }
    }

    private <E extends Exception> void runTaskAs(RegisteredUserDTO user, Task<E> task) throws E {
        try {
            reset(userManager);
            if (user == null) {
                expect(userManager.getCurrentRegisteredUser(request)).andThrow(new NoUserLoggedInException());
            } else {
                expect(userManager.getCurrentRegisteredUser(request)).andReturn(user);
            }
            replay(userManager);
            currentUser = user;

            task.run();

            verifyAll();
        } catch (NoUserLoggedInException e) {
            // Can't happen
            throw new RuntimeException(e);
        }
    }
}
