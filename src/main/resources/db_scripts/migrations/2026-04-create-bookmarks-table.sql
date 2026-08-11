-- Table: public.user_bookmarks

-- DROP TABLE public.user_bookmarks;

CREATE TABLE public.user_bookmarks(
    user_id integer NOT NULL,
    content_id text NOT NULL,
    content_type text NOT NULL,
    created timestamp with time zone NOT NULL DEFAULT now(),
    CONSTRAINT user_bookmarks_pk PRIMARY KEY (user_id, content_id),
    CONSTRAINT user_bookmarks_user_id_fk FOREIGN KEY (user_id)
        REFERENCES public.users(id) ON UPDATE CASCADE ON DELETE CASCADE
);

ALTER TABLE public.user_bookmarks
    owner to rutherford;

