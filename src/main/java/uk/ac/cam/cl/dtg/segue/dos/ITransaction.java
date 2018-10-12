package uk.ac.cam.cl.dtg.segue.dos;

import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;

/**
 * Created by du220 on 04/06/2018.
 */
public interface ITransaction {

    Object getConnection();
    void commit() throws SegueDatabaseException;
}
