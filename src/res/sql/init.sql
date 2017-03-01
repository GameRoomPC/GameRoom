CREATE TABLE IF NOT EXISTS GameEntry (
	id integer PRIMARY KEY AUTOINCREMENT,
	name text,
	release_date datetime,
	description text,
	aggregated_rating real,
	path text,
	cmd_before text,
	cmd_after text,
	launch_args text,
	yt_hash text,
	added_date datetime,
	last_played_date datetime,
	initial_playtime integer,
	installed integer default 1,
	cover_hash text,
	wp_hash text,
	igdb_id integer,
	waiting_scrap integer default 0,
	toAdd integer default 0,
	beingScraped integer default 0
);

CREATE TABLE IF NOT EXISTS Developer (
	igdb_id integer PRIMARY KEY,
	name text
);

CREATE TABLE IF NOT EXISTS Publisher (
	igdb_id integer PRIMARY KEY,
	name text
);

CREATE TABLE IF NOT EXISTS GameGenre (
	igdb_id integer PRIMARY KEY,
	name_key text
);

CREATE TABLE IF NOT EXISTS GameTheme (
	igdb_id integer PRIMARY KEY,
	name_key text
);

CREATE TABLE IF NOT EXISTS Serie (
	igdb_id integer PRIMARY KEY,
	name text
);

CREATE TABLE IF NOT EXISTS PlaySession (
	id integer PRIMARY KEY AUTOINCREMENT,
	start_date datetime,
	time_played_seconds integer
);

CREATE TABLE IF NOT EXISTS develops (
	game_id integer,
	dev_id integer,
	FOREIGN KEY(game_id) REFERENCES GameEntry(id),
	FOREIGN KEY(dev_id) REFERENCES Developer(igdb_id),
	PRIMARY KEY (game_id, dev_id)
);

CREATE TABLE IF NOT EXISTS publishes (
	game_id integer,
	pub_id integer,
	FOREIGN KEY(game_id) REFERENCES GameEntry(id),
	FOREIGN KEY(pub_id) REFERENCES Publisher(igdb_id),
    PRIMARY KEY (game_id, pub_id)
);

CREATE TABLE IF NOT EXISTS has_genre (
	game_id integer,
	genre_id integer,
	FOREIGN KEY(game_id) REFERENCES GameEntry(id),
	FOREIGN KEY(genre_id) REFERENCES GameGenre(igdb_id),
	PRIMARY KEY (game_id, genre_id)
);

CREATE TABLE IF NOT EXISTS has_theme (
	game_id integer,
	theme_id integer,
	FOREIGN KEY(game_id) REFERENCES GameEntry(id),
	FOREIGN KEY(theme_id) REFERENCES GameTheme(igdb_id),
	PRIMARY KEY (game_id, theme_id)
);

CREATE TABLE IF NOT EXISTS regroups (
	game_id integer,
	serie_id integer,
	FOREIGN KEY(game_id) REFERENCES GameEntry(id),
	FOREIGN KEY(serie_id) REFERENCES Serie(igdb_id),
	PRIMARY KEY (game_id, serie_id)
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
	name_key text
);

CREATE TABLE IF NOT EXISTS Emulator (
	id integer PRIMARY KEY,
	name text,
	path text,
	default_args_schema text,
	args_schema text
);

CREATE TABLE IF NOT EXISTS emulates (
	platform_id integer,
	emu_id integer,
	userchoice integer default 0,
	FOREIGN KEY(platform_id) REFERENCES Platform(id),
	FOREIGN KEY(emu_id) REFERENCES Emulator(id),
	PRIMARY KEY (platform_id, emu_id)
);

CREATE TABLE IF NOT EXISTS runs_on (
	specific_id integer,
	platform_id integer,
	game_id integer,
	FOREIGN KEY(platform_id) REFERENCES Platform(id),
	FOREIGN KEY(game_id) REFERENCES GameEntry(id),
	PRIMARY KEY (platform_id, game_id)
);

CREATE TABLE IF NOT EXISTS Settings (
	id integer,
	locale text,
	tileZoom real default 0.365,
	onGameLaunchAction text,
	fullScreen integer default 0,
	windowWidth integer,
	windowHeight integer,
	gamingPowerMode text,
	enableGamingPowerMode integer default 0,
	noNotifications integer default 0,
	noToasts integer default 0,
	startMinimized integer default 0,
	windowMaximized integer default 1,
	hideToolBar integer default 0,
	hideTilesRows integer default 0,
	enableStaticWallpaper integer default 0,
	startWithWindows integer default 0,
	noMoreIconTrayWarning integer default 0,
	enableXboxControllerSupport integer default 0,
	disableMainSceneWallpaper integer default 0,
	disableScrollbarFullScreen integer default 1,
	disableGameMainTheme integer default 1,
	advancedMode integer default 0,
	debugMode integer default 0,
	foldedRowLastPlay integer default 0,
	foldedRowRecentlyAdded integer default 0,
	foldedToAddRow integer default 0,
	cmd_before text,
	cmd_after text,
	displayWelcomeMessage integer default 1,
	supporterKey text,
	steamProfile text,
	scrollbarVValue real default 0.0,
	uiscale text,
	theme text,
	lastSupportMessage datetime,
	lastUpdateCheck datetime,
	drawerMenuWidth real default 0.0
);

CREATE TABLE IF NOT EXISTS GamesFolder (
	id integer PRIMARY KEY AUTOINCREMENT,
	path text
);

CREATE TABLE IF NOT EXISTS IgnoredSteamApps (
	steam_id integer PRIMARY KEY,
	name text
);

CREATE TABLE IF NOT EXISTS IgnoredFiles (
	id integer PRIMARY KEY AUTOINCREMENT,
	path text
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
	
INSERT OR REPLACE INTO Platform(id,name_key) VALUES
	(1,"steam"),
	(2,"steam_online"),
	(3,"origin"),
	(4,"uplay"),
	(5,"battle_net"),
	(6,"gog"),
	(7,"wii"),
	(8,"gamecube"),
	(9,"n64");
	
INSERT OR REPLACE INTO Emulator(id,name,path,default_args_schema) VALUES
	(1,"Dolphin", "C:\Program Files\Dolphin\Dolphin.exe","/b /e %p");
	
INSERT OR REPLACE INTO emulates(platform_id, emu_id) VALUES
	(1,7),
	(1,8);
	
	