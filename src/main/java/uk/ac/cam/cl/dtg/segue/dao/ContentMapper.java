package uk.ac.cam.cl.dtg.segue.dao;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.Validate;
import org.mongojack.internal.MongoJackModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.dtg.isaac.models.JsonType;
import uk.ac.cam.cl.dtg.segue.dto.Content;
import uk.ac.cam.cl.dtg.segue.dto.ContentBase;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.mongodb.DBObject;

/**
 * Class responsible for mapping Content objects (or contentBase objects) to their respective subclass. 
 *
 */
public class ContentMapper {
	private static final Logger log = LoggerFactory.getLogger(ContentMapper.class);
	
	// Used for serialization into the correct POJO as well as deserialization. Currently depends on the string key being the same text value as the type field.
	private final Map<String, Class<? extends Content>> jsonTypes;
	
	/**
	 * Creates a new content mapper
	 * 
	 */
	public ContentMapper() {
		this.jsonTypes = new ConcurrentHashMap<String, Class<? extends Content>>();
	}
	
	/**
	 * Creates a new content mapper initialized with a set of types. 
	 * 
	 * @param additionalTypes
	 */
	public ContentMapper(Map<String, Class<? extends Content>> additionalTypes) {
		this();
		
		Validate.notNull(additionalTypes);
		jsonTypes.putAll(additionalTypes);
	}

	/**
	 * This method will accept a json string and will return a Content object (or one of its subtypes)
	 * @param json
	 * @return A Content object or one of its registered sub classes
	 * @throws JsonParseException
	 * @throws JsonMappingException
	 * @throws IOException
	 */
	public Content load(String docJson) throws JsonParseException, JsonMappingException, IOException {
		Content c = JsonLoader.load(docJson, Content.class, true);

		Class<? extends Content> cls = jsonTypes.get(c.getType());
		
		if (cls != null)
			return JsonLoader.load(docJson, cls);
		else
			return JsonLoader.load(docJson, Content.class);
	}
	
	/**
	 * Map a DBObject into the appropriate Content DTO, without having to know  what type it is.
	 * 
	 * It so happens that RestEasy will correctly serialize Content or any of its subtypes when it is provided with an object from this method (without having to do instanceof checks or anything).
	 * 
	 * @param reference to the DBObject obj
	 * @return A content object or any subclass of Content or Null if the obj param is not provided.
	 * @throws IllegalArgumentException if the database item retrieved fails to map into a content object.
	 */
	public Content mapDBOjectToContentDTO(DBObject obj) throws IllegalArgumentException {
		Validate.notNull(obj);
		
		// Create an ObjectMapper capable of deserializing mongo ObjectIDs
		ObjectMapper contentMapper = MongoJackModule.configure(new ObjectMapper());
		
		// Find out what type label the JSON object has 
		String labelledType = (String)obj.get("type");

		// Lookup the matching POJO class
		Class<? extends Content> contentClass = jsonTypes.get(labelledType); // Returns null if no entry for this type

		if (null == contentClass) {
			// We haven't registered this type. Deserialize into the Content base class.

			return contentMapper.convertValue(obj, Content.class); 
		} else {
			
			// We have a registered POJO class. Deserialize into it.
			// TODO: Work out whether we should configure the contentMapper to ignore missing fields in this case. 
			return contentMapper.convertValue(obj, contentClass);  
		}
	}
	
	/**
	 * Register a new content type with the content mapper
	 * 
	 * @param type - String that should match the type field of the content object.
	 * @param cls - Class implementing the deserialized DTO.
	 */
	public synchronized void registerJsonType(String type, Class<? extends Content> cls) {
		Validate.notEmpty(type, "Invalid type string entered. It cannot be empty.");
		Validate.notNull(cls, "Class cannot be null.");
		
		jsonTypes.put(type, cls);
	}
	
	/**
	 * Works the same as {@link #registerJsonType(String, Class)}
	 * @see #registerJsonType(String, Class)
	 * @param newTypes
	 */
	public synchronized void registerJsonTypes(Map<String, Class<? extends Content>> newTypes){
		Validate.notNull(newTypes, "New types map cannot be null");
		log.info("Adding new content Types to Segue: " +  newTypes.keySet().toString());
		jsonTypes.putAll(newTypes);
	}
	
	public synchronized void registerJsonType(Class<? extends Content> cls) {
		Validate.notNull(cls, "Class cannot be null.");
		
		JsonType jt = cls.getAnnotation(JsonType.class);
		if (jt != null)
			jsonTypes.put(jt.value(), cls);
	}
	
	public Class<? extends Content> getClassByType(String type){
		return jsonTypes.get(type);
	}
	
	/**
	 * Provides a preconfigured module that can be added to an object mapper so that contentBase objects can be deseerialized using the custom deserializer.
	 * @return
	 */
	public ObjectMapper getContentObjectMapper(){ 
	    ContentBaseDeserializer contentDeserializer = new ContentBaseDeserializer();
	    contentDeserializer.registerTypeMap(jsonTypes);
	    		
	    SimpleModule contentDeserializerModule = new SimpleModule("ContentDeserializerModule");
	    contentDeserializerModule.addDeserializer(ContentBase.class, contentDeserializer);
	    
	    ObjectMapper objectMapper = new ObjectMapper();
	    objectMapper.registerModule(contentDeserializerModule);

	    return objectMapper;
	}
	
	/**
	 * Map a list of String to a List of Content
	 * 
	 * @param stringList
	 * @return Content List
	 */
	public List<Content> mapFromStringListToContentList(List<String> stringList){
		// setup object mapper to use preconfigured deserializer module. Required to deal with type polymorphism
	    ObjectMapper objectMapper = this.getContentObjectMapper();
	    
	    List<Content> contentList = new ArrayList<Content>();
	    
	    for(String item : stringList){
	    	try {
				contentList.add((Content) objectMapper.readValue(item, ContentBase.class));
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
}
