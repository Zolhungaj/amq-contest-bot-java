CREATE TABLE IF NOT EXISTS team_player_link
(
    player_id INTEGER NOT NULL
        REFERENCES player,
    team_id   INTEGER NOT NULL
        REFERENCES team,
    PRIMARY KEY ( player_id, team_id )
);

ALTER TABLE team_player_link
    OWNER TO postgres;

GRANT SELECT ON team_player_link TO martinch;

