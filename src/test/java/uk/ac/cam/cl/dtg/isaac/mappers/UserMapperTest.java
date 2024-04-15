package uk.ac.cam.cl.dtg.isaac.mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static uk.ac.cam.cl.dtg.CustomAssertions.assertDeepEquals;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.ac.cam.cl.dtg.isaac.dos.ExamBoard;
import uk.ac.cam.cl.dtg.isaac.dos.GroupMembership;
import uk.ac.cam.cl.dtg.isaac.dos.GroupMembershipStatus;
import uk.ac.cam.cl.dtg.isaac.dos.GroupStatus;
import uk.ac.cam.cl.dtg.isaac.dos.Stage;
import uk.ac.cam.cl.dtg.isaac.dos.UserGroup;
import uk.ac.cam.cl.dtg.isaac.dos.users.AbstractSegueUser;
import uk.ac.cam.cl.dtg.isaac.dos.users.AnonymousUser;
import uk.ac.cam.cl.dtg.isaac.dos.users.EmailVerificationStatus;
import uk.ac.cam.cl.dtg.isaac.dos.users.Gender;
import uk.ac.cam.cl.dtg.isaac.dos.users.RegisteredUser;
import uk.ac.cam.cl.dtg.isaac.dos.users.Role;
import uk.ac.cam.cl.dtg.isaac.dos.users.UserAuthenticationSettings;
import uk.ac.cam.cl.dtg.isaac.dos.users.UserContext;
import uk.ac.cam.cl.dtg.isaac.dos.users.UserFromAuthProvider;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacPodDTO;
import uk.ac.cam.cl.dtg.isaac.dto.UserGroupDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.AbstractSegueUserDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.AnonymousUserDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.GroupMembershipDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.UserAuthenticationSettingsDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.UserSummaryDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.UserSummaryForAdminUsersDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.UserSummaryWithEmailAddressAndGenderDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.UserSummaryWithEmailAddressDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.UserSummaryWithGroupMembershipDTO;
import uk.ac.cam.cl.dtg.segue.auth.AuthenticationProvider;

class UserMapperTest {

  private UserMapper userMapper;

  private static final Instant testDate = Instant.now();
  private static final Instant newTestDate = Instant.now().plus(10000L, ChronoUnit.SECONDS);

  @BeforeEach
  void beforeEach() {
    userMapper = UserMapper.INSTANCE;
  }


  @ParameterizedTest
  @MethodSource("testCasesDOtoDTO")
  <S extends AbstractSegueUser, T extends AbstractSegueUserDTO> void mappingAbstractSegueUserDOReturnsExpectedDTO(
      S source, T expected) {
    AbstractSegueUserDTO actual = userMapper.map(source);
    assertEquals(expected.getClass(), actual.getClass());
    assertDeepEquals(expected, actual);
  }

  @ParameterizedTest
  @MethodSource("testCasesDTOtoDO")
  <S extends AbstractSegueUserDTO, T extends AbstractSegueUser> void mappingAbstractSegueUserDTOReturnsExpectedDO(
      S source, T expected) {
    AbstractSegueUser actual = userMapper.map(source);
    assertEquals(expected.getClass(), actual.getClass());
    assertDeepEquals(expected, actual);
  }

  @Test
  void mappingGroupMembershipDOReturnsExpectedDTO() {
    GroupMembership source = prepareGroupMembershipDO();
    GroupMembershipDTO expected = prepareGroupMembershipDTO();
    GroupMembershipDTO actual = userMapper.map(source);
    assertEquals(expected.getClass(), actual.getClass());
    assertDeepEquals(expected, actual);
  }

  @Test
  void mappingGroupMembershipDTOReturnsExpectedDO() {
    GroupMembershipDTO source = prepareGroupMembershipDTO();
    GroupMembership expected = prepareGroupMembershipDO();
    GroupMembership actual = userMapper.map(source);
    assertEquals(expected.getClass(), actual.getClass());
    assertDeepEquals(expected, actual);
  }

  @Test
  void mappingUserGroupDOReturnsExpectedDTO() {
    UserGroup source = prepareOriginalUserGroupDO();
    UserGroupDTO expected = prepareMappedUserGroupDTO();
    UserGroupDTO actual = userMapper.map(source);
    assertEquals(expected.getClass(), actual.getClass());
    assertDeepEquals(expected, actual);
  }

  @Test
  void mappingUserGroupDTOReturnsExpectedDO() {
    UserGroupDTO source = prepareOriginalUserGroupDTO();
    UserGroup expected = prepareMappedUserGroupDO();
    UserGroup actual = userMapper.map(source);
    assertEquals(expected.getClass(), actual.getClass());
    assertDeepEquals(expected, actual);
  }

  @ParameterizedTest
  @MethodSource("testCasesFromRegisteredUserDTO")
  <T extends UserSummaryDTO> void defaultMappingMethodFrom_RegisteredUserDTO_returnsRequestedClass(Class<T> targetClass, T expected) {
    RegisteredUserDTO source = prepareOriginalRegisteredUserDTO();
    T actual = userMapper.map(source, targetClass);
    assertEquals(targetClass, actual.getClass());
    assertDeepEquals(expected, actual);
  }

  @Test
  void mappingUserFromAuthProviderToRegisteredUserReturnsExpectedObject() {
    UserFromAuthProvider source = prepareUserFromAuthProvider();
    RegisteredUser expected = prepareRegisteredUserDOFromUserFromAuthProvider();
    RegisteredUser actual = userMapper.map(source, RegisteredUser.class);
    assertEquals(RegisteredUser.class, actual.getClass());
    assertDeepEquals(expected, actual);
  }

  @Test
  void defaultMappingMethodFrom_UserFromAuthProvider_throwsUnimplementedMappingExceptionForUnexpectedTarget() {
    UserFromAuthProvider source = prepareUserFromAuthProvider();
    Exception exception = assertThrows(UnimplementedMappingException.class, () -> userMapper.map(source, IsaacPodDTO.class));
    assertEquals("Invocation of unimplemented mapping from UserFromAuthProvider to IsaacPodDTO", exception.getMessage());
  }

  @ParameterizedTest
  @MethodSource("testCasesFromUserSummaryDTO")
  <S extends UserSummaryDTO, T> void defaultMappingMethodFrom_UserSummaryDTO_returnsExpectedClass(S source, Class<T> targetClass, T expected) {
    T actual = userMapper.map(source, targetClass);
    assertEquals(targetClass, actual.getClass());
    assertDeepEquals(expected, actual);
  }

  @Test
  void defaultMappingMethodFrom_UserSummaryDTO_throwsUnimplementedMappingExceptionForUnexpectedTarget() {
    UserSummaryDTO source = new UserSummaryDTO();
    Exception exception = assertThrows(UnimplementedMappingException.class, () -> userMapper.map(source, IsaacPodDTO.class));
    assertEquals("Invocation of unimplemented mapping from UserSummaryDTO to IsaacPodDTO", exception.getMessage());
  }

  @Test
  void copyRegisteredUserDOReturnsNewObjectWithSameProperties() {
    RegisteredUser source = prepareOriginalRegisteredUserDO();
    RegisteredUser actual = userMapper.copy(source);
    assertEquals(source.getClass(), actual.getClass());
    assertNotSame(source, actual);
    assertDeepEquals(source, actual);
  }

  @Test
  void copyRegisteredUserDTOReturnsNewObjectWithSameProperties() {
    RegisteredUserDTO source = prepareOriginalRegisteredUserDTO();
    RegisteredUserDTO actual = userMapper.copy(source);
    assertEquals(source.getClass(), actual.getClass());
    assertNotSame(source, actual);
    assertDeepEquals(source, actual);
  }

  @Test
  void copyGroupMembershipDOReturnsNewObjectWithSameProperties() {
    GroupMembership source = prepareGroupMembershipDO();
    GroupMembership actual = userMapper.copy(source);
    assertEquals(source.getClass(), actual.getClass());
    assertNotSame(source, actual);
    assertDeepEquals(source, actual);
  }

  @Test
  void copyGroupMembershipDTOReturnsNewObjectWithSameProperties() {
    GroupMembershipDTO source = prepareGroupMembershipDTO();
    GroupMembershipDTO actual = userMapper.copy(source);
    assertEquals(source.getClass(), actual.getClass());
    assertNotSame(source, actual);
    assertDeepEquals(source, actual);
  }

  @ParameterizedTest
  @MethodSource("testCasesCopyUserSummaryDTO")
  void copyContentDTOReturnsNewObjectWithSameProperties(UserSummaryDTO source) {
    UserSummaryDTO actual = userMapper.copy(source);
    assertEquals(source.getClass(), actual.getClass());
    assertNotSame(source, actual);
    assertDeepEquals(source, actual);
  }

  @Test
  void mergeRegisteredUserDTOtoRegisteredUserShouldModifyTargetAsExpected() {
    RegisteredUserDTO source = prepareMergeSourceRegisteredUserDTO();
    RegisteredUser target = prepareOriginalRegisteredUserDO();
    RegisteredUser expected = prepareMergeExpectedRegisteredUserDO();
    userMapper.merge(source, target);
    assertDeepEquals(expected, target);
  }

  @Test
  void mergeRegisteredUserDTOtoRegisteredUserShouldIgnoreNullProperties() {
    RegisteredUserDTO source = prepareMergeNullSourceRegisteredUserDTO();
    RegisteredUser target = prepareOriginalRegisteredUserDO();
    RegisteredUser expected = prepareOriginalRegisteredUserDO();
    userMapper.merge(source, target);
    assertDeepEquals(expected, target);
  }

  private static Stream<Arguments> testCasesDOtoDTO() {
    return Stream.of(
        Arguments.of(prepareOriginalRegisteredUserDO(), prepareMappedRegisteredUserDTO()),
        Arguments.of(prepareUserAuthenticationSettingsDO(), prepareUserAuthenticationSettingsDTO()),
        Arguments.of(prepareAnonymousUserDO(), prepareAnonymousUserDTO())
    );
  }

  private static Stream<Arguments> testCasesDTOtoDO() {
    return Stream.of(
        Arguments.of(prepareOriginalRegisteredUserDTO(), prepareMappedRegisteredUserDO()),
        Arguments.of(prepareUserAuthenticationSettingsDTO(), prepareUserAuthenticationSettingsDO()),
        Arguments.of(prepareAnonymousUserDTO(), prepareAnonymousUserDO())
    );
  }

  private static Stream<Arguments> testCasesFromRegisteredUserDTO() {
    return Stream.of(
        Arguments.of(UserSummaryForAdminUsersDTO.class, prepareUserSummaryForAdminUsersDTOFromRegisteredUserDTO()),
        Arguments.of(UserSummaryWithEmailAddressAndGenderDTO.class, prepareUserSummaryWithEmailAddressAndGenderDTOFromRegisteredUserDTO()),
        Arguments.of(UserSummaryWithEmailAddressDTO.class, prepareUserSummaryWithEmailAddressDTOFromRegisteredUserDTO()),
        Arguments.of(UserSummaryWithGroupMembershipDTO.class, prepareUserSummaryWithGroupMembershipDTOFromRegisteredUserDTO()),
        Arguments.of(UserSummaryDTO.class, prepareUserSummaryDTOFromRegisteredUserDTO())
    );
  }

  private static Stream<Arguments> testCasesFromUserSummaryDTO() {
    return Stream.of(
        Arguments.of(prepareUserSummaryDTO(), UserSummaryDTO.class, prepareUserSummaryDTO()),
        Arguments.of(prepareUserSummaryWithEmailAddressDTO(), UserSummaryDTO.class, prepareUserSummaryDTO()),
        Arguments.of(prepareUserSummaryWithEmailAddressAndGenderDTO(), UserSummaryDTO.class, prepareUserSummaryDTO()),
        Arguments.of(prepareUserSummaryWithGroupMembershipDTO(), UserSummaryDTO.class, prepareUserSummaryDTO()),
        Arguments.of(prepareUserSummaryForAdminUsersDTO(), UserSummaryDTO.class, prepareUserSummaryDTO()),
        Arguments.of(prepareUserSummaryDTO(), UserSummaryWithGroupMembershipDTO.class, prepareUserSummaryWithGroupMembershipDTOFromUserSummaryDTO()),
        Arguments.of(prepareUserSummaryWithEmailAddressDTO(), UserSummaryWithGroupMembershipDTO.class, prepareUserSummaryWithGroupMembershipDTOFromUserSummaryDTO()),
        Arguments.of(prepareUserSummaryWithEmailAddressAndGenderDTO(), UserSummaryWithGroupMembershipDTO.class, prepareUserSummaryWithGroupMembershipDTOFromUserSummaryDTO()),
        Arguments.of(prepareUserSummaryWithGroupMembershipDTO(), UserSummaryWithGroupMembershipDTO.class, prepareUserSummaryWithGroupMembershipDTO()),
        Arguments.of(prepareUserSummaryForAdminUsersDTO(), UserSummaryWithGroupMembershipDTO.class, prepareUserSummaryWithGroupMembershipDTOFromUserSummaryDTO())
    );
  }

  private static Stream<Arguments> testCasesCopyUserSummaryDTO() {
    return Stream.of(
        Arguments.of(prepareUserSummaryDTO()),
        Arguments.of(prepareUserSummaryWithEmailAddressDTO()),
        Arguments.of(prepareUserSummaryWithEmailAddressAndGenderDTO()),
        Arguments.of(prepareUserSummaryWithGroupMembershipDTO()),
        Arguments.of(prepareUserSummaryForAdminUsersDTO())
    );
  }

  private static RegisteredUser prepareOriginalRegisteredUserDO() {
    RegisteredUser object = prepareRegisteredUserDO(new RegisteredUser());
    object.setEmailToVerify("emailToVerify");
    object.setEmailVerificationToken("verificationToken");
    return object;
  }

  private static RegisteredUser prepareMappedRegisteredUserDO() {
    RegisteredUser object = prepareRegisteredUserDO(new RegisteredUser());
    object.setEmailToVerify(null);
    object.setEmailVerificationToken(null);
    return object;
  }

  private static RegisteredUser prepareRegisteredUserDO(RegisteredUser object) {
    UserContext userContext = new UserContext();
    userContext.setStage(Stage.a_level);
    userContext.setExamBoard(ExamBoard.aqa);

    object.setId(2L);
    object.setGivenName("givenName");
    object.setFamilyName("familyName");
    object.setEmail("email");
    object.setRole(Role.TEACHER);
    object.setDateOfBirth(testDate);
    object.setGender(Gender.MALE);
    object.setRegistrationDate(testDate);
    object.setSchoolId("schoolId");
    object.setSchoolOther("schoolOther");
    object.setRegisteredContexts(List.of(userContext));
    object.setRegisteredContextsLastConfirmed(testDate);
    object.setEmailVerificationStatus(EmailVerificationStatus.VERIFIED);
    object.setTeacherPending(true);
    object.setLastUpdated(testDate);
    object.setLastSeen(testDate);
    return object;
  }

  private static RegisteredUserDTO prepareOriginalRegisteredUserDTO() {
    RegisteredUserDTO object = prepareRegisteredUserDTO(new RegisteredUserDTO());
    object.setFirstLogin(true);
    return object;
  }

  private static RegisteredUserDTO prepareMappedRegisteredUserDTO() {
    RegisteredUserDTO object = prepareRegisteredUserDTO(new RegisteredUserDTO());
    object.setFirstLogin(false);
    return object;
  }

  private static RegisteredUserDTO prepareRegisteredUserDTO(RegisteredUserDTO object) {
    UserContext userContext = new UserContext();
    userContext.setStage(Stage.a_level);
    userContext.setExamBoard(ExamBoard.aqa);

    object.setId(2L);
    object.setGivenName("givenName");
    object.setFamilyName("familyName");
    object.setEmail("email");
    object.setRole(Role.TEACHER);
    object.setDateOfBirth(testDate);
    object.setGender(Gender.MALE);
    object.setRegistrationDate(testDate);
    object.setSchoolId("schoolId");
    object.setSchoolOther("schoolOther");
    object.setRegisteredContexts(List.of(userContext));
    object.setRegisteredContextsLastConfirmed(testDate);
    object.setLastUpdated(testDate);
    object.setLastSeen(testDate);
    object.setEmailVerificationStatus(EmailVerificationStatus.VERIFIED);
    object.setTeacherPending(true);
    return object;
  }

  private static UserAuthenticationSettings prepareUserAuthenticationSettingsDO() {
    return new UserAuthenticationSettings(2L, List.of(AuthenticationProvider.SEGUE, AuthenticationProvider.GOOGLE),
        true, true);
  }

  private static UserAuthenticationSettingsDTO prepareUserAuthenticationSettingsDTO() {
    return new UserAuthenticationSettingsDTO(2L, List.of(AuthenticationProvider.SEGUE, AuthenticationProvider.GOOGLE),
        true, true);
  }

  private static AnonymousUser prepareAnonymousUserDO() {
    AnonymousUser object = new AnonymousUser();
    object.setSessionId("sessionId");
    object.setDateCreated(testDate);
    object.setLastUpdated(testDate);
    return object;
  }

  private static AnonymousUserDTO prepareAnonymousUserDTO() {
    AnonymousUserDTO object = new AnonymousUserDTO();
    object.setSessionId("sessionId");
    object.setDateCreated(testDate);
    object.setLastUpdated(testDate);
    return object;
  }

  private static GroupMembership prepareGroupMembershipDO() {
    GroupMembership object = new GroupMembership();
    object.setGroupId(3L);
    object.setUserId(7L);
    object.setStatus(GroupMembershipStatus.ACTIVE);
    object.setUpdated(testDate);
    object.setCreated(testDate);
    return object;
  }

  private static GroupMembershipDTO prepareGroupMembershipDTO() {
    GroupMembershipDTO object = new GroupMembershipDTO();
    object.setGroupId(3L);
    object.setUserId(7L);
    object.setStatus(GroupMembershipStatus.ACTIVE);
    object.setUpdated(testDate);
    object.setCreated(testDate);
    return object;
  }

  private static UserGroup prepareOriginalUserGroupDO() {
    UserGroup object = prepareUserGroupDO(new UserGroup());
    object.setStatus(GroupStatus.ACTIVE);
    return object;
  }

  private static UserGroup prepareMappedUserGroupDO() {
    UserGroup object = prepareUserGroupDO(new UserGroup());
    object.setStatus(null);
    return object;
  }

  private static UserGroup prepareUserGroupDO(UserGroup object) {
    object.setId(3L);
    object.setGroupName("groupName");
    object.setOwnerId(5L);

    object.setCreated(testDate);
    object.setArchived(false);
    object.setAdditionalManagerPrivileges(true);
    object.setLastUpdated(testDate);
    return object;
  }

  private static UserGroupDTO prepareOriginalUserGroupDTO() {
    UserSummaryWithEmailAddressDTO ownerSummary = new UserSummaryWithEmailAddressDTO();
    ownerSummary.setId(3L);
    ownerSummary.setEmail("ownerEmail");

    UserSummaryWithEmailAddressDTO additionalManager = new UserSummaryWithEmailAddressDTO();
    ownerSummary.setId(8L);
    ownerSummary.setEmail("managerEmail");

    UserGroupDTO object = prepareUserGroupDTO(new UserGroupDTO());
    object.setToken("token");
    object.setOwnerSummary(ownerSummary);
    object.setAdditionalManagers(Set.of(additionalManager));
    return object;
  }

  private static UserGroupDTO prepareMappedUserGroupDTO() {
    UserGroupDTO object = prepareUserGroupDTO(new UserGroupDTO());
    object.setToken(null);
    object.setOwnerSummary(null);
    object.setAdditionalManagers(Set.of());
    return object;
  }

  private static UserGroupDTO prepareUserGroupDTO(UserGroupDTO object) {
    object.setId(3L);
    object.setGroupName("groupName");
    object.setOwnerId(5L);
    object.setCreated(testDate);
    object.setLastUpdated(testDate);
    object.setArchived(false);
    object.setAdditionalManagerPrivileges(true);
    return object;
  }

  private static UserSummaryDTO prepareUserSummaryDTO() {
    return setUserSummaryDTOCommonFields(new UserSummaryDTO());
  }

  private static UserSummaryWithEmailAddressDTO prepareUserSummaryWithEmailAddressDTO() {
    UserSummaryWithEmailAddressDTO object = setUserSummaryDTOCommonFields(new UserSummaryWithEmailAddressDTO());
    object.setEmail("email");
    return object;
  }

  private static UserSummaryWithEmailAddressAndGenderDTO prepareUserSummaryWithEmailAddressAndGenderDTO() {
    UserSummaryWithEmailAddressAndGenderDTO object = setUserSummaryDTOCommonFields(new UserSummaryWithEmailAddressAndGenderDTO());
    object.setEmail("email");
    object.setGender(Gender.PREFER_NOT_TO_SAY);
    return object;
  }

  private static UserSummaryWithGroupMembershipDTO prepareUserSummaryWithGroupMembershipDTO() {
    UserSummaryWithGroupMembershipDTO object = setUserSummaryDTOCommonFields(new UserSummaryWithGroupMembershipDTO());
    object.setGroupMembershipInformation(prepareGroupMembershipDTO());
    return object;
  }

  private static UserSummaryForAdminUsersDTO prepareUserSummaryForAdminUsersDTO() {
    UserSummaryForAdminUsersDTO object = setUserSummaryDTOCommonFields(new UserSummaryForAdminUsersDTO());
    object.setEmail("email");
    object.setLastUpdated(testDate);
    object.setLastSeen(testDate);
    object.setRegistrationDate(testDate);
    object.setSchoolId("schoolId");
    object.setSchoolOther("schoolOther");
    return object;
  }

  private static <T extends UserSummaryDTO> T setUserSummaryDTOCommonFields(T object) {
    UserContext userContext = new UserContext();
    userContext.setStage(Stage.a_level);
    userContext.setExamBoard(ExamBoard.aqa);

    object.setId(2L);
    object.setGivenName("givenName");
    object.setFamilyName("familyName");
    object.setRole(Role.TEACHER);
    object.setAuthorisedFullAccess(true);
    object.setEmailVerificationStatus(EmailVerificationStatus.VERIFIED);
    object.setTeacherPending(true);
    object.setRegisteredContexts(List.of(userContext));
    return object;
  }

  private static RegisteredUserDTO prepareMergeSourceRegisteredUserDTO() {
    UserContext userContext1 = new UserContext();
    userContext1.setStage(Stage.gcse);
    userContext1.setExamBoard(ExamBoard.edexcel);
    UserContext userContext2 = new UserContext();
    userContext2.setStage(Stage.all);
    userContext2.setExamBoard(ExamBoard.ocr);

    RegisteredUserDTO object = new RegisteredUserDTO();
    object.setId(3L);
    object.setGivenName("newGivenName");
    object.setFamilyName("newFamilyName");
    object.setEmail("newEmail");
    object.setRole(Role.EVENT_MANAGER);
    object.setDateOfBirth(newTestDate);
    object.setGender(Gender.FEMALE);
    object.setRegistrationDate(newTestDate);
    object.setSchoolId("newSchoolId");
    object.setSchoolOther("newSchoolOther");
    object.setRegisteredContexts(List.of(userContext1, userContext2));
    object.setRegisteredContextsLastConfirmed(newTestDate);
    object.setLastUpdated(newTestDate);
    object.setLastSeen(newTestDate);
    object.setEmailVerificationStatus(EmailVerificationStatus.DELIVERY_FAILED);
    object.setTeacherPending(false);
    object.setFirstLogin(false);
    return object;
  }

  private static RegisteredUser prepareMergeExpectedRegisteredUserDO() {
    UserContext userContext1 = new UserContext();
    userContext1.setStage(Stage.gcse);
    userContext1.setExamBoard(ExamBoard.edexcel);
    UserContext userContext2 = new UserContext();
    userContext2.setStage(Stage.all);
    userContext2.setExamBoard(ExamBoard.ocr);

    RegisteredUser object = new RegisteredUser();
    object.setId(3L);
    object.setGivenName("newGivenName");
    object.setFamilyName("newFamilyName");
    object.setEmail("newEmail");
    object.setRole(Role.EVENT_MANAGER);
    object.setDateOfBirth(newTestDate);
    object.setGender(Gender.FEMALE);
    object.setRegistrationDate(newTestDate);
    object.setSchoolId("newSchoolId");
    object.setSchoolOther("newSchoolOther");
    object.setRegisteredContexts(List.of(userContext1, userContext2));
    object.setRegisteredContextsLastConfirmed(newTestDate);
    object.setLastUpdated(newTestDate);
    object.setLastSeen(newTestDate);
    object.setEmailVerificationStatus(EmailVerificationStatus.DELIVERY_FAILED);
    object.setTeacherPending(false);
    object.setEmailToVerify("emailToVerify");
    object.setEmailVerificationToken("verificationToken");
    return object;
  }

  private static RegisteredUserDTO prepareMergeNullSourceRegisteredUserDTO() {
    RegisteredUserDTO object = new RegisteredUserDTO();
    object.setId(null);
    object.setGivenName(null);
    object.setFamilyName(null);
    object.setEmail(null);
    object.setRole(null);
    object.setDateOfBirth(null);
    object.setGender(null);
    object.setRegistrationDate(null);
    object.setSchoolId(null);
    object.setSchoolOther(null);
    object.setRegisteredContexts(null);
    object.setRegisteredContextsLastConfirmed(null);
    object.setLastUpdated(null);
    object.setLastSeen(null);
    object.setEmailVerificationStatus(null);
    object.setTeacherPending(null);
    object.setFirstLogin(false);
    return object;
  }

  private static UserSummaryForAdminUsersDTO prepareUserSummaryForAdminUsersDTOFromRegisteredUserDTO() {
    UserSummaryForAdminUsersDTO object = prepareUserSummaryForAdminUsersDTO();
    object.setAuthorisedFullAccess(false);
    return object;
  }

  private static UserSummaryWithEmailAddressAndGenderDTO prepareUserSummaryWithEmailAddressAndGenderDTOFromRegisteredUserDTO() {
    UserSummaryWithEmailAddressAndGenderDTO object = prepareUserSummaryWithEmailAddressAndGenderDTO();
    object.setAuthorisedFullAccess(false);
    object.setGender(Gender.MALE);
    return object;
  }

  private static UserSummaryWithEmailAddressDTO prepareUserSummaryWithEmailAddressDTOFromRegisteredUserDTO() {
    UserSummaryWithEmailAddressDTO object = prepareUserSummaryWithEmailAddressDTO();
    object.setAuthorisedFullAccess(false);
    return object;
  }

  private static UserSummaryWithGroupMembershipDTO prepareUserSummaryWithGroupMembershipDTOFromRegisteredUserDTO() {
    UserSummaryWithGroupMembershipDTO object = prepareUserSummaryWithGroupMembershipDTO();
    object.setAuthorisedFullAccess(false);
    object.setGroupMembershipInformation(null);
    return object;
  }

  private static UserSummaryDTO prepareUserSummaryDTOFromRegisteredUserDTO() {
    UserSummaryDTO object = prepareUserSummaryDTO();
    object.setAuthorisedFullAccess(false);
    return object;
  }

  private static UserSummaryWithGroupMembershipDTO prepareUserSummaryWithGroupMembershipDTOFromUserSummaryDTO() {
    UserSummaryWithGroupMembershipDTO object = prepareUserSummaryWithGroupMembershipDTO();
    object.setGroupMembershipInformation(null);
    return object;
  }

  private static UserFromAuthProvider prepareUserFromAuthProvider() {
    return new UserFromAuthProvider("providerUserId", "givenName", "familyName", "email", EmailVerificationStatus.VERIFIED,
            testDate, Gender.MALE);
  }

  private static RegisteredUser prepareRegisteredUserDOFromUserFromAuthProvider() {
    RegisteredUser object = new RegisteredUser();
    object.setId(null);
    object.setGivenName("givenName");
    object.setFamilyName("familyName");
    object.setEmail("email");
    object.setRole(null);
    object.setDateOfBirth(testDate);
    object.setGender(Gender.MALE);
    object.setRegistrationDate(null);
    object.setSchoolId(null);
    object.setSchoolOther(null);
    object.setRegisteredContexts(null);
    object.setRegisteredContextsLastConfirmed(null);
    object.setEmailVerificationStatus(EmailVerificationStatus.VERIFIED);
    object.setTeacherPending(null);
    object.setLastUpdated(null);
    object.setLastSeen(null);
    return object;
  }
}