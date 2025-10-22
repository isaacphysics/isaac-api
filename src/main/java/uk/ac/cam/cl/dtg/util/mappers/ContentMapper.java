package uk.ac.cam.cl.dtg.util.mappers;

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

    /**
     * Mapping method to convert a ContentDTO to other types of DTO.
     *
     * @param <T>
     *            - the target type.
     * @param source
     *            - the source ContentDTO.
     * @param targetClass
     *            - the target class to convert to.
     * @return Returns an instance of the targetClass type mapped via the appropriate mapping method.
     */
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
        } else {
            throw new UnimplementedMappingException(ContentDTO.class, targetClass);
        }
    }

    /**
     * Mapping method to convert a Content object to a ContentDTO or a Wildcard.
     *
     * @param <T>
     *            - the target type.
     * @param source
     *            - the source Content object.
     * @param targetClass
     *            - the target class to convert to.
     * @return Returns an instance of the targetClass type mapped via the appropriate mapping method.
     */
    @SuppressWarnings("unchecked")
    default <T> T map(Content source, Class<T> targetClass) {
        // TODO fix the getWildcardById usage that makes this method necessary
        if (targetClass.equals(ContentDTO.class)) {
            return (T) map(source);
        } else {
            throw new UnimplementedMappingException(Content.class, targetClass);
        }
    }

    @SubclassMapping(source = ContentDTO.class, target = Content.class)
    ContentBase map(ContentBaseDTO source);

    @SubclassMapping(source = Content.class, target = ContentDTO.class)
    ContentBaseDTO map(ContentBase source);

    @SubclassMapping(source = ChemicalFormula.class, target = ChemicalFormulaDTO.class)
    @SubclassMapping(source = Formula.class, target = FormulaDTO.class)
    @SubclassMapping(source = FreeTextRule.class, target = FreeTextRuleDTO.class)
    @SubclassMapping(source = GraphChoice.class, target = GraphChoiceDTO.class)
    @SubclassMapping(source = LLMFreeTextChoice.class, target = LLMFreeTextChoiceDTO.class)
    @SubclassMapping(source = LogicFormula.class, target = LogicFormulaDTO.class)
    @SubclassMapping(source = Quantity.class, target = QuantityDTO.class)
    @SubclassMapping(source = RegexPattern.class, target = RegexPatternDTO.class)
    @SubclassMapping(source = StringChoice.class, target = StringChoiceDTO.class)
    // ItemChoice subclasses must come before ItemChoice
    @SubclassMapping(source = CoordinateChoice.class, target = CoordinateChoiceDTO.class)
    @SubclassMapping(source = ParsonsChoice.class, target = ParsonsChoiceDTO.class)
    @SubclassMapping(source = ItemChoice.class, target = ItemChoiceDTO.class)
    ChoiceDTO map(Choice source);

    @Mapping(target = "searchableContent", ignore = true)
    @Mapping(target = "prioritisedSearchableContent", ignore = true)
    @Mapping(target = "explanation", ignore = true)
    @Mapping(target = "correct", ignore = true)
    @SubclassMapping(source = ChemicalFormulaDTO.class, target = ChemicalFormula.class)
    @SubclassMapping(source = FormulaDTO.class, target = Formula.class)
    @SubclassMapping(source = FreeTextRuleDTO.class, target = FreeTextRule.class)
    @SubclassMapping(source = GraphChoiceDTO.class, target = GraphChoice.class)
    @SubclassMapping(source = LLMFreeTextChoiceDTO.class, target = LLMFreeTextChoice.class)
    @SubclassMapping(source = LogicFormulaDTO.class, target = LogicFormula.class)
    @SubclassMapping(source = QuantityDTO.class, target = Quantity.class)
    @SubclassMapping(source = RegexPatternDTO.class, target = RegexPattern.class)
    @SubclassMapping(source = StringChoiceDTO.class, target = StringChoice.class)
    // ItemChoiceDTO subclasses must come before ItemChoiceDTO
    @SubclassMapping(source = CoordinateChoiceDTO.class, target = CoordinateChoice.class)
    @SubclassMapping(source = ParsonsChoiceDTO.class, target = ParsonsChoice.class)
    @SubclassMapping(source = ItemChoiceDTO.class, target = ItemChoice.class)
    Choice map(ChoiceDTO source);

    @SubclassMapping(source = ParsonsItem.class, target = ParsonsItemDTO.class)
    @SubclassMapping(source = CoordinateItem.class, target = CoordinateItemDTO.class)
    ItemDTO map(Item source);

    @Mapping(target = "searchableContent", ignore = true)
    @Mapping(target = "prioritisedSearchableContent", ignore = true)
    @SubclassMapping(source = ParsonsItemDTO.class, target = ParsonsItem.class)
    @SubclassMapping(source = CoordinateItemDTO.class, target = CoordinateItem.class)
    Item map(ItemDTO source);

    @Mapping(target = "url", ignore = true)
    @Mapping(target = "state", ignore = true)
    @Mapping(target = "questionPartIds", ignore = true)
    @Mapping(target = "difficulty", ignore = true)
    DetailedQuizSummaryDTO map(IsaacQuizDTO source);

    @Mapping(target = "sidebar", ignore = true)
    @SubclassMapping(source = IsaacFastTrackQuestionPage.class, target = IsaacFastTrackQuestionPageDTO.class)
    @SubclassMapping(source = IsaacQuestionPage.class, target = IsaacQuestionPageDTO.class)
    @SubclassMapping(source = IsaacConceptPage.class, target = IsaacConceptPageDTO.class)
    @SubclassMapping(source = IsaacBookIndexPage.class, target = IsaacBookIndexPageDTO.class)
    SeguePageDTO map(SeguePage source);

    @Mapping(target = "userBookingStatus", ignore = true)
    @Mapping(target = "placesAvailable", ignore = true)
    @Mapping(target = "sidebar", ignore = true)
    @Mapping(target = "end_date", ignore = true) // end_date isn't actually ignored - see implementation
    IsaacEventPageDTO map(IsaacEventPage source);

    @Mapping(target = "total", ignore = true)
    @Mapping(target = "sectionTotals", ignore = true)
    @Mapping(target = "individualFeedback", ignore = true)
    @Mapping(target = "defaultFeedbackMode", ignore = true)
    @Mapping(target = "sidebar", ignore = true)
    IsaacQuizDTO map(IsaacQuiz source);

    @Mapping(target = "sidebar", ignore = true)
    @Mapping(target = "linkedGameboards", ignore = true)
    IsaacTopicSummaryPageDTO map(IsaacTopicSummaryPage source);

    @Mapping(target = "sidebar", ignore = true)
    @Mapping(target = "gameboards", ignore = true)
    @Mapping(target = "extensionGameboards", ignore = true)
    IsaacBookDetailPageDTO map(IsaacBookDetailPage source);

    @Mapping(target = "sidebar", ignore = true)
    @Mapping(target = "gameboards", ignore = true)
    IsaacRevisionDetailPageDTO map(IsaacRevisionDetailPage source);

    @SubclassMapping(source = SidebarGroup.class, target = SidebarGroupDTO.class)
    SidebarEntryDTO map(SidebarEntry source);

    @Mapping(target = "bestAttempt", ignore = true)
    @SubclassMapping(source = ChoiceQuestion.class, target = ChoiceQuestionDTO.class)
    QuestionDTO map(Question source);

    @Mapping(target = "bestAttempt", ignore = true)
    @SubclassMapping(source = IsaacQuestionBase.class, target = IsaacQuestionBaseDTO.class)
    ChoiceQuestionDTO map(ChoiceQuestion source);

    @SubclassMapping(source = AnvilApp.class, target = AnvilAppDTO.class)
    @SubclassMapping(source = Choice.class, target = ChoiceDTO.class)
    @SubclassMapping(source = CodeSnippet.class, target = CodeSnippetDTO.class)
    @SubclassMapping(source = CodeTabs.class, target = CodeTabsDTO.class)
    @SubclassMapping(source = EmailTemplate.class, target = EmailTemplateDTO.class)
    @SubclassMapping(source = GlossaryTerm.class, target = GlossaryTermDTO.class)
    @SubclassMapping(source = InlineRegion.class, target = InlineRegionDTO.class)
    @SubclassMapping(source = IsaacCard.class, target = IsaacCardDTO.class)
    @SubclassMapping(source = IsaacCardDeck.class, target = IsaacCardDeckDTO.class)
    @SubclassMapping(source = IsaacFeaturedProfile.class, target = IsaacFeaturedProfileDTO.class)
    @SubclassMapping(source = IsaacPageFragment.class, target = IsaacPageFragmentDTO.class)
    @SubclassMapping(source = IsaacPod.class, target = IsaacPodDTO.class)
    @SubclassMapping(source = IsaacQuizSection.class, target = IsaacQuizSectionDTO.class)
    @SubclassMapping(source = IsaacWildcard.class, target = IsaacWildcardDTO.class)
    @SubclassMapping(source = Item.class, target = ItemDTO.class)
    @SubclassMapping(source = Notification.class, target = NotificationDTO.class)
    @SubclassMapping(source = Question.class, target = QuestionDTO.class)
    @SubclassMapping(source = Sidebar.class, target = SidebarDTO.class)
    @SubclassMapping(source = SidebarEntry.class, target = SidebarEntryDTO.class)
    // Media subclasses. Figure must come before Image.
    @SubclassMapping(source = Figure.class, target = FigureDTO.class)
    @SubclassMapping(source = Image.class, target = ImageDTO.class)
    @SubclassMapping(source = Video.class, target = VideoDTO.class)
    // Segue pages. More specific subclasses must come first.
    @SubclassMapping(source = IsaacBookDetailPage.class, target = IsaacBookDetailPageDTO.class)
    @SubclassMapping(source = IsaacRevisionDetailPage.class, target = IsaacRevisionDetailPageDTO.class)
    @SubclassMapping(source = IsaacEventPage.class, target = IsaacEventPageDTO.class)
    @SubclassMapping(source = IsaacQuiz.class, target = IsaacQuizDTO.class)
    @SubclassMapping(source = SeguePage.class, target = SeguePageDTO.class)
    ContentDTO map(Content source);

    @Mapping(target = "url", ignore = true)
    @Mapping(target = "supersededBy", ignore = true)
    @Mapping(target = "summary", ignore = true)
    @Mapping(target = "state", ignore = true)
    @Mapping(target = "questionPartIds", ignore = true)
    @Mapping(target = "difficulty", ignore = true)
    @Mapping(target = "deprecated", ignore = true)
    ContentSummaryDTO mapContentDTOtoContentSummaryDTO(ContentDTO source);

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

    List<String> mapContentSummaryDTOListToStringList(List<ContentSummaryDTO> source);

    List<ContentSummaryDTO> mapStringListToContentSummaryDTOList(List<String> source);

    List<ContentBase> mapContentBaseDTOListToContentBaseList(List<ContentBaseDTO> source);

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

    /**
     * Mapping method to convert a ContentSummaryDTO to a String of its ID. Needed to map related content.
     *
     * @param source
     *            - the source Content object.
     * @return Returns the source Content object's ID.
     */
    default String mapContentSummaryDTOtoString(ContentSummaryDTO source) {
        if (source == null) {
            return null;
        }
        return source.getId();
    }

    @Mapping(target = "end_date", ignore = true)
    IsaacEventPageDTO copy(IsaacEventPageDTO source);

    @Mapping(target = "bestAttempt", ignore = true)
    @SubclassMapping(source = IsaacQuickQuestion.class, target = IsaacQuickQuestionDTO.class)
    @SubclassMapping(source = IsaacClozeQuestion.class, target = IsaacClozeQuestionDTO.class)
    @SubclassMapping(source = IsaacFreeTextQuestion.class, target = IsaacFreeTextQuestionDTO.class)
    @SubclassMapping(source = IsaacGraphSketcherQuestion.class, target = IsaacGraphSketcherQuestionDTO.class)
    @SubclassMapping(source = IsaacItemQuestion.class, target = IsaacItemQuestionDTO.class)
    @SubclassMapping(source = IsaacMultiChoiceQuestion.class, target = IsaacMultiChoiceQuestionDTO.class)
    @SubclassMapping(source = IsaacNumericQuestion.class, target = IsaacNumericQuestionDTO.class)
    @SubclassMapping(source = IsaacParsonsQuestion.class, target = IsaacParsonsQuestionDTO.class)
    @SubclassMapping(source = IsaacRegexMatchQuestion.class, target = IsaacRegexMatchQuestionDTO.class)
    @SubclassMapping(source = IsaacReorderQuestion.class, target = IsaacReorderQuestionDTO.class)
    @SubclassMapping(source = IsaacStringMatchQuestion.class, target = IsaacStringMatchQuestionDTO.class)
    @SubclassMapping(source = IsaacSymbolicChemistryQuestion.class, target = IsaacSymbolicChemistryQuestionDTO.class)
    @SubclassMapping(source = IsaacSymbolicLogicQuestion.class, target = IsaacSymbolicLogicQuestionDTO.class)
    @SubclassMapping(source = IsaacSymbolicQuestion.class, target = IsaacSymbolicQuestionDTO.class)
    @SubclassMapping(source = IsaacCoordinateQuestion.class, target = IsaacCoordinateQuestionDTO.class)
    @SubclassMapping(source = IsaacLLMFreeTextQuestion.class, target = IsaacLLMFreeTextQuestionDTO.class)
    @SubclassMapping(source = IsaacAnvilQuestion.class, target = IsaacAnvilQuestionDTO.class)
    IsaacQuestionBaseDTO mapIsaacQuestionBase(IsaacQuestionBase source);

    @Mapping(target = "bestAttempt", ignore = true)
    @SubclassMapping(source = IsaacClozeQuestion.class, target = IsaacClozeQuestionDTO.class)
    @SubclassMapping(source = IsaacParsonsQuestion.class, target = IsaacParsonsQuestionDTO.class)
    @SubclassMapping(source = IsaacReorderQuestion.class, target = IsaacReorderQuestionDTO.class)
    IsaacItemQuestionDTO mapIsaacItemQuestion(IsaacItemQuestion source);

    @Mapping(target = "bestAttempt", ignore = true)
    @SubclassMapping(source = IsaacSymbolicLogicQuestion.class, target = IsaacSymbolicLogicQuestionDTO.class)
    IsaacSymbolicQuestionDTO mapIsaacSymbolicQuestion(IsaacSymbolicQuestion source);
}