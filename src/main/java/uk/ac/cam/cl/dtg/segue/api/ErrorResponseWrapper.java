/**
 * Copyright 2021 Raspberry Pi Foundation
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

package uk.ac.cam.cl.dtg.segue.api;

import jakarta.ws.rs.core.Response;
import uk.ac.cam.cl.dtg.isaac.dto.SegueErrorResponse;

public class ErrorResponseWrapper extends Exception {
  private static final long serialVersionUID = 776713551334466191L;
  private final SegueErrorResponse response;

  public ErrorResponseWrapper(final SegueErrorResponse response) {
    this.response = response;
  }

  public Response toResponse() {
    return response.toResponse();
  }
}
