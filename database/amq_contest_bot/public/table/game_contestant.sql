CREATE TABLE IF NOT EXISTS game_contestant
(
    id              SERIAL
        PRIMARY KEY,
    game_id         INTEGER               NOT NULL
        REFERENCES game,
    contestant_id   INTEGER               NOT NULL
        REFERENCES contestant,
    score           INTEGER               NOT NULL,
    correct_count   INTEGER               NOT NULL,
    miss_count      INTEGER               NOT NULL,
    correct_time    INTEGER               NOT NULL,
    miss_time       INTEGER               NOT NULL,
    deleted         BOOLEAN DEFAULT FALSE NOT NULL,
    game_mode_score INTEGER               NOT NULL,
    position        INTEGER               NOT NULL,
    UNIQUE ( game_id, contestant_id )
);

ALTER TABLE game_contestant
    OWNER TO postgres;

