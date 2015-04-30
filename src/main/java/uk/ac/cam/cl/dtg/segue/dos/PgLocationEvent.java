/**
 * Copyright 2015 Stephen Cummins
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
package uk.ac.cam.cl.dtg.segue.dos;

import java.util.Date;

import uk.ac.cam.cl.dtg.util.locations.Location;

/**
 * @author sac92
 *
 */
public class PgLocationEvent implements LocationHistoryEvent {
	private Long id;
	private String ipAddress;
	private Location locationInformation;
	private Date created;
	private Date lastUpdated;
	
	/**
	 * @param id of the location entry
	 * @param ipAddress for this record
	 * @param locationInformation - information about the location 
	 * @param created - date the record was created.
	 * @param lastUpdated - date the record was verified.
	 */
	public PgLocationEvent(final Long id, final String ipAddress, final Location locationInformation,
		final Date created, final Date lastUpdated) {
		this.id = id;
		this.ipAddress = ipAddress;
		this.locationInformation = locationInformation;
		this.created = created;
		this.lastUpdated = lastUpdated;
	}

	@Override
	public Long getId() {
		return id;
	}

	@Override
	public String getIpAddress() {
		return ipAddress;
	}

	@Override
	public Location getLocationInformation() {
		return locationInformation;
	}

	@Override
	public Date getCreated() {
		return created;
	}

	@Override
	public Date getLastUpdated() {
		return lastUpdated;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("PgLocationEvent [id=");
		builder.append(id);
		builder.append(", ipAddress=");
		builder.append(ipAddress);
		builder.append(", locationInformation=");
		builder.append(locationInformation);
		builder.append(", created=");
		builder.append(created);
		builder.append(", lastUpdated=");
		builder.append(lastUpdated);
		builder.append("]");
		return builder.toString();
	}
}
