/*
 * Copyright 2015 Stephen Cummins
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.cam.cl.dtg.segue.api.monitors;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.dto.MisuseStatisticDTO;
import uk.ac.cam.cl.dtg.segue.api.managers.SegueResourceMisuseException;

import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.google.common.collect.Maps.immutableEntry;

/**
 * InMemoryMisuseMonitor.
 */
public class InMemoryMisuseMonitor implements IMisuseMonitor {
    // Cache of the form agentIdentifier --> Event --> Date, number
    private final Cache<String, Map<String, Map.Entry<Date, Integer>>> nonPersistentDatabase;

    private final Map<String, IMisuseHandler> handlerMap;

    private static final Logger log = LoggerFactory.getLogger(InMemoryMisuseMonitor.class);

    /**
     * Creates a misuse monitor that just uses non-persistent storage.
     */
    @Inject
    public InMemoryMisuseMonitor() {
        nonPersistentDatabase = CacheBuilder.newBuilder().expireAfterAccess(2, TimeUnit.DAYS)
                .<String, Map<String, Map.Entry<Date, Integer>>> build();
        handlerMap = Maps.newConcurrentMap();
    }

    @Override
    public void registerHandler(final String eventToHandle, final IMisuseHandler handler) {
        handlerMap.put(eventToHandle, handler);
    }

    @Override
    public synchronized void notifyEvent(final String agentIdentifier, final String eventLabel)
            throws SegueResourceMisuseException {
        Validate.notBlank(agentIdentifier);
        Validate.notBlank(eventLabel);
        this.notifyEvent(agentIdentifier, eventLabel, 1);
    }

    @Override
    public synchronized void notifyEvent(final String agentIdentifier, final String eventLabel,
            final Integer adjustmentValue) throws SegueResourceMisuseException {
        Validate.notBlank(agentIdentifier);
        Validate.notBlank(eventLabel);
        Objects.requireNonNull(adjustmentValue);
        Validate.isTrue(adjustmentValue >= 0, "Expected positive integer value.");
        
        IMisuseHandler handler = handlerMap.get(eventLabel);
        Objects.requireNonNull(handler, "No handler has been registered for " + eventLabel);

        Map<String, Entry<Date, Integer>> existingHistory = nonPersistentDatabase.getIfPresent(agentIdentifier);

        if (null == existingHistory) {
            existingHistory = Maps.newConcurrentMap();

            existingHistory.put(eventLabel, immutableEntry(new Date(), adjustmentValue));
            nonPersistentDatabase.put(agentIdentifier, existingHistory);
        } else {
            Entry<Date, Integer> entry = existingHistory.get(eventLabel);
            if (null == entry) {
                existingHistory.put(eventLabel, immutableEntry(new Date(), adjustmentValue));
                log.debug("New Event " + existingHistory.get(eventLabel));
            } else {

                // deal with expired events
                if (!isCountStillFresh(entry.getKey(), handler.getAccountingIntervalInSeconds())) {
                    existingHistory.put(eventLabel, immutableEntry(new Date(), adjustmentValue));
                    log.debug("Event expired starting count over");
                } else {
                    // last events not expired yet so add them.
                    existingHistory.put(eventLabel, immutableEntry(entry.getKey(), entry.getValue() + adjustmentValue));
                    log.debug("Event NOT expired so adding one " + existingHistory.get(eventLabel));
                }

                entry = existingHistory.get(eventLabel);
                int previousValue = entry.getValue() - adjustmentValue;
                
                // deal with threshold violations
                if (handler.getSoftThreshold() != null
                        && (previousValue < handler.getSoftThreshold() && entry.getValue() >= handler
                                .getSoftThreshold())) {
                    handler.executeSoftThresholdAction(String.format("(%s) has exceeded the soft limit!",
                            agentIdentifier));
                }

                if (handler.getHardThreshold() != null
                        && (previousValue < handler.getHardThreshold() && entry.getValue() >= handler
                                .getHardThreshold())) {
                    String errMessage = String.format("(%s) has exceeded the hard limit!", agentIdentifier);

                    handler.executeHardThresholdAction(errMessage);
                }
                
                if (handler.getHardThreshold() != null && entry.getValue() > handler.getHardThreshold()) {
                    throw new SegueResourceMisuseException("Exceeded resource usage limit on " + eventLabel);
                }
            }
        }
    }
    
    @Override
    public boolean hasMisused(final String agentIdentifier, final String eventToCheck) {
        return willHaveMisused(agentIdentifier, eventToCheck, 0);
    }

    @Override
    public boolean willHaveMisused(String agentIdentifier, String eventToCheck, Integer adjustmentValue) {
        Map<String, Entry<Date, Integer>> existingHistory = nonPersistentDatabase.getIfPresent(agentIdentifier);

        if (null == existingHistory || existingHistory.get(eventToCheck) == null) {
            return false;
        }

        Entry<Date, Integer> entry = existingHistory.get(eventToCheck);
        IMisuseHandler handler = handlerMap.get(eventToCheck);

        return isCountStillFresh(entry.getKey(), handler.getAccountingIntervalInSeconds())
                && entry.getValue() + adjustmentValue >= handler.getHardThreshold();
    }

    @Override
    public void resetMisuseCount(final String agentIdentifier, final String eventLabel) {
        Map<String, Entry<Date, Integer>> existingHistory = nonPersistentDatabase.getIfPresent(agentIdentifier);
        
        if (null == existingHistory || existingHistory.get(eventLabel) == null) {
            return;
        }
        
        existingHistory.remove(eventLabel);
    }

    @Override
    public int getRemainingUses(String agentIdentifier, String eventToCheck) {
        Map<String, Entry<Date, Integer>> agentUsage = nonPersistentDatabase.getIfPresent(agentIdentifier);
        IMisuseHandler misuseHandler = handlerMap.get(eventToCheck);
        int maxUsesInInterval = misuseHandler.getHardThreshold();

        if (agentUsage == null || agentUsage.get(eventToCheck) == null) {
            return maxUsesInInterval;
        }

        Entry<Date, Integer> entry = agentUsage.get(eventToCheck);

        if (isCountStillFresh(entry.getKey(), misuseHandler.getAccountingIntervalInSeconds())) {
            return maxUsesInInterval - entry.getValue();
        } else {
            return maxUsesInInterval;
        }
    }

    @Override
    public Map<String, List<MisuseStatisticDTO>> getMisuseStatistics(final long n) {
        Map<String, Map<String, Entry<Date, Integer>>> cache = nonPersistentDatabase.asMap();
        return handlerMap.keySet().stream()
                .map(eventLabel -> {
                    Integer softThreshold = handlerMap.get(eventLabel).getSoftThreshold();
                    return Map.entry(
                            eventLabel,
                            cache.entrySet().stream()
                                .map(e -> Pair.of(e.getKey(), e.getValue().get(eventLabel)))
                                .filter(e -> null != e.getRight())
                                .sorted(Comparator.comparingInt((Entry<String, Entry<Date, Integer>> e) -> e.getValue().getValue()).reversed())
                                .limit(n)
                                .map((Pair<String, Entry<Date, Integer>> e) -> {
                                    String agentIdentifier = e.getKey();
                                    Entry<Date, Integer> misuseEntry = e.getValue();
                                    return new MisuseStatisticDTO(
                                        agentIdentifier, eventLabel, hasMisused(agentIdentifier, eventLabel),
                                        misuseEntry.getValue() >= softThreshold,
                                        misuseEntry.getKey(), misuseEntry.getValue()
                                    );
                                })
                                .collect(Collectors.toList())
                    );
                }).collect(Collectors.toMap(Entry::getKey, Entry::getValue));
    }

    /**
     * Helper to work out whether we can reset the counter or not.
     * 
     * @param mapEntryDate
     *            - the date that the map entry for the misuse database was created.
     * @param secondsUntilExpiry
     *            - the number of seconds until this entry expires.
     * @return true if we can continue counting false if we should reset the counter as the entry has expired.
     */
    private boolean isCountStillFresh(final Date mapEntryDate, final Integer secondsUntilExpiry) {
        Calendar entryExpiry = Calendar.getInstance();
        entryExpiry.setTime(mapEntryDate);
        entryExpiry.add(Calendar.SECOND, secondsUntilExpiry);

        if (new Date().after(entryExpiry.getTime())) {
            return false;
        }

        return true;
    }
}
