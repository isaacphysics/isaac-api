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
package uk.ac.cam.cl.dtg.segue.dto;

import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;

import javax.annotation.Nullable;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * A wrapper object used to indicate an error has occurred to the client using the API. 
 * TODO: should this be converted into some kind of throwable?
 */
public class SegueErrorResponse implements Serializable {
    @JsonIgnore
    private static final long serialVersionUID = 2310360688716715820L;

    @JsonIgnore
    private final Exception exception;
    
    private final Integer responseCode;
    
    private final String responseCodeType;
    
    private final String errorMessage;

    private boolean bypassGenericSiteErrorPage = false;

    /**
     * Constructor for creating a response with just an error code and string message.
     * 
     * @param errorCode
     *            - for response.
     * @param msg
     *            - message to client.
     */
    public SegueErrorResponse(final Status errorCode, final String msg) {
        this(errorCode.getStatusCode(), errorCode.toString(), msg, null);
    }

    /**
     * Constructor for creating a response with just an error code and string message.
     * 
     * @param errorCode
     *            - for response.
     * @param msg
     *            - message to client.
     * @param e
     *            - exception to wrap.
     */
    public SegueErrorResponse(final Status errorCode, final String msg, final Exception e) {
        this(errorCode.getStatusCode(), errorCode.toString(), msg, e);
    }

    /**
     * Constructor for manually setting all values.
     * 
     * @param responseCode
     *            - status code e.g. 404
     * @param responseCodeType
     *            - the string description of the error code. Eg for 404 Not Found
     * @param errorMessage
     *            - any additional information to show to the user.
     * @param e
     *            - if an exception has been triggered and should be shown in the response.
     */
    public SegueErrorResponse(final Integer responseCode, final String responseCodeType, final String errorMessage,
            @Nullable final Exception e) {
        this.responseCode = responseCode;
        this.responseCodeType = responseCodeType;
        this.exception = e;
        this.errorMessage = errorMessage;
    }
    
    /**
     * Get the error code of this object.
     * 
     * @return the error code as an integer.
     */
    public final Integer getResponseCode() {
        return responseCode;
    }

    /**
     * Get the response code as a string for this object.
     * 
     * @return response code as a string.
     */
    public final String getResponseCodeType() {
        return responseCodeType;
    }

    /**
     * Get the error message stored in this object.
     * 
     * @return the message as a string.
     */
    public final String getErrorMessage() {
        return errorMessage;
    }

    public final boolean getBypassGenericSiteErrorPage() {
        return bypassGenericSiteErrorPage;
    }

    public void setBypassGenericSiteErrorPage(boolean bypassGenericSiteErrorPage) {
        this.bypassGenericSiteErrorPage = bypassGenericSiteErrorPage;
    }

    /**
     * Get additional error information from the wrapped exception.
     * 
     * @return a single line of the exception stack trace.
     */
    public final String getAdditionalErrorInformation() {
        if (null == exception) {
            return null;
        }

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        exception.printStackTrace(pw);
        String stackTrace = sw.toString();

        String[] stackTraceByLine = stackTrace.split(System.getProperty("line.separator"));

        if (stackTraceByLine.length >= 0) {
            return stackTraceByLine[0];
        } else {
            return null;
        }
    }

    /**
     * Returns the response builder preconfigured with this SegueErrorMessage.
     * 
     * This allows you to attach cache control headers or anything else that you may want to do.
     * 
     * @return preconfigured reponse builder.
     */
    public final Response.ResponseBuilder toResponseBuilder() {
        return Response.status(responseCode).entity(this).type("application/json");
    }

    /**
     * Convert this object into a Response object ready for the client.
     * 
     * @return Response object.
     */
    public final Response toResponse() {
        return this.toResponseBuilder().build();
    }

    /**
     * Allow cache control options to be configured so that we don't have to generate this SegueError everytime.
     * 
     * @param cacheControl
     *            - a configured cache control object.
     * @param entityTag
     *            - an etag to be returned with the error response.
     * @return A cache control configured error response.
     */
    public final Response toResponse(final CacheControl cacheControl, final EntityTag entityTag) {
        return this.toResponseBuilder().cacheControl(cacheControl).tag(entityTag).build();
    }

    @Override
    public final String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Error Code: " + responseCode.toString() + " ");
        sb.append("Error Message: " + responseCode.toString() + " ");
        sb.append('\n' + this.getAdditionalErrorInformation());
        return sb.toString();
    }

    /**
     * @return a default response for when the user must be logged in to access a resource.
     */
    public static Response getNotLoggedInResponse() {
        return new SegueErrorResponse(Status.UNAUTHORIZED, "You must be logged in to access this resource.")
                .toResponse();
    }

    /**
     * @return a default response for when the user does not have the correct access rights to access a resource.
     */
    public static Response getIncorrectRoleResponse() {
        return new SegueErrorResponse(Status.FORBIDDEN, "You do not have the permissions to complete this action")
                .toResponse();
    }

    /**
     * @return a default response for when an endpoint will exist in the future but has not yet been implemented.
     */
    public static Response getNotImplementedResponse() {
        return new SegueErrorResponse(Status.NOT_IMPLEMENTED, "This endpoint has not yet been implemented")
                .toResponse();
    }
    
    /**
     * @param message - inform the user how long they will be throttled for.
     * @return error response.
     */
    public static Response getRateThrottledResponse(final String message) {
        final Integer throttledStatusCode = 429;
        return new SegueErrorResponse(throttledStatusCode, "Too Many Requests", message, null).toResponse();
    }

    /**
     * @param message
     *            - the message for the user.
     * @return a helper function to get a resource not found response
     */
    public static Response getResourceNotFoundResponse(final String message) {
        return new SegueErrorResponse(Status.NOT_FOUND, message).toResponse();
    }

    /**
     * @param message
     *            - the message for the user.
     * @return a helper function to get a service unavailable response
     */
    public static Response getServiceUnavailableResponse(final String message) {
        return new SegueErrorResponse(Status.SERVICE_UNAVAILABLE, message).toResponse();
    }

    /**
     * @param message - inform the user how long they will be throttled for.
     * @return error response.
     */
    public static Response getMethodNotAllowedReponse(final String message) {
        return new SegueErrorResponse(Status.METHOD_NOT_ALLOWED, message)
                .toResponse();
    }

}
