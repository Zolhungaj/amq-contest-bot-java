CREATE TABLE IF NOT EXISTS admin
(
    player_id  INTEGER NOT NULL
        PRIMARY KEY
        REFERENCES player,
    admin_type TEXT    NOT NULL
        CONSTRAINT valid_type
            CHECK (admin_type = ANY ( ARRAY ['OWNER'::TEXT, 'HOST'::TEXT, 'ADMIN'::TEXT, 'MODERATOR'::TEXT] ))
);

ALTER TABLE admin
    OWNER TO postgres;

GRANT SELECT ON admin TO martinch;

