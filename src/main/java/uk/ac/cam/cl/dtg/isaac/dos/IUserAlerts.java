package uk.ac.cam.cl.dtg.isaac.dos;

import java.util.List;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;

public interface IUserAlerts {

  List<IUserAlert> getUserAlerts(Long userId) throws SegueDatabaseException;

  IUserAlert createAlert(Long userId, String message, String link) throws SegueDatabaseException;

  void recordAlertEvent(Long alertId, IUserAlert.AlertEvents eventType) throws SegueDatabaseException;
}
