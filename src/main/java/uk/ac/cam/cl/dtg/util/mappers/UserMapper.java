package uk.ac.cam.cl.dtg.util.mappers;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.SubclassMapping;
import org.mapstruct.factory.Mappers;

import uk.ac.cam.cl.dtg.isaac.dos.GroupMembership;
import uk.ac.cam.cl.dtg.isaac.dos.UserGroup;
import uk.ac.cam.cl.dtg.isaac.dos.users.AnonymousUser;
import uk.ac.cam.cl.dtg.isaac.dos.users.RegisteredUser;
import uk.ac.cam.cl.dtg.isaac.dos.users.UserAuthenticationSettings;
import uk.ac.cam.cl.dtg.isaac.dos.users.UserFromAuthProvider;
import uk.ac.cam.cl.dtg.isaac.dto.UserGroupDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.AnonymousUserDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.GroupMembershipDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.UserAuthenticationSettingsDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.UserSummaryDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.UserSummaryForAdminUsersDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.UserSummaryWithEmailAddressDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.UserSummaryWithGroupMembershipDTO;

@Mapper
public interface UserMapper {
    UserMapper INSTANCE = Mappers.getMapper(UserMapper.class);

    @Mapping(target = "sessionToken", ignore = true)
    @Mapping(target = "emailVerificationToken", ignore = true)
    @Mapping(target = "emailToVerify", ignore = true)
    RegisteredUser map(RegisteredUserDTO source);

    @Mapping(target = "firstLogin", ignore = true)
    RegisteredUserDTO map(RegisteredUser source);
    UserAuthenticationSettingsDTO map(UserAuthenticationSettings source);
    AnonymousUserDTO map(AnonymousUser source);

    default <T extends UserSummaryDTO> T map(RegisteredUserDTO source, Class<T> targetClass) {
        if (targetClass.equals(UserSummaryDTO.class)) {
            return (T) mapUserToSummary(source);
        } else if (targetClass.equals(UserSummaryForAdminUsersDTO.class)) {
            return (T) mapUserToAdminSummaryDTO(source);
        } else if (targetClass.equals(UserSummaryWithEmailAddressDTO.class)) {
            return (T) mapUserToSummaryWithEmailDTO(source);
        } else if (targetClass.equals(UserSummaryWithGroupMembershipDTO.class)) {
            return (T) mapUserToSummaryWithGroupMembershipDTO(source);
        } else {
            throw new UnimplementedMappingException(RegisteredUserDTO.class, targetClass);
        }
    }

    default <T> T map(UserFromAuthProvider source, Class<T> targetClass) {
        if (targetClass.equals(RegisteredUser.class)) {
            return (T) mapUserFromAuthProviderToRegisteredUser(source);
        } else {
            throw new UnimplementedMappingException(UserFromAuthProvider.class, targetClass);
        }
    }

    default <T> T map(UserSummaryDTO source, Class<T> targetClass) {
        if (targetClass.equals(UserSummaryWithGroupMembershipDTO.class)) {
            return (T) mapUserSummaryDTOtoUserSummaryWithGroupMembershipDTO(source);
        } else if (targetClass.equals(UserSummaryDTO.class)) {
            return (T) mapExtendedUserSummaryDTOtoBaseUserSummaryDTO(source);
        } else {
            throw new UnimplementedMappingException(UserSummaryDTO.class, targetClass);
        }
    }

    @Mapping(target = "authorisedFullAccess", ignore = true)
    UserSummaryDTO mapUserToSummary(RegisteredUserDTO source);

    @Mapping(target = "token", ignore = true)
    @Mapping(target = "ownerSummary", ignore = true)
    @Mapping(target = "mongoId", ignore = true)
    @Mapping(target = "additionalManagersUserIds", ignore = true)
    @Mapping(target = "additionalManagers", ignore = true)
    UserGroupDTO map(UserGroup source);

    @Mapping(target = "authorisedFullAccess", ignore = true)
    UserSummaryForAdminUsersDTO mapUserToAdminSummaryDTO(RegisteredUserDTO source);

    @Mapping(target = "authorisedFullAccess", ignore = true)
    UserSummaryWithEmailAddressDTO mapUserToSummaryWithEmailDTO(RegisteredUserDTO source);

    GroupMembership map(GroupMembershipDTO source);

    GroupMembershipDTO map(GroupMembership source);

    @Mapping(target = "status", ignore = true)
    UserGroup map(UserGroupDTO source);

    @Mapping(target = "groupMembershipInformation", ignore = true)
    @Mapping(target = "authorisedFullAccess", ignore = true)
    UserSummaryWithGroupMembershipDTO mapUserToSummaryWithGroupMembershipDTO(RegisteredUserDTO source);

    RegisteredUser copy(RegisteredUser source);
    RegisteredUserDTO copy(RegisteredUserDTO source);

    GroupMembershipDTO copy(GroupMembershipDTO source);

    @Mapping(target = "sessionToken", ignore = true)
    @Mapping(target = "emailVerificationToken", ignore = true)
    @Mapping(target = "emailToVerify", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
            nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
    void merge(RegisteredUserDTO source, @MappingTarget RegisteredUser target);

    @Mapping(target = "sessionToken", ignore = true)
    @Mapping(target = "schoolOther", ignore = true)
    @Mapping(target = "schoolId", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "registrationDate", ignore = true)
    @Mapping(target = "registeredContextsLastConfirmed", ignore = true)
    @Mapping(target = "registeredContexts", ignore = true)
    @Mapping(target = "lastUpdated", ignore = true)
    @Mapping(target = "lastSeen", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "emailVerificationToken", ignore = true)
    @Mapping(target = "emailToVerify", ignore = true)
    RegisteredUser mapUserFromAuthProviderToRegisteredUser(UserFromAuthProvider source);

    @Mapping(target = "groupMembershipInformation", ignore = true)
    @SubclassMapping(source = UserSummaryForAdminUsersDTO.class, target = UserSummaryWithGroupMembershipDTO.class)
    @SubclassMapping(source = UserSummaryWithEmailAddressDTO.class, target = UserSummaryWithGroupMembershipDTO.class)
    @SubclassMapping(source = UserSummaryWithGroupMembershipDTO.class, target = UserSummaryWithGroupMembershipDTO.class)
    UserSummaryWithGroupMembershipDTO mapUserSummaryDTOtoUserSummaryWithGroupMembershipDTO(UserSummaryDTO source);

    @BeanMapping(resultType = UserSummaryDTO.class)
    UserSummaryDTO mapExtendedUserSummaryDTOtoBaseUserSummaryDTO(UserSummaryDTO source);

    @Named("copyUserSummaryDTO")
    @SubclassMapping(source = UserSummaryForAdminUsersDTO.class, target = UserSummaryForAdminUsersDTO.class)
    @SubclassMapping(source = UserSummaryWithEmailAddressDTO.class, target = UserSummaryWithEmailAddressDTO.class)
    @SubclassMapping(source = UserSummaryWithGroupMembershipDTO.class, target = UserSummaryWithGroupMembershipDTO.class)
    UserSummaryDTO copy(UserSummaryDTO source);
    UserSummaryForAdminUsersDTO copy(UserSummaryForAdminUsersDTO source);
    UserSummaryWithEmailAddressDTO copy(UserSummaryWithEmailAddressDTO source);
    UserSummaryWithGroupMembershipDTO copy(UserSummaryWithGroupMembershipDTO source);
}