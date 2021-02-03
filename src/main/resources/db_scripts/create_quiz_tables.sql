--- QuizAssignmentDO table for Quiz Assignments

CREATE TABLE public.quiz_assignments (
    id integer NOT NULL,
    quiz_id character varying(255) NOT NULL,
    group_id integer NOT NULL,
    owner_user_id integer,
    creation_date timestamp without time zone,
    due_date timestamp with time zone,
    quiz_feedback_mode text NOT NULL
);


ALTER TABLE public.quiz_assignments OWNER TO rutherford;

CREATE SEQUENCE public.quiz_assignments_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.quiz_assignments_id_seq OWNER TO rutherford;

ALTER SEQUENCE public.quiz_assignments_id_seq OWNED BY public.quiz_assignments.id;

ALTER TABLE ONLY public.quiz_assignments ALTER COLUMN id SET DEFAULT nextval('public.quiz_assignments_id_seq'::regclass);

--- QuizAttemptDO table for Quiz Attempts

CREATE TABLE public.quiz_attempts (
    id integer NOT NULL,
    user_id integer NOT NULL,
    quiz_id character varying(255) NOT NULL,
    quiz_assignment_id integer,
    start_date timestamp without time zone NOT NULL,
    completed_date timestamp with time zone
);

ALTER TABLE public.quiz_attempts OWNER TO rutherford;

CREATE SEQUENCE public.quiz_attempts_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.quiz_attempts_id_seq OWNER TO rutherford;

ALTER SEQUENCE public.quiz_attempts_id_seq OWNED BY public.quiz_attempts.id;

ALTER TABLE ONLY public.quiz_attempts ALTER COLUMN id SET DEFAULT nextval('public.quiz_attempts_id_seq'::regclass);

CREATE UNIQUE INDEX only_one_attempt_per_assignment_per_user ON
    public.quiz_attempts (quiz_assignment_id, user_id)
    WHERE quiz_assignment_id IS NOT NULL;
