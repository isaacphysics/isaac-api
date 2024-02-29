package uk.ac.cam.cl.dtg.isaac.api;

import static org.easymock.EasyMock.createMock;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.QUIZ_TEST_FIRST_QUESTION_ANSWER;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.QUIZ_TEST_HIDDEN_FROM_TUTORS_QUESTION_FIRST_ID;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.ac.cam.cl.dtg.isaac.dos.IUserStreaksManager;
import uk.ac.cam.cl.dtg.isaac.dos.PgUserStreakManager;
import uk.ac.cam.cl.dtg.segue.api.QuestionFacade;

class QuestionFacadeIT extends IsaacIntegrationTest {

  private QuestionFacade questionFacade;
  private static final String contentIndex = "4b825dc642cb6eb9a060e54bf8d69288fbee4904";

  @BeforeEach
  public void beforeEach() {
    IUserStreaksManager userStreaksManager = createMock(PgUserStreakManager.class);
    questionFacade =
        new QuestionFacade(properties, contentMapperUtils, contentManager, gameManager, contentIndex, userAccountManager, questionManager,
            logManager, misuseMonitor, userBadgeManager, userStreaksManager, userAssociationManager);
  }

  @Test
  void answerQuestionNotAvailableForQuizQuestions() {
    HttpServletRequest mockRequest = createMock(HttpServletRequest.class);

    try (Response response = questionFacade.answerQuestion(mockRequest, QUIZ_TEST_HIDDEN_FROM_TUTORS_QUESTION_FIRST_ID,
        QUIZ_TEST_FIRST_QUESTION_ANSWER)) {

      assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
    }
  }
}
