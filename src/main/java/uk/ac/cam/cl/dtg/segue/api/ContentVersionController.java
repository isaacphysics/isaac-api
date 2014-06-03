package uk.ac.cam.cl.dtg.segue.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

import uk.ac.cam.cl.dtg.segue.dao.IContentManager;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

public class ContentVersionController {
	
	private static final Logger log = LoggerFactory.getLogger(ContentVersionController.class);
	
	private static volatile String liveVersion;
	
	private PropertiesLoader properties;
	
	private IContentManager contentManager;
	
	@Inject
	public ContentVersionController(PropertiesLoader properties, IContentManager contentManager){
		this.properties = properties;
		this.contentManager = contentManager;
		// we want to make sure we have set a default liveVersion number
		if(null == liveVersion){
			log.info("Setting live version of the site from properties file to " + Constants.INITIAL_LIVE_VERSION);
			liveVersion = this.properties.getProperty(Constants.INITIAL_LIVE_VERSION);		
		}
	}
	
	public synchronized String getLiveVersion(){
		return liveVersion;
	}
	
	public synchronized void syncJobCompleteCallback(String version){
		// for use by ContentSynchronisationJobs to alert that a live version change may occur has finished.
		
	}
	
	public synchronized void setLiveVersion(String newLiveVersion){
		liveVersion = newLiveVersion;
	}

	public IContentManager getContentManager(){
		return contentManager;
	}
	
}
