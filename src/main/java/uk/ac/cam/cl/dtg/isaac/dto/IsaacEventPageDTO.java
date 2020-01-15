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
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import uk.ac.cam.cl.dtg.isaac.dos.EventStatus;
import uk.ac.cam.cl.dtg.isaac.dos.eventbookings.BookingStatus;
import uk.ac.cam.cl.dtg.segue.dos.content.ExternalReference;
import uk.ac.cam.cl.dtg.segue.dos.content.JsonContentType;
import uk.ac.cam.cl.dtg.segue.dto.content.ContentBaseDTO;
import uk.ac.cam.cl.dtg.segue.dto.content.ContentDTO;
import uk.ac.cam.cl.dtg.segue.dto.content.ContentSummaryDTO;
import uk.ac.cam.cl.dtg.segue.dto.content.ImageDTO;
import uk.ac.cam.cl.dtg.util.locations.Address;
import uk.ac.cam.cl.dtg.util.locations.Location;

import static uk.ac.cam.cl.dtg.segue.api.Constants.EVENT_GROUP_RESERVATION_LIMIT;

/**
 * DTO for isaac Event.
 *
 */
@JsonContentType("isaacEventPage")
public class IsaacEventPageDTO extends ContentDTO {
    private Date date;
    private Date end_date;
    private Date bookingDeadline;
    private Date prepWorkDeadline;
    private Location location;

    private List<ExternalReference> preResources;
    private List<ContentDTO> preResourceContent;
    private String emailEventDetails;
    private String emailConfirmedBookingText;
    private String emailWaitingListBookingText;

    private List<ExternalReference> postResources;
    private List<ContentDTO> postResourceContent;

    private ImageDTO eventThumbnail;

    private Integer numberOfPlaces;

    private EventStatus eventStatus;

    private String isaacGroupToken;

    private Boolean isUserBooked;
    private Boolean isUserOnWaitList;
    private BookingStatus userBookingStatus;

    private Integer placesAvailable;

    private Integer groupReservationLimit;

    /**
     *
     * @param id
     * @param title
     * @param subtitle
     * @param type
     * @param author
     * @param encoding
     * @param canonicalSourceFile
     * @param layout
     * @param children
     * @param relatedContent
     * @param published
     * @param tags
     * @param date
     * @param end_date
     * @param location
     * @param preResources
     * @param postResources
     * @param eventThumbnail
     * @param numberOfPlaces
	 * @param eventStatus
	 */
    @JsonCreator
    public IsaacEventPageDTO(@JsonProperty("id") String id,
                             @JsonProperty("title") String title,
                             @JsonProperty("subtitle") String subtitle,
                             @JsonProperty("type") String type,
                             @JsonProperty("author") String author,
                             @JsonProperty("encoding") String encoding,
                             @JsonProperty("canonicalSourceFile") String canonicalSourceFile,
                             @JsonProperty("layout") String layout,
                             @JsonProperty("children") List<ContentBaseDTO> children,
                             @JsonProperty("relatedContent") List<ContentSummaryDTO> relatedContent,
                             @JsonProperty("version") boolean published,
                             @JsonProperty("tags") Set<String> tags,
                             @JsonProperty("date") Date date,
                             @JsonProperty("end_date") Date end_date,
                             @JsonProperty("bookingDeadline") Date bookingDeadline,
                             @JsonProperty("prepWorkDeadline") Date prepWorkDeadline,
                             @JsonProperty("location") Location location,
                             @JsonProperty("preResources") List<ExternalReference> preResources,
                             @JsonProperty("postResources") List<ExternalReference> postResources,
                             @JsonProperty("eventThumbnail") ImageDTO eventThumbnail,
                             @JsonProperty("numberOfPlaces") Integer numberOfPlaces,
                             @JsonProperty("EventStatus") EventStatus eventStatus,
                             @JsonProperty("groupReservationLimit") Integer groupReservationLimit) {
        super(id, title, subtitle, type, author, encoding, canonicalSourceFile, layout, children, null, null,
                relatedContent, published, tags, null);

        this.date = date;
        this.end_date = end_date;
        this.bookingDeadline = bookingDeadline;
        this.prepWorkDeadline = prepWorkDeadline;
        this.location = location;
        this.preResources = preResources;
        this.postResources = postResources;
        this.eventThumbnail = eventThumbnail;
        this.numberOfPlaces = numberOfPlaces;
        this.eventStatus = eventStatus;
        this.groupReservationLimit = groupReservationLimit != null ? groupReservationLimit : EVENT_GROUP_RESERVATION_LIMIT;
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
     * getBookingDeadline.
     *
     * @return bookingDeadline.
     */
    public Date getBookingDeadline() {
        return bookingDeadline;
    }

    /**
     * setBookingDeadline.
     *
     * @param bookingDeadline the booking deadline.
     */
    public void setBookingDeadline(final Date bookingDeadline) {
        this.bookingDeadline = bookingDeadline;
    }

    /**
     * Gets the address.
     *
     * @return the address
     */
    @JsonIgnore
    public Address getAddress() {
        if (location != null) {
            return location.getAddress();
        } else {
            return null;
        }
    }

    /**
     * Sets the address.
     *
     * @param address the location to set
     */
    @JsonIgnore
    public void setAddress(final Address address) {
        if (location != null) {
            this.location.setAddress(address);
        } else {
            this.location = new Location(address, null, null);
        }
    }

    /**
     * Gets the location.
     *
     * @return the location
     */
    public Location getLocation() {
        return location;
    }

    /**
     * Sets the location.
     *
     * @param location the location to set
     */
    public void setLocation(final Location location) {
        this.location = location;
    }

    /**
     * Gets the preResources.
     * 
     * @return the preResources
     */
    @JsonIgnore
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
    @JsonIgnore
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


    /**
     * Gets the isaacGroupToken.
     *
     * @return the group token.
     */
    @JsonIgnore
    public String getIsaacGroupToken() {
        return isaacGroupToken;
    }

    /**
     * Sets the isaac group token.
     *
     * @param isaacGroupToken the group token for the event.
     */
    public void setIsaacGroupToken(final String isaacGroupToken) {
        this.isaacGroupToken = isaacGroupToken;
    }

    /**
     * setEnd_date.
     *
     * @param end_date the end date of the event.
     */
    public void setEnd_date(final Date end_date) {
        this.end_date = end_date;
    }

    /**
     * getPrepWorkDeadline.
     *
     * @return bookingDeadline.
     */
    public Date getPrepWorkDeadline() {
        return prepWorkDeadline;
    }

    /**
     * setPrepWorkDeadline.
     *
     * @param prepWorkDeadline the booking deadline.
     */
    public void setPrepWorkDeadline(final Date prepWorkDeadline) {
        this.prepWorkDeadline = prepWorkDeadline;
    }

    /**
     * getPreResourceContent.
     *
     * @return the preresource content.
     */
    @JsonIgnore
    public List<ContentDTO> getPreResourceContent() {
        return preResourceContent;
    }

    /**
     * setPreResourceContent.
     *
     * @param preResourceContent - the preresource content.
     */
    public void setPreResourceContent(final List<ContentDTO> preResourceContent) {
        this.preResourceContent = preResourceContent;
    }

    /**
     * getPostResourceContent.
     *
     * @return the resource content.
     */
    @JsonIgnore
    public List<ContentDTO> getPostResourceContent() {
        return postResourceContent;
    }

    /**
     * setPostResourceContent.
     *
     * @param postResourceContent the content list.
     */
    public void setPostResourceContent(final List<ContentDTO> postResourceContent) {
        this.postResourceContent = postResourceContent;
    }

	/**
     * Gets whether the currently logged in user is booked onto this event or not.
     * @return true is yes, false is no, null is not logged in
     */
    public Boolean isUserBooked() {
        return isUserBooked;
    }

	/**
     * Sets whether or not the current user is booked on an event.
     * @param loggedInUserBooked - true is yes, false is no, null is not logged in
     */
    public void setUserBooked(final Boolean loggedInUserBooked) {
        isUserBooked = loggedInUserBooked;
    }

	/**
	 * getPlacesAvailable based on current bookings..
     * @return the get the places available.
     */
    public Integer getPlacesAvailable() {
        return placesAvailable;
    }

	/**
	 * Set the places available based on current bookings.
     * @param placesAvailable - the number of places available.
     */
    public void setPlacesAvailable(final Integer placesAvailable) {
        this.placesAvailable = placesAvailable;
    }

    public Boolean isUserOnWaitList() {
        return isUserOnWaitList;
    }

    public void setUserOnWaitList(Boolean userOnWaitList) {
        isUserOnWaitList = userOnWaitList;
    }

    /**
     * Get information about the event that is common to all booking system emails
     * @return emailEventDetails
     */
    @JsonIgnore
    public String getEmailEventDetails() {
        return emailEventDetails;
    }

    /**
     * Set the email event details.
     * @param emailEventDetails - the text to show in the email token
     */
    public void setEmailEventDetails(final String emailEventDetails) {
        this.emailEventDetails = emailEventDetails;
    }


    /**
     * Get text about the event for the confirmed emails
     *
     * @return emailEventDetails
     */
    @JsonIgnore
    public String getEmailConfirmedBookingText() {
        return emailConfirmedBookingText;
    }

    /**
     * Set the email confirmed booking text for emails.
     * @param emailConfirmedBookingText - text to show in emails
     */
    public void setEmailConfirmedBookingText(String emailConfirmedBookingText) {
        this.emailConfirmedBookingText = emailConfirmedBookingText;
    }

    /**
     * Get text about the event for the waiting list emails
     *
     * @return emailEventDetails
     */
    @JsonIgnore
    public String getEmailWaitingListBookingText() {
        return emailWaitingListBookingText;
    }

    /**
     * Set the email waiting list text for emails.
     * @param emailWaitingListBookingText - text to show in email.
     */
    public void setEmailWaitingListBookingText(String emailWaitingListBookingText) {
        this.emailWaitingListBookingText = emailWaitingListBookingText;
    }

    public Integer getGroupReservationLimit() {
        return groupReservationLimit;
    }

    public void setGroupReservationLimit(Integer groupReservationLimit) {
        this.groupReservationLimit = groupReservationLimit;
    }

    public BookingStatus getUserBookingStatus() {
        return userBookingStatus;
    }

    public void setUserBookingStatus(BookingStatus userBookingStatus) {
        this.userBookingStatus = userBookingStatus;
    }

}
