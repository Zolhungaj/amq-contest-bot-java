CREATE TABLE IF NOT EXISTS elo
(
    contestant_id INTEGER NOT NULL
        REFERENCES contestant,
    game_mode_id  INTEGER NOT NULL
        REFERENCES game_mode,
    rating        INTEGER NOT NULL,
    PRIMARY KEY ( contestant_id, game_mode_id )
);

ALTER TABLE elo
    OWNER TO postgres;

GRANT SELECT ON elo TO martinch;

