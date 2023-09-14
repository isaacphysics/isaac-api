package uk.ac.cam.cl.dtg.isaac.dos;

import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;

/**
 * Interface to abstract database transaction connection management.
 * <br>
 * Use an ITransaction to provide a Connection object to be used instead of
 * getting separate Connections from the thread pool for linked queries.
 * Statements executed using that object will be batched and not committed until
 * commit() is called.
 * <br>
 * Use in a try-with-resources block, but you MUST either commit or rollback
 * explicitly since behaviour is undefined on closing without doing so!
 * e.g.
 * <pre>
 *  try (ITransaction transaction = transactionManager.getTransaction()) {
 *      Connection conn = transaction.getConnection();
 *      doSomethingInATransaction(conn);
 *      doMoreInATransaction(conn);
 *      transaction.commit();
 *  }
 *  </pre>
 */
public interface ITransaction extends AutoCloseable {

  Object getConnection();

  void commit() throws SegueDatabaseException;

  void rollback() throws SegueDatabaseException;

  @Override
  void close() throws SegueDatabaseException;
}
