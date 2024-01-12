package uk.ac.cam.cl.dtg.segue.dao;

import static org.junit.Assert.assertTrue;
import static uk.ac.cam.cl.dtg.util.ReflectionUtils.getClasses;

import org.junit.Before;
import org.junit.Test;
import uk.ac.cam.cl.dtg.isaac.dos.content.CodeSnippet;
import uk.ac.cam.cl.dtg.isaac.dos.content.Content;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentDTO;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentMapper;

public class ContentMapperTest {

  private ContentMapper contentMapper;

  @Before
  public void setUp() {
    this.contentMapper = new ContentMapper(getClasses("uk.ac.cam.cl.dtg.isaac"));
  }

  @Test
  public void getDTOByDO_CodeSnippetDOtoDTO_ExpandableSetInDTO() {
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
  public void getDTOByDO_ContentDOtoDTO_ExpandableSetInDTO() {
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
