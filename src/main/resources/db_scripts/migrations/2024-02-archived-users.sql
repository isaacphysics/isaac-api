CREATE TABLE archived_users (
    id INTEGER NOT NULL,
    family_name TEXT,
    given_name TEXT,
    email TEXT,
    date_of_birth DATE,
    school_other TEXT,
    archived TIMESTAMP NOT NULL,
    CONSTRAINT archived_users_pk PRIMARY KEY (id),
    CONSTRAINT archived_users_fk FOREIGN KEY (id) REFERENCES public.users (id) ON DELETE CASCADE
);
