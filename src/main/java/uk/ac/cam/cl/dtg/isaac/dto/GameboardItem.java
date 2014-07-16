package uk.ac.cam.cl.dtg.isaac.dto;

import java.util.List;

/**
 * DTO that provides high level information for Isaac Questions.
 * 
 * Used for gameboards to represent cut down versions of questions
 */
public class GameboardItem {
	private String id;
	private String title;
	private String description;
	private String uri;
	private List<String> tags;
	
	private Integer level;
	private String state;
	
	/**
	 * Gets the id.
	 * @return the id
	 */
	public final String getId() {
		return id;
	}

	/**
	 * Sets the id.
	 * @param id the id to set
	 */
	public final void setId(final String id) {
		this.id = id;
	}

	/**
	 * Gets the title.
	 * @return the title
	 */
	public final String getTitle() {
		return title;
	}

	/**
	 * Sets the title.
	 * @param title the title to set
	 */
	public final void setTitle(final String title) {
		this.title = title;
	}

	/**
	 * Gets the description.
	 * @return the description
	 */
	public final String getDescription() {
		return description;
	}

	/**
	 * Sets the description.
	 * @param description the description to set
	 */
	public final void setDescription(final String description) {
		this.description = description;
	}

	/**
	 * Gets the uri.
	 * @return the uri
	 */
	public final String getUri() {
		return uri;
	}

	/**
	 * Sets the uri.
	 * @param uri the uri to set
	 */
	public final void setUri(final String uri) {
		this.uri = uri;
	}

	/**
	 * Gets the tags.
	 * @return the tags
	 */
	public final List<String> getTags() {
		return tags;
	}

	/**
	 * Sets the tags.
	 * @param tags the tags to set
	 */
	public final void setTags(final List<String> tags) {
		this.tags = tags;
	}

	/**
	 * Gets the level.
	 * @return the level
	 */
	public final Integer getLevel() {
		return level;
	}

	/**
	 * Sets the level.
	 * @param level the level to set
	 */
	public final void setLevel(final Integer level) {
		this.level = level;
	}

	/**
	 * Gets the state.
	 * @return the state
	 */
	public final String getState() {
		return state;
	}

	/**
	 * Sets the state.
	 * @param state the state to set
	 */
	public final void setState(final String state) {
		this.state = state;
	}

}
