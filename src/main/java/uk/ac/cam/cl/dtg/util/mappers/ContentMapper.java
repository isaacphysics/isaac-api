package uk.ac.cam.cl.dtg.util.mappers;

import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.SubclassExhaustiveStrategy;
import org.mapstruct.SubclassMapping;
import org.mapstruct.factory.Mappers;
import uk.ac.cam.cl.dtg.isaac.dos.*;
import uk.ac.cam.cl.dtg.isaac.dos.content.*;
import uk.ac.cam.cl.dtg.isaac.dto.*;
import uk.ac.cam.cl.dtg.isaac.dto.content.*;

import java.util.List;

/**
 * MapStruct mapper for Content objects.
 */
@Mapper(subclassExhaustiveStrategy = SubclassExhaustiveStrategy.RUNTIME_EXCEPTION)
public interface ContentMapper {
    ContentMapper INSTANCE = Mappers.getMapper(ContentMapper.class);

    @SubclassMapping(source = ContentDTO.class, target = Content.class)
    ContentBase map(ContentBaseDTO source);

    @SubclassMapping(source = Content.class, target = ContentDTO.class)
    ContentBaseDTO map(ContentBase source);

    @SuppressWarnings("unchecked")
    default <T> T map(ContentDTO source, Class<T> targetClass) {
        if (targetClass.equals(ContentSummaryDTO.class)) {
            return (T) mapContentDTOtoContentSummaryDTO(source);
        } else if (targetClass.equals(GameboardItem.class)) {
            return (T) mapContentDTOtoGameboardItem(source);
        } else if (targetClass.equals(QuizSummaryDTO.class)) {
            return (T) mapContentDTOtoQuizSummaryDTO(source);
        } else if (targetClass.equals(DetailedQuizSummaryDTO.class)) {
            return (T) mapContentDTOtoDetailedQuizSummaryDTO(source);
        } else if (targetClass.equals(IsaacWildcard.class)) {
            return (T) map(mapContent(source), IsaacWildcard.class);
        } else {
            throw new UnimplementedMappingException(ContentDTO.class, targetClass);
        }
    }

    @SuppressWarnings("unchecked")
    default <T> T map(Content source, Class<T> targetClass) {
        if (targetClass.equals(IsaacWildcard.class)) {
            return (T) mapContentToIsaacWildcard(source);
        } else if (targetClass.equals(ContentDTO.class)) {
            return (T) mapContent(source);
        } else {
            throw new UnimplementedMappingException(Content.class, targetClass);
        }
    }

    @Mapping(target = "url", ignore = true)
    @Mapping(target = "supersededBy", ignore = true)
    @Mapping(target = "summary", ignore = true)
    @Mapping(target = "state", ignore = true)
    @Mapping(target = "questionPartIds", ignore = true)
    @Mapping(target = "difficulty", ignore = true)
    @Mapping(target = "deprecated", ignore = true)
    ContentSummaryDTO mapContentDTOtoContentSummaryDTO(ContentDTO source);

    @Mapping(target = "url", ignore = true)
    @Mapping(target = "description", ignore = true)
    IsaacWildcard mapContentToIsaacWildcard(Content source);

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

    @Mapping(target = "url", ignore = true)
    @Mapping(target = "supersededBy", ignore = true)
    @Mapping(target = "summary", ignore = true)
    @Mapping(target = "state", ignore = true)
    @Mapping(target = "questionPartIds", ignore = true)
    @Mapping(target = "hiddenFromRoles", ignore = true)
    @Mapping(target = "difficulty", ignore = true)
    @Mapping(target = "deprecated", ignore = true)
    QuizSummaryDTO mapContentDTOtoQuizSummaryDTO(ContentDTO source);

    @Mapping(target = "url", ignore = true)
    @Mapping(target = "supersededBy", ignore = true)
    @Mapping(target = "summary", ignore = true)
    @Mapping(target = "state", ignore = true)
    @Mapping(target = "rubric", ignore = true)
    @Mapping(target = "questionPartIds", ignore = true)
    @Mapping(target = "hiddenFromRoles", ignore = true)
    @Mapping(target = "difficulty", ignore = true)
    @Mapping(target = "deprecated", ignore = true)
    @SubclassMapping(source = IsaacQuizDTO.class, target = DetailedQuizSummaryDTO.class)
    DetailedQuizSummaryDTO mapContentDTOtoDetailedQuizSummaryDTO(ContentDTO source);

    @Mapping(target = "url", ignore = true)
    @Mapping(target = "state", ignore = true)
    @Mapping(target = "questionPartIds", ignore = true)
    @Mapping(target = "difficulty", ignore = true)
    DetailedQuizSummaryDTO map(IsaacQuizDTO source);

    @Mapping(target = "version", ignore = true)
    @Mapping(target = "value", ignore = true)
    @Mapping(target = "type", ignore = true)
    @Mapping(target = "title", ignore = true)
    @Mapping(target = "tags", ignore = true)
    @Mapping(target = "subtitle", ignore = true)
    @Mapping(target = "sidebarEntries", ignore = true)
    @Mapping(target = "relatedContent", ignore = true)
    @Mapping(target = "published", ignore = true)
    @Mapping(target = "level", ignore = true)
    @Mapping(target = "layout", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "expandable", ignore = true)
    @Mapping(target = "encoding", ignore = true)
    @Mapping(target = "display", ignore = true)
    @Mapping(target = "children", ignore = true)
    @Mapping(target = "canonicalSourceFile", ignore = true)
    @Mapping(target = "author", ignore = true)
    @Mapping(target = "audience", ignore = true)
    @Mapping(target = "attribution", ignore = true)
    SidebarDTO map(String source);

    @Mapping(target = "searchableContent", ignore = true)
    @Mapping(target = "prioritisedSearchableContent", ignore = true)
    @Mapping(target = "explanation", ignore = true)
    @Mapping(target = "correct", ignore = true)
    Choice map(ChoiceDTO source);

    @SubclassMapping(source = ChemicalFormula.class, target = ChemicalFormulaDTO.class)
    @SubclassMapping(source = Formula.class, target = FormulaDTO.class)
    @SubclassMapping(source = FreeTextRule.class, target = FreeTextRuleDTO.class)
    @SubclassMapping(source = GraphChoice.class, target = GraphChoiceDTO.class)
    @SubclassMapping(source = ItemChoice.class, target = ItemChoiceDTO.class)
    @SubclassMapping(source = LLMFreeTextChoice.class, target = LLMFreeTextChoiceDTO.class)
    @SubclassMapping(source = LogicFormula.class, target = LogicFormulaDTO.class)
    @SubclassMapping(source = Quantity.class, target = QuantityDTO.class)
    @SubclassMapping(source = RegexPattern.class, target = RegexPatternDTO.class)
    @SubclassMapping(source = StringChoice.class, target = StringChoiceDTO.class)
    ChoiceDTO map(Choice source);

    List<String> copyStringList(List<String> source);

    default ResultsWrapper<String> copy(ResultsWrapper<String> source) {
        return new ResultsWrapper<>(copyStringList(source.getResults()), source.getTotalResults());
    }

    @SubclassMapping(source = AnvilApp.class, target = AnvilAppDTO.class)
    @SubclassMapping(source = Choice.class, target = ChoiceDTO.class)
    @SubclassMapping(source = CodeSnippet.class, target = CodeSnippetDTO.class)
    @SubclassMapping(source = CodeTabs.class, target = CodeTabsDTO.class)
    @SubclassMapping(source = EmailTemplate.class, target = EmailTemplateDTO.class)
    @SubclassMapping(source = GlossaryTerm.class, target = GlossaryTermDTO.class)
    @SubclassMapping(source = InlineRegion.class, target = InlineRegionDTO.class)
    @SubclassMapping(source = IsaacCard.class, target = IsaacCardDTO.class)
    @SubclassMapping(source = IsaacCardDeck.class, target = IsaacCardDeckDTO.class)
    @SubclassMapping(source = IsaacEventPage.class, target = IsaacEventPageDTO.class)
    @SubclassMapping(source = IsaacFeaturedProfile.class, target = IsaacFeaturedProfileDTO.class)
    @SubclassMapping(source = IsaacPageFragment.class, target = IsaacPageFragmentDTO.class)
    @SubclassMapping(source = IsaacPod.class, target = IsaacPodDTO.class)
    @SubclassMapping(source = IsaacQuiz.class, target = IsaacQuizDTO.class)
    @SubclassMapping(source = IsaacQuizSection.class, target = IsaacQuizSectionDTO.class)
    @SubclassMapping(source = IsaacWildcard.class, target = IsaacWildcardDTO.class)
    @SubclassMapping(source = Item.class, target = ItemDTO.class)
    @SubclassMapping(source = Notification.class, target = NotificationDTO.class)
    @SubclassMapping(source = Question.class, target = QuestionDTO.class)
    @SubclassMapping(source = Sidebar.class, target = SidebarDTO.class)
    @SubclassMapping(source = SidebarEntry.class, target = SidebarEntryDTO.class)
    ContentDTO mapContent(Content source);

    @InheritInverseConfiguration(name = "mapContent")
    Content mapContent(ContentDTO source);

    List<String> mapContentSummaryDTOListToStringList(List<ContentSummaryDTO> source);

    List<ContentSummaryDTO> mapStringListToContentSummaryDTOList(List<String> source);

    @SubclassMapping(source = IsaacEventPageDTO.class, target = IsaacEventPageDTO.class)
    ContentDTO copy(ContentDTO source);

    // Create a new ContentSummaryDTO and set the id to the source string
    @Mapping(source = "source", target = "id")
    @Mapping(target = "url", ignore = true)
    @Mapping(target = "type", ignore = true)
    @Mapping(target = "title", ignore = true)
    @Mapping(target = "tags", ignore = true)
    @Mapping(target = "supersededBy", ignore = true)
    @Mapping(target = "summary", ignore = true)
    @Mapping(target = "subtitle", ignore = true)
    @Mapping(target = "state", ignore = true)
    @Mapping(target = "questionPartIds", ignore = true)
    @Mapping(target = "level", ignore = true)
    @Mapping(target = "difficulty", ignore = true)
    @Mapping(target = "deprecated", ignore = true)
    @Mapping(target = "audience", ignore = true)
    ContentSummaryDTO mapStringToContentSummaryDTO(String source);

    default String mapContentSummaryDTOtoString(ContentSummaryDTO source) {
        if (source == null) {
            return null;
        }
        return source.getId();
    }
}