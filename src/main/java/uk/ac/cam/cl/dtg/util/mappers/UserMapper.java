package uk.ac.cam.cl.dtg.util.mappers;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.SubclassMapping;
import org.mapstruct.factory.Mappers;

import uk.ac.cam.cl.dtg.isaac.dto.users.UserSummaryDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.UserSummaryForAdminUsersDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.UserSummaryWithEmailAddressDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.UserSummaryWithGroupMembershipDTO;

@Mapper
public interface UserMapper {
    UserMapper INSTANCE = Mappers.getMapper(UserMapper.class);

    default <T> T map(UserSummaryDTO source, Class<T> targetClass) {
        if (targetClass.equals(UserSummaryWithGroupMembershipDTO.class)) {
            return (T) mapUserSummaryDTOtoUserSummaryWithGroupMembershipDTO(source);
        } else if (targetClass.equals(UserSummaryDTO.class)) {
            return (T) mapExtendedUserSummaryDTOtoBaseUserSummaryDTO(source);
        } else {
            throw new RuntimeException();
        }
    }

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