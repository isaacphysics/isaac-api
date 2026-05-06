package uk.ac.cam.cl.dtg.isaac.api.managers;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.ac.cam.cl.dtg.isaac.api.services.ContentSummarizerService;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacQuizDTO;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacQuizSectionDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentDTO;
import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.api.services.ContentService;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dao.content.GitContentManager;
import uk.ac.cam.cl.dtg.util.AbstractConfigLoader;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class QuizManagerTest extends AbstractManagerTest {

    private QuizManager quizManager;
    private AbstractConfigLoader properties;
    private IsaacQuizDTO brokenQuiz;

    @BeforeEach
    public void setUp() {
        properties = mock(AbstractConfigLoader.class);

        ContentService contentService = mock(ContentService.class);
        GitContentManager contentManager = mock(GitContentManager.class);
        ContentSummarizerService contentSummarizerService = mock(ContentSummarizerService.class);
        quizManager = new QuizManager(properties, contentService, contentManager, contentSummarizerService);

        brokenQuiz = new IsaacQuizDTO();
        brokenQuiz.setChildren(ImmutableList.of(quizSection1, new ContentDTO(), quizSection2));
    }

    @Test
    public void extractSectionObjectsInDev() throws ContentManagerException {
        withMock(properties, m ->
            when(m.getProperty(Constants.SEGUE_APP_ENVIRONMENT)).thenReturn(Constants.EnvironmentType.DEV.name())
        );

        List<IsaacQuizSectionDTO> sections = quizManager.extractSectionObjects(studentQuiz);
        assertCorrectSections(sections);

        assertThrows(ContentManagerException.class, () -> quizManager.extractSectionObjects(brokenQuiz));
    }

    @Test
    public void extractSectionObjectsInProd() throws ContentManagerException {
        withMock(properties, m ->
            when(m.getProperty(Constants.SEGUE_APP_ENVIRONMENT)).thenReturn(Constants.EnvironmentType.PROD.name())
        );

        List<IsaacQuizSectionDTO> sections = quizManager.extractSectionObjects(studentQuiz);
        assertCorrectSections(sections);

        sections = quizManager.extractSectionObjects(brokenQuiz);
        assertCorrectSections(sections);
    }

    private void assertCorrectSections(final List<IsaacQuizSectionDTO> sections) {
        assertEquals(ImmutableList.of(quizSection1, quizSection2), sections);
    }
}