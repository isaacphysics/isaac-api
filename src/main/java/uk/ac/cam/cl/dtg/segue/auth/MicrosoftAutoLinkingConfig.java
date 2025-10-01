package uk.ac.cam.cl.dtg.segue.auth;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.commons.lang3.StringUtils;
import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAccountManager;

import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class MicrosoftAutoLinkingConfig {
    private List<ConfigEntry> entries;

    @Inject
    public MicrosoftAutoLinkingConfig(
            @Nullable @Named(Constants.MICROSOFT_ALLOW_AUTO_LINKING) final String jsonConfig
    ) {
        try {
            this.entries = new ObjectMapper().readValue(jsonConfig, new TypeReference<>() {});
        } catch (final JsonProcessingException | IllegalArgumentException e) {
            this.entries = new ArrayList<>();
        }
    }

    public boolean enabledFor(final String email) {
        return UserAccountManager.isUserEmailValid(email)
                && this.entries.stream().anyMatch(ConfigEntry.matchingDomain(email));
    }

    public int size() {
        return this.entries.size();
    }

    private static class ConfigEntry {
        String tenantId;
        String emailDomain;

        @JsonCreator
        public ConfigEntry(
                @JsonProperty("tenantId") final String tenantId,
                @JsonProperty("emailDomain") final String emailDomain
        ) {
            this.tenantId = tenantId;
            this.emailDomain = emailDomain;
        }

        public static Predicate<ConfigEntry> matchingDomain(final String email) {
            return entry -> entry.emailDomain.equals(StringUtils.substringAfter(email, '@'));
        }
    }
}
