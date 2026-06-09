package uk.ac.cam.cl.dtg.isaac.api;

import org.junit.jupiter.api.Test;
import uk.ac.cam.cl.dtg.segue.api.AuthenticationFacade;

import jakarta.ws.rs.core.Response;

@SuppressWarnings("checkstyle:MissingJavadocType")
public class SkillsQuestionFacadeIT extends IsaacIntegrationTestWithREST {
    @Test
    public void notLoggedIn_Returns401() throws Exception {
        var response = testServer().client().post("/skills-questions/some_app/answer", "{}");
        response.assertError("You must be logged in to access this resource.", Response.Status.UNAUTHORIZED);
    }

//    @Test
//    public void loggedIn_unknownApp_Returns404() throws Exception {
//        var client = testServer().client();
//        client.loginAs(integrationTestUsers.TEST_STUDENT);
//        var response = client.post("/skills-questions/unknown_app/answer", "{}");
//        response.assertError("No app found for app ID: unknown_app", Response.Status.NOT_FOUND);
//    }

    private TestServer testServer() throws Exception {
        return startServer(
            new AuthenticationFacade(properties, userAccountManager, logManager, misuseMonitor),
            new SkillsQuestionFacade(properties, userAccountManager, logManager)
        );
    }
}
