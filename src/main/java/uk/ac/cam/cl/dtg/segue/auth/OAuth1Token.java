package uk.ac.cam.cl.dtg.segue.auth;

public class OAuth1Token {
	private String token;
	private String tokenSecret;
	
	public OAuth1Token(String token, String tokenSecret) {
		this.token = token;
		this.tokenSecret = tokenSecret;
	}
	
	/**
	 * @return the token
	 */
	public String getToken() {
		return token;
	}
	/**
	 * @param token the token to set
	 */
	public void setToken(String token) {
		this.token = token;
	}
	/**
	 * @return the tokenSecret
	 */
	public String getTokenSecret() {
		return tokenSecret;
	}
	/**
	 * @param tokenSecret the tokenSecret to set
	 */
	public void setTokenSecret(String tokenSecret) {
		this.tokenSecret = tokenSecret;
	}
}
