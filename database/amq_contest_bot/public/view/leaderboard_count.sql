CREATE VIEW leaderboard_count ( contestant_id, game_mode_score, ruleset, team_size, times_achieved, earliest_achieved )
AS
    SELECT gc.contestant_id,
       gc.game_mode_score,
       gm.ruleset,
       gm.team_size,
       count(*)     AS times_achieved,
       min(g.start) AS earliest_achieved
FROM game_contestant gc
         JOIN game g ON gc.game_id = g.id
         JOIN game_mode gm ON g.game_mode = gm.id
WHERE gm.scoring_type = 'COUNT'::text
  AND gc.game_mode_score = ((SELECT max(gc2.game_mode_score) AS max
                             FROM game_contestant gc2
                                      JOIN game g2 ON gc2.game_id = g2.id
                                      JOIN game_mode gm2 ON g2.game_mode = gm2.id
                             WHERE gc2.contestant_id = gc.contestant_id
                               AND gm2.scoring_type = gm.scoring_type
                               AND gm2.ruleset = gm.ruleset
                               AND gm2.team_size = gm.team_size))
GROUP BY gm.ruleset, gm.team_size, gc.contestant_id, gc.game_mode_score;

ALTER TABLE leaderboard_count
    OWNER TO postgres;

