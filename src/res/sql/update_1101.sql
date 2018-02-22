BEGIN TRANSACTION;
ALTER TABLE GameEntry ADD COLUMN sorting_name text;
ALTER TABLE GameEntry ADD COLUMN alternative_names text;
INSERT OR REPLACE INTO Platform(id,name_key,is_pc,default_supported_extensions,igdb_id) VALUES (21,"microsoft_store",1,"",6);
COMMIT;