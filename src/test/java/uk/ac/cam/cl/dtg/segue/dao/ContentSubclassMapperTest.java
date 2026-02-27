package uk.ac.cam.cl.dtg.segue.dao;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.reflections.Reflections;
import uk.ac.cam.cl.dtg.isaac.dos.content.CodeSnippet;
import uk.ac.cam.cl.dtg.isaac.dos.content.Content;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentDTO;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentSubclassMapper;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class ContentSubclassMapperTest {

    private ContentSubclassMapper contentSubclassMapper;

    @BeforeEach
    public void setUp() {
        this.contentSubclassMapper = new ContentSubclassMapper(new Reflections("uk.ac.cam.cl.dtg.isaac"));
    }

    @Test
    public void getDTOByDO_CodeSnippetDOtoDTO_ExpandableSetInDTO() {
        // Arrange
        CodeSnippet codeSnippet = new CodeSnippet();
        codeSnippet.setType("codeSnippet");
        codeSnippet.setExpandable(true);

        // Act
        ContentDTO codeSnippetDTO = contentSubclassMapper.getDTOByDO(codeSnippet);

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
        ContentDTO contentDTO = contentSubclassMapper.getDTOByDO(content);

        // Assert
        assertTrue(contentDTO.getExpandable());
    }
}
