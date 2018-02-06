-- Column: tags

-- ALTER TABLE public.gameboards DROP COLUMN tags;

ALTER TABLE public.gameboards ADD COLUMN tags jsonb DEFAULT '[]'::jsonb NOT NULL;
CREATE INDEX gameboards_tags_gin_index ON public.gameboards USING gin (tags);