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

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.tutor.TutorExternalService;

public class TutorManager {
    private static final Logger log = LoggerFactory.getLogger(QuizManager.class);

    private final TutorExternalService tutorExternalService;

    @Inject
    public TutorManager(final TutorExternalService tutorExternalService) {
        this.tutorExternalService = tutorExternalService;
    }

    public String createNewThread() {
        return tutorExternalService.createNewThread();
    }
}