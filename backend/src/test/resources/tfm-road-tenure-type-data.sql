DELETE FROM tfm.tfm_vt_road_tenure_type;

-- Stored out of order, with a retired variant, padding and LRMOPS's '!', as the table holds them.
INSERT INTO tfm.tfm_vt_road_tenure_type VALUES ('RP', 'Road Permit');
INSERT INTO tfm.tfm_vt_road_tenure_type VALUES ('RP-RETIRE', 'Retired Road Permit');
INSERT INTO tfm.tfm_vt_road_tenure_type VALUES ('FSR ', ' Forest Service Road ');
INSERT INTO tfm.tfm_vt_road_tenure_type VALUES ('OTH', NULL);
INSERT INTO tfm.tfm_vt_road_tenure_type VALUES ('!', 'Geometry only');
