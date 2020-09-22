/**
 * Copyright 2014 Stephen Cummins and Nick Rogers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.cam.cl.dtg.segue.api.monitors;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.api.managers.SegueResourceMisuseException;
import uk.ac.cam.cl.dtg.segue.comm.EmailCommunicationMessage;
import uk.ac.cam.cl.dtg.segue.comm.EmailManager;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dos.users.EmailVerificationStatus;
import uk.ac.cam.cl.dtg.segue.dos.users.RegisteredUser;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.fail;

/**
 * Test class for the user manager class.
 * 
 */
@PowerMockIgnore({ "javax.ws.*" })
public class MisuseMonitorTest {
    private PropertiesLoader dummyPropertiesLoader;
    private EmailManager dummyCommunicator;

    /**
     * Initial configuration of tests.
     * 
     * @throws Exception
     *             - test exception
     */
    @Before
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
    public final void misuseMonitorTokenOwnerLookup_checkForMisuse_emailShouldBeSentAndExceptionShouldOccur() {
        String userId = "289347298428";
        String event = TokenOwnerLookupMisuseHandler.class.getSimpleName();

        IMisuseMonitor misuseMonitor = new InMemoryMisuseMonitor();
        TokenOwnerLookupMisuseHandler tokenOwnerLookupMisuseHandler = new TokenOwnerLookupMisuseHandler(
                dummyCommunicator, dummyPropertiesLoader);

        misuseMonitor.registerHandler(event, tokenOwnerLookupMisuseHandler);

        try {
			dummyCommunicator.addSystemEmailToQueue(EasyMock.isA(EmailCommunicationMessage.class));
		} catch (SegueDatabaseException e1) {
            fail("Exception should not be thrown during database email preference filtering");
		}
        expectLastCall();
        replay(this.dummyCommunicator);

        for (int i = 1; i < tokenOwnerLookupMisuseHandler.getSoftThreshold(); i++) {
            try {
                misuseMonitor.notifyEvent(userId, event);

            } catch (SegueResourceMisuseException e) {
                fail("Exception should not be thrown after " + tokenOwnerLookupMisuseHandler.getSoftThreshold()
                        + " attempts");
            }
        }

        for (int i = TokenOwnerLookupMisuseHandler.SOFT_THRESHOLD; i < TokenOwnerLookupMisuseHandler.HARD_THRESHOLD; i++) {
            try {
                misuseMonitor.notifyEvent(userId, event);
                if (i > TokenOwnerLookupMisuseHandler.HARD_THRESHOLD) {
                    fail("Exception have been thrown after " + TokenOwnerLookupMisuseHandler.HARD_THRESHOLD
                            + " attempts");
                }
            } catch (SegueResourceMisuseException e) {

            }
        }

        verify(this.dummyCommunicator, this.dummyPropertiesLoader);
    }
    
    /**
     * Verifies that the email verification misuse handler is working.
     */
    @Test
    public final void emailVerificationRequest_checkForMisuse_emailShouldBeSentAndExceptionShouldOccur() {

        String event = EmailVerificationRequestMisuseHandler.class.getSimpleName();
        
        IMisuseMonitor misuseMonitor = new InMemoryMisuseMonitor();

        EmailVerificationRequestMisuseHandler emailVerificationMisuseHandler
            = new EmailVerificationRequestMisuseHandler();
        
        misuseMonitor.registerHandler(event, emailVerificationMisuseHandler);

        
        // Create a test user
        RegisteredUser user = new RegisteredUser();
        user.setEmail("test@test.com");
        user.setEmailVerificationStatus(EmailVerificationStatus.NOT_VERIFIED);
        
        // Soft threshold
        try {
            //Register the misuse monitor
            if (misuseMonitor.hasMisused(user.getEmail(),
                    EmailVerificationRequestMisuseHandler.class.getSimpleName())) {
                throw new SegueResourceMisuseException("Number of requests exceeded. Triggering Error Response");
            }
            
            for (int i = 0; i < EmailVerificationRequestMisuseHandler.SOFT_THRESHOLD; i++) {
                misuseMonitor.notifyEvent(user.getEmail(), event);
            }
        } catch (SegueResourceMisuseException e) {
            fail();
        }   
        
        // Hard threshold
        try {
            //Register the misuse monitor
            if (misuseMonitor.hasMisused(user.getEmail(),
                    EmailVerificationRequestMisuseHandler.class.getSimpleName())) {
                throw new SegueResourceMisuseException("Number of requests exceeded. Triggering Error Response");
            }
            
            for (int i = EmailVerificationRequestMisuseHandler.SOFT_THRESHOLD;
                                        i < EmailVerificationRequestMisuseHandler.HARD_THRESHOLD; i++) {
                misuseMonitor.notifyEvent(user.getEmail(), event);
            }
        } catch (SegueResourceMisuseException e) {
            System.out.println("SegueResourceMisuseException");
        }   
    }

    /**
     * Verifies that the user search misuse handler is working.
     */
    @Test
    public final void userSearchRequest_checkForMisuse_emailShouldBeSentAndExceptionShouldOccur() {

        String event = UserSearchMisuseHandler.class.getSimpleName();

        IMisuseMonitor misuseMonitor = new InMemoryMisuseMonitor();

        UserSearchMisuseHandler userSearchMisuseHandler
                = new UserSearchMisuseHandler();

        misuseMonitor.registerHandler(event, userSearchMisuseHandler);


        // Create a test user
        RegisteredUser user = new RegisteredUser();
        user.setId(1234L);
        user.setEmailVerificationStatus(EmailVerificationStatus.NOT_VERIFIED);

        // Soft threshold
        try {
            //Register the misuse monitor
            if (misuseMonitor.hasMisused(user.getId().toString(),
                    UserSearchMisuseHandler.class.getSimpleName())) {
                throw new SegueResourceMisuseException("Number of requests exceeded. Triggering Error Response");
            }

            for (int i = 0; i < UserSearchMisuseHandler.SOFT_THRESHOLD; i++) {
                misuseMonitor.notifyEvent(user.getId().toString(), event);
            }
        } catch (SegueResourceMisuseException e) {
            fail();
        }

        // Hard threshold
        try {
            //Register the misuse monitor
            if (misuseMonitor.hasMisused(user.getId().toString(),
                    UserSearchMisuseHandler.class.getSimpleName())) {
                throw new SegueResourceMisuseException("Number of requests exceeded. Triggering Error Response");
            }

            for (int i = UserSearchMisuseHandler.SOFT_THRESHOLD;
                 i < UserSearchMisuseHandler.HARD_THRESHOLD; i++) {
                misuseMonitor.notifyEvent(user.getId().toString(), event);
            }
        } catch (SegueResourceMisuseException e) {
            System.out.println("SegueResourceMisuseException");
        }
    }

    /**
     * Verifies that the willHaveMisused method is working.
     */
    @Test
    public final void willHaveMisused() {
        String event = UserSearchMisuseHandler.class.getSimpleName();

        IMisuseMonitor misuseMonitor = new InMemoryMisuseMonitor();

        UserSearchMisuseHandler userSearchMisuseHandler
                = new UserSearchMisuseHandler();

        misuseMonitor.registerHandler(event, userSearchMisuseHandler);

        // Create a test user
        RegisteredUser user = new RegisteredUser();
        user.setId(1234L);
        user.setEmailVerificationStatus(EmailVerificationStatus.NOT_VERIFIED);

        // Soft threshold
        try {
            //Register the misuse monitor
            if (misuseMonitor.hasMisused(user.getId().toString(),
                    UserSearchMisuseHandler.class.getSimpleName())) {
                throw new SegueResourceMisuseException("Number of requests exceeded. Triggering Error Response");
            }

            for (int i = 0; i < UserSearchMisuseHandler.SOFT_THRESHOLD; i++) {
                misuseMonitor.notifyEvent(user.getId().toString(), event);
            }
        } catch (SegueResourceMisuseException e) {
            fail();
        }

        // Hard threshold
        try {
            //Register the misuse monitor
            if (misuseMonitor.hasMisused(user.getId().toString(),
                    UserSearchMisuseHandler.class.getSimpleName())) {
                throw new SegueResourceMisuseException("Number of requests exceeded. Triggering Error Response");
            }

            for (int i = UserSearchMisuseHandler.SOFT_THRESHOLD;
                 i < UserSearchMisuseHandler.HARD_THRESHOLD; i++) {
                if (misuseMonitor.willHaveMisused(user.getId().toString(), event,
                        UserSearchMisuseHandler.HARD_THRESHOLD - i - 1)) {
                    fail();
                } else if (!misuseMonitor.willHaveMisused(user.getId().toString(), event,
                        UserSearchMisuseHandler.HARD_THRESHOLD - i)) {
                    fail();
                }

                misuseMonitor.notifyEvent(user.getId().toString(), event);
            }
        } catch (SegueResourceMisuseException e) {
            System.out.println("SegueResourceMisuseException");
        }
    }
}
