/**
 * Copyright 2014 Stephen Cummins
 * <br>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * <br>
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * <br>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.ac.cam.cl.dtg.segue.dao.content;

import static java.util.Objects.requireNonNull;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.api.client.util.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.dos.QuestionValidationResponse;
import uk.ac.cam.cl.dtg.isaac.dos.content.Choice;
import uk.ac.cam.cl.dtg.isaac.dos.content.Content;
import uk.ac.cam.cl.dtg.isaac.dos.content.ContentBase;
import uk.ac.cam.cl.dtg.isaac.dos.content.DTOMapping;
import uk.ac.cam.cl.dtg.isaac.dos.content.Item;
import uk.ac.cam.cl.dtg.isaac.dos.content.JsonContentType;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentBaseDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentSummaryDTO;
import uk.ac.cam.cl.dtg.isaac.mappers.ContentMapper;
import uk.ac.cam.cl.dtg.segue.dao.JsonLoader;
import uk.ac.cam.cl.dtg.segue.dao.users.QuestionValidationResponseDeserializer;

/**
 * Class responsible for mapping Content objects (or contentBase objects) to their respective subclass.
 */
public class ContentMapperUtils {
  private static final Logger log = LoggerFactory.getLogger(ContentMapperUtils.class);

  // Used for serialization into the correct POJO as well as deserialization.
  // Currently depends on the string key being the same text value as the type
  // field.
  private final Map<String, Class<? extends Content>> jsonTypes;
  private final Map<Class<? extends Content>, Class<? extends ContentDTO>> mapOfDOsToDTOs;

  private static ObjectMapper basicObjectMapper;
  private static ObjectMapper preconfiguredObjectMapper;

  /**
   * Creates a new content mapper without type information.
   * <br>
   * Note: Type information must be provided by using the register type methods.
   */
  @Inject
  public ContentMapperUtils() {
    jsonTypes = Maps.newConcurrentMap();
    mapOfDOsToDTOs = Maps.newConcurrentMap();
  }

  /**
   * Alternative constructor that will attempt to search for valid types to pre-register.
   *
   * @param classes - series of classes contained within the parent package to search for content classes.
   */
  public ContentMapperUtils(final Collection<Class<?>> classes) {
    this();
    requireNonNull(classes);
    Validate.notEmpty(classes);

    // We need to pre-register different content objects here for the
    // auto-mapping to work
    Set<Class<?>> annotated = classes.stream().filter(c -> c.isAnnotationPresent(JsonContentType.class)).collect(
        Collectors.toSet());

    for (Class<?> classToAdd : annotated) {
      if (Content.class.isAssignableFrom(classToAdd)) {
        this.registerJsonTypeAndDTOMapping((Class<Content>) classToAdd);
      }
    }
  }

  /**
   * This method will accept a json string and will return a Content object (or one of its subtypes).
   *
   * @param docJson - to load
   * @return A Content object or one of its registered sub classes
   * @throws IOException - if there is a problem with IO
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
   * @param cls - the class to extract the jsontype value from.
   */
  public synchronized void registerJsonType(final Class<? extends Content> cls) {
    requireNonNull(cls, "Class cannot be null.");

    JsonContentType jt = cls.getAnnotation(JsonContentType.class);
    if (jt != null) {
      jsonTypes.put(jt.value(), cls);
    } else {
      log.error("The jsonType annotation type provided cannot be null. For the class {}", cls);
    }
  }

  /**
   * Registers DTOMapping using class annotation.
   *
   * @param cls - the class to extract the jsontype value from.
   */
  @SuppressWarnings("unchecked")
  public synchronized void registerDTOMapping(final Class<? extends Content> cls) {
    requireNonNull(cls, "Class cannot be null.");

    DTOMapping dtoMapping = cls.getAnnotation(DTOMapping.class);
    if (dtoMapping != null && ContentDTO.class.isAssignableFrom(dtoMapping.value())) {
      this.mapOfDOsToDTOs.put(cls, (Class<? extends ContentDTO>) dtoMapping.value());
    } else {
      log.error("The DTO mapping provided is null or the annotation is not present for the class {}."
          + " This class cannot be auto mapped from DO to DTO.", cls);
    }
  }

  /**
   * Registers JsonTypes and DTO mappings using class annotation.
   *
   * @param cls - the class to extract the jsontype value from.
   */
  public synchronized void registerJsonTypeAndDTOMapping(final Class<? extends Content> cls) {
    this.registerJsonType(cls);
    this.registerDTOMapping(cls);
  }

  /**
   * Find the class that implements the content DO based on a type string.
   *
   * @param type - string to lookup
   * @return the content DO class.
   */
  public Class<? extends Content> getClassByType(final String type) {
    return jsonTypes.get(type);
  }

  /**
   * Get a DTOClass based on a DOClass.
   *
   * @param cls - DO class.
   * @return DTO class.
   */
  public Class<? extends ContentDTO> getDTOClassByDOClass(final Class<? extends Content> cls) {
    return mapOfDOsToDTOs.get(cls);
  }

  /**
   * Populate relatedContent fields on the result and its children with IDs recursively.
   * Only recurses to children of type Content, but this is currently the only possibility.
   * When another subclass of ContentBase is introduced which also has relatedContent,
   * we can decide whether we want to move relatedContent up to the abstract base class etc.
   *
   * @param content - DO class.
   * @param result  - target DTO class.
   */
  private void populateRelatedContentWithIDs(final Content content, final ContentDTO result) {
    List<ContentBase> contentChildren = content.getChildren();
    if (contentChildren != null) {
      List<ContentBaseDTO> resultChildren = result.getChildren();
      for (int i = 0; i < contentChildren.size(); i++) {
        ContentBase contentChild = contentChildren.get(i);
        ContentBaseDTO resultChild = resultChildren.get(i);
        if (contentChild instanceof Content nestedContent && resultChild instanceof ContentDTO nestedContentDTO) {
          this.populateRelatedContentWithIDs(nestedContent, nestedContentDTO);
        }
      }
    }
    if (result.getRelatedContent() != null) {
      List<ContentSummaryDTO> relatedContent = Lists.newArrayList();
      for (String relatedId : content.getRelatedContent()) {
        ContentSummaryDTO contentSummary = new ContentSummaryDTO();
        contentSummary.setId(relatedId);
        relatedContent.add(contentSummary);
      }
      result.setRelatedContent(relatedContent);
    }
  }

  /**
   * Find the default DTO class from a given Domain object.
   *
   * @param content - Content DO to map to DTO.
   * @return DTO that can be used for mapping.
   */
  public ContentDTO getDTOByDO(final Content content) {
    if (null == content) {
      return null;
    }

    ContentDTO result = ContentMapper.INSTANCE.mapContent(content);
    this.populateRelatedContentWithIDs(content, result);
    return result;
  }

  /**
   * Converts the DO list to a list of DTOs.
   *
   * @param contentDOList - list of objects to convert.
   * @return the list of DTOs
   * @see #getDTOByDO(Content)
   */
  public List<ContentDTO> getDTOByDOList(final List<Content> contentDOList) {
    requireNonNull(contentDOList);

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
   * Get an ObjectMapper with the JavaTimeModule enabled, as 'java.time' types are not supported by default.
   */
  public static ObjectMapper getSharedBasicObjectMapper() {
    if (ContentMapperUtils.basicObjectMapper != null) {
      return basicObjectMapper;
    }
    basicObjectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    return basicObjectMapper;
  }

  /**
   * Provides a pre-configured module that can be added to an object mapper so that contentBase objects can be
   * deserialized using the custom deserializer.
   * <br>
   * This object Mapper is shared and should be treated as immutable.
   *
   * @return a jackson object mapper.
   */
  public ObjectMapper getSharedContentObjectMapper() {
    if (ContentMapperUtils.preconfiguredObjectMapper != null) {
      return preconfiguredObjectMapper;
    }

    log.info("Initialising preconfiguredObjectMapper and caching it.");

    preconfiguredObjectMapper = generateNewPreconfiguredContentMapper();

    return preconfiguredObjectMapper;
  }

  /**
   * Map a list of String to a List of Content.
   *
   * @param stringList - Converts a list of strings to a list of content
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
        = new QuestionValidationResponseDeserializer(
        contentDeserializer, choiceDeserializer);

    SimpleModule contentDeserializerModule = new SimpleModule("ContentDeserializerModule");
    contentDeserializerModule.addDeserializer(ContentBase.class, contentDeserializer);
    contentDeserializerModule.addDeserializer(Choice.class, choiceDeserializer);
    contentDeserializerModule.addDeserializer(Item.class, itemDeserializer);
    contentDeserializerModule.addDeserializer(QuestionValidationResponse.class, validationResponseDeserializer);

    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    objectMapper.registerModule(contentDeserializerModule);

    return objectMapper;
  }
}
