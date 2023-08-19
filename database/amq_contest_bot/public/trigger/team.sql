CREATE TRIGGER after_insert_trigger_on_team
    AFTER INSERT
    ON team
    FOR EACH ROW
EXECUTE PROCEDURE new_team( );

