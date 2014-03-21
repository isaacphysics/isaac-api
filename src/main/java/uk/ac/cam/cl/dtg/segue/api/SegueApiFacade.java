package uk.ac.cam.cl.dtg.segue.api;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.annotations.cache.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.dtg.segue.dao.ContentMapper;
import uk.ac.cam.cl.dtg.segue.dao.GitContentManager;
import uk.ac.cam.cl.dtg.segue.dao.IContentManager;
import uk.ac.cam.cl.dtg.segue.dao.ILogManager;
import uk.ac.cam.cl.dtg.segue.database.SeguePersistenceConfigurationModule;
import uk.ac.cam.cl.dtg.segue.dto.Content;
import uk.ac.cam.cl.dtg.segue.dto.User;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

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
	private static String liveVersion;
	private static Date dateOfVersionChange;

	/**
	 * Default constructor used when the default configuration is good enough and we don't need to give segue new dtos to handle
	 */
	public SegueApiFacade(){
		if(null == liveVersion){
			Injector injector = Guice.createInjector(new SeguePersistenceConfigurationModule());
			liveVersion = injector.getInstance(PropertiesLoader.class).getProperty(Constants.INITIAL_LIVE_VERSION);		
			dateOfVersionChange = new Date();
		}
	}

	/**
	 * Constructor that allows pre-configuration of the segue api. 
	 * 
	 * @param segueConfigurationModule
	 */
	public SegueApiFacade(ISegueConfigurationModule segueConfigurationModule){
		// we want to make sure we have set a default liveVersion number
		this();
		
		Injector injector = Guice.createInjector(new SeguePersistenceConfigurationModule());

		ContentMapper mapper = injector.getInstance(ContentMapper.class);
		mapper.getJsonTypes().putAll(segueConfigurationModule.getContentDataTransferObjectMap());

		// TODO: for dev purposes everytime we start segue we want to get the latest version
		this.synchroniseDataStores();
	}

	@POST
	@Path("log")
	@Produces("application/json")
	public ImmutableMap<String, Boolean> postLog(
			@Context HttpServletRequest req,
			@FormParam("sessionId") String sessionId,
			@FormParam("cookieId") String cookieId,
			@FormParam("event") String eventJSON) {

		Injector injector = Guice.createInjector(new SeguePersistenceConfigurationModule());
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
		Injector injector = Guice.createInjector(new SeguePersistenceConfigurationModule());
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
		Injector injector = Guice.createInjector(new SeguePersistenceConfigurationModule());
		IContentManager contentPersistenceManager = injector.getInstance(IContentManager.class);

		Content c = null;

		// Deserialize object into POJO of specified type, providing one exists. 
		try{
			c = contentPersistenceManager.getById(id, SegueApiFacade.liveVersion);

			if (null == c){
				log.debug("No content found with id: " + id);
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
	@Cache
	public Response getFileContent(@PathParam("version") String version, @PathParam("path") String path) {				
		// TODO check if the content being requested is valid for this api call. e.g. only images?
		if(null == version || null == path || Files.getFileExtension(path).isEmpty()){
			log.info("Bad input to api call. Returning null");
			return Response.serverError().entity(null).build();
		}

		Injector injector = Guice.createInjector(new SeguePersistenceConfigurationModule());
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
		Injector injector = Guice.createInjector(new SeguePersistenceConfigurationModule());
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
	@PUT
	@Produces("application/json")
	@Path("admin/changeLiveVersion/{version}")
	public synchronized Response changeLiveVersion(@PathParam("version") String version){
		Injector injector = Guice.createInjector(new SeguePersistenceConfigurationModule());
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

	/**
	 * This method will get the version id of the site that is currently set to be used as the live one.  
	 * 
	 * @return a version id
	 */
	@GET
	@Produces("application/json")
	@Path("admin/getLiveVersion")
	public Response getLiveVersion(){			
		return Response.ok().entity(liveVersion).build();
	}	

	/**
	 * Get the details of the currently logged in user
	 * TODO: test me
	 * 
	 * @return Returns the current user DTO if we can get it or null if we can't
	 */
	public User getCurrentUser(HttpServletRequest request){
		Injector injector = Guice.createInjector(new SeguePersistenceConfigurationModule());
		UserManager userManager = injector.getInstance(UserManager.class);
		//TODO: Make the underlying code more efficient?
		return userManager.getCurrentUser(request);
	}

	/**
	 * This is the initial step of the authentication process.
	 * 
	 * @param request
	 * @param signinProvider - string representing the supported auth provider so that we know who to redirect the user to.
	 * @return Redirect response to the auth providers site.
	 */
	@GET
	@Produces("application/json")
	@Path("auth/{provider}/authenticate")
	public Response authenticationInitialisation(@Context HttpServletRequest request, @PathParam("provider") String signinProvider){
		Injector injector = Guice.createInjector(new SeguePersistenceConfigurationModule());
		UserManager userManager = injector.getInstance(UserManager.class);

		User currentUser = getCurrentUser(request);

		if(null != currentUser){
			return Response.ok().entity(currentUser).build();
		}

		// ok we need to hand over to user manager
		return userManager.authenticate(request, signinProvider);

	}

	/**
	 * This is the callback url that auth providers should use to send us information about users.
	 * 
	 * @param request
	 * @param response
	 * @param signinProvider
	 * @return Redirect?
	 */
	@GET
	@Produces("application/json")
	@Path("auth/{provider}/callback")
	public Response authenticationCallback(@Context HttpServletRequest request, @Context HttpServletResponse response, @PathParam("provider") String signinProvider){
		Injector injector = Guice.createInjector(new SeguePersistenceConfigurationModule());
		UserManager userManager = injector.getInstance(UserManager.class);

		userManager.authenticateCallback(request, response, signinProvider);

		String returnUrl = injector.getInstance(PropertiesLoader.class).getProperty(Constants.HOST_NAME) + injector.getInstance(PropertiesLoader.class).getProperty(Constants.DEFAULT_LANDING_URL_SUFFIX);
		
		//TODO: make less hacky
		return Response.temporaryRedirect(URI.create(returnUrl)).build();
	}

	/**
	 * End point that allows the user to logout - i.e. destroy our cookie.
	 * 
	 * @param request
	 * @param response
	 * @return TODO ? do we return to homepage.
	 */
	@GET
	@Produces("application/json")
	@Path("auth/logout")
	public Response userLogout(@Context HttpServletRequest request, @Context HttpServletResponse response){
		Injector injector = Guice.createInjector(new SeguePersistenceConfigurationModule());
		UserManager userManager = injector.getInstance(UserManager.class);

		userManager.logUserOut(request);
		
		String returnUrl = injector.getInstance(PropertiesLoader.class).getProperty(Constants.HOST_NAME) + injector.getInstance(PropertiesLoader.class).getProperty(Constants.DEFAULT_LANDING_URL_SUFFIX);
		// TODO: make less hacky
		return Response.temporaryRedirect(URI.create(returnUrl)).build();
	}

	@POST
	@Produces("application/json")
	@Path("admin/synchroniseDatastores")	
	public Response synchroniseDataStores(){
		Injector injector = Guice.createInjector(new SeguePersistenceConfigurationModule());
		IContentManager contentPersistenceManager = injector.getInstance(IContentManager.class);

		log.info("Notified of content change - Synchronizing with Git.");

		String newVersion = contentPersistenceManager.getLatestVersionId();

		if(newVersion != liveVersion){
			liveVersion = newVersion;
			dateOfVersionChange = new Date();
			// TODO come up with a better cache eviction strategy.
			contentPersistenceManager.clearCache();
		}

		log.info("Changing live version to be " + liveVersion);

		return Response.ok(newVersion).build();
	}

	public Date dateOfVersionChange(){
		return dateOfVersionChange;
	}

}
