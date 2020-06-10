package uk.ac.cam.cl.dtg.segue.api.managers;

import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserException;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;

/**
 * Created by du220 on 18/01/2018.
 */
public interface IUserAccountManager {

    RegisteredUserDTO getUserDTOById(final Long id) throws NoUserException, SegueDatabaseException;

    RegisteredUserDTO getUserDTOById(final Long id, final boolean includeDeleted) throws NoUserException, SegueDatabaseException;

}
