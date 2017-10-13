package uk.ac.cam.cl.dtg.segue.dos;

import java.util.Date;

public class PgUserAlert implements IUserAlert {

    private Long id;
    private Long userId;
    private String message;
    private String link;
    private Date created;
    private Date seen;
    private Date clicked;
    private Date dismissed;

    public PgUserAlert(final Long id, final Long userId, final String message, final String link, final Date created,
                       final Date seen, final Date clicked, final Date dismissed) {
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
    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public Long getUserId() {
        return userId;
    }

    @Override
    public void setUserId(Long userId) {
        this.userId = userId;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String getLink() {
        return link;
    }

    @Override
    public void setLink(String link) {
        this.link = link;
    }

    @Override
    public Date getCreated() {
        return created;
    }

    @Override
    public void setCreated(Date created) {
        this.created = created;
    }

    @Override
    public Date getSeen() {
        return seen;
    }

    @Override
    public void setSeen(Date seen) {
        this.seen = seen;
    }

    @Override
    public Date getClicked() {
        return clicked;
    }

    @Override
    public void setClicked(Date clicked) {
        this.clicked = clicked;
    }

    @Override
    public Date getDismissed() {
        return dismissed;
    }

    @Override
    public void setDismissed(Date dismissed) {
        this.dismissed = dismissed;
    }
}
