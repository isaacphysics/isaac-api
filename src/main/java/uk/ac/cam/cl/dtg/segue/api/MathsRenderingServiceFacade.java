/**
 * Copyright 2014 Stephen Cummins
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

import java.io.UnsupportedEncodingException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.eclipse.jgit.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.content.MathsContentManager;
import uk.ac.cam.cl.dtg.segue.dto.SegueErrorResponse;

import com.google.inject.Inject;

/**
 * Segue Maths server side rendering service.
 * 
 */
@Path("/maths/")
public class MathsRenderingServiceFacade {
	private static final Logger log = LoggerFactory.getLogger(MathsRenderingServiceFacade.class);

	private MathsContentManager mathsContentManager;
	
	/**
	 * Injectable constructor.
	 * 
	 * @param mathsContentManager - Instance of maths content manager. 
	 */
	@Inject
	public MathsRenderingServiceFacade(final MathsContentManager mathsContentManager) {
		this.mathsContentManager = mathsContentManager;
	}

	/**
	 * Rest endpoint for rendering maths content.
	 * 
	 * @param mathsContent - a base64 encoded latex math string
	 * @return a png with the rendered maths.
	 * @throws UnsupportedEncodingException
	 */
	@GET
	@Path("render/{mathsContent:.*}")
	@Produces("image/png")
	public Response renderMathsAsPng(@PathParam("mathsContent") final String mathsContent) {
		try {
			String maths = new String(Base64.decode(mathsContent), "UTF-8");
			return Response.ok(mathsContentManager.getMaths(maths)).build();
		} catch (SegueDatabaseException e) {
			SegueErrorResponse error = new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR,
					"SegueDatabaseException", e);
			log.error(error.getErrorMessage(), e);
			return error.toResponse();
		} catch (UnsupportedEncodingException e) {
			SegueErrorResponse error = new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR,
					"UnsupportedEncodingException", e);
			log.error(error.getErrorMessage(), e);
			return error.toResponse();
		} catch (IllegalArgumentException e) {
			SegueErrorResponse error = new SegueErrorResponse(Status.BAD_REQUEST,
					"This endpoint only accepts Base64 encoded LaTeX maths strings.");
			log.error(error.getErrorMessage(), e);
			return error.toResponse();
		}
	}
}
