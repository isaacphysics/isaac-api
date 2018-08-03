/*
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

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import com.google.common.collect.Sets;
import com.google.inject.name.Named;
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
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang3.EnumUtils;
import org.jboss.resteasy.annotations.GZIP;

import com.google.api.client.util.Lists;
import com.google.api.client.util.Maps;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import uk.ac.cam.cl.dtg.segue.api.managers.SegueResourceMisuseException;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAccountManager;
import uk.ac.cam.cl.dtg.segue.api.monitors.EmailVerificationMisuseHandler;
import uk.ac.cam.cl.dtg.segue.api.monitors.EmailVerificationRequestMisuseHandler;
import uk.ac.cam.cl.dtg.segue.api.monitors.IMisuseMonitor;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.InvalidTokenException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.MissingRequiredFieldException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserLoggedInException;
import uk.ac.cam.cl.dtg.segue.comm.CommunicationException;
import uk.ac.cam.cl.dtg.segue.comm.EmailCommunicationMessage;
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
import uk.ac.cam.cl.dtg.segue.dto.content.EmailTemplateDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import static uk.ac.cam.cl.dtg.segue.api.Constants.SegueLogType;
import static uk.ac.cam.cl.dtg.segue.api.Constants.CONTENT_INDEX;

/**
 * An email facade front end.
 *
 * @author Alistair Stead
 *
 */
@Path("/")
@Api(value = "/email")
public class EmailFacade extends AbstractSegueFacade {
    private static final Logger log = LoggerFactory.getLogger(EmailFacade.class);
    
    private final EmailManager emailManager;
    private final UserAccountManager userManager;
    private final IContentManager contentManager;
    private final String contentIndex;
    private final IMisuseMonitor misuseMonitor;

    /**
     * EmailFacade. This class is responsible for orchestrating e-mail operations
     * 
     * @param properties
     *            - global properties loader
     * @param logManager
     *            - log manager
     * @param emailManager
     *            - class responsible for sending e-mail
     * @param userManager
     *            - so we can look up users and verify permissions..
     * @param contentManager
     *            - so we can look up email to send.
     * @param misuseMonitor
     *            - misuse monitor.
     */
    @Inject
    public EmailFacade(final PropertiesLoader properties, final ILogManager logManager,
            final EmailManager emailManager, final UserAccountManager userManager,
                       final IContentManager contentManager, @Named(CONTENT_INDEX) final String contentIndex,
                       final IMisuseMonitor misuseMonitor) {
		super(properties, logManager);
        this.contentManager = contentManager;
        this.contentIndex = contentIndex;
		this.emailManager = emailManager;
		this.userManager = userManager;
        this.misuseMonitor = misuseMonitor;
	}
    
    
    /**
     * Get the number of emails left on the queue.
     * 
     * This method will return the current number of emails left on the email queue
     *
     * @param request
     * 			- the request 
     * @return the size of the queue
     */
    @GET
    @Path("/email/queuesize")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    public final Response getEmailQueueSize(@Context final HttpServletRequest request) {
        try {
            if (!isUserAnAdmin(userManager, request)) {
                return new SegueErrorResponse(Status.FORBIDDEN, "You must be an admin to access this endpoint.").toResponse();
            }

            ImmutableMap<String, Integer> response = ImmutableMap.of("length", this.emailManager.getQueueLength());
            return Response.ok(response).build();
        } catch (NoUserLoggedInException e2) {
            return SegueErrorResponse.getNotLoggedInResponse();
        }
    }
    
    /**
     * GetEmailInBrowserById from the database.
     * 
     * This method will return serialised html that displays an email object
     * 
     * @param request
     *            - so that we can allow only logged in users to view their own data.
     * @param id
     *            - the id of the content     
     * @return Response object containing the serialized content object. (with no levels of recursion into the content)
     */
    @GET
    @Path("/email/viewinbrowser/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    public final Response getEmailInBrowserById(@Context final HttpServletRequest request,
            @PathParam("id") final String id) {
    	
    	RegisteredUserDTO currentUser;
		try {
			currentUser = this.userManager.getCurrentRegisteredUser(request);			
		} catch (NoUserLoggedInException e2) {
    		return SegueErrorResponse.getNotLoggedInResponse();
		}

        ContentDTO c;

        // Deserialize object into POJO of specified type, provided one exists.
        try {
            c = this.contentManager.getContentById(this.contentManager.getCurrentContentSHA(), id);

            if (null == c) {
                SegueErrorResponse error = new SegueErrorResponse(Status.NOT_FOUND, "No content found with id: " + id);
                log.info(error.getErrorMessage());
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
        
        EmailTemplateDTO emailTemplateDTO;

        if (c instanceof EmailTemplateDTO) {
            emailTemplateDTO = (EmailTemplateDTO) c;
        } else {
            SegueErrorResponse error = new SegueErrorResponse(Status.NOT_FOUND, "Content is of incorrect type: " + id);
            log.debug(error.getErrorMessage());
            return error.toResponse();
        }
        
		try {
            Properties previewProperties = new Properties();
            // Add all properties in the user DTO (preserving types) so they are available to email templates.
            Map userPropertiesMap = new org.apache.commons.beanutils.BeanMap(currentUser);
            previewProperties.putAll(emailManager.flattenTokenMap(userPropertiesMap, Maps.newHashMap(), ""));

            //TODO: backwards compat - fix content so that case is correct.
            previewProperties.put("givenname", currentUser.getGivenName() == null ? "" : currentUser.getGivenName());
            previewProperties.put("familyname", currentUser.getFamilyName() == null ? "" : currentUser.getFamilyName());

            EmailCommunicationMessage ecm = this.emailManager.constructMultiPartEmail(currentUser.getId(),
                    currentUser.getEmail(), emailTemplateDTO, previewProperties, EmailType.SYSTEM);

            HashMap<String, String> previewMap = Maps.newHashMap();
            previewMap.put("subject", emailTemplateDTO.getSubject());
			previewMap.put("html", ecm.getHTMLMessage());
			previewMap.put("plainText", ecm.getPlainTextMessage());

			return Response.ok(previewMap).build();
		} catch (ResourceNotFoundException e) {
            SegueErrorResponse error = new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, 
            						"Content could not be found: " + id);
            log.warn(error.getErrorMessage());
            return error.toResponse();
		} catch (ContentManagerException e) {
            SegueErrorResponse error = new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, 
            						"Content is of incorrect type: " + id);
            log.debug(error.getErrorMessage());
            return error.toResponse();
		} catch (IllegalArgumentException e) {
	        SegueErrorResponse error = new SegueErrorResponse(Status.BAD_REQUEST, 
	        						"Cannot generate email with non-authorised fields: " + id);
	        log.info(error.getErrorMessage());
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
    @Path("/email/preferences")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    public final Response getEmailTypes(@Context final HttpServletRequest request) {
    	EmailType [] types = EmailType.values();
    	List<Map<String, Object>> resultList = Lists.newArrayList();
    	for (EmailType type : types) {
    		if (type.isValidEmailPreference()) {
    			HashMap<String, Object> map = new HashMap<>();
    			map.put("name", type.toString());
    			resultList.add(map);
    		}
    	}    	
    	
		return Response.ok(resultList).build();
    }
    
    /**
     * End point that verifies whether or not a validation token is valid.
     * 
     * @param userId
     *            - the user's id.
     * @param token
     *            - A password reset token
     * @return Success if the token is valid, otherwise returns not found
     */
    @GET
    @Path("/users/verifyemail/{userid}/{token}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    public Response validateEmailVerificationRequest(@PathParam("userid") final Long userId,
                                                     @PathParam("token") final String token) {

        try {
            misuseMonitor.notifyEvent(userId.toString(), EmailVerificationMisuseHandler.class.toString());
            userManager.processEmailVerification(userId, token);

            // assume that if there are no exceptions that it worked.
            return Response.ok().build();
        } catch (SegueResourceMisuseException e) {
            return SegueErrorResponse
                    .getRateThrottledResponse("You have exceeded the number of requests allowed for this endpoint");
        } catch (InvalidTokenException | NoUserException e) {
            SegueErrorResponse error = new SegueErrorResponse(Status.BAD_REQUEST, "Token invalid or expired.");
            log.error("Invalid email verification token received", e.toString());
            return error.toResponse();
        } catch (SegueDatabaseException e) {
            SegueErrorResponse error = new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR,
                    "There was an error processing your request.");
            log.error("Invalid email token request");
            return error.toResponse();
        }
    }

    /**
     * End point that allows a user to generate an email verification request.
     * 
     * @param payload
     *            - Post request payload containing the email address requested for verification
     * @param request
     *            - For logging purposes.
     * @return a successful response regardless of whether the email exists or an error code if there is a technical
     *         fault
     */
    @POST
    @Path("/users/verifyemail")
    @Consumes(MediaType.APPLICATION_JSON)
    @GZIP
    public Response generateEmailVerificationToken(@Context final HttpServletRequest request,
                                                   final Map<String, String> payload) {
        try {
            String email = payload.get("email");
            if (email == null || email.isEmpty()) {
                throw new MissingRequiredFieldException("No email provided with request");
            }

            misuseMonitor.notifyEvent(email, EmailVerificationRequestMisuseHandler.class.toString());

            userManager.emailVerificationRequest(request, email);

            this.getLogManager().logEvent(userManager.getCurrentUser(request), request,
                    SegueLogType.EMAIL_VERIFICATION_REQUEST_RECEIVED,
                    ImmutableMap.of(Constants.LOCAL_AUTH_EMAIL_VERIFICATION_TOKEN_FIELDNAME, email));

            return Response.ok().build();

        } catch (CommunicationException | NoSuchAlgorithmException | InvalidKeySpecException | SegueDatabaseException e) {
            SegueErrorResponse error = new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR,
                    "Error sending verification message.", e);
            log.error(error.getErrorMessage(), e);
            return error.toResponse();
        } catch (MissingRequiredFieldException | NumberFormatException e) {
            SegueErrorResponse error = new SegueErrorResponse(Status.BAD_REQUEST, e.getMessage());
            log.error(String.format("Invalid parameters sent to /users/verifyemail endpoint: (%s)", e.getMessage()));
            return error.toResponse();
        } catch (SegueResourceMisuseException e) {
            String message = "You have exceeded the number of requests allowed for this endpoint. "
                    + "Please try again later.";
            log.error(String.format("VerifyEmail request endpoint has reached hard limit (%s)",
                    payload.get("email")));
            return SegueErrorResponse.getRateThrottledResponse(message);
        }
    }

    /**
     * SendEmails
     *
     * Send emails to all users of specified roles if their email preferences allow it.
     *
     * @param request
     *            - so that we can allow only logged in users to view their own data.
     * @param contentId
     *            - of the e-mail to send
     * @param emailTypeString
     *            - the type of e-mail that is being sent.
     * @param roles
     *            - string of user roles to boolean (i.e. whether or not to send to this type)
     * @return Response object containing the serialized content object. (with no levels of recursion into the content)
     */
    @POST
    @Path("/email/sendemail/{contentid}/{emailtype}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @GZIP
    public final Response sendEmails(@Context final HttpServletRequest request,
		    		@PathParam("contentid") final String contentId, 
		    		@PathParam("emailtype") final String emailTypeString,
		    		final Map<String, Boolean> roles) {
        EmailType emailType;
        List<RegisteredUserDTO> allSelectedUsers = Lists.newArrayList();

        if (EnumUtils.isValidEnum(EmailType.class, emailTypeString)) {
            emailType = EmailType.valueOf(emailTypeString);
        } else {
            log.warn("Unknown email type '" + emailTypeString + "' provided to admin endpoint!");
            return new SegueErrorResponse(Status.BAD_REQUEST, "Unknown email type!").toResponse();
        }
		
		try {
            RegisteredUserDTO sender = this.userManager.getCurrentRegisteredUser(request);
            if (!isUserAnAdmin(userManager, request)) {
                return SegueErrorResponse.getIncorrectRoleResponse();
            }

    		for (String key : roles.keySet()) {
				RegisteredUserDTO prototype = new RegisteredUserDTO();
				List<RegisteredUserDTO> selectedUsers = Lists.newArrayList();
    			
                Role inferredRole = Role.valueOf(key);
                Boolean userGroupSelected = roles.get(key);

                if (null == userGroupSelected || !userGroupSelected) {
                    continue;
                }

                prototype.setRole(inferredRole);
                selectedUsers = this.userManager.findUsers(prototype);
                allSelectedUsers.addAll(selectedUsers);
            }
    		
    		if (allSelectedUsers.size() == 0) {
                SegueErrorResponse error = new SegueErrorResponse(Status.BAD_REQUEST,
                        "There are no users in the groups you have selected!.");
    			log.error(error.getErrorMessage());
    			return error.toResponse();
    		}
    		
			emailManager.sendCustomEmail(sender, contentId, allSelectedUsers, emailType);
		} catch (SegueDatabaseException e) {
            SegueErrorResponse error = new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR,
                    "There was an error processing your request.");
			log.error(error.getErrorMessage());
			return error.toResponse();
        } catch (IllegalArgumentException e) {
            SegueErrorResponse error = new SegueErrorResponse(Status.BAD_REQUEST,
                    "An unknown type of role was supplied.");
            log.debug(error.getErrorMessage());
        } catch (ContentManagerException e) {
            SegueErrorResponse error = new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR,
                    "There was an error retrieving content.");
			log.debug(error.getErrorMessage());
		} catch (NoUserLoggedInException e2) {
            return SegueErrorResponse.getNotLoggedInResponse();
        }
    	
		return Response.ok().build();
    }

    /**
     * sendemailwithuserids allows sending an email to a given list of userids.
     * 
     * This method will return serialised html that displays an email object
     * 
     * @param request
     *            - so that we can allow only logged in users to view their own data.
     * @param contentId
     *            - of the e-mail to send
     * @param emailTypeString
     *            - the type of e-mail that is being sent.
     * @param userIds
     *            - list of user ids
     * @return Response object containing the serialized content object. (with no levels of recursion into the content)
     */
    @POST
    @Path("/email/sendemailwithuserids/{contentid}/{emailtype}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @GZIP
    public final Response sendEmailsToUserIds(@Context final HttpServletRequest request,
            @PathParam("contentid") final String contentId, @PathParam("emailtype") final String emailTypeString,
            final List<Long> userIds) {
        EmailType emailType;
        Set<RegisteredUserDTO> allSelectedUsers = Sets.newHashSet();

        if (EnumUtils.isValidEnum(EmailType.class, emailTypeString)) {
            emailType = EmailType.valueOf(emailTypeString);
        } else {
            log.warn("Unknown email type '" + emailTypeString + "' provided to admin endpoint!");
            return new SegueErrorResponse(Status.BAD_REQUEST, "Unknown email type!").toResponse();
        }

        try {
            RegisteredUserDTO sender = this.userManager.getCurrentRegisteredUser(request);
            if (!isUserAnAdmin(userManager, request)) {
                return SegueErrorResponse.getIncorrectRoleResponse();
            }

            for (Long userId : userIds) {
                RegisteredUserDTO userDTO = this.userManager.getUserDTOById(userId);
                if (userDTO != null) {
                    allSelectedUsers.add(userDTO);
                } else {
                    log.error(String.format("Skipping - User could not be found from given userId: %s", userId));
                }
            }

            if (allSelectedUsers.size() == 0) {
                SegueErrorResponse error = new SegueErrorResponse(Status.BAD_REQUEST,
                        "There are no users in the groups you have selected!.");
                log.error(error.getErrorMessage());
                return error.toResponse();
            }

            emailManager.sendCustomEmail(sender, contentId, new ArrayList<>(allSelectedUsers), emailType);
        } catch (NoUserException e) {
            SegueErrorResponse error = new SegueErrorResponse(Status.BAD_REQUEST,
                    "One or more userId(s) did not map to a valid user!.");
            log.error(error.getErrorMessage());
            return error.toResponse();
        } catch (SegueDatabaseException e) {
            SegueErrorResponse error = new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR,
                    "There was an error processing your request.");
            log.error(error.getErrorMessage());
            return error.toResponse();
        } catch (IllegalArgumentException e) {
            SegueErrorResponse error = new SegueErrorResponse(Status.BAD_REQUEST,
                    "An unknown type of user was supplied.");
            log.debug(error.getErrorMessage());
        } catch (ContentManagerException e) {
            SegueErrorResponse error = new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR,
                    "There was an error retrieving content.");
            log.debug(error.getErrorMessage());
        } catch (NoUserLoggedInException e2) {
            return SegueErrorResponse.getNotLoggedInResponse();
        }

        return Response.ok().build();
    }
}
