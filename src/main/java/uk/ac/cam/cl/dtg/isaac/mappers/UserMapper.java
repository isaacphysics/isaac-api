package uk.ac.cam.cl.dtg.isaac.mappers;

import org.mapstruct.BeanMapping;
import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.SubclassExhaustiveStrategy;
import org.mapstruct.SubclassMapping;
import org.mapstruct.factory.Mappers;
import uk.ac.cam.cl.dtg.isaac.dos.GroupMembership;
import uk.ac.cam.cl.dtg.isaac.dos.UserGroup;
import uk.ac.cam.cl.dtg.isaac.dos.users.AbstractSegueUser;
import uk.ac.cam.cl.dtg.isaac.dos.users.AnonymousUser;
import uk.ac.cam.cl.dtg.isaac.dos.users.RegisteredUser;
import uk.ac.cam.cl.dtg.isaac.dos.users.UserAuthenticationSettings;
import uk.ac.cam.cl.dtg.isaac.dos.users.UserFromAuthProvider;
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

@Mapper(subclassExhaustiveStrategy = SubclassExhaustiveStrategy.RUNTIME_EXCEPTION, collectionMappingStrategy = CollectionMappingStrategy.TARGET_IMMUTABLE)
public interface UserMapper {

  UserMapper INSTANCE = Mappers.getMapper(UserMapper.class);

  // DO <-> DTO Mappings
  @InheritInverseConfiguration
  AbstractSegueUser map(AbstractSegueUserDTO source);

  @SubclassMapping(source = RegisteredUser.class, target = RegisteredUserDTO.class)
  @SubclassMapping(source = UserAuthenticationSettings.class, target = UserAuthenticationSettingsDTO.class)
  @SubclassMapping(source = AnonymousUser.class, target = AnonymousUserDTO.class)
  AbstractSegueUserDTO map(AbstractSegueUser source);

  @Mapping(target = "emailVerificationToken", ignore = true)
  @Mapping(target = "emailToVerify", ignore = true)
  RegisteredUser map(RegisteredUserDTO source);

  @Mapping(target = "firstLogin", ignore = true)
  RegisteredUserDTO map(RegisteredUser source);

  UserAuthenticationSettings map(UserAuthenticationSettingsDTO source);

  UserAuthenticationSettingsDTO map(UserAuthenticationSettings source);

  AnonymousUser map(AnonymousUserDTO source);

  AnonymousUserDTO map(AnonymousUser source);

  GroupMembership map(GroupMembershipDTO source);

  GroupMembershipDTO map(GroupMembership source);

  @Mapping(target = "status", ignore = true)
  UserGroup map(UserGroupDTO source);

  @Mapping(target = "token", ignore = true)
  @Mapping(target = "ownerSummary", ignore = true)
  @Mapping(target = "mongoId", ignore = true)
  @Mapping(target = "additionalManagers", ignore = true)
  @Mapping(target = "additionalManagersUserIds", ignore = true) // Derived value
  UserGroupDTO map(UserGroup source);

  // Handling classes with multiple mapping targets
  @SuppressWarnings("unchecked")
  default <T extends UserSummaryDTO> T map(RegisteredUserDTO source, Class<T> targetClass) {
    if (targetClass.equals(UserSummaryForAdminUsersDTO.class)) {
      return (T) mapUserToAdminSummaryDTO(source);
    } else if (targetClass.equals(UserSummaryWithEmailAddressAndGenderDTO.class)) {
      return (T) mapUserToSummaryWithEmailAndGenderDTO(source);
    } else if (targetClass.equals(UserSummaryWithEmailAddressDTO.class)) {
      return (T) mapUserToSummaryWithEmailDTO(source);
    } else if (targetClass.equals(UserSummaryWithGroupMembershipDTO.class)) {
      return (T) mapUserToSummaryWithGroupMembershipDTO(source);
    } else if (targetClass.equals(UserSummaryDTO.class)) {
      return (T) mapUserToSummary(source);
    } else {
      throw new UnimplementedMappingException(RegisteredUserDTO.class, targetClass);
    }
  }

  @SuppressWarnings("unchecked")
  default <T> T map(UserFromAuthProvider source, Class<T> targetClass) {
    if (targetClass.equals(RegisteredUser.class)) {
      return (T) mapUserFromAuthProviderToRegisteredUser(source);
    } else {
      throw new UnimplementedMappingException(UserFromAuthProvider.class, targetClass);
    }
  }

  @SuppressWarnings("unchecked")
  default <T> T map(UserSummaryDTO source, Class<T> targetClass) {
    if (targetClass.equals(UserSummaryWithGroupMembershipDTO.class)) {
      return (T) mapUserSummaryDTOtoUserSummaryWithGroupMembershipDTO(source);
    } else if (targetClass.equals(UserSummaryDTO.class)) {
      return (T) mapExtendedUserSummaryDTOtoBaseUserSummaryDTO(source);
    } else {
      throw new UnimplementedMappingException(UserSummaryDTO.class, targetClass);
    }
  }

  // Mapping an object to a new instance of the same class
  RegisteredUser copy(RegisteredUser source);

  RegisteredUserDTO copy(RegisteredUserDTO source);

  GroupMembership copy(GroupMembership source);

  GroupMembershipDTO copy(GroupMembershipDTO source);

  @Named("copyUserSummaryDTO")
  @SubclassMapping(source = UserSummaryForAdminUsersDTO.class, target = UserSummaryForAdminUsersDTO.class)
  @SubclassMapping(source = UserSummaryWithEmailAddressAndGenderDTO.class,
      target = UserSummaryWithEmailAddressAndGenderDTO.class)
  @SubclassMapping(source = UserSummaryWithEmailAddressDTO.class, target = UserSummaryWithEmailAddressDTO.class)
  @SubclassMapping(source = UserSummaryWithGroupMembershipDTO.class, target = UserSummaryWithGroupMembershipDTO.class)
  UserSummaryDTO copy(UserSummaryDTO source);

  UserSummaryWithEmailAddressAndGenderDTO copy(UserSummaryWithEmailAddressAndGenderDTO source);

  UserSummaryWithEmailAddressDTO copy(UserSummaryWithEmailAddressDTO source);

  UserSummaryWithGroupMembershipDTO copy(UserSummaryWithGroupMembershipDTO source);

  UserSummaryForAdminUsersDTO copy(UserSummaryForAdminUsersDTO source);

  // Mapping to an existing target object, without overwriting any target properties where the source would be null
  @Mapping(target = "emailVerificationToken", ignore = true)
  @Mapping(target = "emailToVerify", ignore = true)
  @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
      nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
  void merge(RegisteredUserDTO source, @MappingTarget RegisteredUser target);

  // Specific mappings for use by above mappers
  @Mapping(target = "authorisedFullAccess", ignore = true)
  UserSummaryDTO mapUserToSummary(RegisteredUserDTO source);

  @Mapping(target = "authorisedFullAccess", ignore = true)
  UserSummaryForAdminUsersDTO mapUserToAdminSummaryDTO(RegisteredUserDTO source);

  @Mapping(target = "authorisedFullAccess", ignore = true)
  UserSummaryWithEmailAddressAndGenderDTO mapUserToSummaryWithEmailAndGenderDTO(RegisteredUserDTO source);

  @Mapping(target = "authorisedFullAccess", ignore = true)
  UserSummaryWithEmailAddressDTO mapUserToSummaryWithEmailDTO(RegisteredUserDTO source);

  @Mapping(target = "groupMembershipInformation", ignore = true)
  @Mapping(target = "authorisedFullAccess", ignore = true)
  UserSummaryWithGroupMembershipDTO mapUserToSummaryWithGroupMembershipDTO(RegisteredUserDTO source);

  @Mapping(target = "groupMembershipInformation", ignore = true)
  @SubclassMapping(source = UserSummaryForAdminUsersDTO.class, target = UserSummaryWithGroupMembershipDTO.class)
  @SubclassMapping(source = UserSummaryWithEmailAddressAndGenderDTO.class, target = UserSummaryWithGroupMembershipDTO.class)
  @SubclassMapping(source = UserSummaryWithEmailAddressDTO.class, target = UserSummaryWithGroupMembershipDTO.class)
  @SubclassMapping(source = UserSummaryWithGroupMembershipDTO.class, target = UserSummaryWithGroupMembershipDTO.class)
  UserSummaryWithGroupMembershipDTO mapUserSummaryDTOtoUserSummaryWithGroupMembershipDTO(UserSummaryDTO source);

  @Named("censorUserSummaryDTO")
  @BeanMapping(resultType = UserSummaryDTO.class)
  UserSummaryDTO mapExtendedUserSummaryDTOtoBaseUserSummaryDTO(UserSummaryDTO source);

  @Mapping(target = "teacherPending", ignore = true)
  @Mapping(target = "schoolOther", ignore = true)
  @Mapping(target = "schoolId", ignore = true)
  @Mapping(target = "role", ignore = true)
  @Mapping(target = "registrationDate", ignore = true)
  @Mapping(target = "registeredContextsLastConfirmed", ignore = true)
  @Mapping(target = "registeredContexts", ignore = true)
  @Mapping(target = "lastUpdated", ignore = true)
  @Mapping(target = "privacyPolicyAcceptedTime", ignore = true)
  @Mapping(target = "lastSeen", ignore = true)
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "emailVerificationToken", ignore = true)
  @Mapping(target = "emailToVerify", ignore = true)
  RegisteredUser mapUserFromAuthProviderToRegisteredUser(UserFromAuthProvider source);
}
