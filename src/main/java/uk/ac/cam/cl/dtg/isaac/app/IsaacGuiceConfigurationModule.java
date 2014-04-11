package uk.ac.cam.cl.dtg.isaac.app;

import java.io.IOException;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.dtg.isaac.configuration.SegueConfigurationModule;
import uk.ac.cam.cl.dtg.segue.api.ISegueDTOConfigurationModule;
import uk.ac.cam.cl.dtg.segue.api.SegueApiFacade;
import uk.ac.cam.cl.dtg.segue.dao.ContentMapper;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;

/**
 * This class is responsible for injecting configuration values using GUICE
 *
 * TODO: We should probably make an isaac specific property file?
 */
public class IsaacGuiceConfigurationModule extends AbstractModule {

	private static final Logger log = LoggerFactory.getLogger(IsaacGuiceConfigurationModule.class);

	private static PropertiesLoader globalProperties;
	
	private static SegueApiFacade segueApi = null;

	public IsaacGuiceConfigurationModule(){
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
		// Currently all properties are being provided by the segue properties file.
		//bind(PropertiesLoader.class).toInstance(globalProperties);
		
		bind(ISegueDTOConfigurationModule.class).toInstance(new SegueConfigurationModule());
	}
	
	/**
	 * This provides a singleton of the segue api facade that can be used
	 * by isaac to serve api requests as a library or register the endpoints with resteasy.
	 * 
	 * Note: A lot of the dependencies are injected from the segue project itself.
	 */
	@Inject
	@Provides
	private static SegueApiFacade getSegueFacadeSingleton(PropertiesLoader properties, ContentMapper mapper, @Nullable ISegueDTOConfigurationModule segueConfigurationModule){
		if(null == segueApi){
			segueApi = new SegueApiFacade(properties, mapper, segueConfigurationModule);
		}
		
		return segueApi;
	}
}
