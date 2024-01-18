/**
 * Copyright 2014 Stephen Cummins and Nick Rogers
 * <br>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * <br>
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * <br>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.ac.cam.cl.dtg.segue.api.monitors;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.easymock.EasyMock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.api.managers.SegueResourceMisuseException;
import uk.ac.cam.cl.dtg.segue.comm.EmailCommunicationMessage;
import uk.ac.cam.cl.dtg.segue.comm.EmailManager;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

/**
 * Test class for the user manager class.
 */
class MisuseMonitorTest {
  private PropertiesLoader dummyPropertiesLoader;
  private EmailManager dummyCommunicator;

  /**
   * Initial configuration of tests.
   *
   * @throws Exception - test exception
   */
  @BeforeEach
  public final void setUp() throws Exception {
    this.dummyCommunicator = createMock(EmailManager.class);
    this.dummyPropertiesLoader = createMock(PropertiesLoader.class);


    expect(dummyPropertiesLoader.getProperty(Constants.SERVER_ADMIN_ADDRESS)).andReturn("FROM ADDRESS").anyTimes();
    replay(this.dummyPropertiesLoader);
  }

  /**
   * Verify that the misusehandler detects misuse.
   */
  @Test
  final void misuseMonitorTokenOwnerLookup_checkForMisuse_emailShouldBeSentAndExceptionShouldOccur() {
    String userId = "289347298428";
    String event = TokenOwnerLookupMisuseHandler.class.getSimpleName();

    IMisuseMonitor misuseMonitor = new InMemoryMisuseMonitor();
    TokenOwnerLookupMisuseHandler realTokenOwnerLookupMisuseHandler =
        new TokenOwnerLookupMisuseHandler(dummyCommunicator, dummyPropertiesLoader);

    TokenOwnerLookupMisuseHandler mockHandler = createMock(TokenOwnerLookupMisuseHandler.class);
    expect(mockHandler.getAccountingIntervalInSeconds()).andDelegateTo(realTokenOwnerLookupMisuseHandler).atLeastOnce();
    expect(mockHandler.getSoftThreshold()).andDelegateTo(realTokenOwnerLookupMisuseHandler).atLeastOnce();
    expect(mockHandler.getHardThreshold()).andDelegateTo(realTokenOwnerLookupMisuseHandler).atLeastOnce();
    mockHandler.executeSoftThresholdAction(String.format("(%s) has exceeded the soft limit!", userId));
    expectLastCall().andDelegateTo(realTokenOwnerLookupMisuseHandler);
    mockHandler.executeHardThresholdAction(String.format("(%s) has exceeded the hard limit!", userId));
    expectLastCall().andDelegateTo(realTokenOwnerLookupMisuseHandler);
    replay(mockHandler);

    misuseMonitor.registerHandler(event, mockHandler);

    dummyCommunicator.addSystemEmailToQueue(EasyMock.isA(EmailCommunicationMessage.class));
    expectLastCall();
    replay(this.dummyCommunicator);

    for (int i = 0; i < realTokenOwnerLookupMisuseHandler.getSoftThreshold(); i++) {
      assertDoesNotThrow(() -> misuseMonitor.notifyEvent(userId, event),
          String.format("Exception should not be thrown at %d attempts, under soft threshold of %d", i,
              realTokenOwnerLookupMisuseHandler.getSoftThreshold()));
    }

    for (int i = realTokenOwnerLookupMisuseHandler.getSoftThreshold();
         i < realTokenOwnerLookupMisuseHandler.getHardThreshold(); i++) {
      assertDoesNotThrow(() -> misuseMonitor.notifyEvent(userId, event),
          String.format("Exception should not be thrown at %d attempts, under hard threshold of %d", i,
              realTokenOwnerLookupMisuseHandler.getHardThreshold()));
    }

    assertThrows(SegueResourceMisuseException.class, () -> misuseMonitor.notifyEvent(userId, event),
        String.format("SegueResourceMisuseException should be thrown as the hard threshold of %d has been reached",
            realTokenOwnerLookupMisuseHandler.getHardThreshold()));

    verify(this.dummyCommunicator, this.dummyPropertiesLoader, mockHandler);
  }

  /**
   * Verifies that the email verification misuse handler is working.
   */
  @Test
  final void emailVerificationRequest_checkForMisuse_emailShouldBeSentAndExceptionShouldOccur() {
    String userEmail = "test@test.com";
    String event = EmailVerificationRequestMisuseHandler.class.getSimpleName();

    IMisuseMonitor misuseMonitor = new InMemoryMisuseMonitor();
    EmailVerificationRequestMisuseHandler realEmailVerificationMisuseHandler =
        new EmailVerificationRequestMisuseHandler();

    EmailVerificationRequestMisuseHandler mockHandler = createMock(EmailVerificationRequestMisuseHandler.class);
    expect(mockHandler.getAccountingIntervalInSeconds()).andDelegateTo(realEmailVerificationMisuseHandler)
        .atLeastOnce();
    expect(mockHandler.getSoftThreshold()).andDelegateTo(realEmailVerificationMisuseHandler).atLeastOnce();
    expect(mockHandler.getHardThreshold()).andDelegateTo(realEmailVerificationMisuseHandler).atLeastOnce();
    mockHandler.executeSoftThresholdAction(String.format("(%s) has exceeded the soft limit!", userEmail));
    expectLastCall().andDelegateTo(realEmailVerificationMisuseHandler);
    mockHandler.executeHardThresholdAction(String.format("(%s) has exceeded the hard limit!", userEmail));
    expectLastCall().andDelegateTo(realEmailVerificationMisuseHandler);
    replay(mockHandler);

    misuseMonitor.registerHandler(event, mockHandler);

    // Soft threshold
    for (int i = 0; i < realEmailVerificationMisuseHandler.getSoftThreshold(); i++) {
      assertDoesNotThrow(() -> misuseMonitor.notifyEvent(userEmail, event),
          String.format("Exception should not be thrown at %d attempts, under soft threshold of %d", i,
              realEmailVerificationMisuseHandler.getSoftThreshold()));
    }

    // Hard threshold
    for (int i = realEmailVerificationMisuseHandler.getSoftThreshold();
         i < realEmailVerificationMisuseHandler.getHardThreshold(); i++) {
      assertDoesNotThrow(() -> misuseMonitor.notifyEvent(userEmail, event),
          String.format("Exception should not be thrown at %d attempts, under hard threshold of %d", i,
              realEmailVerificationMisuseHandler.getHardThreshold()));
    }

    assertThrows(SegueResourceMisuseException.class, () -> misuseMonitor.notifyEvent(userEmail, event),
        String.format("SegueResourceMisuseException should be thrown as the hard threshold of %d has been reached",
            realEmailVerificationMisuseHandler.getHardThreshold()));

    verify(mockHandler);
  }

  /**
   * Verifies that the user search misuse handler is working.
   */
  @Test
  final void userSearchRequest_checkForMisuse_emailShouldBeSentAndExceptionShouldOccur() {
    String userId = String.valueOf(1234L);
    String event = UserSearchMisuseHandler.class.getSimpleName();

    IMisuseMonitor misuseMonitor = new InMemoryMisuseMonitor();
    UserSearchMisuseHandler realUserSearchMisuseHandler = new UserSearchMisuseHandler();

    UserSearchMisuseHandler mockHandler = createMock(UserSearchMisuseHandler.class);
    expect(mockHandler.getAccountingIntervalInSeconds()).andDelegateTo(realUserSearchMisuseHandler)
        .atLeastOnce();
    expect(mockHandler.getSoftThreshold()).andDelegateTo(realUserSearchMisuseHandler).atLeastOnce();
    expect(mockHandler.getHardThreshold()).andDelegateTo(realUserSearchMisuseHandler).atLeastOnce();
    mockHandler.executeSoftThresholdAction(String.format("(%s) has exceeded the soft limit!", userId));
    expectLastCall().andDelegateTo(realUserSearchMisuseHandler);
    mockHandler.executeHardThresholdAction(String.format("(%s) has exceeded the hard limit!", userId));
    expectLastCall().andDelegateTo(realUserSearchMisuseHandler);
    replay(mockHandler);

    misuseMonitor.registerHandler(event, mockHandler);

    // Soft threshold
    for (int i = 0; i < realUserSearchMisuseHandler.getSoftThreshold(); i++) {
      assertDoesNotThrow(() -> misuseMonitor.notifyEvent(userId, event),
          String.format("Exception should not be thrown at %d attempts, under soft threshold of %d", i,
              realUserSearchMisuseHandler.getSoftThreshold()));
    }

    // Hard threshold
    for (int i = realUserSearchMisuseHandler.getSoftThreshold(); i < realUserSearchMisuseHandler.getHardThreshold();
         i++) {
      assertDoesNotThrow(() -> misuseMonitor.notifyEvent(userId, event),
          String.format("Exception should not be thrown at %d attempts, under hard threshold of %d", i,
              realUserSearchMisuseHandler.getHardThreshold()));
    }

    assertThrows(SegueResourceMisuseException.class, () -> misuseMonitor.notifyEvent(userId, event),
        String.format("SegueResourceMisuseException should be thrown as the hard threshold of %d has been reached",
            realUserSearchMisuseHandler.getHardThreshold()));
  }

  /**
   * Verifies that the willHaveMisused method is working.
   */
  @Test
  final void willHaveMisused() {
    String userId = String.valueOf(1234L);
    String event = UserSearchMisuseHandler.class.getSimpleName();

    IMisuseMonitor misuseMonitor = new InMemoryMisuseMonitor();
    UserSearchMisuseHandler userSearchMisuseHandler = new UserSearchMisuseHandler();

    misuseMonitor.registerHandler(event, userSearchMisuseHandler);

    // Soft threshold
    for (int i = 0; i < userSearchMisuseHandler.getSoftThreshold(); i++) {
      assertDoesNotThrow(() -> misuseMonitor.notifyEvent(userId, event),
          String.format("Exception should not be thrown at %d attempts, under soft threshold of %d", i,
              userSearchMisuseHandler.getSoftThreshold()));
    }

    // Hard threshold
    for (int i = userSearchMisuseHandler.getSoftThreshold(); i < userSearchMisuseHandler.getHardThreshold(); i++) {
      assertFalse(misuseMonitor.willHaveMisused(userId, event,
          UserSearchMisuseHandler.HARD_THRESHOLD - i - 1));
      assertTrue(
          misuseMonitor.willHaveMisused(userId, event, UserSearchMisuseHandler.HARD_THRESHOLD - i));
      assertDoesNotThrow(() -> misuseMonitor.notifyEvent(userId, event),
          String.format("Exception should not be thrown at %d attempts, under hard threshold of %d", i,
              userSearchMisuseHandler.getHardThreshold()));
    }

    assertThrows(SegueResourceMisuseException.class, () -> misuseMonitor.notifyEvent(userId, event),
        String.format("SegueResourceMisuseException should be thrown as the hard threshold of %d has been reached",
            userSearchMisuseHandler.getHardThreshold()));
  }
}
