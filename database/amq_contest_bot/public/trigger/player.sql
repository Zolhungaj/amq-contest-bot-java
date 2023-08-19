CREATE TRIGGER after_insert_trigger_on_player
    AFTER INSERT
    ON player
    FOR EACH ROW
EXECUTE PROCEDURE new_player( );

