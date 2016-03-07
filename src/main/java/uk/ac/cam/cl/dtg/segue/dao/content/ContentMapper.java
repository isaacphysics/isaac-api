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
package uk.ac.cam.cl.dtg.segue.dao.content;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import ma.glasnost.orika.MapperFacade;
import ma.glasnost.orika.MapperFactory;
import ma.glasnost.orika.converter.ConverterFactory;
import ma.glasnost.orika.impl.DefaultMapperFactory;

import org.apache.commons.lang3.Validate;
import org.elasticsearch.common.collect.Maps;
import org.mongojack.internal.MongoJackModule;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.dtg.segue.dao.JsonLoader;
import uk.ac.cam.cl.dtg.segue.dao.users.AnonymousUserQuestionAttemptsOrikaConverter;
import uk.ac.cam.cl.dtg.segue.dao.users.QuestionValidationResponseDeserializer;
import uk.ac.cam.cl.dtg.segue.dao.users.QuestionValidationResponseOrikaConverter;
import uk.ac.cam.cl.dtg.segue.dos.QuestionValidationResponse;
import uk.ac.cam.cl.dtg.segue.dos.content.Choice;
import uk.ac.cam.cl.dtg.segue.dos.content.Content;
import uk.ac.cam.cl.dtg.segue.dos.content.ContentBase;
import uk.ac.cam.cl.dtg.segue.dos.content.DTOMapping;
import uk.ac.cam.cl.dtg.segue.dos.content.JsonContentType;
import uk.ac.cam.cl.dtg.segue.dos.users.AnonymousUser;
import uk.ac.cam.cl.dtg.segue.dto.content.ContentDTO;
import uk.ac.cam.cl.dtg.segue.dto.content.ContentSummaryDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.AnonymousUserDTO;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.api.client.util.Lists;
import com.google.inject.Inject;
import com.mongodb.DBObject;

/**
 * Class responsible for mapping Content objects (or contentBase objects) to their respective subclass.
 */
public class ContentMapper {
    private static final Logger log = LoggerFactory.getLogger(ContentMapper.class);

    // Used for serialization into the correct POJO as well as deserialization.
    // Currently depends on the string key being the same text value as the type
    // field.
    private final Map<String, Class<? extends Content>> jsonTypes;
    private final Map<Class<? extends Content>, Class<? extends ContentDTO>> mapOfDOsToDTOs;

    // this autoMapper is initialised lazily in the getAutoMapper method
    private MapperFacade autoMapper = null;
    
    private static ObjectMapper preconfiguredObjectMapper;

    /**
     * Creates a new content mapper without type information.
     * 
     * Note: Type information must be provided by using the register type methods.
     * 
     */
    @Inject
    public ContentMapper() {
        jsonTypes = Maps.newConcurrentMap();
        mapOfDOsToDTOs = Maps.newConcurrentMap();
    }
    
    /**
     * Alternative constructor that will attempt to search for valid types to pre-register.
     * 
     * @param configuredReflectionClass
     *            - string representing the parent package to search for content classes. e.g. uk.ac.cam.cl.dtg.segue
     */
    @SuppressWarnings("unchecked")
    public ContentMapper(final Reflections configuredReflectionClass) {
        this();
        Validate.notNull(configuredReflectionClass);

        // We need to pre-register different content objects here for the
        // auto-mapping to work
        Set<Class<?>> annotated = configuredReflectionClass.getTypesAnnotatedWith(JsonContentType.class);

        for (Class<?> classToAdd : annotated) {
            if (Content.class.isAssignableFrom(classToAdd)) {
                this.registerJsonTypeAndDTOMapping((Class<Content>) classToAdd);
            }
        }
    }

    /**
     * Creates a new content mapper initialized with a set of types.
     * 
     * @param additionalTypes
     *            - types to add to our look up map.
     * @param mapOfDOsToDTOs
     *            - map of DOs To DTOs.
     */
    public ContentMapper(final Map<String, Class<? extends Content>> additionalTypes,
            final Map<Class<? extends Content>, Class<? extends ContentDTO>> mapOfDOsToDTOs) {
        Validate.notNull(additionalTypes);

        this.jsonTypes = new ConcurrentHashMap<String, Class<? extends Content>>();
        jsonTypes.putAll(additionalTypes);

        this.mapOfDOsToDTOs = Maps.newConcurrentMap();

        if (mapOfDOsToDTOs != null) {
            this.mapOfDOsToDTOs.putAll(mapOfDOsToDTOs);
        }
    }

    /**
     * This method will accept a json string and will return a Content object (or one of its subtypes).
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
     * Map a DBObject into the appropriate Content DTO, without having to know what type it is.
     * 
     * It so happens that RestEasy will correctly serialize Content or any of its subtypes when it is provided with an
     * object from this method (without having to do instanceof checks or anything).
     * 
     * @param obj
     *            to the DBObject obj
     * @return A content object or any subclass of Content or Null if the obj param is not provided.
     */
    public Content mapDBObjectToContentDO(final DBObject obj) {
        Validate.notNull(obj);

        // Create an ObjectMapper capable of deserializing mongo ObjectIDs
        ObjectMapper contentMapper = MongoJackModule.configure(new ObjectMapper());

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
     *            - String that should match the type field of the content object.
     * @param cls
     *            - Class implementing the deserialized DO.
     */
    public synchronized void registerJsonTypeToDO(final String type, final Class<? extends Content> cls) {
        Validate.notEmpty(type, "Invalid type string entered. It cannot be empty.");
        Validate.notNull(cls, "Class cannot be null.");

        jsonTypes.put(type, cls);
    }

    /**
     * Works the same as {@link #registerJsonTypeToDO(String, Class)}.
     * 
     * @see #registerJsonTypeToDO(String, Class)
     * @param newTypes
     *            a map of types to merge with the segue type map. The classes added to the map must contain jsonType
     *            annotations and may contain DTOMapping annotations.
     */
    public synchronized void registerJsonTypes(final List<Class<? extends Content>> newTypes) {
        Validate.notNull(newTypes, "New types map cannot be null");
        StringBuilder sb = new StringBuilder();
        sb.append("Adding new content Types to Segue: ");

        for (Class<? extends Content> contentClass : newTypes) {
            this.registerJsonTypeAndDTOMapping(contentClass);
            sb.append(contentClass.toString());
            sb.append(", ");
        }

        log.info(sb.toString());
    }

    /**
     * Registers JsonTypes using class annotation.
     * 
     * @param cls
     *            - the class to extract the jsontype value from.
     */
    public synchronized void registerJsonType(final Class<? extends Content> cls) {
        Validate.notNull(cls, "Class cannot be null.");

        JsonContentType jt = cls.getAnnotation(JsonContentType.class);
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
    @SuppressWarnings("unchecked")
    public synchronized void registerDTOMapping(final Class<? extends Content> cls) {
        Validate.notNull(cls, "Class cannot be null.");

        DTOMapping dtoMapping = cls.getAnnotation(DTOMapping.class);
        if (dtoMapping != null && ContentDTO.class.isAssignableFrom(dtoMapping.value())) {
            this.mapOfDOsToDTOs.put(cls, (Class<? extends ContentDTO>) dtoMapping.value());
        } else {
            log.error("The DTO mapping provided is null or the annotation is not present" + " for the class " + cls
                    + ". This class cannot be auto mapped from DO to DTO.");
        }
    }

    /**
     * Registers JsonTypes and DTO mappings using class annotation.
     * 
     * @param cls
     *            - the class to extract the jsontype value from.
     */
    public synchronized void registerJsonTypeAndDTOMapping(final Class<? extends Content> cls) {
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
     * Get a DTOClass based on a DOClass.
     * 
     * @param cls
     *            - DO class.
     * @return DTO class.
     */
    public Class<? extends ContentDTO> getDTOClassByDOClass(final Class<? extends Content> cls) {
        return mapOfDOsToDTOs.get(cls);
    }

    /**
     * Find the default DTO class from a given Domain object.
     * 
     * @param content
     *            - Content DO to map to DTO.
     * @return DTO that can be used for mapping.
     */
    public ContentDTO getDTOByDO(final Content content) {
        if (null == content) {
            return null;
        }

        ContentDTO result = getAutoMapper().map(content, this.mapOfDOsToDTOs.get(content.getClass()));
        if (result.getRelatedContent() != null) {
            List<ContentSummaryDTO> relatedContent = Lists.newArrayList();

            for (String relatedId : content.getRelatedContent()) {
                ContentSummaryDTO contentSummary = new ContentSummaryDTO();
                contentSummary.setId(relatedId);
                relatedContent.add(contentSummary);
            }
            result.setRelatedContent(relatedContent);
        }

        return result;
    }

    /**
     * Converts the DO list to a list of DTOs.
     * 
     * @see #getDTOByDO(Content)
     * @param contentDOList
     *            - list of objects to convert.
     * @return the list of DTOs
     */
    public List<ContentDTO> getDTOByDOList(final List<Content> contentDOList) {
        Validate.notNull(contentDOList);

        List<ContentDTO> resultList = Lists.newArrayList();
        for (Content c : contentDOList) {
            if (this.mapOfDOsToDTOs.get(c.getClass()) != null) {
                resultList.add(this.getDTOByDO(c));
            } else {
                log.error("Unable to find DTO mapping class");
            }
        }

        return resultList;
    }

    /**
     * Provides a pre-configured module that can be added to an object mapper so that contentBase objects can be
     * deseerialized using the custom deserializer.
     * 
     * This object Mapper is shared and should be treated as immutable.
     * 
     * @return a jackson object mapper.
     */
    public ObjectMapper getSharedContentObjectMapper() {
        if (ContentMapper.preconfiguredObjectMapper != null) {
            return preconfiguredObjectMapper;
        }
        
        preconfiguredObjectMapper = generateNewPreconfiguredContentMapper();

        return preconfiguredObjectMapper;
    }

    /**
     * Map a list of String to a List of Content.
     * 
     * @param stringList
     *            - Converts a list of strings to a list of content
     * @return Content List
     */
    public List<Content> mapFromStringListToContentList(final List<String> stringList) {
        // setup object mapper to use preconfigured deserializer module.
        // Required to deal with type polymorphism
        ObjectMapper objectMapper = this.getSharedContentObjectMapper();

        List<Content> contentList = new ArrayList<Content>();

        for (String item : stringList) {
            try {
                contentList.add((Content) objectMapper.readValue(item, ContentBase.class));
            } catch (IOException e) {
                log.error("Error whilst mapping from string to list of content", e);
            }
        }
        return contentList;
    }

    /**
     * Get an instance of the automapper which has been configured to cope with recursive content objects. This
     * automapper is more efficient than the jackson one as there is no intermediate representation.
     * 
     * @return autoMapper
     */
    public MapperFacade getAutoMapper() {
        if (null == this.autoMapper) {
            log.info("Creating instance of content auto mapper.");
            MapperFactory mapperFactory = new DefaultMapperFactory.Builder().build();

            ContentBaseOrikaConverter contentConverter = new ContentBaseOrikaConverter(this);
            ChoiceOrikaConverter choiceConverter = new ChoiceOrikaConverter();

            QuestionValidationResponseOrikaConverter questionValidationResponseConverter 
                = new QuestionValidationResponseOrikaConverter();

            AnonymousUserQuestionAttemptsOrikaConverter anonymousUserOrikaConverter 
                = new AnonymousUserQuestionAttemptsOrikaConverter();

            ConverterFactory converterFactory = mapperFactory.getConverterFactory();

            converterFactory.registerConverter(contentConverter);
            converterFactory.registerConverter(choiceConverter);
            converterFactory.registerConverter(questionValidationResponseConverter);
            converterFactory.registerConverter("anonymousUserAttemptsToDTOConverter", anonymousUserOrikaConverter);

            // special rules
            mapperFactory.classMap(AnonymousUser.class, AnonymousUserDTO.class).fieldMap("temporaryQuestionAttempts")
                    .converter("anonymousUserAttemptsToDTOConverter").add().byDefault().register();

            this.autoMapper = mapperFactory.getMapperFacade();
        }

        return this.autoMapper;
    }
    
    /**
     * Creates a brand new object mapper.
     * This should be used sparingly as it is resource intensive to maintain these things.
     * 
     * @return ObjectMapper that has been configured to handle the segue recursive object model.
     */
    public ObjectMapper generateNewPreconfiguredContentMapper() {
        ContentBaseDeserializer contentDeserializer = new ContentBaseDeserializer();
        contentDeserializer.registerTypeMap(jsonTypes);
        
        ChoiceDeserializer choiceDeserializer = new ChoiceDeserializer(contentDeserializer);

        QuestionValidationResponseDeserializer validationResponseDeserializer 
            = new QuestionValidationResponseDeserializer(
                contentDeserializer, choiceDeserializer);

        SimpleModule contentDeserializerModule = new SimpleModule("ContentDeserializerModule");
        contentDeserializerModule.addDeserializer(ContentBase.class, contentDeserializer);
        contentDeserializerModule.addDeserializer(Choice.class, choiceDeserializer);
        contentDeserializerModule.addDeserializer(QuestionValidationResponse.class, validationResponseDeserializer);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(contentDeserializerModule);
        
        return objectMapper;
    }
}
