/**
 * Copyright 2015 Alistair Stead
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
package uk.ac.cam.cl.dtg.segue.api;

import static uk.ac.cam.cl.dtg.segue.api.Constants.CONTENT_VERSION;
import static uk.ac.cam.cl.dtg.segue.api.Constants.TYPE_FIELDNAME;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.swagger.annotations.Api;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import ma.glasnost.orika.MapperFacade;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.elasticsearch.common.collect.ImmutableMap;
import org.jboss.resteasy.annotations.GZIP;

import com.google.api.client.util.Lists;
import com.google.api.client.util.Maps;
import com.google.inject.Inject;

import uk.ac.cam.cl.dtg.segue.api.managers.ContentVersionController;
import uk.ac.cam.cl.dtg.segue.api.managers.UserManager;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserLoggedInException;
import uk.ac.cam.cl.dtg.segue.comm.EmailManager;
import uk.ac.cam.cl.dtg.segue.comm.EmailType;
import uk.ac.cam.cl.dtg.segue.dao.ILogManager;
import uk.ac.cam.cl.dtg.segue.dao.ResourceNotFoundException;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dao.content.IContentManager;
import uk.ac.cam.cl.dtg.segue.dos.users.Role;
import uk.ac.cam.cl.dtg.segue.dto.SegueErrorResponse;
import uk.ac.cam.cl.dtg.segue.dto.content.ContentDTO;
import uk.ac.cam.cl.dtg.segue.dto.content.SeguePageDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An email facade front end.
 *
 * @author Alistair Stead
 *
 */
@Path("/email")
@Api(value = "/email")
public class EmailFacade extends AbstractSegueFacade {
	
    private EmailManager emailManager;
    private UserManager userManager;
    private ContentVersionController versionManager;
    private final SegueApiFacade api;
    private static final Logger log = LoggerFactory.getLogger(EmailFacade.class);
    private final MapperFacade mapper;

	/**
	 * TODO Comment Here
	 * @param properties
	 * @param logManager
	 */
    @Inject
	public EmailFacade(final SegueApiFacade api, final PropertiesLoader properties, final ILogManager logManager, 
					final EmailManager emailManager, final UserManager userManager, 
				    final MapperFacade mapper, final ContentVersionController contentVersionController) {
		super(properties, logManager);
		this.api = api;
		this.versionManager = contentVersionController;
		this.emailManager = emailManager;
		this.userManager = userManager;
		this.mapper = mapper;
	}
    
    
    /**
     * GetEmailInBrowserById from the database.
     * 
     * This method will returnserialised html that displays an email object
     * 
     * @param request
     *            - so that we can allow only logged in users to view their own data.
     * @param id
     *            - the id of the content     
     * @return Response object containing the serialized content object. (with no levels of recursion into the content)
     */
    @GET
    @Path("viewinbrowser/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    public final Response getEmailInBrowserById(@Context final HttpServletRequest request,
            @PathParam("id") final String id) {
    	
    	//TODO user needs to be logged in. should be admin to view others' info
    	RegisteredUserDTO currentUser;
		try {
			currentUser = this.api.getCurrentUser(request);
		} catch (NoUserLoggedInException e2) {
    		return SegueErrorResponse.getNotLoggedInResponse();
		}
    	
        String newVersion = versionManager.getLiveVersion();


        ContentDTO c = null;

        // Deserialize object into POJO of specified type, providing one exists.
        try {

            IContentManager contentPersistenceManager = versionManager.getContentManager();
            c = contentPersistenceManager.getContentById(newVersion, id);

            if (null == c) {
                SegueErrorResponse error = new SegueErrorResponse(Status.NOT_FOUND, "No content found with id: " + id);
                log.debug(error.getErrorMessage());
                return error.toResponse();
            }

        } catch (IllegalArgumentException e) {
            SegueErrorResponse error = new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR,
                    "Error while trying to map to a content object.", e);
            log.error(error.getErrorMessage(), e);
            return error.toResponse();
        } catch (ContentManagerException e1) {
            SegueErrorResponse error = new SegueErrorResponse(Status.NOT_FOUND, "Error locating the version requested",
                    e1);
            log.error(error.getErrorMessage(), e1);
            return error.toResponse();
        } 
        
        SeguePageDTO segueContentDTO = null;

        if (c instanceof SeguePageDTO) {
            segueContentDTO = (SeguePageDTO) c;
        } else {
            SegueErrorResponse error = new SegueErrorResponse(Status.NOT_FOUND, "Content is of incorrect type: " + id);
            log.debug(error.getErrorMessage());
            return error.toResponse();
        }
        
		try {
			String htmlTemplatePreview = this.emailManager.getHTMLTemplatePreview(segueContentDTO, currentUser);
			
			HashMap<String, String> htmlPreviewMap = Maps.newHashMap();
			htmlPreviewMap.put("html", htmlTemplatePreview);
			return Response.ok(htmlPreviewMap).build();
		} catch (ResourceNotFoundException e) {
            SegueErrorResponse error = new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, 
            						"Content could not be found: " + id);
            log.debug(error.getErrorMessage());
            return error.toResponse();
		} catch (SegueDatabaseException e) {
            SegueErrorResponse error = new SegueErrorResponse(Status.NOT_FOUND, 
            						"SegueDatabaseException during creation of email preview: " + id);
            log.debug(error.getErrorMessage());
            return error.toResponse();
		} catch (ContentManagerException e) {
            SegueErrorResponse error = new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, 
            						"Content is of incorrect type: " + id);
            log.debug(error.getErrorMessage());
            return error.toResponse();
		} catch (IllegalArgumentException e) {
	        SegueErrorResponse error = new SegueErrorResponse(Status.BAD_REQUEST, 
	        						"Cannot generate email with non-authorised fields: " + id);
	        log.debug(error.getErrorMessage());
	        return error.toResponse();
		}

    }
    
    /**
     * GetEmailTypes returns the valid email preferences.
     * 
     * This method will returnserialised html that displays an email object
     * 
     * @param request
     *            - so that we can allow only logged in users to view their own data. 
     * @return Response object containing the serialized content object. (with no levels of recursion into the content)
     */
    @GET
    @Path("preferences")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    public final Response getEmailTypes(@Context final HttpServletRequest request) {
    	EmailType [] types = EmailType.values();
    	List<Map<String, Object>> resultList = Lists.newArrayList();
    	for (EmailType type : types) {
    		if (type.isValidEmailPreference()) {
    			HashMap<String, Object> map = new HashMap<String, Object>();
    			map.put("id", type.mapEmailTypeToInt());
    			map.put("name", type.toString());
    			resultList.add(map);
    		}
    	}    	
    	
		return Response.ok(resultList).build();
    }
    
    
    /**
     * GetEmailTypes returns the valid email preferences.
     * 
     * This method will returnserialised html that displays an email object
     * 
     * @param request
     *            - so that we can allow only logged in users to view their own data. 
     * @return Response object containing the serialized content object. (with no levels of recursion into the content)
     */
    @POST
    @Path("sendemail/{contentid}/{emailtype}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @GZIP
    public final Response sendEmails(@Context final HttpServletRequest request,
		    		@PathParam("contentid") final String contentId, 
		    		@PathParam("emailtype") final Integer emailTypeInt, 
		    		final Map<String, Boolean> users) {

		EmailType emailType = EmailType.mapIntToPreference(emailTypeInt);

		List<RegisteredUserDTO> allSelectedUsers =  Lists.newArrayList();
		
		try {
    		for (String key : users.keySet()) {
				RegisteredUserDTO prototype = new RegisteredUserDTO();
				List<RegisteredUserDTO> selectedUsers = Lists.newArrayList();
    			switch (key) {
	    			case "adminUsers":
	    				if (users.get("adminUsers")) {
	    		    		prototype.setRole(Role.ADMIN);
	    				}
	    				break;
	    			case "eventManagerUsers":
	    				if (users.get("eventManagerUsers")) {
	    		    		prototype.setRole(Role.EVENT_MANAGER);
	    				}
	    				break;
	    			case "studentUsers":
	    				if (users.get("studentUsers")) {
	    		    		prototype.setRole(Role.STUDENT);
	    				}
	    				break;
	    			case "contentEditorUsers":
	    				if (users.get("contentEditorUsers")) {
	    		    		prototype.setRole(Role.CONTENT_EDITOR);
	    				}
	    				break;
	    			case "teacherUsers":
	    				if (users.get("teacherUsers")) {
	    		    		prototype.setRole(Role.TEACHER);
	    				}
	    				break;
	    			case "testerUsers":
	    				if (users.get("testerUsers")) {
	    		    		prototype.setRole(Role.TESTER);
	    				}
	    				break;
					default:
						break;
    			}
    			if (prototype.getRole() != null) {
		    		selectedUsers = this.userManager.findUsers(prototype);
		    		allSelectedUsers.addAll(selectedUsers);
    			}
    		}
    		
			emailManager.sendCustomEmail(contentId, allSelectedUsers, emailType);
		
		} catch (SegueDatabaseException e) {
            SegueErrorResponse error = new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR,
                    "There was an error processing your request.");
			log.debug(error.getErrorMessage());
			return error.toResponse();
		} catch (ContentManagerException e) {
            SegueErrorResponse error = new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR,
                    "There was an error retrieving content.");
			log.debug(error.getErrorMessage());
		}
		
    	
		return Response.ok().build();
    }



}
