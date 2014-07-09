package uk.ac.cam.cl.dtg.isaac.dto;

import java.util.List;

import com.google.api.client.util.Lists;

/**
 * Object to represent the filter used to generate a particular gameboard.
 * 
 */
public class GameFilter {
	private List<String> subjects;
	private List<String> fields;
	private List<String> topics;
	private List<String> levels;
	private List<String> concepts;

	/**
	 * Create a default game filter object.
	 * 
	 */
	public GameFilter() {
		this.subjects = Lists.newArrayList();
		this.fields = Lists.newArrayList();
		this.topics = Lists.newArrayList();
		this.levels = Lists.newArrayList();
		this.concepts = Lists.newArrayList();
	}

	/**
	 * Constructor to fully populate the game filter object.
	 * 
	 * @param subjectsList
	 *            - List of subjects used to get the gameboard
	 * @param fieldsList
	 *            - List of fields used to get the gameboard
	 * @param topicsList
	 *            - List of topics used to get the gameboard
	 * @param levelsList
	 *            - List of levels used to get the gameboard
	 * @param conceptsList
	 *            - List of concepts used to get the gameboard
	 */
	public GameFilter(final List<String> subjectsList,
			final List<String> fieldsList, final List<String> topicsList,
			final List<String> levelsList, final List<String> conceptsList) {

		this.subjects = subjectsList;
		this.fields = fieldsList;
		this.topics = topicsList;
		this.levels = levelsList;
		this.concepts = conceptsList;
	}

	/**
	 * Gets the subjectsList.
	 * 
	 * @return the subjectsList
	 */
	public final List<String> getSubjects() {
		return subjects;
	}

	/**
	 * Gets the fieldsList.
	 * 
	 * @return the fieldsList
	 */
	public final List<String> getFields() {
		return fields;
	}

	/**
	 * Gets the topicsList.
	 * 
	 * @return the topicsList
	 */
	public final List<String> getTopics() {
		return topics;
	}

	/**
	 * Gets the levelsList.
	 * 
	 * @return the levelsList
	 */
	public final List<String> getLevels() {
		return levels;
	}

	/**
	 * Gets the conceptsList.
	 * 
	 * @return the conceptsList
	 */
	public final List<String> getConcepts() {
		return concepts;
	}
}
