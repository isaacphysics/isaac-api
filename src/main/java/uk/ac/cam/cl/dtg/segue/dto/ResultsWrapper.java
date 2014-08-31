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
package uk.ac.cam.cl.dtg.segue.dto;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * A generic DO / DTO wrapper that can be used to return results from one layer
 * of the application to another or indeed as a DTO.
 * 
 * 
 * @param <T>
 *            - The type of result that can be held in the Wrapper.
 */
public class ResultsWrapper<T> {

	private final List<T> results;
	private Long totalResultsAvailable;

	/**
	 * Default constructor for constructing an empty results wrapper.
	 */
	public ResultsWrapper() {
		this.results = new ArrayList<T>();
		totalResultsAvailable = 0L;
	}

	/**
	 * The most commonly used constructor.
	 * 
	 * @param results
	 *            - a list of results to wrap.
	 * @param totalResults
	 *            - the total results which could be requested by the server -
	 *            assuming that the results returned is a subset of all results
	 *            that could be returned.
	 */
	@JsonCreator
	public ResultsWrapper(final List<T> results, final Long totalResults) {
		this.results = results;
		this.totalResultsAvailable = totalResults;
	}

	/**
	 * Get the results as a list.
	 * 
	 * @return the list of results.
	 */
	public List<T> getResults() {
		return results;
	}

	/**
	 * Get the total number of results which could be returned if asked.
	 * 
	 * @return the total number of results which could be returned.
	 */
	public Long getTotalResults() {
		return totalResultsAvailable;
	}
}
