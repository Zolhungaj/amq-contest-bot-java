CREATE TABLE IF NOT EXISTS game_song
(
    id      SERIAL
        PRIMARY KEY,
    game_id INTEGER NOT NULL
        REFERENCES game,
    ordinal INTEGER NOT NULL,
    song_id INTEGER NOT NULL
        REFERENCES song,
    UNIQUE ( game_id, ordinal )
);

ALTER TABLE game_song
    OWNER TO postgres;

GRANT SELECT ON game_song TO martinch;

