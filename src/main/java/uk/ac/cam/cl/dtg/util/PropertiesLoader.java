/**
 * Copyright 2014 Stephen Cummins
 * <br>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * <br>
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * <br>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.ac.cam.cl.dtg.util;

import com.google.inject.Inject;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Properties;
import java.util.Set;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple helper class for loading properties files and retrieving values from them.
 *
 * @author Stephen Cummins
 */
public class PropertiesLoader {
  private static final Logger log = LoggerFactory.getLogger(PropertiesLoader.class);

  private final Properties loadedProperties;
  private final String propertiesFile;
  private Date lastRefreshed;

  /**
   * This constructor will give attempt to read the contents of the file specified and load each key value pair into
   * memory.
   *
   * @param propertiesFile
   *            - the location of the properties file.
   * @throws IOException
   *             - if we cannot read the file for whatever reason.
   */
  @Inject
  public PropertiesLoader(final String propertiesFile) throws IOException {
    this.loadedProperties = new Properties();
    this.propertiesFile = propertiesFile;
    Validate.notBlank(propertiesFile, "Properties file cannot be null");

    loadProperties();
  }

  /**
   * Attempt to retrieve a property value from the registered propertiesFile.
   *
   * @param key
   *            - that the property is listed under.
   * @return value as a String
   */
  public String getProperty(final String key) {
    Validate.notBlank(propertiesFile, "Property key requested cannot be null");

    String value = loadedProperties.getProperty(key);

    if (null == value) {
      log.warn("Failed to resolve requested property with key: {}, {}", key, propertiesFile);
    }

    return value;
  }

  public Set<String> stringPropertyNames() {
    Validate.notBlank(propertiesFile, "Property file cannot be null");

    return loadedProperties.stringPropertyNames();
  }

  /**
   * triggerPropertiesRefresh.
   *
   * @throws IOException
   *             - if we cannot load the properties file.
   */
  public synchronized void triggerPropertiesRefresh() throws IOException {
    loadProperties();
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
   * Gets the propertiesFile.
   *
   * @return the propertiesFile
   */
  protected String getPropertiesFile() {
    return propertiesFile;
  }

  /**
   * Gets the lastRefreshed.
   *
   * @return the lastRefreshed
   */
  public Date getLastRefreshed() {
    return lastRefreshed;
  }

  /**
   * @throws IOException
   *             if we cannot read the properties file.
   */
  private synchronized void loadProperties() throws IOException {
    // check to see if this a resource or a file somewhere else
    if (getClass().getClassLoader().getResourceAsStream(this.propertiesFile) == null) {
      File file = new File(this.propertiesFile);
      try (FileInputStream ioStream = new FileInputStream(file)) {
        // then we have to look further afield
        loadedProperties.load(ioStream);
      }
    } else {
      loadedProperties.load(getClass().getClassLoader().getResourceAsStream(this.propertiesFile));
    }
    lastRefreshed = new Date();
    log.debug("Properties file read successfully {}", propertiesFile);
  }

  /**
   * Numerical properties must be parsed from Strings. While it should not be an issue with properly configured
   * properties, this could cause a NumberFormatException. This method will try to load the property and return the
   * provided fallback value if it fails.
   *
   * @param key - the identifier for the property to load
   * @param fallbackValue - an alternative value in the event of failure to parse
   * @return the loaded value or the fallback value
   */
  public Integer getIntegerPropertyOrFallback(final String key, final Integer fallbackValue) {
    try {
      return Integer.parseInt(this.getProperty(key));
    } catch (NumberFormatException e) {
      log.error(
          String.format("Could not read %1$s from property configuration. Defaulting to %2$d.", key, fallbackValue), e);
      return fallbackValue;
    }
  }
}
