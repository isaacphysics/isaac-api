package uk.ac.cam.cl.dtg.isaac.app;

import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.dtg.util.PropertiesLoader;

import com.google.inject.AbstractModule;

/**
 * This class is responsible for injecting configuration values 
 *
 * TODO: should this be a singleton 
 */
public class IsaacPersistenceConfigurationModule extends AbstractModule {

	private static final Logger log = LoggerFactory.getLogger(IsaacPersistenceConfigurationModule.class);

	private static PropertiesLoader globalProperties;

	public IsaacPersistenceConfigurationModule(){
		try {
			if(null == globalProperties){
				globalProperties = new PropertiesLoader("/config/segue-config.properties");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void configure() {
		// Setup different persistence bindings
		bind(PropertiesLoader.class).toInstance(globalProperties);
	}
}
