CREATE VIEW valour_recursive( level, player_id, referer_id )
AS
    WITH RECURSIVE valour_recursive( level, player_id, referer_id ) AS (
                                                                       SELECT 0 AS "?column?",
                                                                              v.player_id,
                                                                              v.referer_id
                                                                       FROM valour v
                                                                       WHERE v.referer_id IS NULL
                                                                       UNION ALL
                                                                       SELECT r.level + 1,
                                                                              v.player_id,
                                                                              v.referer_id
                                                                       FROM valour_recursive r
                                                                           JOIN valour       v ON r.player_id = v.referer_id )
    SELECT valour_recursive.level,
           valour_recursive.player_id,
           valour_recursive.referer_id
    FROM valour_recursive;

ALTER TABLE valour_recursive
    OWNER TO postgres;

GRANT SELECT ON valour_recursive TO martinch;

