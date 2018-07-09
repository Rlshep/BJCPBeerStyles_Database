DROP TABLE fts_search;
CREATE VIRTUAL TABLE fts_search USING fts4(result_id, table_name, language, revision, body);
INSERT INTO fts_search ( result_id ,table_name ,LANGUAGE ,revision ,body ) SELECT C._ID ,'CATEGORY' ,C.LANGUAGE ,C.revision ,C.NAME FROM CATEGORY C WHERE C.LANGUAGE = 'en' AND C.revision = 2015;
INSERT INTO fts_search (result_id ,table_name ,LANGUAGE ,revision ,body ) SELECT c._id ,'CATEGORY' ,c.LANGUAGE ,c.revision ,s.body FROM CATEGORY C JOIN SECTION S ON S.CATEGORY_ID = C._ID WHERE C.LANGUAGE = 'en' AND C.revision = 2015;
