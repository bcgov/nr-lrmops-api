DELETE FROM tfm.tfm_road;

-- (objectid, road_id, section_id, road_name, roadtenuretype, roadstatus, client_name,
--  org_unit_no, unitcode, bctsoffice)

-- A retired road permit, stored with LRMOPS's leading and double spaces, with a BCTS office.
INSERT INTO tfm.tfm_road VALUES (1, 'R08317', 'I', '   AM  4', 'RP-RETIRE', NULL, 'CANFOR', 18, 'DPG', 'TPG');
-- One section across two districts: two rows, one pair.
INSERT INTO tfm.tfm_road VALUES (2, 'R07571', 'AA', 'BIG CREEK FSR', 'FSR', NULL, NULL, 19, 'DMH', NULL);
INSERT INTO tfm.tfm_road VALUES (3, 'R07571', 'AA', 'BIG CREEK FSR', 'FSR', NULL, NULL, 18, 'DPG', NULL);
-- An edit in progress: the OLDGEOMETRY row counts, the NEWGEOMETRY row does not.
INSERT INTO tfm.tfm_road VALUES (4, '9456', '01', 'CEDAR ROAD', 'RP', 'OLDGEOMETRY', 'WEST FRASER MILLS LTD.', 18, 'DPG', NULL);
INSERT INTO tfm.tfm_road VALUES (5, '9456', '01', 'CEDAR ROAD NEW', 'RP', 'NEWGEOMETRY', 'WEST FRASER MILLS LTD.', 19, 'DMH', NULL);
-- Only in a NEWGEOMETRY row: never returned.
INSERT INTO tfm.tfm_road VALUES (6, '9999', '01', 'GHOST ROAD', 'RP', 'NEWGEOMETRY', NULL, 18, 'DPG', NULL);
-- A literal percent sign and underscore in the name; a lower-case section id with a space.
INSERT INTO tfm.tfm_road VALUES (7, 'S00100', 'a 1', '100% DOWN_HILL', 'SUP', NULL, 'SMITH', 20, 'DQC', NULL);
-- No name, no district.
INSERT INTO tfm.tfm_road VALUES (8, 'R00001', 'B', NULL, 'OTH', NULL, NULL, NULL, NULL, NULL);
-- No section: can't be asked for, never returned.
INSERT INTO tfm.tfm_road VALUES (9, 'R00002', NULL, 'NO SECTION', 'RP', NULL, NULL, 20, 'DQC', NULL);
