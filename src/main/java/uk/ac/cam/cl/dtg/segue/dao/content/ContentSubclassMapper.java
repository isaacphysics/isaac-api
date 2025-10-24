/*
 * Copyright 2014 Stephen Cummins
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.cam.cl.dtg.segue.dao.content;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.api.client.util.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacQuestionBase;
import uk.ac.cam.cl.dtg.isaac.dos.QuestionValidationResponse;
import uk.ac.cam.cl.dtg.isaac.dos.content.Choice;
import uk.ac.cam.cl.dtg.isaac.dos.content.Content;
import uk.ac.cam.cl.dtg.isaac.dos.content.ContentBase;
import uk.ac.cam.cl.dtg.isaac.dos.content.DTOMapping;
import uk.ac.cam.cl.dtg.isaac.dos.content.Item;
import uk.ac.cam.cl.dtg.isaac.dos.content.JsonContentType;
import uk.ac.cam.cl.dtg.isaac.dos.content.LLMMarkingExpression;
import uk.ac.cam.cl.dtg.isaac.dos.content.SeguePage;
import uk.ac.cam.cl.dtg.isaac.dos.content.SidebarEntry;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.SeguePageDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.SidebarDTO;
import uk.ac.cam.cl.dtg.segue.dao.JsonLoader;
import uk.ac.cam.cl.dtg.segue.dao.users.QuestionValidationResponseDeserializer;
import uk.ac.cam.cl.dtg.util.mappers.MainMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Class responsible for mapping Content objects (or contentBase objects) to their respective subclass.
 */
public class ContentSubclassMapper {
    private static final Logger log = LoggerFactory.getLogger(ContentSubclassMapper.class);

    // Used for serialization into the correct POJO as well as deserialization.
    // Currently depends on the string key being the same text value as the type
    // field.
    private final Map<String, Class<? extends Content>> jsonTypes;
    private final Map<Class<? extends Content>, Class<? extends ContentDTO>> mapOfDOsToDTOs;
    
    private static ObjectMapper preconfiguredObjectMapper;

    /**
     * Creates a new content mapper without type information.
     * 
     * Note: Type information must be provided by using the register type methods.
     * 
     */
    @Inject
    public ContentSubclassMapper() {
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
    public ContentSubclassMapper(final Reflections configuredReflectionClass) {
        this();
        Objects.requireNonNull(configuredReflectionClass);

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
     * This method will accept a json string and will return a Content object (or one of its subtypes).
     * 
     * @param docJson
     *            - to load
     * @return A Content object or one of its registered sub classes
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
     * Registers JsonTypes using class annotation.
     * 
     * @param cls
     *            - the class to extract the jsontype value from.
     */
    public synchronized void registerJsonType(final Class<? extends Content> cls) {
        Objects.requireNonNull(cls, "Class cannot be null.");

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
        Objects.requireNonNull(cls, "Class cannot be null.");

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
     *  Populate the DTO object sidebar with a placeholder, if a page type.
     *
     *  This is necessary since the DO has a String but the DTO has a Sidebar object,
     *  which cannot be automapped and would otherwise lead to the ID of the sidebar
     *  being lost in the DTO conversion.
     *
     * @param content the DO version of the page object to extract the ID from.
     * @param result the DTO version of the page object to augment.
     */
    private void populateSidebarWithIDs(final Content content, final ContentDTO result) {
        // No need for recursion, since we don't support SeguePages that aren't top-level.
        if (content instanceof SeguePage && result instanceof SeguePageDTO) {
            SeguePage seguePage = (SeguePage) content;
            SeguePageDTO seguePageDTO = (SeguePageDTO) result;
            if (null != seguePage.getSidebar() && !seguePage.getSidebar().isEmpty()) {
                SidebarDTO placeholder = new SidebarDTO();
                placeholder.setId(seguePage.getSidebar());
                seguePageDTO.setSidebar(placeholder);
            }
        }
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
        ContentDTO result = MainMapper.INSTANCE.map(content);
        populateSidebarWithIDs(content, result);
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
        Objects.requireNonNull(contentDOList);

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
     * deserialized using the custom deserializer.
     * 
     * This object Mapper is shared and should be treated as immutable.
     * 
     * @return a jackson object mapper.
     */
    public ObjectMapper getSharedContentObjectMapper() {
        if (ContentSubclassMapper.preconfiguredObjectMapper != null) {
            return preconfiguredObjectMapper;
        }

        log.info("Initialising preconfiguredObjectMapper and caching it.");
        
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

        List<Content> contentList = new ArrayList<>();

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
     * Creates a brand new object mapper.
     * This should be used sparingly as it is resource intensive to maintain these things.
     * 
     * @return ObjectMapper that has been configured to handle the segue recursive object model.
     */
    public ObjectMapper generateNewPreconfiguredContentMapper() {
        ContentBaseDeserializer contentDeserializer = new ContentBaseDeserializer();
        contentDeserializer.registerTypeMap(jsonTypes);

        /* When deserialising from Git, the top-level content mapper needs to have an ItemDeserializer,
           and when deserialising a Choice object directly then the ChoiceDeserializer needs to have an
           ItemDeserializer inside it too. The perils of a recursive content model! */
        ItemDeserializer itemDeserializer = new ItemDeserializer(contentDeserializer);
        ChoiceDeserializer choiceDeserializer = new ChoiceDeserializer(contentDeserializer, itemDeserializer);

        QuestionValidationResponseDeserializer validationResponseDeserializer 
            = new QuestionValidationResponseDeserializer(contentDeserializer, choiceDeserializer);

        IsaacQuestionBaseDeserializer isaacQuestionBaseDeserializer =
                new IsaacQuestionBaseDeserializer(contentDeserializer);

        SidebarEntryDeserializer sidebarEntryDeserializer = new SidebarEntryDeserializer(contentDeserializer);

        SimpleModule contentDeserializerModule = new SimpleModule("ContentDeserializerModule");
        contentDeserializerModule.addDeserializer(ContentBase.class, contentDeserializer);
        contentDeserializerModule.addDeserializer(IsaacQuestionBase.class, isaacQuestionBaseDeserializer);
        contentDeserializerModule.addDeserializer(Choice.class, choiceDeserializer);
        contentDeserializerModule.addDeserializer(Item.class, itemDeserializer);
        contentDeserializerModule.addDeserializer(QuestionValidationResponse.class, validationResponseDeserializer);
        contentDeserializerModule.addDeserializer(LLMMarkingExpression.class, new LLMMarkingExpressionDeserializer());
        contentDeserializerModule.addDeserializer(SidebarEntry.class, sidebarEntryDeserializer);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        objectMapper.registerModule(contentDeserializerModule);
        
        return objectMapper;
    }
}
