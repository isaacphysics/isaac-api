package uk.ac.cam.cl.dtg.segue.api.managers;

import uk.ac.cam.cl.dtg.isaac.dos.ITransaction;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;

/**
 * Created by du220 on 04/06/2018.
 */
public interface ITransactionManager {

  ITransaction getTransaction() throws SegueDatabaseException;

}
