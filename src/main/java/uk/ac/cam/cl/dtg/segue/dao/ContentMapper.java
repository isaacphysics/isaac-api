package uk.ac.cam.cl.dtg.segue.dao;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.Validate;
import org.dozer.Mapper;
import org.elasticsearch.common.collect.Maps;
import org.mongojack.internal.MongoJackModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.dtg.segue.dos.content.Choice;
import uk.ac.cam.cl.dtg.segue.dos.content.Content;
import uk.ac.cam.cl.dtg.segue.dos.content.ContentBase;
import uk.ac.cam.cl.dtg.segue.dos.content.DTOMapping;
import uk.ac.cam.cl.dtg.segue.dos.content.JsonType;
import uk.ac.cam.cl.dtg.segue.dto.content.ContentDTO;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.mongodb.DBObject;

/**
 * Class responsible for mapping Content objects (or contentBase objects) to
 * their respective subclass.
 */
public class ContentMapper {
	private static final Logger log = LoggerFactory
			.getLogger(ContentMapper.class);

	// Used for serialization into the correct POJO as well as deserialization.
	// Currently depends on the string key being the same text value as the type
	// field.
	private final Map<String, Class<? extends Content>> jsonTypes;

	private final Map<Class<? extends Content>, Class<? extends ContentDTO>> mapOfDOsToDTOs;

	private final Mapper dozerDOandDTOMapper;

	/**
	 * Creates a new content mapper without type information.
	 * 
	 * Note: Type information must be provided by using the register type
	 * methods.
	 * 
	 * @param dozerDOandDTOMapper
	 *            - the mapper to use for DO and DTO mapping.
	 */
	public ContentMapper(final Mapper dozerDOandDTOMapper) {
		Validate.notNull(dozerDOandDTOMapper);
		jsonTypes = Maps.newConcurrentMap();
		mapOfDOsToDTOs = Maps.newConcurrentMap();
		this.dozerDOandDTOMapper = dozerDOandDTOMapper;
	}

	/**
	 * Creates a new content mapper initialized with a set of types.
	 * 
	 * @param additionalTypes
	 *            - types to add to our look up map.
	 * @param dozerDOandDTOMapper
	 *            - instance of the autoMapper.
	 * @param mapOfDOsToDTOs
	 *            - map of DOs To DTOs.
	 */
	public ContentMapper(
			final Map<String, Class<? extends Content>> additionalTypes,
			final Mapper dozerDOandDTOMapper,
			final Map<Class<? extends Content>, Class<? extends ContentDTO>> mapOfDOsToDTOs) {
		Validate.notNull(additionalTypes);
		this.jsonTypes = new ConcurrentHashMap<String, Class<? extends Content>>();
		jsonTypes.putAll(additionalTypes);

		this.dozerDOandDTOMapper = dozerDOandDTOMapper;

		this.mapOfDOsToDTOs = Maps.newConcurrentMap();

		if (mapOfDOsToDTOs != null) {
			this.mapOfDOsToDTOs.putAll(mapOfDOsToDTOs);
		}
	}

	/**
	 * This method will accept a json string and will return a Content object
	 * (or one of its subtypes).
	 * 
	 * @param docJson
	 *            - to load
	 * @return A Content object or one of its registered sub classes
	 * @throws JsonParseException
	 * @throws JsonMappingException
	 * @throws IOException
	 *             - if there is a problem with IO
	 */
	public Content load(final String docJson) throws IOException {
		Content c = JsonLoader.load(docJson, Content.class, true);

		Class<? extends Content> cls = jsonTypes.get(c.getType());

		if (cls != null) {
			return JsonLoader.load(docJson, cls);
		} else {
			return JsonLoader.load(docJson, Content.class);
		}
	}

	/**
	 * Map a DBObject into the appropriate Content DTO, without having to know
	 * what type it is.
	 * 
	 * It so happens that RestEasy will correctly serialize Content or any of
	 * its subtypes when it is provided with an object from this method (without
	 * having to do instanceof checks or anything).
	 * 
	 * @param obj
	 *            to the DBObject obj
	 * @return A content object or any subclass of Content or Null if the obj
	 *         param is not provided.
	 */
	public Content mapDBOjectToContentDO(final DBObject obj) {
		Validate.notNull(obj);

		// Create an ObjectMapper capable of deserializing mongo ObjectIDs
		ObjectMapper contentMapper = MongoJackModule
				.configure(new ObjectMapper());

		// Find out what type label the JSON object has
		String labelledType = (String) obj.get("type");

		// Lookup the matching POJO class
		Class<? extends Content> contentClass = jsonTypes.get(labelledType);

		if (null == contentClass) {
			// We haven't registered this type. Deserialize into the Content
			// base class.

			return contentMapper.convertValue(obj, Content.class);
		} else {

			// We have a registered POJO class. Deserialize into it.
			return contentMapper.convertValue(obj, contentClass);
		}
	}

	/**
	 * Register a new content type with the content mapper.
	 * 
	 * @param type
	 *            - String that should match the type field of the content
	 *            object.
	 * @param cls
	 *            - Class implementing the deserialized DO.
	 */
	public synchronized void registerJsonTypeToDO(final String type,
			final Class<? extends Content> cls) {
		Validate.notEmpty(type,
				"Invalid type string entered. It cannot be empty.");
		Validate.notNull(cls, "Class cannot be null.");

		jsonTypes.put(type, cls);
	}

	/**
	 * Works the same as {@link #registerJsonTypeToDO(String, Class)}.
	 * 
	 * @see #registerJsonTypeToDO(String, Class)
	 * @param newTypes
	 *            a map of types to merge with the segue type map. The classes added to the map
	 *            must contain jsonType annotations and may contain DTOMapping annotations.
	 */
	public synchronized void registerJsonTypes(
			final List<Class<? extends Content>> newTypes) {
		Validate.notNull(newTypes, "New types map cannot be null");
		
		log.info("Adding new content Types to Segue");
		
		for (Class<? extends Content> contentClass : newTypes) {
			this.registerJsonTypeAndDTOMapping(contentClass);
		}
	}

	/**
	 * Registers JsonTypes using class annotation.
	 * 
	 * @param cls
	 *            - the class to extract the jsontype value from.
	 */
	public synchronized void registerJsonType(final Class<? extends Content> cls) {
		Validate.notNull(cls, "Class cannot be null.");

		JsonType jt = cls.getAnnotation(JsonType.class);
		if (jt != null) {
			jsonTypes.put(jt.value(), cls);
		} else {
			log.error("The jsonType annotation type provided cannot be null. For the class " + cls);
		}
	}

	/**
	 * Registers DTOMapping using class annotation.
	 * 
	 * @param cls
	 *            - the class to extract the jsontype value from.
	 */
	public synchronized void registerDTOMapping(
			final Class<? extends Content> cls) {
		Validate.notNull(cls, "Class cannot be null.");

		DTOMapping dtoMapping = cls.getAnnotation(DTOMapping.class);
		if (dtoMapping != null) {
			this.mapOfDOsToDTOs.put(cls, dtoMapping.value());
		} else {
			log.warn("The DTO mapping provided is null or the annotation is not present"
					+ " for the class " 
					+ cls 
					+ ". This class cannot be auto mapped from DO to DTO.");
		}
	}

	/**
	 * Registers JsonTypes and DTO mappings using class annotation.
	 * 
	 * @param cls
	 *            - the class to extract the jsontype value from.
	 */
	public synchronized void registerJsonTypeAndDTOMapping(
			final Class<? extends Content> cls) {
		this.registerJsonType(cls);
		this.registerDTOMapping(cls);
	}

	/**
	 * Find the class that implements the content DO based on a type string.
	 * 
	 * @param type
	 *            - string to lookup
	 * @return the content DO class.
	 */
	public Class<? extends Content> getClassByType(final String type) {
		return jsonTypes.get(type);
	}

	/**
	 * Provides a pre-configured module that can be added to an object mapper so
	 * that contentBase objects can be deseerialized using the custom
	 * deserializer.
	 * 
	 * @return a jackson object mapper.
	 */
	public ObjectMapper getContentObjectMapper() {
		ContentBaseDeserializer contentDeserializer = new ContentBaseDeserializer();
		contentDeserializer.registerTypeMap(jsonTypes);
		ChoiceDeserializer choiceDeserializer = new ChoiceDeserializer(
				contentDeserializer);

		SimpleModule contentDeserializerModule = new SimpleModule(
				"ContentDeserializerModule");
		contentDeserializerModule.addDeserializer(ContentBase.class,
				contentDeserializer);
		contentDeserializerModule.addDeserializer(Choice.class,
				choiceDeserializer);

		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.registerModule(contentDeserializerModule);

		return objectMapper;
	}

	/**
	 * Map a list of String to a List of Content.
	 * 
	 * @param stringList
	 *            - Converts a list of strings to a list of content
	 * @return Content List
	 */
	public List<Content> mapFromStringListToContentList(
			final List<String> stringList) {
		// setup object mapper to use preconfigured deserializer module.
		// Required to deal with type polymorphism
		ObjectMapper objectMapper = this.getContentObjectMapper();

		List<Content> contentList = new ArrayList<Content>();

		for (String item : stringList) {
			try {
				contentList.add((Content) objectMapper.readValue(item,
						ContentBase.class));
			} catch (JsonParseException e) {
				e.printStackTrace();
			} catch (JsonMappingException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return contentList;
	}

	/**
	 * Gets the instance of the Dozer auto mapper.
	 * 
	 * @return Auto Mapper.
	 */
	public Mapper getDTOandDOMapper() {
		return this.dozerDOandDTOMapper;
	}

}
