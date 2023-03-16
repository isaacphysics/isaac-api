/*
 * Copyright 2023 Matthew Trew
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

import com.google.inject.Inject;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.annotations.GZIP;
import uk.ac.cam.cl.dtg.segue.api.managers.CountryLookupManager;

import java.util.Locale;

@Path("/countries")
@Tag(name = "/countries")
public class CountryLookupFacade {

    private final CountryLookupManager countryLookupManager;

    @Inject
    public CountryLookupFacade(CountryLookupManager countryLookupManager) {
        this.countryLookupManager = countryLookupManager;

    }

    @GET
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    @Operation(summary = "Get a sorted map of all known country codes and display names.")
    public Response countries() {
        return Response.ok(countryLookupManager.getCountryCodesAndNames()).build();
    }

    @GET
    @Path("/priority")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    @Operation(summary = "Get a sorted map of priority country codes and display names.")
    public Response priorityCountries() {
        return Response.ok(countryLookupManager.getPriorityCountryCodesAndNames()).build();
    }
}
