/*
 * Copyright 2023 Matthew Trew
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.ac.cam.cl.dtg.segue.api.managers;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CountryLookupManager {

    /**
     * Map of country codes to display names.
     */
    private static Map<String, String> allCountryCodesAndNames;
    private static Map<String, String> priorityCountryCodesAndNames;


    /**
     * Manages the set of known countries. This set is based on ISO 3166 with the option to add custom entries.
     *
     * @param customCountryCodes A map of custom country codes and display names to be included, which can be empty.
     */
    public CountryLookupManager(Map<String, String> customCountryCodes, List<String> priorityCountryCodes) {
        allCountryCodesAndNames = getAllCountryCodesAndNames(customCountryCodes);
        priorityCountryCodesAndNames = getPriorityCountryCodesAndNames(allCountryCodesAndNames, priorityCountryCodes);
    }

    /**
     * Checks if the provided country code is known.
     *
     * @param countryCode an ISO 3166 alpha-2 country code.
     * @return true if the country code is known, false otherwise.
     */
    public static boolean isKnownCountryCode(String countryCode) {
        return allCountryCodesAndNames.containsKey(countryCode);
    }

    /**
     * Returns a sorted {@link Map} of ISO 3166 country codes (plus any custom codes) to display names.
     *
     * @return A {@link Map} of ISO 3166 alpha-2 country codes (plus any custom codes) to display names, sorted
     * alphabetically by display name.
     */
    public Map<String, String> getCountryCodesAndNames() {
        return allCountryCodesAndNames;
    }

    /**
     * Returns a sorted {@link Map} of priority ISO 3166 country codes (plus any custom codes) to display names.
     *
     * @return A {@link Map} of ISO 3166 alpha-2 country codes (plus any custom codes) to display names, sorted
     * alphabetically by display name.
     */
    public Map<String, String> getPriorityCountryCodesAndNames() {
        return priorityCountryCodesAndNames;
    }

    private static Map<String, String> getAllCountryCodesAndNames(Map<String, String> customCountries) {
        // Merge custom and ISO country maps.
        return Stream.of(getISOCountryCodesAndNames(), customCountries)
                .flatMap(map -> map.entrySet().stream())
                .sorted(Map.Entry.comparingByValue())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
    }

    private static Map<String, String> getPriorityCountryCodesAndNames(Map<String, String> allCountries, List<String> priorityCountries) {
        return allCountries.entrySet().stream()
                .filter(entry -> priorityCountries.contains(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
    }

    private static Map<String, String> getISOCountryCodesAndNames(){
        return Arrays.stream(Locale.getISOCountries())
                .collect(Collectors.toMap(String::new, CountryLookupManager::getDisplayNameForISOCountryCode));
    }

    private static String getDisplayNameForISOCountryCode(String code) {
        return new Locale(Locale.ENGLISH.getLanguage(), code).getDisplayCountry();
    }
}
