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

import com.google.api.client.util.Lists;
import com.google.api.client.util.Maps;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.EnumUtils;
import org.jboss.resteasy.annotations.GZIP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.segue.api.managers.SegueResourceMisuseException;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAccountManager;
import uk.ac.cam.cl.dtg.segue.api.monitors.EmailVerificationMisuseHandler;
import uk.ac.cam.cl.dtg.segue.api.monitors.EmailVerificationRequestMisuseHandler;
import uk.ac.cam.cl.dtg.segue.api.monitors.IMisuseMonitor;
import uk.ac.cam.cl.dtg.segue.api.monitors.SendEmailMisuseHandler;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.InvalidTokenException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.MissingRequiredFieldException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserLoggedInException;
import uk.ac.cam.cl.dtg.segue.comm.EmailCommunicationMessage;
import uk.ac.cam.cl.dtg.segue.comm.EmailManager;
import uk.ac.cam.cl.dtg.segue.comm.EmailType;
import uk.ac.cam.cl.dtg.segue.dao.ILogManager;
import uk.ac.cam.cl.dtg.segue.dao.ResourceNotFoundException;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dao.content.IContentManager;
import uk.ac.cam.cl.dtg.segue.dos.users.Role;
import uk.ac.cam.cl.dtg.segue.dto.ContentEmailDTO;
import uk.ac.cam.cl.dtg.segue.dto.SegueErrorResponse;
import uk.ac.cam.cl.dtg.segue.dto.content.ContentDTO;
import uk.ac.cam.cl.dtg.segue.dto.content.EmailTemplateDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import static uk.ac.cam.cl.dtg.segue.api.Constants.*;

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
                       final IContentManager contentManager, final IMisuseMonitor misuseMonitor) {
		super(properties, logManager);
        this.contentManager = contentManager;
		this.emailManager = emailManager;
		this.userManager = userManager;
        this.misuseMonitor = misuseMonitor;
	}
    
    /**
     * GetEmailInBrowserById from the database.
     * 
     * This method will return serialised html that displays an email object
     *
     * FIXME - this cannot be used for more complicated templated emails, only those using account info!
     * (i.e. events emails or assignment emails cannot be previewed like this!)
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
    @ApiOperation(value = "Get an email by ID.",
                  notes =  "The details of the current user will be used to fill in template fields.")
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

            // Sanitizes inputs from users
            EmailManager.sanitizeEmailParameters(previewProperties);

            EmailCommunicationMessage ecm = this.emailManager.constructMultiPartEmail(currentUser.getId(),
                    currentUser.getEmail(), emailTemplateDTO, previewProperties, EmailType.SYSTEM);

            HashMap<String, String> previewMap = Maps.newHashMap();
            previewMap.put("subject", emailTemplateDTO.getSubject());
            previewMap.put("from", emailTemplateDTO.getOverrideFromAddress());
            previewMap.put("fromName", emailTemplateDTO.getOverrideFromName());
            previewMap.put("replyTo", emailTemplateDTO.getReplyToEmailAddress());
            previewMap.put("replyToName", emailTemplateDTO.getReplyToName());
            previewMap.put("sender", emailTemplateDTO.getOverrideEnvelopeFrom());
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
    @ApiOperation(value = "Verify an email verification token is valid for use.")
    public Response validateEmailVerificationRequest(@PathParam("userid") final Long userId,
                                                     @PathParam("token") final String token) {

        try {
            misuseMonitor.notifyEvent(userId.toString(), EmailVerificationMisuseHandler.class.getSimpleName());
            userManager.processEmailVerification(userId, token);

            // assume that if there are no exceptions that it worked.
            return Response.ok().build();
        } catch (SegueResourceMisuseException e) {
            return SegueErrorResponse
                    .getRateThrottledResponse("You have exceeded the number of requests allowed for this endpoint");
        } catch (InvalidTokenException | NoUserException e) {
            SegueErrorResponse error = new SegueErrorResponse(Status.BAD_REQUEST, "Token invalid or expired.");
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
    @ApiOperation(value = "Initiate an email verification request.",
                  notes = "The email to verify must be provided as 'email' in the request body.")
    public Response generateEmailVerificationToken(@Context final HttpServletRequest request,
                                                   final Map<String, String> payload) {
        try {
            String email = payload.get("email");
            if (email == null || email.isEmpty()) {
                throw new MissingRequiredFieldException("No email address was provided.");
            }

            misuseMonitor.notifyEvent(email, EmailVerificationRequestMisuseHandler.class.getSimpleName());

            userManager.emailVerificationRequest(request, email);

            this.getLogManager().logEvent(userManager.getCurrentUser(request), request,
                    SegueServerLogType.EMAIL_VERIFICATION_REQUEST_RECEIVED,
                    ImmutableMap.of(Constants.LOCAL_AUTH_EMAIL_VERIFICATION_TOKEN_FIELDNAME, email));

            return Response.ok().build();

        } catch (SegueDatabaseException e) {
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
    @ApiOperation(value = "Send an email to all users of a specific role.")
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
            if (!isUserAnAdmin(userManager, sender)) {
                return SegueErrorResponse.getIncorrectRoleResponse();
            }

    		for (String key : roles.keySet()) {
				RegisteredUserDTO prototype = new RegisteredUserDTO();
				List<RegisteredUserDTO> selectedUsers;
    			
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
    @ApiOperation(value = "Send an email to a list of user IDs.")
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
            if (!isUserAnAdminOrEventManager(userManager, sender)) {
                return SegueErrorResponse.getIncorrectRoleResponse();
            }

            if (isUserAnEventManager(userManager, sender)) {
                if (!emailType.equals(EmailType.EVENTS)) {
                    return new SegueErrorResponse(Status.FORBIDDEN,
                            "Event managers can only send event emails.").toResponse();
                }
                if (misuseMonitor.willHaveMisused(sender.getId().toString(),
                        SendEmailMisuseHandler.class.getSimpleName(), userIds.size())) {
                    return SegueErrorResponse
                            .getRateThrottledResponse("You would have exceeded the number of emails you are allowed to send per day." +
                                    " No emails have been sent.");
                }
                misuseMonitor.notifyEvent(sender.getId().toString(),
                        SendEmailMisuseHandler.class.getSimpleName(), userIds.size());
            }

            for (Long userId : userIds) {
                try {
                    RegisteredUserDTO userDTO = this.userManager.getUserDTOById(userId);
                    if (userDTO != null) {
                        allSelectedUsers.add(userDTO);
                    } else {
                        // This should never be possible, since getUserDTOById throws rather than returning null.
                        throw new NoUserException("No user found with this ID!");
                    }
                } catch (NoUserException e) {
                    // Skip missing users rather than failing hard!
                    log.error(String.format("Skipping email to non-existent user (%s)!", userId));
                }
            }

            if (allSelectedUsers.size() == 0) {
                SegueErrorResponse error = new SegueErrorResponse(Status.BAD_REQUEST,
                        "There are no users in the groups you have selected!.");
                log.error(error.getErrorMessage());
                return error.toResponse();
            }

            emailManager.sendCustomEmail(sender, contentId, new ArrayList<>(allSelectedUsers), emailType);
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
        } catch (SegueResourceMisuseException e) {
            return SegueErrorResponse
                    .getRateThrottledResponse("You have exceeded the number of emails you are allowed to send per day.");
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
     * @param emailTypeString
     *            - the type of e-mail that is being sent.
     * @param emailTemplates
     *            - map which must contain the plaintextTemplate and htmlTemplate
     * @return Response object containing the serialized content object. (with no levels of recursion into the content)
     */
    @POST
    @Path("/email/sendcontentemailwithuserids/{emailtype}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @GZIP
    @ApiOperation(value = "Send an email to a list of user IDs.")
    public final Response sendContentEmailsToUserIds(@Context final HttpServletRequest request,
                                              @PathParam("emailtype") final String emailTypeString,
                                                     final ContentEmailDTO emailTemplates) {
        if (Strings.isNullOrEmpty(emailTemplates.getPlaintextTemplate()) || Strings.isNullOrEmpty(emailTemplates.getHtmlTemplate()) || Strings.isNullOrEmpty(emailTemplates.getEmailSubject())) {
            return SegueErrorResponse.getBadRequestResponse("Response must include plaintextTemplate, htmlTemplate and emailSubject");
        }

        final String plaintextTemplate = emailTemplates.getPlaintextTemplate();
        final String htmlTemplate = emailTemplates.getHtmlTemplate();
        final String emailSubject = emailTemplates.getEmailSubject();
        final List<Long> userIds = emailTemplates.getUserIds();

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
            if (!isUserStaff(userManager, sender)) {
                return SegueErrorResponse.getIncorrectRoleResponse();
            }

            for (Long userId : userIds) {
                try {
                    RegisteredUserDTO userDTO = this.userManager.getUserDTOById(userId);
                    if (userDTO != null) {
                        allSelectedUsers.add(userDTO);
                    } else {
                        // This should never be possible, since getUserDTOById throws rather than returning null.
                        throw new NoUserException("No user found with this ID!");
                    }
                } catch (NoUserException e) {
                    // Skip missing users rather than failing hard!
                    log.error(String.format("Skipping email to non-existent user (%s)!", userId));
                }
            }

            if (allSelectedUsers.size() == 0) {
                SegueErrorResponse error = new SegueErrorResponse(Status.BAD_REQUEST,
                        "There are no users in the groups you have selected!.");
                log.error(error.getErrorMessage());
                return error.toResponse();
            }

            emailManager.sendCustomContentEmail(sender, plaintextTemplate, htmlTemplate, emailSubject, new ArrayList<>(allSelectedUsers), emailType);
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
