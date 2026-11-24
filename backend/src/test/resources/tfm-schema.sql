-- The columns of LRMOPS's TFM.TFM_ROAD that the API reads, for H2 in tests. The real table is an
-- ArcSDE feature table with 31 columns and no key on road and section.
CREATE SCHEMA IF NOT EXISTS tfm;

CREATE TABLE IF NOT EXISTS tfm.tfm_road (
  objectid       NUMBER(38) NOT NULL,
  road_id        VARCHAR2(10),
  section_id     VARCHAR2(30),
  road_name      VARCHAR2(50),
  roadtenuretype VARCHAR2(10),
  roadstatus     VARCHAR2(20),
  client_name    VARCHAR2(60),
  org_unit_no    NUMBER(10),
  unitcode       VARCHAR2(10),
  bctsoffice     VARCHAR2(10),
  roadlength     NUMBER
);

-- TFM.TFM_VT_ROAD_TENURE_TYPE, the code table behind TFM_ROAD.ROADTENURETYPE: the two columns the
-- API reads.
CREATE TABLE IF NOT EXISTS tfm.tfm_vt_road_tenure_type (
  roadtenuretype VARCHAR2(10) NOT NULL,
  description    VARCHAR2(100)
);
