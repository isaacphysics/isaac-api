package uk.ac.cam.cl.dtg.segue.dto;

import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class SegueErrorResponse implements Serializable {

	@JsonIgnore
	private static final long serialVersionUID = 2310360688716715820L;

	@JsonIgnore
	private Exception exception;

	private Status responseCode;

	private String errorMessage;

	public SegueErrorResponse(Status errorCode, String msg) {
		this.responseCode = errorCode;
		this.errorMessage = msg;
	}

	public SegueErrorResponse(Status errorCode, String msg, Exception e) {
		this(errorCode, msg);
		this.responseCode = errorCode;
	}

	public Integer getResponseCode() {
		return responseCode.getStatusCode();
	}

	public String getResponseCodeType() {
		return responseCode.toString();
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public String getAdditionalErrorInformation() {
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

	public Response toResponse() {
		return Response.status(responseCode).entity(this)
				.type("application/json").build();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Error Code: " + responseCode.toString() + " ");
		sb.append("Error Message: " + responseCode.toString() + " ");
		sb.append('\n' + this.getAdditionalErrorInformation());
		return sb.toString();
	}
}
