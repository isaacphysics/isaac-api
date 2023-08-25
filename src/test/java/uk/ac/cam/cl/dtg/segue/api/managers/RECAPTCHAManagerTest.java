package uk.ac.cam.cl.dtg.segue.api.managers;

import org.junit.Before;
import org.junit.Test;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;
import static uk.ac.cam.cl.dtg.segue.api.Constants.GOOGLE_RECAPTCHA_SECRET;

public class RECAPTCHAManagerTest {
    private PropertiesLoader properties;

    @Before
    public final void setUp() throws Exception {
        properties = createMock(PropertiesLoader.class);
        expect(properties.getProperty(GOOGLE_RECAPTCHA_SECRET)).andReturn("6LeIxAcTAAAAAGG-vFI1TnRWxMZNFuojJ4WifJWe").atLeastOnce();
        replay(properties);
    }

    private void setSecretInvalid() {
        properties = createMock(PropertiesLoader.class);
        expect(properties.getProperty(GOOGLE_RECAPTCHA_SECRET)).andReturn("invalid-secret").atLeastOnce();
        replay(properties);
    }

    private RECAPTCHAManager createTestInstance() {
        return new RECAPTCHAManager(properties);
    }

    @Test
    public final void verifyRecaptcha_ValidResponse_ShouldReturnSuccess() {
        RECAPTCHAManager testInstance = createTestInstance();
        assertTrue(testInstance.verifyRecaptcha("valid-response"));
    }

    @Test
    public final void isCaptchaValid_ValidResponse_ShouldReturnSuccessMessage() {
        RECAPTCHAManager testInstance = createTestInstance();
        assertEquals("reCAPTCHA verification successful.", testInstance.recaptchaResultString("valid-response"));
    }

    @Test
    public final void verifyRecaptcha_InvalidResponse_ShouldReturnFailure() {
        setSecretInvalid();
        RECAPTCHAManager testInstance = createTestInstance();
        assertFalse(testInstance.verifyRecaptcha("invalid-response"));
    }

    @Test
    public final void isCaptchaValid_InvalidResponse_ShouldReturnFailedMessage() {
        setSecretInvalid();
        RECAPTCHAManager testInstance = createTestInstance();
        assertEquals("reCAPTCHA verification failed.", testInstance.recaptchaResultString("invalid-response"));
    }

    @Test
    public final void verifyRecaptcha_EmptyResponse_ShouldReturnFailure() {
        RECAPTCHAManager testInstance = createTestInstance();
        assertFalse(testInstance.verifyRecaptcha(""));
    }

    @Test
    public final void isCaptchaValid_EmptyResponse_ShouldReturnMissingResponseMessage() {
        RECAPTCHAManager testInstance = createTestInstance();
        assertEquals("Missing reCAPTCHA response token.", testInstance.recaptchaResultString(""));
    }
}
