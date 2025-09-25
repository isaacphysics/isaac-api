package uk.ac.cam.cl.dtg.util.mappers;

import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.SubclassMapping;
import org.mapstruct.factory.Mappers;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacCard;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacCardDeck;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacEventPage;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacFeaturedProfile;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacPageFragment;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacPod;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacQuiz;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacQuizSection;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacWildcard;
import uk.ac.cam.cl.dtg.isaac.dos.content.*;
import uk.ac.cam.cl.dtg.isaac.dto.GameboardItem;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacCardDTO;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacCardDeckDTO;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacEventPageDTO;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacFeaturedProfileDTO;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacPageFragmentDTO;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacPodDTO;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacQuizDTO;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacQuizSectionDTO;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacWildcardDTO;
import uk.ac.cam.cl.dtg.isaac.dto.ResultsWrapper;
import uk.ac.cam.cl.dtg.isaac.dto.content.*;

import java.util.List;

@Mapper
public interface ContentMapperMS {
    ContentMapperMS INSTANCE = Mappers.getMapper(ContentMapperMS.class);

    @SubclassMapping(source = IsaacEventPageDTO.class, target = IsaacEventPageDTO.class)
    ContentDTO copy(ContentDTO source);

    default <T> T map(ContentDTO source, Class<T> targetClass) {
        if (targetClass.equals(ContentSummaryDTO.class)) {
            return (T) mapContentDTOtoContentSummaryDTO(source);
        } else if (targetClass.equals(GameboardItem.class)) {
            return (T) mapContentDTOtoGameboardItem(source);
        } else if (targetClass.equals(QuizSummaryDTO.class)) {
            return (T) mapContentDTOtoQuizSummaryDTO(source);
        } else if (targetClass.equals(DetailedQuizSummaryDTO.class)) {
            return (T) mapContentDTOtoDetailedQuizSummaryDTO(source);
        } else {
            throw new UnimplementedMappingException(ContentDTO.class, targetClass);
        }
    }

    @Mapping(target = "url", ignore = true)
    @Mapping(target = "supersededBy", ignore = true)
    @Mapping(target = "summary", ignore = true)
    @Mapping(target = "questionPartIds", ignore = true)
    @Mapping(target = "difficulty", ignore = true)
    ContentSummaryDTO mapContentDTOtoContentSummaryDTO(ContentDTO source);

    IsaacWildcard map(Content source);

    @Mapping(target = "supersededBy", ignore = true)
    @Mapping(target = "state", ignore = true)
    @Mapping(target = "questionPartsTotal", ignore = true)
    @Mapping(target = "questionPartsNotAttempted", ignore = true)
    @Mapping(target = "questionPartsIncorrect", ignore = true)
    @Mapping(target = "questionPartsCorrect", ignore = true)
    @Mapping(target = "questionPartStates", ignore = true)
    @Mapping(target = "passMark", ignore = true)
    @Mapping(target = "difficulty", ignore = true)
    @Mapping(target = "description", ignore = true)
    @Mapping(target = "creationContext", ignore = true)
    @Mapping(target = "contentType", ignore = true)
    @Mapping(target = "boardId", ignore = true)
    GameboardItem mapContentDTOtoGameboardItem(ContentDTO source);

    QuizSummaryDTO mapContentDTOtoQuizSummaryDTO(ContentDTO source);

    @SubclassMapping(source = IsaacQuizDTO.class, target = DetailedQuizSummaryDTO.class)
    DetailedQuizSummaryDTO mapContentDTOtoDetailedQuizSummaryDTO(ContentDTO source);

    DetailedQuizSummaryDTO map(IsaacQuizDTO source);

    SidebarDTO map(String source);

    @Mapping(target = "searchableContent", ignore = true)
    @Mapping(target = "correct", ignore = true)
    @Mapping(target = "explanation", ignore = true)
    @InheritInverseConfiguration(name = "mapChoice")
    Choice mapChoice(ChoiceDTO source);

    @SubclassMapping(source = Formula.class, target = FormulaDTO.class)
    @SubclassMapping(source = FreeTextRule.class, target = FreeTextRuleDTO.class)
    @SubclassMapping(source = ItemChoice.class, target = ItemChoiceDTO.class)
    @SubclassMapping(source = LogicFormula.class, target = LogicFormulaDTO.class)
    @SubclassMapping(source = Quantity.class, target = QuantityDTO.class)
    @SubclassMapping(source = RegexPattern.class, target = RegexPatternDTO.class)
    @SubclassMapping(source = StringChoice.class, target = StringChoiceDTO.class)
    ChoiceDTO mapChoice(Choice source);

    List<String> copyListOfString(List<String> source);
    default ResultsWrapper<String> copy(ResultsWrapper<String> source) {
        return new ResultsWrapper<>(copyListOfString(source.getResults()), source.getTotalResults());
    }

    @Mapping(target = "searchableContent", ignore = true)
    @InheritInverseConfiguration(name = "mapContent")
    Content mapContent(ContentDTO source);

    @SubclassMapping(source = AnvilApp.class, target = AnvilAppDTO.class)
    @SubclassMapping(source = Choice.class, target = ChoiceDTO.class)
    @SubclassMapping(source = CodeSnippet.class, target = CodeSnippetDTO.class)
    @SubclassMapping(source = CodeTabs.class, target = CodeTabsDTO.class)
    @SubclassMapping(source = EmailTemplate.class, target = EmailTemplateDTO.class)
    @SubclassMapping(source = GlossaryTerm.class, target = GlossaryTermDTO.class)
    @SubclassMapping(source = IsaacCard.class, target = IsaacCardDTO.class)
    @SubclassMapping(source = IsaacCardDeck.class, target = IsaacCardDeckDTO.class)
    @SubclassMapping(source = IsaacEventPage.class, target = IsaacEventPageDTO.class)
    @SubclassMapping(source = IsaacFeaturedProfile.class, target = IsaacFeaturedProfileDTO.class)
    @SubclassMapping(source = IsaacPageFragment.class, target = IsaacPageFragmentDTO.class)
    @SubclassMapping(source = IsaacPod.class, target = IsaacPodDTO.class)
    @SubclassMapping(source = IsaacQuizSection.class, target = IsaacQuizSectionDTO.class)
    @SubclassMapping(source = IsaacWildcard.class, target = IsaacWildcardDTO.class)
    @SubclassMapping(source = Item.class, target = ItemDTO.class)
    @SubclassMapping(source = Notification.class, target = NotificationDTO.class)
    @SubclassMapping(source = Question.class, target = QuestionDTO.class)
    @SubclassMapping(source = IsaacQuiz.class, target = IsaacQuizDTO.class)
    ContentDTO mapContent(Content source);

    List<String> mapListOfContentSummaryDtoToListOfString(List<ContentSummaryDTO> source);

    List<ContentSummaryDTO> mapListOfStringToListOfContentSummaryDTO(List<String> source);

    default ContentSummaryDTO mapStringToContentSummaryDTO(String source) {
        if (source == null) {
            return null;
        }
        ContentSummaryDTO contentSummaryDTO = new ContentSummaryDTO();
        contentSummaryDTO.setId(source);
        return contentSummaryDTO;
    }

    default String mapContentSummaryDTOtoString(ContentSummaryDTO source) {
        if (source == null) {
            return null;
        }
        return source.getId();
    }

    // Needed to avoid abstract interface errors
    default ContentBase map(ContentBaseDTO source) {
        if (source == null) {
            return null;
        } else if (source instanceof ContentDTO) {
            return mapContent((ContentDTO) source);
        } else {
            throw new UnimplementedMappingException(source.getClass(), ContentBase.class);
        }
    }

    default ContentBaseDTO map(ContentBase source) {
        if (source == null) {
            return null;
        } else if (source instanceof Content) {
            return mapContent((Content) source);
        } else {
            throw new UnimplementedMappingException(source.getClass(), ContentBaseDTO.class);
        }
    }
}