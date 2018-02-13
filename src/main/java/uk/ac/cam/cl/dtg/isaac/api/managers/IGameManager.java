package uk.ac.cam.cl.dtg.isaac.api.managers;

import uk.ac.cam.cl.dtg.isaac.dto.GameboardDTO;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;

/**
 * Created by du220 on 08/02/2018.
 */
public interface IGameManager {

    GameboardDTO getGameboard(final String gameboardId) throws SegueDatabaseException;

}
