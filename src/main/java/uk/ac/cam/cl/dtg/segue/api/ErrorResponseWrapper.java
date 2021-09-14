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
package uk.ac.cam.cl.dtg.segue.api;

import uk.ac.cam.cl.dtg.segue.dto.SegueErrorResponse;

import javax.ws.rs.core.Response;

public class ErrorResponseWrapper extends Exception {
    private static final long serialVersionUID = 776713551334466191L;
    private final SegueErrorResponse response;

    public ErrorResponseWrapper(SegueErrorResponse response) {
        this.response = response;
    }

    public Response toResponse() {
        return response.toResponse();
    }
}
