import psycopg2
import psycopg2.extensions
import sqlite3
import argparse


class OldPlayer:
    def __init__(self, player_id: int, truename: str):
        self.player_id = player_id
        self.truename = truename

    def __str__(self):
        return f"OldPlayer({self.player_id}, {self.truename})"

    def __repr__(self):
        return self.__str__()


class NewPlayer:
    def __init__(self, player_id: int, original_name: str):
        self.id = player_id
        self.original_name = original_name

    def __str__(self):
        return f"NewPlayer({self.id}, {self.original_name})"

    def __repr__(self):
        return self.__str__()


class PlayerMap:
    """Maps old players to new players and vice versa, through their common key, the player name."""

    def __init__(self):
        self.old_player_id_to_name: dict[int, str] = dict()
        self.name_to_old_player_id: dict[str, int] = dict()
        self.old_player_id_to_old_player: dict[int, OldPlayer] = dict()

        self.new_player_id_to_name: dict[int, str] = dict()
        self.name_to_new_player_id: dict[str, int] = dict()
        self.new_player_id_to_new_player: dict[int, NewPlayer] = dict()

    def add_old_player(self, old_player: OldPlayer):
        self.old_player_id_to_name[old_player.player_id] = old_player.truename
        self.name_to_old_player_id[old_player.truename] = old_player.player_id
        self.old_player_id_to_old_player[old_player.player_id] = old_player

    def add_new_player(self, new_player: NewPlayer):
        self.new_player_id_to_name[new_player.id] = new_player.original_name
        self.name_to_new_player_id[new_player.original_name] = new_player.id
        self.new_player_id_to_new_player[new_player.id] = new_player

    def get_old_player(self, new_player: NewPlayer) -> OldPlayer | None:
        return self.old_player_id_to_old_player[self.name_to_old_player_id[self.new_player_id_to_name[new_player.id]]]

    def get_unmapped_old_players(self) -> list[OldPlayer]:
        return [old_player
                for old_player
                in self.old_player_id_to_old_player.values()
                if old_player.truename not in self.new_player_id_to_name.values()]

    def old_player_id_to_new_player_id(self, old_player_id: int) -> int:
        return self.name_to_new_player_id[self.old_player_id_to_name[old_player_id]]

    def get_list(self) -> list[tuple[int, str, int]]:
        names = set(self.name_to_old_player_id.keys()).union(set(self.name_to_new_player_id.keys()))
        print(names)
        return [(self.name_to_old_player_id.get(name), name, self.name_to_new_player_id.get(name)) for name in names]


def main(sqlite3_database_filename: str,
         postgres_host: str,
         postgres_port: int,
         postgres_database_name: str,
         postgres_username: str,
         postgres_password: str):
    (sqlite3_connection, postgres_connection) = setup_connections(sqlite3_database_filename,
                                                                  postgres_host,
                                                                  postgres_port,
                                                                  postgres_database_name,
                                                                  postgres_username,
                                                                  postgres_password)
    player_map: PlayerMap = setup_players(sqlite3_connection, postgres_connection)
    print(player_map.get_list())
    # todo: map bannned to ban
    # todo: map player to contestant
    # todo: map game to game
    # todo: map gameplayer to game_contestant
    # todo:


def setup_connections(
        sqlite3_database_filename: str,
        postgres_host: str,
        postgres_port: int,
        postgres_database_name: str,
        postgres_username: str,
        postgres_password: str) -> tuple[sqlite3.Connection, psycopg2.extensions.connection]:
    sqlite3_connection: sqlite3.Connection = sqlite3.connect(sqlite3_database_filename)
    postgres_connection: psycopg2.extensions.connection = psycopg2.connect(
        f"dbname={postgres_database_name} \
        user={postgres_username} \
        password={postgres_password} \
        host={postgres_host} \
        port={postgres_port}")
    return sqlite3_connection, postgres_connection


def setup_players(sqlite3_connection: sqlite3.Connection, postgres_connection: psycopg2.extensions.connection) -> PlayerMap:
    player_map: PlayerMap = PlayerMap()
    result = sqlite3_connection.execute("SELECT player_id, truename from player").fetchall()
    for (player_id, truename) in result:
        player_map.add_old_player(OldPlayer(player_id, truename))
    postgres_cursor = postgres_connection.cursor()
    postgres_cursor.execute("SELECT id, original_name from player")
    result2 = postgres_cursor.fetchall()
    for (player_id, original_name) in result2:
        player_map.add_new_player(NewPlayer(player_id, original_name))
    insert_unmapped_players(player_map, postgres_connection)
    return player_map


def insert_unmapped_players(player_map: PlayerMap, postgres_connection: psycopg2.extensions.connection):
    unmapped_players = player_map.get_unmapped_old_players()
    postgres_cursor = postgres_connection.cursor()
    for unmapped_player in unmapped_players:
        postgres_cursor.execute("INSERT INTO player (original_name) VALUES (%s)", (unmapped_player.truename,))
    postgres_connection.commit()
    postgres_cursor.execute("SELECT id, original_name from player")
    result = postgres_cursor.fetchall()
    for (player_id, original_name) in result:
        player_map.add_new_player(NewPlayer(player_id, original_name))
    assert len(player_map.get_unmapped_old_players()) == 0


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Migrate the database from SQLite3 to PostgreSQL.")
    parser.add_argument("sqlite3_database_filename",
                        type=str,
                        help="The filename of the SQLite3 database.")
    parser.add_argument("postgres_host", type=str, help="The url of the Postgres host.")
    parser.add_argument("postgres_port", type=int, help="The port of the Postgres host.")
    parser.add_argument("--postgres_database_name",
                        type=str,
                        help="The name of the Postgres database.",
                        default="amq_contest_bot")
    parser.add_argument("--postgres_username",
                        type=str,
                        help="The name of the Postgres user.",
                        default="amq_contest_bot")
    parser.add_argument("--postgres_password", type=str, help="The password of the Postgres user.", required=True)
    print(parser.parse_args())
    parsedArgs: argparse.Namespace = parser.parse_args()
    main(parsedArgs.sqlite3_database_filename,
         parsedArgs.postgres_host,
         parsedArgs.postgres_port,
         parsedArgs.postgres_database_name,
         parsedArgs.postgres_username,
         parsedArgs.postgres_password
         )
