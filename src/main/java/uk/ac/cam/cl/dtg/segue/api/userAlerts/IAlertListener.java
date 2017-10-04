package uk.ac.cam.cl.dtg.segue.api.userAlerts;

import uk.ac.cam.cl.dtg.segue.dos.IUserAlert;

public interface IAlertListener {
    void notifyAlert(IUserAlert alert);
}
