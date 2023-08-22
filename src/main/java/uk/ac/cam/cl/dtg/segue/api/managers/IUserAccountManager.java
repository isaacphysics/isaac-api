package uk.ac.cam.cl.dtg.segue.api.managers;

import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserException;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.isaac.dto.users.RegisteredUserDTO;

/**
 * Created by du220 on 18/01/2018.
 */
public interface IUserAccountManager {

    RegisteredUserDTO getUserDTOById(Long id) throws NoUserException, SegueDatabaseException;

    RegisteredUserDTO getUserDTOById(Long id, boolean includeDeleted) throws NoUserException, SegueDatabaseException;

}
