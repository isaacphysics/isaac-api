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

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Properties;

import org.junit.Before;
import org.junit.Test;

import uk.ac.cam.cl.dtg.segue.dos.content.ContentBase;
import uk.ac.cam.cl.dtg.segue.dos.content.SeguePage;

/**
 * Class to test the template replacement helper.
 *
 * @author Alistair Stead
 *
 */
public class EmailTemplateParserTest {
    /**
     * Do some set up.
     * 
     * @throws Exception
     *             - test exception
     */
    @Before
    public final void setUp() throws Exception {

    }

    /**
     * Verify that the email template parser works.
     */
    @Test
    public final void testEmailTemplateParser() {

        String content = "Hi, {{user}}.\nThanks for registering!\nYour Isaac email address is: "
                + "</a href='mailto:{{email}}'>{{email}}<a>.\n" + "address</a>\n{{sig}}";

        ArrayList<ContentBase> children = new ArrayList<ContentBase>();

        SeguePage child = new SeguePage(null, null, "content", null, null, null, null, null, null, null, content, null,
                null, null, null, 0);

        children.add(child);

        SeguePage seguePage = new SeguePage("someid", "subtitle", "page", "ags46", "markdown", "canonical-source-file",
                null, null, null, children, null, null, null, false, null, 0);

        Properties p = new Properties();
        p.put("user", "Testy McTest");
        p.put("email", "test.test@gmail.com");
        p.put("sig", "Isaac Physics Team");

        String result = EmailTemplateParser.completeTemplateWithProperties(seguePage, p);

        assertEquals("Hi, Testy McTest.\nThanks for registering!\nYour Isaac email address is: "
                + "</a href='mailto:test.test@gmail.com'>test.test@gmail.com<a>..\n"
                + "address</a>\nIsaac Physics Team", result);
    }

    /**
     * Verify that the email template parser works.
     */
    @Test(expected = IllegalArgumentException.class)
    public final void testEmailTemplateNotEnoughProperties() {

        String content = "Hi, {{user}}.\nThanks for registering!\nYour Isaac email address is: "
                + "</a href='mailto:{{email}}'>{{email}}<a>.\n" + "address</a>\n{{sig}} {{test}}";

        ArrayList<ContentBase> children = new ArrayList<ContentBase>();

        SeguePage child = new SeguePage(null, null, "content", null, null, null, null, null, null, null, content, null,
                null, null, null, 0);

        children.add(child);

        SeguePage seguePage = new SeguePage("someid", "subtitle", "page", "ags46", "markdown", "canonical-source-file",
                null, null, null, children, null, null, null, false, null, 0);

        Properties p = new Properties();
        p.put("user", "Testy McTest");
        p.put("email", "test.test@gmail.com");
        p.put("sig", "Isaac Physics Team");

        EmailTemplateParser.completeTemplateWithProperties(seguePage, p);
    }
}
