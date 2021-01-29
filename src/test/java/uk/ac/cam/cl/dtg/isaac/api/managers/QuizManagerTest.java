/*
 * Copyright 2021 Raspberry Pi Foundation
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
package uk.ac.cam.cl.dtg.isaac.api.managers;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Names;
import ma.glasnost.orika.MapperFacade;
import org.junit.Before;
import org.junit.Test;
import uk.ac.cam.cl.dtg.isaac.configuration.SegueConfigurationModule;
import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.configuration.ISegueDTOConfigurationModule;
import uk.ac.cam.cl.dtg.segue.configuration.SegueGuiceConfigurationModule;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentMapper;
import uk.ac.cam.cl.dtg.segue.dao.content.IContentManager;
import uk.ac.cam.cl.dtg.segue.dto.ResultsWrapper;
import uk.ac.cam.cl.dtg.segue.dto.content.ContentDTO;
import uk.ac.cam.cl.dtg.segue.dto.content.ContentSummaryDTO;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static uk.ac.cam.cl.dtg.segue.api.Constants.CONTENT_INDEX;
import static uk.ac.cam.cl.dtg.segue.configuration.SegueGuiceConfigurationModule.getReflectionsClass;

/**
 * QuizManagerTest.
 */
public class QuizManagerTest {

    private IContentManager contentManager;
    private QuizManager quizManager;
    private List<ContentDTO> studentQuiz;
    private Injector injector;

    private ContentDTO studentQuizDTO() {
        ContentMapper mapper = injector.getInstance(ContentMapper.class);
        try {
            return mapper.getDTOByDO(mapper.load("{\"type\": \"isaacQuiz\", \"id\": \"student_quiz\", \"title\": \"Student Quiz\"}"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private PropertiesLoader mockPropertiesLoader() {
        PropertiesLoader loader = createNiceMock(PropertiesLoader.class);
        final int[] iNeedClosure = new int[]{0};
        expect(loader.getProperty(anyString())).andStubAnswer(() -> "prop" + iNeedClosure[0]++);
        replay(loader);
        return loader;
    }

    class TestModule extends AbstractModule {
        /**
         * Creates a new isaac guice configuration module.
         */
        TestModule() {

        }

        @Override
        protected void configure() {
            // Properties loader
            PropertiesLoader globalProperties = mockPropertiesLoader();
            bind(PropertiesLoader.class).toInstance(globalProperties);

            this.bindConstantToProperty(Constants.SEARCH_CLUSTER_NAME, globalProperties);
            this.bindConstantToProperty(Constants.SEARCH_CLUSTER_ADDRESS, globalProperties);
            this.bindConstantToProperty(Constants.SEARCH_CLUSTER_PORT, globalProperties);

            this.bindConstantToProperty(Constants.HOST_NAME, globalProperties);
            this.bindConstantToProperty(Constants.MAILER_SMTP_SERVER, globalProperties);
            this.bindConstantToProperty(Constants.MAIL_FROM_ADDRESS, globalProperties);
            this.bindConstantToProperty(Constants.MAIL_NAME, globalProperties);
            this.bindConstantToProperty(Constants.SERVER_ADMIN_ADDRESS, globalProperties);

            this.bindConstantToProperty(Constants.LOGGING_ENABLED, globalProperties);

            // IP address geocoding
            this.bindConstantToProperty(Constants.IP_INFO_DB_API_KEY, globalProperties);

            this.bindConstantToProperty(Constants.SCHOOL_CSV_LIST_PATH, globalProperties);

            this.bindConstantToProperty(CONTENT_INDEX, globalProperties);

            this.bindConstantToProperty(Constants.API_METRICS_EXPORT_PORT, globalProperties);

            bind(ISegueDTOConfigurationModule.class).toInstance(new SegueConfigurationModule());

            contentManager = createMock(IContentManager.class);
            bind(IContentManager.class).toInstance(contentManager);

        }
        private void bindConstantToProperty(final String propertyLabel, final PropertiesLoader propertyLoader) {
            bindConstant().annotatedWith(Names.named(propertyLabel)).to(propertyLoader.getProperty(propertyLabel));
        }
        /**
         * This provides a singleton of the contentVersionController for the segue facade.
         * Note: This is a singleton because this content mapper has to use reflection to register all content classes.
         *
         * @return Content version controller with associated dependencies.
         */
        @Inject
        @Provides
        @Singleton
        private ContentMapper getContentMapper() {
            return new ContentMapper(getReflectionsClass("uk.ac.cam.cl.dtg.segue"));
        }
        /**
         * Gets the instance of the dozer mapper object.
         *
         * @return a preconfigured instance of an Auto Mapper. This is specialised for mapping SegueObjects.
         */
        @Provides
        @Singleton
        @Inject
        public MapperFacade getDOtoDTOMapper() {
            return getContentMapper().getAutoMapper();
        }


    }

    /**
     * Initial configuration of tests.
     */
    @Before
    public final void setUp() throws Exception {
        PropertiesLoader properties = mockPropertiesLoader();
        Field globalPropertiesField = SegueGuiceConfigurationModule.class.getDeclaredField("globalProperties");
        globalPropertiesField.setAccessible(true);
        globalPropertiesField.set(null, properties);

        injector = Guice.createInjector(new TestModule());

        SegueConfigurationModule segueConfigurationModule = injector.getInstance(SegueConfigurationModule.class);
        ContentMapper mapper = injector.getInstance(ContentMapper.class);
        if (segueConfigurationModule != null) {
            mapper.registerJsonTypes(segueConfigurationModule.getContentDataTransferObjectMap());
        }

        quizManager = injector.getInstance(QuizManager.class);

        studentQuiz = Collections.singletonList(studentQuizDTO());
    }

    @Test
    public void getAvailableQuizzesForStudents() throws ContentManagerException {
        // Set up
        expect(contentManager.findByFieldNames(anyString(), anyObject(), anyObject(), anyObject())).andReturn(new ResultsWrapper<ContentDTO>(studentQuiz, 1L));
        replay(contentManager);

        // Act
        ResultsWrapper<ContentSummaryDTO> availableQuizzes = quizManager.getAvailableQuizzes(true, null, null);

        // Expect
        assertEquals(availableQuizzes.getResults().size(), 1);
        assertEquals(availableQuizzes.getResults().get(0).getTitle(), "Student Quiz");
    }
}