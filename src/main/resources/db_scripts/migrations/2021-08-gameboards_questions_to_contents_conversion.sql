CREATE OR REPLACE FUNCTION question_to_content(character varying[]) RETURNS jsonb[] AS
$$
DECLARE
questionIDs ALIAS FOR $1;
    retVal jsonb[];
BEGIN
FOR I IN array_lower(questionIDs, 1)..array_upper(questionIDs, 1) LOOP
            retVal[I] := CAST('{"id": "' || regexp_replace(questionIDs[I], E'[\\n\\r]+', '', 'g' ) || '", "contentType": "isaacQuestionPage", "context": {"role": null, "stage": null, "examBoard": null, "difficulty": null}}' AS jsonb);
END LOOP;
RETURN retVal;
END; --
$$
LANGUAGE plpgsql IMMUTABLE ;

UPDATE public.gameboards SET contents = question_to_content(questions)
WHERE array_length(contents, 1) IS NULL;