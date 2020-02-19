/**
 * Copyright 2014 Stephen Cummins
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * <p>
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.cam.cl.dtg.isaac.dos;

import java.util.Date;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import uk.ac.cam.cl.dtg.isaac.dto.IsaacEventPageDTO;
import uk.ac.cam.cl.dtg.segue.dos.content.Content;
import uk.ac.cam.cl.dtg.segue.dos.content.ContentBase;
import uk.ac.cam.cl.dtg.segue.dos.content.DTOMapping;
import uk.ac.cam.cl.dtg.segue.dos.content.ExternalReference;
import uk.ac.cam.cl.dtg.segue.dos.content.Image;
import uk.ac.cam.cl.dtg.segue.dos.content.JsonContentType;
import uk.ac.cam.cl.dtg.util.locations.Address;
import uk.ac.cam.cl.dtg.util.locations.Location;

import static uk.ac.cam.cl.dtg.segue.api.Constants.EVENT_GROUP_RESERVATION_LIMIT;

/**
 * DO for isaac Event.
 */
@DTOMapping(IsaacEventPageDTO.class)
@JsonContentType("isaacEventPage")
public class IsaacEventPage extends Content {
	private Date date;
	private Date end_date;
	private Date bookingDeadline;
	private Date prepWorkDeadline;

	private Location location;

	private List<ExternalReference> preResources;
	private List<Content> preResourceContent;

	private String emailEventDetails;

	private String emailConfirmedBookingText;
	private String emailWaitingListBookingText;

	private List<ExternalReference> postResources;
	private List<Content> postResourceContent;

	private Image eventThumbnail;

	private Integer numberOfPlaces;

	private EventStatus eventStatus;

	private String isaacGroupToken;

	private Integer groupReservationLimit;

	private Boolean allowGroupReservations;

	@JsonCreator
	public IsaacEventPage(@JsonProperty("id") String id,
						  @JsonProperty("title") String title,
						  @JsonProperty("subtitle") String subtitle,
						  @JsonProperty("type") String type,
						  @JsonProperty("author") String author,
						  @JsonProperty("encoding") String encoding,
						  @JsonProperty("canonicalSourceFile") String canonicalSourceFile,
						  @JsonProperty("layout") String layout,
						  @JsonProperty("children") List<ContentBase> children,
						  @JsonProperty("relatedContent") List<String> relatedContent,
						  @JsonProperty("version") boolean published, @JsonProperty("tags") Set<String> tags,
						  @JsonProperty("date") Date date, @JsonProperty("end_date") Date end_date,
						  @JsonProperty("bookingDeadline") Date bookingDeadline,
						  @JsonProperty("prepWorkDeadline") Date prepWorkDeadline,
						  @JsonProperty("location") Location location,
						  @JsonProperty("preResources") List<ExternalReference> preResources,
						  @JsonProperty("postResources") List<ExternalReference> postResources,
						  @JsonProperty("eventThumbnail") Image eventThumbnail,
						  @JsonProperty("numberOfPlaces") Integer numberOfPlaces,
						  @JsonProperty("EventStatus") EventStatus eventStatus,
						  @JsonProperty("groupReservationLimit") Integer groupReservationLimit,
						  @JsonProperty("allowGroupReservations") Boolean allowGroupReservations) {
		super(id, title, subtitle, type, author, encoding, canonicalSourceFile, layout, children, null,
			null, relatedContent, published, tags, null);

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
		this.allowGroupReservations = allowGroupReservations;
	}

	/**
	 * Default constructor required for Jackson.
	 */
	public IsaacEventPage() {

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
	 * @param date the date to set
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
	 * Sets the end date.
	 *
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
	 * Gets the address.
	 *
	 * @return the address
	 */
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
	public List<ExternalReference> getPreResources() {
		return preResources;
	}

	/**
	 * Sets the preResources.
	 *
	 * @param preResources the preResources to set
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
	 * @param postResources the postResources to set
	 */
	public void setPostResources(final List<ExternalReference> postResources) {
		this.postResources = postResources;
	}

	/**
	 * Gets the eventThumbnail.
	 *
	 * @return the eventThumbnail
	 */
	public Image getEventThumbnail() {
		return eventThumbnail;
	}

	/**
	 * Sets the eventThumbnail.
	 *
	 * @param eventThumbnail the eventThumbnail to set
	 */
	public void setEventThumbnail(final Image eventThumbnail) {
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
	 * @param numberOfPlaces the numberOfPlaces to set
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
	 * @param eventStatus the eventStatus to set
	 */
	public void setEventStatus(final EventStatus eventStatus) {
		this.eventStatus = eventStatus;
	}

	/**
	 * Gets the isaacGroupToken.
	 *
	 * @return the group token.
	 */
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
	 * getPreResourceContent.
	 *
	 * @return the preresource content.
	 */
	public List<Content> getPreResourceContent() {
		return preResourceContent;
	}

	/**
	 * setPreResourceContent.
	 *
	 * @param preResourceContent - the preresource content.
	 */
	public void setPreResourceContent(final List<Content> preResourceContent) {
		this.preResourceContent = preResourceContent;
	}

	/**
	 * getPostResourceContent.
	 *
	 * @return the resource content.
	 */
	public List<Content> getPostResourceContent() {
		return postResourceContent;
	}

	/**
	 * setPostResourceContent.
	 *
	 * @param postResourceContent the content list.
	 */
	public void setPostResourceContent(final List<Content> postResourceContent) {
		this.postResourceContent = postResourceContent;
	}

	/**
	 * Get information about the event that is common to all booking system emails
	 * @return emailEventDetails
	 */
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

	/**
	 * Get the maximum number of reservations per event that a teacher can request.
	 * @return groupReservationLimit
	 */
	public Integer getGroupReservationLimit() {
		return groupReservationLimit;
	}

	/**
	 * Set the maximum number of reservations per event that a teacher can request.
	 * @param groupReservationLimit
	 */
	public void setGroupReservationLimit(Integer groupReservationLimit) {
		this.groupReservationLimit = groupReservationLimit;
	}

	public Boolean getAllowGroupReservations() {
		return allowGroupReservations;
	}

	public void setAllowGroupReservations(Boolean allowGroupReservations) {
		this.allowGroupReservations = allowGroupReservations;
	}


}