LRMOPS Oracle Database Deployment
=====================================
DATE CREATED: 2026-11-12
AUTHOR: SodhiA1
TARGET: DBQ05 (test), then DBP05 (production)

INSTRUCTIONS:

  1) Position yourself in the directory the zip file was uncompressed to

    cd <dir_zip_file_uncompressed_to>

  2) Log in to database as DBA, via SQL*Plus.

    Execute the SET statements and start spooling before running the scripts

    $ sqlplus DBA/password@Target

    SET DEFINE OFF
    SET TRIMSPOOL ON
    SET SQLBL ON

    SPOOL LRMOPS_API_DBA_1.log

  3) From the SQL Prompt, run the following scripts in order:

    @DBA/ROLES/V999999999999.1__FSA_LRM_READ_WRITE_ROLE.sql

    SET DEFINE ON
    @DBA/USERS/V999999999999.2__PROXY_FSA_LRM_READ_WRITE_USER.sql

    SET DEFINE OFF
    @DBA/SYS_GRANTS/V999999999999.3__PROXY_FSA_LRM_READ_WRITE_USER.sql

  4) Exit SQL*Plus

    SPOOL OFF
    EXIT


  5) In the case a rollback is required run the following scripts in reverse order
    starting with the version number prior to the failing script. For example,
    if V100.4 fails, run U100.3 then U100.2 then U100.1 :

    @DBA/SYS_GRANTS/U999999999999.3__PROXY_FSA_LRM_READ_WRITE_USER.sql

    SET DEFINE ON
    @DBA/USERS/U999999999999.2__PROXY_FSA_LRM_READ_WRITE_USER.sql

    SET DEFINE OFF
    @DBA/ROLES/U999999999999.1__FSA_LRM_READ_WRITE_ROLE.sql

  6) Log in to database as TFM, via SQL*Plus.

    Execute the SET statements and start spooling before running the scripts

    $ sqlplus TFM/password@Target

    SET DEFINE OFF
    SET TRIMSPOOL ON
    SET SQLBL ON

    SPOOL LRMOPS_API_TFM_1.log

  7) From the SQL Prompt, run the following scripts in order:

    @TFM/GRANTS/V999999999999.4__FSA_LRM_READ_WRITE_ROLE.sql

  8) Exit SQL*Plus

    SPOOL OFF
    EXIT


  9) In the case a rollback is required run the following scripts in reverse order
    starting with the version number prior to the failing script. For example,
    if V100.4 fails, run U100.3 then U100.2 then U100.1 :

    @TFM/GRANTS/U999999999999.4__FSA_LRM_READ_WRITE_ROLE.sql
