CREATE TABLE IF NOT EXISTS contestant
(
    id        SERIAL
        PRIMARY KEY,
    player_id INTEGER
        UNIQUE
        REFERENCES player,
    team_id   INTEGER
        UNIQUE
        REFERENCES team,
    type      TEXT NOT NULL,
    CONSTRAINT is_not_both
        CHECK (( player_id IS NULL ) OR ( team_id IS NULL )),
    CONSTRAINT is_at_least_one
        CHECK (( player_id IS NOT NULL ) OR ( team_id IS NOT NULL )),
    CONSTRAINT is_player
        CHECK (( player_id IS NULL ) OR ( type = 'PLAYER'::TEXT )),
    CONSTRAINT is_team
        CHECK (( team_id IS NULL ) OR ( type = 'TEAM'::TEXT ))
);

ALTER TABLE contestant
    OWNER TO postgres;

GRANT SELECT ON contestant TO martinch;

