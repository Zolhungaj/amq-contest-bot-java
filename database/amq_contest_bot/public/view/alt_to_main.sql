CREATE VIEW alt_to_main( alt_player_id, main_player_id )
AS
    WITH RECURSIVE alt_resolved AS (
                                   SELECT alt.alt_player_id,
                                          alt.main_player_id,
                                          0                          AS depth,
                                          ARRAY [alt.main_player_id] AS visited
                                   FROM alt
                                   UNION ALL
                                   SELECT ar.alt_player_id,
                                          a_1.main_player_id,
                                          ar.depth + 1,
                                          ar.visited || a_1.main_player_id
                                   FROM alt_resolved ar
                                       JOIN alt      a_1 ON ar.main_player_id = a_1.alt_player_id
                                   WHERE NOT ( a_1.main_player_id = ANY ( ar.visited ) ) )
    SELECT a.alt_player_id,
           a.main_player_id
    FROM alt_resolved a
    WHERE a.depth = ((
                     SELECT MAX( b.depth ) AS max
                     FROM alt_resolved b
                     WHERE a.alt_player_id = b.alt_player_id ) )
    UNION
    SELECT player.id AS alt_player_id,
           player.id AS main_player_id
    FROM player
    WHERE NOT ( player.id IN (
                             SELECT alt.alt_player_id
                             FROM alt ) );

ALTER TABLE alt_to_main
    OWNER TO postgres;

