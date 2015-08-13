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

import java.io.IOException;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.Validate;
import org.joda.time.LocalDate;
import org.mongojack.DBQuery;
import org.mongojack.DBQuery.Query;
import org.mongojack.JacksonDBCollection;
import org.mongojack.WriteResult;
import org.mongojack.internal.MongoJackModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.database.MongoDb;
import uk.ac.cam.cl.dtg.segue.dos.LogEvent;
import uk.ac.cam.cl.dtg.segue.dto.users.AbstractSegueUserDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.AnonymousUserDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.util.Lists;
import com.google.api.client.util.Maps;
import com.google.api.client.util.Sets;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.MongoException;

import static uk.ac.cam.cl.dtg.segue.api.Constants.*;

/**
 * Log Manager.
 * 
 * Records events in the database so that we can analyse them later.
 * 
 */
public class MongoLogManager implements ILogManager {
    private static final Logger log = LoggerFactory.getLogger(MongoLogManager.class);

    private final MongoDb database;
    private final ObjectMapper objectMapper;

    private final boolean loggingEnabled;

    private final LocationHistoryManager locationManager;

    /**
     * Create an instance of the log manager.
     * 
     * @param database
     *            - instance of mongodb database.
     * @param objectMapper
     *            - used for serialising eventDetails into something useful.
     * @param loggingEnabled
     *            - should logging be enabled. True means that log messages will be saved false is that they wont.
     * @param locationManager
     *            - Allows us to geocode ip addresses.
     */
    @Inject
    public MongoLogManager(final MongoDb database, final ObjectMapper objectMapper,
            @Named(Constants.LOGGING_ENABLED) final boolean loggingEnabled, 
            final LocationHistoryManager locationManager) {
        this.database = database;

        this.objectMapper = objectMapper;
        this.objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        MongoJackModule.configure(this.objectMapper);

        this.loggingEnabled = loggingEnabled;
        this.locationManager = locationManager;
    }

    @Override
    public void logEvent(final AbstractSegueUserDTO user, final HttpServletRequest httpRequest, final String eventType,
            final Object eventDetails) {
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

    @Override
    public void logInternalEvent(final AbstractSegueUserDTO user, final String eventType, final Object eventDetails) {
        Validate.notNull(user);
        try {
            if (user instanceof RegisteredUserDTO) {
                this.persistLogEvent(((RegisteredUserDTO) user).getDbId(), null, eventType, eventDetails, null);
            } else {
                this.persistLogEvent(null, ((AnonymousUserDTO) user).getSessionId(), eventType, eventDetails, null);
            }

        } catch (JsonProcessingException e) {
            log.error("Unable to serialize eventDetails as json string", e);
        }
    }

    @Override
    public void transferLogEventsToRegisteredUser(final String oldUserId, final String newUserId) {
        Validate.notBlank(oldUserId);
        Validate.notNull(newUserId);

        JacksonDBCollection<DBObject, String> jc = JacksonDBCollection.wrap(
                database.getDB().getCollection(Constants.LOG_TABLE_NAME), DBObject.class, String.class,
                this.objectMapper);

        BasicDBObject updateQuery = new BasicDBObject();
        BasicDBObject fieldsToUpdate = new BasicDBObject();
        fieldsToUpdate.put("userId", newUserId);
        fieldsToUpdate.put("anonymousUser", false);
        updateQuery.append("$set", fieldsToUpdate);

        BasicDBObject searchQuery = new BasicDBObject();
        searchQuery.append("userId", oldUserId);

        WriteResult<DBObject, String> result = jc.updateMulti(searchQuery, updateQuery);

        if (result.getError() != null) {
            log.error("Error while trying to reassign anonymous user log events to registered user.");
        }
    }

    @Override
    public List<LogEvent> getLogsByType(final String type) {
        JacksonDBCollection<LogEvent, String> jc = JacksonDBCollection.wrap(
                database.getDB().getCollection(Constants.LOG_TABLE_NAME), LogEvent.class, String.class,
                this.objectMapper);

        List<LogEvent> results = jc.find(DBQuery.is("eventType", type)).toArray();

        return results;
    }

    @Override
    public List<LogEvent> getLogsByType(final String type, final Date fromDate) {
        return getLogsByType(type, fromDate, null);
    }

    @Override
    public List<LogEvent> getLogsByType(final String type, final Date fromDate, final Date toDate) {
        return this.getLogsByType(type, fromDate, toDate, null);
    }

    @Override
    public Map<String, Map<LocalDate, Integer>> getLogCountByDate(final Collection<String> eventTypes,
            final Date fromDate, final Date toDate, final List<RegisteredUserDTO> usersOfInterest,
            final boolean binDataByMonth) {

        Validate.notNull(eventTypes);

        Map<String, Map<LocalDate, Integer>> result = Maps.newHashMap();

        for (String typeOfInterest : eventTypes) {
            Iterator<LogEvent> logsByType = this.getLogsIteratorByType(typeOfInterest, fromDate, toDate,
                    usersOfInterest);

            if (!result.containsKey(typeOfInterest)) {
                result.put(typeOfInterest, new HashMap<LocalDate, Integer>());
            }

            while (logsByType.hasNext()) {
                LocalDate dateGroup = this.getDateGroup(logsByType.next(), binDataByMonth);

                if (result.get(typeOfInterest).containsKey(dateGroup)) {
                    result.get(typeOfInterest).put(dateGroup, result.get(typeOfInterest).get(dateGroup) + 1);
                } else {
                    result.get(typeOfInterest).put(dateGroup, 1);
                }
            }
        }

        return result;
    }
    
    @Override
    public List<LogEvent> getLogsByType(final String type, final Date fromDate, final Date toDate,
            final List<RegisteredUserDTO> usersOfInterest) {
        Date newToDate;
        if (null == toDate) {
            newToDate = new Date();
        } else {
            newToDate = toDate;
        }

        JacksonDBCollection<LogEvent, String> jc = JacksonDBCollection.wrap(
                database.getDB().getCollection(Constants.LOG_TABLE_NAME), LogEvent.class, String.class,
                this.objectMapper);

        BasicDBObject queryDateRange = new BasicDBObject("timestamp", //
                new BasicDBObject("$gte", fromDate).append("$lt", newToDate));

        BasicDBObject andQuery = new BasicDBObject();
        List<BasicDBObject> obj = Lists.newArrayList();
        obj.add(queryDateRange);
        obj.add(new BasicDBObject("eventType", type));

        if (null != usersOfInterest && usersOfInterest.size() > 0) {
            BasicDBObject orQuery = new BasicDBObject();

            List<BasicDBObject> ids = Lists.newArrayList();
            for (RegisteredUserDTO user : usersOfInterest) {
                ids.add(new BasicDBObject(USER_ID_FKEY_FIELDNAME, user.getDbId()));
            }

            orQuery.put("$or", ids);
            obj.add(orQuery);
        }

        andQuery.put("$and", obj);
        
        log.info("getLogs by type mongo query: " + andQuery);
        
        List<LogEvent> results = jc.find(andQuery).toArray();

        return results;
    }    

    @Override
    public Long getLogCountByType(final String type) {
        JacksonDBCollection<LogEvent, String> jc = JacksonDBCollection.wrap(
                database.getDB().getCollection(Constants.LOG_TABLE_NAME), LogEvent.class, String.class,
                this.objectMapper);
        BasicDBObject query = new BasicDBObject("eventType", type);
        return jc.count(query);
    }

    @Override
    public Set<String> getAllEventTypes() {
        JacksonDBCollection<LogEvent, String> jc = JacksonDBCollection.wrap(
                database.getDB().getCollection(Constants.LOG_TABLE_NAME), LogEvent.class, String.class,
                this.objectMapper);
        Set<String> results = Sets.newHashSet();

        for (Object e : jc.distinct("eventType")) {
            if (e instanceof String) {
                results.add((String) e);
            }
        }

        return results;
    }

    @Override
    public Set<String> getAllIpAddresses() {
        JacksonDBCollection<LogEvent, String> jc = JacksonDBCollection.wrap(
                database.getDB().getCollection(Constants.LOG_TABLE_NAME), LogEvent.class, String.class,
                this.objectMapper);
        Set<String> results = Sets.newHashSet();

        for (LogEvent e : jc.find()) {
            if (e.getIpAddress() != null) {
                results.add(e.getIpAddress());
            }
        }

        return results;
    }

    @Override
    public List<LogEvent> getAllLogsByUserType(final Class<? extends AbstractSegueUserDTO> userType) {
        // sanity check
        if (!userType.equals(RegisteredUserDTO.class) && !userType.equals(AnonymousUserDTO.class)) {
            throw new IllegalArgumentException(
                    "This method only accepts RegisteredUserDTO or AnonymousUserDTO parameters.");
        }

        JacksonDBCollection<LogEvent, String> jc = JacksonDBCollection.wrap(
                database.getDB().getCollection(Constants.LOG_TABLE_NAME), LogEvent.class, String.class,
                this.objectMapper);

        List<LogEvent> results = jc.find(DBQuery.is("anonymousUser", userType.equals(AnonymousUserDTO.class)))
                .toArray();

        return results;
    }

    @Override
    public List<LogEvent> getAllLogsByUserTypeAndEvent(final Class<? extends AbstractSegueUserDTO> userType,
            final String eventType) {
        // sanity check
        if (!userType.equals(RegisteredUserDTO.class) && !userType.equals(AnonymousUserDTO.class)) {
            throw new IllegalArgumentException(
                    "This method only accepts RegisteredUserDTO or AnonymousUserDTO parameters.");
        }

        Validate.notBlank(eventType);

        JacksonDBCollection<LogEvent, String> jc = JacksonDBCollection.wrap(
                database.getDB().getCollection(Constants.LOG_TABLE_NAME), LogEvent.class, String.class,
                this.objectMapper);

        Query q = DBQuery.and(DBQuery.is("anonymousUser", userType.equals(AnonymousUserDTO.class)),
                DBQuery.is("eventType", eventType));

        List<LogEvent> results = jc.find(q).toArray();

        return results;
    }

    @Override
    public List<LogEvent> getAllLogsByUser(final AbstractSegueUserDTO prototype) {
        Validate.notNull(prototype, "You must provide a user object as a prototype for this search.");

        JacksonDBCollection<LogEvent, String> jc = JacksonDBCollection.wrap(
                database.getDB().getCollection(Constants.LOG_TABLE_NAME), LogEvent.class, String.class,
                this.objectMapper);

        Query q;
        if (prototype instanceof RegisteredUserDTO) {
            RegisteredUserDTO user = (RegisteredUserDTO) prototype;
            Validate.notBlank(user.getDbId(), "You must provide an id in the user object to perform this search.");

            q = DBQuery.and(DBQuery.is("anonymousUser", false), DBQuery.is("_id", user.getDbId()));
        } else if (prototype instanceof AnonymousUserDTO) {
            AnonymousUserDTO user = (AnonymousUserDTO) prototype;
            q = DBQuery.and(DBQuery.is("anonymousUser", true), DBQuery.is("_id", user.getSessionId()));
        } else {
            throw new IllegalArgumentException("Unknown user type provided.");
        }

        List<LogEvent> results = jc.find(q).toArray();
        return results;
    }

    @Override
    public LogEvent getLastLogForUser(final AbstractSegueUserDTO prototype) {
        Validate.notNull(prototype, "You must provide a user object as a prototype for this search.");

        JacksonDBCollection<LogEvent, String> jc = JacksonDBCollection.wrap(
                database.getDB().getCollection(Constants.LOG_TABLE_NAME), LogEvent.class, String.class,
                this.objectMapper);

        Query q;
        if (prototype instanceof RegisteredUserDTO) {
            RegisteredUserDTO user = (RegisteredUserDTO) prototype;
            Validate.notBlank(user.getDbId(), "You must provide an id in the user object to perform this search.");

            q = DBQuery.and(DBQuery.is("anonymousUser", false), DBQuery.is("userId", user.getDbId()));
        } else if (prototype instanceof AnonymousUserDTO) {
            AnonymousUserDTO user = (AnonymousUserDTO) prototype;
            q = DBQuery.and(DBQuery.is("anonymousUser", true), DBQuery.is("userId", user.getSessionId()));
        } else {
            throw new IllegalArgumentException("Unknown user type provided.");
        }

        List<LogEvent> results = jc.find(q).sort(new BasicDBObject("_id", -1)).limit(1).toArray();

        if (results.size() > 0) {
            return results.get(0);
        } else {
            return null;
        }
    }

    @Override
    public Map<String, LogEvent> getLastLogForAllUsers() {
        return this.getLastLogForAllUsers(null);
    }

    @Override
    public Map<String, LogEvent> getLastLogForAllUsers(@Nullable final String qualifyingLogEventType) {
        List<LogEvent> allLogsByUserType;
        if (qualifyingLogEventType != null) {
            allLogsByUserType = this.getAllLogsByUserTypeAndEvent(RegisteredUserDTO.class, qualifyingLogEventType);
        } else {
            allLogsByUserType = this.getAllLogsByUserType(RegisteredUserDTO.class);
        }

        Map<String, LogEvent> results = Maps.newHashMap();
        
        for (LogEvent log : allLogsByUserType) {
            if (results.containsKey((String) log.getUserId())) {

                if (results.get((String) log.getUserId()).getTimestamp().before(log.getTimestamp())) {
                    results.put((String) log.getUserId(), log);
                }

            } else {
                results.put((String) log.getUserId(), log);
            }
        }
        return results;
    }

    @Override
    public Iterator<LogEvent> getLogsIteratorByType(final String type, final Date fromDate, final Date toDate,
            final List<RegisteredUserDTO> usersOfInterest) {
        Date newToDate;
        if (null == toDate) {
            newToDate = new Date();
        } else {
            newToDate = toDate;
        }

        JacksonDBCollection<LogEvent, String> jc = JacksonDBCollection.wrap(
                database.getDB().getCollection(Constants.LOG_TABLE_NAME), LogEvent.class, String.class,
                this.objectMapper);

        BasicDBObject queryDateRange = new BasicDBObject("timestamp", //
                new BasicDBObject("$gte", fromDate).append("$lt", newToDate));

        BasicDBObject andQuery = new BasicDBObject();
        List<BasicDBObject> obj = Lists.newArrayList();
        obj.add(queryDateRange);
        obj.add(new BasicDBObject("eventType", type));

        if (null != usersOfInterest && usersOfInterest.size() > 0) {
            BasicDBObject orQuery = new BasicDBObject();

            List<BasicDBObject> ids = Lists.newArrayList();
            for (RegisteredUserDTO user : usersOfInterest) {
                ids.add(new BasicDBObject(USER_ID_FKEY_FIELDNAME, user.getDbId()));
            }

            orQuery.put("$or", ids);
            obj.add(orQuery);
        }

        andQuery.put("$and", obj);
        
        return jc.find(andQuery).iterator();
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
     * @throws JsonProcessingException
     *             - if we are unable to serialize the eventDetails as a string.
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

        JacksonDBCollection<LogEvent, String> jc = JacksonDBCollection.wrap(
                database.getDB().getCollection(Constants.LOG_TABLE_NAME), LogEvent.class, String.class,
                this.objectMapper);

        LogEvent logEvent = new LogEvent();

        if (null != userId) {
            logEvent.setUserId(userId);
            logEvent.setAnonymousUser(false);
        } else {
            logEvent.setUserId(anonymousUserId);
            logEvent.setAnonymousUser(true);
        }

        logEvent.setEventType(eventType);

        if (eventDetails != null) {
            logEvent.setEventDetailsType(eventDetails.getClass().getCanonicalName());
            logEvent.setEventDetails(eventDetails);
        }

        if (ipAddress != null) {
            logEvent.setIpAddress(ipAddress);

            try {
                // split based on the fact that we usually get ip addresses of the form
                // [user_ip], [balancer/gateway_ip]
                locationManager.refreshLocation(ipAddress.split(",")[0]);
            } catch (SegueDatabaseException | IOException e1) {
                log.error("Unable to record location information for ip Address: " + ipAddress, e1);
            }
        }

        logEvent.setTimestamp(new Date());

        try {
            WriteResult<LogEvent, String> result = jc.insert(logEvent);
            if (result.getError() != null) {
                log.error("Error during log operation: " + result.getError());
            }
        } catch (MongoException e) {
            log.error("MongoDb exception while trying to log a user event.", e);
        }
    }
    
    /**
     * Extract client ip address.
     * 
     * Solution retrieved from:
     * http://stackoverflow.com/questions/4678797/how-do-i-get-the-remote-address-of-a-client-in-servlet
     * 
     * @param request
     *            - to attempt to extract a valid Ip from.
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
    
    /**
     * Get a date object that is configured correctly.
     * 
     * @param log
     *            containing a valid timestamp
     * @param binDataByMonth
     *            whether we should bin the date by month or not.
     * @return the local date either as per the log event or with the day of the month set to 1.
     */
    private LocalDate getDateGroup(final LogEvent log, final boolean binDataByMonth) {
        LocalDate dateGroup;
        if (binDataByMonth) {
            Calendar logDate = new GregorianCalendar();
            logDate.setTime(log.getTimestamp());
            logDate.set(Calendar.DAY_OF_MONTH, 1);

            dateGroup = new LocalDate(logDate.getTime());
        } else {
            dateGroup = new LocalDate(log.getTimestamp());
        }
        return dateGroup;
    }
}