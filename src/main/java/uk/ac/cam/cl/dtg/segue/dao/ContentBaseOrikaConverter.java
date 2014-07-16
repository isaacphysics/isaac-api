package uk.ac.cam.cl.dtg.segue.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.dtg.segue.dos.content.Content;
import uk.ac.cam.cl.dtg.segue.dos.content.ContentBase;
import uk.ac.cam.cl.dtg.segue.dto.content.ContentBaseDTO;
import uk.ac.cam.cl.dtg.segue.dto.content.ContentDTO;
import ma.glasnost.orika.CustomConverter;
import ma.glasnost.orika.metadata.Type;

/**
 * ContentBaseOrikaConverter A specialist converter class to work with the Orika
 * automapper library.
 * 
 * Responsible for converting Content objects to their correct subtype.
 * 
 */
public class ContentBaseOrikaConverter extends
		CustomConverter<ContentBase, ContentBaseDTO> {
	private static final Logger log = LoggerFactory
			.getLogger(ContentBaseOrikaConverter.class);

	private ContentMapper contentMapper;

	/**
	 * Constructs an Orika Converter specialises in selecting the correct
	 * subclass for content objects.
	 * 
	 * @param contentMapper
	 *            - An instance of a preconfigured content mapper that knows
	 *            about the content inheritance hierarchy.
	 */
	public ContentBaseOrikaConverter(final ContentMapper contentMapper) {
		this.contentMapper = contentMapper;
	}

	@Override
	public ContentBaseDTO convert(final ContentBase arg0,
			final Type<? extends ContentBaseDTO> arg1) {

		if (null == arg0) {
			return null;
		}

		Class<? extends Content> contentClass = contentMapper
				.getClassByType(arg0.getType());

		if (contentClass == null) {
			// if we cannot figure out what content object default to content.
			contentClass = Content.class;
		}

		Class<? extends ContentDTO> destinationClass = contentMapper
				.getDTOClassByDOClass(contentClass);

		if (destinationClass == null) {
			log.error("Error - unable to locate DTO class from DO class ");
			return null;
		}

		return super.mapperFacade.map(arg0, destinationClass);
	}

}
