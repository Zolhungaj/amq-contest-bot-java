CREATE TABLE IF NOT EXISTS message
(
    id              SERIAL
        PRIMARY KEY,
    player_id       INTEGER                             NOT NULL
        REFERENCES player,
    content         TEXT                                NOT NULL,
    time            TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    room_id         INTEGER                             NOT NULL,
    room_message_id INTEGER                             NOT NULL
);

ALTER TABLE message
    OWNER TO postgres;

GRANT SELECT ON message TO martinch;

