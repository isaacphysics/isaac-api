package uk.ac.cam.cl.dtg.segue.auth;

public interface IOAuth2AuthenticatorWithSignupFlow  extends IOAuth2Authenticator {
    /**
     * Step 1 of OAUTH2 - Request authorisation URL. Request an authorisation
     * url which will allow the user to be logged in with their oauth provider.
     *
     * @param antiForgeryStateToken A unique token to protect against CSRF attacks
     * @param isSignUpFlow Whether this is an initial sign-up, which may be used to direct the client to a sign-up flow.
     *
     * @return String - A url which should be fully formed and ready for the
     *         user to login with - this should result in a callback to a
     *         prearranged api endpoint if successful.
     */
    public String getAuthorizationUrl(String antiForgeryStateToken, boolean isSignUpFlow);
}
