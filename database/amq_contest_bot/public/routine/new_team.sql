CREATE FUNCTION new_team( ) RETURNS trigger
    LANGUAGE plpgsql
AS
$$
    BEGIN
       insert into contestant(team_id, type)
        values(NEW.id, 'TEAM');
       return NEW;
    end;
    $$;

ALTER FUNCTION new_team() OWNER TO postgres;

