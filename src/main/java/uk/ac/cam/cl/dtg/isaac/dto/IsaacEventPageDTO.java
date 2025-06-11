/**
 * Copyright 2014 Stephen Cummins
 * <br>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * <br>
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * <br>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.ac.cam.cl.dtg.isaac.dto;

import static uk.ac.cam.cl.dtg.segue.api.Constants.EVENT_GROUP_RESERVATION_DEFAULT_LIMIT;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import uk.ac.cam.cl.dtg.isaac.dos.EventStatus;
import uk.ac.cam.cl.dtg.isaac.dos.Hub;
import uk.ac.cam.cl.dtg.isaac.dos.content.ExternalReference;
import uk.ac.cam.cl.dtg.isaac.dos.content.JsonContentType;
import uk.ac.cam.cl.dtg.isaac.dos.eventbookings.BookingStatus;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentBaseDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentSummaryDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.ImageDTO;
import uk.ac.cam.cl.dtg.util.locations.Address;
import uk.ac.cam.cl.dtg.util.locations.Location;

/**
 * DTO for isaac Event.
 *
 */
@JsonContentType("isaacEventPage")
public class IsaacEventPageDTO extends ContentDTO {
  private Instant date;
  private Instant endDate;
  private Instant bookingDeadline;
  private Instant prepWorkDeadline;
  private Instant publicationDate;
  private Location location;

  private List<ExternalReference> preResources;
  private List<ContentDTO> preResourceContent;
  private String emailEventDetails;
  private String emailConfirmedBookingText;
  private String emailWaitingListBookingText;

  private List<ExternalReference> postResources;
  private List<ContentDTO> postResourceContent;
  private String eventSurveyTitle;
  private String eventSurveyUrl;

  private ImageDTO eventThumbnail;

  private Integer numberOfPlaces;

  private EventStatus eventStatus;

  private String isaacGroupToken;

  private BookingStatus userBookingStatus;

  private Integer placesAvailable;

  private Integer groupReservationLimit;

  private Boolean allowGroupReservations;

  private Boolean privateEvent;

  private Boolean competitionEvent;

  private Hub hub;

  private String meetingUrl;

  /**
   * Constructor for IsaacEventPageDTO, taking event-specific properties in addition to those from parent ContentDTO.
   *
   * @param date date the event is scheduled to start
   * @param endDate date the event is scheduled to end
   * @param bookingDeadline date at which bookings will no longer be accepted
   * @param prepWorkDeadline date by which preceding tasks should be completed
   * @param publicationDate date of when the event is published on the website
   * @param location where the event will occur
   * @param preResources resources to be provided to attendees before the event
   * @param postResources resources to be provided to attendees after the event
   * @param eventSurveyTitle a survey title of event that will be sent to attendees after the event
   * @param eventSurveyUrl a survey Url will be sent to attendees after the event
   * @param eventThumbnail thumbnail image for event
   * @param numberOfPlaces maximum number of booking places to allow
   * @param eventStatus status of event {@link EventStatus}
   * @param groupReservationLimit limit on student reservations that an individual teacher may request
   * @param allowGroupReservations whether group bookings are permitted
   * @param privateEvent if an event should be publicly visible or hidden
   * @param hub the hub hosting the event
   * @param meetingUrl link to virtual meeting
   */
  @JsonCreator
  public IsaacEventPageDTO(
      @JsonProperty("id") final String id,
      @JsonProperty("title") final String title,
      @JsonProperty("subtitle") final String subtitle,
      @JsonProperty("type") final String type,
      @JsonProperty("author") final String author,
      @JsonProperty("encoding") final String encoding,
      @JsonProperty("canonicalSourceFile") final String canonicalSourceFile,
      @JsonProperty("layout") final String layout,
      @JsonProperty("children") final List<ContentBaseDTO> children,
      @JsonProperty("relatedContent") final List<ContentSummaryDTO> relatedContent,
      @JsonProperty("version") final boolean published,
      @JsonProperty("deprecated") final Boolean deprecated,
      @JsonProperty("tags") final Set<String> tags,
      @JsonProperty("date") final Instant date,
      @JsonProperty("end_date") final Instant endDate,
      @JsonProperty("bookingDeadline") final Instant bookingDeadline,
      @JsonProperty("prepWorkDeadline") final Instant prepWorkDeadline,
      @JsonProperty("publicationDate") final Instant publicationDate,
      @JsonProperty("location") final Location location,
      @JsonProperty("preResources") final List<ExternalReference> preResources,
      @JsonProperty("postResources") final List<ExternalReference> postResources,
      @JsonProperty("eventSurveyTitle") final String eventSurveyTitle,
      @JsonProperty("eventSurveyUrl") final String eventSurveyUrl,
      @JsonProperty("eventThumbnail") final ImageDTO eventThumbnail,
      @JsonProperty("numberOfPlaces") final Integer numberOfPlaces,
      @JsonProperty("EventStatus") final EventStatus eventStatus,
      @JsonProperty("groupReservationLimit") final Integer groupReservationLimit,
      @JsonProperty("allowGroupReservations") final Boolean allowGroupReservations,
      @JsonProperty("privateEvent") final Boolean privateEvent,
      @JsonProperty("competitionEvent") final Boolean competitionEvent,
      @JsonProperty("hub") final Hub hub,
      @JsonProperty("meetingUrl") final String meetingUrl) {
    super(id, title, subtitle, type, author, encoding, canonicalSourceFile, layout, children, null, null,
        relatedContent, published, deprecated, tags, null);

    this.date = date;
    this.endDate = endDate;
    this.bookingDeadline = bookingDeadline;
    this.prepWorkDeadline = prepWorkDeadline;
    this.publicationDate = publicationDate;
    this.location = location;
    this.preResources = preResources;
    this.postResources = postResources;
    this.eventSurveyTitle = eventSurveyTitle;
    this.eventSurveyUrl = eventSurveyUrl;
    this.eventThumbnail = eventThumbnail;
    this.numberOfPlaces = numberOfPlaces;
    this.eventStatus = eventStatus;
    this.groupReservationLimit =
        groupReservationLimit != null ? groupReservationLimit : EVENT_GROUP_RESERVATION_DEFAULT_LIMIT;
    this.allowGroupReservations = allowGroupReservations;
    this.privateEvent = privateEvent;
    this.competitionEvent = competitionEvent;
    this.hub = hub;
    this.meetingUrl = meetingUrl;
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
  public Instant getDate() {
    return date;
  }

  /**
   * Sets the date.
   *
   * @param date
   *            the date to set
   */
  public void setDate(final Instant date) {
    this.date = date;
  }

  /**
   * Gets the end date.
   *
   * @return the end date
   */
  public Instant getEndDate() {
    return endDate;
  }

  /**
   * Sets the end date.
   *
   * @param endDate the end date to set
   */
  public void setEndDate(final Instant endDate) {
    // Don't want 'endDate' to be null ever; force it to 'date' for consistency if necessary.
    if (null != endDate) {
      this.endDate = endDate;
    } else {
      this.endDate = this.date;
    }
  }

  /**
   * getBookingDeadline.
   *
   * @return bookingDeadline.
   */
  public Instant getBookingDeadline() {
    return bookingDeadline;
  }

  /**
   * setBookingDeadline.
   *
   * @param bookingDeadline the booking deadline.
   */
  public void setBookingDeadline(final Instant bookingDeadline) {
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
   * Gets the eventSurveyTitle.
   *
   * @return the eventSurveyTitle
   */
  @JsonIgnore
  public String getEventSurveyTitle() {
    return eventSurveyTitle;
  }

  /**
   * Sets the eventSurveyTitle.
   *
   * @param eventSurveyTitle
   *            the eventSurveyTitle to set
   */
  public void setEventSurveyTitle(final String eventSurveyTitle) {
    this.eventSurveyTitle = eventSurveyTitle;
  }

  /**
   * Gets the eventSurveyUrl.
   *
   * @return the eventSurveyUrl
   */
  @JsonIgnore
  public String getEventSurveyUrl() {
    return eventSurveyUrl;
  }

  /**
   * Sets the eventSurveyUrl.
   *
   * @param eventSurveyUrl
   *            the eventSurveyUrl to set
   */
  public void setEventSurveyUrl(final String eventSurveyUrl) {
    this.eventSurveyUrl = eventSurveyUrl;
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
   * getPrepWorkDeadline.
   *
   * @return bookingDeadline.
   */
  public Instant getPrepWorkDeadline() {
    return prepWorkDeadline;
  }

  /**
   * setPrepWorkDeadline.
   *
   * @param prepWorkDeadline the booking deadline.
   */
  public void setPrepWorkDeadline(final Instant prepWorkDeadline) {
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
   * getPlacesAvailable based on current bookings.
   *
   * @return the get the places available.
   */
  public Integer getPlacesAvailable() {
    return placesAvailable;
  }

  /**
   * Set the places available based on current bookings.
   *
   * @param placesAvailable - the number of places available.
   */
  public void setPlacesAvailable(final Integer placesAvailable) {
    this.placesAvailable = placesAvailable;
  }

  /**
   * Get information about the event that is common to all booking system emails.
   *
   * @return emailEventDetails
   */
  @JsonIgnore
  public String getEmailEventDetails() {
    return emailEventDetails;
  }

  /**
   * Set the email event details.
   *
   * @param emailEventDetails - the text to show in the email token
   */
  public void setEmailEventDetails(final String emailEventDetails) {
    this.emailEventDetails = emailEventDetails;
  }


  /**
   * Get text about the event for the confirmed emails.
   *
   * @return emailEventDetails
   */
  @JsonIgnore
  public String getEmailConfirmedBookingText() {
    return emailConfirmedBookingText;
  }

  /**
   * Set the email confirmed booking text for emails.
   *
   * @param emailConfirmedBookingText - text to show in emails
   */
  public void setEmailConfirmedBookingText(final String emailConfirmedBookingText) {
    this.emailConfirmedBookingText = emailConfirmedBookingText;
  }

  /**
   * Get text about the event for the waiting list emails.
   *
   * @return emailEventDetails
   */
  @JsonIgnore
  public String getEmailWaitingListBookingText() {
    return emailWaitingListBookingText;
  }

  /**
   * Set the email waiting list text for emails.
   *
   * @param emailWaitingListBookingText - text to show in email.
   */
  public void setEmailWaitingListBookingText(final String emailWaitingListBookingText) {
    this.emailWaitingListBookingText = emailWaitingListBookingText;
  }

  public Integer getGroupReservationLimit() {
    return groupReservationLimit;
  }

  public void setGroupReservationLimit(final Integer groupReservationLimit) {
    this.groupReservationLimit = groupReservationLimit;
  }

  public BookingStatus getUserBookingStatus() {
    return userBookingStatus;
  }

  public void setUserBookingStatus(final BookingStatus userBookingStatus) {
    this.userBookingStatus = userBookingStatus;
  }

  public Boolean getAllowGroupReservations() {
    return allowGroupReservations;
  }

  public void setAllowGroupReservations(final Boolean allowGroupReservations) {
    this.allowGroupReservations = allowGroupReservations;
  }

  @Override
  @JsonIgnore(false) // Override the parent class decorator!
  public String getCanonicalSourceFile() {
    return super.getCanonicalSourceFile();
  }

  public Boolean isPrivateEvent() {
    return privateEvent;
  }

  public void setPrivateEvent(Boolean privateEvent) {
    this.privateEvent = privateEvent;
  }

  public Boolean isCompetitionEvent() {
    return competitionEvent;
  }

  public void setCompetitionEvent(Boolean competitionEvent) {
    this.competitionEvent = competitionEvent;
  }

  public Hub getHub() {
    return hub;
  }

  public void setHub(Hub hub) {
    this.hub = hub;
  }

  public String getMeetingUrl() {
    return meetingUrl;
  }

  public void setMeetingUrl(String meetingUrl) {
    this.meetingUrl = meetingUrl;
  }
}
