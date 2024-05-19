CREATE VIEW leaderboard_speedrun
            ( contestant_id, game_mode_score, correct_time, ruleset, team_size, times_achieved, earliest_achieved )
AS
    SELECT gc.contestant_id,
           gc.game_mode_score,
           gc.correct_time,
           gm.ruleset,
           gm.team_size,
           COUNT( * )     AS times_achieved,
           MIN( g.start ) AS earliest_achieved
    FROM game_contestant     gc
        JOIN      game       g ON gc.game_id = g.id
        JOIN      game_mode  gm ON g.game_mode = gm.id
        JOIN      contestant c ON gc.contestant_id = c.id
        LEFT JOIN player     p ON c.player_id = p.id
        LEFT JOIN team       t ON c.team_id = t.id
    WHERE gm.scoring_type = 'SPEEDRUN'::TEXT
      AND ( ( gc.game_mode_score, gc.correct_time ) = (
                                                      SELECT gc2.game_mode_score AS max,
                                                             gc2.correct_time    AS "time"
                                                      FROM game_contestant gc2
                                                          JOIN game        g2 ON gc2.game_id = g2.id
                                                          JOIN game_mode   gm2 ON g2.game_mode = gm2.id
                                                      WHERE gc2.contestant_id = gc.contestant_id
                                                        AND gm2.id = gm.id
                                                        AND gc2.deleted = FALSE
                                                        AND g2.start >= MAKE_DATE( 2024, 1, 1 )
                                                      ORDER BY gc2.game_mode_score DESC,
                                                               gc2.correct_time
                                                      LIMIT 1 ) )
      AND ( p.original_name IS NULL OR p.original_name !~~ 'Guest-%'::TEXT )
      AND NOT ( EXISTS (
                       SELECT NULL::TEXT AS text
                       FROM player               p2
                           JOIN team_player_link tpl ON p2.id = tpl.player_id
                       WHERE tpl.team_id = t.id
                         AND p2.original_name ~~ 'Guest-%'::TEXT ) )
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
             gc.contestant_id,
             gc.game_mode_score,
             gc.correct_time;

ALTER TABLE leaderboard_speedrun
    OWNER TO postgres;

GRANT SELECT ON leaderboard_speedrun TO martinch;

