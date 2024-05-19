CREATE TABLE IF NOT EXISTS team
(
    id   SERIAL
        PRIMARY KEY,
    name TEXT NOT NULL
        UNIQUE
);

ALTER TABLE team
    OWNER TO postgres;

GRANT SELECT ON team TO martinch;

