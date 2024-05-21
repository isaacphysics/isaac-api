/**
 * Copyright 2014 Stephen Cummins
 * <br>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * <br>
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * <br>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.ac.cam.cl.dtg.segue.configuration;

import static java.util.Objects.requireNonNull;
import static uk.ac.cam.cl.dtg.segue.api.Constants.CONFIG_LOCATION_SYSTEM_PROPERTY;
import static uk.ac.cam.cl.dtg.segue.api.Constants.CONTENT_INDEX;
import static uk.ac.cam.cl.dtg.segue.api.Constants.DEFAULT_LINUX_CONFIG_LOCATION;
import static uk.ac.cam.cl.dtg.segue.api.Constants.EMAIL_SIGNATURE;
import static uk.ac.cam.cl.dtg.segue.api.Constants.EVENT_PRE_POST_EMAILS;
import static uk.ac.cam.cl.dtg.segue.api.Constants.EnvironmentType.DEV;
import static uk.ac.cam.cl.dtg.segue.api.Constants.HOST_NAME;
import static uk.ac.cam.cl.dtg.segue.api.Constants.MAILJET_API_KEY;
import static uk.ac.cam.cl.dtg.segue.api.Constants.MAILJET_API_SECRET;
import static uk.ac.cam.cl.dtg.segue.api.Constants.MAILJET_EVENTS_LIST_ID;
import static uk.ac.cam.cl.dtg.segue.api.Constants.MAILJET_LEGAL_LIST_ID;
import static uk.ac.cam.cl.dtg.segue.api.Constants.MAILJET_NEWS_LIST_ID;
import static uk.ac.cam.cl.dtg.segue.api.Constants.RASPBERRYPI_CALLBACK_URI;
import static uk.ac.cam.cl.dtg.segue.api.Constants.RASPBERRYPI_CLIENT_ID;
import static uk.ac.cam.cl.dtg.segue.api.Constants.RASPBERRYPI_CLIENT_SECRET;
import static uk.ac.cam.cl.dtg.segue.api.Constants.RASPBERRYPI_LOCAL_IDP_METADATA_PATH;
import static uk.ac.cam.cl.dtg.segue.api.Constants.RASPBERRYPI_OAUTH_SCOPES;
import static uk.ac.cam.cl.dtg.segue.api.Constants.SEGUE_APP_ENVIRONMENT;
import static uk.ac.cam.cl.dtg.segue.api.Constants.SEGUE_CONFIG_LOCATION_ENVIRONMENT_PROPERTY;
import static uk.ac.cam.cl.dtg.segue.api.Constants.SEGUE_CONFIG_LOCATION_NOT_SPECIFIED_MESSAGE;
import static uk.ac.cam.cl.dtg.util.ReflectionUtils.getClasses;
import static uk.ac.cam.cl.dtg.util.ReflectionUtils.getSubTypes;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.api.client.util.Lists;
import com.google.api.client.util.Maps;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.UnknownHostException;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.apache.commons.lang3.SystemUtils;
import org.elasticsearch.client.RestHighLevelClient;
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
import uk.ac.cam.cl.dtg.isaac.dos.AbstractUserPreferenceManager;
import uk.ac.cam.cl.dtg.isaac.dos.IUserAlerts;
import uk.ac.cam.cl.dtg.isaac.dos.IUserStreaksManager;
import uk.ac.cam.cl.dtg.isaac.dos.PgUserAlerts;
import uk.ac.cam.cl.dtg.isaac.dos.PgUserPreferenceManager;
import uk.ac.cam.cl.dtg.isaac.dos.PgUserStreakManager;
import uk.ac.cam.cl.dtg.isaac.mappers.ContentMapper;
import uk.ac.cam.cl.dtg.isaac.mappers.EventMapper;
import uk.ac.cam.cl.dtg.isaac.mappers.MainObjectMapper;
import uk.ac.cam.cl.dtg.isaac.mappers.MiscMapper;
import uk.ac.cam.cl.dtg.isaac.mappers.UserMapper;
import uk.ac.cam.cl.dtg.isaac.quiz.IQuestionAttemptManager;
import uk.ac.cam.cl.dtg.isaac.quiz.IsaacSymbolicLogicValidator;
import uk.ac.cam.cl.dtg.isaac.quiz.IsaacSymbolicValidator;
import uk.ac.cam.cl.dtg.isaac.quiz.PgQuestionAttempts;
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
import uk.ac.cam.cl.dtg.segue.api.monitors.AnonQuestionAttemptMisuseHandler;
import uk.ac.cam.cl.dtg.segue.api.monitors.EmailVerificationMisuseHandler;
import uk.ac.cam.cl.dtg.segue.api.monitors.EmailVerificationRequestMisuseHandler;
import uk.ac.cam.cl.dtg.segue.api.monitors.GroupManagerLookupMisuseHandler;
import uk.ac.cam.cl.dtg.segue.api.monitors.IMetricsExporter;
import uk.ac.cam.cl.dtg.segue.api.monitors.IMisuseMonitor;
import uk.ac.cam.cl.dtg.segue.api.monitors.IPQuestionAttemptMisuseHandler;
import uk.ac.cam.cl.dtg.segue.api.monitors.InMemoryMisuseMonitor;
import uk.ac.cam.cl.dtg.segue.api.monitors.LogEventMisuseHandler;
import uk.ac.cam.cl.dtg.segue.api.monitors.PasswordResetByEmailMisuseHandler;
import uk.ac.cam.cl.dtg.segue.api.monitors.PasswordResetByIPMisuseHandler;
import uk.ac.cam.cl.dtg.segue.api.monitors.PrometheusMetricsExporter;
import uk.ac.cam.cl.dtg.segue.api.monitors.QuestionAttemptMisuseHandler;
import uk.ac.cam.cl.dtg.segue.api.monitors.RegistrationMisuseHandler;
import uk.ac.cam.cl.dtg.segue.api.monitors.SegueLoginByEmailMisuseHandler;
import uk.ac.cam.cl.dtg.segue.api.monitors.SegueLoginByIPMisuseHandler;
import uk.ac.cam.cl.dtg.segue.api.monitors.SendEmailMisuseHandler;
import uk.ac.cam.cl.dtg.segue.api.monitors.TeacherPasswordResetMisuseHandler;
import uk.ac.cam.cl.dtg.segue.api.monitors.TokenOwnerLookupMisuseHandler;
import uk.ac.cam.cl.dtg.segue.api.monitors.UserSearchMisuseHandler;
import uk.ac.cam.cl.dtg.segue.auth.AuthenticationProvider;
import uk.ac.cam.cl.dtg.segue.auth.FacebookAuthenticator;
import uk.ac.cam.cl.dtg.segue.auth.GoogleAuthenticator;
import uk.ac.cam.cl.dtg.segue.auth.IAuthenticator;
import uk.ac.cam.cl.dtg.segue.auth.ISecondFactorAuthenticator;
import uk.ac.cam.cl.dtg.segue.auth.ISegueHashingAlgorithm;
import uk.ac.cam.cl.dtg.segue.auth.RaspberryPiOidcAuthenticator;
import uk.ac.cam.cl.dtg.segue.auth.SegueLocalAuthenticator;
import uk.ac.cam.cl.dtg.segue.auth.SeguePBKDF2v1;
import uk.ac.cam.cl.dtg.segue.auth.SeguePBKDF2v2;
import uk.ac.cam.cl.dtg.segue.auth.SeguePBKDF2v3;
import uk.ac.cam.cl.dtg.segue.auth.SegueSCryptv1;
import uk.ac.cam.cl.dtg.segue.auth.SegueTOTPAuthenticator;
import uk.ac.cam.cl.dtg.segue.auth.TwitterAuthenticator;
import uk.ac.cam.cl.dtg.segue.comm.EmailCommunicator;
import uk.ac.cam.cl.dtg.segue.comm.EmailManager;
import uk.ac.cam.cl.dtg.segue.comm.ICommunicator;
import uk.ac.cam.cl.dtg.segue.dao.ILogManager;
import uk.ac.cam.cl.dtg.segue.dao.LogManagerEventPublisher;
import uk.ac.cam.cl.dtg.segue.dao.PgLogManager;
import uk.ac.cam.cl.dtg.segue.dao.PgLogManagerEventListener;
import uk.ac.cam.cl.dtg.segue.dao.associations.IAssociationDataManager;
import uk.ac.cam.cl.dtg.segue.dao.associations.PgAssociationDataManager;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentMapperUtils;
import uk.ac.cam.cl.dtg.segue.dao.content.GitContentManager;
import uk.ac.cam.cl.dtg.segue.dao.schools.SchoolListReader;
import uk.ac.cam.cl.dtg.segue.dao.userbadges.IUserBadgePersistenceManager;
import uk.ac.cam.cl.dtg.segue.dao.userbadges.PgUserBadgePersistenceManager;
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
import uk.ac.cam.cl.dtg.segue.scheduler.SegueJobService;
import uk.ac.cam.cl.dtg.segue.scheduler.SegueScheduledDatabaseScriptJob;
import uk.ac.cam.cl.dtg.segue.scheduler.SegueScheduledJob;
import uk.ac.cam.cl.dtg.segue.scheduler.jobs.DeleteEventAdditionalBookingInformationJob;
import uk.ac.cam.cl.dtg.segue.scheduler.jobs.DeleteEventAdditionalBookingInformationOneYearJob;
import uk.ac.cam.cl.dtg.segue.scheduler.jobs.EventFeedbackEmailJob;
import uk.ac.cam.cl.dtg.segue.scheduler.jobs.EventReminderEmailJob;
import uk.ac.cam.cl.dtg.segue.scheduler.jobs.ScheduledAssignmentsEmailJob;
import uk.ac.cam.cl.dtg.segue.scheduler.jobs.SegueScheduledSyncMailjetUsersJob;
import uk.ac.cam.cl.dtg.segue.search.ElasticSearchProvider;
import uk.ac.cam.cl.dtg.segue.search.ISearchProvider;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;
import uk.ac.cam.cl.dtg.util.email.MailJetApiClientWrapper;

/**
 * This class is responsible for injecting configuration values for persistence related classes.
 */
public class SegueGuiceConfigurationModule extends AbstractModule implements ServletContextListener {
  private static final Logger log = LoggerFactory.getLogger(SegueGuiceConfigurationModule.class);

  private static String version = null;

  private static Injector injector = null;

  private static PropertiesLoader globalProperties = null;

  // Singletons - we only ever want there to be one instance of each of these.
  private static PostgresSqlDb postgresDB;
  private static ContentMapperUtils mapperUtils = null;
  private static GitContentManager contentManager = null;
  private static RestHighLevelClient elasticSearchClient = null;
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
  private static final Map<String, Set<Class<?>>> classesByPackage = new HashMap<>();

  /**
   * A setter method that is mostly useful for testing. It populates the global properties static value if it has not
   * previously been set.
   *
   * @param globalProperties PropertiesLoader object to be used for loading properties
   *                             (if it has not previously been set).
   */
  public static void setGlobalPropertiesIfNotSet(final PropertiesLoader globalProperties) {
    if (SegueGuiceConfigurationModule.globalProperties == null) {
      SegueGuiceConfigurationModule.globalProperties = globalProperties;
    }
  }

  /**
   * Create a SegueGuiceConfigurationModule.
   */
  public SegueGuiceConfigurationModule() {
    if (globalProperties == null) {
      // check the following places to determine where config file location may be.
      // 1) system env variable, 2) java param (system property), 3) use a default from the constant file.
      String configLocation = SystemUtils.IS_OS_LINUX ? DEFAULT_LINUX_CONFIG_LOCATION : null;
      if (System.getProperty(CONFIG_LOCATION_SYSTEM_PROPERTY) != null) {
        configLocation = System.getProperty(CONFIG_LOCATION_SYSTEM_PROPERTY);
      }
      if (System.getenv(SEGUE_CONFIG_LOCATION_ENVIRONMENT_PROPERTY) != null) {
        configLocation = System.getenv(SEGUE_CONFIG_LOCATION_ENVIRONMENT_PROPERTY);
      }

      try {
        if (null == configLocation) {
          throw new FileNotFoundException(SEGUE_CONFIG_LOCATION_NOT_SPECIFIED_MESSAGE);
        }

        globalProperties = new PropertiesLoader(configLocation);

        log.info("Segue using configuration file: {}", configLocation);

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
      log.error("IOException during setup process.", e);
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
    this.bindConstantToProperty(Constants.SEARCH_CLUSTER_INFO_PORT, globalProperties);
    this.bindConstantToProperty(Constants.SEARCH_CLUSTER_USERNAME, globalProperties);
    this.bindConstantToProperty(Constants.SEARCH_CLUSTER_PASSWORD, globalProperties);

    this.bindConstantToProperty(Constants.HOST_NAME, globalProperties);
    this.bindConstantToProperty(Constants.MAILER_SMTP_SERVER, globalProperties);
    this.bindConstantToProperty(Constants.MAILER_SMTP_USERNAME, globalProperties);
    this.bindConstantToProperty(Constants.MAILER_SMTP_PASSWORD, globalProperties);
    this.bindConstantToProperty(Constants.MAILER_SMTP_PORT, globalProperties);
    this.bindConstantToProperty(Constants.MAIL_FROM_ADDRESS, globalProperties);
    this.bindConstantToProperty(Constants.MAIL_NAME, globalProperties);

    this.bindConstantToProperty(Constants.LOGGING_ENABLED, globalProperties);

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
   * Configure all things persistence-related.
   *
   * @throws IOException when we cannot load the database.
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
    MapBinder<AuthenticationProvider, IAuthenticator> mapBinder = MapBinder.newMapBinder(binder(),
        AuthenticationProvider.class, IAuthenticator.class);

    this.bindConstantToProperty(Constants.HMAC_SALT, globalProperties);
    //Google reCAPTCHA
    this.bindConstantToProperty(Constants.GOOGLE_RECAPTCHA_SECRET, globalProperties);

    // Configure security providers
    // Google
    this.bindConstantToProperty(Constants.GOOGLE_CLIENT_SECRET_LOCATION, globalProperties);
    this.bindConstantToProperty(Constants.GOOGLE_CALLBACK_URI, globalProperties);
    this.bindConstantToProperty(Constants.GOOGLE_OAUTH_SCOPES, globalProperties);
    mapBinder.addBinding(AuthenticationProvider.GOOGLE).to(GoogleAuthenticator.class);

    // Facebook
    this.bindConstantToProperty(Constants.FACEBOOK_SECRET, globalProperties);
    this.bindConstantToProperty(Constants.FACEBOOK_CLIENT_ID, globalProperties);
    this.bindConstantToProperty(Constants.FACEBOOK_CALLBACK_URI, globalProperties);
    this.bindConstantToProperty(Constants.FACEBOOK_OAUTH_SCOPES, globalProperties);
    this.bindConstantToProperty(Constants.FACEBOOK_USER_FIELDS, globalProperties);
    mapBinder.addBinding(AuthenticationProvider.FACEBOOK).to(FacebookAuthenticator.class);

    // Twitter
    this.bindConstantToProperty(Constants.TWITTER_SECRET, globalProperties);
    this.bindConstantToProperty(Constants.TWITTER_CLIENT_ID, globalProperties);
    this.bindConstantToProperty(Constants.TWITTER_CALLBACK_URI, globalProperties);
    mapBinder.addBinding(AuthenticationProvider.TWITTER).to(TwitterAuthenticator.class);

    // Raspberry Pi
    try {
      // Ensure all the required config properties are present.
      requireNonNull(globalProperties.getProperty(RASPBERRYPI_CLIENT_ID));
      requireNonNull(globalProperties.getProperty(RASPBERRYPI_CLIENT_SECRET));
      requireNonNull(globalProperties.getProperty(RASPBERRYPI_CALLBACK_URI));
      requireNonNull(globalProperties.getProperty(RASPBERRYPI_OAUTH_SCOPES));
      requireNonNull(globalProperties.getProperty(RASPBERRYPI_LOCAL_IDP_METADATA_PATH));

      // If so, bind them to constants.
      this.bindConstantToProperty(Constants.RASPBERRYPI_CLIENT_ID, globalProperties);
      this.bindConstantToProperty(Constants.RASPBERRYPI_CLIENT_SECRET, globalProperties);
      this.bindConstantToProperty(Constants.RASPBERRYPI_CALLBACK_URI, globalProperties);
      this.bindConstantToProperty(Constants.RASPBERRYPI_OAUTH_SCOPES, globalProperties);
      this.bindConstantToProperty(Constants.RASPBERRYPI_LOCAL_IDP_METADATA_PATH, globalProperties);

      // Register the authenticator.
      mapBinder.addBinding(AuthenticationProvider.RASPBERRYPI).to(RaspberryPiOidcAuthenticator.class);

    } catch (NullPointerException e) {
      log.error(String.format("Failed to initialise authenticator %s due to one or more absent config properties.",
          AuthenticationProvider.RASPBERRYPI));
    }

    // Segue local
    mapBinder.addBinding(AuthenticationProvider.SEGUE).to(SegueLocalAuthenticator.class);
  }

  /**
   * Deals with application data managers.
   */
  private void configureApplicationManagers() {
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
        log.info("Creating MetricsExporter on port ({})", port);
        metricsExporter = new PrometheusMetricsExporter(port);
        log.info("Exporting default JVM metrics.");
        metricsExporter.exposeJvmMetrics();
      } catch (IOException e) {
        log.error("Could not create MetricsExporter on port ({})", port);
        return null;
      }
    }
    return metricsExporter;
  }

  /**
   * This provides a singleton of the elasticSearch client that can be used by Guice.
   * <br>
   * The client is threadsafe, so we don't need to keep creating new ones.
   *
   * @param address  address of the cluster to create.
   * @param port     port of the cluster to create.
   * @param username username for cluster user.
   * @param password password for cluster user.
   * @return Client to be injected into ElasticSearch Provider.
   */
  @Inject
  @Provides
  @Singleton
  private static RestHighLevelClient getSearchConnectionInformation(
      @Named(Constants.SEARCH_CLUSTER_ADDRESS) final String address,
      @Named(Constants.SEARCH_CLUSTER_INFO_PORT) final int port,
      @Named(Constants.SEARCH_CLUSTER_USERNAME) final String username,
      @Named(Constants.SEARCH_CLUSTER_PASSWORD) final String password) {
    if (null == elasticSearchClient) {
      try {
        elasticSearchClient = ElasticSearchProvider.getClient(address, port, username, password);
        log.info("Creating singleton of ElasticSearchProvider");
      } catch (UnknownHostException e) {
        log.error("Could not create ElasticSearchProvider");
        return null;
      }
    }
    // eventually we want to do something like the below to make sure we get updated clients
    //    if (elasticSearchClient instanceof TransportClient) {
    //      TransportClient tc = (TransportClient) elasticSearchClient;
    //      if (tc.connectedNodes().isEmpty()) {
    //        tc.close();
    //        log.error("The elasticsearch client is not connected to any nodes. Trying to reconnect...");
    //        elasticSearchClient = null;
    //        return getSearchConnectionInformation(clusterName, address, port);
    //      }
    //    }

    return elasticSearchClient;
  }

  /**
   * This provides a singleton of the git content manager for the segue facade.
   * <br>
   * TODO: This is a singleton as the units and tags are stored in memory. If we move these out it can be an instance.
   *  This would be better as then we can give it a new search provider if the client has closed.
   *
   * @param database           database reference
   * @param searchProvider     search provider to use
   * @param contentMapperUtils content mapper to use.
   * @param globalProperties   properties loader to use
   * @return a fully configured content Manager.
   */
  @Inject
  @Provides
  @Singleton
  private static GitContentManager getContentManager(final GitDb database, final ISearchProvider searchProvider,
                                                     final ContentMapperUtils contentMapperUtils,
                                                     final ContentMapper objectMapper,
                                                     final PropertiesLoader globalProperties) {
    if (null == contentManager) {
      contentManager =
          new GitContentManager(database, searchProvider, contentMapperUtils, objectMapper, globalProperties);
      log.info("Creating singleton of ContentManager");
    }

    return contentManager;
  }

  /**
   * This provides a singleton of the LogManager for the Segue facade.
   * <br>
   * Note: This is a singleton as logs are created very often and we wanted to minimise the overhead in class
   * creation. Although we can convert this to instances if we want to tidy this up.
   *
   * @param database       database reference
   * @param loggingEnabled boolean to determine if we should persist log messages.
   * @return A fully configured LogManager
   */
  @Inject
  @Provides
  @Singleton
  private static ILogManager getLogManager(final PostgresSqlDb database,
                                           @Named(Constants.LOGGING_ENABLED) final boolean loggingEnabled) {

    if (null == logManager) {
      ObjectMapper objectMapper = new ObjectMapper();
      objectMapper.registerModule(new JavaTimeModule());
      objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
      objectMapper.configure(DeserializationFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS, false);
      logManager = new PgLogManagerEventListener(new PgLogManager(database, objectMapper, loggingEnabled));

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
  private static ContentMapperUtils getContentMapper() {
    if (null == mapperUtils) {
      Set<Class<?>> c = getClasses("uk.ac.cam.cl.dtg");
      mapperUtils = new ContentMapperUtils(c);
      log.info("Creating Singleton of the Content Mapper");
    }

    return mapperUtils;
  }

  /**
   * This provides an instance of the SegueLocalAuthenticator.
   * <br>
   *
   * @param database            the database to access userInformation
   * @param passwordDataManager the database to access passwords
   * @param properties          the global system properties
   * @return an instance of the queue
   */
  @Inject
  @Provides
  private static SegueLocalAuthenticator getSegueLocalAuthenticator(final IUserDataManager database,
                                                                    final IPasswordDataManager passwordDataManager,
                                                                    final PropertiesLoader properties) {
    ISegueHashingAlgorithm preferredAlgorithm = new SegueSCryptv1();
    ISegueHashingAlgorithm oldAlgorithm1 = new SeguePBKDF2v1();
    ISegueHashingAlgorithm oldAlgorithm2 = new SeguePBKDF2v2();
    ISegueHashingAlgorithm oldAlgorithm3 = new SeguePBKDF2v3();

    Map<String, ISegueHashingAlgorithm> possibleAlgorithms = Map.of(
        preferredAlgorithm.hashingAlgorithmName(), preferredAlgorithm,
        oldAlgorithm1.hashingAlgorithmName(), oldAlgorithm1,
        oldAlgorithm2.hashingAlgorithmName(), oldAlgorithm2,
        oldAlgorithm3.hashingAlgorithmName(), oldAlgorithm3
    );

    return new SegueLocalAuthenticator(database, passwordDataManager, properties, possibleAlgorithms,
        preferredAlgorithm);
  }

  /**
   * This provides a singleton of the e-mail manager class.
   * <br>
   * Note: This has to be a singleton because it manages all emails sent using this JVM.
   *
   * @param properties            the properties so we can generate email
   * @param emailCommunicator     the class the queue will send messages with
   * @param userPreferenceManager the class providing email preferences
   * @param contentManager        the content so we can access email templates
   * @param logManager            the logManager to log email sent
   * @return an instance of the queue
   */
  @Inject
  @Provides
  @Singleton
  private static EmailManager getMessageCommunicationQueue(final PropertiesLoader properties,
                                                           final EmailCommunicator emailCommunicator,
                                                           final AbstractUserPreferenceManager userPreferenceManager,
                                                           final GitContentManager contentManager,
                                                           final ILogManager logManager) {

    Map<String, String> globalTokens = Maps.newHashMap();
    globalTokens.put("sig", properties.getProperty(EMAIL_SIGNATURE));
    globalTokens.put("emailPreferencesURL", String.format("https://%s/account#emailpreferences",
        properties.getProperty(HOST_NAME)));
    globalTokens.put("myAssignmentsURL", String.format("https://%s/assignments",
        properties.getProperty(HOST_NAME)));
    String myQuizzesURL = String.format("https://%s/tests", properties.getProperty(HOST_NAME));
    globalTokens.put("myQuizzesURL", myQuizzesURL);
    globalTokens.put("myTestsURL", myQuizzesURL);
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
   * <br>
   * Note: This has to be a singleton as the User Manager keeps a temporary cache of anonymous users.
   *
   * @param database            the user persistence manager.
   * @param properties          properties loader
   * @param providersToRegister list of known providers.
   * @param emailQueue          so that we can send e-mails.
   * @return Content version controller with associated dependencies.
   */
  @Inject
  @Provides
  @Singleton
  private UserAuthenticationManager getUserAuthenticationManager(
      final IUserDataManager database, final PropertiesLoader properties,
      final Map<AuthenticationProvider, IAuthenticator> providersToRegister, final EmailManager emailQueue) {
    if (null == userAuthenticationManager) {
      userAuthenticationManager = new UserAuthenticationManager(database, properties, providersToRegister, emailQueue);
      log.info("Creating singleton of UserAuthenticationManager");
    }

    return userAuthenticationManager;
  }

  /**
   * This provides a singleton of the UserManager for various facades.
   * <br>
   * Note: This has to be a singleton as the User Manager keeps a temporary cache of anonymous users.
   *
   * @param database                  the user persistence manager.
   * @param questionManager           IUserManager
   * @param properties                properties loader
   * @param providersToRegister       list of known providers.
   * @param emailQueue                so that we can send e-mails.
   * @param temporaryUserCache        to manage temporary anonymous users
   * @param logManager                so that we can log interesting user based events.
   * @param mapperFacade              for DO and DTO mapping.
   * @param userAuthenticationManager Responsible for handling the various authentication functions.
   * @param secondFactorManager       For managing TOTP multifactor authentication.
   * @param userPreferenceManager     For managing user preferences.
   * @return Content version controller with associated dependencies.
   */
  @SuppressWarnings("checkstyle:ParameterNumber")
  @Inject
  @Provides
  @Singleton
  private IUserAccountManager getUserManager(final IUserDataManager database, final QuestionManager questionManager,
                                             final PropertiesLoader properties,
                                             final Map<AuthenticationProvider, IAuthenticator> providersToRegister,
                                             final EmailManager emailQueue,
                                             final IAnonymousUserDataManager temporaryUserCache,
                                             final ILogManager logManager, final MainObjectMapper mapperFacade,
                                             final UserAuthenticationManager userAuthenticationManager,
                                             final ISecondFactorAuthenticator secondFactorManager,
                                             final AbstractUserPreferenceManager userPreferenceManager,
                                             final SchoolListReader schoolListReader) {
    if (null == userManager) {
      userManager = new UserAccountManager(database, questionManager, properties, providersToRegister,
          mapperFacade, emailQueue, temporaryUserCache, logManager, userAuthenticationManager,
          secondFactorManager, userPreferenceManager, schoolListReader);
      log.info("Creating singleton of UserManager");
    }

    return userManager;
  }

  /**
   * QuestionManager.
   * Note: This has to be a singleton as the question manager keeps anonymous question attempts in memory.
   *
   * @param ds           postgres data source
   * @param objectMapper mapper
   * @return a singleton for question persistence.
   */
  @Inject
  @Provides
  @Singleton
  private IQuestionAttemptManager getQuestionManager(final PostgresSqlDb ds, final ContentMapperUtils objectMapper) {
    // this needs to be a singleton as it provides a temporary cache for anonymous question attempts.
    if (null == questionPersistenceManager) {
      questionPersistenceManager = new PgQuestionAttempts(ds, objectMapper);
      log.info("Creating singleton of IQuestionAttemptManager");
    }

    return questionPersistenceManager;
  }

  /**
   * This provides a singleton of the GroupManager.
   * <br>
   * Note: This needs to be a singleton as we register observers for groups.
   *
   * @param userGroupDataManager user group data manager
   * @param userManager          user manager
   * @param gameManager          game manager
   * @param dtoMapper            dtoMapper
   * @return group manager
   */
  @Inject
  @Provides
  @Singleton
  private GroupManager getGroupManager(final IUserGroupPersistenceManager userGroupDataManager,
                                       final UserAccountManager userManager, final GameManager gameManager,
                                       final UserMapper dtoMapper) {

    if (null == groupManager) {
      groupManager = new GroupManager(userGroupDataManager, userManager, gameManager, dtoMapper);
      log.info("Creating singleton of GroupManager");
    }

    return groupManager;
  }


  @Inject
  @Provides
  @Singleton
  private IGroupObserver getGroupObserver(
      final EmailManager emailManager, final GroupManager groupManager, final UserAccountManager userManager,
      final AssignmentManager assignmentManager, final QuizAssignmentManager quizAssignmentManager) {
    if (null == groupObserver) {
      groupObserver =
          new GroupChangedService(emailManager, groupManager, userManager, assignmentManager, quizAssignmentManager);
      log.info("Creating singleton of GroupObserver");
    }
    return groupObserver;
  }

  /**
   * Get singleton of misuseMonitor.
   * <br>
   * Note: this has to be a singleton as it tracks (in memory) the number of misuses.
   *
   * @param emailManager so that the monitors can send e-mails.
   * @param properties   so that the monitors can look up email settings etc.
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

      misuseMonitor.registerHandler(SegueLoginByEmailMisuseHandler.class.getSimpleName(),
          new SegueLoginByEmailMisuseHandler(properties));

      misuseMonitor.registerHandler(SegueLoginByIPMisuseHandler.class.getSimpleName(),
          new SegueLoginByIPMisuseHandler());

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

  @Provides
  @Singleton
  @Inject
  public static MainObjectMapper getMainMapperInstance() {
    return MainObjectMapper.INSTANCE;
  }

  @Provides
  @Singleton
  @Inject
  public static ContentMapper getContentMapperInstance() {
    return ContentMapper.INSTANCE;
  }

  @Provides
  @Singleton
  @Inject
  public static UserMapper getUserMapperInstance() {
    return UserMapper.INSTANCE;
  }

  @Provides
  @Singleton
  @Inject
  public static EventMapper getEventMapperInstance() {
    return EventMapper.INSTANCE;
  }

  @Provides
  @Singleton
  @Inject
  public static MiscMapper getMiscMapperInstance() {
    return MiscMapper.INSTANCE;
  }

  /**
   * Get the segue version currently running. Returns the value stored on the module if present or retrieves it from
   * the properties if not.
   *
   * @return the segue version as a string or 'unknown' if it cannot be retrieved
   */
  public static String getSegueVersion() {
    if (SegueGuiceConfigurationModule.version != null) {
      return SegueGuiceConfigurationModule.version;
    }
    String version = "unknown";
    try {
      Properties p = new Properties();
      try (InputStream is = SegueGuiceConfigurationModule.class.getResourceAsStream("/version.properties")) {
        if (is != null) {
          p.load(is);
          version = p.getProperty("version", "");
        }
      }
    } catch (Exception e) {
      log.error(e.getMessage());
    }
    SegueGuiceConfigurationModule.version = version;
    return version;
  }

  /**
   * Gets the instance of the postgres connection wrapper.
   * <br>
   * Note: This needs to be a singleton as it contains a connection pool.
   *
   * @param databaseUrl database to connect to.
   * @param username    port that the mongodb service is running on.
   * @param password    the name of the database to configure the wrapper to use.
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
   * @param userManager              to query user information
   * @param logManager               to query Log information
   * @param schoolManager            to query School information
   * @param contentManager           to query live version information
   * @param contentIndex             index string for current content version
   * @param groupManager             so that we can see how many groups we have site wide.
   * @param questionManager          so that we can see how many questions were answered.
   * @param contentSummarizerService to produce content summary objects
   * @param userStreaksManager       to notify users when their answer streak changes
   * @return stats manager
   */
  @SuppressWarnings("checkstyle:ParameterNumber")
  @Provides
  @Singleton
  @Inject
  private static StatisticsManager getStatsManager(final UserAccountManager userManager,
                                                   final ILogManager logManager, final SchoolListReader schoolManager,
                                                   final GitContentManager contentManager,
                                                   @Named(CONTENT_INDEX) final String contentIndex,
                                                   final GroupManager groupManager,
                                                   final QuestionManager questionManager,
                                                   final ContentSummarizerService contentSummarizerService,
                                                   final IUserStreaksManager userStreaksManager) {

    if (null == statsManager) {
      statsManager = new StatisticsManager(userManager, logManager, schoolManager, contentManager, contentIndex,
          groupManager, questionManager, contentSummarizerService, userStreaksManager);
      log.info("Created Singleton of Statistics Manager");
    }

    return statsManager;
  }

  static final String CRON_STRING_0200_DAILY = "0 0 2 * * ?";
  static final String CRON_STRING_0230_DAILY = "0 30 2 * * ?";
  static final String CRON_STRING_0700_DAILY = "0 0 7 * * ?";
  static final String CRON_STRING_2000_DAILY = "0 0 20 * * ?";
  static final String CRON_STRING_HOURLY = "0 0 * ? * * *";
  static final String CRON_STRING_EVERY_FOUR_HOURS = "0 0 0/4 ? * * *";
  static final String CRON_GROUP_NAME_SQL_MAINTENANCE = "SQLMaintenance";
  static final String CRON_GROUP_NAME_JAVA_JOB = "JavaJob";

  @Provides
  @Singleton
  @Inject
  private static SegueJobService getSegueJobService(final PropertiesLoader properties, final PostgresSqlDb database) {
    if (null == segueJobService) {
      String mailjetKey = properties.getProperty(MAILJET_API_KEY);
      String mailjetSecret = properties.getProperty(MAILJET_API_SECRET);
      String eventPrePostEmails = properties.getProperty(EVENT_PRE_POST_EMAILS);
      boolean eventPrePostEmailsEnabled =
          null != eventPrePostEmails && !eventPrePostEmails.isEmpty() && Boolean.parseBoolean(eventPrePostEmails);

      SegueScheduledJob piiSqlJob = new SegueScheduledDatabaseScriptJob(
          "PIIDeleteScheduledJob",
          CRON_GROUP_NAME_SQL_MAINTENANCE,
          "SQL scheduled job that deletes PII",
          CRON_STRING_0200_DAILY, "db_scripts/scheduled/pii-delete-task.sql");

      SegueScheduledJob cleanUpOldAnonymousUsers = new SegueScheduledDatabaseScriptJob(
          "cleanAnonymousUsers",
          CRON_GROUP_NAME_SQL_MAINTENANCE,
          "SQL scheduled job that deletes old AnonymousUsers",
          CRON_STRING_0230_DAILY, "db_scripts/scheduled/anonymous-user-clean-up.sql");

      SegueScheduledJob cleanUpExpiredReservations = new SegueScheduledDatabaseScriptJob(
          "cleanUpExpiredReservations",
          CRON_GROUP_NAME_SQL_MAINTENANCE,
          "SQL scheduled job that deletes expired reservations for the event booking system",
          CRON_STRING_0700_DAILY, "db_scripts/scheduled/expired-reservations-clean-up.sql");

      SegueScheduledJob deleteEventAdditionalBookingInformation = SegueScheduledJob.createCustomJob(
          "deleteEventAdditionalBookingInformation",
          CRON_GROUP_NAME_JAVA_JOB,
          "Delete event additional booking information a given period after an event has taken place",
          CRON_STRING_0700_DAILY,
          Maps.newHashMap(),
          new DeleteEventAdditionalBookingInformationJob()
      );

      SegueScheduledJob deleteEventAdditionalBookingInformationOneYearJob = SegueScheduledJob.createCustomJob(
          "deleteEventAdditionalBookingInformationOneYear",
          CRON_GROUP_NAME_JAVA_JOB,
          "Delete event additional booking information a year after an event has taken place if not already removed",
          CRON_STRING_0700_DAILY,
          Maps.newHashMap(),
          new DeleteEventAdditionalBookingInformationOneYearJob()
      );

      SegueScheduledJob eventReminderEmail = SegueScheduledJob.createCustomJob(
          "eventReminderEmail",
          CRON_GROUP_NAME_JAVA_JOB,
          "Send scheduled reminder emails to events",
          CRON_STRING_0700_DAILY,
          Maps.newHashMap(),
          new EventReminderEmailJob()
      );

      SegueScheduledJob eventFeedbackEmail = SegueScheduledJob.createCustomJob(
          "eventFeedbackEmail",
          CRON_GROUP_NAME_JAVA_JOB,
          "Send scheduled feedback emails to events",
          CRON_STRING_2000_DAILY,
          Maps.newHashMap(),
          new EventFeedbackEmailJob()
      );

      SegueScheduledJob scheduledAssignmentsEmail = SegueScheduledJob.createCustomJob(
          "scheduledAssignmentsEmail",
          CRON_GROUP_NAME_JAVA_JOB,
          "Send scheduled assignment notification emails to groups",
          CRON_STRING_HOURLY,
          Maps.newHashMap(),
          new ScheduledAssignmentsEmailJob()
      );

      SegueScheduledJob syncMailjetUsers = new SegueScheduledSyncMailjetUsersJob(
          "syncMailjetUsersJob",
          CRON_GROUP_NAME_JAVA_JOB,
          "Sync users to mailjet",
          CRON_STRING_EVERY_FOUR_HOURS);

      List<SegueScheduledJob> configuredScheduledJobs = new ArrayList<>(Arrays.asList(
          piiSqlJob,
          cleanUpOldAnonymousUsers,
          cleanUpExpiredReservations,
          deleteEventAdditionalBookingInformation,
          deleteEventAdditionalBookingInformationOneYearJob,
          scheduledAssignmentsEmail
      ));

      // Simply removing jobs from configuredScheduledJobs won't de-register them if they
      // are currently configured, so the constructor takes a list of jobs to remove too.
      List<SegueScheduledJob> scheduledJobsToRemove = new ArrayList<>();

      if (null != mailjetKey && null != mailjetSecret && !mailjetKey.isEmpty() && !mailjetSecret.isEmpty()) {
        configuredScheduledJobs.add(syncMailjetUsers);
      } else {
        scheduledJobsToRemove.add(syncMailjetUsers);
      }

      if (eventPrePostEmailsEnabled) {
        configuredScheduledJobs.add(eventReminderEmail);
        configuredScheduledJobs.add(eventFeedbackEmail);
      } else {
        scheduledJobsToRemove.add(eventReminderEmail);
        scheduledJobsToRemove.add(eventFeedbackEmail);
      }
      segueJobService = new SegueJobService(database, configuredScheduledJobs, scheduledJobsToRemove);

    }

    return segueJobService;
  }

  @Provides
  @Singleton
  @Inject
  private static IExternalAccountManager getExternalAccountManager(final PropertiesLoader properties,
                                                                   final PostgresSqlDb database) {

    if (null == externalAccountManager) {
      String mailjetKey = properties.getProperty(MAILJET_API_KEY);
      String mailjetSecret = properties.getProperty(MAILJET_API_SECRET);

      if (null != mailjetKey && null != mailjetSecret && !mailjetKey.isEmpty() && !mailjetSecret.isEmpty()) {
        // If MailJet is configured, initialise the sync:
        IExternalAccountDataManager externalAccountDataManager = new PgExternalAccountPersistenceManager(database);
        MailJetApiClientWrapper mailJetApiClientWrapper = new MailJetApiClientWrapper(mailjetKey, mailjetSecret,
            properties.getProperty(MAILJET_NEWS_LIST_ID), properties.getProperty(MAILJET_EVENTS_LIST_ID),
            properties.getProperty(MAILJET_LEGAL_LIST_ID));

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
   * Gets a Game persistence manager.
   * <br>
   * This needs to be a singleton as it maintains temporary boards in memory.
   *
   * @param database       the database that persists gameboards.
   * @param contentManager api that the game manager can use for content resolution.
   * @param mapper         an instance of an auto mapper for translating gameboard DOs and DTOs efficiently.
   * @param objectMapper   a mapper to allow content to be resolved.
   * @param uriManager     so that we can create content that is aware of its own location
   * @return Game persistence manager object.
   */
  @Inject
  @Provides
  @Singleton
  private static GameboardPersistenceManager getGameboardPersistenceManager(
      final PostgresSqlDb database,
      final GitContentManager contentManager,
      final MainObjectMapper mapper,
      final ObjectMapper objectMapper,
      final URIManager uriManager
  ) {
    if (null == gameboardPersistenceManager) {
      gameboardPersistenceManager = new GameboardPersistenceManager(database, contentManager, mapper,
          objectMapper, uriManager);
      log.info("Creating Singleton of GameboardPersistenceManager");
    }

    return gameboardPersistenceManager;
  }

  /**
   * Gets an assignment manager.
   * <br>
   * This needs to be a singleton because operations like emailing are run for each IGroupObserver, the
   * assignment manager should only be one observer.
   *
   * @param assignmentPersistenceManager to save assignments
   * @param groupManager                 to allow communication with the group manager.
   * @param emailService                 email service
   * @param gameManager                  the game manager object
   * @param properties                   properties loader for the service's hostname
   * @return Assignment manager object.
   */
  @Inject
  @Provides
  @Singleton
  private static AssignmentManager getAssignmentManager(
      final IAssignmentPersistenceManager assignmentPersistenceManager, final GroupManager groupManager,
      final EmailService emailService, final GameManager gameManager, final PropertiesLoader properties) {
    if (null == assignmentManager) {
      assignmentManager =
          new AssignmentManager(assignmentPersistenceManager, groupManager, emailService, gameManager, properties);
      log.info("Creating Singleton AssignmentManager");
    }
    return assignmentManager;
  }

  /**
   * Gets an instance of the symbolic question validator.
   *
   * @param properties properties loader to get the symbolic validator host
   * @return IsaacSymbolicValidator preconfigured to work with the specified checker.
   */
  @Provides
  @Singleton
  @Inject
  private static IsaacSymbolicValidator getSymbolicValidator(final PropertiesLoader properties) {

    return new IsaacSymbolicValidator(properties.getProperty(Constants.EQUALITY_CHECKER_HOST),
        properties.getProperty(Constants.EQUALITY_CHECKER_PORT));
  }

  /**
   * Gets an instance of the symbolic logic question validator.
   *
   * @param properties properties loader to get the symbolic logic validator host
   * @return IsaacSymbolicLogicValidator preconfigured to work with the specified checker.
   */
  @Provides
  @Singleton
  @Inject
  private static IsaacSymbolicLogicValidator getSymbolicLogicValidator(final PropertiesLoader properties) {

    return new IsaacSymbolicLogicValidator(properties.getProperty(Constants.EQUALITY_CHECKER_HOST),
        properties.getProperty(Constants.EQUALITY_CHECKER_PORT));
  }

  /**
   * This provides a singleton of the SchoolListReader for use by segue backed applications..
   * <br>
   * We want this to be a singleton as otherwise it may not be threadsafe for loading into same SearchProvider.
   *
   * @param provider The search provider.
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
   * @param propertyLabel  Key for a given property
   * @param propertyLoader property loader to use
   */
  private void bindConstantToProperty(final String propertyLabel, final PropertiesLoader propertyLoader) {
    bindConstant().annotatedWith(Names.named(propertyLabel)).to(propertyLoader.getProperty(propertyLabel));
  }

  /**
   * Utility method to get a pre-generated reflections class for the uk.ac.cam.cl.dtg.segue package.
   *
   * @param pkg class name to use as key
   * @return reflections.
   */
  public static Set<Class<?>> getPackageClasses(final String pkg) {
    return classesByPackage.computeIfAbsent(pkg, key -> {
      log.info(String.format("Caching reflections scan on '%s'", key));
      return getClasses(key);
    });
  }

  /**
   * Gets the segue classes that should be registered as context listeners.
   *
   * @return the list of context listener classes (these should all be singletons).
   */
  public static Collection<Class<? extends ServletContextListener>> getRegisteredContextListenerClasses() {

    if (null == contextListeners) {
      contextListeners = Lists.newArrayList();

      Set<Class<? extends ServletContextListener>> subTypes =
          getSubTypes(getPackageClasses("uk.ac.cam.cl.dtg.segue"), ServletContextListener.class);

      Set<Class<? extends ServletContextListener>> etlSubTypes =
          getSubTypes(getPackageClasses("uk.ac.cam.cl.dtg.segue.etl"), ServletContextListener.class);

      subTypes.removeAll(etlSubTypes);

      for (Class<? extends ServletContextListener> contextListener : subTypes) {
        contextListeners.add(contextListener);
        log.info("Registering context listener class {}", contextListener.getCanonicalName());
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
    try {
      elasticSearchClient.close();
      elasticSearchClient = null;
    } catch (IOException e) {
      log.error("Error releasing Elasticsearch client", e);
    }

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

  @Provides
  @Singleton
  public static Clock getDefaultClock() {
    return Clock.systemUTC();
  }
}
