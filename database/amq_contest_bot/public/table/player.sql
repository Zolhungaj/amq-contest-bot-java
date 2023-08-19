CREATE TABLE IF NOT EXISTS player
(
    id            SERIAL
        PRIMARY KEY,
    original_name TEXT
        UNIQUE,
    level         INTEGER,
    avatar        json
);

ALTER TABLE player
    OWNER TO postgres;

