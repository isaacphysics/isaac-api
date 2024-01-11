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
package uk.ac.cam.cl.dtg.isaac.tutor;

public class TutorExternalService {

    private final String hostname;
    private final String port;
    private final String externalTutorUrl;

    public TutorExternalService(final String hostname, final String port) {
        this.hostname = hostname;
        this.port = port;
        this.externalTutorUrl = "http://" + this.hostname + ":" + this.port;
    }

    public String createNewThread() {
        return "Dummy thread ID from " + this.externalTutorUrl;
    }

}
