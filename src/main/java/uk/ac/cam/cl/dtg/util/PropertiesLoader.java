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
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

/**
 * A simple Immutable helper class for loading properties files and retrieving
 * values from them.
 * 
 * @author Stephen Cummins
 */
public class PropertiesLoader {
	private static final Logger log = LoggerFactory
			.getLogger(PropertiesLoader.class);
	
	private final Properties loadedProperties;
	private final String propertiesFile;

	/**
	 * This constructor will give attempt to read the contents of the file
	 * specified and load each key value pair into memory.
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

		if (null == propertiesFile) {
			log.error("Properties file cannot be null");
			throw new NullPointerException();
		} else {
			// check to see if this a resource or a file somewhere else
			if (getClass().getClassLoader()
					.getResourceAsStream(this.propertiesFile) == null) {
				File file = new File(this.propertiesFile);
				FileInputStream ioStream = new FileInputStream(file);

				// then we have to look further a field
				loadedProperties.load(ioStream);
			} else {
				loadedProperties.load(getClass().getClassLoader()
						.getResourceAsStream(this.propertiesFile));				
			}
			
			log.debug("Properties file read successfully " + propertiesFile);
		}
	}

	/**
	 * Attempt to retrieve a property value from the registered propertiesFile.
	 * 
	 * @param key - that the property is listed under.
	 * @return value as a String
	 */
	public String getProperty(final String key) {
		if (null == key) {
			log.error("Property key requested cannot be null");
			throw new NullPointerException();
		}

		String value = loadedProperties.getProperty(key);

		if (null == value) {
			log.warn("Failed to resolve requested property with key: " + key);
		}

		return value;
	}

	/**
	 * Gets the loadedProperties.
	 * @return the loadedProperties
	 */
	protected Properties getLoadedProperties() {
		return loadedProperties;
	}

	/**
	 * Gets the propertiesFile.
	 * @return the propertiesFile
	 */
	protected String getPropertiesFile() {
		return propertiesFile;
	}
	
}
