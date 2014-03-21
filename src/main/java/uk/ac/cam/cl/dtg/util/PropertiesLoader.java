package uk.ac.cam.cl.dtg.util;

import java.io.IOException;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.dtg.segue.api.SegueApiFacade;

public class PropertiesLoader {
	private static final Logger log = LoggerFactory.getLogger(SegueApiFacade.class);
	private final Properties loadedProperties;
	private final String propertiesFile;
	
	public PropertiesLoader(String propertiesFile) throws IOException{
		this.loadedProperties = new Properties();
		this.propertiesFile = propertiesFile;
			 
    		if(propertiesFile==null){
    	        log.error("Properties file cannot be null");
    	        throw new NullPointerException();
    		}
    		else
    		{
    			loadedProperties.load(getClass().getClassLoader().getResourceAsStream(this.propertiesFile));
    		}
	}
	
	public String getProperty(String key){
		if(null == key){
			log.error("Property key requested cannot be null");
			throw new NullPointerException();
		}
		return loadedProperties.getProperty(key);
	}
	
}
