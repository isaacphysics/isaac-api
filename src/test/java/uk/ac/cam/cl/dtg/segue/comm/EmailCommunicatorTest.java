/**
 * Copyright 2015 Alistair Stead
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
package uk.ac.cam.cl.dtg.segue.comm;

import java.util.ArrayList;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.dtg.isaac.api.APIOverviewResource;
import uk.ac.cam.cl.dtg.segue.dos.content.ContentBase;
import uk.ac.cam.cl.dtg.segue.dos.content.SeguePage;

/**
 * Test class for the user manager class.
 * 
 */
public class EmailCommunicatorTest {
    private EmailCommunicator emailCommunicator;
    private SeguePage seguePage;
    private static final Logger log = LoggerFactory.getLogger(APIOverviewResource.class);

    /**
     * Fields for the test.
     * 
     * @author Alistair Stead
     *
     */
    class EmailParams {
        static final String RECIPIENT_ADDRESS = "";
        static final String RECIPIENT_NAME = "";
        static final String SUBJECT = "";
        static final String MESSAGE = "";
    }

    /**
     * Initial configuration of tests.
     * 
     * @throws Exception
     *             - test exception
     */
    @Before
    public final void setUp() throws Exception {

        String content = "Hi, {{user}}.\nThanks for registering!\nYour Isaac email address is: "
                + "</a href='mailto:{{email}}'>{{email}}<a>.\n" + "address</a>\n{{sig}}";

        ArrayList<ContentBase> children = new ArrayList<ContentBase>();

        SeguePage child = new SeguePage(null, null, "content", null, null, null, null, null, null, null, content, null,
                null, null, null, 0);

        children.add(child);

        seguePage = new SeguePage("someid", "subtitle", "page", "ags46", "markdown", "canonical-source-file", null,
                null, null, children, null, null, null, false, null, 0);

        emailCommunicator = new EmailCommunicator("ppsw.cam.ac.uk", "cl-isaac-contact@lists.cam.ac.uk");



    }

    /**
     * Verify that the correct type of email is created.
     * 
     * @throws CommunicationException
     */
    @Test
    public final void testEmailConstruction() {
        // assertTrue(this.dummyMailer instanceof Mailer.class);

        // RegistrationConfirmation rc = new RegistrationConfirmation(emailCommunicator, seguePage, "alistair.stead",
        // "alistair.stead@gmail.com");

    }
}
