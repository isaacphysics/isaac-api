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
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.dtg.segue.dao.ContentMapper;
import uk.ac.cam.cl.dtg.segue.dao.GitContentManager;
import uk.ac.cam.cl.dtg.segue.dao.IContentManager;
import uk.ac.cam.cl.dtg.segue.dao.ILogManager;
import uk.ac.cam.cl.dtg.segue.database.GitDb;
import uk.ac.cam.cl.dtg.segue.database.PersistenceConfigurationModule;
import uk.ac.cam.cl.dtg.segue.dto.Content;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Guice;
import com.google.inject.Injector;


@Path("/")
public class SegueApiFacade {
	private static final Logger log = LoggerFactory.getLogger(SegueApiFacade.class);

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
	 * GetContentBy Id from the database
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
			c = contentPersistenceManager.getById(id);
		}
		catch(IllegalArgumentException e){
			log.error("Unable to map content object.", e);
			return Response.serverError().entity(e).build();
		}
		
		return Response.ok().entity(c).build();
	}
	
	/**
	 * GetContentBy filename from the git database
	 * 
	 * Currently this method will return a single Json Object containing all of the fields available to the object retrieved from the database.
	 * 
	 * @param id - our id not the dbid
	 * @return Response object containing the serialized content object. (with no levels of recursion into the content)
	 */
	@GET
	@Produces("application/json")
	@Path("content/fromGit/{sha}/{id}")
	public Response getFromGit(@PathParam("sha") String sha, @PathParam("id") String id) {		
		Content c = null;
		try {
			GitDb gdb = new GitDb("c:\\rutherford-test\\.git");
			GitContentManager gcm = new GitContentManager(gdb);
			gcm.buildGitIndex(sha);
			
			c = gcm.getById(id);
			
			//b = gdb.getFileByCommitSHA(sha, filename);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			log.error("Unable to locate Git repo.");
		}
		if (null == c){
			return Response.ok().entity("No results found for your search").build();
		}
		return Response.ok().entity(c).build();
	}	
	
	@GET
	@Produces("application/json")
	@Path("content/getAllContentByType/{type}")
	public Response getAllContentByType(String type, Integer limit){
		Injector injector = Guice.createInjector(new PersistenceConfigurationModule());
		IContentManager contentPersistenceManager = injector.getInstance(IContentManager.class);
		
		List<Content> c = null;
		
		// Deserialize object into POJO of specified type, providing one exists. 
		try{
			log.info("Finding all concepts from the api.");
			c = contentPersistenceManager.findAllByType(type, limit);
		}
		catch(IllegalArgumentException e){
			log.error("Unable to map content object.", e);
			return Response.serverError().entity(e).build();
		}
		
		return Response.ok().entity(c).build();
	}
}
