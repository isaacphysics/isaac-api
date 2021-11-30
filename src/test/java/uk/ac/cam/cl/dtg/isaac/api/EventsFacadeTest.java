package uk.ac.cam.cl.dtg.isaac.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.util.Modules;
import ma.glasnost.orika.MapperFacade;
import org.apache.commons.lang3.SystemUtils;
import org.junit.Before;
import org.junit.Test;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import uk.ac.cam.cl.dtg.isaac.api.managers.EventBookingManager;
import uk.ac.cam.cl.dtg.isaac.api.managers.GameManager;
import uk.ac.cam.cl.dtg.isaac.api.services.GroupChangedService;
import uk.ac.cam.cl.dtg.isaac.dao.EventBookingPersistenceManager;
import uk.ac.cam.cl.dtg.segue.api.managers.PgTransactionManager;
import uk.ac.cam.cl.dtg.segue.api.managers.QuestionManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAccountManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAssociationManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAuthenticationManager;
import uk.ac.cam.cl.dtg.segue.auth.AuthenticationProvider;
import uk.ac.cam.cl.dtg.segue.auth.IAuthenticator;
import uk.ac.cam.cl.dtg.segue.auth.ISecondFactorAuthenticator;
import uk.ac.cam.cl.dtg.segue.auth.SegueTOTPAuthenticator;
import uk.ac.cam.cl.dtg.segue.comm.EmailManager;
import uk.ac.cam.cl.dtg.segue.configuration.SegueGuiceConfigurationModule;
import uk.ac.cam.cl.dtg.segue.dao.ILogManager;
import uk.ac.cam.cl.dtg.segue.dao.associations.PgAssociationDataManager;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentMapper;
import uk.ac.cam.cl.dtg.segue.dao.content.GitContentManager;
import uk.ac.cam.cl.dtg.segue.dao.content.IContentManager;
import uk.ac.cam.cl.dtg.segue.dao.users.PgAnonymousUsers;
import uk.ac.cam.cl.dtg.segue.dao.users.PgUsers;
import uk.ac.cam.cl.dtg.segue.database.GitDb;
import uk.ac.cam.cl.dtg.segue.database.PostgresSqlDb;
import uk.ac.cam.cl.dtg.segue.search.ElasticSearchProvider;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.powermock.api.easymock.PowerMock.createMock;
import static uk.ac.cam.cl.dtg.segue.api.Constants.DEFAULT_LINUX_CONFIG_LOCATION;
import static uk.ac.cam.cl.dtg.segue.api.Constants.LOCAL_GIT_DB;

@PowerMockIgnore("javax.net.ssl.*")
public class EventsFacadeTest extends AbstractFacadeTest {

    public EventsFacade eventsFacade;

    @Before
    public void setUp() throws RuntimeException, IOException, ClassNotFoundException {
        PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:12"))
                .withDatabaseName("rutherford")
                .withEnv("POSTGRES_HOST_AUTH_METHOD", "trust")
                .withUsername("rutherford")
                .withPassword("somerandompassword")
                .withInitScript("db_scripts/postgres-rutherford-create-script.sql")
                .waitingFor(Wait.forLogMessage(".*PostgreSQL init process complete.*", 1));
                ;
        postgres.start();

        ElasticsearchContainer elasticsearch = new ElasticsearchContainer(DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch-oss:7.8.0"))
                .withEnv("cluster.name", "isaac")
                .withEnv("network.host", "0.0.0.0")
                .withEnv("node.name", "localhost")
                // .withEnv("cluster.initial_master_nodes", "localhost")
                .waitingFor(Wait.forHealthcheck());
        elasticsearch.start();

        String configLocation = SystemUtils.IS_OS_LINUX ? DEFAULT_LINUX_CONFIG_LOCATION : null;
        if (System.getProperty("config.location") != null) {
            configLocation = System.getProperty("config.location");
        }
        if (System.getenv("SEGUE_CONFIG_LOCATION") != null){
            configLocation = System.getenv("SEGUE_CONFIG_LOCATION");
        }

        PropertiesLoader mockedProperties = new PropertiesLoader(configLocation) {
            final Map<String, String> propertyOverrides = ImmutableMap.of(
                    "SEARCH_CLUSTER_NAME", "isaac"
            );
            @Override
            public String getProperty(String key) {
                return propertyOverrides.getOrDefault(key, super.getProperty(key));
            }
        };

        PostgresSqlDb postgresSqlDb = new PostgresSqlDb(
            postgres.getJdbcUrl(),
            "rutherford",
            "somerandompassword")
            ; // user/pass are irrelevant because POSTGRES_HOST_AUTH_METHOD is set to "trust"
        
        PgUsers pgUsers = new PgUsers(postgresSqlDb, null); // FIXME: This null thing.
        PgAnonymousUsers pgAnonymousUsers = new PgAnonymousUsers(postgresSqlDb);
        QuestionManager questionDb = createMock(QuestionManager.class);

        // The following may need some actual authentication providers...
        Map<AuthenticationProvider, IAuthenticator> providersToRegister = createMock(Map.class);

        MapperFacade dtoMapper = createMock(MapperFacade.class);
        EmailManager emailManager = createMock(EmailManager.class);
        ILogManager logManager = createMock(ILogManager.class);
        UserAuthenticationManager userAuthenticationManager = new UserAuthenticationManager(pgUsers, mockedProperties,
                providersToRegister, emailManager);
        ISecondFactorAuthenticator secondFactorManager = createMock(SegueTOTPAuthenticator.class);

        UserAccountManager userAccountManager =
                new UserAccountManager(pgUsers, questionDb, mockedProperties, providersToRegister, dtoMapper,
                        emailManager, pgAnonymousUsers, logManager, userAuthenticationManager, secondFactorManager);

        // FIXME: This should be passed in from the environment and point to an actual test repo.
        GitDb gitDb = new GitDb(mockedProperties.getProperty(LOCAL_GIT_DB));
        ElasticSearchProvider elasticSearchProvider =
                new ElasticSearchProvider(ElasticSearchProvider.getTransportClient(
                        "isaac",
                        "localhost",
                        elasticsearch.getMappedPort(9200))
                );
        ContentMapper contentMapper = new ContentMapper();
        IContentManager contentManager = new GitContentManager(gitDb, elasticSearchProvider, contentMapper, mockedProperties);
        ObjectMapper objectMapper = new ObjectMapper();
        EventBookingPersistenceManager bookingPersistanceManager =
                new EventBookingPersistenceManager(postgresSqlDb, userManager, contentManager, objectMapper, null,
                        "contentIndex");
        PgAssociationDataManager pgAssociationDataManager = new PgAssociationDataManager(postgresSqlDb);
        UserAssociationManager userAssociationManager = new UserAssociationManager(pgAssociationDataManager, userManager, groupManager);
        PgTransactionManager pgTransactionManager = new PgTransactionManager(postgresSqlDb);
        EventBookingManager eventBookingManager =
                new EventBookingManager(bookingPersistanceManager, emailManager, userAssociationManager,
                        mockedProperties, groupManager, userAccountManager, pgTransactionManager);

        // Create Mocked Injector
        SegueGuiceConfigurationModule.setGlobalPropertiesIfNotSet(mockedProperties);
        Module productionModule = new SegueGuiceConfigurationModule();
        Module testModule = Modules.override(productionModule).with(new AbstractModule() {
            @Override protected void configure() {
                // ... register mocks
                bind(UserAccountManager.class).toInstance(userAccountManager);
                bind(GameManager.class).toInstance(createMock(GameManager.class));
                bind(GroupChangedService.class).toInstance(createMock(GroupChangedService.class));
            }
        });
        Injector injector = Guice.createInjector(testModule);
        // Register DTOs to json mapper
//        SegueConfigurationModule segueConfigurationModule = injector.getInstance(SegueConfigurationModule.class);
        ContentMapper mapper = injector.getInstance(ContentMapper.class);
//        mapper.registerJsonTypes(segueConfigurationModule.getContentDataTransferObjectMap());
        // Get instance of class to test
        eventsFacade = injector.getInstance(EventsFacade.class);
    }

    @Test
    public void someTest() {
        Response response = eventsFacade.getEvents(this.request, "", 0, 1000, "DESC", false, false, false, false);
        int status = response.getStatus();
        assertEquals(status, Response.Status.OK.getStatusCode());
    }
}
