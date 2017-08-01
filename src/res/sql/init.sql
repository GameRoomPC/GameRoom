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
    igdb_id integer,
	name_key text unique,
	id_needs_update integer default 0
);

CREATE TABLE IF NOT EXISTS develops (
	game_id integer,
	dev_id integer,
	FOREIGN KEY(game_id) REFERENCES GameEntry(id) ON DELETE CASCADE,
	FOREIGN KEY(dev_id) REFERENCES Company(id) ON DELETE CASCADE,
	PRIMARY KEY (game_id, dev_id)
);

CREATE TABLE IF NOT EXISTS publishes (
	game_id integer,
	pub_id integer,
	FOREIGN KEY(game_id) REFERENCES GameEntry(id) ON DELETE CASCADE,
	FOREIGN KEY(pub_id) REFERENCES Company(id) ON DELETE CASCADE,
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
	FOREIGN KEY(game_id) REFERENCES GameEntry(id) ON DELETE CASCADE,
	FOREIGN KEY(serie_id) REFERENCES Serie(id) ON DELETE CASCADE,
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
	FOREIGN KEY(game_id) REFERENCES GameEntry(id) ON DELETE CASCADE,
	FOREIGN KEY(genre_id) REFERENCES GameGenre(id) ON DELETE CASCADE,
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
	FOREIGN KEY(game_id) REFERENCES GameEntry(id) ON DELETE CASCADE,
	FOREIGN KEY(theme_id) REFERENCES GameTheme(id) ON DELETE CASCADE,
	PRIMARY KEY (game_id, theme_id)
);

CREATE TABLE IF NOT EXISTS PlaySession (
	id integer PRIMARY KEY AUTOINCREMENT,
	start_date datetime,
	time_played_seconds integer,
	game_id integer,
	FOREIGN KEY(game_id) REFERENCES GameEntry(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS Platform (
	id integer PRIMARY KEY,
	igdb_id integer default -1,
	name_key text,
	default_supported_extensions text,
	supported_extensions text,
	is_pc integer default 0
);

CREATE TABLE IF NOT EXISTS runs_on (
	platformGameId integer,
	platform_id integer,
	game_id integer unique,
	FOREIGN KEY(platform_id) REFERENCES Platform(id) ON DELETE CASCADE,
	FOREIGN KEY(game_id) REFERENCES GameEntry(id) ON DELETE CASCADE,
	PRIMARY KEY (platform_id, game_id)
);

CREATE TABLE IF NOT EXISTS Emulator (
	id integer PRIMARY KEY,
	name text,
    default_path text,
	path text,
	default_args_schema text
);

CREATE TABLE IF NOT EXISTS emulates (
	platform_id integer,
	emu_id integer,
	user_choice integer default 0,
	args_schema text,
	FOREIGN KEY(platform_id) REFERENCES Platform(id) ON DELETE CASCADE,
	FOREIGN KEY(emu_id) REFERENCES Emulator(id) ON DELETE CASCADE,
	PRIMARY KEY (platform_id, emu_id)
);

CREATE TABLE IF NOT EXISTS Settings (
	id text PRIMARY KEY,
	value text
);

CREATE TABLE IF NOT EXISTS GameFolder (
    id integer PRIMARY KEY AUTOINCREMENT,
	path text,
	platform_id integer default -2,
	FOREIGN KEY(platform_id) REFERENCES Platform(id) ON DELETE CASCADE
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
	
INSERT OR REPLACE INTO Platform(id,name_key,is_pc,default_supported_extensions,igdb_id) VALUES
	(-2,"pc",1,"exe,lnk",-1,6),
	(1,"steam",1,"exe,jar,lnk",-1),
	(2,"steam_online",1,"exe,lnk",-1),
	(3,"origin",1,"exe,lnk",-1),
	(4,"uplay",1,"exe,lnk",-1),
	(5,"battlenet",1,"exe,lnk",-1),
	(6,"gog",1,"exe,lnk",-1),
	(7,"wii",0,"ciso,iso,wbfs",5),
	(8,"gamecube",0,"iso",21),
	(9,"n64",0,"n64",4),
	(10,"ps2",0,"elf,gz,iso",8),
	(11,"ps3",0,"iso,pkg,bin,elf",9),
	(12,"wiiu",0,"iso,rpx,wud,wux",41),
	(13,"gb",0,"gb",33),
	(14,"gbc",0,"gc",22),
	(15,"gba",0,"gba",24),
	(16,"nes",0,"nes",18),
	(17,"snes",0,"sfc,smc",19),
	(18,"psx",0,"bin,iso,img,cue,ccd,mds,pbp,ecm",7),
	(19,"psp",0,"iso,cso,pbp,elf,prx",38);
	
INSERT OR IGNORE INTO Emulator(id,name,default_path,default_args_schema) VALUES
	(1,"Dolphin", "C:\Program Files\Dolphin\Dolphin.exe","/b /e %a %p"),
	(2,"PCSX2", "C:\Program Files (x86)\PCSX2 1.4.0\pcsx2.exe","--fullscreen --nogui %a %p"),
	(3,"cemu", "C:\Program Files (x86)\cemu\Cemu.exe","-f -g %a %p"),
	(4,"Project64","C:\Program Files (x86)\Project64 2.3\Project64.exe","%p"),
	(5,"RetroArch","C:\Program Files (x86)\retroarch.exe","-f -L ""path/to/core"" %a %p"),
	(6,"RPCS3","C:\Program Files (x86)\rpcs3.exe","%a %p"),
	(7,"PPSSPP","C:\Program Files (x86)\PPSSPP\PPSSPPWindows64.exe","%a %p"),
	(8,"ePSXe","C:\Program Files (x86)\ePSXe205\ePSXe.exe","-nogui -loadbin %a %p");

INSERT OR IGNORE INTO emulates(emu_id,platform_id) VALUES
	(1,7),
	(1,8),
	(2,10),
	(3,12),
	(4,9),
	(5,7),
	(5,8),
	(5,9),
	(5,13),
	(5,14),
	(5,15),
	(5,16),
	(5,17),
	(5,18),
	(5,19),
	(6,11),
	(7,19),
	(8,18);
	
	