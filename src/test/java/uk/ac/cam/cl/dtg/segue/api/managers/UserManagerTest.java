/**
 * Copyright 2014 Stephen Cummins and Nick Rogers
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

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import ma.glasnost.orika.MapperFacade;

import org.easymock.EasyMock;
import org.elasticsearch.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.reflect.Whitebox;

import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.auth.AuthenticationProvider;
import uk.ac.cam.cl.dtg.segue.auth.FacebookAuthenticator;
import uk.ac.cam.cl.dtg.segue.auth.IAuthenticator;
import uk.ac.cam.cl.dtg.segue.auth.IFederatedAuthenticator;
import uk.ac.cam.cl.dtg.segue.auth.IOAuth2Authenticator;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.CodeExchangeException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserLoggedInException;
import uk.ac.cam.cl.dtg.segue.comm.EmailManager;
import uk.ac.cam.cl.dtg.segue.dao.ILogManager;
import uk.ac.cam.cl.dtg.segue.dao.users.IUserDataManager;
import uk.ac.cam.cl.dtg.segue.dos.users.AnonymousUser;
import uk.ac.cam.cl.dtg.segue.dos.users.EmailVerificationStatus;
import uk.ac.cam.cl.dtg.segue.dos.users.Gender;
import uk.ac.cam.cl.dtg.segue.dos.users.RegisteredUser;
import uk.ac.cam.cl.dtg.segue.dos.users.Role;
import uk.ac.cam.cl.dtg.segue.dos.users.UserFromAuthProvider;
import uk.ac.cam.cl.dtg.segue.dto.users.AnonymousUserDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableMap;

/**
 * Test class for the user manager class.
 * 
 */
@PowerMockIgnore({ "javax.ws.*" })
public class UserManagerTest {
    private IUserDataManager dummyDatabase;
    private String dummyHMACSalt;
    private Map<AuthenticationProvider, IAuthenticator> dummyProvidersMap;
    private String dummyHostName;
    private PropertiesLoader dummyPropertiesLoader;
    private static final String CSRF_TEST_VALUE = "CSRFTESTVALUE";

    private MapperFacade dummyMapper;
    private EmailManager dummyQueue;
    private SimpleDateFormat sdf;

    private Cache<String, AnonymousUser> dummyUserCache;
    private ILogManager dummyLogManager;

    /**
     * Initial configuration of tests.
     * 
     * @throws Exception
     *             - test exception
     */
    @Before
    public final void setUp() throws Exception {
        this.dummyDatabase = createMock(IUserDataManager.class);
        this.dummyHMACSalt = "BOB";
        this.dummyProvidersMap = new HashMap<AuthenticationProvider, IAuthenticator>();
        this.dummyHostName = "bob";
        this.dummyMapper = createMock(MapperFacade.class);
        this.dummyQueue = createMock(EmailManager.class);
        this.dummyPropertiesLoader = createMock(PropertiesLoader.class);
        this.sdf = new SimpleDateFormat("EEE MMM dd HH:mm:ss Z yyyy");

        this.dummyUserCache = CacheBuilder.newBuilder()
                .expireAfterAccess(Constants.ANONYMOUS_SESSION_DURATION_IN_MINUTES, TimeUnit.MINUTES)
                .<String, AnonymousUser> build();

        this.dummyLogManager = createMock(ILogManager.class);

        expect(this.dummyPropertiesLoader.getProperty(Constants.HMAC_SALT)).andReturn(dummyHMACSalt).anyTimes();
        expect(this.dummyPropertiesLoader.getProperty(Constants.HOST_NAME)).andReturn(dummyHostName).anyTimes();
        expect(this.dummyPropertiesLoader.getProperty(Constants.SESSION_EXPIRY_SECONDS)).andReturn("60").anyTimes();
        replay(this.dummyPropertiesLoader);
    }

    /**
     * Verify that the constructor responds correctly to bad input.
     */
    @Test
    public final void userManager_checkConstructorForBadInput_exceptionsShouldBeThrown() {
        try {
            new UserManager(null, this.dummyPropertiesLoader, this.dummyProvidersMap, this.dummyMapper,
                    this.dummyQueue, this.dummyLogManager);
            fail("Expected a null pointer exception immediately");
        } catch (NullPointerException e) {
            // fine
        }
        try {
            new UserManager(this.dummyDatabase, null, this.dummyProvidersMap, this.dummyMapper, this.dummyQueue,
                    this.dummyLogManager);
            fail("Expected a null pointer exception immediately");
        } catch (NullPointerException e) {
            // fine
        }
        try {
            new UserManager(this.dummyDatabase, this.dummyPropertiesLoader, null, this.dummyMapper, this.dummyQueue,
                    this.dummyLogManager);
            fail("Expected a null pointer exception immediately");
        } catch (NullPointerException e) {
            // fine
        }
    }

    /**
     * Test that the get current user method behaves correctly when not logged in.
     */
    @Test
    public final void getCurrentUser_isNotLoggedIn_NoUserLoggedInExceptionThrown() {
        UserManager userManager = buildTestUserManager();

        HttpSession dummySession = createMock(HttpSession.class);
        HttpServletRequest request = createMock(HttpServletRequest.class);

        Cookie[] emptyCookies = {};
        expect(request.getCookies()).andReturn(emptyCookies).anyTimes();

        replay(dummySession);
        replay(request);
        replay(dummyDatabase);

        // Act
        try {
            userManager.getCurrentRegisteredUser(request);

            // Assert
            fail("Expected NoUserLoggedInException");
        } catch (NoUserLoggedInException e) {
            // fine
        }

        verify(dummyDatabase, dummySession, request);
    }

    /**
     * Test that get current user with valid HMAC works correctly.
     * 
     * @throws Exception
     */
    @Test
    public final void getCurrentUser_IsAuthenticatedWithValidHMAC_userIsReturned() throws Exception {
        UserManager userManager = buildTestUserManager();
        HttpServletRequest request = createMock(HttpServletRequest.class);

        String validUserId = "533ee66842f639e95ce35e29";
        String validDateString = sdf.format(new Date());

        Map<String, String> sessionInformation = getSessionInformationAsAMap(userManager, validUserId, validDateString);
        Cookie[] cookieWithSessionInfo = getCookieArray(sessionInformation);

        RegisteredUser returnUser = new RegisteredUser(validUserId, "TestFirstName", "TestLastName", "", Role.STUDENT,
                new Date(), Gender.MALE, new Date(), null, null, null, null, new Date(), null, null, 
                null);

        dummyDatabase.updateUserLastSeen(validUserId);
        expectLastCall();

        expect(request.getCookies()).andReturn(cookieWithSessionInfo).anyTimes();
        replay(request);

        expect(dummyDatabase.getById("533ee66842f639e95ce35e29")).andReturn(returnUser);
        expect(dummyDatabase.getAuthenticationProvidersByUser(returnUser)).andReturn(
                Arrays.asList(AuthenticationProvider.GOOGLE));
        replay(dummyDatabase);

        expect(dummyMapper.map(returnUser, RegisteredUserDTO.class)).andReturn(new RegisteredUserDTO()).atLeastOnce();
        replay(dummyMapper);
        // Act
        RegisteredUserDTO user = userManager.getCurrentRegisteredUser(request);

        // Assert
        assertTrue(user != null);

        verify(dummyDatabase, request, dummyMapper);
    }

    /**
     * Test that requesting authentication with a bad provider behaves as expected.
     * 
     * @throws Exception
     */
    @Test
    public final void authenticate_badProviderGiven_givesServerErrorResponse() throws Exception {
        UserManager userManager = buildTestUserManager();

        HttpServletRequest request = createMock(HttpServletRequest.class);

        Cookie[] cookieWithSessionInfo = {}; // empty as not logged in.

        expect(request.getCookies()).andReturn(cookieWithSessionInfo).atLeastOnce();

        String someInvalidProvider = "BAD_PROVIDER!!";
        Status expectedResponseCode = Status.BAD_REQUEST;

        replay(request);
        replay(dummyDatabase);

        // Act
        Response r = userManager.authenticate(request, someInvalidProvider);

        // Assert
        assertTrue(r.getStatus() == expectedResponseCode.getStatusCode());
        verify(dummyDatabase, request);
    }

    /**
     * Test that a valid OAuth provider (Facebook) provides a redirect response.
     * 
     * @throws IOException
     *             - test exception
     */
    @Test
    public final void authenticate_selectedValidOAuthProvider_providesRedirectResponseForAuthorization()
            throws IOException {
        // Arrange
        IOAuth2Authenticator dummyAuth = createMock(IOAuth2Authenticator.class);
        UserManager userManager = buildTestUserManager(AuthenticationProvider.TEST, dummyAuth);

        HttpSession dummySession = createMock(HttpSession.class);
        HttpServletRequest request = createMock(HttpServletRequest.class);
        String exampleRedirectUrl = "https://accounts.google.com/o/oauth2/auth?"
                + "client_id=267566420063-jalcbiffcpmteh42cib5hmgb16upspc0.apps.googleusercontent.com&"
                + "redirect_uri=http://localhost:8080/rutherford-server/segue/api/auth/google/callback&"
                + "response_type=code&scope=https://www.googleapis.com/auth/userinfo.profile%20"
                + "https://www.googleapis.com/auth/userinfo.email&state=googleomrdd07hbe6vc1efim5rnsgvms";
        String someValidProviderString = "test";
        Status expectedResponseCode = Status.OK;

        // for CSRF state information
        expect(request.getSession()).andReturn(dummySession).atLeastOnce();
        dummySession.setAttribute(EasyMock.<String> anyObject(), EasyMock.<String> anyObject());
        expectLastCall().atLeastOnce();

        Cookie[] cookieWithSessionInfo = {}; // empty as not logged in.
        expect(request.getCookies()).andReturn(cookieWithSessionInfo).atLeastOnce();

        replay(dummySession);
        replay(request);
        replay(dummyDatabase);

        String someAntiForgeryToken = "someAntiForgeryToken";
        expect(dummyAuth.getAntiForgeryStateToken()).andReturn(someAntiForgeryToken).once();
        expect(dummyAuth.getAuthorizationUrl(someAntiForgeryToken)).andReturn(exampleRedirectUrl).once();
        replay(dummyAuth);

        // Act
        Response r = userManager.authenticate(request, someValidProviderString);

        // Assert
        verify(dummyDatabase, request);

        @SuppressWarnings("unchecked")
        Map<String, URI> result = (Map<String, URI>) r.getEntity();

        assertTrue(result.get(Constants.REDIRECT_URL).toString().equals(exampleRedirectUrl));
        assertTrue(r.getStatus() == expectedResponseCode.getStatusCode());
    }

    /**
     * Check that a new (unseen) user is registered when seen with 3rd party authenticator.
     * 
     * @throws Exception
     *             -
     */
    @Test
    public final void authenticateCallback_checkNewUserIsAuthenticated_createInternalUserAccount() throws Exception {
        IOAuth2Authenticator dummyAuth = createMock(FacebookAuthenticator.class);
        UserManager userManager = buildTestUserManager(AuthenticationProvider.TEST, dummyAuth);

        // method param setup for method under test
        HttpSession dummySession = createMock(HttpSession.class);
        HttpServletRequest request = createMock(HttpServletRequest.class);
        HttpServletResponse response = createMock(HttpServletResponse.class);

        String someDomain = "http://www.somedomain.com/";
        String someClientId = "someClientId";
        String someAuthCode = "someAuthCode";
        String someState = "someState";

        StringBuffer sb = new StringBuffer(someDomain + "?state=" + someState + "&code=" + someAuthCode);
        String validQueryStringFromProvider = "client_id=" + someClientId + "&redirect_uri=" + someDomain;
        String fullResponseUrlFromProvider = someDomain + "?state=" + someState + "&code=" + someAuthCode
                + "?client_id=" + someClientId + "&redirect_uri=" + someDomain;
        String someProviderGeneratedLookupValue = "MYPROVIDERREF";
        String someProviderUniqueUserId = "USER-1";

        String someSegueUserId = "533ee66842f639e95ce35e29";
        String someSegueAnonymousUserId = "9284723987anonymous83924923";

        AnonymousUser au = new AnonymousUser();
        au.setSessionId(someSegueAnonymousUserId);
        this.dummyUserCache.put(au.getSessionId(), au);

        AnonymousUserDTO someAnonymousUserDTO = new AnonymousUserDTO();
        someAnonymousUserDTO.setSessionId(someSegueAnonymousUserId);

        String validOAuthProvider = "test";
        String validDateString = sdf.format(new Date());

        expect(request.getSession()).andReturn(dummySession).atLeastOnce();

        Cookie[] cookieWithoutSessionInfo = {}; // empty as not logged in.
        expect(request.getCookies()).andReturn(cookieWithoutSessionInfo).times(2);

        expect(dummySession.getAttribute(Constants.ANONYMOUS_USER)).andReturn(someSegueAnonymousUserId).atLeastOnce(); // session
                                                                                                                       // id

        // Mock CSRF checks
        expect(dummySession.getAttribute(Constants.STATE_PARAM_NAME)).andReturn(CSRF_TEST_VALUE).atLeastOnce();
        expect(request.getParameter(Constants.STATE_PARAM_NAME)).andReturn(CSRF_TEST_VALUE).atLeastOnce();

        // Mock URL params extract stuff
        expect(request.getQueryString()).andReturn(validQueryStringFromProvider).atLeastOnce();
        expect(request.getRequestURL()).andReturn(sb);

        // Mock extract auth code call
        expect(dummyAuth.extractAuthCode(fullResponseUrlFromProvider)).andReturn(someAuthCode);

        // Mock exchange code for token call
        expect(dummyAuth.exchangeCode(someAuthCode)).andReturn(someProviderGeneratedLookupValue).atLeastOnce();

        expect(((IFederatedAuthenticator) dummyAuth).getAuthenticationProvider())
                .andReturn(AuthenticationProvider.TEST).atLeastOnce();

        // User object back from provider
        UserFromAuthProvider providerUser = new UserFromAuthProvider(someProviderUniqueUserId, "TestFirstName",
                "TestLastName", "", Role.STUDENT, new Date(), Gender.MALE);

        // Mock get User Information from provider call
        expect(((IFederatedAuthenticator) dummyAuth).getUserInfo(someProviderGeneratedLookupValue)).andReturn(
                providerUser).atLeastOnce();

        // Expect this to be a new user and to register them (i.e. return null
        // from database)
        expect(dummyDatabase.getByLinkedAccount(AuthenticationProvider.TEST, someProviderUniqueUserId)).andReturn(null)
                .atLeastOnce();

        RegisteredUser mappedUser = new RegisteredUser(null, "TestFirstName", "testLastName", "", Role.STUDENT,
                new Date(), Gender.MALE, new Date(), null, null, null, null, new Date(), null, null, null);

        expect(dummyDatabase.getAuthenticationProvidersByUser(mappedUser)).andReturn(
                Lists.newArrayList(AuthenticationProvider.GOOGLE)).atLeastOnce();

        RegisteredUserDTO mappedUserDTO = new RegisteredUserDTO();

        expect(dummyMapper.map(providerUser, RegisteredUser.class)).andReturn(mappedUser).atLeastOnce();
        expect(dummyMapper.map(mappedUser, RegisteredUserDTO.class)).andReturn(mappedUserDTO).atLeastOnce();
        expect(dummyMapper.map(au, AnonymousUserDTO.class)).andReturn(someAnonymousUserDTO).once();

        // A main part of the test is to check the below call happens
        expect(
                dummyDatabase.registerNewUserWithProvider(mappedUser, AuthenticationProvider.TEST,
                        someProviderUniqueUserId)).andReturn(someSegueUserId).atLeastOnce();

        mappedUser.setDbId(someSegueUserId);

        expect(dummyDatabase.getById(someSegueUserId)).andReturn(mappedUser);

        Map<String, String> sessionInformation = getSessionInformationAsAMap(userManager, someSegueUserId,
                validDateString);
        Cookie[] cookieWithSessionInfo = getCookieArray(sessionInformation);

        // Expect a session to be created
        response.addCookie(cookieWithSessionInfo[0]);
        expectLastCall().once();
        expect(request.getCookies()).andReturn(cookieWithSessionInfo).anyTimes();

        replay(dummySession, request, dummyAuth, dummyDatabase, dummyMapper);

        // Act
        Response r = userManager.authenticateCallback(request, response, validOAuthProvider);

        // Assert
        verify(dummySession, request, dummyAuth, dummyDatabase);
        assertTrue(r.getStatusInfo().equals(Status.OK));
    }

    // TODO: Write a test to check what happens with anonymous users and merges.

    /**
     * Verify that a bad CSRF response from the authentication provider causes an error response.
     * 
     * @throws IOException
     *             - test exceptions
     * @throws CodeExchangeException
     *             - test exceptions
     * @throws NoUserException
     *             - test exceptions
     */
    @Test
    public final void authenticateCallback_checkInvalidCSRF_returnsUnauthorizedResponse() throws IOException,
            CodeExchangeException, NoUserException {
        UserManager userManager = buildTestUserManager();

        HttpSession dummySession = createMock(HttpSession.class);
        HttpServletRequest request = createMock(HttpServletRequest.class);
        HttpServletResponse response = createMock(HttpServletResponse.class);

        String someInvalidCSRFValue = "FRAUDHASHAPPENED";
        String validOAuthProvider = "test";
        Status expectedResponseCode = Status.UNAUTHORIZED;

        expect(request.getSession()).andReturn(dummySession).atLeastOnce();
        expect(dummySession.getAttribute(Constants.SESSION_USER_ID)).andReturn(null).anyTimes();

        // Mock URL params extract stuff
        // Return any non-null string
        String queryString = Constants.STATE_PARAM_NAME + "=" + someInvalidCSRFValue;
        expect(request.getQueryString()).andReturn(queryString).once();

        // Mock CSRF checks
        expect(dummySession.getAttribute(Constants.STATE_PARAM_NAME)).andReturn(CSRF_TEST_VALUE).atLeastOnce();

        expect(request.getParameter(Constants.STATE_PARAM_NAME)).andReturn(someInvalidCSRFValue).atLeastOnce();

        replay(dummySession, request, dummyDatabase);

        // Act
        Response r = userManager.authenticateCallback(request, response, validOAuthProvider);

        // Assert
        verify(dummyDatabase, dummySession, request);
        assertTrue(r.getStatus() == expectedResponseCode.getStatusCode());
    }

    /**
     * Verify that a bad (null) CSRF response from the authentication provider causes an error response.
     * 
     * @throws IOException
     *             - test exceptions
     * @throws CodeExchangeException
     *             - test exceptions
     * @throws NoUserException
     *             - test exceptions
     */
    @Test
    public final void authenticateCallback_checkWhenNoCSRFProvided_respondWithUnauthorized() throws IOException,
            CodeExchangeException, NoUserException {
        UserManager userManager = buildTestUserManager();

        // method param setup for method under test
        HttpSession dummySession = createMock(HttpSession.class);
        HttpServletRequest request = createMock(HttpServletRequest.class);
        HttpServletResponse response = createMock(HttpServletResponse.class);

        String validOAuthProvider = "test";
        Status expectedResponseCode = Status.UNAUTHORIZED;

        expect(request.getSession()).andReturn(dummySession).atLeastOnce();
        expect(dummySession.getAttribute(Constants.SESSION_USER_ID)).andReturn(null).anyTimes();

        // Mock URL params extract stuff
        expect(request.getQueryString()).andReturn("").atLeastOnce();

        // Mock CSRF checks
        expect(dummySession.getAttribute(Constants.STATE_PARAM_NAME)).andReturn(null).atLeastOnce();
        expect(request.getParameter(Constants.STATE_PARAM_NAME)).andReturn(CSRF_TEST_VALUE).atLeastOnce();

        replay(dummySession);
        replay(request);
        replay(dummyDatabase);

        // Act
        Response r = userManager.authenticateCallback(request, response, validOAuthProvider);

        // Assert
        verify(dummyDatabase, dummySession, request);
        assertTrue(r.getStatus() == expectedResponseCode.getStatusCode());
    }

    /**
     * Verify that a correct HMAC response works correctly.
     * 
     * This method is dependent on the crypto algorithm used.
     * 
     * @throws Exception
     */
    @Test
    public final void validateUsersSession_checkForValidHMAC_shouldReturnAsCorrect() throws Exception {
        UserManager userManager = buildTestUserManager();

        // method param setup for method under test
        HttpSession dummySession = createMock(HttpSession.class);
        HttpServletRequest request = createMock(HttpServletRequest.class);

        String validUserId = "533ee66842f639e95ce35e29";
        String validDateString = sdf.format(new Date());

        Map<String, String> sessionInformation = getSessionInformationAsAMap(userManager, validUserId, validDateString);

        replay(dummySession);
        replay(request);
        replay(dummyDatabase);

        // Act
        boolean valid = Whitebox.<Boolean> invokeMethod(userManager, "isValidUsersSession", sessionInformation);

        // Assert
        verify(dummyDatabase, dummySession, request);
        assertTrue(valid);
    }

    /**
     * Verify that a user session which has been tampered with is detected as invalid.
     * 
     * @throws Exception
     */
    @Test
    public final void validateUsersSession_badUsersSession_shouldReturnAsIncorrect() throws Exception {
        UserManager userManager = buildTestUserManager();

        // method param setup for method under test
        HttpSession dummySession = createMock(HttpSession.class);
        HttpServletRequest request = createMock(HttpServletRequest.class);

        String validUserId = "533ee66842f639e95ce35e29";
        String validDateString = sdf.format(new Date());

        Map<String, String> validSessionInformation = getSessionInformationAsAMap(userManager, validUserId,
                validDateString);

        Map<String, String> tamperedSessionInformation = ImmutableMap.of(Constants.SESSION_USER_ID, validUserId,
                Constants.DATE_SIGNED, validDateString + "1", Constants.HMAC,
                validSessionInformation.get(Constants.HMAC));

        replay(dummySession);
        replay(request);
        replay(dummyDatabase);

        // Act
        boolean valid = Whitebox.<Boolean> invokeMethod(userManager, "isValidUsersSession", tamperedSessionInformation);

        // Assert
        verify(dummyDatabase, dummySession, request);
        assertTrue(!valid);
    }

    /**
     * Verify that an expired user session is detected as invalid.
     * 
     * @throws Exception
     */
    @Test
    public final void validateUsersSession_expiredUsersSession_shouldReturnAsIncorrect() throws Exception {
        UserManager userManager = buildTestUserManager();

        // method param setup for method under test
        HttpSession dummySession = createMock(HttpSession.class);
        HttpServletRequest request = createMock(HttpServletRequest.class);

        String validUserId = "533ee66842f639e95ce35e29";
        String validDateString = sdf.format(sdf.parse("Mon Oct 06 15:36:27 +0100 2013"));

        Map<String, String> validSessionInformation = getSessionInformationAsAMap(userManager, validUserId,
                validDateString);

        replay(dummySession);
        replay(request);
        replay(dummyDatabase);

        // Act
        boolean valid = Whitebox.<Boolean> invokeMethod(userManager, "isValidUsersSession", validSessionInformation);

        // Assert
        verify(dummyDatabase, dummySession, request);
        assertTrue(!valid);
    }

    /**
     * Helper method to construct a UserManager with the default TEST provider.
     * 
     * @return A new UserManager instance
     */
    private UserManager buildTestUserManager() {
        return buildTestUserManager(AuthenticationProvider.TEST, createMock(IOAuth2Authenticator.class));
    }

    /**
     * Helper method to construct a UserManager with the specified providers.
     * 
     * @param provider
     *            - The provider to register
     * @param authenticator
     *            - The associated authenticating engine
     * @return A new UserManager instance
     */
    private UserManager buildTestUserManager(final AuthenticationProvider provider,
            final IFederatedAuthenticator authenticator) {
        HashMap<AuthenticationProvider, IAuthenticator> providerMap = new HashMap<AuthenticationProvider, IAuthenticator>();
        providerMap.put(provider, authenticator);
        return new UserManager(this.dummyDatabase, this.dummyPropertiesLoader, providerMap, this.dummyMapper,
                this.dummyQueue, this.dummyUserCache, this.dummyLogManager);
    }

    private Map<String, String> getSessionInformationAsAMap(UserManager userManager, String userId, String dateCreated)
            throws Exception {
        String validHMAC = Whitebox.<String> invokeMethod(userManager, "calculateSessionHMAC", dummyHMACSalt, userId,
                dateCreated);
        return ImmutableMap.of(Constants.SESSION_USER_ID, userId, Constants.DATE_SIGNED, dateCreated, Constants.HMAC,
                validHMAC);
    }

    private Cookie[] getCookieArray(Map<String, String> sessionInformation) throws JsonProcessingException {
        ObjectMapper om = new ObjectMapper();
        Cookie[] cookieWithSessionInfo = { new Cookie(Constants.SEGUE_AUTH_COOKIE,
                om.writeValueAsString(sessionInformation)) };
        return cookieWithSessionInfo;
    }
}
