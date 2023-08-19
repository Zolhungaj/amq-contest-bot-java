CREATE TABLE IF NOT EXISTS ban
(
    id        SERIAL
        PRIMARY KEY,
    player_id INTEGER                             NOT NULL
        REFERENCES player,
    start     TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    expiry    TIMESTAMP                           NOT NULL,
    reason    TEXT                                NOT NULL
);

ALTER TABLE ban
    OWNER TO postgres;

