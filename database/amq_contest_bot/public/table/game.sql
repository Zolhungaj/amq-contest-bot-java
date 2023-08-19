CREATE TABLE IF NOT EXISTS game
(
    id        SERIAL
        PRIMARY KEY,
    game_mode INTEGER
        REFERENCES game_mode,
    start     TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    finish    TIMESTAMP
);

ALTER TABLE game
    OWNER TO postgres;

