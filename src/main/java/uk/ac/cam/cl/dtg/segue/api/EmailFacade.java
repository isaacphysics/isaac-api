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

import io.swagger.annotations.Api;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.jboss.resteasy.annotations.GZIP;

import com.google.inject.Inject;

import uk.ac.cam.cl.dtg.segue.api.managers.ContentVersionController;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserLoggedInException;
import uk.ac.cam.cl.dtg.segue.comm.EmailManager;
import uk.ac.cam.cl.dtg.segue.dao.ILogManager;
import uk.ac.cam.cl.dtg.segue.dao.ResourceNotFoundException;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dao.content.IContentManager;
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
    private ContentVersionController versionManager;
    private final SegueApiFacade api;
    private static final Logger log = LoggerFactory.getLogger(EmailFacade.class);

	/**
	 * TODO Comment Here
	 * @param properties
	 * @param logManager
	 */
    @Inject
	public EmailFacade(final SegueApiFacade api, final PropertiesLoader properties, final ILogManager logManager, 
					final EmailManager emailManager, final ContentVersionController contentVersionController) {
		super(properties, logManager);
		this.api = api;
		this.versionManager = contentVersionController;
		this.emailManager = emailManager;
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
			return Response.ok(htmlTemplatePreview).build();
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


}
