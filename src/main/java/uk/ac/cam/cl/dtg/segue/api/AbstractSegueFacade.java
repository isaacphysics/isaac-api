/**
 * Copyright 2014 Stephen Cummins
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAccountManager;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserLoggedInException;
import uk.ac.cam.cl.dtg.segue.dao.ILogManager;
import uk.ac.cam.cl.dtg.segue.dos.users.Role;
import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.Collections;

/**
 * An abstract representation of a Segue CMS facade.
 * 
 * Provides useful methods such as cache control.
 * 
 * @author Stephen Cummins
 *
 */
public abstract class AbstractSegueFacade {
    private static final Logger log = LoggerFactory.getLogger(AbstractSegueFacade.class);

    private final PropertiesLoader properties;
    private final ILogManager logManager;

    /**
     * Constructor that provides a properties loader.
     * 
     * @param properties
     *            the propertiesLoader.
     * @param logManager
     *            - For logging interesting user events.
     */
    public AbstractSegueFacade(final PropertiesLoader properties, final ILogManager logManager) {
        this.properties = properties;
        this.logManager = logManager;
    }

    /**
     * Gets the properties.
     * 
     * @return the properties
     */
    public PropertiesLoader getProperties() {
        return properties;
    }

    /**
     * generateCachedResponse This method will accept a request and an entity tag and determine whether the entity tag
     * is the same.
     * 
     * If the entity tag is the same a response will be returned which is ready to be sent to the client as we do not
     * need to resent anything.
     * 
     * @param request
     *            - clients request
     * @param etag
     *            - the entity tag we have computed for the resource being requested.
     * @return if the resource etag provided is the same as the one sent by the client then a Response will be returned.
     *         This can be sent directly to the client. If not (i.e. if the resource has changed since the client last
     *         requested it) a null value is returned. This indicates that we need to send a new version of the
     *         resource.
     */
    public Response generateCachedResponse(final Request request, final EntityTag etag) {
        return generateCachedResponse(request, etag, null);
    }

    /**
     * generateCachedResponse This method will accept a request and an entity tag and determine whether the entity tag
     * is the same.
     * 
     * If the entity tag is the same a response will be returned which is ready to be sent to the client as we do not
     * need to resent anything.
     * 
     * @param request
     *            - clients request
     * @param etag
     *            - the entity tag we have computed for the resource being requested.
     * @param maxAge
     *            - this allows you to set the time at which the cache response will go stale.
     * @return if the resource etag provided is the same as the one sent by the client then a Response will be returned.
     *         This can be sent directly to the client. If not (i.e. if the resource has changed since the client last
     *         requested it) a null value is returned. This indicates that we need to send a new version of the
     *         resource.
     */
    public Response generateCachedResponse(final Request request, final EntityTag etag, final Integer maxAge) {
        Response.ResponseBuilder rb = null;

        // Verify if it matched with etag available in http request
        rb = request.evaluatePreconditions(etag);

        // If ETag matches the rb will be non-null;
        if (rb != null) {
            // Use the rb to return the response without any further processing
            log.debug("This resource is unchanged. Serving empty request with etag.");
            return rb.cacheControl(getCacheControl(maxAge, null)).tag(etag).build();
        }
        // the resource must have changed as the etags are different.
        return null;
    }

    /**
     * Same as integer varient.
     * 
     * @param maxAge
     *            in seconds for the returned object to remain fresh. Longs will be converted into integers
     * @param isPublicData
     *            Should the data being delivered be cacheable by intermediate proxy servers?
     * @return a CacheControl object configured with a MaxAge.
     */
    public CacheControl getCacheControl(@Nullable final Integer maxAge, @Nullable final Boolean isPublicData) {
        Long maxAgeToSend = null;
        
        if (maxAge != null) {
            maxAgeToSend = new Long(maxAge);
        }
        
        return getCacheControl(maxAgeToSend, isPublicData);
    }
    
    /**
     * Helper to get cache control information for response objects that can be cached.
     * 
     * @param maxAge
     *            in seconds for the returned object to remain fresh.
     * @param isPublicData
     *            Should the data being delivered be cacheable by intermediate proxy servers?
     * @return a CacheControl object configured with a MaxAge.
     */
    public CacheControl getCacheControl(@Nullable final Long maxAge, @Nullable final Boolean isPublicData) {
        // Create cache control header
        CacheControl cc = new CacheControl();
        
        int maxCacheAge;
        if (null == maxAge) {
            // set max age to server default.
            maxCacheAge = Integer.parseInt(this.properties.getProperty(Constants.MAX_CONTENT_CACHE_TIME));
        } else {
            maxCacheAge = maxAge.intValue();
        }

        cc.setMaxAge(maxCacheAge);
        
        // assume if null or false that the data is private
        if (isPublicData != null && isPublicData) {
            cc.getCacheExtension().put("public", "");
        }
        
        return cc;
    }
    
    /**
     * Gets the logManager.
     * 
     * @return the logManager
     */
    public ILogManager getLogManager() {
        return logManager;
    }

    /**
     * Is the current user an admin.
     * 
     * @param userManager
     *            - Instance of User Manager
     * @param request
     *            - with session information
     * @return true if user is logged in as an admin, false otherwise.
     * @throws NoUserLoggedInException
     *             - if we are unable to tell because they are not logged in.
     */
    public static boolean isUserAnAdmin(final UserAccountManager userManager, final HttpServletRequest request)
            throws NoUserLoggedInException {
        return userManager.checkUserRole(request, Collections.singletonList(Role.ADMIN));
    }

    /**
     * Is the current user an admin.
     *
     * @param userManager
     *            - Instance of User Manager
     * @param userDTO
     *            - for the user of interest
     * @return true if user is logged in as an admin, false otherwise.
     * @throws NoUserLoggedInException
     *             - if we are unable to tell because they are not logged in.
     */
    public static boolean isUserAnAdmin(final UserAccountManager userManager, final RegisteredUserDTO userDTO)
            throws NoUserLoggedInException {
        return userManager.checkUserRole(userDTO, Collections.singletonList(Role.ADMIN));
    }

    /**
     * Is the current user in an event manager role.
     *
     * @param userManager
     *            - Instance of User Manager
     * @param request
     *            - with session information
     * @return true if user is logged in as an event manager, false otherwise.
     * @throws NoUserLoggedInException
     *             - if we are unable to tell because they are not logged in.
     */
    public static boolean isUserAnEventManager(final UserAccountManager userManager,
                                                      final HttpServletRequest request) throws NoUserLoggedInException {
        return userManager.checkUserRole(request, Collections.singletonList(Role.EVENT_MANAGER));
    }

    /**
     * Is the current user in an event manager role.
     *
     * @param userManager
     *            - Instance of User Manager
     * @param userDTO
     *            - for the user of interest
     * @return true if user is logged in as an event manager, false otherwise.
     * @throws NoUserLoggedInException
     *             - if we are unable to tell because they are not logged in.
     */
    public static boolean isUserAnEventManager(final UserAccountManager userManager,
                                                      final RegisteredUserDTO userDTO) throws NoUserLoggedInException {
        return userManager.checkUserRole(userDTO, Collections.singletonList(Role.EVENT_MANAGER));
    }
    
    /**
     * Is the current user in an admin or event manager role.
     * 
     * @param userManager
     *            - Instance of User Manager
     * @param request
     *            - with session information
     * @return true if user is logged in as an admin or event manager, false otherwise.
     * @throws NoUserLoggedInException
     *             - if we are unable to tell because they are not logged in.
     */
    public static boolean isUserAnAdminOrEventManager(final UserAccountManager userManager,
            final HttpServletRequest request) throws NoUserLoggedInException {
        return userManager.checkUserRole(request, Arrays.asList(Role.ADMIN, Role.EVENT_MANAGER));
    }

    /**
     * Is the current user in an admin or event manager role.
     *
     * @param userManager
     *            - Instance of User Manager
     * @param userDTO
     *            - for the user of interest
     * @return true if user is logged in as an admin or event manager, false otherwise.
     * @throws NoUserLoggedInException
     *             - if we are unable to tell because they are not logged in.
     */
    public static boolean isUserAnAdminOrEventManager(final UserAccountManager userManager,
                                                      final RegisteredUserDTO userDTO) throws NoUserLoggedInException {
        return userManager.checkUserRole(userDTO, Arrays.asList(Role.ADMIN, Role.EVENT_MANAGER));
    }

    /**
     * Is the current user in a staff role.
     * 
     * @param userManager
     *            - Instance of User Manager
     * @param request
     *            - with session information
     * @return true if user is logged in as an staff, false otherwise.
     * @throws NoUserLoggedInException
     *             - if we are unable to tell because they are not logged in.
     */
    public static boolean isUserStaff(final UserAccountManager userManager, final HttpServletRequest request)
            throws NoUserLoggedInException {
        return userManager.checkUserRole(request,
                Arrays.asList(Role.ADMIN, Role.EVENT_MANAGER, Role.CONTENT_EDITOR));
    }

    /**
     * Is the current user in a staff role.
     *
     * @param userManager
     *            - Instance of User Manager
     * @param userDTO
     *            - for the user of interest
     * @return true if user is logged in as an admin, false otherwise.
     * @throws NoUserLoggedInException
     *             - if we are unable to tell because they are not logged in.
     */
    public static boolean isUserStaff(final UserAccountManager userManager, final RegisteredUserDTO userDTO)
            throws NoUserLoggedInException {
        return userManager.checkUserRole(userDTO,
                Arrays.asList(Role.ADMIN, Role.EVENT_MANAGER, Role.CONTENT_EDITOR));
    }
}
