package uk.ac.cam.cl.dtg.isaac.mappers;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.SubclassMapping;
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

@Mapper
public interface ContentMapperSubclasses {

  // MapStruct seems to prefer QuizSummaryDTO over ContentSummaryDTO, presumably because the former is more specific
  // This means resultType specification is required when the latter is specifically requested, which does not appear
  // to propagate to automatically-generated subclass mappings
  @SubclassMapping(source = ParsonsChoiceDTO.class, target = ContentSummaryDTO.class)
  @SubclassMapping(source = FormulaDTO.class, target = ContentSummaryDTO.class)
  @SubclassMapping(source = FreeTextRuleDTO.class, target = ContentSummaryDTO.class)
  @SubclassMapping(source = ItemChoiceDTO.class, target = ContentSummaryDTO.class)
  @SubclassMapping(source = LogicFormulaDTO.class, target = ContentSummaryDTO.class)
  @SubclassMapping(source = QuantityDTO.class, target = ContentSummaryDTO.class)
  @SubclassMapping(source = RegexPatternDTO.class, target = ContentSummaryDTO.class)
  @SubclassMapping(source = StringChoiceDTO.class, target = ContentSummaryDTO.class)
  @BeanMapping(resultType = ContentSummaryDTO.class)
  ContentSummaryDTO mapToContentSummaryDTO(ChoiceDTO source);

  @BeanMapping(resultType = ContentSummaryDTO.class)
  ContentSummaryDTO mapToContentSummaryDTO(ParsonsChoiceDTO source);

  @BeanMapping(resultType = ContentSummaryDTO.class)
  ContentSummaryDTO mapToContentSummaryDTO(FormulaDTO source);

  @BeanMapping(resultType = ContentSummaryDTO.class)
  ContentSummaryDTO mapToContentSummaryDTO(FreeTextRuleDTO source);

  @BeanMapping(resultType = ContentSummaryDTO.class)
  ContentSummaryDTO mapToContentSummaryDTO(ItemChoiceDTO source);

  @BeanMapping(resultType = ContentSummaryDTO.class)
  ContentSummaryDTO mapToContentSummaryDTO(LogicFormulaDTO source);

  @BeanMapping(resultType = ContentSummaryDTO.class)
  ContentSummaryDTO mapToContentSummaryDTO(QuantityDTO source);

  @BeanMapping(resultType = ContentSummaryDTO.class)
  ContentSummaryDTO mapToContentSummaryDTO(RegexPatternDTO source);

  @BeanMapping(resultType = ContentSummaryDTO.class)
  ContentSummaryDTO mapToContentSummaryDTO(StringChoiceDTO source);

  @SubclassMapping(source = IsaacConceptPageDTO.class, target = ContentSummaryDTO.class)
  @SubclassMapping(source = IsaacQuestionPageDTO.class, target = ContentSummaryDTO.class)
  @SubclassMapping(source = IsaacQuizDTO.class, target = ContentSummaryDTO.class)
  @SubclassMapping(source = IsaacQuizSectionDTO.class, target = ContentSummaryDTO.class)
  @SubclassMapping(source = IsaacTopicSummaryPageDTO.class, target = ContentSummaryDTO.class)
  @BeanMapping(resultType = ContentSummaryDTO.class)
  ContentSummaryDTO mapToContentSummaryDTO(SeguePageDTO source);

  @BeanMapping(resultType = ContentSummaryDTO.class)
  ContentSummaryDTO mapToContentSummaryDTO(IsaacConceptPageDTO source);

  @BeanMapping(resultType = ContentSummaryDTO.class)
  ContentSummaryDTO mapToContentSummaryDTO(IsaacQuestionPageDTO source);

  @BeanMapping(resultType = ContentSummaryDTO.class)
  ContentSummaryDTO mapToContentSummaryDTO(IsaacQuizDTO source);

  @BeanMapping(resultType = ContentSummaryDTO.class)
  ContentSummaryDTO mapToContentSummaryDTO(IsaacQuizSectionDTO source);

  @BeanMapping(resultType = ContentSummaryDTO.class)
  ContentSummaryDTO mapToContentSummaryDTO(IsaacTopicSummaryPageDTO source);

  @SubclassMapping(source = IsaacSymbolicLogicQuestionDTO.class, target = ContentSummaryDTO.class)
  @SubclassMapping(source = IsaacClozeQuestionDTO.class, target = ContentSummaryDTO.class)
  @SubclassMapping(source = IsaacParsonsQuestionDTO.class, target = ContentSummaryDTO.class)
  @SubclassMapping(source = IsaacReorderQuestionDTO.class, target = ContentSummaryDTO.class)
  @SubclassMapping(source = IsaacAnvilQuestionDTO.class, target = ContentSummaryDTO.class)
  @SubclassMapping(source = IsaacFreeTextQuestionDTO.class, target = ContentSummaryDTO.class)
  @SubclassMapping(source = IsaacItemQuestionDTO.class, target = ContentSummaryDTO.class)
  @SubclassMapping(source = IsaacMultiChoiceQuestionDTO.class, target = ContentSummaryDTO.class)
  @SubclassMapping(source = IsaacNumericQuestionDTO.class, target = ContentSummaryDTO.class)
  @SubclassMapping(source = IsaacQuickQuestionDTO.class, target = ContentSummaryDTO.class)
  @SubclassMapping(source = IsaacRegexMatchQuestionDTO.class, target = ContentSummaryDTO.class)
  @SubclassMapping(source = IsaacStringMatchQuestionDTO.class, target = ContentSummaryDTO.class)
  @SubclassMapping(source = IsaacSymbolicQuestionDTO.class, target = ContentSummaryDTO.class)
  @SubclassMapping(source = IsaacQuestionBaseDTO.class, target = ContentSummaryDTO.class)
  @SubclassMapping(source = ChoiceQuestionDTO.class, target = ContentSummaryDTO.class)
  @BeanMapping(resultType = ContentSummaryDTO.class)
  ContentSummaryDTO mapToContentSummaryDTO(QuestionDTO source);

  @BeanMapping(resultType = ContentSummaryDTO.class)
  ContentSummaryDTO mapToContentSummaryDTO(IsaacSymbolicLogicQuestionDTO source);

  @BeanMapping(resultType = ContentSummaryDTO.class)
  ContentSummaryDTO mapToContentSummaryDTO(IsaacClozeQuestionDTO source);

  @BeanMapping(resultType = ContentSummaryDTO.class)
  ContentSummaryDTO mapToContentSummaryDTO(IsaacParsonsQuestionDTO source);

  @BeanMapping(resultType = ContentSummaryDTO.class)
  ContentSummaryDTO mapToContentSummaryDTO(IsaacReorderQuestionDTO source);

  @BeanMapping(resultType = ContentSummaryDTO.class)
  ContentSummaryDTO mapToContentSummaryDTO(IsaacAnvilQuestionDTO source);

  @BeanMapping(resultType = ContentSummaryDTO.class)
  ContentSummaryDTO mapToContentSummaryDTO(IsaacFreeTextQuestionDTO source);

  @BeanMapping(resultType = ContentSummaryDTO.class)
  ContentSummaryDTO mapToContentSummaryDTO(IsaacItemQuestionDTO source);

  @BeanMapping(resultType = ContentSummaryDTO.class)
  ContentSummaryDTO mapToContentSummaryDTO(IsaacMultiChoiceQuestionDTO source);

  @BeanMapping(resultType = ContentSummaryDTO.class)
  ContentSummaryDTO mapToContentSummaryDTO(IsaacNumericQuestionDTO source);

  @BeanMapping(resultType = ContentSummaryDTO.class)
  ContentSummaryDTO mapToContentSummaryDTO(IsaacQuickQuestionDTO source);

  @BeanMapping(resultType = ContentSummaryDTO.class)
  ContentSummaryDTO mapToContentSummaryDTO(IsaacRegexMatchQuestionDTO source);

  @BeanMapping(resultType = ContentSummaryDTO.class)
  ContentSummaryDTO mapToContentSummaryDTO(IsaacStringMatchQuestionDTO source);

  @BeanMapping(resultType = ContentSummaryDTO.class)
  ContentSummaryDTO mapToContentSummaryDTO(IsaacSymbolicQuestionDTO source);

  @BeanMapping(resultType = ContentSummaryDTO.class)
  ContentSummaryDTO mapToContentSummaryDTO(IsaacQuestionBaseDTO source);

  @BeanMapping(resultType = ContentSummaryDTO.class)
  ContentSummaryDTO mapToContentSummaryDTO(ChoiceQuestionDTO source);

  @BeanMapping(resultType = ContentSummaryDTO.class)
  ContentSummaryDTO mapToContentSummaryDTO(FigureDTO source);

  @BeanMapping(resultType = ContentSummaryDTO.class)
  ContentSummaryDTO mapToContentSummaryDTO(ImageDTO source);

  @BeanMapping(resultType = ContentSummaryDTO.class)
  ContentSummaryDTO mapToContentSummaryDTO(VideoDTO source);

  @SubclassMapping(source = FigureDTO.class, target = ContentSummaryDTO.class)
  @SubclassMapping(source = ImageDTO.class, target = ContentSummaryDTO.class)
  @SubclassMapping(source = VideoDTO.class, target = ContentSummaryDTO.class)
  @BeanMapping(resultType = ContentSummaryDTO.class)
  ContentSummaryDTO mapToContentSummaryDTO(MediaDTO source);

  @BeanMapping(resultType = ContentSummaryDTO.class)
  ContentSummaryDTO mapToContentSummaryDTO(InteractiveCodeSnippetDTO source);

  @BeanMapping(resultType = ContentSummaryDTO.class)
  ContentSummaryDTO mapToContentSummaryDTO(ParsonsItemDTO source);

  @BeanMapping(resultType = ContentSummaryDTO.class)
  ContentSummaryDTO mapToContentSummaryDTO(AnvilAppDTO source);

  @BeanMapping(resultType = ContentSummaryDTO.class)
  ContentSummaryDTO mapToContentSummaryDTO(CodeSnippetDTO source);

  @BeanMapping(resultType = ContentSummaryDTO.class)
  ContentSummaryDTO mapToContentSummaryDTO(CodeTabsDTO source);

  @BeanMapping(resultType = ContentSummaryDTO.class)
  ContentSummaryDTO mapToContentSummaryDTO(EmailTemplateDTO source);

  @BeanMapping(resultType = ContentSummaryDTO.class)
  ContentSummaryDTO mapToContentSummaryDTO(GlossaryTermDTO source);

  @BeanMapping(resultType = ContentSummaryDTO.class)
  ContentSummaryDTO mapToContentSummaryDTO(IsaacCardDTO source);

  @BeanMapping(resultType = ContentSummaryDTO.class)
  ContentSummaryDTO mapToContentSummaryDTO(IsaacCardDeckDTO source);

  @BeanMapping(resultType = ContentSummaryDTO.class)
  ContentSummaryDTO mapToContentSummaryDTO(IsaacEventPageDTO source);

  @BeanMapping(resultType = ContentSummaryDTO.class)
  ContentSummaryDTO mapToContentSummaryDTO(IsaacFeaturedProfileDTO source);

  @BeanMapping(resultType = ContentSummaryDTO.class)
  ContentSummaryDTO mapToContentSummaryDTO(IsaacPageFragmentDTO source);

  @BeanMapping(resultType = ContentSummaryDTO.class)
  ContentSummaryDTO mapToContentSummaryDTO(IsaacPodDTO source);

  @BeanMapping(resultType = ContentSummaryDTO.class)
  ContentSummaryDTO mapToContentSummaryDTO(IsaacWildcardDTO source);

  @BeanMapping(resultType = ContentSummaryDTO.class)
  ContentSummaryDTO mapToContentSummaryDTO(ItemDTO source);

  @BeanMapping(resultType = ContentSummaryDTO.class)
  ContentSummaryDTO mapToContentSummaryDTO(NotificationDTO source);
}
