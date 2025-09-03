package uk.ac.cam.cl.dtg.isaac.api.requests;

import java.time.Instant;


public class PrivacyPolicyRequest {
  private long privacyPolicyAcceptedTime;

  public Instant getPrivacyPolicyAcceptedTimeInstant() {
    return Instant.ofEpochMilli(privacyPolicyAcceptedTime);
  }

  public long getPrivacyPolicyAcceptedTime() {
    return privacyPolicyAcceptedTime;
  }

  public void setPrivacyPolicyAcceptedTime(long privacyPolicyAcceptedTime) {
    this.privacyPolicyAcceptedTime = privacyPolicyAcceptedTime;
  }
}
