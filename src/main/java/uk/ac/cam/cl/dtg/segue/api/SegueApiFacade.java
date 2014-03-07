package uk.ac.cam.cl.dtg.segue.api;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.dtg.segue.dao.ContentMapper;
import uk.ac.cam.cl.dtg.segue.dao.GitContentManager;
import uk.ac.cam.cl.dtg.segue.dao.IContentManager;
import uk.ac.cam.cl.dtg.segue.dao.ILogManager;
import uk.ac.cam.cl.dtg.segue.database.PersistenceConfigurationModule;
import uk.ac.cam.cl.dtg.segue.dto.Content;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;
import com.google.inject.Guice;
import com.google.inject.Injector;


@Path("/")
public class SegueApiFacade {
	private static final Logger log = LoggerFactory.getLogger(SegueApiFacade.class);

	// TODO Move to a config value, perhaps stored in Mongo? Should this be an app setting or API one?
	private static String liveVersion = "a82ac5e8d8bd8ee6b8f98bbd973710ab5f0adc9a";
	
	@POST
	@Path("log")
	@Produces("application/json")
	public ImmutableMap<String, Boolean> postLog(
			@Context HttpServletRequest req,
			@FormParam("sessionId") String sessionId,
			@FormParam("cookieId") String cookieId,
			@FormParam("event") String eventJSON) {
		
		Injector injector = Guice.createInjector(new PersistenceConfigurationModule());
		ILogManager logPersistenceManager = injector.getInstance(ILogManager.class);

		boolean success = logPersistenceManager.log(sessionId, cookieId, eventJSON);

		return ImmutableMap.of("success", success);
	}	

	/**
	 * This method specifically uses mongodb to save content objects
	 * @deprecated
	 * @param docJson
	 * @return
	 */
	@POST
	@Produces("application/json")
	@Path("content/save")
	public Response contentSave(@FormParam("doc") String docJson) {
		Injector injector = Guice.createInjector(new PersistenceConfigurationModule());
		IContentManager contentPersistenceManager = injector.getInstance(IContentManager.class);

		ContentMapper mapper = injector.getInstance(ContentMapper.class);
		
		log.info("INSERTING DOC: " + docJson);
		
		String newId = null;
		try {			
			Content cnt = mapper.load(docJson);
			
			newId = contentPersistenceManager.save(cnt);			
		} catch (JsonParseException e) {
			e.printStackTrace();
			return Response.serverError().entity(ImmutableMap.of("error", e.toString())).build();
		} catch (JsonMappingException e) {
			e.printStackTrace();
			return Response.serverError().entity(ImmutableMap.of("error", e.toString())).build();
		} catch (IOException e) {
			e.printStackTrace();
			return Response.serverError().entity(ImmutableMap.of("error", e.toString())).build();
		}
		
		if (newId != null)
			return Response.ok().entity(ImmutableMap.of("result", "success", "newId", newId)).build();
		else
			return Response.serverError().entity(ImmutableMap.of("error", "No new Id was assigned by the database")).build();
	}
	
	/**
	 * GetContentById from the database
	 * 
	 * Currently this method will return a single Json Object containing all of the fields available to the object retrieved from the database.
	 * 
	 * @param id - our id not the dbid
	 * @return Response object containing the serialized content object. (with no levels of recursion into the content)
	 */
	@GET
	@Produces("application/json")
	@Path("content/get/{id}")
	public Response getContentById(@PathParam("id") String id) {		
		Injector injector = Guice.createInjector(new PersistenceConfigurationModule());
		IContentManager contentPersistenceManager = injector.getInstance(IContentManager.class);
		
		Content c = null;
		
		// Deserialize object into POJO of specified type, providing one exists. 
		try{
			log.info("RETRIEVING DOC: " + id);
			c = contentPersistenceManager.getById(id, SegueApiFacade.liveVersion);
			
			if (null == c){
				return Response.noContent().entity(null).build();
			}
			
		}
		catch(IllegalArgumentException e){
			log.error("Unable to map content object.", e);
			return Response.serverError().entity(e).build();
		}
		
		return Response.ok().entity(c).build();
	}
	
	/**
	 * getFileContent from the file store
	 * 
	 * This method will return a byte array of the contents of a single file for the given path.
	 * 
	 * This is a temporary method for serving image files directly from git with a view that we can have a CDN cache these for us.
	 * 
	 * @param version number - e.g. a sha
	 * @return Response object containing the serialized content object. (with no levels of recursion into the content)
	 * @throws java.lang.UnsupportedOperationException if multiple files match the search input
	 */
	@GET
	@Produces("*/*")
	@Path("content/getFileContent/{version}/{path:.*}")
	public Response getFileContent(@PathParam("version") String version, @PathParam("path") String path) {				
		// TODO check if the content being requested is valid for this api call. e.g. only images?
		if(null == version || null == path || Files.getFileExtension(path).isEmpty()){
			log.info("Bad input to api call. Returning null");
			return Response.serverError().entity(null).build();
		}

		Injector injector = Guice.createInjector(new PersistenceConfigurationModule());
		GitContentManager gcm = injector.getInstance(GitContentManager.class);
		
		ByteArrayOutputStream fileContent = null;
		String mimeType = MediaType.WILDCARD; 
		
		switch(Files.getFileExtension(path).toLowerCase()){
			case "svg":{
				mimeType = "image/svg+xml";
				break;
			}
		}
		
		try {
			fileContent = gcm.getFileBytes(version, path);
		} catch (IOException e) {
			log.error("Error reading from file repository");
			e.printStackTrace();
		} catch (UnsupportedOperationException e){
			log.error("Multiple files match the search path provided. Returning null as the result.");
		}
		
		if (null == fileContent){
			return Response.noContent().entity(null).build();
		}
		
		return Response.ok().type(mimeType).entity(fileContent.toByteArray()).build();
	}	

	/**
	 * Developer method that will return all content of a given type.
	 * 
	 * Useful for retrieving all content of a specific type from the cache.
	 *  
	 * @param type
	 * @param limit
	 * @return
	 */
	@GET
	@Produces("application/json")
	@Path("content/getAllContentByType/{type}")
	public Response getAllContentByType(String type, Integer limit){
		Injector injector = Guice.createInjector(new PersistenceConfigurationModule());
		IContentManager contentPersistenceManager = injector.getInstance(IContentManager.class);
		
		List<Content> c = null;
		
		// Deserialize object into POJO of specified type, providing one exists. 
		try{
			log.info("Finding all content from the api with type: " + type);
			c = contentPersistenceManager.findAllByType(type, SegueApiFacade.liveVersion, limit);
		}
		catch(IllegalArgumentException e){
			log.error("Unable to map content object.", e);
			return Response.serverError().entity(e).build();
		}
		
		return Response.ok().entity(c).build();
	}
	
	/**
	 * This method will allow the live version served by the site to be changed 
	 * TODO: Maybe some security???!
	 * 
	 * @param version
	 * @return Success shown by returning the new liveSHA or failed message "Invalid version selected".
	 */
	@GET
	@Produces("application/json")
	@Path("admin/changeLiveVersion/{version}")
	public synchronized Response changeLiveVersion(@PathParam("version") String version){
		Injector injector = Guice.createInjector(new PersistenceConfigurationModule());
		IContentManager contentPersistenceManager = injector.getInstance(IContentManager.class);
		
		List<String> availableVersions = contentPersistenceManager.listAvailableVersions();
		
		if(null == liveVersion || liveVersion.equals(""))
			liveVersion = availableVersions.get(0);
		
		if(!availableVersions.contains(version))
			return Response.ok().entity("Invalid version selected").build();
		else{
			liveVersion = version;
			log.info("Live version of the site changed to: " + version);
		}
			
		return Response.ok().entity("live Version changed to " + version).build();
	}	
}
