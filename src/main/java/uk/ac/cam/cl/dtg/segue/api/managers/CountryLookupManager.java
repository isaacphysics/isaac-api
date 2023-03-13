/*
 * Copyright 2023 Matthew Trew
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.ac.cam.cl.dtg.segue.api.managers;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class CountryLookupManager {

    /**
     * Maps of ISO country codes to display names in a particular language, keyed by language code.
     */
    public Map<String, Map<String, String>> isoCountryNamesCache;

    public CountryLookupManager() {
        isoCountryNamesCache = new ConcurrentHashMap<>();
    }

    /**
     * Checks if the provided country code is known.
     *
     * @param countryCode an ISO 3166 alpha-2 country code.
     * @return true if the country code is known, false otherwise.
     */
    public static boolean isKnownCountryCode(String countryCode) {
        return List.of(Locale.getISOCountries()).contains(countryCode);
    }

    /**
     * Returns a sorted {@link Map} of ISO 3166 country codes to display names for a particular language.
     * The map is cached for re-use.
     *
     * @param isoLanguageCode An ISO 639 language code. The returned display names will be in this language.
     * @return A {@link Map} of ISO 3166 alpha-2 country codes to display names, sorted alphabetically by display name.
     */
    public Map<String, String> getISOCountryCodesAndNames(String isoLanguageCode) {
        if (null == isoCountryNamesCache.get(isoLanguageCode)) {
            isoCountryNamesCache.put(isoLanguageCode, getSortedISOCountryCodesAndNamesForLanguage(isoLanguageCode));
        }
        return isoCountryNamesCache.get(isoLanguageCode);
    }

    private Map<String, String> getSortedISOCountryCodesAndNamesForLanguage(String isoLanguageCode) {
        Map<String, String> countries = new HashMap<>();

        for (String countryCode : Locale.getISOCountries()) {
            countries.put(countryCode, new Locale(isoLanguageCode, countryCode).getDisplayCountry());
        }

        // Sort alphabetically by display name, ascending
        return countries.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
    }
}
