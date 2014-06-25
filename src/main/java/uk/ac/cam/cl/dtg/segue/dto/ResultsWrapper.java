package uk.ac.cam.cl.dtg.segue.dto;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;

public class ResultsWrapper<T> {

	private final List<T> results;
	private Long totalResultsAvailable;

	public ResultsWrapper() {
		this.results = new ArrayList<T>();
		totalResultsAvailable = 0L;
	}

	@JsonCreator
	public ResultsWrapper(List<T> results, Long totalResults) {
		this.results = results;
		this.totalResultsAvailable = totalResults;
	}

	public List<T> getResults() {
		return results;
	}

	public Long getTotalResults() {
		return totalResultsAvailable;
	}
}
