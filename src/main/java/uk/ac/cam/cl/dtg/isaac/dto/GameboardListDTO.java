package uk.ac.cam.cl.dtg.isaac.dto;

import java.util.List;

import uk.ac.cam.cl.dtg.segue.dto.ResultsWrapper;

/**
 * GameboardListDTO
 * Provides statistics for the My Boards Page.
 */
public class GameboardListDTO extends ResultsWrapper<GameboardDTO> {
	private Long totalNotStarted;
	private Long totalInProgress;
	private Long totalCompleted;
	
	/**
	 * Default constructor for an empty results set.
	 */
	public GameboardListDTO() {
		super();
		this.totalCompleted = 0L;
		this.totalInProgress = 0L;
		this.totalNotStarted = 0L;
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
	 * @param totalCompleted
	 *            - The total number of gameboards in the completed state.
	 * @param totalInProgress
	 *            - The total number of gameboards in the in progress state.
	 * @param totalNotStarted
	 *            - The total number of gameboards in the not started state.
	 */
	public GameboardListDTO(final List<GameboardDTO> results, final Long totalResults,
			final Long totalNotStarted, final Long totalInProgress, final Long totalCompleted) {
		super(results, totalResults);
		this.totalCompleted = totalCompleted;
		this.totalInProgress = totalInProgress;
		this.totalNotStarted = totalNotStarted;
	}
	
	/**
	 * Gets the totalCompleted.
	 * @return the totalCompleted
	 */
	public Long getTotalCompleted() {
		return totalCompleted;
	}
	
	/**
	 * Gets the totalInProgress.
	 * @return the totalInProgress
	 */
	public Long getTotalInProgress() {
		return totalInProgress;
	}
	
	/**
	 * Gets the totalNotStarted.
	 * @return the totalNotStarted
	 */
	public Long getTotalNotStarted() {
		return totalNotStarted;
	}
}
