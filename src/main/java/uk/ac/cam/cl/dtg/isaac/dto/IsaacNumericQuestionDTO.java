package uk.ac.cam.cl.dtg.isaac.dto;

import java.util.List;

import com.google.api.client.util.Lists;

import uk.ac.cam.cl.dtg.isaac.quiz.IsaacNumericValidator;
import uk.ac.cam.cl.dtg.segue.dos.content.JsonType;
import uk.ac.cam.cl.dtg.segue.dto.content.ChoiceDTO;
import uk.ac.cam.cl.dtg.segue.dto.content.QuantityDTO;
import uk.ac.cam.cl.dtg.segue.quiz.ValidatesWith;

/**
 * DO for isaacNumericQuestions.
 * 
 */
@JsonType("isaacNumericQuestion")
@ValidatesWith(IsaacNumericValidator.class)
public class IsaacNumericQuestionDTO extends IsaacQuestionBaseDTO {
	private Boolean requireUnits;
	
	/**
	 * Gets the requireUnits.
	 * @return the requireUnits
	 */
	public final Boolean getRequireUnits() {
		if (requireUnits == null) {
			return true;
		}
		
		return requireUnits;
	}

	/**
	 * Sets the requireUnits.
	 * @param requireUnits the requireUnits to set
	 */
	public final void setRequireUnits(final Boolean requireUnits) {
		this.requireUnits = requireUnits;
	}

	@Override
	public final List<ChoiceDTO> getChoices() {
		// we do not want the choice list to be displayed to users.
		return null;
	}
	
	/**
	 * Gets the knownUnits.
	 * 
	 * This is a hack so that the frontend can display all units available as a
	 * drop down list.
	 * 
	 * @return the knownUnits
	 */
	public List<String> getKnownUnits() {
		List<String> unitsToReturn = Lists.newArrayList();
		
		for (ChoiceDTO c : this.choices) {
			if (c instanceof QuantityDTO) {
				QuantityDTO quantity = (QuantityDTO) c;
				if (quantity.getUnits() != null && !quantity.getUnits().isEmpty()) {
					unitsToReturn.add(quantity.getUnits());	
				}
			}
		}
		
		if (unitsToReturn.isEmpty()) {
			return null;	
		}
		
		return unitsToReturn;
	}
}
