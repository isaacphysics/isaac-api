package uk.ac.cam.cl.dtg.isaac.mappers;

import org.mapstruct.BeanMapping;
import org.mapstruct.InheritConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.SubclassMapping;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacPod;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacWildcard;
import uk.ac.cam.cl.dtg.isaac.dos.content.CodeSnippet;
import uk.ac.cam.cl.dtg.isaac.dos.content.InteractiveCodeSnippet;
import uk.ac.cam.cl.dtg.isaac.dto.GameboardItem;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacAnvilQuestionDTO;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacCardDTO;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacCardDeckDTO;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacClozeQuestionDTO;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacConceptPageDTO;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacEventPageDTO;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacFeaturedProfileDTO;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacFreeTextQuestionDTO;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacItemQuestionDTO;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacMultiChoiceQuestionDTO;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacNumericQuestionDTO;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacPageFragmentDTO;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacParsonsQuestionDTO;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacPodDTO;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacQuestionBaseDTO;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacQuestionPageDTO;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacQuickQuestionDTO;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacQuizDTO;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacQuizSectionDTO;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacRegexMatchQuestionDTO;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacReorderQuestionDTO;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacStringMatchQuestionDTO;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacSymbolicLogicQuestionDTO;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacSymbolicQuestionDTO;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacTopicSummaryPageDTO;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacWildcardDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.AnvilAppDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.ChoiceDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.ChoiceQuestionDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.CodeSnippetDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.CodeTabsDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentSummaryDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.EmailTemplateDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.FigureDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.FormulaDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.FreeTextRuleDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.GlossaryTermDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.ImageDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.InteractiveCodeSnippetDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.ItemChoiceDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.ItemDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.LogicFormulaDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.MediaDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.NotificationDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.ParsonsChoiceDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.ParsonsItemDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.QuantityDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.QuestionDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.RegexPatternDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.SeguePageDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.StringChoiceDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.VideoDTO;

/**
 * ContentMapperSubclasses is used to contain (some of) the mapping methods for Content subclasses that are not invoked
 * directly in the code but require additional specification to function correctly, in order to improve organisation
 * and mitigate some implementation duplication caused by the MainObjectMapper.
 */
@Mapper
@SuppressWarnings({"unused", "checkstyle:OverloadMethodsDeclarationOrder"})
public interface ContentMapperSubclasses {

  /*
   * MapStruct seems to prefer QuizSummaryDTO over ContentSummaryDTO, presumably because the former is more specific.
   * This means resultType specification is required when the latter is specifically requested, which does not appear
   * to propagate to automatically-generated subclass mappings.
   *
   * The @InheritConfiguration annotation can propagate ignored fields (and other annotations) from parent methods
   * within the same mapper class. However, it can suffer from ambiguity issues for classes with multiple levels of
   * inheritance handled within the same mapper class. While using different method names can resolve this, that may not
   * conflict with a preference for consistent method names.
   * Additionally, if the mapped class has fields that the parent does not, additional @Mapping annotations may be
   * required to override the 'ignore' setting.
   * For such cases with a small number of annotations, it may be simpler to simply apply mapping configurations from
   * scratch instead of building on the parent configuration.
   */

  // toContentSummary - ChoiceDTO Subclasses

  @Mapping(target = "url", ignore = true)
  @Mapping(target = "supersededBy", ignore = true)
  @Mapping(target = "summary", ignore = true)
  @Mapping(target = "questionPartIds", ignore = true)
  @Mapping(target = "difficulty", ignore = true)
  @Mapping(target = "correct", ignore = true)
  @SubclassMapping(source = FormulaDTO.class, target = ContentSummaryDTO.class)
  @SubclassMapping(source = FreeTextRuleDTO.class, target = ContentSummaryDTO.class)
  @SubclassMapping(source = ItemChoiceDTO.class, target = ContentSummaryDTO.class)
  @SubclassMapping(source = LogicFormulaDTO.class, target = ContentSummaryDTO.class)
  @SubclassMapping(source = QuantityDTO.class, target = ContentSummaryDTO.class)
  @SubclassMapping(source = RegexPatternDTO.class, target = ContentSummaryDTO.class)
  @SubclassMapping(source = StringChoiceDTO.class, target = ContentSummaryDTO.class)
  @BeanMapping(resultType = ContentSummaryDTO.class)
  ContentSummaryDTO mapToContentSummaryDTO(ChoiceDTO source);

  @Mapping(target = "url", ignore = true)
  @Mapping(target = "supersededBy", ignore = true)
  @Mapping(target = "summary", ignore = true)
  @Mapping(target = "questionPartIds", ignore = true)
  @Mapping(target = "difficulty", ignore = true)
  @Mapping(target = "correct", ignore = true)
  @BeanMapping(resultType = ContentSummaryDTO.class)
  ContentSummaryDTO mapToContentSummaryDTO(ParsonsChoiceDTO source);

  @InheritConfiguration
  @BeanMapping(resultType = ContentSummaryDTO.class)
  ContentSummaryDTO mapToContentSummaryDTO(FormulaDTO source);

  @InheritConfiguration
  @BeanMapping(resultType = ContentSummaryDTO.class)
  ContentSummaryDTO mapToContentSummaryDTO(FreeTextRuleDTO source);

  @InheritConfiguration
  @SubclassMapping(source = ParsonsChoiceDTO.class, target = ContentSummaryDTO.class)
  @BeanMapping(resultType = ContentSummaryDTO.class)
  ContentSummaryDTO mapToContentSummaryDTO(ItemChoiceDTO source);

  @InheritConfiguration
  @BeanMapping(resultType = ContentSummaryDTO.class)
  ContentSummaryDTO mapToContentSummaryDTO(LogicFormulaDTO source);

  @InheritConfiguration
  @BeanMapping(resultType = ContentSummaryDTO.class)
  ContentSummaryDTO mapToContentSummaryDTO(QuantityDTO source);

  @InheritConfiguration
  @BeanMapping(resultType = ContentSummaryDTO.class)
  ContentSummaryDTO mapToContentSummaryDTO(RegexPatternDTO source);

  @InheritConfiguration
  @BeanMapping(resultType = ContentSummaryDTO.class)
  ContentSummaryDTO mapToContentSummaryDTO(StringChoiceDTO source);

  @Mapping(target = "url", ignore = true)
  @Mapping(target = "supersededBy", ignore = true)
  @Mapping(target = "questionPartIds", ignore = true)
  @Mapping(target = "difficulty", ignore = true)
  @Mapping(target = "correct", ignore = true)
  @SubclassMapping(source = IsaacConceptPageDTO.class, target = ContentSummaryDTO.class)
  @SubclassMapping(source = IsaacQuestionPageDTO.class, target = ContentSummaryDTO.class)
  @SubclassMapping(source = IsaacQuizDTO.class, target = ContentSummaryDTO.class)
  @SubclassMapping(source = IsaacQuizSectionDTO.class, target = ContentSummaryDTO.class)
  @SubclassMapping(source = IsaacTopicSummaryPageDTO.class, target = ContentSummaryDTO.class)
  @BeanMapping(resultType = ContentSummaryDTO.class)
  ContentSummaryDTO mapToContentSummaryDTO(SeguePageDTO source);

  @InheritConfiguration
  @BeanMapping(resultType = ContentSummaryDTO.class)
  ContentSummaryDTO mapToContentSummaryDTO(IsaacConceptPageDTO source);

  @Mapping(target = "url", ignore = true)
  @Mapping(target = "questionPartIds", ignore = true)
  @Mapping(target = "correct", ignore = true)
  @BeanMapping(resultType = ContentSummaryDTO.class)
  ContentSummaryDTO mapToContentSummaryDTO(IsaacQuestionPageDTO source);

  @InheritConfiguration
  @BeanMapping(resultType = ContentSummaryDTO.class)
  ContentSummaryDTO mapToContentSummaryDTO(IsaacQuizDTO source);

  @InheritConfiguration
  @BeanMapping(resultType = ContentSummaryDTO.class)
  ContentSummaryDTO mapToContentSummaryDTO(IsaacQuizSectionDTO source);

  @InheritConfiguration
  @BeanMapping(resultType = ContentSummaryDTO.class)
  ContentSummaryDTO mapToContentSummaryDTO(IsaacTopicSummaryPageDTO source);

  // toContentSummary - QuestionDTO Subclasses

  @Mapping(target = "url", ignore = true)
  @Mapping(target = "supersededBy", ignore = true)
  @Mapping(target = "summary", ignore = true)
  @Mapping(target = "questionPartIds", ignore = true)
  @Mapping(target = "difficulty", ignore = true)
  @Mapping(target = "correct", ignore = true)
  @SubclassMapping(source = ChoiceQuestionDTO.class, target = ContentSummaryDTO.class)
  @BeanMapping(resultType = ContentSummaryDTO.class)
  ContentSummaryDTO mapToContentSummaryDTO(QuestionDTO source);

  @Mapping(target = "url", ignore = true)
  @Mapping(target = "supersededBy", ignore = true)
  @Mapping(target = "summary", ignore = true)
  @Mapping(target = "questionPartIds", ignore = true)
  @Mapping(target = "difficulty", ignore = true)
  @Mapping(target = "correct", ignore = true)
  @BeanMapping(resultType = ContentSummaryDTO.class)
  ContentSummaryDTO mapToContentSummaryDTO(IsaacSymbolicLogicQuestionDTO source);

  @Mapping(target = "url", ignore = true)
  @Mapping(target = "supersededBy", ignore = true)
  @Mapping(target = "summary", ignore = true)
  @Mapping(target = "questionPartIds", ignore = true)
  @Mapping(target = "difficulty", ignore = true)
  @Mapping(target = "correct", ignore = true)
  @BeanMapping(resultType = ContentSummaryDTO.class)
  ContentSummaryDTO mapToContentSummaryDTO(IsaacClozeQuestionDTO source);

  @Mapping(target = "url", ignore = true)
  @Mapping(target = "supersededBy", ignore = true)
  @Mapping(target = "summary", ignore = true)
  @Mapping(target = "questionPartIds", ignore = true)
  @Mapping(target = "difficulty", ignore = true)
  @Mapping(target = "correct", ignore = true)
  @BeanMapping(resultType = ContentSummaryDTO.class)
  ContentSummaryDTO mapToContentSummaryDTO(IsaacParsonsQuestionDTO source);

  @Mapping(target = "url", ignore = true)
  @Mapping(target = "supersededBy", ignore = true)
  @Mapping(target = "summary", ignore = true)
  @Mapping(target = "questionPartIds", ignore = true)
  @Mapping(target = "difficulty", ignore = true)
  @Mapping(target = "correct", ignore = true)
  @BeanMapping(resultType = ContentSummaryDTO.class)
  ContentSummaryDTO mapToContentSummaryDTO(IsaacReorderQuestionDTO source);

  @Mapping(target = "url", ignore = true)
  @Mapping(target = "supersededBy", ignore = true)
  @Mapping(target = "summary", ignore = true)
  @Mapping(target = "questionPartIds", ignore = true)
  @Mapping(target = "difficulty", ignore = true)
  @Mapping(target = "correct", ignore = true)
  @BeanMapping(resultType = ContentSummaryDTO.class)
  ContentSummaryDTO mapToContentSummaryDTO(IsaacAnvilQuestionDTO source);

  @Mapping(target = "url", ignore = true)
  @Mapping(target = "supersededBy", ignore = true)
  @Mapping(target = "summary", ignore = true)
  @Mapping(target = "questionPartIds", ignore = true)
  @Mapping(target = "difficulty", ignore = true)
  @Mapping(target = "correct", ignore = true)
  @BeanMapping(resultType = ContentSummaryDTO.class)
  ContentSummaryDTO mapToContentSummaryDTO(IsaacFreeTextQuestionDTO source);

  @Mapping(target = "url", ignore = true)
  @Mapping(target = "supersededBy", ignore = true)
  @Mapping(target = "summary", ignore = true)
  @Mapping(target = "questionPartIds", ignore = true)
  @Mapping(target = "difficulty", ignore = true)
  @Mapping(target = "correct", ignore = true)
  @SubclassMapping(source = IsaacClozeQuestionDTO.class, target = ContentSummaryDTO.class)
  @SubclassMapping(source = IsaacParsonsQuestionDTO.class, target = ContentSummaryDTO.class)
  @SubclassMapping(source = IsaacReorderQuestionDTO.class, target = ContentSummaryDTO.class)
  @BeanMapping(resultType = ContentSummaryDTO.class)
  ContentSummaryDTO mapToContentSummaryDTO(IsaacItemQuestionDTO source);

  @Mapping(target = "url", ignore = true)
  @Mapping(target = "supersededBy", ignore = true)
  @Mapping(target = "summary", ignore = true)
  @Mapping(target = "questionPartIds", ignore = true)
  @Mapping(target = "difficulty", ignore = true)
  @Mapping(target = "correct", ignore = true)
  @BeanMapping(resultType = ContentSummaryDTO.class)
  ContentSummaryDTO mapToContentSummaryDTO(IsaacMultiChoiceQuestionDTO source);

  @Mapping(target = "url", ignore = true)
  @Mapping(target = "supersededBy", ignore = true)
  @Mapping(target = "summary", ignore = true)
  @Mapping(target = "questionPartIds", ignore = true)
  @Mapping(target = "difficulty", ignore = true)
  @Mapping(target = "correct", ignore = true)
  @BeanMapping(resultType = ContentSummaryDTO.class)
  ContentSummaryDTO mapToContentSummaryDTO(IsaacNumericQuestionDTO source);

  @Mapping(target = "url", ignore = true)
  @Mapping(target = "supersededBy", ignore = true)
  @Mapping(target = "summary", ignore = true)
  @Mapping(target = "questionPartIds", ignore = true)
  @Mapping(target = "difficulty", ignore = true)
  @Mapping(target = "correct", ignore = true)
  @BeanMapping(resultType = ContentSummaryDTO.class)
  ContentSummaryDTO mapToContentSummaryDTO(IsaacQuickQuestionDTO source);

  @Mapping(target = "url", ignore = true)
  @Mapping(target = "supersededBy", ignore = true)
  @Mapping(target = "summary", ignore = true)
  @Mapping(target = "questionPartIds", ignore = true)
  @Mapping(target = "difficulty", ignore = true)
  @Mapping(target = "correct", ignore = true)
  @BeanMapping(resultType = ContentSummaryDTO.class)
  ContentSummaryDTO mapToContentSummaryDTO(IsaacRegexMatchQuestionDTO source);

  @Mapping(target = "url", ignore = true)
  @Mapping(target = "supersededBy", ignore = true)
  @Mapping(target = "summary", ignore = true)
  @Mapping(target = "questionPartIds", ignore = true)
  @Mapping(target = "difficulty", ignore = true)
  @Mapping(target = "correct", ignore = true)
  @BeanMapping(resultType = ContentSummaryDTO.class)
  ContentSummaryDTO mapToContentSummaryDTO(IsaacStringMatchQuestionDTO source);

  @Mapping(target = "url", ignore = true)
  @Mapping(target = "supersededBy", ignore = true)
  @Mapping(target = "summary", ignore = true)
  @Mapping(target = "questionPartIds", ignore = true)
  @Mapping(target = "difficulty", ignore = true)
  @Mapping(target = "correct", ignore = true)
  @SubclassMapping(source = IsaacSymbolicLogicQuestionDTO.class, target = ContentSummaryDTO.class)
  @BeanMapping(resultType = ContentSummaryDTO.class)
  ContentSummaryDTO mapToContentSummaryDTO(IsaacSymbolicQuestionDTO source);

  @Mapping(target = "url", ignore = true)
  @Mapping(target = "supersededBy", ignore = true)
  @Mapping(target = "summary", ignore = true)
  @Mapping(target = "questionPartIds", ignore = true)
  @Mapping(target = "difficulty", ignore = true)
  @Mapping(target = "correct", ignore = true)
  @SubclassMapping(source = IsaacAnvilQuestionDTO.class, target = ContentSummaryDTO.class)
  @SubclassMapping(source = IsaacFreeTextQuestionDTO.class, target = ContentSummaryDTO.class)
  @SubclassMapping(source = IsaacItemQuestionDTO.class, target = ContentSummaryDTO.class)
  @SubclassMapping(source = IsaacMultiChoiceQuestionDTO.class, target = ContentSummaryDTO.class)
  @SubclassMapping(source = IsaacNumericQuestionDTO.class, target = ContentSummaryDTO.class)
  @SubclassMapping(source = IsaacQuickQuestionDTO.class, target = ContentSummaryDTO.class)
  @SubclassMapping(source = IsaacRegexMatchQuestionDTO.class, target = ContentSummaryDTO.class)
  @SubclassMapping(source = IsaacStringMatchQuestionDTO.class, target = ContentSummaryDTO.class)
  @BeanMapping(resultType = ContentSummaryDTO.class)
  ContentSummaryDTO mapToContentSummaryDTO(IsaacQuestionBaseDTO source);

  @InheritConfiguration
  @SubclassMapping(source = IsaacQuestionBaseDTO.class, target = ContentSummaryDTO.class)
  @BeanMapping(resultType = ContentSummaryDTO.class)
  ContentSummaryDTO mapToContentSummaryDTO(ChoiceQuestionDTO source);

  // toContentSummary - MediaDTO Subclasses

  @Mapping(target = "url", ignore = true)
  @Mapping(target = "supersededBy", ignore = true)
  @Mapping(target = "summary", ignore = true)
  @Mapping(target = "questionPartIds", ignore = true)
  @Mapping(target = "difficulty", ignore = true)
  @Mapping(target = "correct", ignore = true)
  @BeanMapping(resultType = ContentSummaryDTO.class)
  ContentSummaryDTO mapToContentSummaryDTO(FigureDTO source);

  @InheritConfiguration
  @BeanMapping(resultType = ContentSummaryDTO.class)
  ContentSummaryDTO mapToContentSummaryDTO(ImageDTO source);

  @InheritConfiguration
  @BeanMapping(resultType = ContentSummaryDTO.class)
  ContentSummaryDTO mapToContentSummaryDTO(VideoDTO source);

  @Mapping(target = "url", ignore = true)
  @Mapping(target = "supersededBy", ignore = true)
  @Mapping(target = "summary", ignore = true)
  @Mapping(target = "questionPartIds", ignore = true)
  @Mapping(target = "difficulty", ignore = true)
  @Mapping(target = "correct", ignore = true)
  @SubclassMapping(source = FigureDTO.class, target = ContentSummaryDTO.class)
  @SubclassMapping(source = ImageDTO.class, target = ContentSummaryDTO.class)
  @SubclassMapping(source = VideoDTO.class, target = ContentSummaryDTO.class)
  @BeanMapping(resultType = ContentSummaryDTO.class)
  ContentSummaryDTO mapToContentSummaryDTO(MediaDTO source);

  // toContentSummary - Other DTO Subclasses

  @Mapping(target = "supersededBy", ignore = true)
  @Mapping(target = "summary", ignore = true)
  @Mapping(target = "questionPartIds", ignore = true)
  @Mapping(target = "difficulty", ignore = true)
  @Mapping(target = "correct", ignore = true)
  @BeanMapping(resultType = ContentSummaryDTO.class)
  ContentSummaryDTO mapToContentSummaryDTO(InteractiveCodeSnippetDTO source);

  @Mapping(target = "url", ignore = true)
  @Mapping(target = "supersededBy", ignore = true)
  @Mapping(target = "summary", ignore = true)
  @Mapping(target = "questionPartIds", ignore = true)
  @Mapping(target = "difficulty", ignore = true)
  @Mapping(target = "correct", ignore = true)
  @BeanMapping(resultType = ContentSummaryDTO.class)
  ContentSummaryDTO mapToContentSummaryDTO(ParsonsItemDTO source);

  @Mapping(target = "url", ignore = true)
  @Mapping(target = "supersededBy", ignore = true)
  @Mapping(target = "summary", ignore = true)
  @Mapping(target = "questionPartIds", ignore = true)
  @Mapping(target = "difficulty", ignore = true)
  @Mapping(target = "correct", ignore = true)
  @BeanMapping(resultType = ContentSummaryDTO.class)
  ContentSummaryDTO mapToContentSummaryDTO(AnvilAppDTO source);

  @Mapping(target = "supersededBy", ignore = true)
  @Mapping(target = "summary", ignore = true)
  @Mapping(target = "questionPartIds", ignore = true)
  @Mapping(target = "difficulty", ignore = true)
  @Mapping(target = "correct", ignore = true)
  @SubclassMapping(source = InteractiveCodeSnippetDTO.class, target = ContentSummaryDTO.class)
  @BeanMapping(resultType = ContentSummaryDTO.class)
  ContentSummaryDTO mapToContentSummaryDTO(CodeSnippetDTO source);

  @Mapping(target = "url", ignore = true)
  @Mapping(target = "supersededBy", ignore = true)
  @Mapping(target = "summary", ignore = true)
  @Mapping(target = "questionPartIds", ignore = true)
  @Mapping(target = "difficulty", ignore = true)
  @Mapping(target = "correct", ignore = true)
  @BeanMapping(resultType = ContentSummaryDTO.class)
  ContentSummaryDTO mapToContentSummaryDTO(CodeTabsDTO source);

  @Mapping(target = "url", ignore = true)
  @Mapping(target = "supersededBy", ignore = true)
  @Mapping(target = "summary", ignore = true)
  @Mapping(target = "questionPartIds", ignore = true)
  @Mapping(target = "difficulty", ignore = true)
  @Mapping(target = "correct", ignore = true)
  @BeanMapping(resultType = ContentSummaryDTO.class)
  ContentSummaryDTO mapToContentSummaryDTO(EmailTemplateDTO source);

  @Mapping(target = "url", ignore = true)
  @Mapping(target = "supersededBy", ignore = true)
  @Mapping(target = "summary", ignore = true)
  @Mapping(target = "questionPartIds", ignore = true)
  @Mapping(target = "difficulty", ignore = true)
  @Mapping(target = "correct", ignore = true)
  @BeanMapping(resultType = ContentSummaryDTO.class)
  ContentSummaryDTO mapToContentSummaryDTO(GlossaryTermDTO source);

  @Mapping(target = "url", ignore = true)
  @Mapping(target = "supersededBy", ignore = true)
  @Mapping(target = "summary", ignore = true)
  @Mapping(target = "questionPartIds", ignore = true)
  @Mapping(target = "difficulty", ignore = true)
  @Mapping(target = "correct", ignore = true)
  @BeanMapping(resultType = ContentSummaryDTO.class)
  ContentSummaryDTO mapToContentSummaryDTO(IsaacCardDTO source);

  @Mapping(target = "url", ignore = true)
  @Mapping(target = "supersededBy", ignore = true)
  @Mapping(target = "summary", ignore = true)
  @Mapping(target = "questionPartIds", ignore = true)
  @Mapping(target = "difficulty", ignore = true)
  @Mapping(target = "correct", ignore = true)
  @BeanMapping(resultType = ContentSummaryDTO.class)
  ContentSummaryDTO mapToContentSummaryDTO(IsaacCardDeckDTO source);

  @Mapping(target = "url", ignore = true)
  @Mapping(target = "supersededBy", ignore = true)
  @Mapping(target = "summary", ignore = true)
  @Mapping(target = "questionPartIds", ignore = true)
  @Mapping(target = "difficulty", ignore = true)
  @Mapping(target = "correct", ignore = true)
  @BeanMapping(resultType = ContentSummaryDTO.class)
  ContentSummaryDTO mapToContentSummaryDTO(IsaacEventPageDTO source);

  @Mapping(target = "url", ignore = true)
  @Mapping(target = "supersededBy", ignore = true)
  @Mapping(target = "summary", ignore = true)
  @Mapping(target = "questionPartIds", ignore = true)
  @Mapping(target = "difficulty", ignore = true)
  @Mapping(target = "correct", ignore = true)
  @BeanMapping(resultType = ContentSummaryDTO.class)
  ContentSummaryDTO mapToContentSummaryDTO(IsaacFeaturedProfileDTO source);

  @Mapping(target = "url", ignore = true)
  @Mapping(target = "supersededBy", ignore = true)
  @Mapping(target = "questionPartIds", ignore = true)
  @Mapping(target = "difficulty", ignore = true)
  @Mapping(target = "correct", ignore = true)
  @BeanMapping(resultType = ContentSummaryDTO.class)
  ContentSummaryDTO mapToContentSummaryDTO(IsaacPageFragmentDTO source);

  @Mapping(target = "supersededBy", ignore = true)
  @Mapping(target = "summary", ignore = true)
  @Mapping(target = "questionPartIds", ignore = true)
  @Mapping(target = "difficulty", ignore = true)
  @Mapping(target = "correct", ignore = true)
  @BeanMapping(resultType = ContentSummaryDTO.class)
  ContentSummaryDTO mapToContentSummaryDTO(IsaacPodDTO source);

  @Mapping(target = "supersededBy", ignore = true)
  @Mapping(target = "summary", ignore = true)
  @Mapping(target = "questionPartIds", ignore = true)
  @Mapping(target = "difficulty", ignore = true)
  @Mapping(target = "correct", ignore = true)
  @BeanMapping(resultType = ContentSummaryDTO.class)
  ContentSummaryDTO mapToContentSummaryDTO(IsaacWildcardDTO source);

  @Mapping(target = "url", ignore = true)
  @Mapping(target = "supersededBy", ignore = true)
  @Mapping(target = "summary", ignore = true)
  @Mapping(target = "questionPartIds", ignore = true)
  @Mapping(target = "difficulty", ignore = true)
  @Mapping(target = "correct", ignore = true)
  @BeanMapping(resultType = ContentSummaryDTO.class)
  ContentSummaryDTO mapToContentSummaryDTO(ItemDTO source);

  @Mapping(target = "url", ignore = true)
  @Mapping(target = "supersededBy", ignore = true)
  @Mapping(target = "summary", ignore = true)
  @Mapping(target = "questionPartIds", ignore = true)
  @Mapping(target = "difficulty", ignore = true)
  @Mapping(target = "correct", ignore = true)
  @BeanMapping(resultType = ContentSummaryDTO.class)
  ContentSummaryDTO mapToContentSummaryDTO(NotificationDTO source);

  // toIsaacWildCard

  @Mapping(target = "description", ignore = true)
  @SubclassMapping(source = InteractiveCodeSnippet.class, target = IsaacWildcard.class)
  IsaacWildcard mapToIsaacWildcard(CodeSnippet source);

  @Mapping(target = "description", ignore = true)
  IsaacWildcard mapToIsaacWildcard(InteractiveCodeSnippet source);

  @Mapping(target = "description", ignore = true)
  IsaacWildcard mapToIsaacWildcard(IsaacPod source);

  IsaacWildcard copy(IsaacWildcard source);

  // toGameboardItem

  @Mapping(target = "uri", ignore = true)
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
  @SubclassMapping(source = IsaacQuestionPageDTO.class, target = GameboardItem.class)
  GameboardItem mapToGameboardItem(SeguePageDTO source);

  @Mapping(target = "uri", ignore = true)
  @Mapping(target = "state", ignore = true)
  @Mapping(target = "questionPartsTotal", ignore = true)
  @Mapping(target = "questionPartsNotAttempted", ignore = true)
  @Mapping(target = "questionPartsIncorrect", ignore = true)
  @Mapping(target = "questionPartsCorrect", ignore = true)
  @Mapping(target = "questionPartStates", ignore = true)
  @Mapping(target = "description", ignore = true)
  @Mapping(target = "creationContext", ignore = true)
  @Mapping(target = "contentType", ignore = true)
  @Mapping(target = "boardId", ignore = true)
  GameboardItem mapToGameboardItem(IsaacQuestionPageDTO source);
}
