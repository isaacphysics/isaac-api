package uk.ac.cam.cl.dtg.clojure;

public interface InterestRegistration {
	
	/*
	 * Logs a JSON event from the specified browser session.
	 * Returns true if we were able to store the event. 
	 */
	public boolean register(String name, String email, String role, String school, String year, boolean feedback);

}
