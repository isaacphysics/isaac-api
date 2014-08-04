package uk.ac.cam.cl.dtg.segue.dao;

import uk.ac.cam.cl.dtg.segue.dos.QuantityValidationResponse;
import uk.ac.cam.cl.dtg.segue.dos.QuestionValidationResponse;
import uk.ac.cam.cl.dtg.segue.dto.QuantityValidationResponseDTO;
import uk.ac.cam.cl.dtg.segue.dto.QuestionValidationResponseDTO;
import ma.glasnost.orika.CustomConverter;
import ma.glasnost.orika.metadata.Type;

/**
 * QuestionValidationResponseOrikaConverter A specialist converter class to work
 * with the Orika automapper library.
 * 
 * Responsible for converting QuestionValidationResponse objects to their correct subtype.
 * 
 */
public class QuestionValidationResponseOrikaConverter extends
		CustomConverter<QuestionValidationResponse, QuestionValidationResponseDTO> {

	/**
	 * Constructs an Orika Converter specialises in selecting the correct
	 * subclass for choice objects.
	 * 
	 */
	public QuestionValidationResponseOrikaConverter() {

	}

	@Override
	public QuestionValidationResponseDTO convert(final QuestionValidationResponse source,
			final Type<? extends QuestionValidationResponseDTO> destinationType) {
		if (null == source) {
			return null;
		}

		if (source instanceof QuantityValidationResponse) {
			return super.mapperFacade.map(source, QuantityValidationResponseDTO.class);
		} else {
			// I would have expected this to cause an infinite loop / stack
			// overflow but apparently it doesn't.
			QuestionValidationResponseDTO questionValidationResponseDTO = new QuestionValidationResponseDTO();
			super.mapperFacade.map(source, questionValidationResponseDTO);
			return questionValidationResponseDTO;
		}
	}
}
