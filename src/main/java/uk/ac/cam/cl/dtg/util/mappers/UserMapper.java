package uk.ac.cam.cl.dtg.util.mappers;

import org.mapstruct.Mapper;
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

    @Named("copyUserSummaryDTO")
    @SubclassMapping(source = UserSummaryForAdminUsersDTO.class, target = UserSummaryForAdminUsersDTO.class)
    @SubclassMapping(source = UserSummaryWithEmailAddressDTO.class, target = UserSummaryWithEmailAddressDTO.class)
    @SubclassMapping(source = UserSummaryWithGroupMembershipDTO.class, target = UserSummaryWithGroupMembershipDTO.class)
    UserSummaryDTO copy(UserSummaryDTO source);
    UserSummaryForAdminUsersDTO copy(UserSummaryForAdminUsersDTO source);
    UserSummaryWithEmailAddressDTO copy(UserSummaryWithEmailAddressDTO source);
    UserSummaryWithGroupMembershipDTO copy(UserSummaryWithGroupMembershipDTO source);
}