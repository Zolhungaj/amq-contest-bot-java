CREATE SEQUENCE song_song_id_seq
    AS INTEGER;

ALTER SEQUENCE song_song_id_seq OWNER TO postgres;

ALTER SEQUENCE song_song_id_seq OWNED BY song.id;

