package uk.ac.cam.cl.dtg.segue.util.email;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.mailjet.client.MailjetClient;
import com.mailjet.client.MailjetRequest;
import com.mailjet.client.MailjetResponse;
import com.mailjet.client.errors.MailjetException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.ac.cam.cl.dtg.util.email.MailJetApiClientWrapper;

class MailJetApiClientWrapperTest {

  private MailJetApiClientWrapper mailJetApiClientWrapper;
  private MailjetClient mockMailjetClient;

  @BeforeEach
  void setUp() {
    mockMailjetClient = createMock(MailjetClient.class);

    mailJetApiClientWrapper = new MailJetApiClientWrapper(
        "apiKey", "apiSecret", "newsListId", "eventsListId", "legalListId"
    );
    injectMockMailjetClient(mailJetApiClientWrapper, mockMailjetClient);
  }

  @Test
  void getAccountByIdOrEmail_WithValidInput_ShouldReturnAccount() throws MailjetException {
    // Arrange
    String mailjetId = "123";
    MailjetResponse mockResponse = createMock(MailjetResponse.class);
    JSONArray mockData = new JSONArray();
    JSONObject mockAccount = new JSONObject();
    mockAccount.put("ID", 123);
    mockData.put(mockAccount);

    expect(mockMailjetClient.get(anyObject(MailjetRequest.class))).andReturn(mockResponse);
    expect(mockResponse.getTotal()).andReturn(1);
    expect(mockResponse.getData()).andReturn(mockData);

    replay(mockMailjetClient, mockResponse);

    // Act
    JSONObject result = mailJetApiClientWrapper.getAccountByIdOrEmail(mailjetId);

    // Assert
    verify(mockMailjetClient, mockResponse);
    assertNotNull(result);
    assertEquals(123, result.getInt("ID"));
  }

  @Test
  void addNewUserOrGetUserIfExists_WithNewEmail_ShouldReturnNewId() throws MailjetException {
    // Arrange
    String email = "test@example.com";
    MailjetResponse mockResponse = createMock(MailjetResponse.class);
    JSONArray mockData = new JSONArray();
    JSONObject mockUser = new JSONObject();
    mockUser.put("ID", 456);
    mockData.put(mockUser);

    expect(mockMailjetClient.post(anyObject(MailjetRequest.class))).andReturn(mockResponse);
    expect(mockResponse.getData()).andReturn(mockData);

    replay(mockMailjetClient, mockResponse);

    // Act
    String result = mailJetApiClientWrapper.addNewUserOrGetUserIfExists(email);

    // Assert
    verify(mockMailjetClient, mockResponse);
    assertEquals("456", result);
  }

  @Test
  void addNewUserOrGetUserIfExists_WithNullEmail_ShouldReturnNull() throws MailjetException {
    assertNull(mailJetApiClientWrapper.addNewUserOrGetUserIfExists(null));
  }

  private void injectMockMailjetClient(MailJetApiClientWrapper wrapper, MailjetClient client) {
    try {
      var field = MailJetApiClientWrapper.class.getDeclaredField("mailjetClient");
      field.setAccessible(true);
      field.set(wrapper, client);
      field.setAccessible(false);
    } catch (NoSuchFieldException | IllegalAccessException e) {
      throw new RuntimeException("Failed to inject mocked Mailjet client", e);
    }
  }
}
