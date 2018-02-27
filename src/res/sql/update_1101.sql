BEGIN TRANSACTION;
ALTER TABLE GameEntry ADD COLUMN sorting_name text;
ALTER TABLE GameEntry ADD COLUMN alternative_names text;
INSERT OR REPLACE INTO Platform(id,name_key,is_pc,default_supported_extensions,igdb_id) VALUES (21,"microsoft_store",1,"",6);
INSERT OR REPLACE INTO Platform(id,name_key,is_pc,default_supported_extensions,igdb_id) VALUES (22,"3ds",0,"3ds,3dsx,elf,axf,cci,cxi,app",37);
INSERT OR REPLACE INTO Emulator(id,name,default_path,default_args_schema) VALUES (10,"Citra", "C:\Program Files\Citra\citra-qt.exe","%p");
INSERT OR REPLACE INTO emulates(emu_id,platform_id) VALUES(10,22);
COMMIT;