package uk.ac.cam.cl.dtg.segue.api.managers;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.ac.cam.cl.dtg.segue.api.Constants.GOOGLE_RECAPTCHA_SECRET;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

class RecaptchaManagerTest {
  private PropertiesLoader properties;

  @BeforeEach
  public final void setUp() throws Exception {
    properties = createMock(PropertiesLoader.class);
    expect(properties.getProperty(GOOGLE_RECAPTCHA_SECRET)).andReturn("6LeIxAcTAAAAAGG-vFI1TnRWxMZNFuojJ4WifJWe")
        .atLeastOnce();
    replay(properties);
  }

  private void setSecretInvalid() {
    properties = createMock(PropertiesLoader.class);
    expect(properties.getProperty(GOOGLE_RECAPTCHA_SECRET)).andReturn("invalid-secret").atLeastOnce();
    replay(properties);
  }

  private RecaptchaManager createTestInstance() {
    return new RecaptchaManager(properties);
  }

  @Test
  final void verifyRecaptcha_ValidResponse_ShouldReturnSuccess() {
    RecaptchaManager testInstance = createTestInstance();
    assertTrue(testInstance.verifyRecaptcha("valid-response"));
  }

  @Test
  final void isCaptchaValid_ValidResponse_ShouldReturnSuccessMessage() {
    RecaptchaManager testInstance = createTestInstance();
    assertEquals("reCAPTCHA verification successful.", testInstance.recaptchaResultString("valid-response"));
  }

  @Test
  final void verifyRecaptcha_InvalidResponse_ShouldReturnFailure() {
    setSecretInvalid();
    RecaptchaManager testInstance = createTestInstance();
    assertFalse(testInstance.verifyRecaptcha("invalid-response"));
  }

  @Test
  final void isCaptchaValid_InvalidResponse_ShouldReturnFailedMessage() {
    setSecretInvalid();
    RecaptchaManager testInstance = createTestInstance();
    assertEquals("reCAPTCHA verification failed.", testInstance.recaptchaResultString("invalid-response"));
  }

  @Test
  final void verifyRecaptcha_EmptyResponse_ShouldReturnFailure() {
    RecaptchaManager testInstance = createTestInstance();
    assertFalse(testInstance.verifyRecaptcha(""));
  }

  @Test
  final void isCaptchaValid_EmptyResponse_ShouldReturnMissingResponseMessage() {
    RecaptchaManager testInstance = createTestInstance();
    assertEquals("Missing reCAPTCHA response token.", testInstance.recaptchaResultString(""));
  }
}
