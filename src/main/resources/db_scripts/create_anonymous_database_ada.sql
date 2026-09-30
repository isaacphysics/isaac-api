-- Note that this requires create_anonymous_database to have run first

CREATE OR REPLACE FUNCTION create_anonymous_database_ada(hash_salt TEXT) RETURNS boolean
AS
$$
BEGIN
    CREATE TABLE anonymous.survey_autumn_2023 AS
    SELECT survey,
           anonymise(user_id, hash_salt) AS user_id,
           question,
           response_number,
           response,
           question_mapped,
           question_category,
           response_sort_order,
           positive_response,
           response_format,
           other_feedback_code_1,
           other_feedback_code_2
    FROM public.survey_autumn_2023;

    CREATE TABLE anonymous.pilot_teachers_data_science_course_2026 AS
    SELECT anonymise(user_id, hash_salt) AS user_id
    FROM public.pilot_teachers_data_science_course_2026;

    RETURN true;
END;
$$
    LANGUAGE plpgsql;