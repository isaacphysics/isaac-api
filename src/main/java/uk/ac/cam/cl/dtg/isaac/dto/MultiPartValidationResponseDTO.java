package uk.ac.cam.cl.dtg.isaac.dto;

import uk.ac.cam.cl.dtg.isaac.dto.content.ChoiceDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentDTO;

import java.util.Date;
import java.util.List;

public class MultiPartValidationResponseDTO extends QuestionValidationResponseDTO {
    private List<QuestionValidationResponseDTO> validationResponses;

    /**
     * Default constructor.
     */
    public MultiPartValidationResponseDTO() {

    }

    public MultiPartValidationResponseDTO(final String questionId, final ChoiceDTO answer, final Boolean correct,
                                   final ContentDTO explanation, final Date dateAttempted,
                                   final List<QuestionValidationResponseDTO> validationResponses) {
        super(questionId, answer, correct, explanation, dateAttempted);
        this.validationResponses = validationResponses;
    }

    public List<QuestionValidationResponseDTO> getValidationResponses() {
        return validationResponses;
    }

    public void setValidationResponses(final List<QuestionValidationResponseDTO> validationResponses) {
        this.validationResponses = validationResponses;
    }
}
