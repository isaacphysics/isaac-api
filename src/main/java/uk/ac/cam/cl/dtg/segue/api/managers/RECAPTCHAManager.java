package uk.ac.cam.cl.dtg.segue.api.managers;

import static uk.ac.cam.cl.dtg.segue.api.Constants.GOOGLE_RECAPTCHA_SECRET;

import com.google.inject.Inject;
import org.apache.commons.lang3.Validate;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.segue.api.services.SimpleHttpClientService;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

public class RECAPTCHAManager {
  private final PropertiesLoader properties;
  private static final Logger log = LoggerFactory.getLogger(RECAPTCHAManager.class);

  @Inject
  public RECAPTCHAManager(final PropertiesLoader properties) {
    Validate.notNull(properties.getProperty(GOOGLE_RECAPTCHA_SECRET));
    this.properties = properties;
  }

  public String recaptchaResultString(final String response) {
    if (response == null || response.isEmpty()) {
      return "Missing reCAPTCHA response token.";
    }

    if (verifyRecaptcha(response)) {
      return "reCAPTCHA verification successful.";
    } else {
      return "reCAPTCHA verification failed.";
    }
  }


  public synchronized boolean verifyRecaptcha(final String response) {
    try {
      if (!response.isEmpty()) {
        String secretKey = properties.getProperty(GOOGLE_RECAPTCHA_SECRET);
        SimpleHttpClientService client = new SimpleHttpClientService();
        String url = "https://www.google.com/recaptcha/api/siteverify";
        String params = "secret=" + secretKey + "&response=" + response;
        JSONObject json = client.post(url, params);
        return json.getBoolean("success");
      }
    } catch (Exception e) {
      log.error("Error during reCAPTCHA validation.", e);
    }
    return false;
  }
}
