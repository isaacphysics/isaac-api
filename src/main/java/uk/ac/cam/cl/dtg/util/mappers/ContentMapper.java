package uk.ac.cam.cl.dtg.util.mappers;

import org.mapstruct.BeanMapping;
import org.mapstruct.InheritConfiguration;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.SubclassExhaustiveStrategy;
import org.mapstruct.SubclassMapping;
import org.mapstruct.factory.Mappers;
import uk.ac.cam.cl.dtg.isaac.dos.*;
import uk.ac.cam.cl.dtg.isaac.dos.content.*;
import uk.ac.cam.cl.dtg.isaac.dto.*;
import uk.ac.cam.cl.dtg.isaac.dto.content.*;
import uk.ac.cam.cl.dtg.segue.dos.content.InteractiveCodeSnippet;
import uk.ac.cam.cl.dtg.segue.dto.content.InteractiveCodeSnippetDTO;
import uk.ac.cam.cl.dtg.util.locations.Address;
import uk.ac.cam.cl.dtg.util.locations.Location;

import java.util.List;

@Mapper(subclassExhaustiveStrategy = SubclassExhaustiveStrategy.RUNTIME_EXCEPTION, uses = {ContentMapperSubclasses.class})
public interface ContentMapper {
    ContentMapper INSTANCE = Mappers.getMapper(ContentMapper.class);

    @SubclassMapping(source = ContentDTO.class, target = Content.class)
    ContentBase map(ContentBaseDTO source);

    @SubclassMapping(source = Content.class, target = ContentDTO.class)
    ContentBaseDTO map(ContentBase source);

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
        } else if (targetClass.equals(Content.class)) {
            return (T) mapContent(source);
        } else {
            throw new UnimplementedMappingException(ContentDTO.class, targetClass);
        }
    }

    default <T> T map(Content source, Class<T> targetClass) {
        if (targetClass.equals(IsaacWildcard.class)) {
            return (T) mapContentToIsaacWildcard(source);
        } else if (targetClass.equals(ContentDTO.class)) {
            return (T) mapContent(source);
        } else {
            throw new UnimplementedMappingException(Content.class, targetClass);
        }
    }

    @Mapping(target = "searchableContent", ignore = true)
    AnvilApp mapAnvilApp(AnvilAppDTO source);

    @Mapping(target = "searchableContent", ignore = true)
    IsaacCard mapIsaacCard(IsaacCardDTO source);

    @Mapping(target = "url", ignore = true)
    @Mapping(target = "supersededBy", ignore = true)
    @Mapping(target = "summary", ignore = true)
    @Mapping(target = "questionPartIds", ignore = true)
    @Mapping(target = "difficulty", ignore = true)
    @BeanMapping(resultType = ContentSummaryDTO.class)
    @SubclassMapping(source = InteractiveCodeSnippetDTO.class, target = ContentSummaryDTO.class)
    @SubclassMapping(source = ParsonsItemDTO.class, target = ContentSummaryDTO.class)
    @SubclassMapping(source = AnvilAppDTO.class, target = ContentSummaryDTO.class)
    @SubclassMapping(source = ChoiceDTO.class, target = ContentSummaryDTO.class)
    @SubclassMapping(source = CodeSnippetDTO.class, target = ContentSummaryDTO.class)
    @SubclassMapping(source = CodeTabsDTO.class, target = ContentSummaryDTO.class)
    @SubclassMapping(source = EmailTemplateDTO.class, target = ContentSummaryDTO.class)
    @SubclassMapping(source = GlossaryTermDTO.class, target = ContentSummaryDTO.class)
    @SubclassMapping(source = IsaacCardDTO.class, target = ContentSummaryDTO.class)
    @SubclassMapping(source = IsaacCardDeckDTO.class, target = ContentSummaryDTO.class)
    @SubclassMapping(source = IsaacEventPageDTO.class, target = ContentSummaryDTO.class)
    @SubclassMapping(source = IsaacFeaturedProfileDTO.class, target = ContentSummaryDTO.class)
    @SubclassMapping(source = IsaacPageFragmentDTO.class, target = ContentSummaryDTO.class)
    @SubclassMapping(source = IsaacPodDTO.class, target = ContentSummaryDTO.class)
    @SubclassMapping(source = IsaacWildcardDTO.class, target = ContentSummaryDTO.class)
    @SubclassMapping(source = ItemDTO.class, target = ContentSummaryDTO.class)
    @SubclassMapping(source = MediaDTO.class, target = ContentSummaryDTO.class)
    @SubclassMapping(source = NotificationDTO.class, target = ContentSummaryDTO.class)
    @SubclassMapping(source = QuestionDTO.class, target = ContentSummaryDTO.class)
    @SubclassMapping(source = SeguePageDTO.class, target = ContentSummaryDTO.class)
    ContentSummaryDTO mapContentDTOtoContentSummaryDTO(ContentDTO source);

    @Named("toDetailedQuizSummaryDTO")
    @SubclassMapping(source = IsaacQuizDTO.class, target = DetailedQuizSummaryDTO.class)
    DetailedQuizSummaryDTO mapContentDTOtoDetailedQuizSummaryDTO(ContentDTO source);

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
    @SubclassMapping(source = Media.class, target = MediaDTO.class)
    @SubclassMapping(source = Notification.class, target = NotificationDTO.class)
    @SubclassMapping(source = Question.class, target = QuestionDTO.class)
    @SubclassMapping(source = SeguePage.class, target = SeguePageDTO.class)
    ContentDTO mapContent(Content source);

    List<IsaacCard> mapListOfIsaacCardDTOToListOfIsaacCard(List<IsaacCardDTO> source);
    List<IsaacCardDTO> mapListOfIsaacCardToListOfIsaacCardDTO(List<IsaacCard> source);

    IsaacQuizSectionDTO mapIsaacQuizSection(IsaacQuizSection source);

    @Mapping(target = "requiresExactMatch", expression = "java(source.requiresExactMatch())")
    @InheritConfiguration(name = "mapChoice")
    Formula mapFormula(FormulaDTO source);

    @Mapping(target = "requiresExactMatch", expression = "java(source.requiresExactMatch())")
    @InheritConfiguration(name = "mapChoice")
    LogicFormula mapLogicFormula(LogicFormulaDTO source);

    @Mapping(target = "searchableContent", ignore = true)
    @Mapping(target = "explanation", ignore = true)
    @Mapping(target = "correct", ignore = true)
    @SubclassMapping(source = ParsonsChoiceDTO.class, target = ParsonsChoice.class)
    ItemChoice map(ItemChoiceDTO source);

    @SubclassMapping(source = ParsonsChoice.class, target = ParsonsChoiceDTO.class)
    ItemChoiceDTO map(ItemChoice source);

    @Mapping(target = "searchableContent", ignore = true)
    @SubclassMapping(source = InteractiveCodeSnippetDTO.class, target = InteractiveCodeSnippet.class)
    CodeSnippet map(CodeSnippetDTO source);

    @SubclassMapping(source = InteractiveCodeSnippet.class, target = InteractiveCodeSnippetDTO.class)
    CodeSnippetDTO map(CodeSnippet source);

    @Mapping(target = "searchableContent", ignore = true)
    @SubclassMapping(source = ImageDTO.class, target = Image.class)
    @SubclassMapping(source = VideoDTO.class, target = Video.class)
    Media map(MediaDTO source);

    @SubclassMapping(source = Image.class, target = ImageDTO.class)
    @SubclassMapping(source = Video.class, target = VideoDTO.class)
    MediaDTO map(Media source);

    @Mapping(target = "searchableContent", ignore = true)
    @SubclassMapping(source = FigureDTO.class, target = Figure.class)
    Image map(ImageDTO source);

    @SubclassMapping(source = Figure.class, target = FigureDTO.class)
    ImageDTO map(Image source);

    @Mapping(target = "searchableContent", ignore = true)
    @SubclassMapping(source = ParsonsItemDTO.class, target = ParsonsItem.class)
    Item map(ItemDTO source);

    @SubclassMapping(source = ParsonsItem.class, target = ParsonsItemDTO.class)
    ItemDTO map(Item source);

    @Mapping(target = "searchableContent", ignore = true)
    @InheritInverseConfiguration(name = "mapSeguePage")
    SeguePage mapSeguePage(SeguePageDTO source);

    @Mapping(target = "userBookingStatus", ignore = true)
    @Mapping(target = "placesAvailable", ignore = true)
    @InheritConfiguration(name = "mapContent")
    IsaacEventPageDTO map(IsaacEventPage source);

    @SubclassMapping(source = IsaacConceptPage.class, target = IsaacConceptPageDTO.class)
    @SubclassMapping(source = IsaacQuestionPage.class, target = IsaacQuestionPageDTO.class)
    @SubclassMapping(source = IsaacQuiz.class, target = IsaacQuizDTO.class)
    @SubclassMapping(source = IsaacTopicSummaryPage.class, target = IsaacTopicSummaryPageDTO.class)
    SeguePageDTO mapSeguePage(SeguePage source);

    @Mapping(target = "total", ignore = true)
    @Mapping(target = "sectionTotals", ignore = true)
    @Mapping(target = "individualFeedback", ignore = true)
    @Mapping(target = "defaultFeedbackMode", ignore = true)
    IsaacQuizDTO mapIsaacQuiz(IsaacQuiz source);

    @Mapping(target = "searchableContent", ignore = true)
    IsaacQuestionPage map(IsaacQuestionPageDTO source);

    IsaacQuestionPageDTO map(IsaacQuestionPage source);

    @Mapping(target = "searchableContent", ignore = true)
    @Mapping(target = "defaultFeedback", ignore = true)
    @SubclassMapping(source = ChoiceQuestionDTO.class, target = ChoiceQuestion.class)
    Question map(QuestionDTO source);

    @Mapping(target = "bestAttempt", ignore = true)
    @SubclassMapping(source = ChoiceQuestion.class, target = ChoiceQuestionDTO.class)
    QuestionDTO map(Question source);

    @Mapping(target = "searchableContent", ignore = true)
    @Mapping(target = "defaultFeedback", ignore = true)
    @SubclassMapping(source = IsaacQuestionBaseDTO.class, target = IsaacQuestionBase.class)
    ChoiceQuestion map(ChoiceQuestionDTO source);

    @Mapping(target = "bestAttempt", ignore = true)
    @SubclassMapping(source = IsaacQuestionBase.class, target = IsaacQuestionBaseDTO.class)
    ChoiceQuestionDTO map(ChoiceQuestion source);

    @Mapping(target = "searchableContent", ignore = true)
    @Mapping(target = "defaultFeedback", ignore = true)
    @InheritInverseConfiguration(name = "mapIsaacQuestionBase")
    IsaacQuestionBase mapIsaacQuestionBase(IsaacQuestionBaseDTO source);

    @Mapping(target = "significantFiguresMin", ignore = true)
    @Mapping(target = "significantFiguresMax", ignore = true)
    @Mapping(target = "searchableContent", ignore = true)
    @Mapping(target = "defaultFeedback", ignore = true)
    IsaacNumericQuestion mapIsaacNumericQuestion(IsaacNumericQuestionDTO source);

    @Mapping(target = "bestAttempt", ignore = true)
    @SubclassMapping(source = IsaacAnvilQuestion.class, target = IsaacAnvilQuestionDTO.class)
    @SubclassMapping(source = IsaacFreeTextQuestion.class, target = IsaacFreeTextQuestionDTO.class)
    @SubclassMapping(source = IsaacItemQuestion.class, target = IsaacItemQuestionDTO.class)
    @SubclassMapping(source = IsaacMultiChoiceQuestion.class, target = IsaacMultiChoiceQuestionDTO.class)
    @SubclassMapping(source = IsaacNumericQuestion.class, target = IsaacNumericQuestionDTO.class)
    @SubclassMapping(source = IsaacQuickQuestion.class, target = IsaacQuickQuestionDTO.class)
    @SubclassMapping(source = IsaacRegexMatchQuestion.class, target = IsaacRegexMatchQuestionDTO.class)
    @SubclassMapping(source = IsaacStringMatchQuestion.class, target = IsaacStringMatchQuestionDTO.class)
    @SubclassMapping(source = IsaacSymbolicQuestion.class, target = IsaacSymbolicQuestionDTO.class)
    IsaacQuestionBaseDTO mapIsaacQuestionBase(IsaacQuestionBase source);

    @Mapping(target = "searchableContent", ignore = true)
    @Mapping(target = "defaultFeedback", ignore = true)
    @InheritInverseConfiguration(name = "mapIsaacItemQuestion")
    IsaacItemQuestion mapIsaacItemQuestion(IsaacItemQuestionDTO source);

    @Mapping(target = "searchableContent", ignore = true)
    @Mapping(target = "detailedItemFeedback", ignore = true)
    @Mapping(target = "defaultFeedback", ignore = true)
    IsaacClozeQuestion mapIsaacClozeQuestion(IsaacClozeQuestionDTO source);

    @Mapping(target = "knownUnits", ignore = true)
    @Mapping(target = "bestAttempt", ignore = true)
    IsaacNumericQuestionDTO mapIsaacNumericQuestion(IsaacNumericQuestion source);

    @Mapping(target = "bestAttempt", ignore = true)
    @SubclassMapping(source = IsaacClozeQuestion.class, target = IsaacClozeQuestionDTO.class)
    @SubclassMapping(source = IsaacParsonsQuestion.class, target = IsaacParsonsQuestionDTO.class)
    @SubclassMapping(source = IsaacReorderQuestion.class, target = IsaacReorderQuestionDTO.class)
    IsaacItemQuestionDTO mapIsaacItemQuestion(IsaacItemQuestion source);

    @Mapping(target = "searchableContent", ignore = true)
    @Mapping(target = "defaultFeedback", ignore = true)
    @SubclassMapping(source = IsaacSymbolicLogicQuestionDTO.class, target = IsaacSymbolicLogicQuestion.class)
    IsaacSymbolicQuestion mapIsaacSymbolicQuestion(IsaacSymbolicQuestionDTO source);

    @Mapping(target = "bestAttempt", ignore = true)
    @SubclassMapping(source = IsaacSymbolicLogicQuestion.class, target = IsaacSymbolicLogicQuestionDTO.class)
    IsaacSymbolicQuestionDTO mapIsaacSymbolicQuestion(IsaacSymbolicQuestion source);

    List<String> mapListOfContentSummaryDtoToListOfString(List<ContentSummaryDTO> source);
    List<ContentSummaryDTO> mapListOfStringToListOfContentSummaryDTO(List<String> source);
    List<String> mapListOfGameboardDTOtoListOfString(List<GameboardDTO> source);
    List<GameboardDTO> mapListOfStringToListOfGameboardDTO(List<String> source);

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

    default GameboardDTO mapStringToGameboardDTO(String source) {
        if (source == null) {
            return null;
        }
        GameboardDTO gameboardDTO = new GameboardDTO();
        gameboardDTO.setId(source);
        return gameboardDTO;
    }

    default String mapGameboardDTOtoString(GameboardDTO source) {
        if (source == null) {
            return null;
        }
        return source.getId();
    }

    @SubclassMapping(source = ParsonsChoiceDTO.class, target = QuizSummaryDTO.class)
    @SubclassMapping(source = FormulaDTO.class, target = QuizSummaryDTO.class)
    @SubclassMapping(source = FreeTextRuleDTO.class, target = QuizSummaryDTO.class)
    @SubclassMapping(source = ItemChoiceDTO.class, target = QuizSummaryDTO.class)
    @SubclassMapping(source = LogicFormulaDTO.class, target = QuizSummaryDTO.class)
    @SubclassMapping(source = QuantityDTO.class, target = QuizSummaryDTO.class)
    @SubclassMapping(source = RegexPatternDTO.class, target = QuizSummaryDTO.class)
    @SubclassMapping(source = StringChoiceDTO.class, target = QuizSummaryDTO.class)
    @SubclassMapping(source = ChoiceDTO.class, target = QuizSummaryDTO.class)
    @SubclassMapping(source = IsaacConceptPageDTO.class, target = QuizSummaryDTO.class)
    @SubclassMapping(source = IsaacQuestionPageDTO.class, target = QuizSummaryDTO.class)
    @SubclassMapping(source = IsaacQuizSectionDTO.class, target = QuizSummaryDTO.class)
    @SubclassMapping(source = IsaacTopicSummaryPageDTO.class, target = QuizSummaryDTO.class)
    @SubclassMapping(source = IsaacSymbolicLogicQuestionDTO.class, target = QuizSummaryDTO.class)
    @SubclassMapping(source = IsaacClozeQuestionDTO.class, target = QuizSummaryDTO.class)
    @SubclassMapping(source = IsaacParsonsQuestionDTO.class, target = QuizSummaryDTO.class)
    @SubclassMapping(source = IsaacReorderQuestionDTO.class, target = QuizSummaryDTO.class)
    @SubclassMapping(source = IsaacAnvilQuestionDTO.class, target = QuizSummaryDTO.class)
    @SubclassMapping(source = IsaacFreeTextQuestionDTO.class, target = QuizSummaryDTO.class)
    @SubclassMapping(source = IsaacItemQuestionDTO.class, target = QuizSummaryDTO.class)
    @SubclassMapping(source = IsaacMultiChoiceQuestionDTO.class, target = QuizSummaryDTO.class)
    @SubclassMapping(source = IsaacNumericQuestionDTO.class, target = QuizSummaryDTO.class)
    @SubclassMapping(source = IsaacQuickQuestionDTO.class, target = QuizSummaryDTO.class)
    @SubclassMapping(source = IsaacRegexMatchQuestionDTO.class, target = QuizSummaryDTO.class)
    @SubclassMapping(source = IsaacStringMatchQuestionDTO.class, target = QuizSummaryDTO.class)
    @SubclassMapping(source = IsaacSymbolicQuestionDTO.class, target = QuizSummaryDTO.class)
    @SubclassMapping(source = IsaacQuestionBaseDTO.class, target = QuizSummaryDTO.class)
    @SubclassMapping(source = ChoiceQuestionDTO.class, target = QuizSummaryDTO.class)
    @SubclassMapping(source = QuestionDTO.class, target = QuizSummaryDTO.class)
    @SubclassMapping(source = InteractiveCodeSnippetDTO.class, target = QuizSummaryDTO.class)
    @SubclassMapping(source = FigureDTO.class, target = QuizSummaryDTO.class)
    @SubclassMapping(source = ImageDTO.class, target = QuizSummaryDTO.class)
    @SubclassMapping(source = VideoDTO.class, target = QuizSummaryDTO.class)
    @SubclassMapping(source = ParsonsItemDTO.class, target = QuizSummaryDTO.class)
    @SubclassMapping(source = AnvilAppDTO.class, target = QuizSummaryDTO.class)
    @SubclassMapping(source = CodeSnippetDTO.class, target = QuizSummaryDTO.class)
    @SubclassMapping(source = CodeTabsDTO.class, target = QuizSummaryDTO.class)
    @SubclassMapping(source = EmailTemplateDTO.class, target = QuizSummaryDTO.class)
    @SubclassMapping(source = GlossaryTermDTO.class, target = QuizSummaryDTO.class)
    @SubclassMapping(source = IsaacCardDTO.class, target = QuizSummaryDTO.class)
    @SubclassMapping(source = IsaacCardDeckDTO.class, target = QuizSummaryDTO.class)
    @SubclassMapping(source = IsaacEventPageDTO.class, target = QuizSummaryDTO.class)
    @SubclassMapping(source = IsaacFeaturedProfileDTO.class, target = QuizSummaryDTO.class)
    @SubclassMapping(source = IsaacPageFragmentDTO.class, target = QuizSummaryDTO.class)
    @SubclassMapping(source = IsaacPodDTO.class, target = QuizSummaryDTO.class)
    @SubclassMapping(source = IsaacWildcardDTO.class, target = QuizSummaryDTO.class)
    @SubclassMapping(source = ItemDTO.class, target = QuizSummaryDTO.class)
    @SubclassMapping(source = MediaDTO.class, target = QuizSummaryDTO.class)
    @SubclassMapping(source = NotificationDTO.class, target = QuizSummaryDTO.class)
    QuizSummaryDTO mapContentDTOtoQuizSummaryDTO(ContentDTO source);

    @Mapping(target = "url", ignore = true)
    @Mapping(target = "description", ignore = true)
    @SubclassMapping(source = ParsonsChoice.class, target = IsaacWildcard.class)
    @SubclassMapping(source = Formula.class, target = IsaacWildcard.class)
    @SubclassMapping(source = FreeTextRule.class, target = IsaacWildcard.class)
    @SubclassMapping(source = ItemChoice.class, target = IsaacWildcard.class)
    @SubclassMapping(source = LogicFormula.class, target = IsaacWildcard.class)
    @SubclassMapping(source = Quantity.class, target = IsaacWildcard.class)
    @SubclassMapping(source = RegexPattern.class, target = IsaacWildcard.class)
    @SubclassMapping(source = StringChoice.class, target = IsaacWildcard.class)
    @SubclassMapping(source = Choice.class, target = IsaacWildcard.class)
    @SubclassMapping(source = IsaacConceptPage.class, target = IsaacWildcard.class)
    @SubclassMapping(source = IsaacQuestionPage.class, target = IsaacWildcard.class)
    @SubclassMapping(source = IsaacQuiz.class, target = IsaacWildcard.class)
    @SubclassMapping(source = IsaacQuizSection.class, target = IsaacWildcard.class)
    @SubclassMapping(source = IsaacTopicSummaryPage.class, target = IsaacWildcard.class)
    @SubclassMapping(source = SeguePage.class, target = IsaacWildcard.class)
    @SubclassMapping(source = IsaacSymbolicLogicQuestion.class, target = IsaacWildcard.class)
    @SubclassMapping(source = IsaacClozeQuestion.class, target = IsaacWildcard.class)
    @SubclassMapping(source = IsaacParsonsQuestion.class, target = IsaacWildcard.class)
    @SubclassMapping(source = IsaacReorderQuestion.class, target = IsaacWildcard.class)
    @SubclassMapping(source = IsaacAnvilQuestion.class, target = IsaacWildcard.class)
    @SubclassMapping(source = IsaacFreeTextQuestion.class, target = IsaacWildcard.class)
    @SubclassMapping(source = IsaacItemQuestion.class, target = IsaacWildcard.class)
    @SubclassMapping(source = IsaacMultiChoiceQuestion.class, target = IsaacWildcard.class)
    @SubclassMapping(source = IsaacNumericQuestion.class, target = IsaacWildcard.class)
    @SubclassMapping(source = IsaacQuickQuestion.class, target = IsaacWildcard.class)
    @SubclassMapping(source = IsaacRegexMatchQuestion.class, target = IsaacWildcard.class)
    @SubclassMapping(source = IsaacStringMatchQuestion.class, target = IsaacWildcard.class)
    @SubclassMapping(source = IsaacSymbolicQuestion.class, target = IsaacWildcard.class)
    @SubclassMapping(source = IsaacQuestionBase.class, target = IsaacWildcard.class)
    @SubclassMapping(source = ChoiceQuestion.class, target = IsaacWildcard.class)
    @SubclassMapping(source = Question.class, target = IsaacWildcard.class)
    @SubclassMapping(source = InteractiveCodeSnippet.class, target = IsaacWildcard.class)
    @SubclassMapping(source = Figure.class, target = IsaacWildcard.class)
    @SubclassMapping(source = Image.class, target = IsaacWildcard.class)
    @SubclassMapping(source = Video.class, target = IsaacWildcard.class)
    @SubclassMapping(source = ParsonsItem.class, target = IsaacWildcard.class)
    @SubclassMapping(source = AnvilApp.class, target = IsaacWildcard.class)
    @SubclassMapping(source = CodeSnippet.class, target = IsaacWildcard.class)
    @SubclassMapping(source = CodeTabs.class, target = IsaacWildcard.class)
    @SubclassMapping(source = EmailTemplate.class, target = IsaacWildcard.class)
    @SubclassMapping(source = GlossaryTerm.class, target = IsaacWildcard.class)
    @SubclassMapping(source = IsaacCard.class, target = IsaacWildcard.class)
    @SubclassMapping(source = IsaacCardDeck.class, target = IsaacWildcard.class)
    @SubclassMapping(source = IsaacEventPage.class, target = IsaacWildcard.class)
    @SubclassMapping(source = IsaacFeaturedProfile.class, target = IsaacWildcard.class)
    @SubclassMapping(source = IsaacPageFragment.class, target = IsaacWildcard.class)
    @SubclassMapping(source = IsaacPod.class, target = IsaacWildcard.class)
    @SubclassMapping(source = IsaacWildcard.class, target = IsaacWildcard.class)
    @SubclassMapping(source = Item.class, target = IsaacWildcard.class)
    @SubclassMapping(source = Media.class, target = IsaacWildcard.class)
    @SubclassMapping(source = Notification.class, target = IsaacWildcard.class)
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
    @SubclassMapping(source = ParsonsChoiceDTO.class, target = GameboardItem.class)
    @SubclassMapping(source = FormulaDTO.class, target = GameboardItem.class)
    @SubclassMapping(source = FreeTextRuleDTO.class, target = GameboardItem.class)
    @SubclassMapping(source = ItemChoiceDTO.class, target = GameboardItem.class)
    @SubclassMapping(source = LogicFormulaDTO.class, target = GameboardItem.class)
    @SubclassMapping(source = QuantityDTO.class, target = GameboardItem.class)
    @SubclassMapping(source = RegexPatternDTO.class, target = GameboardItem.class)
    @SubclassMapping(source = StringChoiceDTO.class, target = GameboardItem.class)
    @SubclassMapping(source = ChoiceDTO.class, target = GameboardItem.class)
    @SubclassMapping(source = IsaacConceptPageDTO.class, target = GameboardItem.class)
    @SubclassMapping(source = IsaacQuestionPageDTO.class, target = GameboardItem.class)
    @SubclassMapping(source = IsaacQuizDTO.class, target = GameboardItem.class)
    @SubclassMapping(source = IsaacQuizSectionDTO.class, target = GameboardItem.class)
    @SubclassMapping(source = IsaacTopicSummaryPageDTO.class, target = GameboardItem.class)
    @SubclassMapping(source = SeguePageDTO.class, target = GameboardItem.class)
    @SubclassMapping(source = IsaacSymbolicLogicQuestionDTO.class, target = GameboardItem.class)
    @SubclassMapping(source = IsaacClozeQuestionDTO.class, target = GameboardItem.class)
    @SubclassMapping(source = IsaacParsonsQuestionDTO.class, target = GameboardItem.class)
    @SubclassMapping(source = IsaacReorderQuestionDTO.class, target = GameboardItem.class)
    @SubclassMapping(source = IsaacAnvilQuestionDTO.class, target = GameboardItem.class)
    @SubclassMapping(source = IsaacFreeTextQuestionDTO.class, target = GameboardItem.class)
    @SubclassMapping(source = IsaacItemQuestionDTO.class, target = GameboardItem.class)
    @SubclassMapping(source = IsaacMultiChoiceQuestionDTO.class, target = GameboardItem.class)
    @SubclassMapping(source = IsaacNumericQuestionDTO.class, target = GameboardItem.class)
    @SubclassMapping(source = IsaacQuickQuestionDTO.class, target = GameboardItem.class)
    @SubclassMapping(source = IsaacRegexMatchQuestionDTO.class, target = GameboardItem.class)
    @SubclassMapping(source = IsaacStringMatchQuestionDTO.class, target = GameboardItem.class)
    @SubclassMapping(source = IsaacSymbolicQuestionDTO.class, target = GameboardItem.class)
    @SubclassMapping(source = IsaacQuestionBaseDTO.class, target = GameboardItem.class)
    @SubclassMapping(source = ChoiceQuestionDTO.class, target = GameboardItem.class)
    @SubclassMapping(source = QuestionDTO.class, target = GameboardItem.class)
    @SubclassMapping(source = InteractiveCodeSnippetDTO.class, target = GameboardItem.class)
    @SubclassMapping(source = FigureDTO.class, target = GameboardItem.class)
    @SubclassMapping(source = ImageDTO.class, target = GameboardItem.class)
    @SubclassMapping(source = VideoDTO.class, target = GameboardItem.class)
    @SubclassMapping(source = ParsonsItemDTO.class, target = GameboardItem.class)
    @SubclassMapping(source = AnvilAppDTO.class, target = GameboardItem.class)
    @SubclassMapping(source = CodeSnippetDTO.class, target = GameboardItem.class)
    @SubclassMapping(source = CodeTabsDTO.class, target = GameboardItem.class)
    @SubclassMapping(source = EmailTemplateDTO.class, target = GameboardItem.class)
    @SubclassMapping(source = GlossaryTermDTO.class, target = GameboardItem.class)
    @SubclassMapping(source = IsaacCardDTO.class, target = GameboardItem.class)
    @SubclassMapping(source = IsaacCardDeckDTO.class, target = GameboardItem.class)
    @SubclassMapping(source = IsaacEventPageDTO.class, target = GameboardItem.class)
    @SubclassMapping(source = IsaacFeaturedProfileDTO.class, target = GameboardItem.class)
    @SubclassMapping(source = IsaacPageFragmentDTO.class, target = GameboardItem.class)
    @SubclassMapping(source = IsaacPodDTO.class, target = GameboardItem.class)
    @SubclassMapping(source = IsaacWildcardDTO.class, target = GameboardItem.class)
    @SubclassMapping(source = ItemDTO.class, target = GameboardItem.class)
    @SubclassMapping(source = MediaDTO.class, target = GameboardItem.class)
    @SubclassMapping(source = NotificationDTO.class, target = GameboardItem.class)
    GameboardItem mapContentDTOtoGameboardItem(ContentDTO source);

    @SubclassMapping(source = ContentDTO.class, target = ContentDTO.class)
    ContentBaseDTO copy(ContentBaseDTO source);

    @SubclassMapping(source = ParsonsChoiceDTO.class, target = ParsonsChoiceDTO.class)
    @SubclassMapping(source = FormulaDTO.class, target = FormulaDTO.class)
    @SubclassMapping(source = FreeTextRuleDTO.class, target = FreeTextRuleDTO.class)
    @SubclassMapping(source = ItemChoiceDTO.class, target = ItemChoiceDTO.class)
    @SubclassMapping(source = LogicFormulaDTO.class, target = LogicFormulaDTO.class)
    @SubclassMapping(source = QuantityDTO.class, target = QuantityDTO.class)
    @SubclassMapping(source = RegexPatternDTO.class, target = RegexPatternDTO.class)
    @SubclassMapping(source = StringChoiceDTO.class, target = StringChoiceDTO.class)
    @SubclassMapping(source = ChoiceDTO.class, target = ChoiceDTO.class)
    @SubclassMapping(source = IsaacConceptPageDTO.class, target = IsaacConceptPageDTO.class)
    @SubclassMapping(source = IsaacQuestionPageDTO.class, target = IsaacQuestionPageDTO.class)
    @SubclassMapping(source = IsaacQuizDTO.class, target = IsaacQuizDTO.class)
    @SubclassMapping(source = IsaacQuizSectionDTO.class, target = IsaacQuizSectionDTO.class)
    @SubclassMapping(source = IsaacTopicSummaryPageDTO.class, target = IsaacTopicSummaryPageDTO.class)
    @SubclassMapping(source = IsaacSymbolicLogicQuestionDTO.class, target = IsaacSymbolicLogicQuestionDTO.class)
    @SubclassMapping(source = IsaacClozeQuestionDTO.class, target = IsaacClozeQuestionDTO.class)
    @SubclassMapping(source = IsaacParsonsQuestionDTO.class, target = IsaacParsonsQuestionDTO.class)
    @SubclassMapping(source = IsaacReorderQuestionDTO.class, target = IsaacReorderQuestionDTO.class)
    @SubclassMapping(source = IsaacAnvilQuestionDTO.class, target = IsaacAnvilQuestionDTO.class)
    @SubclassMapping(source = IsaacFreeTextQuestionDTO.class, target = IsaacFreeTextQuestionDTO.class)
    @SubclassMapping(source = IsaacItemQuestionDTO.class, target = IsaacItemQuestionDTO.class)
    @SubclassMapping(source = IsaacMultiChoiceQuestionDTO.class, target = IsaacMultiChoiceQuestionDTO.class)
    @SubclassMapping(source = IsaacNumericQuestionDTO.class, target = IsaacNumericQuestionDTO.class)
    @SubclassMapping(source = IsaacQuickQuestionDTO.class, target = IsaacQuickQuestionDTO.class)
    @SubclassMapping(source = IsaacRegexMatchQuestionDTO.class, target = IsaacRegexMatchQuestionDTO.class)
    @SubclassMapping(source = IsaacStringMatchQuestionDTO.class, target = IsaacStringMatchQuestionDTO.class)
    @SubclassMapping(source = IsaacSymbolicQuestionDTO.class, target = IsaacSymbolicQuestionDTO.class)
    @SubclassMapping(source = IsaacQuestionBaseDTO.class, target = IsaacQuestionBaseDTO.class)
    @SubclassMapping(source = ChoiceQuestionDTO.class, target = ChoiceQuestionDTO.class)
    @SubclassMapping(source = QuestionDTO.class, target = QuestionDTO.class)
    @SubclassMapping(source = InteractiveCodeSnippetDTO.class, target = InteractiveCodeSnippetDTO.class)
    @SubclassMapping(source = FigureDTO.class, target = FigureDTO.class)
    @SubclassMapping(source = ImageDTO.class, target = ImageDTO.class)
    @SubclassMapping(source = VideoDTO.class, target = VideoDTO.class)
    @SubclassMapping(source = ParsonsItemDTO.class, target = ParsonsItemDTO.class)
    @SubclassMapping(source = AnvilAppDTO.class, target = AnvilAppDTO.class)
    @SubclassMapping(source = CodeSnippetDTO.class, target = CodeSnippetDTO.class)
    @SubclassMapping(source = CodeTabsDTO.class, target = CodeTabsDTO.class)
    @SubclassMapping(source = EmailTemplateDTO.class, target = EmailTemplateDTO.class)
    @SubclassMapping(source = GlossaryTermDTO.class, target = GlossaryTermDTO.class)
    @SubclassMapping(source = IsaacCardDTO.class, target = IsaacCardDTO.class)
    @SubclassMapping(source = IsaacCardDeckDTO.class, target = IsaacCardDeckDTO.class)
    @SubclassMapping(source = IsaacEventPageDTO.class, target = IsaacEventPageDTO.class)
    @SubclassMapping(source = IsaacFeaturedProfileDTO.class, target = IsaacFeaturedProfileDTO.class)
    @SubclassMapping(source = IsaacPageFragmentDTO.class, target = IsaacPageFragmentDTO.class)
    @SubclassMapping(source = IsaacPodDTO.class, target = IsaacPodDTO.class)
    @SubclassMapping(source = IsaacWildcardDTO.class, target = IsaacWildcardDTO.class)
    @SubclassMapping(source = ItemDTO.class, target = ItemDTO.class)
    @SubclassMapping(source = MediaDTO.class, target = MediaDTO.class)
    @SubclassMapping(source = NotificationDTO.class, target = NotificationDTO.class)
    ContentDTO copy(ContentDTO source);

    @SubclassMapping(source = QuizSummaryDTO.class, target = QuizSummaryDTO.class)
    ContentSummaryDTO copy(ContentSummaryDTO source);

    ParsonsChoiceDTO copy(ParsonsChoiceDTO source);

    @Mapping(target = "requiresExactMatch", expression = "java(source.requiresExactMatch())")
    FormulaDTO copy(FormulaDTO source);

    FreeTextRuleDTO copy(FreeTextRuleDTO source);
    ItemChoiceDTO copy(ItemChoiceDTO source);

    @Mapping(target = "requiresExactMatch", expression = "java(source.requiresExactMatch())")
    LogicFormulaDTO copy(LogicFormulaDTO source);

    QuantityDTO copy(QuantityDTO source);
    RegexPatternDTO copy(RegexPatternDTO source);
    StringChoiceDTO copy(StringChoiceDTO source);
    ChoiceDTO copy(ChoiceDTO source);
    IsaacConceptPageDTO copy(IsaacConceptPageDTO source);
    IsaacQuestionPageDTO copy(IsaacQuestionPageDTO source);
    IsaacQuizDTO copy(IsaacQuizDTO source);
    IsaacQuizSectionDTO copy(IsaacQuizSectionDTO source);
    IsaacTopicSummaryPageDTO copy(IsaacTopicSummaryPageDTO source);
    SeguePageDTO copy(SeguePageDTO source);
    IsaacSymbolicLogicQuestionDTO copy(IsaacSymbolicLogicQuestionDTO source);
    IsaacClozeQuestionDTO copy(IsaacClozeQuestionDTO source);
    IsaacParsonsQuestionDTO copy(IsaacParsonsQuestionDTO source);
    IsaacReorderQuestionDTO copy(IsaacReorderQuestionDTO source);
    IsaacAnvilQuestionDTO copy(IsaacAnvilQuestionDTO source);
    IsaacFreeTextQuestionDTO copy(IsaacFreeTextQuestionDTO source);
    IsaacItemQuestionDTO copy(IsaacItemQuestionDTO source);
    IsaacMultiChoiceQuestionDTO copy(IsaacMultiChoiceQuestionDTO source);

    @Mapping(target = "knownUnits", ignore = true)
    IsaacNumericQuestionDTO copy(IsaacNumericQuestionDTO source);

    IsaacQuickQuestionDTO copy(IsaacQuickQuestionDTO source);
    IsaacRegexMatchQuestionDTO copy(IsaacRegexMatchQuestionDTO source);
    IsaacStringMatchQuestionDTO copy(IsaacStringMatchQuestionDTO source);
    IsaacSymbolicQuestionDTO copy(IsaacSymbolicQuestionDTO source);
    IsaacQuestionBaseDTO copy(IsaacQuestionBaseDTO source);
    ChoiceQuestionDTO copy(ChoiceQuestionDTO source);
    QuestionDTO copy(QuestionDTO source);
    InteractiveCodeSnippetDTO copy(InteractiveCodeSnippetDTO source);
    FigureDTO copy(FigureDTO source);
    ImageDTO copy(ImageDTO source);
    VideoDTO copy(VideoDTO source);
    ParsonsItemDTO copy(ParsonsItemDTO source);
    AnvilAppDTO copy(AnvilAppDTO source);
    CodeSnippetDTO copy(CodeSnippetDTO source);
    CodeTabsDTO copy(CodeTabsDTO source);
    EmailTemplateDTO copy(EmailTemplateDTO source);
    GlossaryTermDTO copy(GlossaryTermDTO source);
    IsaacCardDTO copy(IsaacCardDTO source);
    IsaacCardDeckDTO copy(IsaacCardDeckDTO source);
    IsaacEventPageDTO copy(IsaacEventPageDTO source);
    IsaacFeaturedProfileDTO copy(IsaacFeaturedProfileDTO source);
    IsaacPageFragmentDTO copy(IsaacPageFragmentDTO source);
    IsaacPodDTO copy(IsaacPodDTO source);
    IsaacWildcardDTO copy(IsaacWildcardDTO source);
    ItemDTO copy(ItemDTO source);

    @SubclassMapping(source = FigureDTO.class, target = FigureDTO.class)
    @SubclassMapping(source = ImageDTO.class, target = ImageDTO.class)
    @SubclassMapping(source = VideoDTO.class, target = VideoDTO.class)
    MediaDTO copy(MediaDTO source);

    NotificationDTO copy(NotificationDTO source);
    QuizSummaryDTO copy(QuizSummaryDTO source);
    Location copy(Location source);
    Address copy(Address source);

    List<ContentBaseDTO> copyListOfContentBaseDTO(List<ContentBaseDTO> source);
    List<ContentDTO> copyListOfContentDTO(List<ContentDTO> source);
    List<ContentSummaryDTO> copyListOfContentSummaryDTO(List<ContentSummaryDTO> source);
    List<Location> copyListOfLocation(List<Location> source);
    List<Address> copyListOfAddress(List<Address> source);
}