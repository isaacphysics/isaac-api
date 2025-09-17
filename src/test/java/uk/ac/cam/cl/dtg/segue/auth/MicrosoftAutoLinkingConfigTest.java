package uk.ac.cam.cl.dtg.segue.auth;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import static org.junit.Assert.*;

@SuppressWarnings("checkstyle:MissingJavadocType")
public class MicrosoftAutoLinkingConfigTest {
    private final String emptyConfig = "{}";
    private final String tenantId = "766576da-ca7a-46eb-bf55-43eea65e85b7";
    private final String emailDomain = "@linkable.com";
    private final String validConfig = new JSONArray()
            .put(new JSONObject().put("tenantId", tenantId).put("emailDomain", emailDomain))
            .toString();

    @Test
    public final void testConstruction_emptyConfig_noConfigParsed() {
        assertEquals(0, new MicrosoftAutoLinkingConfig(emptyConfig).size());
    }

    @Test
    public final void testConstruction_nullConfig_noConfigParsed() {
        assertEquals(0, new MicrosoftAutoLinkingConfig(null).size());
    }

    @Test
    public final void testConstruction_validConfig_parsedSuccessfully() {
        assertEquals(1, new MicrosoftAutoLinkingConfig(validConfig).size());
    }

    @Test
    public final void testEnabledFor_validConfig_enabledForMatchingTidAndEmail() {
        var autoLinkingConfig = new MicrosoftAutoLinkingConfig(validConfig);
        assertTrue(autoLinkingConfig.enabledFor(String.format("someone@%s", emailDomain)));
    }
}