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
package uk.ac.cam.cl.dtg.segue.dao;

import java.util.Date;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.Validate;
import org.mongojack.JacksonDBCollection;
import org.mongojack.WriteResult;
import org.mongojack.internal.MongoJackModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.dto.users.AbstractSegueUserDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.AnonymousUserDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBObject;
import com.mongodb.MongoException;

/**
 * Log Manager.
 * 
 * Records events in the database so that we can analyse them later.
 * 
 */
public class MongoLogManager implements ILogManager {
	private static final Logger log = LoggerFactory.getLogger(MongoLogManager.class);

	private final DB database;
	private final ObjectMapper objectMapper;
	
	private final boolean loggingEnabled;

	/**
	 * Create an instance of the log manager.
	 * 
	 * @param database
	 *            - instance of mongodb database.
	 * @param objectMapper
	 *            - used for serialising eventDetails into something useful.
	 * @param loggingEnabled
	 *            - should logging be enabled. True means that log messages will
	 *            be saved false is that they wont.
	 */
	@Inject
	public MongoLogManager(final DB database, final ObjectMapper objectMapper,
			@Named(Constants.LOGGING_ENABLED) final boolean loggingEnabled) {
		this.database = database;
		this.objectMapper = objectMapper;
		this.loggingEnabled = loggingEnabled;
	}

	@Override
	public void logEvent(final AbstractSegueUserDTO user, final HttpServletRequest httpRequest,
			final String eventType, final Object eventDetails) {
		Validate.notNull(user);

		try {
			if (user instanceof RegisteredUserDTO) {
				this.persistLogEvent(((RegisteredUserDTO) user).getDbId(), null, eventType, eventDetails,
						getClientIpAddr(httpRequest));
			} else {
				this.persistLogEvent(null, ((AnonymousUserDTO) user).getSessionId(), eventType, eventDetails,
						getClientIpAddr(httpRequest));
			}
			
		} catch (JsonProcessingException e) {
			log.error("Unable to serialize eventDetails as json string", e);
		}
	}

	/**
	 * log an event in the database.
	 * 
	 * @param userId
	 *            -
	 * @param anonymousUserId
	 *            -
	 * @param eventType
	 *            -
	 * @param eventDetails
	 *            -
	 * @param ipAddress
	 *            -
	 * @throws JsonProcessingException - if we are unable to serialize the eventDetails as a string.
	 */
	private void persistLogEvent(final String userId, final String anonymousUserId, final String eventType,
			final Object eventDetails, final String ipAddress) throws JsonProcessingException {
		// don't do anything if logging is not enabled.
		if (!this.loggingEnabled) {
			return;
		}
		
		if (null == userId && null == anonymousUserId) {
			throw new IllegalArgumentException("UserId or anonymousUserId must be set.");
		}
		
		this.objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
		MongoJackModule.configure(objectMapper);
		
		JacksonDBCollection<DBObject, String> jc = JacksonDBCollection.wrap(
				database.getCollection(Constants.LOG_TABLE_NAME), DBObject.class,
				String.class, this.objectMapper);

		DBObject dbo = new BasicDBObject();

		if (null != userId) {
			dbo.put("userId", userId);
			dbo.put("anonymousUser", false);
		} else {
			dbo.put("userId", anonymousUserId);
			dbo.put("anonymousUser", true);
		}

		dbo.put("eventType", eventType);
		dbo.put("eventDetailsType", eventDetails.getClass().getCanonicalName());
		dbo.put("eventDetails", eventDetails);
		dbo.put("ipAddress", ipAddress);
		dbo.put("timestamp", new Date());

		try {
			WriteResult<DBObject, String> result = jc.insert(dbo);
			if (result.getError() != null) {
				log.error("Error during log operation: " + result.getError());
			}
		} catch (MongoException e) {
			log.error("MongoDb exception while trying to log a user event.");
		}
	}
	
	/**
	 * Extract client ip address.
	 * 
	 * Solution retrieved from: 
	 * http://stackoverflow.com/questions/4678797/how-do-i-get-the-remote-address-of-a-client-in-servlet
	 * 
	 * @param request - to attempt to extract a valid Ip from.
	 * @return string representation of the client's ip address.
	 */
	private static String getClientIpAddr(final HttpServletRequest request) {  
        String ip = request.getHeader("X-Forwarded-For");  
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {  
            ip = request.getHeader("Proxy-Client-IP");  
        }  
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {  
            ip = request.getHeader("WL-Proxy-Client-IP");  
        }  
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {  
            ip = request.getHeader("HTTP_CLIENT_IP");  
        }  
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {  
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");  
        }  
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {  
            ip = request.getRemoteAddr();  
        }  
        return ip;  
    }  
}
