package uk.ac.cam.cl.dtg.segue.dao.users;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.ResultSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.ac.cam.cl.dtg.isaac.dos.users.EmailVerificationStatus;
import uk.ac.cam.cl.dtg.isaac.dos.users.Role;
import uk.ac.cam.cl.dtg.isaac.dos.users.UserExternalAccountChanges;
import uk.ac.cam.cl.dtg.util.ReflectionUtils;

class PgExternalAccountPersistenceManagerTest {

  private PgExternalAccountPersistenceManager persistenceManager;
  private ResultSet mockResultSet;

  @BeforeEach
  void setUp() {
    persistenceManager = new PgExternalAccountPersistenceManager(null);
    mockResultSet = createMock(ResultSet.class);
  }

  @Test
  void buildUserExternalAccountChanges_ShouldParseRegisteredContextsCorrectly() throws Exception {
    // Arrange
    String registeredContextsJson = "{\"stage\": \"gcse\", \"examBoard\": \"ocr\"}";

    // Mock ResultSet behavior
    expect(mockResultSet.getString("registered_contexts")).andReturn(registeredContextsJson); // Expect this call once
    expect(mockResultSet.getLong("id")).andReturn(1L);
    expect(mockResultSet.getString("provider_user_identifier")).andReturn("providerId");
    expect(mockResultSet.getString("email")).andReturn("test@example.com");
    expect(mockResultSet.getString("role")).andReturn("STUDENT");
    expect(mockResultSet.getString("given_name")).andReturn("John");
    expect(mockResultSet.getBoolean("deleted")).andReturn(false);
    expect(mockResultSet.getString("email_verification_status")).andReturn("VERIFIED");
    expect(mockResultSet.getBoolean("news_emails")).andReturn(true);
    expect(mockResultSet.getBoolean("events_emails")).andReturn(false);

    replay(mockResultSet);

    // Act
    UserExternalAccountChanges result = ReflectionUtils.invokePrivateMethod(
        persistenceManager,
        "buildUserExternalAccountChanges",
        new Class[]{ResultSet.class},
        new Object[]{mockResultSet}
    );

    // Assert
    verify(mockResultSet); // Verify all expected calls were made
    assertNotNull(result);
    assertEquals(1L, result.getUserId());
    assertEquals("providerId", result.getProviderUserId());
    assertEquals("test@example.com", result.getAccountEmail());
    assertEquals(Role.STUDENT, result.getRole());
    assertEquals("John", result.getGivenName());
    assertFalse(result.isDeleted());
    assertEquals(EmailVerificationStatus.VERIFIED, result.getEmailVerificationStatus());
    assertTrue(result.allowsNewsEmails());
    assertFalse(result.allowsEventsEmails());

    // Verify JSON parsing and stage extraction
    assertEquals("gcse", result.getStage()); // Ensure "stage" is correctly extracted from JSON
  }
}