CREATE VIEW leaderboard_count ( contestant_id, game_mode_score, ruleset, team_size, times_achieved, earliest_achieved )
AS
    SELECT c.id           AS contestant_id,
           gc.game_mode_score,
           gm.ruleset,
           gm.team_size,
           COUNT( * )     AS times_achieved,
           MIN( g.start ) AS earliest_achieved
    FROM game_contestant                 gc
        JOIN      game                   g ON gc.game_id = g.id
        JOIN      game_mode              gm ON g.game_mode = gm.id
        JOIN      contestant             c_o ON gc.contestant_id = c_o.id
        JOIN      contestant_alt_to_main c_a ON c_o.id = c_a.alt_id
        JOIN      contestant             c ON c_a.main_id = c.id
        LEFT JOIN player                 p ON c.player_id = p.id
        LEFT JOIN team                   t ON c.team_id = t.id
    WHERE gm.scoring_type = 'COUNT'::TEXT
      AND gc.game_mode_score = ((
                                SELECT MAX( gc2.game_mode_score ) AS max
                                FROM game_contestant            gc2
                                    JOIN contestant_alt_to_main c2 ON gc2.contestant_id = c2.alt_id
                                    JOIN game                   g2 ON gc2.game_id = g2.id
                                    JOIN game_mode              gm2 ON g2.game_mode = gm2.id
                                WHERE c2.main_id = c.id
                                  AND gm2.id = gm.id
                                  AND gc2.deleted = FALSE
                                  AND g2.start >= MAKE_DATE( 2024, 1, 1 ) ) )
      AND gc.deleted = FALSE
      AND ( p.original_name IS NULL OR p.original_name !~~ 'Guest-%'::TEXT )
      AND COALESCE( p.level, 9999 ) > 150
      AND NOT ( EXISTS (
                       SELECT NULL::TEXT AS text
                       FROM player               p2
                           JOIN team_player_link tpl ON p2.id = tpl.player_id
                       WHERE tpl.team_id = t.id
                         AND ( p2.original_name ~~ 'Guest-%'::TEXT OR COALESCE( p2.level, 9999 ) < 75 ) ) )
      AND NOT ( EXISTS (
                       SELECT NULL::TEXT AS text
                       FROM team_player_link tpl
                           JOIN ban          b ON tpl.player_id = b.player_id
                       WHERE tpl.team_id = t.id ) )
      AND NOT ( EXISTS (
                       SELECT NULL::TEXT AS text
                       FROM ban b
                       WHERE b.player_id = p.id ) )
      AND g.start >= MAKE_DATE( 2024, 1, 1 )
    GROUP BY gm.ruleset,
             gm.team_size,
             c.id,
             gc.game_mode_score;

ALTER TABLE leaderboard_count
    OWNER TO postgres;

GRANT SELECT ON leaderboard_count TO martinch;

