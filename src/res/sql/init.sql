CREATE TABLE IF NOT EXISTS GameEntry (
	id integer PRIMARY KEY AUTOINCREMENT,
	name text unique,
	release_date datetime,
	description text,
	aggregated_rating integer,
	path text,
	cmd_before text,
	cmd_after text,
	launch_args text,
	yt_hash text,
	added_date datetime,
	last_played_date datetime,
	initial_playtime integer,
	cover_hash text,
	wp_hash text,
	igdb_id integer,
	installed integer default 1,
	waiting_scrap integer default 0,
	toAdd integer default 0,
	ignored integer default 0,
	runAsAdmin integer default 0
);

CREATE TABLE IF NOT EXISTS Company (
    id integer PRIMARY KEY AUTOINCREMENT,
    igdb_id integer unique,
	name_key text unique,
	id_needs_update integer default 0
);

CREATE TABLE IF NOT EXISTS develops (
	game_id integer,
	dev_id integer,
	FOREIGN KEY(game_id) REFERENCES GameEntry(id),
	FOREIGN KEY(dev_id) REFERENCES Company(id),
	PRIMARY KEY (game_id, dev_id)
);

CREATE TABLE IF NOT EXISTS publishes (
	game_id integer,
	pub_id integer,
	FOREIGN KEY(game_id) REFERENCES GameEntry(id),
	FOREIGN KEY(pub_id) REFERENCES Company(id),
    PRIMARY KEY (game_id, pub_id)
);

CREATE TABLE IF NOT EXISTS Serie (
    id integer PRIMARY KEY AUTOINCREMENT,
    igdb_id integer unique,
	name_key text unique,
    id_needs_update integer default 0
);

CREATE TABLE IF NOT EXISTS regroups (
	game_id integer,
	serie_id integer,
	FOREIGN KEY(game_id) REFERENCES GameEntry(id),
	FOREIGN KEY(serie_id) REFERENCES Serie(id),
	PRIMARY KEY (game_id, serie_id)
);

CREATE TABLE IF NOT EXISTS GameGenre (
    id integer PRIMARY KEY AUTOINCREMENT,
    igdb_id integer unique,
	name_key text,
	id_needs_update integer default 0
);

CREATE TABLE IF NOT EXISTS has_genre (
	game_id integer,
	genre_id integer,
	FOREIGN KEY(game_id) REFERENCES GameEntry(id),
	FOREIGN KEY(genre_id) REFERENCES GameGenre(id),
	PRIMARY KEY (game_id, genre_id)
);

CREATE TABLE IF NOT EXISTS GameTheme (
    id integer PRIMARY KEY AUTOINCREMENT,
    igdb_id integer unique,
	name_key text,
	id_needs_update integer default 0
);

CREATE TABLE IF NOT EXISTS has_theme (
	game_id integer,
	theme_id integer,
	FOREIGN KEY(game_id) REFERENCES GameEntry(id),
	FOREIGN KEY(theme_id) REFERENCES GameTheme(id),
	PRIMARY KEY (game_id, theme_id)
);

CREATE TABLE IF NOT EXISTS PlaySession (
	id integer PRIMARY KEY AUTOINCREMENT,
	start_date datetime,
	time_played_seconds integer
);

CREATE TABLE IF NOT EXISTS played (
	game_id integer,
	play_session_id integer,
	FOREIGN KEY(game_id) REFERENCES GameEntry(id),
	FOREIGN KEY(play_session_id) REFERENCES PlaySession(id),
	PRIMARY KEY (game_id, play_session_id)
);

CREATE TABLE IF NOT EXISTS Platform (
	id integer PRIMARY KEY,
	name_key text,
	igdb_id integer unique,
	is_pc integer default 0
);

CREATE TABLE IF NOT EXISTS runs_on (
	platformGameId integer,
	platform_id integer,
	game_id integer unique,
	FOREIGN KEY(platform_id) REFERENCES Platform(id),
	FOREIGN KEY(game_id) REFERENCES GameEntry(id),
	PRIMARY KEY (platform_id, game_id)
);

CREATE TABLE IF NOT EXISTS Emulator (
	id integer PRIMARY KEY,
	name text,
    default_path text,
	path text,
	default_args_schema text,
	args_schema text
);

CREATE TABLE IF NOT EXISTS emulates (
	platform_id integer,
	emu_id integer,
	user_choice integer default 0,
	FOREIGN KEY(platform_id) REFERENCES Platform(id),
	FOREIGN KEY(emu_id) REFERENCES Emulator(id),
	PRIMARY KEY (platform_id, emu_id)
);

CREATE TABLE IF NOT EXISTS Settings (
	id text PRIMARY KEY,
	value text
);

CREATE TABLE IF NOT EXISTS GameFolder (
	path text PRIMARY KEY
);

CREATE TABLE IF NOT EXISTS GameScanners (
	id integer PRIMARY KEY,
	name_key text,
	enabled integer default 0
);

INSERT OR REPLACE INTO GameTheme(igdb_id,name_key) VALUES 
	(1,"action"),
	(17,"fantasy"),
	(18,"science_fiction"),
	(19,"horror"),
	(20,"thriller"),
	(21,"survival"),
	(22,"historical"),
	(23,"stealth"),
	(27,"comedy"),
	(28,"business"),
	(31,"drama"),
	(32,"non_fiction"),
	(33,"sandbox"),
	(34,"educational"),
	(35,"kids"),
	(38,"open_world"),
	(39,"warfare"),
	(40,"party"),
	(41,"4x"),
	(42,"erotic"),
	(43,"mystery");
	
INSERT OR REPLACE INTO GameGenre(igdb_id,name_key) VALUES 
	(2,"point_and_click"),
	(4,"fighting"),
	(5,"fps"),
	(7,"music"),
	(8,"platform"),
	(9,"puzzle"),
	(10,"racing"),
	(11,"rts"),
	(12,"rpg"),
	(13,"simulator"),
	(14,"sport"),
	(15,"strategy"),
	(16,"tbs"),
	(24,"tactical"),
	(25,"hack_and_slash"),
	(26,"quiz"),
	(30,"pinball"),
	(31,"adventure"),
	(32,"indie"),
	(33,"arcade");
	
INSERT OR IGNORE INTO Platform(id,name_key,is_pc) VALUES
	(1,"steam",1),
	(2,"steam_online",1),
	(3,"origin",1),
	(4,"uplay",1),
	(5,"battlenet",1),
	(6,"gog",1),
	(7,"wii",0),
	(8,"gamecube",0),
	(9,"n64",0),
	(10,"ps2",0);
	
INSERT OR IGNORE INTO Emulator(id,name,default_path,default_args_schema) VALUES
	(1,"Dolphin", "C:\Program Files\Dolphin\Dolphin.exe","/b /e %p"),
	(2,"PCSX2", "C:\Program Files (x86)\PCSX2 1.4.0\pcsx2.exe","--fullscreen --nogui %p");

INSERT OR IGNORE INTO emulates(platform_id, emu_id) VALUES
	(7,1),
	(8,1),
	(10,2);
	
	