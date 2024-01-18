package uk.ac.cam.cl.dtg.segue.dao;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.ac.cam.cl.dtg.util.ReflectionUtils.getClasses;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.ac.cam.cl.dtg.isaac.dos.content.CodeSnippet;
import uk.ac.cam.cl.dtg.isaac.dos.content.Content;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentDTO;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentMapper;

class ContentMapperTest {

  private ContentMapper contentMapper;

  @BeforeEach
  public void setUp() {
    this.contentMapper = new ContentMapper(getClasses("uk.ac.cam.cl.dtg.isaac"));
  }

  @Test
  void getDTOByDO_CodeSnippetDOtoDTO_ExpandableSetInDTO() {
    // Arrange
    CodeSnippet codeSnippet = new CodeSnippet();
    codeSnippet.setType("codeSnippet");
    codeSnippet.setExpandable(true);

    // Act
    ContentDTO codeSnippetDTO = contentMapper.getDTOByDO(codeSnippet);

    // Assert
    assertTrue(codeSnippetDTO.getExpandable());
  }

  @Test
  void getDTOByDO_ContentDOtoDTO_ExpandableSetInDTO() {
    // Arrange
    Content content = new Content();
    content.setType("content");
    content.setExpandable(true);

    // Act
    ContentDTO contentDTO = contentMapper.getDTOByDO(content);

    // Assert
    assertTrue(contentDTO.getExpandable());
  }
}
