CREATE VIEW valour_record( level, player_id, referer_id )
AS
    SELECT valour_recursive.level,
       valour_recursive.player_id,
       valour_recursive.referer_id
FROM valour_recursive;

ALTER TABLE valour_record
    OWNER TO postgres;

