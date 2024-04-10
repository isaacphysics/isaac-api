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

package uk.ac.cam.cl.dtg.isaac.dos;

import static uk.ac.cam.cl.dtg.segue.api.Constants.EVENT_GROUP_RESERVATION_DEFAULT_LIMIT;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import uk.ac.cam.cl.dtg.isaac.dos.content.Content;
import uk.ac.cam.cl.dtg.isaac.dos.content.ContentBase;
import uk.ac.cam.cl.dtg.isaac.dos.content.DTOMapping;
import uk.ac.cam.cl.dtg.isaac.dos.content.ExternalReference;
import uk.ac.cam.cl.dtg.isaac.dos.content.Image;
import uk.ac.cam.cl.dtg.isaac.dos.content.JsonContentType;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacEventPageDTO;
import uk.ac.cam.cl.dtg.util.locations.Address;
import uk.ac.cam.cl.dtg.util.locations.Location;

/**
 * DO for isaac Event.
 */
@DTOMapping(IsaacEventPageDTO.class)
@JsonContentType("isaacEventPage")
public class IsaacEventPage extends Content {
  private Instant date;
  private Instant endDate;
  private Instant bookingDeadline;
  private Instant prepWorkDeadline;

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

  private Boolean privateEvent;
  private Hub hub;

  @JsonCreator
  public IsaacEventPage(@JsonProperty("id") final String id,
                        @JsonProperty("title") final String title,
                        @JsonProperty("subtitle") final String subtitle,
                        @JsonProperty("type") final String type,
                        @JsonProperty("author") final String author,
                        @JsonProperty("encoding") final String encoding,
                        @JsonProperty("canonicalSourceFile") final String canonicalSourceFile,
                        @JsonProperty("layout") final String layout,
                        @JsonProperty("children") final List<ContentBase> children,
                        @JsonProperty("relatedContent") final List<String> relatedContent,
                        @JsonProperty("version") final boolean published,
                        @JsonProperty("deprecated") final Boolean deprecated,
                        @JsonProperty("tags") final Set<String> tags,
                        @JsonProperty("date") final Instant date,
                        @JsonProperty("end_date") final Instant endDate,
                        @JsonProperty("bookingDeadline") final Instant bookingDeadline,
                        @JsonProperty("prepWorkDeadline") final Instant prepWorkDeadline,
                        @JsonProperty("location") final Location location,
                        @JsonProperty("preResources") final List<ExternalReference> preResources,
                        @JsonProperty("postResources") final List<ExternalReference> postResources,
                        @JsonProperty("eventThumbnail") final Image eventThumbnail,
                        @JsonProperty("numberOfPlaces") final Integer numberOfPlaces,
                        @JsonProperty("EventStatus") final EventStatus eventStatus,
                        @JsonProperty("groupReservationLimit") final Integer groupReservationLimit,
                        @JsonProperty("allowGroupReservations") final Boolean allowGroupReservations,
                        @JsonProperty("privateEvent") final Boolean privateEvent,
                        @JsonProperty("hub") final Hub hub) {
    super(id, title, subtitle, type, author, encoding, canonicalSourceFile, layout, children, null,
        null, relatedContent, published, deprecated, tags, null);

    this.date = date;
    this.endDate = endDate;
    this.bookingDeadline = bookingDeadline;
    this.prepWorkDeadline = prepWorkDeadline;
    this.location = location;
    this.preResources = preResources;
    this.postResources = postResources;
    this.eventThumbnail = eventThumbnail;
    this.numberOfPlaces = numberOfPlaces;
    this.eventStatus = eventStatus;
    this.groupReservationLimit =
        groupReservationLimit != null ? groupReservationLimit : EVENT_GROUP_RESERVATION_DEFAULT_LIMIT;
    this.allowGroupReservations = allowGroupReservations != null ? allowGroupReservations : false;
    this.privateEvent = privateEvent;
    this.hub = hub;
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
  public Instant getDate() {
    return date;
  }

  /**
   * Sets the date.
   *
   * @param date the date to set
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
   * Get information about the event that is common to all booking system emails.
   *
   * @return emailEventDetails
   */
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

  /**
   * Get the maximum number of reservations per event that a teacher can request.
   *
   * @return groupReservationLimit
   */
  public Integer getGroupReservationLimit() {
    return groupReservationLimit;
  }

  /**
   * Set the maximum number of reservations per event that a teacher can request.
   *
   * @param groupReservationLimit
   */
  public void setGroupReservationLimit(final Integer groupReservationLimit) {
    this.groupReservationLimit = groupReservationLimit;
  }

  public Boolean getAllowGroupReservations() {
    return allowGroupReservations;
  }

  public void setAllowGroupReservations(final Boolean allowGroupReservations) {
    this.allowGroupReservations = allowGroupReservations;
  }

  public Boolean isPrivateEvent() {
    return privateEvent;
  }

  public void setPrivateEvent(Boolean privateEvent) {
    this.privateEvent = privateEvent;
  }

  public Hub getHub() {
    return hub;
  }

  public void setHub(Hub hub) {
    this.hub = hub;
  }
}