BEGIN TRANSACTION;
pragma foreign_keys = OFF;
ALTER TABLE GameEntry RENAME TO temp_GameEntry;

CREATE TABLE IF NOT EXISTS GameEntry (
	id integer PRIMARY KEY AUTOINCREMENT,
	name text,
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
	runAsAdmin integer default 0,
	sorting_name text,
	alternative_names text,
	monitor_process text
);

INSERT INTO GameEntry
SELECT
 id, name, release_date,description,aggregated_rating,path,cmd_before,cmd_after,launch_args,yt_hash,added_date,
	last_played_date,
	cover_hash ,
	initial_playtime ,
	wp_hash ,
	igdb_id ,
	installed ,
	waiting_scrap,
	toAdd,
	ignored ,
	runAsAdmin,
	null,
	null,
	null
FROM
 temp_GameEntry;

DROP TABLE temp_GameEntry;
pragma foreign_keys = ON;

INSERT OR REPLACE INTO Platform(id,name_key,is_pc,default_supported_extensions,igdb_id) VALUES (21,"microsoft_store",1,"exe",6);
INSERT OR REPLACE INTO Platform(id,name_key,is_pc,default_supported_extensions,igdb_id) VALUES (22,"3ds",0,"3ds,3dsx,elf,axf,cci,cxi,app",37);
INSERT OR REPLACE INTO Emulator(id,name,default_path,default_args_schema) VALUES (10,"Citra", "C:\Program Files\Citra\citra-qt.exe","%p");
INSERT OR REPLACE INTO emulates(emu_id,platform_id) VALUES(10,22);
INSERT OR REPLACE INTO Platform(id,name_key,is_pc,default_supported_extensions,igdb_id) VALUES (23,"ds",0,"nds,zip,7z,rar,gz",20);
INSERT OR REPLACE INTO Emulator(id,name,default_path,default_args_schema) VALUES (11,"DeSmuME", "C:\Program Files\desmume-0.9.11-win32\DeSmuME_0.9.11_x86.exe","%p");
INSERT OR REPLACE INTO emulates(emu_id,platform_id) VALUES(11,23),(5,23);
COMMIT;