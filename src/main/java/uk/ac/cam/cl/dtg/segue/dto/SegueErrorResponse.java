package uk.ac.cam.cl.dtg.segue.dto;

import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;

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
	private Exception exception;

	private Status responseCode;

	private String errorMessage;

	/**
	 * Constructor for creating a response with just an error code and string message.
	 * @param errorCode - for response.
	 * @param msg - message to client.
	 */
	public SegueErrorResponse(final Status errorCode, final String msg) {
		this.responseCode = errorCode;
		this.errorMessage = msg;
	}

	/**
	 * Constructor for creating a response with just an error code and string message.
	 * @param errorCode - for response.
	 * @param msg - message to client.
	 * @param e - exception to wrap.
	 */
	public SegueErrorResponse(final Status errorCode, final String msg, final Exception e) {
		this(errorCode, msg);
		this.responseCode = errorCode;
		this.exception = e;
	}
	
	
	/**
	 * Get the error code of this object.
	 * @return the error code as an integer.
	 */
	public final Integer getResponseCode() {
		return responseCode.getStatusCode();
	}
	
	/**
	 * Get the response code as a string for this object.
	 * @return response code as a string.
	 */
	public final String getResponseCodeType() {
		return responseCode.toString();
	}

	/**
	 * Get the error message stored in this object.
	 * @return the message as a string.
	 */
	public final String getErrorMessage() {
		return errorMessage;
	}
	
	/**
	 * Get additional error information from the wrapped exception. 
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

		String[] stackTraceByLine = stackTrace.split(System
				.getProperty("line.separator"));

		if (stackTraceByLine.length >= 0) {
			return stackTraceByLine[0];
		} else {
			return null;
		}
	}
	
	/**
	 * Convert this object into a Response object ready for the client.
	 * @return Response object.
	 */
	public final Response toResponse() {
		return Response.status(responseCode).entity(this)
				.type("application/json").build();
	}

	@Override
	public final String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Error Code: " + responseCode.toString() + " ");
		sb.append("Error Message: " + responseCode.toString() + " ");
		sb.append('\n' + this.getAdditionalErrorInformation());
		return sb.toString();
	}
}
