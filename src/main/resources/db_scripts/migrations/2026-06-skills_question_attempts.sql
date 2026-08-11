CREATE TABLE public.skills_question_attempts (
    id UUID PRIMARY KEY,
    user_id INTEGER NOT NULL,
    skill_assignment_id TEXT,
    skill_id TEXT NOT NULL,
    subskill_id TEXT NOT NULL,
    question JSONB NOT NULL,
    question_attempt JSONB NOT NULL,
    marks INTEGER NOT NULL,
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT user_id_skills_question_attempts_fk FOREIGN KEY (user_id) REFERENCES public.users (id)
        ON UPDATE CASCADE ON DELETE CASCADE -- account deletion does not delete user so this won't clear attempt either
);

ALTER TABLE public.skills_question_attempts OWNER to rutherford;
