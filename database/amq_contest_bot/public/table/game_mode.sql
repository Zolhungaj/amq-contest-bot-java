CREATE TABLE IF NOT EXISTS game_mode
(
    id           SERIAL
        CONSTRAINT game_mode_pk
            PRIMARY KEY,
    name         TEXT
        UNIQUE,
    ruleset      TEXT    NOT NULL
        CONSTRAINT valid_rule_set
            CHECK (ruleset = ANY
                   ( ARRAY ['OPENINGS'::TEXT, 'ENDINGS'::TEXT, 'INSERTS'::TEXT, 'OPENINGS_ENDINGS'::TEXT, 'ALL'::TEXT, 'ALL_HARD'::TEXT, 'MASTER_OF_THE_SEASON'::TEXT, 'MASTER_OF_SEASONS'::TEXT] )),
    scoring_type TEXT    NOT NULL
        CONSTRAINT valid_scoring_type
            CHECK (scoring_type = ANY ( ARRAY ['COUNT'::TEXT, 'SPEEDRUN'::TEXT, 'SPEED'::TEXT, 'LIVES'::TEXT] )),
    team_size    INTEGER NOT NULL
);

ALTER TABLE game_mode
    OWNER TO postgres;

GRANT SELECT ON game_mode TO martinch;

