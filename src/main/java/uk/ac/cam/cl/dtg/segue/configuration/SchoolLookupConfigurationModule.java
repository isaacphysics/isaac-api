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
package uk.ac.cam.cl.dtg.segue.configuration;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.dao.schools.SchoolListReader;
import uk.ac.cam.cl.dtg.segue.search.ISearchProvider;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.name.Names;

/**
 * This class is responsible for injecting configuration values for the school look up service and
 * related classes.
 * 
 */
public class SchoolLookupConfigurationModule extends AbstractModule {
	private static final Logger log = LoggerFactory.getLogger(SchoolLookupConfigurationModule.class);

	private PropertiesLoader globalProperties = null;
	private SchoolListReader schoolListReader = null;
	/**
	 * Create a SegueGuiceConfigurationModule.
	 */
	public SchoolLookupConfigurationModule() {
		try {
			globalProperties = new PropertiesLoader("/config/segue-config.properties");

		} catch (IOException e) {
			log.error("Error loading properties file.", e);
		}
	}

	@Override
	protected void configure() {
		this.configureProperties();
	}

	/**
	 * Extract properties and bind them to constants.
	 */
	private void configureProperties() {
		// School look up config
		this.bindConstantToProperty(Constants.SCHOOL_CSV_LIST_PATH, globalProperties);
	}

	/**
	 * This provides a singleton of the SchoolListReader for use by segue backed applications..
	 *
	 * We want this to be a singleton as otherwise it may not be threadsafe for loading into same SearchProvider.
	 * 
	 * @param schoolListPath - The location of the school data.
	 * @param provider - The search provider.
	 * @return schoolList reader
	 * @throws IOException - if there is a problem loading the school list.
	 */
	@Inject
	@Provides
	@Singleton
	private SchoolListReader getSchoolListReader(
			@Named(Constants.SCHOOL_CSV_LIST_PATH) final String schoolListPath, final ISearchProvider provider)
		throws IOException {
		if (null == schoolListReader) {
			schoolListReader = new SchoolListReader(schoolListPath, provider);
			log.info("Creating singleton of SchoolListReader");
		}

		return schoolListReader;
	}
	
	/**
	 * Utility method to make the syntax of property bindings clearer.
	 * 
	 * @param propertyLabel
	 *            - Key for a given property
	 * @param propertyLoader
	 *            - property loader to use
	 */
	private void bindConstantToProperty(final String propertyLabel, final PropertiesLoader propertyLoader) {
		bindConstant().annotatedWith(Names.named(propertyLabel))
				.to(propertyLoader.getProperty(propertyLabel));
	}
}
