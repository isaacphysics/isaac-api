package uk.ac.cam.cl.dtg.isaac.models.content;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;

import uk.ac.cam.cl.dtg.isaac.app.Constants;

public class Gameboard {
	private static final Logger log = LoggerFactory.getLogger(Gameboard.class);

	private String id;
	private List<GameboardItem> gameboardItems;
	private Date creationDate;

	@JsonIgnore
	private Wildcard wildCard;

	public Gameboard() {
		this.gameboardItems = new ArrayList<GameboardItem>();
	}

	public Gameboard(String id, List<GameboardItem> questions, Date creationDate)
			throws IllegalArgumentException {
		this.id = id;
		this.gameboardItems = questions;
		this.creationDate = creationDate;

		if (questions.size() > Constants.GAME_BOARD_SIZE) {
			throw new IllegalArgumentException(
					"Too many questions added to gameboard");
		} else if (questions.size() < Constants.GAME_BOARD_SIZE) {
			log.warn("Gameboard created without enough questions.");
		}
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public List<GameboardItem> getQuestions() {
		return gameboardItems;
	}

	public void setQuestions(List<GameboardItem> questions) {
		this.gameboardItems = questions;
	}

	public Date getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(Date creationDate) {
		this.creationDate = creationDate;
	}

	public void setWildCard(Wildcard wildCard) {
		this.wildCard = wildCard;
	}

}
