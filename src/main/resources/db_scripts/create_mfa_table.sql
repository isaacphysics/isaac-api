-- Table: public.user_totp

-- DROP TABLE public.user_totp;

CREATE TABLE public.user_totp
(
    user_id integer NOT NULL,
    shared_secret text COLLATE pg_catalog."default" NOT NULL,
    created timestamp with time zone NOT NULL,
    CONSTRAINT user_id_mfa_pk PRIMARY KEY (user_id),
    CONSTRAINT user_id_mfa_fk FOREIGN KEY (user_id)
        REFERENCES public.users (id) MATCH SIMPLE
        ON UPDATE CASCADE
        ON DELETE CASCADE
);

ALTER TABLE public.user_totp
    OWNER to rutherford;