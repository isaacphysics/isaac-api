/*
 * Copyright 2024 Meurig Thomas
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.cam.cl.dtg.isaac.api.managers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.dos.users.UserContext;
import uk.ac.cam.cl.dtg.isaac.tutor.TutorExternalService;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class TutorManager {
    private static final Logger log = LoggerFactory.getLogger(QuizManager.class);

    private final TutorExternalService tutorExternalService;

    @Inject
    public TutorManager(final TutorExternalService tutorExternalService) {
        this.tutorExternalService = tutorExternalService;
    }

    private Map<String, String> deserialiseSimpleJSON(final String json) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(json, Map.class);
    }

    public Map<String, Object> createNewThread(final List<UserContext> registeredContexts) throws IOException {
        return tutorExternalService.createNewThread(registeredContexts);
    }

    public Map<String, Object> getThreadMessages(final String threadId) throws IOException {
        return tutorExternalService.getThreadMessages(threadId);
    }

    public Map<String, Object> addMessageToThread(final String threadId, final String jsonMessage) throws IOException {
        Map<String, String> message = deserialiseSimpleJSON(jsonMessage);
        return tutorExternalService.addMessageToThread(threadId, message);
    }

    public Map<String, Object> getRun(final String threadId, final String runId) throws IOException {
        return tutorExternalService.getRun(threadId, runId);
    }
}