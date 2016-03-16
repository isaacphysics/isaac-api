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
package uk.ac.cam.cl.dtg.isaac.dto;

import java.util.Date;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import uk.ac.cam.cl.dtg.isaac.dos.EventStatus;
import uk.ac.cam.cl.dtg.segue.dos.content.ExternalReference;
import uk.ac.cam.cl.dtg.segue.dos.content.JsonContentType;
import uk.ac.cam.cl.dtg.segue.dto.content.ContentBaseDTO;
import uk.ac.cam.cl.dtg.segue.dto.content.ContentDTO;
import uk.ac.cam.cl.dtg.segue.dto.content.ContentSummaryDTO;
import uk.ac.cam.cl.dtg.segue.dto.content.ImageDTO;
import uk.ac.cam.cl.dtg.util.locations.Address;

/**
 * DO for isaac Event.
 *
 */
@JsonContentType("isaacEventPage")
public class IsaacEventPageDTO extends ContentDTO {
    private Date date;
    private Date end_date;
    private Address location;

    private List<ExternalReference> preResources;
    private List<ExternalReference> postResources;

    private ImageDTO eventThumbnail;

    private Integer numberOfPlaces;

    private EventStatus eventStatus;

    @JsonCreator
    public IsaacEventPageDTO(@JsonProperty("_id") String _id, @JsonProperty("id") String id,
            @JsonProperty("title") String title, @JsonProperty("subtitle") String subtitle,
            @JsonProperty("type") String type, @JsonProperty("author") String author,
            @JsonProperty("encoding") String encoding, @JsonProperty("canonicalSourceFile") String canonicalSourceFile,
            @JsonProperty("layout") String layout, @JsonProperty("children") List<ContentBaseDTO> children,
            @JsonProperty("relatedContent") List<ContentSummaryDTO> relatedContent,
            @JsonProperty("version") boolean published, @JsonProperty("tags") Set<String> tags,
            @JsonProperty("date") Date date, @JsonProperty("end_date") Date end_date,
            @JsonProperty("location") Address location,
            @JsonProperty("preResources") List<ExternalReference> preResources,
            @JsonProperty("postResources") List<ExternalReference> postResources,
            @JsonProperty("eventThumbnail") ImageDTO eventThumbnail,
            @JsonProperty("numberOfPlaces") Integer numberOfPlaces, @JsonProperty("EventStatus") EventStatus eventStatus) {
        super(_id, id, title, subtitle, type, author, encoding, canonicalSourceFile, layout, children, null, null,
                relatedContent, published, tags, null);

        this.date = date;
        this.setEndDate(end_date);
        this.location = location;
        this.preResources = preResources;
        this.postResources = postResources;
        this.eventThumbnail = eventThumbnail;
        this.numberOfPlaces = numberOfPlaces;
        this.eventStatus = eventStatus;
    }

    /**
     * Default constructor required for Jackson.
     */
    public IsaacEventPageDTO() {

    }

    /**
     * Gets the date.
     * 
     * @return the date
     */
    public Date getDate() {
        return date;
    }

    /**
     * Sets the date.
     * 
     * @param date
     *            the date to set
     */
    public void setDate(final Date date) {
        this.date = date;
    }

    /**
     * Gets the end date.
     *
     * @return the end date
     */
    public Date getEndDate() {
        return end_date;
    }

    /**
     * Sets the end date.
     * @param end_date the end date to set
     */
    public void setEndDate(final Date end_date) {
        // Don't want 'end_date' to be null ever; force it to 'date' for consistency if necessary.
        if (null != end_date) {
            this.end_date = end_date;
        } else {
            this.end_date = this.date;
        }
    }
    /**
     * Gets the location.
     * 
     * @return the location
     */
    public Address getLocation() {
        return location;
    }

    /**
     * Sets the location.
     * 
     * @param location
     *            the location to set
     */
    public void setLocation(final Address location) {
        this.location = location;
    }

    /**
     * Gets the preResources.
     * 
     * @return the preResources
     */
    public List<ExternalReference> getPreResources() {
        return preResources;
    }

    /**
     * Sets the preResources.
     * 
     * @param preResources
     *            the preResources to set
     */
    public void setPreResources(final List<ExternalReference> preResources) {
        this.preResources = preResources;
    }

    /**
     * Gets the postResources.
     * 
     * @return the postResources
     */
    public List<ExternalReference> getPostResources() {
        return postResources;
    }

    /**
     * Sets the postResources.
     * 
     * @param postResources
     *            the postResources to set
     */
    public void setPostResources(final List<ExternalReference> postResources) {
        this.postResources = postResources;
    }

    /**
     * Gets the eventThumbnail.
     * 
     * @return the eventThumbnail
     */
    public ImageDTO getEventThumbnail() {
        return eventThumbnail;
    }

    /**
     * Sets the eventThumbnail.
     * 
     * @param eventThumbnail
     *            the eventThumbnail to set
     */
    public void setEventThumbnail(final ImageDTO eventThumbnail) {
        this.eventThumbnail = eventThumbnail;
    }

    /**
     * Gets the numberOfPlaces.
     * 
     * @return the numberOfPlaces
     */
    public Integer getNumberOfPlaces() {
        return numberOfPlaces;
    }

    /**
     * Sets the numberOfPlaces.
     * 
     * @param numberOfPlaces
     *            the numberOfPlaces to set
     */
    public void setNumberOfPlaces(final Integer numberOfPlaces) {
        this.numberOfPlaces = numberOfPlaces;
    }

    /**
     * Gets the eventStatus.
     * 
     * @return the eventStatus
     */
    public EventStatus getEventStatus() {
        return eventStatus;
    }

    /**
     * Sets the eventStatus.
     * 
     * @param eventStatus
     *            the eventStatus to set
     */
    public void setEventStatus(final EventStatus eventStatus) {
        this.eventStatus = eventStatus;
    }
}
