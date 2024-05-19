CREATE TABLE IF NOT EXISTS alt
(
    alt_player_id  INTEGER NOT NULL
        CONSTRAINT alt_pk
            PRIMARY KEY
        CONSTRAINT alt_player_id_fk
            REFERENCES player
            ON UPDATE CASCADE ON DELETE CASCADE,
    main_player_id INTEGER NOT NULL
        CONSTRAINT alt_alt_player_id_fk
            REFERENCES player
            ON UPDATE CASCADE ON DELETE CASCADE
);

ALTER TABLE alt
    OWNER TO postgres;

