CREATE TABLE IF NOT EXISTS song
(
    id         INTEGER DEFAULT NEXTVAL( 'song_song_id_seq'::regclass ) NOT NULL
        PRIMARY KEY,
    anime_id   INTEGER                                                 NOT NULL
        REFERENCES anime,
    type       TEXT                                                    NOT NULL
        CONSTRAINT valid_type
            CHECK (type = ANY ( ARRAY ['OPENING'::TEXT, 'ENDING'::TEXT, 'INSERT'::TEXT] )),
    number     INTEGER                                                 NOT NULL,
    title      TEXT                                                    NOT NULL,
    artist     TEXT                                                    NOT NULL,
    difficulty DOUBLE PRECISION
);

ALTER TABLE song
    OWNER TO postgres;

