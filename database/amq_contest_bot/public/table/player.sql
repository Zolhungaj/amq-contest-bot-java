CREATE TABLE IF NOT EXISTS player
(
    id                  SERIAL
        PRIMARY KEY,
    original_name       TEXT
        UNIQUE,
    level               INTEGER,
    avatar              json,
    latest_patreon_date DATE
);

ALTER TABLE player
    OWNER TO postgres;

GRANT SELECT ON player TO martinch;

