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

import java.io.IOException;
import java.util.Date;
import java.util.Set;

public abstract class AbstractConfigLoader {

    protected final String configPath;
    protected Date lastRefreshed;

    public AbstractConfigLoader(final String configPath) {
        Validate.notBlank(configPath, "Config file path cannot be empty!");
        this.configPath = configPath;
    }

    /**
     * Retrieve the value associated with {@code key} in the config.
     *
     * @param key A key in the config.
     * @return The associated value.
     */
    public abstract String getProperty(String key);

    /**
     * Retrieve all config keys.
     *
     * @return All config keys.
     */
    public abstract Set<String> getKeys();

    /**
     * Returns the path to the config file.
     *
     * @return The path to the config file.
     */
    protected String getConfigPath() {
        return configPath;
    }

    /**
     * Loads the config file at {@link this.configPath}.
     *
     * @throws IOException If we cannot read the config file.
     */
    protected abstract void loadConfig() throws IOException;

    /**
     * Reload the config file.
     *
     * @throws IOException If we cannot load the config file.
     */
    public synchronized void reloadConfig() throws IOException {
        loadConfig();
    }

    /**
     * Gets the lastRefreshed.
     *
     * @return the lastRefreshed
     */
    public Date getLastRefreshed() {
        return lastRefreshed;
    }
}
