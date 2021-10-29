/*
 * Copyright 2014 Stephen Cummins
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * <p>
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.cam.cl.dtg.segue.configuration;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.util.Lists;
import com.google.api.client.util.Maps;
import com.google.common.collect.ImmutableMap;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import ma.glasnost.orika.MapperFacade;
import org.apache.commons.lang3.SystemUtils;
import org.elasticsearch.client.Client;
import org.quartz.SchedulerException;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.api.managers.AssignmentManager;
import uk.ac.cam.cl.dtg.isaac.api.managers.GameManager;
import uk.ac.cam.cl.dtg.isaac.api.managers.QuizAssignmentManager;
import uk.ac.cam.cl.dtg.isaac.api.managers.URIManager;
import uk.ac.cam.cl.dtg.isaac.api.services.ContentSummarizerService;
import uk.ac.cam.cl.dtg.isaac.api.services.EmailService;
import uk.ac.cam.cl.dtg.isaac.api.services.GroupChangedService;
import uk.ac.cam.cl.dtg.isaac.dao.GameboardPersistenceManager;
import uk.ac.cam.cl.dtg.isaac.dao.IAssignmentPersistenceManager;
import uk.ac.cam.cl.dtg.isaac.dao.IQuizAssignmentPersistenceManager;
import uk.ac.cam.cl.dtg.isaac.dao.IQuizAttemptPersistenceManager;
import uk.ac.cam.cl.dtg.isaac.dao.IQuizQuestionAttemptPersistenceManager;
import uk.ac.cam.cl.dtg.isaac.dao.PgAssignmentPersistenceManager;
import uk.ac.cam.cl.dtg.isaac.dao.PgQuizAssignmentPersistenceManager;
import uk.ac.cam.cl.dtg.isaac.dao.PgQuizAttemptPersistenceManager;
import uk.ac.cam.cl.dtg.isaac.dao.PgQuizQuestionAttemptPersistenceManager;
import uk.ac.cam.cl.dtg.isaac.quiz.IsaacSymbolicChemistryValidator;
import uk.ac.cam.cl.dtg.isaac.quiz.IsaacSymbolicLogicValidator;
import uk.ac.cam.cl.dtg.isaac.quiz.IsaacSymbolicValidator;
import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.api.managers.ExternalAccountManager;
import uk.ac.cam.cl.dtg.segue.api.managers.GroupManager;
import uk.ac.cam.cl.dtg.segue.api.managers.IExternalAccountManager;
import uk.ac.cam.cl.dtg.segue.api.managers.IGroupObserver;
import uk.ac.cam.cl.dtg.segue.api.managers.IStatisticsManager;
import uk.ac.cam.cl.dtg.segue.api.managers.ITransactionManager;
import uk.ac.cam.cl.dtg.segue.api.managers.IUserAccountManager;
import uk.ac.cam.cl.dtg.segue.api.managers.PgTransactionManager;
import uk.ac.cam.cl.dtg.segue.api.managers.QuestionManager;
import uk.ac.cam.cl.dtg.segue.api.managers.StatisticsManager;
import uk.ac.cam.cl.dtg.segue.api.managers.StubExternalAccountManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAccountManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAuthenticationManager;
import uk.ac.cam.cl.dtg.segue.api.monitors.*;
import uk.ac.cam.cl.dtg.segue.auth.AuthenticationProvider;
import uk.ac.cam.cl.dtg.segue.auth.FacebookAuthenticator;
import uk.ac.cam.cl.dtg.segue.auth.GoogleAuthenticator;
import uk.ac.cam.cl.dtg.segue.auth.IAuthenticator;
import uk.ac.cam.cl.dtg.segue.auth.ISecondFactorAuthenticator;
import uk.ac.cam.cl.dtg.segue.auth.ISegueHashingAlgorithm;
import uk.ac.cam.cl.dtg.segue.auth.SegueLocalAuthenticator;
import uk.ac.cam.cl.dtg.segue.auth.SeguePBKDF2v1;
import uk.ac.cam.cl.dtg.segue.auth.SeguePBKDF2v2;
import uk.ac.cam.cl.dtg.segue.auth.SeguePBKDF2v3;
import uk.ac.cam.cl.dtg.segue.auth.SegueTOTPAuthenticator;
import uk.ac.cam.cl.dtg.segue.auth.TwitterAuthenticator;
import uk.ac.cam.cl.dtg.segue.comm.EmailCommunicator;
import uk.ac.cam.cl.dtg.segue.comm.EmailManager;
import uk.ac.cam.cl.dtg.segue.comm.ICommunicator;
import uk.ac.cam.cl.dtg.segue.dao.ILogManager;
import uk.ac.cam.cl.dtg.segue.dao.LocationManager;
import uk.ac.cam.cl.dtg.segue.dao.LogManagerEventPublisher;
import uk.ac.cam.cl.dtg.segue.dao.PgLogManager;
import uk.ac.cam.cl.dtg.segue.dao.PgLogManagerEventListener;
import uk.ac.cam.cl.dtg.segue.dao.associations.IAssociationDataManager;
import uk.ac.cam.cl.dtg.segue.dao.associations.PgAssociationDataManager;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentMapper;
import uk.ac.cam.cl.dtg.segue.dao.content.GitContentManager;
import uk.ac.cam.cl.dtg.segue.dao.content.IContentManager;
import uk.ac.cam.cl.dtg.segue.dao.schools.SchoolListReader;
import uk.ac.cam.cl.dtg.segue.dao.userBadges.IUserBadgePersistenceManager;
import uk.ac.cam.cl.dtg.segue.dao.userBadges.PgUserBadgePersistenceManager;
import uk.ac.cam.cl.dtg.segue.dao.users.IAnonymousUserDataManager;
import uk.ac.cam.cl.dtg.segue.dao.users.IExternalAccountDataManager;
import uk.ac.cam.cl.dtg.segue.dao.users.IPasswordDataManager;
import uk.ac.cam.cl.dtg.segue.dao.users.ITOTPDataManager;
import uk.ac.cam.cl.dtg.segue.dao.users.IUserDataManager;
import uk.ac.cam.cl.dtg.segue.dao.users.IUserGroupPersistenceManager;
import uk.ac.cam.cl.dtg.segue.dao.users.PgAnonymousUsers;
import uk.ac.cam.cl.dtg.segue.dao.users.PgExternalAccountPersistenceManager;
import uk.ac.cam.cl.dtg.segue.dao.users.PgPasswordDataManager;
import uk.ac.cam.cl.dtg.segue.dao.users.PgTOTPDataManager;
import uk.ac.cam.cl.dtg.segue.dao.users.PgUserGroupPersistenceManager;
import uk.ac.cam.cl.dtg.segue.dao.users.PgUsers;
import uk.ac.cam.cl.dtg.segue.database.GitDb;
import uk.ac.cam.cl.dtg.segue.database.PostgresSqlDb;
import uk.ac.cam.cl.dtg.segue.dos.AbstractUserPreferenceManager;
import uk.ac.cam.cl.dtg.segue.dos.IUserAlerts;
import uk.ac.cam.cl.dtg.segue.dos.IUserStreaksManager;
import uk.ac.cam.cl.dtg.segue.dos.LocationHistory;
import uk.ac.cam.cl.dtg.segue.dos.PgLocationHistory;
import uk.ac.cam.cl.dtg.segue.dos.PgUserAlerts;
import uk.ac.cam.cl.dtg.segue.dos.PgUserPreferenceManager;
import uk.ac.cam.cl.dtg.segue.dos.PgUserStreakManager;
import uk.ac.cam.cl.dtg.segue.quiz.IQuestionAttemptManager;
import uk.ac.cam.cl.dtg.segue.quiz.PgQuestionAttempts;
import uk.ac.cam.cl.dtg.segue.scheduler.SegueJobService;
import uk.ac.cam.cl.dtg.segue.scheduler.SegueScheduledDatabaseScriptJob;
import uk.ac.cam.cl.dtg.segue.scheduler.SegueScheduledJob;
import uk.ac.cam.cl.dtg.segue.scheduler.jobs.SegueScheduledSyncMailjetUsersJob;
import uk.ac.cam.cl.dtg.segue.search.ElasticSearchProvider;
import uk.ac.cam.cl.dtg.segue.search.ISearchProvider;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;
import uk.ac.cam.cl.dtg.util.email.MailJetApiClientWrapper;
import uk.ac.cam.cl.dtg.util.locations.IPInfoDBLocationResolver;
import uk.ac.cam.cl.dtg.util.locations.IPLocationResolver;
import uk.ac.cam.cl.dtg.util.locations.PostCodeIOLocationResolver;
import uk.ac.cam.cl.dtg.util.locations.PostCodeLocationResolver;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import static uk.ac.cam.cl.dtg.segue.api.Constants.*;
import static uk.ac.cam.cl.dtg.segue.api.Constants.EnvironmentType.*;

/**
 * This class is responsible for injecting configuration values for persistence related classes.
 *
 */
public class SegueGuiceConfigurationModule extends AbstractModule implements ServletContextListener {
    private static final Logger log = LoggerFactory.getLogger(SegueGuiceConfigurationModule.class);

    private static Injector injector = null;

    private static PropertiesLoader globalProperties = null;

    // Singletons - we only ever want there to be one instance of each of these.
    private static PostgresSqlDb postgresDB;
    private static ContentMapper mapper = null;
    private static GitContentManager contentManager = null;
    private static Client elasticSearchClient = null;
    private static UserAccountManager userManager = null;
    private static UserAuthenticationManager userAuthenticationManager = null;
    private static IQuestionAttemptManager questionPersistenceManager = null;
    private static SegueJobService segueJobService = null;

    private static LogManagerEventPublisher logManager;
    private static EmailManager emailCommunicationQueue = null;
    private static IMisuseMonitor misuseMonitor = null;
    private static IMetricsExporter metricsExporter = null;
    private static StatisticsManager statsManager = null;
    private static GroupManager groupManager = null;
    private static IExternalAccountManager externalAccountManager = null;
    private static GameboardPersistenceManager gameboardPersistenceManager = null;
    private static SchoolListReader schoolListReader = null;
    private static AssignmentManager assignmentManager = null;
    private static IGroupObserver groupObserver = null;

    private static Collection<Class<? extends ServletContextListener>> contextListeners;
    private static final Map<String, Reflections> reflections = com.google.common.collect.Maps.newHashMap();

    /**
     * Create a SegueGuiceConfigurationModule.
     */
    public SegueGuiceConfigurationModule() {
        if (globalProperties == null) {
            // check the following places to determine where config file location may be.
            // 1) system env variable, 2) java param (system property), 3) use a default from the constant file.
            String configLocation = SystemUtils.IS_OS_LINUX ? DEFAULT_LINUX_CONFIG_LOCATION : null;
            if (System.getProperty("config.location") != null) {
                configLocation = System.getProperty("config.location");
            }
            if (System.getenv("SEGUE_CONFIG_LOCATION") != null){
                configLocation = System.getenv("SEGUE_CONFIG_LOCATION");
            }

            try {
                if (null == configLocation) {
                    throw new FileNotFoundException("Segue configuration location not specified, please provide it as either a java system property (config.location) or environment variable SEGUE_CONFIG_LOCATION");
                }

                globalProperties = new PropertiesLoader(configLocation);

                log.info(String.format("Segue using configuration file: %s", configLocation));

            } catch (IOException e) {
                log.error("Error loading properties file.", e);
            }
        }
    }

    @Override
    protected void configure() {
        try {
            this.configureProperties();
            this.configureDataPersistence();
            this.configureSegueSearch();
            this.configureAuthenticationProviders();
            this.configureApplicationManagers();

        } catch (IOException e) {
            e.printStackTrace();
            log.error("IOException during setup process.");
        }
    }

    /**
     * Extract properties and bind them to constants.
     */
    private void configureProperties() {
        // Properties loader
        bind(PropertiesLoader.class).toInstance(globalProperties);

        this.bindConstantToProperty(Constants.SEARCH_CLUSTER_NAME, globalProperties);
        this.bindConstantToProperty(Constants.SEARCH_CLUSTER_ADDRESS, globalProperties);
        this.bindConstantToProperty(Constants.SEARCH_CLUSTER_PORT, globalProperties);

        this.bindConstantToProperty(Constants.HOST_NAME, globalProperties);
        this.bindConstantToProperty(Constants.MAILER_SMTP_SERVER, globalProperties);
        this.bindConstantToProperty(Constants.MAIL_FROM_ADDRESS, globalProperties);
        this.bindConstantToProperty(Constants.MAIL_NAME, globalProperties);
        this.bindConstantToProperty(Constants.SERVER_ADMIN_ADDRESS, globalProperties);

        this.bindConstantToProperty(Constants.LOGGING_ENABLED, globalProperties);

        // IP address geocoding
        this.bindConstantToProperty(Constants.IP_INFO_DB_API_KEY, globalProperties);

        this.bindConstantToProperty(Constants.SCHOOL_CSV_LIST_PATH, globalProperties);

        this.bindConstantToProperty(CONTENT_INDEX, globalProperties);

        this.bindConstantToProperty(Constants.API_METRICS_EXPORT_PORT, globalProperties);

        this.bind(String.class).toProvider(() -> {
            // Any binding to String without a matching @Named annotation will always get the empty string
            // which seems incredibly likely to cause errors and rarely to be intended behaviour,
            // so throw an error early in DEV and log an error in PROD.
            try {
                throw new IllegalArgumentException("Binding a String without a matching @Named annotation");
            } catch (IllegalArgumentException e) {
                if (globalProperties.getProperty(SEGUE_APP_ENVIRONMENT).equals(DEV.name())) {
                    throw e;
                }
                log.error("Binding a String without a matching @Named annotation", e);
            }
            return "";
        });
    }

    /**
     * Configure all things persistency.
     *
     * @throws IOException
     *             - when we cannot load the database.
     */
    private void configureDataPersistence() throws IOException {
        this.bindConstantToProperty(Constants.SEGUE_DB_NAME, globalProperties);

        // postgres
        this.bindConstantToProperty(Constants.POSTGRES_DB_URL, globalProperties);
        this.bindConstantToProperty(Constants.POSTGRES_DB_USER, globalProperties);
        this.bindConstantToProperty(Constants.POSTGRES_DB_PASSWORD, globalProperties);

        // GitDb
        bind(GitDb.class).toInstance(
                new GitDb(globalProperties.getProperty(Constants.LOCAL_GIT_DB), globalProperties
                        .getProperty(Constants.REMOTE_GIT_SSH_URL), globalProperties
                        .getProperty(Constants.REMOTE_GIT_SSH_KEY_PATH)));

        bind(IUserGroupPersistenceManager.class).to(PgUserGroupPersistenceManager.class);
        bind(IAssociationDataManager.class).to(PgAssociationDataManager.class);
        bind(IAssignmentPersistenceManager.class).to(PgAssignmentPersistenceManager.class);
        bind(IQuizAssignmentPersistenceManager.class).to(PgQuizAssignmentPersistenceManager.class);
        bind(IQuizAttemptPersistenceManager.class).to(PgQuizAttemptPersistenceManager.class);
        bind(IQuizQuestionAttemptPersistenceManager.class).to(PgQuizQuestionAttemptPersistenceManager.class);
        bind(IUserBadgePersistenceManager.class).to(PgUserBadgePersistenceManager.class);
    }

    /**
     * Configure segue search classes.
     */
    private void configureSegueSearch() {
        bind(ISearchProvider.class).to(ElasticSearchProvider.class);
    }

    /**
     * Configure user security related classes.
     */
    private void configureAuthenticationProviders() {
        this.bindConstantToProperty(Constants.HMAC_SALT, globalProperties);

        // Configure security providers
        // Google
        this.bindConstantToProperty(Constants.GOOGLE_CLIENT_SECRET_LOCATION, globalProperties);
        this.bindConstantToProperty(Constants.GOOGLE_CALLBACK_URI, globalProperties);
        this.bindConstantToProperty(Constants.GOOGLE_OAUTH_SCOPES, globalProperties);

        // Facebook
        this.bindConstantToProperty(Constants.FACEBOOK_SECRET, globalProperties);
        this.bindConstantToProperty(Constants.FACEBOOK_CLIENT_ID, globalProperties);
        this.bindConstantToProperty(Constants.FACEBOOK_CALLBACK_URI, globalProperties);
        this.bindConstantToProperty(Constants.FACEBOOK_OAUTH_SCOPES, globalProperties);
        this.bindConstantToProperty(Constants.FACEBOOK_USER_FIELDS, globalProperties);

        // Twitter
        this.bindConstantToProperty(Constants.TWITTER_SECRET, globalProperties);
        this.bindConstantToProperty(Constants.TWITTER_CLIENT_ID, globalProperties);
        this.bindConstantToProperty(Constants.TWITTER_CALLBACK_URI, globalProperties);

        // Register a map of security providers
        MapBinder<AuthenticationProvider, IAuthenticator> mapBinder = MapBinder.newMapBinder(binder(),
                AuthenticationProvider.class, IAuthenticator.class);
        mapBinder.addBinding(AuthenticationProvider.GOOGLE).to(GoogleAuthenticator.class);
        mapBinder.addBinding(AuthenticationProvider.FACEBOOK).to(FacebookAuthenticator.class);
        mapBinder.addBinding(AuthenticationProvider.TWITTER).to(TwitterAuthenticator.class);
        mapBinder.addBinding(AuthenticationProvider.SEGUE).to(SegueLocalAuthenticator.class);
    }

    /**
     * Deals with application data managers.
     */
    private void configureApplicationManagers() {
        // Allows GitDb to take over content Management
        bind(IContentManager.class).to(GitContentManager.class);

        bind(LocationHistory.class).to(PgLocationHistory.class);

        bind(PostCodeLocationResolver.class).to(PostCodeIOLocationResolver.class);

        bind(IUserDataManager.class).to(PgUsers.class);

        bind(IAnonymousUserDataManager.class).to(PgAnonymousUsers.class);

        bind(IPasswordDataManager.class).to(PgPasswordDataManager.class);

        bind(ICommunicator.class).to(EmailCommunicator.class);

        bind(AbstractUserPreferenceManager.class).to(PgUserPreferenceManager.class);

        bind(IUserAlerts.class).to(PgUserAlerts.class);

        bind(IUserStreaksManager.class).to(PgUserStreakManager.class);

        bind(IStatisticsManager.class).to(StatisticsManager.class);

        bind(ITransactionManager.class).to(PgTransactionManager.class);

        bind(ITOTPDataManager.class).to(PgTOTPDataManager.class);

        bind(ISecondFactorAuthenticator.class).to(SegueTOTPAuthenticator.class);
    }


    @Inject
    @Provides
    @Singleton
    private static IMetricsExporter getMetricsExporter(
            @Named(Constants.API_METRICS_EXPORT_PORT) final int port) {
        if (null == metricsExporter) {
            try {
                log.info("Creating MetricsExporter on port (" + port + ")");
                metricsExporter = new PrometheusMetricsExporter(port);
                log.info("Exporting default JVM metrics.");
                metricsExporter.exposeJvmMetrics();
            } catch (IOException e) {
                log.error("Could not create MetricsExporter on port (" + port + ")");
                return null;
            }
        }
        return metricsExporter;
    }

    /**
     * This provides a singleton of the elasticSearch client that can be used by Guice.
     *
     * The client is threadsafe, so we don't need to keep creating new ones.
     *
     * @param clusterName
     *            - The name of the cluster to create.
     * @param address
     *            - address of the cluster to create.
     * @param port
     *            - port of the cluster to create.
     * @return Client to be injected into ElasticSearch Provider.
     */
    @Inject
    @Provides
    @Singleton
    private static Client getSearchConnectionInformation(
            @Named(Constants.SEARCH_CLUSTER_NAME) final String clusterName,
            @Named(Constants.SEARCH_CLUSTER_ADDRESS) final String address,
            @Named(Constants.SEARCH_CLUSTER_PORT) final int port) {
        if (null == elasticSearchClient) {
            try {
                elasticSearchClient = ElasticSearchProvider.getTransportClient(clusterName, address, port);
                log.info("Creating singleton of ElasticSearchProvider");
            } catch (UnknownHostException e) {
                log.error("Could not create ElasticSearchProvider");
                return null;
            }
        }
        // eventually we want to do something like the below to make sure we get updated clients        
//        if (elasticSearchClient instanceof TransportClient) {
//            TransportClient tc = (TransportClient) elasticSearchClient;
//            if (tc.connectedNodes().isEmpty()) {
//                tc.close();        
//                log.error("The elasticsearch client is not connected to any nodes. Trying to reconnect...");
//                elasticSearchClient = null;
//                return getSearchConnectionInformation(clusterName, address, port);
//            }
//        }

        return elasticSearchClient;
    }

    /**
     * This provides a singleton of the git content manager for the segue facade.
     *
     * TODO: This is a singleton as the units and tags are stored in memory. If we move these out it can be an instance.
     * This would be better as then we can give it a new search provider if the client has closed.
     *
     * @param database
     *            - database reference
     * @param searchProvider
     *            - search provider to use
     * @param contentMapper
     *            - content mapper to use.
     * @return a fully configured content Manager.
     */
    @Inject
    @Provides
    @Singleton
    private static GitContentManager getContentManager(final GitDb database, final ISearchProvider searchProvider,
                                                       final ContentMapper contentMapper, final PropertiesLoader globalProperties) {
        if (null == contentManager) {
            contentManager = new GitContentManager(database, searchProvider, contentMapper, globalProperties);
            log.info("Creating singleton of ContentManager");
        }

        return contentManager;
    }

    /**
     * This provides a singleton of the LogManager for the Segue facade.
     *
     * Note: This is a singleton as logs are created very often and we wanted to minimise the overhead in class
     * creation. Although we can convert this to instances if we want to tidy this up.
     *
     * @param database
     *            - database reference
     * @param loggingEnabled
     *            - boolean to determine if we should persist log messages.
     * @param lhm
     *            - location history manager
     * @return A fully configured LogManager
     */
    @Inject
    @Provides
    @Singleton
    private static ILogManager getLogManager(final PostgresSqlDb database,
                                             @Named(Constants.LOGGING_ENABLED) final boolean loggingEnabled,
                                             final LocationManager lhm) {

        if (null == logManager) {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            logManager = new PgLogManagerEventListener(new PgLogManager(database, objectMapper, loggingEnabled, lhm));

            log.info("Creating singleton of LogManager");
            if (loggingEnabled) {
                log.info("Log manager configured to record logging.");
            } else {
                log.info("Log manager configured NOT to record logging.");
            }
        }

        return logManager;
    }

    /**
     * This provides a singleton of the contentVersionController for the segue facade. 
     * Note: This is a singleton because this content mapper has to use reflection to register all content classes.
     *
     * @return Content version controller with associated dependencies.
     */
    @Inject
    @Provides
    @Singleton
    private static ContentMapper getContentMapper() {
        if (null == mapper) {
            mapper = new ContentMapper(getReflectionsClass("uk.ac.cam.cl.dtg"));
            log.info("Creating Singleton of the Content Mapper");
        }

        return mapper;
    }

    /**
     * This provides an instance of the SegueLocalAuthenticator.
     *
     *
     * @param database
     * 			- the database to access userInformation
     * @param passwordDataManager
     * 			- the database to access passwords
     * @param properties
     * 			- the global system properties
     * @return an instance of the queue
     */
    @Inject
    @Provides
    private static SegueLocalAuthenticator getSegueLocalAuthenticator(final IUserDataManager database, final IPasswordDataManager passwordDataManager,
                                                                      final PropertiesLoader properties) {
        ISegueHashingAlgorithm preferredAlgorithm = new SeguePBKDF2v3();
        ISegueHashingAlgorithm oldAlgorithm1 = new SeguePBKDF2v1();
        ISegueHashingAlgorithm oldAlgorithm2 = new SeguePBKDF2v2();

        Map<String, ISegueHashingAlgorithm> possibleAlgorithms = ImmutableMap.of(
                        preferredAlgorithm.hashingAlgorithmName(), preferredAlgorithm,
                        oldAlgorithm1.hashingAlgorithmName(), oldAlgorithm1,
                        oldAlgorithm2.hashingAlgorithmName(), oldAlgorithm2
        );

        return new SegueLocalAuthenticator(database, passwordDataManager, properties, possibleAlgorithms, preferredAlgorithm);
    }

    /**
     * This provides a singleton of the e-mail manager class.
     *
     * Note: This has to be a singleton because it manages all emails sent using this JVM.
     *
     * @param properties
     * 			- the properties so we can generate email
     * @param emailCommunicator
     *            the class the queue will send messages with
     * @param userPreferenceManager
     * 			- the class providing email preferences
     * @param contentManager
     * 			- the content so we can access email templates
     * @param logManager
     * 			- the logManager to log email sent
     * @return an instance of the queue
     */
    @Inject
    @Provides
    @Singleton
    private static EmailManager getMessageCommunicationQueue(final PropertiesLoader properties, final EmailCommunicator emailCommunicator,
                                                             final AbstractUserPreferenceManager userPreferenceManager,
                                                             final IContentManager contentManager,
                                                             final ILogManager logManager) {

        Map<String, String> globalTokens = Maps.newHashMap();
        globalTokens.put("sig", properties.getProperty(EMAIL_SIGNATURE));
        globalTokens.put("emailPreferencesURL", String.format("https://%s/account#emailpreferences",
                properties.getProperty(HOST_NAME)));
        globalTokens.put("myAssignmentsURL", String.format("https://%s/assignments",
                properties.getProperty(HOST_NAME)));
        globalTokens.put("myQuizzesURL", String.format("https://%s/quizzes",
            properties.getProperty(HOST_NAME)));
        globalTokens.put("myBookedEventsURL", String.format("https://%s/events?show_booked_only=true",
                properties.getProperty(HOST_NAME)));
        globalTokens.put("contactUsURL", String.format("https://%s/contact",
                properties.getProperty(HOST_NAME)));
        globalTokens.put("accountURL", String.format("https://%s/account",
                properties.getProperty(HOST_NAME)));
        globalTokens.put("siteBaseURL", String.format("https://%s", properties.getProperty(HOST_NAME)));

        if (null == emailCommunicationQueue) {
            emailCommunicationQueue = new EmailManager(emailCommunicator, userPreferenceManager, properties,
                    contentManager, logManager, globalTokens);
            log.info("Creating singleton of EmailCommunicationQueue");
        }
        return emailCommunicationQueue;
    }

    /**
     * This provides a singleton of the UserManager for various facades.
     *
     * Note: This has to be a singleton as the User Manager keeps a temporary cache of anonymous users.
     *
     * @param database
     *            - the user persistence manager.
     * @param properties
     *            - properties loader
     * @param providersToRegister
     *            - list of known providers.
     * @param emailQueue
     *            - so that we can send e-mails.
     * @return Content version controller with associated dependencies.
     */
    @Inject
    @Provides
    @Singleton
    private UserAuthenticationManager getUserAuthenticationManager(final IUserDataManager database, final PropertiesLoader properties,
                                              final Map<AuthenticationProvider, IAuthenticator> providersToRegister,
                                              final EmailManager emailQueue) {
        if (null == userAuthenticationManager) {
            userAuthenticationManager = new UserAuthenticationManager(database, properties, providersToRegister, emailQueue);
            log.info("Creating singleton of UserAuthenticationManager");
        }

        return userAuthenticationManager;
    }

    /**
     * This provides a singleton of the UserManager for various facades.
     *
     * Note: This has to be a singleton as the User Manager keeps a temporary cache of anonymous users.
     *
     * @param database
     *            - the user persistence manager.
     * @param questionManager
     *            - IUserManager
     * @param properties
     *            - properties loader
     * @param providersToRegister
     *            - list of known providers.
     * @param emailQueue
     *            - so that we can send e-mails.
     * @param temporaryUserCache
     *            - to manage temporary anonymous users
     * @param logManager
     *            - so that we can log interesting user based events.
     * @param mapperFacade
     *            - for DO and DTO mapping.
     * @param userAuthenticationManager
     *            - Responsible for handling the various authentication functions.
     * @param secondFactorManager
     *            - For managing TOTP multifactor authentication.
     * @return Content version controller with associated dependencies.
     */
    @Inject
    @Provides
    @Singleton
    private IUserAccountManager getUserManager(final IUserDataManager database, final QuestionManager questionManager,
                                               final PropertiesLoader properties, final Map<AuthenticationProvider, IAuthenticator> providersToRegister,
                                               final EmailManager emailQueue, final IAnonymousUserDataManager temporaryUserCache,
                                               final ILogManager logManager, final MapperFacade mapperFacade,
                                               final UserAuthenticationManager userAuthenticationManager,
                                               final ISecondFactorAuthenticator secondFactorManager) {
        if (null == userManager) {
            userManager = new UserAccountManager(database, questionManager, properties, providersToRegister,
                    mapperFacade, emailQueue, temporaryUserCache, logManager, userAuthenticationManager, secondFactorManager);
            log.info("Creating singleton of UserManager");
        }

        return userManager;
    }

    /**
     * QuestionManager.
     * Note: This has to be a singleton as the question manager keeps anonymous question attempts in memory.
     *
     * @param ds - postgres data source
     * @param objectMapper - mapper
     * @return a singleton for question persistence.
     */
    @Inject
    @Provides
    @Singleton
    private IQuestionAttemptManager getQuestionManager(final PostgresSqlDb ds, final ContentMapper objectMapper) {
        // this needs to be a singleton as it provides a temporary cache for anonymous question attempts.
        if (null == questionPersistenceManager) {
            questionPersistenceManager = new PgQuestionAttempts(ds, objectMapper);
            log.info("Creating singleton of IQuestionAttemptManager");
        }

        return questionPersistenceManager;
    }

    /**
     * This provides a singleton of the GroupManager.
     *
     * Note: This needs to be a singleton as we register observers for groups.
     *
     * @param userGroupDataManager
     *            - user group data manager
     * @param userManager
     *            - user manager
     * @param dtoMapper
     *            - dtoMapper
     * @return group manager
     */
    @Inject
    @Provides
    @Singleton
    private GroupManager getGroupManager(final IUserGroupPersistenceManager userGroupDataManager,
                                         final UserAccountManager userManager, final GameManager gameManager,
                                         final MapperFacade dtoMapper) {

        if (null == groupManager) {
            groupManager = new GroupManager(userGroupDataManager, userManager, gameManager, dtoMapper);
            log.info("Creating singleton of GroupManager");
        }

        return groupManager;
    }


    @Inject
    @Provides
    @Singleton
    private IGroupObserver getGroupObserver(EmailManager emailManager, GroupManager groupManager, UserAccountManager userManager,
                                            AssignmentManager assignmentManager, QuizAssignmentManager quizAssignmentManager) {
        if (null == groupObserver) {
            groupObserver = new GroupChangedService(emailManager, groupManager, userManager, assignmentManager, quizAssignmentManager);
            log.info("Creating singleton of GroupObserver");
        }
        return groupObserver;
    }

    /**
     * Get singleton of misuseMonitor.
     *
     * Note: this has to be a singleton as it tracks (in memory) the number of misuses.
     *
     * @param emailManager
     *            - so that the monitors can send e-mails.
     * @param properties
     *            - so that the monitors can look up email settings etc.
     * @return gets the singleton of the misuse manager.
     */
    @Inject
    @Provides
    @Singleton
    private IMisuseMonitor getMisuseMonitor(final EmailManager emailManager, final PropertiesLoader properties) {
        if (null == misuseMonitor) {
            misuseMonitor = new InMemoryMisuseMonitor();
            log.info("Creating singleton of MisuseMonitor");

            // TODO: We should automatically register all handlers that implement this interface using reflection?
            // register handlers segue specific handlers
            misuseMonitor.registerHandler(TokenOwnerLookupMisuseHandler.class.getSimpleName(),
                    new TokenOwnerLookupMisuseHandler(emailManager, properties));

            misuseMonitor.registerHandler(GroupManagerLookupMisuseHandler.class.getSimpleName(),
                    new GroupManagerLookupMisuseHandler(emailManager, properties));

            misuseMonitor.registerHandler(EmailVerificationMisuseHandler.class.getSimpleName(),
                    new EmailVerificationMisuseHandler());

            misuseMonitor.registerHandler(EmailVerificationRequestMisuseHandler.class.getSimpleName(),
                    new EmailVerificationRequestMisuseHandler());

            misuseMonitor.registerHandler(PasswordResetByEmailMisuseHandler.class.getSimpleName(),
                    new PasswordResetByEmailMisuseHandler());

            misuseMonitor.registerHandler(PasswordResetByIPMisuseHandler.class.getSimpleName(),
                    new PasswordResetByIPMisuseHandler(emailManager, properties));

            misuseMonitor.registerHandler(TeacherPasswordResetMisuseHandler.class.getSimpleName(),
                    new TeacherPasswordResetMisuseHandler());

            misuseMonitor.registerHandler(RegistrationMisuseHandler.class.getSimpleName(),
                    new RegistrationMisuseHandler(emailManager, properties));

            misuseMonitor.registerHandler(SegueLoginMisuseHandler.class.getSimpleName(),
                    new SegueLoginMisuseHandler(emailManager, properties));

            misuseMonitor.registerHandler(LogEventMisuseHandler.class.getSimpleName(),
                    new LogEventMisuseHandler(emailManager, properties));

            misuseMonitor.registerHandler(QuestionAttemptMisuseHandler.class.getSimpleName(),
                    new QuestionAttemptMisuseHandler(properties));

            misuseMonitor.registerHandler(AnonQuestionAttemptMisuseHandler.class.getSimpleName(),
                    new AnonQuestionAttemptMisuseHandler());

            misuseMonitor.registerHandler(IPQuestionAttemptMisuseHandler.class.getSimpleName(),
                    new IPQuestionAttemptMisuseHandler(emailManager, properties));

            misuseMonitor.registerHandler(UserSearchMisuseHandler.class.getSimpleName(),
                    new UserSearchMisuseHandler());

            misuseMonitor.registerHandler(SendEmailMisuseHandler.class.getSimpleName(),
                    new SendEmailMisuseHandler());
        }

        return misuseMonitor;
    }

    /**
     * Gets the instance of the dozer mapper object.
     *
     * @return a preconfigured instance of an Auto Mapper. This is specialised for mapping SegueObjects.
     */
    @Provides
    @Singleton
    @Inject
    public static MapperFacade getDOtoDTOMapper() {
        return SegueGuiceConfigurationModule.getContentMapper().getAutoMapper();
    }

    /**
     * @return segue version currently running.
     */
    public static String getSegueVersion() {
        return System.getProperty("segue.version");
    }

    /**
     * Gets the instance of the postgres connection wrapper.
     *
     * Note: This needs to be a singleton as it contains a connection pool.
     *
     * @param databaseUrl
     *            - database to connect to.
     * @param username
     *            - port that the mongodb service is running on.
     * @param password
     *            - the name of the database to configure the wrapper to use.
     * @return PostgresSqlDb db object preconfigured to work with the segue database.
     */
    @Provides
    @Singleton
    @Inject
    private static PostgresSqlDb getPostgresDB(@Named(Constants.POSTGRES_DB_URL) final String databaseUrl,
                                               @Named(Constants.POSTGRES_DB_USER) final String username,
                                               @Named(Constants.POSTGRES_DB_PASSWORD) final String password) {

        if (null == postgresDB) {
            postgresDB = new PostgresSqlDb(databaseUrl, username, password);
            log.info("Created Singleton of PostgresDb wrapper");
        }

        return postgresDB;
    }

    /**
     * Gets the instance of the StatisticsManager. Note: this class is a hack and needs to be refactored.... It is
     * currently only a singleton as it keeps a cache.
     *
     * @param userManager
     *            - dependency
     * @param logManager
     *            - dependency
     * @param schoolManager
     *            - dependency
     * @param contentManager
     *            - dependency
     * @param locationHistoryManager
     *            - dependency
     * @param groupManager
     *            - dependency
     * @param questionManager
     *            - dependency
     * @return stats manager
     */
    @Provides
    @Singleton
    @Inject
    private static StatisticsManager getStatsManager(final UserAccountManager userManager,
                                                     final ILogManager logManager, final SchoolListReader schoolManager,
                                                     final IContentManager contentManager, @Named(CONTENT_INDEX) final String contentIndex, final LocationManager locationHistoryManager,
                                                     final GroupManager groupManager, final QuestionManager questionManager,
                                                     final ContentSummarizerService contentSummarizerService,
                                                     final IUserStreaksManager userStreaksManager) {

        if (null == statsManager) {
            statsManager = new StatisticsManager(userManager, logManager, schoolManager, contentManager, contentIndex,
                    locationHistoryManager, groupManager, questionManager, contentSummarizerService, userStreaksManager);
            log.info("Created Singleton of Statistics Manager");
        }

        return statsManager;
    }

    @Provides
    @Singleton
    @Inject
    private static SegueJobService getSegueJobService(final PropertiesLoader properties, final PostgresSqlDb database) throws SchedulerException {
        if (null == segueJobService) {
            SegueScheduledJob PIISQLJob = new SegueScheduledDatabaseScriptJob(
                    "PIIDeleteScheduledJob",
                    "SQLMaintenance",
                    "SQL scheduled job that deletes PII",
                    "0 0 2 * * ?", "db_scripts/scheduled/pii-delete-task.sql");

            SegueScheduledJob cleanUpOldAnonymousUsers = new SegueScheduledDatabaseScriptJob(
                    "cleanAnonymousUsers",
                    "SQLMaintenance",
                    "SQL scheduled job that deletes old AnonymousUsers",
                    "0 30 2 * * ?", "db_scripts/scheduled/anonymous-user-clean-up.sql");

            SegueScheduledJob cleanUpExpiredReservations = new SegueScheduledDatabaseScriptJob(
                    "cleanUpExpiredReservations",
                    "SQLMaintenence",
                    "SQL scheduled job that deletes expired reservations for the event booking system",
                    "0 0 7 * * ?", "db_scripts/scheduled/expired-reservations-clean-up.sql");

            SegueScheduledJob syncMailjetUsers = new SegueScheduledSyncMailjetUsersJob(
                    "syncMailjetUsersJob",
                    "JavaJob",
                    "Sync users to mailjet",
                    "0 0 0/4 ? * * *");

            segueJobService = new SegueJobService(Arrays.asList(PIISQLJob, cleanUpOldAnonymousUsers, cleanUpExpiredReservations, syncMailjetUsers));
            
            String mailjetKey = properties.getProperty(MAILJET_API_KEY);
            String mailjetSecret = properties.getProperty(MAILJET_API_SECRET);

            if (mailjetKey == null && mailjetSecret == null) {
                segueJobService.removeScheduleJob(syncMailjetUsers);
            }
            log.info("Created Segue Job Manager for scheduled jobs");
        }

        return segueJobService;
    }

    /**
     */
    @Provides
    @Singleton
    @Inject
    private static IExternalAccountManager getExternalAccountManager(final PropertiesLoader properties, final PostgresSqlDb database) {

        if (null == externalAccountManager) {
            String mailjetKey = properties.getProperty(MAILJET_API_KEY);
            String mailjetSecret = properties.getProperty(MAILJET_API_SECRET);

            if (null != mailjetKey && null != mailjetSecret && !mailjetKey.isEmpty() && !mailjetSecret.isEmpty()) {
                // If MailJet is configured, initialise the sync:
                IExternalAccountDataManager externalAccountDataManager = new PgExternalAccountPersistenceManager(database);
                MailJetApiClientWrapper mailJetApiClientWrapper = new MailJetApiClientWrapper(mailjetKey, mailjetSecret,
                        properties.getProperty(MAILJET_NEWS_LIST_ID), properties.getProperty(MAILJET_EVENTS_LIST_ID));

                log.info("Created singleton of ExternalAccountManager.");
                externalAccountManager = new ExternalAccountManager(mailJetApiClientWrapper, externalAccountDataManager);
            } else {
                // Else warn and initialise a placeholder that always throws an error if used:
                log.warn("Created stub of ExternalAccountManager since external provider not configured.");
                externalAccountManager = new StubExternalAccountManager();
            }
        }
        return externalAccountManager;
    }

    /**
     * This provides a new instance of the location resolver.
     *
     * @param apiKey
     *            - for using the third party service.
     * @return The singleton instance of EmailCommunicator
     */
    @Inject
    @Provides
    private IPLocationResolver getIPLocator(@Named(Constants.IP_INFO_DB_API_KEY) final String apiKey) {
        return new IPInfoDBLocationResolver(apiKey);
    }

    /**
     * Gets a Game persistence manager.
     *
     * This needs to be a singleton as it maintains temporary boards in memory.
     *
     * @param database
     *            - the database that persists gameboards.
     * @param contentManager
     *            - api that the game manager can use for content resolution.
     * @param mapper
     *            - an instance of an auto mapper for translating gameboard DOs and DTOs efficiently.
     * @param objectMapper
     *            - a mapper to allow content to be resolved.
     * @param uriManager
     *            - so that the we can create content that is aware of its own location
     * @return Game persistence manager object.
     */
    @Inject
    @Provides
    @Singleton
    private static GameboardPersistenceManager getGameboardPersistenceManager(final PostgresSqlDb database,
                                                                              final IContentManager contentManager, final MapperFacade mapper, final ObjectMapper objectMapper,
                                                                              final URIManager uriManager, @Named(CONTENT_INDEX) final String contentIndex) {
        if (null == gameboardPersistenceManager) {
            gameboardPersistenceManager = new GameboardPersistenceManager(database, contentManager, mapper,
                    objectMapper, uriManager, contentIndex);
            log.info("Creating Singleton of GameboardPersistenceManager");
        }

        return gameboardPersistenceManager;
    }

    /**
     * Gets an assignment manager.
     *
     * This needs to be a singleton because operations like emailing are run for each IGroupObserver, the
     * assignment manager should only be one observer.
     *
     * @param assignmentPersistenceManager
     *            - to save assignments
     * @param groupManager
     *            - to allow communication with the group manager.
     * @param emailService
     *            - email service
     * @param gameManager
     *            - the game manager object
     * @param properties
     *            - properties loader for the service's hostname
     * @return Assignment manager object.
     */
    @Inject
    @Provides
    @Singleton
    private static AssignmentManager getAssignmentManager(
            final IAssignmentPersistenceManager assignmentPersistenceManager, final GroupManager groupManager,
            final EmailService emailService, final GameManager gameManager, final PropertiesLoader properties) {
        if (null == assignmentManager) {
            assignmentManager =  new AssignmentManager(assignmentPersistenceManager, groupManager, emailService, gameManager, properties);
            log.info("Creating Singleton AssignmentManager");
        }
        return assignmentManager;
    }

    /**
     * Gets an instance of the symbolic question validator.
     *
     * @return IsaacSymbolicValidator preconfigured to work with the specified checker.
     */
    @Provides
    @Singleton
    @Inject
    private static IsaacSymbolicValidator getSymbolicValidator(PropertiesLoader properties) {

        return new IsaacSymbolicValidator(properties.getProperty(Constants.EQUALITY_CHECKER_HOST),
                properties.getProperty(Constants.EQUALITY_CHECKER_PORT));
    }

    /**
     * Gets an instance of the chemistry question validator.
     *
     * @return IsaacSymbolicChemistryValidator preconfigured to work with the specified checker.
     */
    @Provides
    @Singleton
    @Inject
    private static IsaacSymbolicChemistryValidator getSymbolicChemistryValidator(PropertiesLoader properties) {

        return new IsaacSymbolicChemistryValidator(properties.getProperty(Constants.CHEMISTRY_CHECKER_HOST),
                properties.getProperty(Constants.CHEMISTRY_CHECKER_PORT));
    }

    /**
     * Gets an instance of the symbolic logic question validator.
     *
     * @return IsaacSymbolicLogicValidator preconfigured to work with the specified checker.
     */
    @Provides
    @Singleton
    @Inject
    private static IsaacSymbolicLogicValidator getSymbolicLogicValidator(PropertiesLoader properties) {

        return new IsaacSymbolicLogicValidator(properties.getProperty(Constants.EQUALITY_CHECKER_HOST),
                properties.getProperty(Constants.EQUALITY_CHECKER_PORT));
    }

    /**
     * This provides a singleton of the SchoolListReader for use by segue backed applications..
     *
     * We want this to be a singleton as otherwise it may not be threadsafe for loading into same SearchProvider.
     *
     * @param provider
     *            - The search provider.
     * @return schoolList reader
     */
    @Inject
    @Provides
    @Singleton
    private SchoolListReader getSchoolListReader(final ISearchProvider provider) {
        if (null == schoolListReader) {
            schoolListReader = new SchoolListReader(provider);
            log.info("Creating singleton of SchoolListReader");
        }
        return schoolListReader;
    }

    /**
     * Utility method to make the syntax of property bindings clearer.
     *
     * @param propertyLabel
     *            - Key for a given property
     * @param propertyLoader
     *            - property loader to use
     */
    private void bindConstantToProperty(final String propertyLabel, final PropertiesLoader propertyLoader) {
        bindConstant().annotatedWith(Names.named(propertyLabel)).to(propertyLoader.getProperty(propertyLabel));
    }

    /**
     * Utility method to get a pre-generated reflections class for the uk.ac.cam.cl.dtg.segue package.
     *
     * @return reflections.
     */
    public static Reflections getReflectionsClass(final String pkg) {
        if (!reflections.containsKey(pkg)) {
            log.info(String.format("Caching reflections scan on '%s'", pkg));
            reflections.put(pkg, new Reflections(pkg));
        }
        return reflections.get(pkg);
    }

    /**
     * Gets the segue classes that should be registered as context listeners.
     *
     * @return the list of context listener classes (these should all be singletons).
     */
    public static Collection<Class<? extends ServletContextListener>> getRegisteredContextListenerClasses() {

        if (null == contextListeners) {
            contextListeners = Lists.newArrayList();

            Set<Class<? extends ServletContextListener>> subTypes = getReflectionsClass("uk.ac.cam.cl.dtg.segue")
                    .getSubTypesOf(ServletContextListener.class);

            Set<Class<? extends ServletContextListener>> etlSubTypes = getReflectionsClass("uk.ac.cam.cl.dtg.segue.etl")
                    .getSubTypesOf(ServletContextListener.class);

            subTypes.removeAll(etlSubTypes);

            for (Class<? extends ServletContextListener> contextListener : subTypes) {
                contextListeners.add(contextListener);
                log.info("Registering context listener class " + contextListener.getCanonicalName());
            }
        }

        return contextListeners;
    }

    @Override
    public void contextInitialized(final ServletContextEvent sce) {
        // nothing needed
    }

    @Override
    public void contextDestroyed(final ServletContextEvent sce) {
        // Close all resources we hold.
        log.info("Segue Config Module notified of shutdown. Releasing resources");
        elasticSearchClient.close();
        elasticSearchClient = null;

        postgresDB.close();
        postgresDB = null;
    }

    /**
     * Factory method for providing a single Guice Injector class.
     *
     * @return a Guice Injector configured with this SegueGuiceConfigurationModule.
     */
    public static synchronized Injector getGuiceInjector() {
        if (null == injector) {
            injector = Guice.createInjector(new SegueGuiceConfigurationModule());
        }
        return injector;
    }
}
