package uk.ac.cam.cl.dtg.isaac.models.content;

/**
 * DTO that provides high level information for Isaac Questions
 * 
 * Used for gameboards and for related questions links
 */
public class IsaacQuestionInfo extends GameboardItem{
	private String level;

	private String state;
	
	public String getLevel() {
		return level;
	}
	public void setLevel(String level) {
		this.level = level;
	}

	public String getState() {
		return state;
	}
	public void setState(String state) {
		this.state = state;
	}
	
}
