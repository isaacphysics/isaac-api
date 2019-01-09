package uk.ac.cam.cl.dtg.segue.api.userAlerts;

public interface IAlertListener {
    // Useful listener methods are static because each user can have multiple alert listeners (one for each tab).
    // We can't get the same polymorphism gains for static methods so, for now, we probably don't need this interface.
}
