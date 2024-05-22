package uk.ac.cam.cl.dtg.isaac.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.google.api.client.util.Maps;
import ma.glasnost.orika.MapperFacade;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.SystemUtils;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.eclipse.jgit.api.Git;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;
import uk.ac.cam.cl.dtg.isaac.api.managers.AssignmentManager;
import uk.ac.cam.cl.dtg.isaac.api.managers.UserAttemptManager;
import uk.ac.cam.cl.dtg.isaac.api.managers.EventBookingManager;
import uk.ac.cam.cl.dtg.isaac.api.managers.GameManager;
import uk.ac.cam.cl.dtg.isaac.api.managers.QuizAssignmentManager;
import uk.ac.cam.cl.dtg.isaac.api.managers.QuizAttemptManager;
import uk.ac.cam.cl.dtg.isaac.api.managers.QuizManager;
import uk.ac.cam.cl.dtg.isaac.api.managers.QuizQuestionManager;
import uk.ac.cam.cl.dtg.isaac.api.managers.URIManager;
import uk.ac.cam.cl.dtg.isaac.api.services.AssignmentService;
import uk.ac.cam.cl.dtg.isaac.api.services.ContentSummarizerService;
import uk.ac.cam.cl.dtg.isaac.api.services.EmailService;
import uk.ac.cam.cl.dtg.isaac.dao.EventBookingPersistenceManager;
import uk.ac.cam.cl.dtg.isaac.dao.GameboardPersistenceManager;
import uk.ac.cam.cl.dtg.isaac.dao.IAssignmentPersistenceManager;
import uk.ac.cam.cl.dtg.isaac.dao.IQuizAssignmentPersistenceManager;
import uk.ac.cam.cl.dtg.isaac.dao.IQuizAttemptPersistenceManager;
import uk.ac.cam.cl.dtg.isaac.dao.IQuizQuestionAttemptPersistenceManager;
import uk.ac.cam.cl.dtg.isaac.dao.PgAssignmentPersistenceManager;
import uk.ac.cam.cl.dtg.isaac.dao.PgQuizAssignmentPersistenceManager;
import uk.ac.cam.cl.dtg.isaac.dao.PgQuizAttemptPersistenceManager;
import uk.ac.cam.cl.dtg.isaac.dao.PgQuizQuestionAttemptPersistenceManager;
import uk.ac.cam.cl.dtg.isaac.dos.AbstractUserPreferenceManager;
import uk.ac.cam.cl.dtg.isaac.dos.PgUserPreferenceManager;
import uk.ac.cam.cl.dtg.isaac.dos.users.RegisteredUser;
import uk.ac.cam.cl.dtg.isaac.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.isaac.quiz.PgQuestionAttempts;
import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.api.managers.GroupManager;
import uk.ac.cam.cl.dtg.segue.api.managers.PgTransactionManager;
import uk.ac.cam.cl.dtg.segue.api.managers.QuestionManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAccountManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAssociationManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAuthenticationManager;
import uk.ac.cam.cl.dtg.segue.api.monitors.EmailVerificationMisuseHandler;
import uk.ac.cam.cl.dtg.segue.api.monitors.GroupManagerLookupMisuseHandler;
import uk.ac.cam.cl.dtg.segue.api.monitors.IMisuseMonitor;
import uk.ac.cam.cl.dtg.segue.api.monitors.InMemoryMisuseMonitor;
import uk.ac.cam.cl.dtg.segue.api.monitors.RegistrationMisuseHandler;
import uk.ac.cam.cl.dtg.segue.api.services.ContentService;
import uk.ac.cam.cl.dtg.segue.auth.AuthenticationProvider;
import uk.ac.cam.cl.dtg.segue.auth.IAuthenticator;
import uk.ac.cam.cl.dtg.segue.auth.ISecondFactorAuthenticator;
import uk.ac.cam.cl.dtg.segue.auth.ISegueHashingAlgorithm;
import uk.ac.cam.cl.dtg.segue.auth.RaspberryPiOidcAuthenticator;
import uk.ac.cam.cl.dtg.segue.auth.SegueLocalAuthenticator;
import uk.ac.cam.cl.dtg.segue.auth.SeguePBKDF2v3;
import uk.ac.cam.cl.dtg.segue.auth.SegueSCryptv1;
import uk.ac.cam.cl.dtg.segue.auth.SegueTOTPAuthenticator;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.AdditionalAuthenticationRequiredException;
import uk.ac.cam.cl.dtg.segue.comm.EmailCommunicator;
import uk.ac.cam.cl.dtg.segue.comm.EmailManager;
import uk.ac.cam.cl.dtg.segue.comm.EmailMustBeVerifiedException;
import uk.ac.cam.cl.dtg.segue.comm.MailGunEmailManager;
import uk.ac.cam.cl.dtg.segue.dao.ILogManager;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.associations.PgAssociationDataManager;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentMapper;
import uk.ac.cam.cl.dtg.segue.dao.content.GitContentManager;
import uk.ac.cam.cl.dtg.segue.dao.schools.SchoolListReader;
import uk.ac.cam.cl.dtg.segue.dao.users.PgAnonymousUsers;
import uk.ac.cam.cl.dtg.segue.dao.users.PgPasswordDataManager;
import uk.ac.cam.cl.dtg.segue.dao.users.PgUserGroupPersistenceManager;
import uk.ac.cam.cl.dtg.segue.dao.users.PgUsers;
import uk.ac.cam.cl.dtg.segue.database.GitDb;
import uk.ac.cam.cl.dtg.segue.database.PostgresSqlDb;
import uk.ac.cam.cl.dtg.segue.search.ElasticSearchProvider;
import uk.ac.cam.cl.dtg.util.AbstractConfigLoader;
import uk.ac.cam.cl.dtg.util.YamlLoader;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.easymock.EasyMock.and;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.replay;
import static uk.ac.cam.cl.dtg.segue.api.Constants.*;

/**
 * Abstract superclass for integration tests, providing them with dependencies including Elasticsearch and PostgreSQL
 * (as docker containers) and other managers (some of which are mocked). Except for the Elasticsearch container, these
 * dependencies are created before and destroyed after every test class.
 *
 * Subclasses should be named "*IT.java" so Maven Failsafe detects them. Use the "verify" Maven target to run them.
 */
public abstract class IsaacIntegrationTest {

    protected static final Logger log = LoggerFactory.getLogger(IsaacIntegrationTest.class);

    private final ObjectMapper serializationMapper = new ObjectMapper();

    protected static HttpSession httpSession;
    protected static PostgreSQLContainer postgres;
    protected static ElasticsearchContainer elasticsearch;
    protected static AbstractConfigLoader properties;
    protected static Map<String, String> globalTokens;
    protected static PostgresSqlDb postgresSqlDb;
    protected static ElasticSearchProvider elasticSearchProvider;
    protected static SchoolListReader schoolListReader;
    protected static MapperFacade mapperFacade;
    protected static ContentSummarizerService contentSummarizerService;
    protected static IMisuseMonitor misuseMonitor;

    // Managers
    protected static EmailManager emailManager;
    protected static MailGunEmailManager mailGunEmailManager;
    protected static UserAuthenticationManager userAuthenticationManager;
    protected static UserAccountManager userAccountManager;
    protected static GameManager gameManager;
    protected static GroupManager groupManager;
    protected static EventBookingManager eventBookingManager;
    protected static ILogManager logManager;
    protected static GitContentManager contentManager;
    protected static UserAssociationManager userAssociationManager;
    protected static AssignmentManager assignmentManager;
    protected static QuestionManager questionManager;
    protected static QuizManager quizManager;
    protected static PgPasswordDataManager passwordDataManager;
    protected static UserAttemptManager userAttemptManager;

    // Manager dependencies
    protected static IQuizAssignmentPersistenceManager quizAssignmentPersistenceManager;
    protected static QuizAssignmentManager quizAssignmentManager;
    protected static QuizAttemptManager quizAttemptManager;
    protected static IQuizAttemptPersistenceManager quizAttemptPersistenceManager;
    protected static IQuizQuestionAttemptPersistenceManager quizQuestionAttemptPersistenceManager;
    protected static QuizQuestionManager quizQuestionManager;
    protected static PgUsers pgUsers;

    // Services
    protected static AssignmentService assignmentService;

    protected static AbstractUserPreferenceManager userPreferenceManager;

    protected static ITUsers integrationTestUsers;

    protected static Map<AuthenticationProvider, IAuthenticator> providersToRegister;

    protected static PgAnonymousUsers pgAnonymousUsers;

    protected static ISecondFactorAuthenticator secondFactorManager;

    protected class LoginResult {
        public RegisteredUserDTO user;
        public Cookie cookie;

        public LoginResult(final RegisteredUserDTO user, final Cookie cookie) {
            this.user = user;
            this.cookie = cookie;
        }
    }

    private static String getClassLoaderResourcePath(final String resource) {
        return IsaacIntegrationTest.class.getClassLoader().getResource(resource)
                .getPath()
                // ":" is removed from the resource path for Windows compatibility
                .replace(":", "");
    }

    static {
        // Statically initialise Elasticsearch once - this instance is shared across test classes.
        elasticsearch = new ElasticsearchContainer(DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch:7.17.6"))
                .withCopyFileToContainer(MountableFile.forClasspathResource("isaac-test-es-data.tar.gz"), "/usr/share/elasticsearch/isaac-test-es-data.tar.gz")
                .withCopyFileToContainer(MountableFile.forClasspathResource("isaac-test-es-docker-entrypoint.sh", 0100775), "/usr/local/bin/docker-entrypoint.sh")
                .withExposedPorts(9200, 9300)
                .withEnv("cluster.name", "isaac")
                .withEnv("node.name", "localhost")
                .withEnv("http.max_content_length", "512mb")
                .withEnv("xpack.security.enabled", "true")
                .withEnv("ELASTIC_PASSWORD", "elastic")
                .withStartupTimeout(Duration.ofSeconds(120));
        ;

        elasticsearch.start();

        try {
            elasticSearchProvider = new ElasticSearchProvider(ElasticSearchProvider.getClient(
                    "localhost",
                    elasticsearch.getMappedPort(9200),
                    "elastic",
                    "elastic"
            )
            );
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    @BeforeAll
    public static void setUpClass() throws Exception {
        // Initialise Postgres - we will create a new, clean instance for each test class.
        postgres = new PostgreSQLContainer<>("postgres:12")
                .withEnv("POSTGRES_HOST_AUTH_METHOD", "trust")
                .withUsername("rutherford")
                .withFileSystemBind(getClassLoaderResourcePath("db_scripts/postgres-rutherford-create-script.sql"), "/docker-entrypoint-initdb.d/00-isaac-create.sql")
                .withFileSystemBind(getClassLoaderResourcePath("db_scripts/postgres-rutherford-functions.sql"), "/docker-entrypoint-initdb.d/01-isaac-functions.sql")
                .withFileSystemBind(getClassLoaderResourcePath("db_scripts/quartz_scheduler_create_script.sql"), "/docker-entrypoint-initdb.d/02-isaac-quartz.sql")
                .withFileSystemBind(getClassLoaderResourcePath("test-postgres-rutherford-data-dump.sql"), "/docker-entrypoint-initdb.d/03-data-dump.sql")
        ;

        postgres.start();

        postgresSqlDb = new PostgresSqlDb(
                postgres.getJdbcUrl(),
                "rutherford",
                "somerandompassword"
        ); // user/pass are irrelevant because POSTGRES_HOST_AUTH_METHOD is set to "trust"

        String configLocation = SystemUtils.IS_OS_LINUX ? DEFAULT_LINUX_CONFIG_LOCATION : null;
        if (System.getProperty("test.config.location") != null) {
            configLocation = System.getProperty("test.config.location");
        }
        if (System.getenv("SEGUE_TEST_CONFIG_LOCATION") != null){
            configLocation = System.getenv("SEGUE_TEST_CONFIG_LOCATION");
        }

        try {
            properties = new YamlLoader(configLocation);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        globalTokens = Maps.newHashMap();
        globalTokens.put("sig", properties.getProperty(EMAIL_SIGNATURE));
        globalTokens.put("emailPreferencesURL", String.format("https://%s/account#emailpreferences", properties.getProperty(HOST_NAME)));
        globalTokens.put("myAssignmentsURL", String.format("https://%s/assignments", properties.getProperty(HOST_NAME)));
        globalTokens.put("myQuizzesURL", String.format("https://%s/quizzes", properties.getProperty(HOST_NAME)));
        globalTokens.put("myBookedEventsURL", String.format("https://%s/events?show_booked_only=true", properties.getProperty(HOST_NAME)));
        globalTokens.put("contactUsURL", String.format("https://%s/contact", properties.getProperty(HOST_NAME)));
        globalTokens.put("accountURL", String.format("https://%s/account", properties.getProperty(HOST_NAME)));
        globalTokens.put("siteBaseURL", String.format("https://%s", properties.getProperty(HOST_NAME)));

        JsonMapper jsonMapper = new JsonMapper();
        pgUsers = new PgUsers(postgresSqlDb, jsonMapper);
        pgAnonymousUsers = new PgAnonymousUsers(postgresSqlDb);
        passwordDataManager = new PgPasswordDataManager(postgresSqlDb);

        ContentMapper contentMapper = new ContentMapper(new Reflections("uk.ac.cam.cl.dtg"));
        PgQuestionAttempts pgQuestionAttempts = new PgQuestionAttempts(postgresSqlDb, contentMapper);
        questionManager = new QuestionManager(contentMapper, pgQuestionAttempts);

        mapperFacade = contentMapper.getAutoMapper();

        providersToRegister = new HashMap<>();
        providersToRegister.put(AuthenticationProvider.RASPBERRYPI, new RaspberryPiOidcAuthenticator(
                    "id",
                    "secret",
                    "http://localhost:8003/auth/raspberrypi/callback",
                    "email;profile;openid;force-consent",
                    "src/test/resources/test-rpf-idp-metadata.json"
                )
        );

        Map<String, ISegueHashingAlgorithm> algorithms = new HashMap<>(Map.of("SeguePBKDF2v3", new SeguePBKDF2v3(), "SegueSCryptv1", new SegueSCryptv1()));
        providersToRegister.put(AuthenticationProvider.SEGUE, new SegueLocalAuthenticator(pgUsers, passwordDataManager, properties, algorithms, algorithms.get("SegueSCryptv1")));

        EmailCommunicator communicator = new EmailCommunicator("localhost", "default@localhost", "Howdy!");
        userPreferenceManager = new PgUserPreferenceManager(postgresSqlDb);

        Git git = createNiceMock(Git.class);
        GitDb gitDb = new GitDb(git);
        contentManager = new GitContentManager(gitDb, elasticSearchProvider, contentMapper, properties);
        logManager = createNiceMock(ILogManager.class);

        emailManager = new EmailManager(communicator, userPreferenceManager, properties, contentManager, logManager, globalTokens);

        userAuthenticationManager = new UserAuthenticationManager(pgUsers, properties, providersToRegister, emailManager);
        secondFactorManager = createMock(SegueTOTPAuthenticator.class);
        // We don't care for MFA here so we can safely disable it
        try {
            expect(secondFactorManager.has2FAConfigured(anyObject())).andReturn(false).atLeastOnce();
        } catch (SegueDatabaseException e) {
            throw new RuntimeException(e);
        }
        replay(secondFactorManager);

        userAccountManager = new UserAccountManager(pgUsers, questionManager, properties, providersToRegister, mapperFacade, emailManager, pgAnonymousUsers, logManager, userAuthenticationManager, secondFactorManager, userPreferenceManager);

        ObjectMapper objectMapper = new ObjectMapper();
        mailGunEmailManager = new MailGunEmailManager(globalTokens, properties, userPreferenceManager);
        EventBookingPersistenceManager bookingPersistanceManager = new EventBookingPersistenceManager(postgresSqlDb, userAccountManager, contentManager, objectMapper);
        PgAssociationDataManager pgAssociationDataManager = new PgAssociationDataManager(postgresSqlDb);
        PgUserGroupPersistenceManager pgUserGroupPersistenceManager = new PgUserGroupPersistenceManager(postgresSqlDb);
        IAssignmentPersistenceManager assignmentPersistenceManager = new PgAssignmentPersistenceManager(postgresSqlDb, mapperFacade);

        GameboardPersistenceManager gameboardPersistenceManager = new GameboardPersistenceManager(postgresSqlDb, contentManager, mapperFacade, contentMapper, new URIManager(properties));
        gameManager = new GameManager(contentManager, gameboardPersistenceManager, mapperFacade, questionManager);
        groupManager = new GroupManager(pgUserGroupPersistenceManager, userAccountManager, gameManager, mapperFacade);
        userAssociationManager = new UserAssociationManager(pgAssociationDataManager, userAccountManager, groupManager);
        PgTransactionManager pgTransactionManager = new PgTransactionManager(postgresSqlDb);
        eventBookingManager = new EventBookingManager(bookingPersistanceManager, emailManager, userAssociationManager, properties, groupManager, userAccountManager, pgTransactionManager);
        assignmentManager = new AssignmentManager(assignmentPersistenceManager, groupManager, new EmailService(properties, emailManager, groupManager, userAccountManager, mailGunEmailManager), gameManager, properties);
        schoolListReader = createNiceMock(SchoolListReader.class);

        quizManager = new QuizManager(properties, new ContentService(contentManager), contentManager, new ContentSummarizerService(mapperFacade, new URIManager(properties)), contentMapper);
        quizAssignmentPersistenceManager =  new PgQuizAssignmentPersistenceManager(postgresSqlDb, mapperFacade);
        quizAssignmentManager = new QuizAssignmentManager(quizAssignmentPersistenceManager, new EmailService(properties, emailManager, groupManager, userAccountManager, mailGunEmailManager), quizManager, groupManager, properties);
        assignmentService = new AssignmentService(userAccountManager);
        quizAttemptPersistenceManager = new PgQuizAttemptPersistenceManager(postgresSqlDb, mapperFacade);
        quizAttemptManager = new QuizAttemptManager(quizAttemptPersistenceManager);
        quizQuestionAttemptPersistenceManager = new PgQuizQuestionAttemptPersistenceManager(postgresSqlDb, contentMapper);
        quizQuestionManager = new QuizQuestionManager(questionManager, contentMapper, quizQuestionAttemptPersistenceManager, quizManager, quizAttemptManager);
        userAttemptManager = new UserAttemptManager(questionManager);

        misuseMonitor = new InMemoryMisuseMonitor();
        misuseMonitor.registerHandler(GroupManagerLookupMisuseHandler.class.getSimpleName(), new GroupManagerLookupMisuseHandler(emailManager, properties));
        misuseMonitor.registerHandler(RegistrationMisuseHandler.class.getSimpleName(), new RegistrationMisuseHandler(emailManager, properties));
        misuseMonitor.registerHandler(EmailVerificationMisuseHandler.class.getSimpleName(), new EmailVerificationMisuseHandler());
        // todo: more handlers as required by different endpoints

        String someSegueAnonymousUserId = "9284723987anonymous83924923";
        httpSession = createNiceMock(HttpSession.class);
        expect(httpSession.getAttribute(Constants.ANONYMOUS_USER)).andReturn(null).anyTimes();
        expect(httpSession.getId()).andReturn(someSegueAnonymousUserId).anyTimes();
        replay(httpSession);

        contentSummarizerService = new ContentSummarizerService(mapperFacade, new URIManager(properties));

        // NOTE: The next part is commented out until we figure out a way of actually using Guice to do the heavy lifting for us..
        /*
        // Create Mocked Injector
        SegueGuiceConfigurationModule.setGlobalPropertiesIfNotSet(properties);
        Module productionModule = new SegueGuiceConfigurationModule();
        Module testModule = Modules.override(productionModule).with(new AbstractModule() {
            @Override protected void configure() {
                // ... register mocks
                bind(UserAccountManager.class).toInstance(userAccountManager);
                bind(GameManager.class).toInstance(createNiceMock(GameManager.class));
                bind(GroupChangedService.class).toInstance(createNiceMock(GroupChangedService.class));
                bind(EventBookingManager.class).toInstance(eventBookingManager);
            }
        });
        Injector injector = Guice.createInjector(testModule);
         */

        integrationTestUsers = new ITUsers(pgUsers);
    }

    @AfterAll
    public static void tearDownClass() {
        // Stop Postgres - we will create a new, clean instance for each test class.
        postgres.stop();
    }

    protected LoginResult loginAs(final HttpSession httpSession, final String username, final String password) throws Exception {
        Capture<Cookie> capturedUserCookie = Capture.newInstance(); // new Capture<Cookie>(); seems deprecated

        HttpServletRequest userLoginRequest = createNiceMock(HttpServletRequest.class);
        expect(userLoginRequest.getSession()).andReturn(httpSession).atLeastOnce();
        replay(userLoginRequest);

        HttpServletResponse userLoginResponse = createNiceMock(HttpServletResponse.class);
        userLoginResponse.addCookie(and(capture(capturedUserCookie), isA(Cookie.class)));
        expectLastCall().atLeastOnce(); // This is how you expect void methods, apparently...
        replay(userLoginResponse);

        RegisteredUserDTO user;
        try {
            user = userAccountManager.authenticateWithCredentials(userLoginRequest, userLoginResponse, AuthenticationProvider.SEGUE.toString(), username, password, false);
        } catch (AdditionalAuthenticationRequiredException | EmailMustBeVerifiedException e) {
            // In this case, we won't get a user object but the cookies have still been set.
            user = null;
        }

        return new LoginResult(user, capturedUserCookie.getValue());
    }

    protected HttpServletRequest createRequestWithCookies(final Cookie[] cookies) {
        HttpServletRequest request = createNiceMock(HttpServletRequest.class);
        expect(request.getCookies()).andReturn(cookies).anyTimes();
        return request;
    }

    protected HttpServletRequest createRequestWithSession() {
        HttpServletRequest request = createNiceMock(HttpServletRequest.class);
        expect(request.getSession()).andReturn(httpSession).anyTimes();
        return request;
    }

    protected HttpServletResponse createResponseAndCaptureCookies(Capture<Cookie> cookieToCapture) {
        HttpServletResponse response = createNiceMock(HttpServletResponse.class);
        response.addCookie(capture(cookieToCapture));
        EasyMock.expectLastCall();
        return response;
    }

    protected HashMap<String, String> getSessionInformationFromCookie(Cookie cookie) throws Exception {
        return this.serializationMapper.readValue(Base64.decodeBase64(cookie.getValue()), HashMap.class);
    }

    protected List<String> getCaveatsFromCookie(Cookie cookie) throws Exception {
        return serializationMapper.readValue(getSessionInformationFromCookie(cookie)
                        .get(SESSION_CAVEATS), new TypeReference<ArrayList<String>>(){});
    }

    static Set<RegisteredUser> allTestUsersProvider() {
        return integrationTestUsers.ALL;
    }
}
