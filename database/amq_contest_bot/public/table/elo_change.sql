CREATE TABLE IF NOT EXISTS elo_change
(
    game_id       INTEGER NOT NULL,
    contestant_id INTEGER NOT NULL,
    rating_change INTEGER,
    PRIMARY KEY ( game_id, contestant_id ),
    FOREIGN KEY ( game_id, contestant_id ) REFERENCES game_contestant ( game_id, contestant_id )
);

ALTER TABLE elo_change
    OWNER TO postgres;

