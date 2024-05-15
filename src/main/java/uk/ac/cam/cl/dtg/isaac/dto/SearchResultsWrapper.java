package uk.ac.cam.cl.dtg.isaac.dto;

import java.util.List;

/**
 * A generic DO / DTO wrapper that can be used to return results from one layer of the application to another or indeed
 * as a DTO.
 *
 *
 * @param <T>
 *            - The type of result that can be held in the Wrapper.
 */
public class SearchResultsWrapper<T> extends ResultsWrapper<T> {
    private int nextSearchOffset;

    /**
     * Default constructor for constructing an empty search results wrapper.
     */
    public SearchResultsWrapper() {
        super();
        this.nextSearchOffset = 0;
    }

    /**
     * The most commonly used constructor.
     *
     * @param results - a list of results to wrap
     * @param totalResults - the total results which could be requested by the server - assuming that the results
     *                     returned is a subset of all results that could be returned.
     * @param nextSearchOffset - the offset to use for the next search.
     */
    public SearchResultsWrapper(final List<T> results, final Long totalResults, final int nextSearchOffset) {
        super(results, totalResults);
        this.nextSearchOffset = nextSearchOffset;
    }

    /**
     * Get the offset to use for the next search.
     *
     * @return the offset to use for the next search.
     */
    public int getNextSearchOffset() {
        return nextSearchOffset;
    }

    /**
     * Set the offset to use for the next search.
     *
     * @param nextSearchOffset - the offset to use for the next search.
     */
    public void setNextSearchOffset(int nextSearchOffset) {
        this.nextSearchOffset = nextSearchOffset;
    }
}
