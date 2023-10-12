CREATE TABLE expired_users (
    id INTEGER NOT NULL,
    family_name TEXT,
    given_name TEXT,
    email TEXT,
    date_of_birth DATE,
    school_other TEXT,
    expired TIMESTAMP NOT NULL,
    CONSTRAINT expired_users_pk PRIMARY KEY (id),
    CONSTRAINT expired_users_fk FOREIGN KEY (id) REFERENCES public.users (id) ON DELETE CASCADE
);
