package uk.ac.cam.cl.dtg.teaching;

public interface DatomicLogger {
	
	public void logEvent(String sessionId, String eventJson);

}
