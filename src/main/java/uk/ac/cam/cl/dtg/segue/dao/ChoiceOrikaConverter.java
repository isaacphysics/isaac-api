package uk.ac.cam.cl.dtg.segue.dao;

import uk.ac.cam.cl.dtg.segue.dos.content.Choice;
import uk.ac.cam.cl.dtg.segue.dos.content.Quantity;
import uk.ac.cam.cl.dtg.segue.dto.content.ChoiceDTO;
import uk.ac.cam.cl.dtg.segue.dto.content.QuantityDTO;
import ma.glasnost.orika.CustomConverter;
import ma.glasnost.orika.MapperFactory;
import ma.glasnost.orika.impl.DefaultMapperFactory;
import ma.glasnost.orika.metadata.Type;

/**
 * ContentBaseOrikaConverter A specialist converter class to work with the Orika
 * automapper library.
 * 
 * Responsible for converting Choice objects to their correct subtype.
 * 
 */
public class ChoiceOrikaConverter extends CustomConverter<Choice, ChoiceDTO> {

	private ContentBaseOrikaConverter contentBaseOrikaConverter;

	/**
	 * Constructs an Orika Converter specialises in selecting the correct
	 * subclass for choice objects.
	 * 
	 * @param contentBaseOrikaConverter
	 *            - converter that can be used when mapping a plain choice
	 *            object to prevent infinite loops.
	 */
	public ChoiceOrikaConverter(
			final ContentBaseOrikaConverter contentBaseOrikaConverter) {
		this.contentBaseOrikaConverter = contentBaseOrikaConverter;
	}

	@Override
	public ChoiceDTO convert(final Choice source,
			final Type<? extends ChoiceDTO> destinationType) {
		if (null == source) {
			return null;
		}

		if (source instanceof Quantity) {
			return super.mapperFacade.map(source, QuantityDTO.class);
		} else {
			MapperFactory mapperFactory = new DefaultMapperFactory.Builder()
					.build();

			mapperFactory.getConverterFactory().registerConverter(
					contentBaseOrikaConverter);
			return mapperFactory.getMapperFacade().map(source, ChoiceDTO.class);
		}
	}
}
