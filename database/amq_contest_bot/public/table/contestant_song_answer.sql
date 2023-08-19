CREATE TABLE IF NOT EXISTS contestant_song_answer
(
    id            SERIAL
        PRIMARY KEY,
    game_song_id  INTEGER NOT NULL
        REFERENCES game_song,
    contestant_id INTEGER NOT NULL
        REFERENCES contestant,
    answer        TEXT,
    correct       BOOLEAN NOT NULL,
    answer_time   BIGINT,
    UNIQUE ( game_song_id, contestant_id )
);

ALTER TABLE contestant_song_answer
    OWNER TO postgres;

