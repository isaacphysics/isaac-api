package uk.ac.cam.cl.dtg.segue.api.managers;

import com.google.inject.Inject;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.database.PostgresSqlDb;
import uk.ac.cam.cl.dtg.segue.dos.ITransaction;
import uk.ac.cam.cl.dtg.segue.dos.PgTransaction;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Created by du220 on 21/05/2018.
 */
public class PgTransactionManager implements ITransactionManager {

    private PostgresSqlDb postgresSqlDb;

    @Inject
    public PgTransactionManager(PostgresSqlDb postgresSqlDb) {
        this.postgresSqlDb = postgresSqlDb;
    }

    @Override
    public PgTransaction getTransaction() throws SegueDatabaseException {
        return new PgTransaction(postgresSqlDb);
    }
}
