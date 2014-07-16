package uk.ac.cam.cl.dtg.segue.api;

import uk.ac.cam.cl.dtg.segue.dao.ChoiceOrikaConverter;
import uk.ac.cam.cl.dtg.segue.dao.ContentBaseOrikaConverter;
import uk.ac.cam.cl.dtg.segue.dao.ContentMapper;
import ma.glasnost.orika.MapperFacade;
import ma.glasnost.orika.MapperFactory;
import ma.glasnost.orika.converter.ConverterFactory;
import ma.glasnost.orika.impl.DefaultMapperFactory;

/**
 * Segue Object Mapper This class is responsible for setting up and configuring
 * the object to object mapper used by Segue.
 * 
 */
public class SegueObjectMapper {
	private MapperFacade mapper;

	/**
	 * SegueObjectMapper Create an instance of the object mapper.
	 * 
	 * @param mapper
	 *            - The content Mapper instance preconfigured with information
	 *            on the type hierarchy for content objects.
	 */
	public SegueObjectMapper(final ContentMapper mapper) {
		MapperFactory mapperFactory = new DefaultMapperFactory.Builder()
				.build();

		// Register Content Converter - TODO: This should be made more generic.
		ContentBaseOrikaConverter contentConverter = new ContentBaseOrikaConverter(
				mapper);
		
		ChoiceOrikaConverter choiceConverter = new ChoiceOrikaConverter(contentConverter);
		
		ConverterFactory converterFactory = mapperFactory.getConverterFactory();
		converterFactory.registerConverter(contentConverter);
		converterFactory.registerConverter(choiceConverter);

		this.mapper = mapperFactory.getMapperFacade();
	}

	/**
	 * Gets the mapper.
	 * 
	 * @return the mapper
	 */
	public final MapperFacade getMapper() {
		return mapper;
	}
}
