package uk.ac.cam.cl.dtg.segue.api.managers;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static uk.ac.cam.cl.dtg.isaac.dos.users.Role.STUDENT;
import static uk.ac.cam.cl.dtg.isaac.dos.users.Role.TEACHER;
import static uk.ac.cam.cl.dtg.segue.api.Constants.HMAC_SALT;
import static uk.ac.cam.cl.dtg.segue.api.managers.UserAccountManager.isEmailValid;
import static uk.ac.cam.cl.dtg.segue.api.managers.UserAccountManager.isUserNameValid;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import ma.glasnost.orika.MapperFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.ac.cam.cl.dtg.isaac.dos.PgUserPreferenceManager;
import uk.ac.cam.cl.dtg.isaac.dos.users.RegisteredUser;
import uk.ac.cam.cl.dtg.isaac.dos.users.School;
import uk.ac.cam.cl.dtg.isaac.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.auth.AuthenticationProvider;
import uk.ac.cam.cl.dtg.segue.auth.IAuthenticator;
import uk.ac.cam.cl.dtg.segue.auth.ISecondFactorAuthenticator;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.MissingRequiredFieldException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserException;
import uk.ac.cam.cl.dtg.segue.comm.EmailManager;
import uk.ac.cam.cl.dtg.segue.dao.ILogManager;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dao.schools.SchoolListReader;
import uk.ac.cam.cl.dtg.segue.dao.schools.UnableToIndexSchoolsException;
import uk.ac.cam.cl.dtg.segue.dao.users.PgAnonymousUsers;
import uk.ac.cam.cl.dtg.segue.dao.users.PgUsers;
import uk.ac.cam.cl.dtg.segue.search.SegueSearchException;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

class UserAccountManagerTest {

  private PgUsers database;
  private EmailManager emailManager;
  private SchoolListReader schoolListReader;
  private UserAccountManager userAccountManager;

  @BeforeEach
  public void beforeEach() {
    database = createMock(PgUsers.class);
    QuestionManager questionmanager = createNiceMock(QuestionManager.class);
    PropertiesLoader propertiesLoader = createNiceMock(PropertiesLoader.class);
    Map<AuthenticationProvider, IAuthenticator> providersToRegister = Map.of();
    MapperFacade dtoMapper = createNiceMock(MapperFacade.class);
    emailManager = createMock(EmailManager.class);
    PgAnonymousUsers pgAnonymousUsers = createNiceMock(PgAnonymousUsers.class);
    ILogManager logManager = createNiceMock(ILogManager.class);
    UserAuthenticationManager userAuthenticationManager = createNiceMock(UserAuthenticationManager.class);
    ISecondFactorAuthenticator secondFactorAuthenticator = createNiceMock(ISecondFactorAuthenticator.class);
    PgUserPreferenceManager userPreferenceManager = createNiceMock(PgUserPreferenceManager.class);
    schoolListReader = createMock(SchoolListReader.class);

    expect(propertiesLoader.getProperty(HMAC_SALT)).andStubReturn("SALTY");
    expect(propertiesLoader.getProperty(Constants.SESSION_EXPIRY_SECONDS_DEFAULT)).andStubReturn("60");
    expect(propertiesLoader.getProperty(Constants.HOST_NAME)).andStubReturn("HOST");
    expect(propertiesLoader.getProperty(Constants.MAIL_RECEIVERS)).andReturn("admin@localhost");
    replay(propertiesLoader);

    userAccountManager =
        new UserAccountManager(database, questionmanager, propertiesLoader, providersToRegister, dtoMapper,
            emailManager, pgAnonymousUsers, logManager, userAuthenticationManager, secondFactorAuthenticator,
            userPreferenceManager, schoolListReader);
  }

  @ParameterizedTest
  @MethodSource("namesToTest")
  void nameValidation(boolean expectedIsValid, String name) {
    boolean actualIsValid = isUserNameValid(name);
    assertEquals(expectedIsValid, actualIsValid);
  }

  private static Stream<Arguments> namesToTest() {
    return Stream.of(
        Arguments.of(true, "testname"), // Lowercase is valid
        Arguments.of(true, "TESTNAME"), // Uppercase is valid
        Arguments.of(true, "TestName"), // Mixture of cases is valid
        Arguments.of(true, "TestName123"), // Numbers are valid
        Arguments.of(true, "Test_Name"), // Underscores are valid
        Arguments.of(true, "Test Name"), // Double-barrelled names with spaces are valid
        Arguments.of(true, "Test-Name"), // Double-barrelled names with hyphens are valid
        Arguments.of(true, "O'Name"), // Apostrophes are valid
        // Accented and non-English characters are valid.
        // Please note, this is not an exhustive test - unicode supports thousands of such characters
        Arguments.of(true, "ÃëóûÿĉĐŗǥȕḍṦμϱнӯ"),
        Arguments.of(false, null), // Null is not valid
        Arguments.of(false, ""), // Empty string is not valid
        Arguments.of(false, " "), // Only whitespace is not valid
        Arguments.of(false, "a".repeat(256)), // Names exceeding the maximum length of 255 characters are not valid
        // Other special characters are not permitted
        Arguments.of(false, "Test!Name"),
        Arguments.of(false, "Test?Name"),
        Arguments.of(false, "Test#Name"),
        Arguments.of(false, "Test.Name"),
        Arguments.of(false, "Test,Name"),
        Arguments.of(false, "Test*Name"),
        Arguments.of(false, "Test<Name"),
        Arguments.of(false, "Test>Name"),
        Arguments.of(false, "Test:Name"),
        Arguments.of(false, "Test;Name"),
        Arguments.of(false, "Test/Name")
    );
  }

  @ParameterizedTest
  @MethodSource("emailsToTest")
  void emailValidation(boolean expectedIsValid, String email) {
    boolean actualIsValid = isEmailValid(email);
    assertEquals(expectedIsValid, actualIsValid);
  }

  private static Stream<Arguments> emailsToTest() {
    return Stream.of(
        Arguments.of(true, "testemail@test.com"), // Standard email format is valid
        // Alternate pattern for twitter|google|facebook is valid
        Arguments.of(true, "testemail-twitter"),
        Arguments.of(true, "testemail-google"),
        Arguments.of(true, "testemail-facebook"),
        Arguments.of(true, "test!#$%&'+-=?^_`.{|}~email@test.com"), // Some special characters are permitted
        Arguments.of(true, "testemail@test.co.uk"), // Multiple seperated .s are valid
        Arguments.of(false, null), // Null is not valid
        Arguments.of(false, ""), // Empty string is not valid
        Arguments.of(false, " "), // Only whitespace is not valid
        Arguments.of(false, "test.email@testcom"), // Email must have at least one . after the @
        Arguments.of(false, "testemailtest.com"), // Standard email must include an @
        Arguments.of(false, "testemail@test."), // Email must have be at least character after the last .
        Arguments.of(false, "testemail@.com"), // Email must have at least one character between the @ and the last .
        Arguments.of(false, "testemail@test..com"), // Email cannot contain consecutive full stops
        // Other special characters are not permitted
        Arguments.of(false, "test\"email@test.com"),
        Arguments.of(false, "test(email@test.com"),
        Arguments.of(false, "test)email@test.com"),
        Arguments.of(false, "test*email@test.com"),
        Arguments.of(false, "test/email@test.com"),
        Arguments.of(false, "test<email@test.com"),
        Arguments.of(false, "test>email@test.com"),
        Arguments.of(false, "test:email@test.com"),
        Arguments.of(false, "test;email@test.com"),
        // Other alternate endings are not permitted
        Arguments.of(false, "testemail-example"),
        Arguments.of(false, "testemail-twittter")
    );
  }

  @Test
  void updateTeacherPendingFlag_success() throws SegueDatabaseException, NoUserException {
    RegisteredUser initialUserState = new RegisteredUser();
    initialUserState.setId(1L);
    initialUserState.setTeacherPending(false);

    RegisteredUser expectedUserState = new RegisteredUser();
    expectedUserState.setId(1L);
    expectedUserState.setTeacherPending(true);

    expect(database.getById(1L)).andReturn(initialUserState);
    expect(database.createOrUpdateUser(expectedUserState)).andStubReturn(expectedUserState);
    replay(database);

    userAccountManager.updateTeacherPendingFlag(1L, true);

    verify(database);
  }

  @Test
  void updateTeacherPendingFlag_missingUser() throws SegueDatabaseException {
    expect(database.getById(1L)).andReturn(null);
    replay(database);

    assertThrows(NoUserException.class, () -> userAccountManager.updateTeacherPendingFlag(1L, true));

    verify(database);
  }

  @Test
  void sendRoleChangeRequestEmail_success()
      throws UnableToIndexSchoolsException, ContentManagerException, IOException, SegueDatabaseException,
      MissingRequiredFieldException {
    School school = prepareSchoolWithUrn();
    expect(schoolListReader.findSchoolById("1")).andReturn(school);
    replay(schoolListReader);

    Map<String, Object> expectedEmailDetails = Map.of(
        "contactGivenName", "GivenName",
        "contactFamilyName", "FamilyName",
        "contactUserId", 1L,
        "contactUserRole", STUDENT,
        "contactEmail", "test@test.com",
        "contactSubject", "Teacher Account Request",
        "contactMessage", "Hello,\n<br>\n<br>"
            + "Please could you convert my Isaac account into a teacher account.\n<br>\n<br>"
            + "My school is: SchoolName, Postcode\n<br>"
            + "A link to my school website with a staff list showing my name and email"
            + " (or a phone number to contact the school) is: school staff url\n<br>\n<br>\n<br>"
            + "Any other information: more information\n<br>\n<br>"
            + "Thanks, \n<br>\n<br>GivenName FamilyName",
        "replyToName", "GivenName FamilyName"
    );
    emailManager.sendContactUsFormEmail("admin@localhost", expectedEmailDetails);
    expectLastCall();
    replay(emailManager);

    HttpServletRequest request = createNiceMock(HttpServletRequest.class);
    replay(request);
    RegisteredUserDTO user = prepareRegisteredUserDtoWithDetails();
    Map<String, String> requestDetails = Map.of(
        "verificationDetails", "school staff url",
        "otherDetails", "more information"
    );

    userAccountManager.sendRoleChangeRequestEmail(request, user, TEACHER, requestDetails);

    verify(schoolListReader);
    verify(emailManager);
  }

  @Test
  void sendRoleChangeRequestEmail_successNoOtherDetails()
      throws UnableToIndexSchoolsException, ContentManagerException, IOException, SegueDatabaseException,
      MissingRequiredFieldException {
    School school = prepareSchoolWithUrn();
    expect(schoolListReader.findSchoolById("1")).andReturn(school);
    replay(schoolListReader);

    Map<String, Object> expectedEmailDetails = Map.of(
        "contactGivenName", "GivenName",
        "contactFamilyName", "FamilyName",
        "contactUserId", 1L,
        "contactUserRole", STUDENT,
        "contactEmail", "test@test.com",
        "contactSubject", "Teacher Account Request",
        "contactMessage", "Hello,\n<br>\n<br>"
            + "Please could you convert my Isaac account into a teacher account.\n<br>\n<br>"
            + "My school is: SchoolName, Postcode\n<br>"
            + "A link to my school website with a staff list showing my name and email"
            + " (or a phone number to contact the school) is: school staff url\n<br>\n<br>\n<br>"
            + "Thanks, \n<br>\n<br>GivenName FamilyName",
        "replyToName", "GivenName FamilyName"
    );
    emailManager.sendContactUsFormEmail("admin@localhost", expectedEmailDetails);
    expectLastCall();
    replay(emailManager);

    HttpServletRequest request = createNiceMock(HttpServletRequest.class);
    replay(request);
    RegisteredUserDTO user = prepareRegisteredUserDtoWithDetails();
    Map<String, String> requestDetails = Map.of(
        "verificationDetails", "school staff url"
    );

    userAccountManager.sendRoleChangeRequestEmail(request, user, TEACHER, requestDetails);

    verify(schoolListReader);
    verify(emailManager);
  }

  @Test
  void sendRoleChangeRequestEmail_missingSchool()
      throws UnableToIndexSchoolsException, SegueSearchException, IOException {
    expect(schoolListReader.findSchoolById("1")).andReturn(null);
    replay(schoolListReader);

    HttpServletRequest request = createNiceMock(HttpServletRequest.class);
    replay(request);
    RegisteredUserDTO user = prepareRegisteredUserDtoWithDetails();
    Map<String, String> requestDetails = Map.of(
        "verificationDetails", "school staff url",
        "otherDetails", "more information"
    );

    assertThrows(MissingRequiredFieldException.class,
        () -> userAccountManager.sendRoleChangeRequestEmail(request, user, TEACHER, requestDetails));

    verify(schoolListReader);
  }

  @ParameterizedTest(name = "{index} {0}")
  @MethodSource("sendRoleChangeRequestEmail_invalidVerificationDetails")
  void sendRoleChangeRequestEmail_invalidVerificationDetails(String ignoredTestLabel, Map<String, String> requestDetails)
      throws UnableToIndexSchoolsException, SegueSearchException, IOException {
    School school = prepareSchoolWithUrn();
    expect(schoolListReader.findSchoolById("1")).andReturn(school);
    replay(schoolListReader);

    HttpServletRequest request = createNiceMock(HttpServletRequest.class);
    replay(request);
    RegisteredUserDTO user = prepareRegisteredUserDtoWithDetails();

    assertThrows(MissingRequiredFieldException.class,
        () -> userAccountManager.sendRoleChangeRequestEmail(request, user, TEACHER, requestDetails));

    verify(schoolListReader);
  }

  private static Stream<Arguments> sendRoleChangeRequestEmail_invalidVerificationDetails() {
    return Stream.of(
        Arguments.of("missingVerificationDetails", Map.of(
            "otherDetails", "more information"
        )),
        Arguments.of("nullVerificationDetails", new HashMap<>() {
              {
                put("verificationDetails", null);
                put("otherDetails", "more information");
              }
            }),
        Arguments.of("emptyVerificationDetails", Map.of(
                "verificationDetails", "",
            "otherDetails", "more information"
        ))
    );
  }

  private static RegisteredUserDTO prepareRegisteredUserDtoWithDetails() {
    RegisteredUserDTO user = new RegisteredUserDTO();
    user.setId(1L);
    user.setSchoolId("1");
    user.setGivenName("GivenName");
    user.setFamilyName("FamilyName");
    user.setEmail("test@test.com");
    user.setRole(STUDENT);
    return user;
  }

  private static School prepareSchoolWithUrn() {
    School school = new School();
    school.setUrn("1");
    school.setName("SchoolName");
    school.setPostcode("Postcode");
    return school;
  }

  @Test
  void getSchoolNameWithPostcode_validUrn()
      throws UnableToIndexSchoolsException, SegueSearchException, IOException {
    School school = prepareSchoolWithUrn();
    expect(schoolListReader.findSchoolById("1")).andReturn(school);
    replay(schoolListReader);

    RegisteredUserDTO user = prepareRegisteredUserDtoWithUrn();

    String result = userAccountManager.getSchoolNameWithPostcode(user);

    assertEquals("SchoolName, Postcode", result);

    verify(schoolListReader);
  }

  @Test
  void getSchoolNameWithPostcode_unknownUrn()
      throws UnableToIndexSchoolsException, SegueSearchException, IOException {
    expect(schoolListReader.findSchoolById("1")).andReturn(null);
    replay(schoolListReader);

    RegisteredUserDTO user = prepareRegisteredUserDtoWithUrn();

    String result = userAccountManager.getSchoolNameWithPostcode(user);

    assertNull(result);

    verify(schoolListReader);
  }

  @Test
  void getSchoolNameWithPostcode_errorOnUrn()
      throws UnableToIndexSchoolsException, SegueSearchException, IOException {
    expect(schoolListReader.findSchoolById("1")).andThrow(new SegueSearchException("Error"));
    replay(schoolListReader);

    RegisteredUserDTO user = prepareRegisteredUserDtoWithUrn();

    String result = userAccountManager.getSchoolNameWithPostcode(user);

    assertNull(result);

    verify(schoolListReader);
  }

  private static RegisteredUserDTO prepareRegisteredUserDtoWithUrn() {
    RegisteredUserDTO user = new RegisteredUserDTO();
    user.setId(1L);
    user.setSchoolId("1");
    return user;
  }

  @ParameterizedTest
  @MethodSource("getSchoolNameWithPostcode_urnlessUsers")
  void getSchoolNameWithPostcode_urnlessUsers(String expectedResult, RegisteredUserDTO user) {
    replay(schoolListReader);

    String result = userAccountManager.getSchoolNameWithPostcode(user);

    assertEquals(expectedResult, result);

    // Confirm no unexpected methods have been called on the SchoolListReader
    verify(schoolListReader);
  }

  private static Stream<Arguments> getSchoolNameWithPostcode_urnlessUsers() {
    return Stream.of(
        Arguments.of("Also a school", prepareRegisteredUserDtoWithSchoolOther(null, "Also a school")),
        Arguments.of("Also a school", prepareRegisteredUserDtoWithSchoolOther("", "Also a school")),
        Arguments.of(null, prepareRegisteredUserDtoWithSchoolOther(null, null)),
        Arguments.of(null, prepareRegisteredUserDtoWithSchoolOther("", null)),
        Arguments.of(null, prepareRegisteredUserDtoWithSchoolOther(null, "")),
        Arguments.of(null, prepareRegisteredUserDtoWithSchoolOther("", ""))
    );
  }

  private static RegisteredUserDTO prepareRegisteredUserDtoWithSchoolOther(String schoolUrn, String schoolOther) {
    RegisteredUserDTO user = new RegisteredUserDTO();
    user.setId(1L);
    user.setSchoolId(schoolUrn);
    user.setSchoolOther(schoolOther);
    return user;
  }
}
