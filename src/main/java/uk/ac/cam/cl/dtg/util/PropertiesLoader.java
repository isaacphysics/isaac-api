package uk.ac.cam.cl.dtg.util;

import java.io.IOException;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

/**
 * A simple Immutable helper class for loading properties files and retrieving values from them.
 * 
 * @author Stephen Cummins
 */
public class PropertiesLoader {
	private static final Logger log = LoggerFactory.getLogger(PropertiesLoader.class);
	private final Properties loadedProperties;
	private final String propertiesFile;
	
	/**
	 * This constructor will give attempt to read the contents of the file specified and load each key value pair into memory.
	 * 
	 * @param propertiesFile - the location of the properties file.
	 * @throws IOException - if we cannot read the file for whatever reason.
	 */
	@Inject
	public PropertiesLoader(String propertiesFile) throws IOException{
		this.loadedProperties = new Properties();
		this.propertiesFile = propertiesFile;
			 
    		if(null == propertiesFile){
    	        log.error("Properties file cannot be null");
    	        throw new NullPointerException();
    		}
    		else
    		{
    			loadedProperties.load(getClass().getClassLoader().getResourceAsStream(this.propertiesFile));
    			log.debug("Properties file read successfully " + propertiesFile);
    		}
	}
	
	/**
	 * Attempt to retrieve a property value from the registered propertiesFile.
	 * 
	 * @param key
	 * @return value as a String
	 */
	public String getProperty(String key){
		if(null == key){
			log.error("Property key requested cannot be null");
			throw new NullPointerException();
		}
		
		String value = loadedProperties.getProperty(key);
		
		if(null == value)
			log.warn("Failed to resolve requested property with key: " + key);
		
		return value;
	}
}
