package uk.ac.cam.cl.dtg.isaac.dos.users;

import java.util.Date;

public class AccountDeletionToken {
    private Long userId;
    private String token;
    private Date tokenExpiry;
    private Date created;
    private Date lastUpdated;

    public AccountDeletionToken(final Long userId, final String token, final Date tokenExpiry,
                                final Date created, final Date lastUpdated) {
        this.userId = userId;
        this.token = token;
        this.tokenExpiry = tokenExpiry;
        this.created = created;
        this.lastUpdated = lastUpdated;
    }

    public AccountDeletionToken(final Long userId, final String token, final Date tokenExpiry) {
        this.userId = userId;
        this.token = token;
        this.tokenExpiry = tokenExpiry;
    }

    public Long getUserId() {
        return userId;
    }

    public String getToken() {
        return token;
    }

    public Date getTokenExpiry() {
        return tokenExpiry;
    }

    public Date getCreated() {
        return created;
    }

    public Date getLastUpdated() {
        return lastUpdated;
    }
}
