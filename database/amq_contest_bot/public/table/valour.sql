CREATE TABLE IF NOT EXISTS valour
(
    player_id  INTEGER NOT NULL
        PRIMARY KEY
        REFERENCES player,
    surplus    INTEGER NOT NULL,
    referer_id INTEGER
        REFERENCES player
);

ALTER TABLE valour
    OWNER TO postgres;

GRANT SELECT ON valour TO martinch;

