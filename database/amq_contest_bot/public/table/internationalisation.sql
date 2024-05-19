CREATE TABLE IF NOT EXISTS internationalisation
(
    id             SERIAL
        PRIMARY KEY,
    language_code  TEXT NOT NULL,
    sub_language   TEXT NOT NULL,
    canonical_name TEXT NOT NULL,
    content        TEXT NOT NULL,
    CONSTRAINT internationalisation_language_sub_language_name_value_key
        UNIQUE ( language_code, sub_language, canonical_name, content )
);

ALTER TABLE internationalisation
    OWNER TO postgres;

GRANT SELECT ON internationalisation TO martinch;

