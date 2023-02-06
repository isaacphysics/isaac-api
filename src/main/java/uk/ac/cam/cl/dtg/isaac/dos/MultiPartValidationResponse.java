package uk.ac.cam.cl.dtg.isaac.dos;

import uk.ac.cam.cl.dtg.isaac.dos.content.Choice;
import uk.ac.cam.cl.dtg.isaac.dos.content.Content;
import uk.ac.cam.cl.dtg.isaac.dos.content.DTOMapping;
import uk.ac.cam.cl.dtg.isaac.dto.MultiPartValidationResponseDTO;

import java.util.Date;
import java.util.List;

@DTOMapping(MultiPartValidationResponseDTO.class)
public class MultiPartValidationResponse extends QuestionValidationResponse {
    private List<QuestionValidationResponse> validationResponses;

    /**
     * Default constructor.
     */
    public MultiPartValidationResponse() {
    }

    public MultiPartValidationResponse(final String questionId, final Choice answer, final Boolean correct,
                                final Content explanation, final Date dateAttempted,
                                final List<QuestionValidationResponse> validationResponses) {
        super(questionId, answer, correct, explanation, dateAttempted);
        this.validationResponses = validationResponses;
    }

    public List<QuestionValidationResponse> getValidationResponses() {
        return validationResponses;
    }

    public void setValidationResponses(final List<QuestionValidationResponse> validationResponses) {
        this.validationResponses = validationResponses;
    }
}
