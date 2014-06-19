package uk.ac.cam.cl.dtg.segue.auth;

public class CrossSiteRequestForgeryException extends Exception {
	private static final long serialVersionUID = -8542483814754486874L;

	
	public CrossSiteRequestForgeryException(String msg){
		super(msg);
	}
}
