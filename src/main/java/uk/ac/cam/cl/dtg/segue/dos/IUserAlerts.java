package uk.ac.cam.cl.dtg.segue.dos;

import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;

import java.util.List;

public interface IUserAlerts {

    List<IUserAlert> getUserAlerts(Long userId) throws SegueDatabaseException;

    IUserAlert createAlert(Long userId, String message, String link) throws SegueDatabaseException;

    void recordAlertEvent(Long alertId, IUserAlert.AlertEvents eventType) throws SegueDatabaseException;
}
