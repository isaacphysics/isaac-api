CREATE TABLE public.skills_question_attempts (
    id TEXT NOT NULL,
    user_id INTEGER NOT NULL,
    skill_assignment_id TEXT,
    skill_id TEXT NOT NULL,
    subskill_id TEXT NOT NULL,
    question_text TEXT NOT NULL,
    question_answer TEXT NOT NULL,
    question_attempt TEXT NOT NULL,
    marks INTEGER NOT NULL,
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL
);

ALTER TABLE public.skills_question_attempts OWNER to rutherford;
