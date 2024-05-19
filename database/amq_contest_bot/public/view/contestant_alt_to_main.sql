CREATE VIEW contestant_alt_to_main( alt_id, main_id )
AS
    SELECT contestant.id AS alt_id,
           contestant.id AS main_id
    FROM contestant
    WHERE NOT ( contestant.player_id IN (
                                        SELECT alt.alt_player_id
                                        FROM alt ) )
    UNION
    SELECT c1.id AS alt_id,
           c2.id AS main_id
    FROM contestant     c1
        JOIN alt_to_main ON alt_to_main.alt_player_id = c1.player_id
        JOIN contestant c2 ON alt_to_main.main_player_id = c2.player_id
    WHERE ( c1.player_id IN (
                            SELECT alt.alt_player_id
                            FROM alt ) );

ALTER TABLE contestant_alt_to_main
    OWNER TO postgres;

