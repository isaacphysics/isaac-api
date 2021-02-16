package uk.ac.cam.cl.dtg.isaac.api.managers;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import uk.ac.cam.cl.dtg.isaac.api.services.ContentSummarizerService;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacQuizDTO;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacQuizSectionDTO;
import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.api.services.ContentService;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentMapper;
import uk.ac.cam.cl.dtg.segue.dao.content.IContentManager;
import uk.ac.cam.cl.dtg.segue.dto.content.ContentDTO;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

import java.util.List;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.replayAll;

public class QuizManagerTest extends AbstractManagerTest {

    private QuizManager quizManager;
    private PropertiesLoader properties;
    private IsaacQuizDTO brokenQuiz;

    @Before
    public void setUp() {
        properties = createMock(PropertiesLoader.class);

        ContentService contentService = createMock(ContentService.class);
        IContentManager contentManager = createMock(IContentManager.class);
        ContentSummarizerService contentSummarizerService = createMock(ContentSummarizerService.class);
        ContentMapper mapper = createMock(ContentMapper.class);
        String contentIndex = "4b825dc642cb6eb9a060e54bf8d69288fbee4904";
        quizManager = new QuizManager(properties, contentService, contentManager, contentSummarizerService, mapper, contentIndex);

        brokenQuiz = new IsaacQuizDTO();
        brokenQuiz.setChildren(ImmutableList.of(quizSection1, new ContentDTO(), quizSection2));

        replayAll();
    }

    @Test
    public void extractSectionObjectsInDev() throws ContentManagerException {
        withMock(properties, m ->
            expect(m.getProperty(Constants.SEGUE_APP_ENVIRONMENT)).andStubReturn(Constants.EnvironmentType.DEV.name())
        );

        List<IsaacQuizSectionDTO> sections = quizManager.extractSectionObjects(studentQuiz);
        assertCorrectSections(sections);

        assertThrows(ContentManagerException.class, () -> quizManager.extractSectionObjects(brokenQuiz));
    }

    @Test
    public void extractSectionObjectsInProd() throws ContentManagerException {
        withMock(properties, m ->
            expect(m.getProperty(Constants.SEGUE_APP_ENVIRONMENT)).andStubReturn(Constants.EnvironmentType.PROD.name())
        );

        List<IsaacQuizSectionDTO> sections = quizManager.extractSectionObjects(studentQuiz);
        assertCorrectSections(sections);

        sections = quizManager.extractSectionObjects(brokenQuiz);
        assertCorrectSections(sections);
    }

    private void assertCorrectSections(List<IsaacQuizSectionDTO> sections) {
        assertEquals(ImmutableList.of(quizSection1, quizSection2), sections);
    }
}