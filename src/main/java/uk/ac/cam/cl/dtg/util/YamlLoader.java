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

package uk.ac.cam.cl.dtg.util;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class YamlLoader extends AbstractConfigLoader {

    private static final Logger log = LoggerFactory.getLogger(YamlLoader.class);

    private Map<String, String> loadedConfig;

    /**
     * @param configPaths A comma-separated list of paths to config files. If more than one is provided, conflicted keys
     *                    will prefer values from later configs in the list.
     * @throws IOException If the config files at the paths provided cannot be read.
     */
    public YamlLoader(final String configPaths) throws IOException {
        super(configPaths);
        this.loadedConfig = new ConcurrentHashMap<>();
        loadConfig();
    }

    @Override
    public String getProperty(final String key) {
        Validate.notBlank(configPath, "Property key requested cannot be null");

        String value = loadedConfig.get(key);

        if (null == value) {
            log.warn("Failed to resolve requested property with key: " + key + ", " + this.configPath);
        }

        return value;
    }

    @Override
    public Set<String> getKeys() {
        return this.loadedConfig.keySet();
    }

    /**
     * Factory method that can be overridden for testing
     */
    protected Yaml createYaml() {
        return new Yaml();
    }

    /**
     * Factory method that can be overridden for testing
     */
    protected FileInputStream createFileInputStream(String path) throws IOException {
        return new FileInputStream(path);
    }

    @Override
    protected synchronized void loadConfig() throws IOException {
        Yaml yaml = createYaml();

        String[] configPaths = this.configPath.split(",");

        for (String path : configPaths) {
            Map<String, String> subConfig;

            // check to see if this a resource or a file somewhere else
            if (getClass().getClassLoader().getResourceAsStream(path) == null) {
                try (FileInputStream ioStream = createFileInputStream(path)) {
                    // then we have to look further afield
                    subConfig = yaml.load(ioStream);
                }
            } else {
                subConfig = yaml.load(getClass().getClassLoader().getResourceAsStream(path));
            }

            // Merge the just-loaded config values with those previously loaded, giving precedence to just-loaded values
            // if there's a conflict.
            this.loadedConfig = Stream.of(this.loadedConfig, subConfig)
                    .flatMap(map -> map.entrySet().stream())
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (v1, v2) -> v2)
                    );
        }
        this.lastRefreshed = new Date();
        log.debug("YAML config files read successfully: " + Arrays.toString(configPaths));
    }
}
