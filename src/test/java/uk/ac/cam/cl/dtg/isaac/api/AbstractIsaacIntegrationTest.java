package uk.ac.cam.cl.dtg.isaac.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.google.api.client.util.Maps;
import org.apache.commons.lang3.SystemUtils;
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
import uk.ac.cam.cl.dtg.isaac.api.managers.EventBookingManager;
import uk.ac.cam.cl.dtg.isaac.api.managers.EventsManager;
import uk.ac.cam.cl.dtg.isaac.api.managers.FastTrackManger;
import uk.ac.cam.cl.dtg.isaac.api.managers.GameManager;
import uk.ac.cam.cl.dtg.isaac.api.managers.QuizAssignmentManager;
import uk.ac.cam.cl.dtg.isaac.api.managers.QuizAttemptManager;
import uk.ac.cam.cl.dtg.isaac.api.managers.QuizManager;
import uk.ac.cam.cl.dtg.isaac.api.managers.QuizQuestionManager;
import uk.ac.cam.cl.dtg.isaac.api.managers.URIManager;
import uk.ac.cam.cl.dtg.isaac.api.managers.UserAttemptManager;
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
import uk.ac.cam.cl.dtg.isaac.quiz.PgQuestionAttempts;
import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.api.managers.GroupManager;
import uk.ac.cam.cl.dtg.segue.api.managers.PgTransactionManager;
import uk.ac.cam.cl.dtg.segue.api.managers.QuestionManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAccountManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAssociationManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAuthenticationManager;
import uk.ac.cam.cl.dtg.segue.api.monitors.AnonQuestionAttemptMisuseHandler;
import uk.ac.cam.cl.dtg.segue.api.monitors.EmailVerificationMisuseHandler;
import uk.ac.cam.cl.dtg.segue.api.monitors.GroupManagerLookupMisuseHandler;
import uk.ac.cam.cl.dtg.segue.api.monitors.IMisuseMonitor;
import uk.ac.cam.cl.dtg.segue.api.monitors.IPQuestionAttemptMisuseHandler;
import uk.ac.cam.cl.dtg.segue.api.monitors.InMemoryMisuseMonitor;
import uk.ac.cam.cl.dtg.segue.api.monitors.RegistrationMisuseHandler;
import uk.ac.cam.cl.dtg.segue.api.monitors.TeacherPasswordResetMisuseHandler;
import uk.ac.cam.cl.dtg.segue.api.monitors.TokenOwnerLookupMisuseHandler;
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
import uk.ac.cam.cl.dtg.segue.comm.EmailCommunicator;
import uk.ac.cam.cl.dtg.segue.comm.EmailManager;
import uk.ac.cam.cl.dtg.segue.comm.MailGunEmailManager;
import uk.ac.cam.cl.dtg.segue.dao.ILogManager;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.associations.PgAssociationDataManager;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentSubclassMapper;
import uk.ac.cam.cl.dtg.segue.dao.content.GitContentManager;
import uk.ac.cam.cl.dtg.segue.dao.schools.SchoolListReader;
import uk.ac.cam.cl.dtg.segue.dao.users.IDeletionTokenPersistenceManager;
import uk.ac.cam.cl.dtg.segue.dao.users.PgAnonymousUsers;
import uk.ac.cam.cl.dtg.segue.dao.users.PgDeletionTokenPersistenceManager;
import uk.ac.cam.cl.dtg.segue.dao.users.PgPasswordDataManager;
import uk.ac.cam.cl.dtg.segue.dao.users.PgUserGroupPersistenceManager;
import uk.ac.cam.cl.dtg.segue.dao.users.PgUsers;
import uk.ac.cam.cl.dtg.segue.database.GitDb;
import uk.ac.cam.cl.dtg.segue.database.PostgresSqlDb;
import uk.ac.cam.cl.dtg.segue.etl.ElasticSearchIndexer;
import uk.ac.cam.cl.dtg.segue.search.ElasticSearchProvider;
import uk.ac.cam.cl.dtg.util.AbstractConfigLoader;
import uk.ac.cam.cl.dtg.util.YamlLoader;
import uk.ac.cam.cl.dtg.util.mappers.MainMapper;

import jakarta.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static uk.ac.cam.cl.dtg.segue.api.Constants.*;
import static uk.ac.cam.cl.dtg.segue.api.Constants.EMAIL_SIGNATURE;
import static uk.ac.cam.cl.dtg.segue.api.Constants.HOST_NAME;

/**
 * IMPORTANT: Rather than directly subclass this, use either IsaacIntegrationTestWithREST or IsaacIntegrationTest
 * Abstract superclass for integration tests, providing them with dependencies including Elasticsearch and PostgreSQL
 * (as docker containers) and other managers (some of which are mocked). Except for the Elasticsearch container, these
 * dependencies are created before and destroyed after every test class.
 * Subclasses should be named "*IT.java" so Maven Failsafe detects them. Use the "verify" Maven target to run them.
 */
public class AbstractIsaacIntegrationTest {

    protected static final Logger log = LoggerFactory.getLogger(IsaacIntegrationTest.class);

    protected static HttpSession httpSession;
    protected static PostgreSQLContainer postgres;
    protected static ElasticsearchContainer elasticsearch;
    protected static AbstractConfigLoader properties;
    protected static Map<String, String> globalTokens;
    protected static PostgresSqlDb postgresSqlDb;
    protected static ElasticSearchIndexer elasticSearchProvider;
    protected static SchoolListReader schoolListReader;
    protected static MainMapper mainMapper;
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
    protected static EventsManager eventsManager;
    protected static ILogManager logManager;
    protected static GitContentManager contentManager;
    protected static UserAssociationManager userAssociationManager;
    protected static AssignmentManager assignmentManager;
    protected static QuestionManager questionManager;
    protected static QuizManager quizManager;
    protected static PgPasswordDataManager passwordDataManager;
    protected static UserAttemptManager userAttemptManager;
    protected static FastTrackManger fastTrackManger;

    // Manager dependencies
    protected static IQuizAssignmentPersistenceManager quizAssignmentPersistenceManager;
    protected static QuizAssignmentManager quizAssignmentManager;
    protected static QuizAttemptManager quizAttemptManager;
    protected static IQuizAttemptPersistenceManager quizAttemptPersistenceManager;
    protected static IQuizQuestionAttemptPersistenceManager quizQuestionAttemptPersistenceManager;
    protected static QuizQuestionManager quizQuestionManager;
    protected static PgUsers pgUsers;
    protected static ContentSubclassMapper contentMapper;

    // Services
    protected static AssignmentService assignmentService;

    protected static AbstractUserPreferenceManager userPreferenceManager;

    protected static ITUsers integrationTestUsers;

    protected static Map<AuthenticationProvider, IAuthenticator> providersToRegister;

    protected static PgAnonymousUsers pgAnonymousUsers;

    protected static ISecondFactorAuthenticator secondFactorManager;

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

        elasticsearch.start();

        try {
            elasticSearchProvider = new ElasticSearchIndexer(ElasticSearchProvider.getClient(
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
        Git.init().setDirectory(new File("tmp/dummy_repo")).call();

        // Initialise Postgres - we will create a new, clean instance for each test class.
        postgres = new PostgreSQLContainer<>("postgres:16")
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

        contentMapper = new ContentSubclassMapper(new Reflections("uk.ac.cam.cl.dtg"));
        PgQuestionAttempts pgQuestionAttempts = new PgQuestionAttempts(postgresSqlDb, contentMapper);
        mainMapper = MainMapper.INSTANCE;
        questionManager = new QuestionManager(contentMapper, mainMapper, pgQuestionAttempts);


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
        contentManager = new GitContentManager(gitDb, elasticSearchProvider, mainMapper, contentMapper, properties);
        logManager = createNiceMock(ILogManager.class);
        IDeletionTokenPersistenceManager deletionTokenPersistenceManager = new PgDeletionTokenPersistenceManager(postgresSqlDb);

        emailManager = new EmailManager(communicator, userPreferenceManager, properties, contentManager, logManager, globalTokens);

        userAuthenticationManager = new UserAuthenticationManager(pgUsers, deletionTokenPersistenceManager, properties, providersToRegister, emailManager);
        secondFactorManager = createMock(SegueTOTPAuthenticator.class);
        // We don't care for MFA here so we can safely disable it
        try {
            expect(secondFactorManager.has2FAConfigured(anyObject())).andReturn(false).atLeastOnce();
        } catch (SegueDatabaseException e) {
            throw new RuntimeException(e);
        }
        replay(secondFactorManager);

        userAccountManager = new UserAccountManager(pgUsers, questionManager, properties, providersToRegister, mainMapper, emailManager, pgAnonymousUsers, logManager, userAuthenticationManager, secondFactorManager, userPreferenceManager);

        ObjectMapper objectMapper = new ObjectMapper();
        mailGunEmailManager = new MailGunEmailManager(globalTokens, properties, userPreferenceManager);
        EventBookingPersistenceManager bookingPersistanceManager = new EventBookingPersistenceManager(postgresSqlDb, userAccountManager, contentManager, objectMapper);
        PgAssociationDataManager pgAssociationDataManager = new PgAssociationDataManager(postgresSqlDb);
        PgUserGroupPersistenceManager pgUserGroupPersistenceManager = new PgUserGroupPersistenceManager(postgresSqlDb);
        IAssignmentPersistenceManager assignmentPersistenceManager = new PgAssignmentPersistenceManager(postgresSqlDb, mainMapper);

        GameboardPersistenceManager gameboardPersistenceManager = new GameboardPersistenceManager(postgresSqlDb, contentManager, mainMapper, contentMapper);
        gameManager = new GameManager(contentManager, gameboardPersistenceManager, mainMapper, questionManager, properties);
        groupManager = new GroupManager(pgUserGroupPersistenceManager, userAccountManager, gameManager, mainMapper);
        userAssociationManager = new UserAssociationManager(pgAssociationDataManager, userAccountManager, groupManager);
        PgTransactionManager pgTransactionManager = new PgTransactionManager(postgresSqlDb);
        eventBookingManager = new EventBookingManager(bookingPersistanceManager, emailManager, userAssociationManager, properties, groupManager, userAccountManager, pgTransactionManager);
        eventsManager = new EventsManager(properties, elasticSearchProvider,  userAccountManager, eventBookingManager, contentManager, mainMapper, contentMapper);
        assignmentManager = new AssignmentManager(assignmentPersistenceManager, groupManager, new EmailService(properties, emailManager, groupManager, userAccountManager, mailGunEmailManager), gameManager, properties);
        schoolListReader = createNiceMock(SchoolListReader.class);

        quizManager = new QuizManager(properties, new ContentService(contentManager), contentManager, new ContentSummarizerService(mainMapper, new URIManager(properties)));
        quizAssignmentPersistenceManager =  new PgQuizAssignmentPersistenceManager(postgresSqlDb, mainMapper);
        quizAssignmentManager = new QuizAssignmentManager(quizAssignmentPersistenceManager, new EmailService(properties, emailManager, groupManager, userAccountManager, mailGunEmailManager), quizManager, groupManager, properties);
        assignmentService = new AssignmentService(userAccountManager);
        quizAttemptPersistenceManager = new PgQuizAttemptPersistenceManager(postgresSqlDb, mainMapper);
        quizAttemptManager = new QuizAttemptManager(quizAttemptPersistenceManager);
        quizQuestionAttemptPersistenceManager = new PgQuizQuestionAttemptPersistenceManager(postgresSqlDb, contentMapper);
        quizQuestionManager = new QuizQuestionManager(questionManager, mainMapper, quizQuestionAttemptPersistenceManager, quizManager, quizAttemptManager);
        userAttemptManager = new UserAttemptManager(questionManager);
        fastTrackManger = new FastTrackManger(properties, contentManager, gameManager);

        misuseMonitor = new InMemoryMisuseMonitor();
        misuseMonitor.registerHandler(GroupManagerLookupMisuseHandler.class.getSimpleName(), new GroupManagerLookupMisuseHandler(emailManager, properties));
        misuseMonitor.registerHandler(RegistrationMisuseHandler.class.getSimpleName(), new RegistrationMisuseHandler(emailManager, properties));
        misuseMonitor.registerHandler(EmailVerificationMisuseHandler.class.getSimpleName(), new EmailVerificationMisuseHandler());
        misuseMonitor.registerHandler(TeacherPasswordResetMisuseHandler.class.getSimpleName(), new TeacherPasswordResetMisuseHandler());
        misuseMonitor.registerHandler(TokenOwnerLookupMisuseHandler.class.getSimpleName(), new TokenOwnerLookupMisuseHandler(emailManager, properties));
        misuseMonitor.registerHandler(AnonQuestionAttemptMisuseHandler.class.getSimpleName(), new AnonQuestionAttemptMisuseHandler());
        misuseMonitor.registerHandler(IPQuestionAttemptMisuseHandler.class.getSimpleName(), new IPQuestionAttemptMisuseHandler(emailManager, properties));
        // todo: more handlers as required by different endpoints

        String someSegueAnonymousUserId = "9284723987anonymous83924923";
        httpSession = createNiceMock(HttpSession.class);
        expect(httpSession.getAttribute(Constants.ANONYMOUS_USER)).andReturn(null).anyTimes();
        expect(httpSession.getId()).andReturn(someSegueAnonymousUserId).anyTimes();
        replay(httpSession);

        contentSummarizerService = new ContentSummarizerService(mainMapper, new URIManager(properties));

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


    private static String getClassLoaderResourcePath(final String resource) {
        return AbstractIsaacIntegrationTest.class.getClassLoader().getResource(resource)
                .getPath()
                // ":" is removed from the resource path for Windows compatibility
                .replace(":", "");
    }
}
