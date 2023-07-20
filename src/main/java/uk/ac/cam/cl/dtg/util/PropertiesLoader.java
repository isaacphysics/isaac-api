/**
 * Copyright 2014 Stephen Cummins
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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

/**
 * A simple helper class for loading properties files and retrieving values from them.
 * 
 * @author Stephen Cummins
 */
public class PropertiesLoader extends AbstractConfigLoader {
    private static final Logger log = LoggerFactory.getLogger(PropertiesLoader.class);

    private final Properties loadedProperties;
    private Date lastRefreshed;

    /**
     * This constructor will give attempt to read the contents of the file specified and load each key value pair into
     * memory.
     * 
     * @param configPath
     *            - the location of the properties file.
     * @throws IOException
     *             - if we cannot read the file for whatever reason.
     */
    @Inject
    public PropertiesLoader(final String configPath) throws IOException {
        super(configPath);
        this.loadedProperties = new Properties();

        loadConfig();
    }

    /**
     * Attempt to retrieve a property value from the registered configPath.
     * 
     * @param key
     *            - that the property is listed under.
     * @return value as a String
     */
    public String getProperty(final String key) {
        Validate.notBlank(configPath, "Property key requested cannot be null");

        String value = loadedProperties.getProperty(key);

        if (null == value) {
            log.warn("Failed to resolve requested property with key: " + key + ", " + this.configPath);
        }

        return value;
    }

    @Override
    public Set<String> getKeys() {
        Validate.notBlank(configPath, "Property file cannot be null");

        return loadedProperties.stringPropertyNames();
    }

    /**
     * Gets the loadedProperties.
     * 
     * @return the loadedProperties
     */
    protected Properties getLoadedProperties() {
        return loadedProperties;
    }

    /**
     * @throws IOException
     *             if we cannot read the properties file.
     */
    @Override
    protected synchronized void loadConfig() throws IOException {
        // check to see if this a resource or a file somewhere else
        if (getClass().getClassLoader().getResourceAsStream(this.configPath) == null) {
            File file = new File(this.configPath);
            try (FileInputStream ioStream = new FileInputStream(file)) {
                // then we have to look further afield
                loadedProperties.load(ioStream);
            }
        } else {
            loadedProperties.load(getClass().getClassLoader().getResourceAsStream(this.configPath));
        }
        lastRefreshed = new Date();
        log.debug("Properties file read successfully " + configPath);
    }
}
