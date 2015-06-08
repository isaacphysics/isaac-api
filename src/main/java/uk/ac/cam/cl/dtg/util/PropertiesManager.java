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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

/**
 * A simple helper class for loading and modifying properties files.
 * 
 * @author Stephen Cummins
 */
public class PropertiesManager extends PropertiesLoader {
    private static final Logger log = LoggerFactory.getLogger(PropertiesManager.class);

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
    public PropertiesManager(final String propertiesFile) throws IOException {
        super(propertiesFile);

    }

    /**
     * Causes the property provided to be stored in the underlying file.
     * 
     * @param key
     *            - property name
     * @param value
     *            - property value.
     * @throws IOException
     *             - if the write operation fails.
     */
    public synchronized void saveProperty(final String key, final String value) throws IOException {
        this.getLoadedProperties().setProperty(key, value);

        File file = new File(this.getPropertiesFile());
        OutputStream out = new FileOutputStream(file);
        this.getLoadedProperties().store(out, "");

        log.debug("Writing out properties file " + this.getPropertiesFile());
        out.close();
    }
}
