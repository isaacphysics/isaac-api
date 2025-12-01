package uk.ac.cam.cl.dtg.isaac.api;

import org.junit.jupiter.api.Test;
import uk.ac.cam.cl.dtg.isaac.dto.GameFilter;
import uk.ac.cam.cl.dtg.isaac.dto.GameboardDTO;
import uk.ac.cam.cl.dtg.isaac.dto.GameboardItem;
import uk.ac.cam.cl.dtg.segue.api.AuthenticationFacade;

import jakarta.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.ASSIGNMENT_TEST_PAGE_ID;

public class GameboardsFacadeIT extends IsaacIntegrationTestWithREST {
    TestServer subject() throws Exception {
        return startServer(
            new AuthenticationFacade(properties, userAccountManager, logManager, misuseMonitor),
            new GameboardsFacade(properties, logManager, gameManager, questionManager, userAccountManager,
                    fastTrackManger)
        );
    }

    @Test
    public void saveNewGameboard_validGameboard_isAccepted() throws Exception {
        // Arrange
        GameboardDTO gameboardDTO = new GameboardDTO();
        gameboardDTO.setTitle("Test Gameboard");

        // Create gameboard
        GameFilter gameFilter = new GameFilter();
        List<String> subjects = new ArrayList<>();
        subjects.add("physics");
        gameFilter.setSubjects(subjects);
        gameboardDTO.setGameFilter(gameFilter);

        // Add 30 questions
        List<GameboardItem> questions = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            GameboardItem item = new GameboardItem();
            item.setId(ASSIGNMENT_TEST_PAGE_ID);
            item.setTitle(ASSIGNMENT_TEST_PAGE_ID);
            questions.add(item);
        }
        gameboardDTO.setContents(questions);

        TestClient client = subject().client();

        // Log in as student
        client.loginAs(integrationTestUsers.TEST_STUDENT);

        // Act
        TestResponse r = client.post("/gameboards",  gameboardDTO);

        // Assert
        assertEquals(200, r.response.getStatus());
    }

    @Test
    public void saveNewGameboard_tooManyQuestions_isRejected() throws Exception {
        // Arrange
        GameboardDTO gameboardDTO = new GameboardDTO();
        gameboardDTO.setTitle("Test Gameboard");

        // Create an otherwise valid gameboard
        GameFilter gameFilter = new GameFilter();
        List<String> subjects = new ArrayList<>();
        subjects.add("physics");
        gameFilter.setSubjects(subjects);
        gameboardDTO.setGameFilter(gameFilter);

        // Add over 30 questions
        List<GameboardItem> questions = new ArrayList<>();
        for (int i = 0; i < 31; i++) {
            GameboardItem item = new GameboardItem();
            item.setId(ASSIGNMENT_TEST_PAGE_ID);
            item.setTitle(ASSIGNMENT_TEST_PAGE_ID);
            questions.add(item);
        }
        gameboardDTO.setContents(questions);

        TestClient client = subject().client();

        // Log in as student
        client.loginAs(integrationTestUsers.TEST_STUDENT);

        // Act
        TestResponse r = client.post("/gameboards", gameboardDTO);

        // Assert
        r.assertError("The gameboard you provided is invalid", Response.Status.BAD_REQUEST);
    }
}
