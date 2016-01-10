CREATE TABLE uk_post_codes
(
  postcode character varying(255) NOT NULL,
  lat numeric NOT NULL,
  lon numeric NOT NULL,
  CONSTRAINT uk_post_codes_pk PRIMARY KEY (postcode)
)