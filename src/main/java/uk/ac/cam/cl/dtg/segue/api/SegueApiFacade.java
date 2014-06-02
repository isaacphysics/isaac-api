package uk.ac.cam.cl.dtg.segue.api;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.jboss.resteasy.annotations.cache.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.dtg.segue.dao.ContentMapper;
import uk.ac.cam.cl.dtg.segue.dao.IContentManager;
import uk.ac.cam.cl.dtg.segue.dao.ILogManager;
import uk.ac.cam.cl.dtg.segue.dto.Content;
import uk.ac.cam.cl.dtg.segue.dto.User;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.google.api.client.util.Maps;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;

@Path("/")
public class SegueApiFacade {
	private static final Logger log = LoggerFactory.getLogger(SegueApiFacade.class);

	// TODO Perhaps stored in Mongo? Should this be an app setting or API one?
	private volatile static String liveVersion;
	
	private static ContentMapper mapper;
	
	private PropertiesLoader properties;

	/**
	 * Constructor that allows pre-configuration of the segue api. 
	 * 
	 * @param segueConfigurationModule
	 */
	@Inject
	public SegueApiFacade(PropertiesLoader properties, ContentMapper mapper, @Nullable ISegueDTOConfigurationModule segueConfigurationModule){
		this.properties = properties;
		
		// we want to make sure we have set a default liveVersion number
		if(null == liveVersion){
			log.info("Setting live version of the site from properties file to " + Constants.INITIAL_LIVE_VERSION);
			liveVersion = this.properties.getProperty(Constants.INITIAL_LIVE_VERSION);		
		}
		
		// We only want to do this if the mapper needs to be changed - I expect the same instance to be injected from Guice each time.
		if(SegueApiFacade.mapper != mapper){
			SegueApiFacade.mapper = mapper;

			// Add client specific data structures to the set of managed DTOs.
			if(null != segueConfigurationModule){
				SegueApiFacade.mapper.registerJsonTypes(segueConfigurationModule.getContentDataTransferObjectMap());
			}
		}

		// Check if we want to get the latest from git each time a request is made from segue. - Will add overhead
		if(Boolean.parseBoolean(this.properties.getProperty(Constants.FOLLOW_GIT_VERSION))){
			this.synchroniseDataStores();
		}
	}

//	@POST
//	@Path("log")
//	@Produces("application/json")
	public ImmutableMap<String, Boolean> postLog(
			@Context HttpServletRequest req,
			@FormParam("sessionId") String sessionId,
			@FormParam("cookieId") String cookieId,
			@FormParam("event") String eventJSON) {

		Injector injector = Guice.createInjector(new SegueGuiceConfigurationModule());
		ILogManager logPersistenceManager = injector.getInstance(ILogManager.class);

		boolean success = logPersistenceManager.log(sessionId, cookieId, eventJSON);

		return ImmutableMap.of("success", success);
	}	

	/**
	 * This method specifically uses mongodb to save content objects
	 * 
	 * @deprecated content objects are no longer saved in mongodb
	 * @param docJson
	 * @return
	 */
//	@POST
//	@Produces("application/json")
//	@Path("content/save")
//	@Deprecated
	public Response contentSave(@FormParam("doc") String docJson) {
		Injector injector = Guice.createInjector(new SegueGuiceConfigurationModule());
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
	 * Routing endpoint: this method will either return results from one of the following:
	 * getContentByTags
	 * getContentByType
	 * 
	 * @param version
	 * @return Response object containing the list of content objects.
	 */
	@GET
	@Produces("application/json")
	@Path("content/{version}")
	public Response getContentList(@PathParam("version") String version, @QueryParam("tags") String tags, 
			@QueryParam("type") String type, @QueryParam("start_index") String startIndex, @QueryParam("limit") String limit){

		Map<String, List<String>> fieldsToMatch = Maps.newHashMap();
		
		fieldsToMatch.put("type", Arrays.asList(type));
		fieldsToMatch.put("tags", Arrays.asList(tags));
		
		List<Content> c = (List<Content>) this.findMatchingContent(version, fieldsToMatch, startIndex, limit).getEntity();
		
		return Response.ok().entity(c).build();
	}

	/**
	 * This method will return a List<Content> based on the parameters supplied. 
	 *  
	 * @param version
	 * @param fieldsToMatch - Map representing fieldName -> field value mappings to search for. Note: tags is a special field name and the list will be split by commas.
	 * @param startIndex
	 * @param limit
	 * @return Response containing a list of content or a Response containing null if none found. 
	 */
	public Response findMatchingContent(String version, Map<String,List<String>> fieldsToMatch, String startIndex, String limit){
		Injector injector = Guice.createInjector(new SegueGuiceConfigurationModule());
		IContentManager contentPersistenceManager = injector.getInstance(IContentManager.class);

		if(null == version)
			version = SegueApiFacade.liveVersion;
		
		if(null==limit){
			limit = Constants.DEFAULT_SEARCH_LIMIT;
		}
		
		if(null == startIndex){
			startIndex = "0";
		}

		List<Content> c = null;

		// Deserialize object into POJO of specified type, providing one exists. 
		try{
			log.info("Finding all content from the api with fields: " + fieldsToMatch);
			
			c = contentPersistenceManager.findByFieldNames(version, fieldsToMatch, Integer.parseInt(startIndex), Integer.parseInt(limit));
		}
		catch(IllegalArgumentException e){
			log.error("Unable to map content object.", e);
			return Response.serverError().entity(e).build();
		}
		

		return Response.ok().entity(c).build();
	}
	
	/**
	 * GetContentById from the database
	 * 
	 * Currently this method will return a single Json Object containing all of the fields available to the object retrieved from the database.
	 * 
	 * @param version - the version of the datastore to query
	 * @param id - our id not the dbid
	 * @return Response object containing the serialized content object. (with no levels of recursion into the content)
	 */
	@GET
	@Produces("application/json")
	@Path("content/{version}/{id}")
	public Response getContentById(@PathParam("version") String version, @PathParam("id") String id) {		
		Injector injector = Guice.createInjector(new SegueGuiceConfigurationModule());
		IContentManager contentPersistenceManager = injector.getInstance(IContentManager.class);
		
		if(null == version)
			version = SegueApiFacade.liveVersion;
		
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
	 * This method provides a set of all tags for a given version of the content.
	 * 
	 * @return a version info as json response
	 */
	@GET
	@Produces("application/json")
	@Path("content/tags")
	public Response getTagListByLiveVersion(){
		return this.getTagListByVersion(liveVersion);
	}		
	
	/**
	 * This method provides a set of all tags for a given version of the content.
	 * @param version of the site to provide the tag list from.
	 * @return a version info as json response
	 */
	@GET
	@Produces("application/json")
	@Path("content/tags/{version}")
	public Response getTagListByVersion(@PathParam("version") String version){
		Injector injector = Guice.createInjector(new SegueGuiceConfigurationModule());
		IContentManager contentPersistenceManager = injector.getInstance(IContentManager.class);
		
		Set<String> tags = contentPersistenceManager.getTagsList(version);
		
		return Response.ok().entity(tags).build();
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
	@Path("content/file_content/{version}/{path:.*}")
	@Cache
	public Response getImageFileContent(@PathParam("version") String version, @PathParam("path") String path) {				
		// TODO check if the content being requested is valid for this api call. e.g. only images?
		if(null == version || null == path || Files.getFileExtension(path).isEmpty()){
			log.info("Bad input to api call. Returning null");
			return Response.serverError().entity(null).build();
		}

		Injector injector = Guice.createInjector(new SegueGuiceConfigurationModule());
		IContentManager gcm = injector.getInstance(IContentManager.class);

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
			log.info("Unable to locate the file " + path);
			return Response.status(Status.NOT_FOUND).build();
		}

		return Response.ok().type(mimeType).entity(fileContent.toByteArray()).build();
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
	@Path("admin/live_version/{version}")
	public synchronized Response changeLiveVersion(@PathParam("version") String version){
		Injector injector = Guice.createInjector(new SegueGuiceConfigurationModule());
		IContentManager contentPersistenceManager = injector.getInstance(IContentManager.class);

		List<String> availableVersions = contentPersistenceManager.listAvailableVersions();

		if(null == liveVersion || liveVersion.equals(""))
			liveVersion = availableVersions.get(0);

		if(!availableVersions.contains(version))
			return Response.status(Status.NOT_FOUND).entity("Invalid version selected").build();
		else{
			liveVersion = version;
			log.info("Live version of the site changed to: " + version);
		}

		return Response.ok().entity("live Version changed to " + version).build();
	}

	/**
	 * This method returns all versions as an immutablemap version_list: []
	 * 
	 * @param This parameter if not null will set the limit of the number entries to return the default is the latest 10 (indices starting at 0).
	 * 
	 * @return a Response containing an immutablemap version_list: [x..y..]
	 */
	@GET
	@Produces("application/json")
	@Path("info/content_versions")
	public Response getVersionsList(@QueryParam("limit") String limit){
		if(null == limit){
			limit = "9";
		}
		
		// try to parse the integer
		Integer limitAsInt = null;
		
		try{
			limitAsInt = Integer.parseInt(limit);
		}
		catch(NumberFormatException e){
			// ignore the parameter
			limitAsInt = 9;
		}
		
		Injector injector = Guice.createInjector(new SegueGuiceConfigurationModule());
		IContentManager contentPersistenceManager = injector.getInstance(IContentManager.class);
		
		List<String> allVersions = contentPersistenceManager.listAvailableVersions();
		List<String> limitedVersions = null;
		try{
			limitedVersions = new ArrayList<String>(allVersions.subList(0, limitAsInt));
		}
		// they have requested a stupid limit so just give them what we have got.
		catch(IndexOutOfBoundsException e){
			limitedVersions = allVersions;
		}
		
		ImmutableMap<String, Collection<String>> result = new ImmutableMap.Builder<String,Collection<String>>()
				.put("version_list", limitedVersions)
				.build();
		
		return Response.ok().entity(result).build();
	}
	
	/**
	 * This method return a json response containing version related information
	 * 
	 * @return a version info as json response
	 */
	@GET
	@Produces("application/json")
	@Path("info/content_versions/live_version")
	public Response getLiveVersionInfo(){
		Injector injector = Guice.createInjector(new SegueGuiceConfigurationModule());
		IContentManager contentPersistenceManager = injector.getInstance(IContentManager.class);
		
		ImmutableMap<String, String> result = new ImmutableMap.Builder<String,String>()
				.put("live_version",liveVersion)
				.put("latest_known_version", contentPersistenceManager.getLatestVersionId())
				.build();
		
		return Response.ok().entity(result).build();
	}
	
	/**
	 * This method return a json response containing version related information
	 * 
	 * @return a version info as json response
	 */
	@GET
	@Produces("application/json")
	@Path("info/content_versions/cached")
	public Response getCachedVersions(){
		Injector injector = Guice.createInjector(new SegueGuiceConfigurationModule());
		IContentManager contentPersistenceManager = injector.getInstance(IContentManager.class);
		
		ImmutableMap<String, Collection<String>> result = new ImmutableMap.Builder<String,Collection<String>>()
				.put("cached_versions", contentPersistenceManager.getCachedVersionList())
				.build();
		
		return Response.ok().entity(result).build();
	}
	
	public String getLiveVersion(){
		// Check if we want to get the latest from git each time a request is made from segue. - Will add overhead
		if(Boolean.parseBoolean(this.properties.getProperty(Constants.FOLLOW_GIT_VERSION))){
			this.synchroniseDataStores();
		}
		return liveVersion;
	}

	/**
	 * Get the details of the currently logged in user
	 * TODO: test me
	 * 
	 * @return Returns the current user DTO if we can get it or null if we can't
	 */
	public User getCurrentUser(HttpServletRequest request){
		Injector injector = Guice.createInjector(new SegueGuiceConfigurationModule());
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
		Injector injector = Guice.createInjector(new SegueGuiceConfigurationModule());
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
		Injector injector = Guice.createInjector(new SegueGuiceConfigurationModule());
		UserManager userManager = injector.getInstance(UserManager.class);

		userManager.authenticateCallback(request, response, signinProvider);

		String returnUrl = this.properties.getProperty(Constants.HOST_NAME) + this.properties.getProperty(Constants.DEFAULT_LANDING_URL_SUFFIX);
		
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
		Injector injector = Guice.createInjector(new SegueGuiceConfigurationModule());
		UserManager userManager = injector.getInstance(UserManager.class);

		userManager.logUserOut(request);
		
		String returnUrl = this.properties.getProperty(Constants.HOST_NAME) + this.properties.getProperty(Constants.DEFAULT_LANDING_URL_SUFFIX);
		// TODO: make less hacky
		return Response.temporaryRedirect(URI.create(returnUrl)).build();
	}

	/**
	 * This method will try to bring the live version that Segue is using to host content up-to-date with the latest in the git remote.
	 * @return
	 */
	@POST
	@Produces("application/json")
	@Path("admin/synchronise_datastores")
	public synchronized Response synchroniseDataStores(){
		Injector injector = Guice.createInjector(new SegueGuiceConfigurationModule());
		IContentManager contentPersistenceManager = injector.getInstance(IContentManager.class);
		
		String newVersion = contentPersistenceManager.getLatestVersionId();
		
		log.debug("Checking for content change - Synchronizing with Git.");
		
		if(!newVersion.equals(liveVersion)){
			log.info("Changing live version to be " + newVersion + " from " + liveVersion);
			liveVersion = newVersion;
			
			// TODO: come up with a better cache eviction strategy without random magic numbers.
			if(contentPersistenceManager.getCachedVersionList().size() > 9){
				List<String> allVersions = contentPersistenceManager.listAvailableVersions();
				log.info("Cache full finding and deleting old versions");
				// got through all versions in reverse until you find the oldest one that is also in the cached versions list and then remove it.
				for(int i = allVersions.size()-1; contentPersistenceManager.getCachedVersionList().size() > 9; i--){
					if(contentPersistenceManager.getCachedVersionList().contains(allVersions.get(i))){
						log.info("Requesting to delete the content at version " + allVersions.get(i) + " from the cache.");
						contentPersistenceManager.clearCache(allVersions.get(i));
					}
				}
			}
			else
			{
				log.info("Not evicting cache as we have enough space.");
			}
		}
		else
		{
			log.debug("No change to live version required.");
		}

		return Response.ok(newVersion).build();
	}
	
	/**
	 * This method will try to bring the live version that Segue is using to host content up-to-date with the latest in the git remote.
	 * @return
	 */
	@POST
	@Produces("application/json")
	@Path("admin/clear_caches")
	public synchronized Response clearCaches(){
		Injector injector = Guice.createInjector(new SegueGuiceConfigurationModule());
		IContentManager contentPersistenceManager = injector.getInstance(IContentManager.class);
		
		String newVersion = contentPersistenceManager.getLatestVersionId();
		
		log.info("Clearing all caches...");
		
		contentPersistenceManager.clearCache();

		return Response.ok(newVersion).build();
	}
	

	/**
	 * Search the content manager for some search string
	 * 
	 */
	@GET
	@Produces("application/json")
	@Path("search/{searchString}")
	public Response search(@PathParam("searchString") String searchString){			
		Injector injector = Guice.createInjector(new SegueGuiceConfigurationModule());
		IContentManager contentManager = injector.getInstance(IContentManager.class);
		
		List<Content> searchResults = contentManager.searchForContent(liveVersion, searchString);
		//TODO: we probably only want to return summaries of content objects?
		
		return Response.ok(searchResults).build();
	}
}
