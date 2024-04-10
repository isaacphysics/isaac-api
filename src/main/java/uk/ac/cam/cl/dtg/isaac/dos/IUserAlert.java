package uk.ac.cam.cl.dtg.isaac.dos;

import java.time.Instant;

public interface IUserAlert {

  enum AlertEvents {
    SEEN, CLICKED, DISMISSED
  }

  Long getId();

  void setId(Long id);

  Long getUserId();

  void setUserId(Long userId);

  String getMessage();

  void setMessage(String message);

  String getLink();

  void setLink(String link);

  Instant getCreated();

  void setCreated(Instant created);

  Instant getSeen();

  void setSeen(Instant seen);

  Instant getClicked();

  void setClicked(Instant clicked);

  Instant getDismissed();

  void setDismissed(Instant dismissed);

}
