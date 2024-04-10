package uk.ac.cam.cl.dtg.isaac.dos;

import java.time.Instant;

public class PgUserAlert implements IUserAlert {

  private Long id;
  private Long userId;
  private String message;
  private String link;
  private Instant created;
  private Instant seen;
  private Instant clicked;
  private Instant dismissed;

  public PgUserAlert(final Long id, final Long userId, final String message, final String link, final Instant created,
                     final Instant seen, final Instant clicked, final Instant dismissed) {
    this.id = id;
    this.userId = userId;
    this.message = message;
    this.link = link;
    this.created = created;
    this.seen = seen;
    this.clicked = clicked;
    this.dismissed = dismissed;
  }

  @Override
  public Long getId() {
    return id;
  }

  @Override
  public void setId(final Long id) {
    this.id = id;
  }

  @Override
  public Long getUserId() {
    return userId;
  }

  @Override
  public void setUserId(final Long userId) {
    this.userId = userId;
  }

  @Override
  public String getMessage() {
    return message;
  }

  @Override
  public void setMessage(final String message) {
    this.message = message;
  }

  @Override
  public String getLink() {
    return link;
  }

  @Override
  public void setLink(final String link) {
    this.link = link;
  }

  @Override
  public Instant getCreated() {
    return created;
  }

  @Override
  public void setCreated(final Instant created) {
    this.created = created;
  }

  @Override
  public Instant getSeen() {
    return seen;
  }

  @Override
  public void setSeen(final Instant seen) {
    this.seen = seen;
  }

  @Override
  public Instant getClicked() {
    return clicked;
  }

  @Override
  public void setClicked(final Instant clicked) {
    this.clicked = clicked;
  }

  @Override
  public Instant getDismissed() {
    return dismissed;
  }

  @Override
  public void setDismissed(final Instant dismissed) {
    this.dismissed = dismissed;
  }
}
