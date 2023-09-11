package uk.ac.cam.cl.dtg.segue.etl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Created by Ian on 01/11/2016.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class GithubPushEventPayload {
  private String ref, after;

  public GithubPushEventPayload() {

  }

  public void setRef(final String ref) {
    this.ref = ref;
  }

  public String getRef() {
    return this.ref;
  }

  public void setAfter(final String after) {
    this.after = after;
  }

  public String getAfter() {
    return after;
  }
}
