package uk.ac.cam.cl.dtg.isaac.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.google.api.client.util.Maps;
import ma.glasnost.orika.MapperFacade;
import org.apache.commons.lang3.SystemUtils;
import org.easymock.Capture;
import org.eclipse.jgit.api.Git;
import org.junit.BeforeClass;
import org.reflections.Reflections;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;
import uk.ac.cam.cl.dtg.isaac.api.managers.AssignmentManager;
import uk.ac.cam.cl.dtg.isaac.api.managers.EventBookingManager;
import uk.ac.cam.cl.dtg.isaac.api.managers.GameManager;
import uk.ac.cam.cl.dtg.isaac.api.managers.URIManager;
import uk.ac.cam.cl.dtg.isaac.api.services.EmailService;
import uk.ac.cam.cl.dtg.isaac.dao.EventBookingPersistenceManager;
import uk.ac.cam.cl.dtg.isaac.dao.GameboardPersistenceManager;
import uk.ac.cam.cl.dtg.isaac.dao.IAssignmentPersistenceManager;
import uk.ac.cam.cl.dtg.isaac.dao.PgAssignmentPersistenceManager;
import uk.ac.cam.cl.dtg.isaac.dos.AbstractUserPreferenceManager;
import uk.ac.cam.cl.dtg.isaac.dos.PgUserPreferenceManager;
import uk.ac.cam.cl.dtg.isaac.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.isaac.quiz.PgQuestionAttempts;
import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.api.managers.GroupManager;
import uk.ac.cam.cl.dtg.segue.api.managers.PgTransactionManager;
import uk.ac.cam.cl.dtg.segue.api.managers.QuestionManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAccountManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAssociationManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAuthenticationManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserBadgeManager;
import uk.ac.cam.cl.dtg.segue.auth.AuthenticationProvider;
import uk.ac.cam.cl.dtg.segue.auth.IAuthenticator;
import uk.ac.cam.cl.dtg.segue.auth.ISecondFactorAuthenticator;
import uk.ac.cam.cl.dtg.segue.auth.ISegueHashingAlgorithm;
import uk.ac.cam.cl.dtg.segue.auth.SegueLocalAuthenticator;
import uk.ac.cam.cl.dtg.segue.auth.SeguePBKDF2v3;
import uk.ac.cam.cl.dtg.segue.auth.SegueSCryptv1;
import uk.ac.cam.cl.dtg.segue.auth.SegueTOTPAuthenticator;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.AdditionalAuthenticationRequiredException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.AuthenticationProviderMappingException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.IncorrectCredentialsProvidedException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.MFARequiredButNotConfiguredException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoCredentialsAvailableException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserException;
import uk.ac.cam.cl.dtg.segue.comm.EmailCommunicator;
import uk.ac.cam.cl.dtg.segue.comm.EmailManager;
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
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.easymock.EasyMock.and;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.replay;
import static uk.ac.cam.cl.dtg.segue.api.Constants.DEFAULT_LINUX_CONFIG_LOCATION;
import static uk.ac.cam.cl.dtg.segue.api.Constants.EMAIL_SIGNATURE;
import static uk.ac.cam.cl.dtg.segue.api.Constants.HOST_NAME;

public class IsaacIntegrationTest {

    protected static HttpSession httpSession;
    protected static PostgreSQLContainer postgres;
    protected static ElasticsearchContainer elasticsearch;
    protected static PropertiesLoader properties;
    protected static Map<String, String> globalTokens;
    protected static PostgresSqlDb postgresSqlDb;
    protected static ElasticSearchProvider elasticSearchProvider;
    protected static SchoolListReader schoolListReader;
    protected static MapperFacade mapperFacade;

    // Managers
    protected static EmailManager emailManager;
    protected static UserAuthenticationManager userAuthenticationManager;
    protected static UserAccountManager userAccountManager;
    protected static GameManager gameManager;
    protected static GroupManager groupManager;
    protected static EventBookingManager eventBookingManager;
    protected static ILogManager logManager;
    protected static GitContentManager contentManager;
    protected static UserBadgeManager userBadgeManager;
    protected static UserAssociationManager userAssociationManager;
    protected static AssignmentManager assignmentManager;
    protected static QuestionManager questionManager;

    protected class LoginResult {
        public RegisteredUserDTO user;
        public Cookie cookie;

        public LoginResult(final RegisteredUserDTO user, final Cookie cookie) {
            this.user = user;
            this.cookie = cookie;
        }
    }

    @BeforeClass
    public static void setUpClass() {
        postgres = new PostgreSQLContainer<>("postgres:12")
                .withEnv("POSTGRES_HOST_AUTH_METHOD", "trust")
                .withUsername("rutherford")
                .withFileSystemBind(IsaacIntegrationTest.class.getClassLoader().getResource("db_scripts/postgres-rutherford-create-script.sql").getPath(), "/docker-entrypoint-initdb.d/00-isaac-create.sql")
                .withFileSystemBind(IsaacIntegrationTest.class.getClassLoader().getResource("db_scripts/postgres-rutherford-functions.sql").getPath(), "/docker-entrypoint-initdb.d/01-isaac-functions.sql")
                .withFileSystemBind(IsaacIntegrationTest.class.getClassLoader().getResource("db_scripts/quartz_scheduler_create_script.sql").getPath(), "/docker-entrypoint-initdb.d/02-isaac-quartz.sql")
                .withFileSystemBind(IsaacIntegrationTest.class.getClassLoader().getResource("test-postgres-rutherford-data-dump.sql").getPath(), "/docker-entrypoint-initdb.d/03-data-dump.sql")
        ;

        // TODO It would be nice if we could pull the version from pom.xml
        elasticsearch = new ElasticsearchContainer(DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch:7.14.2"))
                .withCopyFileToContainer(MountableFile.forClasspathResource("isaac-test-es-data.tar.gz"), "/usr/share/elasticsearch/isaac-test-es-data.tar.gz")
                .withCopyFileToContainer(MountableFile.forClasspathResource("isaac-test-es-docker-entrypoint.sh", 0100775), "/usr/local/bin/docker-entrypoint.sh")
                .withExposedPorts(9200, 9300)
                .withEnv("cluster.name", "isaac")
                .withEnv("node.name", "localhost")
                .withStartupTimeout(Duration.ofSeconds(120));
        ;

        postgres.start();
        elasticsearch.start();

        postgresSqlDb = new PostgresSqlDb(
                postgres.getJdbcUrl(),
                "rutherford",
                "somerandompassword"
        ); // user/pass are irrelevant because POSTGRES_HOST_AUTH_METHOD is set to "trust"

        try {
            elasticSearchProvider =
                    new ElasticSearchProvider(ElasticSearchProvider.getTransportClient(
                            "isaac",
                            "localhost",
                            elasticsearch.getMappedPort(9300))
                    );
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }


        String configLocation = SystemUtils.IS_OS_LINUX ? DEFAULT_LINUX_CONFIG_LOCATION : null;
        if (System.getProperty("test.config.location") != null) {
            configLocation = System.getProperty("test.config.location");
        }
        if (System.getenv("SEGUE_TEST_CONFIG_LOCATION") != null){
            configLocation = System.getenv("SEGUE_TEST_CONFIG_LOCATION");
        }

        try {
            properties = new PropertiesLoader(configLocation);
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
        PgUsers pgUsers = new PgUsers(postgresSqlDb, jsonMapper);
        PgAnonymousUsers pgAnonymousUsers = new PgAnonymousUsers(postgresSqlDb);
        PgPasswordDataManager passwordDataManager = new PgPasswordDataManager(postgresSqlDb);

        ContentMapper contentMapper = new ContentMapper(new Reflections("uk.ac.cam.cl.dtg"));
        PgQuestionAttempts pgQuestionAttempts = new PgQuestionAttempts(postgresSqlDb, contentMapper);
        questionManager = new QuestionManager(contentMapper, pgQuestionAttempts);

        mapperFacade = contentMapper.getAutoMapper();

        // The following may need some actual authentication providers...
        Map<AuthenticationProvider, IAuthenticator> providersToRegister = new HashMap<>();
        Map<String, ISegueHashingAlgorithm> algorithms = new HashMap<>(Map.of("SeguePBKDF2v3", new SeguePBKDF2v3(), "SegueSCryptv1", new SegueSCryptv1()));
        providersToRegister.put(AuthenticationProvider.SEGUE, new SegueLocalAuthenticator(pgUsers, passwordDataManager, properties, algorithms, algorithms.get("SegueSCryptv1")));

        EmailCommunicator communicator = new EmailCommunicator("localhost", "default@localhost", "Howdy!");
        AbstractUserPreferenceManager userPreferenceManager = new PgUserPreferenceManager(postgresSqlDb);

        Git git = createNiceMock(Git.class);
        GitDb gitDb = new GitDb(git);
        contentManager = new GitContentManager(gitDb, elasticSearchProvider, contentMapper, properties);
        logManager = createNiceMock(ILogManager.class);

        emailManager = new EmailManager(communicator, userPreferenceManager, properties, contentManager, logManager, globalTokens);

        userAuthenticationManager = new UserAuthenticationManager(pgUsers, properties, providersToRegister, emailManager);
        ISecondFactorAuthenticator secondFactorManager = createMock(SegueTOTPAuthenticator.class);
        // We don't care for MFA here so we can safely disable it
        try {
            expect(secondFactorManager.has2FAConfigured(anyObject())).andReturn(false).atLeastOnce();
        } catch (SegueDatabaseException e) {
            throw new RuntimeException(e);
        }
        replay(secondFactorManager);

        userAccountManager = new UserAccountManager(pgUsers, questionManager, properties, providersToRegister, mapperFacade, emailManager, pgAnonymousUsers, logManager, userAuthenticationManager, secondFactorManager, userPreferenceManager);

        ObjectMapper objectMapper = new ObjectMapper();
        EventBookingPersistenceManager bookingPersistanceManager = new EventBookingPersistenceManager(postgresSqlDb, userAccountManager, contentManager, objectMapper);
        PgAssociationDataManager pgAssociationDataManager = new PgAssociationDataManager(postgresSqlDb);
        PgUserGroupPersistenceManager pgUserGroupPersistenceManager = new PgUserGroupPersistenceManager(postgresSqlDb);
        IAssignmentPersistenceManager assignmentPersistenceManager = new PgAssignmentPersistenceManager(postgresSqlDb, mapperFacade);

        GameboardPersistenceManager gameboardPersistenceManager = new GameboardPersistenceManager(postgresSqlDb, contentManager, mapperFacade, objectMapper, new URIManager(properties), "latest");
        gameManager = new GameManager(contentManager, gameboardPersistenceManager, mapperFacade, questionManager, "latest");
        groupManager = new GroupManager(pgUserGroupPersistenceManager, userAccountManager, gameManager, mapperFacade);
        userAssociationManager = new UserAssociationManager(pgAssociationDataManager, userAccountManager, groupManager);
        PgTransactionManager pgTransactionManager = new PgTransactionManager(postgresSqlDb);
        eventBookingManager = new EventBookingManager(bookingPersistanceManager, emailManager, userAssociationManager, properties, groupManager, userAccountManager, pgTransactionManager);
        userBadgeManager = createNiceMock(UserBadgeManager.class);
        assignmentManager = new AssignmentManager(assignmentPersistenceManager, groupManager, new EmailService(emailManager, groupManager, userAccountManager), gameManager, properties);
        schoolListReader = createNiceMock(SchoolListReader.class);

        String someSegueAnonymousUserId = "9284723987anonymous83924923";
        httpSession = createNiceMock(HttpSession.class);
        expect(httpSession.getAttribute(Constants.ANONYMOUS_USER)).andReturn(null).anyTimes();
        expect(httpSession.getId()).andReturn(someSegueAnonymousUserId).anyTimes();
        replay(httpSession);

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
    }

    protected LoginResult loginAs(final HttpSession httpSession, final String username, final String password) throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException, AuthenticationProviderMappingException, IncorrectCredentialsProvidedException, AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException, MFARequiredButNotConfiguredException {
        Capture<Cookie> capturedUserCookie = Capture.newInstance(); // new Capture<Cookie>(); seems deprecated

        HttpServletRequest userLoginRequest = createNiceMock(HttpServletRequest.class);
        expect(userLoginRequest.getSession()).andReturn(httpSession).atLeastOnce();
        replay(userLoginRequest);

        HttpServletResponse userLoginResponse = createNiceMock(HttpServletResponse.class);
        userLoginResponse.addCookie(and(capture(capturedUserCookie), isA(Cookie.class)));
        expectLastCall().atLeastOnce(); // This is how you expect void methods, apparently...
        replay(userLoginResponse);

        RegisteredUserDTO user = userAccountManager.authenticateWithCredentials(userLoginRequest, userLoginResponse, AuthenticationProvider.SEGUE.toString(), username, password, false);

        return new LoginResult(user, capturedUserCookie.getValue());
    }

    protected HttpServletRequest createRequestWithCookies(final Cookie[] cookies) {
        HttpServletRequest request = createNiceMock(HttpServletRequest.class);
        expect(request.getCookies()).andReturn(cookies).anyTimes();
        return request;
    }
}
