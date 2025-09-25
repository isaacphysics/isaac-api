package uk.ac.cam.cl.dtg.segue.dao;

import org.junit.Before;
import org.junit.Test;
import org.reflections.Reflections;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentMapperUtils;
import uk.ac.cam.cl.dtg.isaac.dos.content.CodeSnippet;
import uk.ac.cam.cl.dtg.isaac.dos.content.Content;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentDTO;

import static org.junit.Assert.assertTrue;

public class ContentMapperTest {

    private ContentMapperUtils contentMapperUtils;

    @Before
    public void setUp() {
        this.contentMapperUtils = new ContentMapperUtils(new Reflections("uk.ac.cam.cl.dtg.isaac"));
    }

    @Test
    public void getDTOByDO_CodeSnippetDOtoDTO_ExpandableSetInDTO() {
        // Arrange
        CodeSnippet codeSnippet = new CodeSnippet();
        codeSnippet.setType("codeSnippet");
        codeSnippet.setExpandable(true);

        // Act
        ContentDTO codeSnippetDTO = contentMapperUtils.getDTOByDO(codeSnippet);

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
        ContentDTO contentDTO = contentMapperUtils.getDTOByDO(content);

        // Assert
        assertTrue(contentDTO.getExpandable());
    }
}
