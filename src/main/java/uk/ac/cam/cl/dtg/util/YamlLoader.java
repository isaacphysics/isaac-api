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

package uk.ac.cam.cl.dtg.util;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class YamlLoader extends AbstractConfigLoader {

    private static final Logger log = LoggerFactory.getLogger(YamlLoader.class);

    private Map<String, String> loadedConfig;

    public YamlLoader(final String configPath) throws IOException {
        super(configPath);
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

    @Override
    protected synchronized void loadConfig() throws IOException {
        Yaml yaml = new Yaml();

        // check to see if this a resource or a file somewhere else
        if (getClass().getClassLoader().getResourceAsStream(this.configPath) == null) {
            File file = new File(this.configPath);
            try (FileInputStream ioStream = new FileInputStream(file)) {
                // then we have to look further afield
                this.loadedConfig = yaml.load(ioStream);
            }
        } else {
            loadedConfig = yaml.load(getClass().getClassLoader().getResourceAsStream(this.configPath));
        }
        this.lastRefreshed = new Date();
        log.debug("YAML config file read successfully " + configPath);
    }
}
