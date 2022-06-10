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
import org.eclipse.jgit.api.Git;
import org.junit.Before;
import org.junit.Test;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.reflections.Reflections;
import uk.ac.cam.cl.dtg.isaac.IsaacTest;
import uk.ac.cam.cl.dtg.isaac.api.managers.EventBookingManager;
import uk.ac.cam.cl.dtg.isaac.api.managers.GameManager;
import uk.ac.cam.cl.dtg.isaac.api.services.GroupChangedService;
import uk.ac.cam.cl.dtg.isaac.dao.EventBookingPersistenceManager;
import uk.ac.cam.cl.dtg.isaac.dos.AbstractUserPreferenceManager;
import uk.ac.cam.cl.dtg.isaac.dos.PgUserPreferenceManager;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacEventPageDTO;
import uk.ac.cam.cl.dtg.isaac.dto.ResultsWrapper;
import uk.ac.cam.cl.dtg.segue.api.managers.PgTransactionManager;
import uk.ac.cam.cl.dtg.segue.api.managers.QuestionManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAccountManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAssociationManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAuthenticationManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserBadgeManager;
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
import uk.ac.cam.cl.dtg.segue.dao.schools.SchoolListReader;
import uk.ac.cam.cl.dtg.segue.dao.users.PgAnonymousUsers;
import uk.ac.cam.cl.dtg.segue.dao.users.PgUsers;
import uk.ac.cam.cl.dtg.segue.database.GitDb;
import uk.ac.cam.cl.dtg.segue.database.PostgresSqlDb;
import uk.ac.cam.cl.dtg.segue.search.ElasticSearchProvider;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertNotNull;
import static uk.ac.cam.cl.dtg.segue.api.Constants.DEFAULT_LINUX_CONFIG_LOCATION;

//@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.net.ssl.*")
public class EventsFacadeTest extends IsaacTest {

    public EventsFacade eventsFacade;

    @Before
    public void setUp() throws RuntimeException, IOException, ClassNotFoundException {
        String configLocation = SystemUtils.IS_OS_LINUX ? DEFAULT_LINUX_CONFIG_LOCATION : null;
        if (System.getProperty("test.config.location") != null) {
            configLocation = System.getProperty("test.config.location");
        }
        if (System.getenv("SEGUE_TEST_CONFIG_LOCATION") != null){
            configLocation = System.getenv("SEGUE_TEST_CONFIG_LOCATION");
        }

        PropertiesLoader properties = new PropertiesLoader(configLocation) {
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

        ContentMapper contentMapper = new ContentMapper(new Reflections("uk.ac.cam.cl.dtg"));
        MapperFacade mapper = contentMapper.getAutoMapper();

        // The following may need some actual authentication providers...
        Map<AuthenticationProvider, IAuthenticator> providersToRegister = createMock(Map.class);

        EmailManager emailManager = createMock(EmailManager.class);
        ILogManager logManager = createMock(ILogManager.class);
        UserAuthenticationManager userAuthenticationManager = new UserAuthenticationManager(pgUsers, properties,
                providersToRegister, emailManager);
        ISecondFactorAuthenticator secondFactorManager = createMock(SegueTOTPAuthenticator.class);
        AbstractUserPreferenceManager userPreferenceManager = new PgUserPreferenceManager(postgresSqlDb);

        UserAccountManager userAccountManager =
                new UserAccountManager(pgUsers, questionDb, properties, providersToRegister, mapper,
                        emailManager, pgAnonymousUsers, logManager, userAuthenticationManager, secondFactorManager,
                        userPreferenceManager);

        Git git = createMock(Git.class);
        GitDb gitDb = new GitDb(git);
        ElasticSearchProvider elasticSearchProvider =
                new ElasticSearchProvider(ElasticSearchProvider.getTransportClient(
                        "isaac",
                        "localhost",
                        elasticsearch.getMappedPort(9300))
                );
        IContentManager contentManager = new GitContentManager(gitDb, elasticSearchProvider, contentMapper, properties);
        ObjectMapper objectMapper = new ObjectMapper();
        EventBookingPersistenceManager bookingPersistanceManager =
                new EventBookingPersistenceManager(postgresSqlDb, userAccountManager, contentManager, objectMapper);
        PgAssociationDataManager pgAssociationDataManager = new PgAssociationDataManager(postgresSqlDb);
        UserAssociationManager userAssociationManager = new UserAssociationManager(pgAssociationDataManager, userAccountManager, groupManager);
        PgTransactionManager pgTransactionManager = new PgTransactionManager(postgresSqlDb);
        EventBookingManager eventBookingManager =
                new EventBookingManager(bookingPersistanceManager, emailManager, userAssociationManager,
                        properties, groupManager, userAccountManager, pgTransactionManager);
        UserBadgeManager userBadgeManager = createMock(UserBadgeManager.class);
        SchoolListReader schoolListReader = createMock(SchoolListReader.class);

        // Create Mocked Injector
        SegueGuiceConfigurationModule.setGlobalPropertiesIfNotSet(properties);
        Module productionModule = new SegueGuiceConfigurationModule();
        Module testModule = Modules.override(productionModule).with(new AbstractModule() {
            @Override protected void configure() {
                // ... register mocks
                bind(UserAccountManager.class).toInstance(userAccountManager);
                bind(GameManager.class).toInstance(createMock(GameManager.class));
                bind(GroupChangedService.class).toInstance(createMock(GroupChangedService.class));
                bind(EventBookingManager.class).toInstance(eventBookingManager);
            }
        });
        Injector injector = Guice.createInjector(testModule);
        // Register DTOs to json mapper
        MapperFacade mapperFacade = contentMapper.getAutoMapper();
        // Get instance of class to test
        eventsFacade = new EventsFacade(properties, logManager, eventBookingManager, userAccountManager, contentManager, "latest", userBadgeManager, userAssociationManager, groupManager, userAccountManager, schoolListReader, mapperFacade);
    }

    @Test
    public void getEventsTest() throws InterruptedException {
        HttpServletRequest request = createMock(HttpServletRequest.class);
        Cookie[] emptyCookies = {};
        expect(request.getCookies()).andReturn(emptyCookies).anyTimes();
        replay(request);

        Response response = eventsFacade.getEvents(request, null, 0, 10, null, null, null, null, null, null);
        int status = response.getStatus();
        assertEquals(status, Response.Status.OK.getStatusCode());
        ResultsWrapper<IsaacEventPageDTO> entity = (ResultsWrapper<IsaacEventPageDTO>) response.getEntity();
        assertNotNull(entity);
        List<IsaacEventPageDTO> results = entity.getResults();
        assertNotNull(entity);
        assertEquals(results.size(), 5);
    }
}
