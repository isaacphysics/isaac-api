package uk.ac.cam.cl.dtg.segue.dao;

public interface ILogManager {

	public boolean log(String sessionId, String cookieId, String eventJSON);
}
