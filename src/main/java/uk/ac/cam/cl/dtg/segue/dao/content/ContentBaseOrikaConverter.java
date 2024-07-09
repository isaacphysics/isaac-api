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

import ma.glasnost.orika.MappingContext;
import ma.glasnost.orika.metadata.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacQuestionBase;
import uk.ac.cam.cl.dtg.isaac.dos.content.Content;
import uk.ac.cam.cl.dtg.isaac.dos.content.ContentBase;
import uk.ac.cam.cl.dtg.isaac.dos.content.InlineRegion;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacQuestionBaseDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentBaseDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.InlineRegionDTO;

import java.util.ArrayList;
import java.util.List;

/**
 * ContentBaseOrikaConverter A specialist converter class to work with the Orika automapper library.
 * 
 * Responsible for converting Content objects to their correct subtype.
 * 
 */
public class ContentBaseOrikaConverter extends AbstractPolymorphicConverter<ContentBase, ContentBaseDTO> {
    private static final Logger log = LoggerFactory.getLogger(ContentBaseOrikaConverter.class);

    private ContentMapper contentMapper;

    /**
     * Constructs an Orika Converter specialises in selecting the correct subclass for content objects.
     * 
     * @param contentMapper
     *            - An instance of a preconfigured content mapper that knows about the content inheritance hierarchy.
     */
    public ContentBaseOrikaConverter(final ContentMapper contentMapper) {
        this.contentMapper = contentMapper;
    }

    @Override
    public ContentBaseDTO convert(final ContentBase source, final Type<? extends ContentBaseDTO> destinationType,
                                  MappingContext _context) {

        if (null == source) {
            return null;
        }
             
        Class<? extends Content> contentClass = contentMapper.getClassByType(source.getType());

        if (contentClass == null) {
            // if we cannot figure out what content object default to content.
            contentClass = Content.class;
        }

        Class<? extends ContentDTO> destinationClass = contentMapper.getDTOClassByDOClass(contentClass);

        if (destinationClass == null) {
            log.error("Error - unable to locate DTO class from DO class ");
            return null;
        }

        ContentDTO result = super.mapperFacade.map(source, destinationClass);

        if (result instanceof InlineRegionDTO) {
            List<IsaacQuestionBase> questionBases = ((InlineRegion) source).getInlineQuestions();
            List<IsaacQuestionBaseDTO> newQuestionBases = new ArrayList<>();

            for (IsaacQuestionBase questionBase : questionBases) {
                Class<? extends Content> questionClass = contentMapper.getClassByType(questionBase.getType());
                Class<? extends ContentDTO> questionDestinationClass = contentMapper.getDTOClassByDOClass(questionClass);
                newQuestionBases.add((IsaacQuestionBaseDTO) super.mapperFacade.map(questionBase, questionDestinationClass));
            }

            ((InlineRegionDTO) result).setInlineQuestions(newQuestionBases);
        }

        return result;
    }

}
