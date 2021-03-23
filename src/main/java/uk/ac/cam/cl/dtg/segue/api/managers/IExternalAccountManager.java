package uk.ac.cam.cl.dtg.segue.api.managers;

public interface IExternalAccountManager {

    void synchroniseChangedUsers() throws ExternalAccountSynchronisationException;
}
