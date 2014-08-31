/**
 * Copyright 2014 Stephen Cummins
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.cam.cl.dtg.segue.dao.users;

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
