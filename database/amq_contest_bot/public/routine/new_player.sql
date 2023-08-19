CREATE FUNCTION new_player( ) RETURNS trigger
    LANGUAGE plpgsql
AS
$$
    BEGIN
       insert into contestant(player_id,type)
        values(NEW.id, 'PLAYER');
       return NEW;
    end;
    $$;

ALTER FUNCTION new_player() OWNER TO postgres;

