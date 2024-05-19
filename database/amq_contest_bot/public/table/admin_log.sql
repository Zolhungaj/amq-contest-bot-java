CREATE TABLE IF NOT EXISTS admin_log
(
    id       SERIAL
        PRIMARY KEY,
    admin_id INTEGER NOT NULL
        REFERENCES player,
    action   TEXT    NOT NULL
);

ALTER TABLE admin_log
    OWNER TO postgres;

GRANT SELECT ON admin_log TO martinch;

