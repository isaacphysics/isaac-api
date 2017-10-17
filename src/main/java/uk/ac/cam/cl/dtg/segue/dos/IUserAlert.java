package uk.ac.cam.cl.dtg.segue.dos;

import java.util.Date;

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

    Date getCreated();
    void setCreated(Date created);

    Date getSeen();
    void setSeen(Date seen);

    Date getClicked();
    void setClicked(Date clicked);

    Date getDismissed();
    void setDismissed(Date dismissed);

}
