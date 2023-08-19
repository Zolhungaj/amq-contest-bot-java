CREATE TABLE IF NOT EXISTS anime
(
    id                  INTEGER NOT NULL
        PRIMARY KEY,
    name_english        TEXT,
    name_romaji         TEXT,
    broadcast_format    TEXT    NOT NULL
        CONSTRAINT valid_broadcast_format
            CHECK (broadcast_format = ANY
                   ( ARRAY ['TV'::TEXT, 'MOVIE'::TEXT, 'OVA'::TEXT, 'ONA'::TEXT, 'SPECIAL'::TEXT] )),
    kitsu_id            INTEGER,
    animenewsnetwork_id INTEGER,
    myanimelist_id      INTEGER,
    anilist_id          INTEGER,
    rating              NUMERIC(4, 2),
    year                INTEGER,
    season              TEXT
        CONSTRAINT valid_season
            CHECK (season = ANY ( ARRAY ['WINTER'::TEXT, 'SPRING'::TEXT, 'SUMMER'::TEXT, 'AUTUMN'::TEXT] ))
);

ALTER TABLE anime
    OWNER TO postgres;

