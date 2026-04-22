-- Create a federated connection straight to the Operational PostgreSQL database!
CREATE DATABASE IF NOT EXISTS ext_postgres
ENGINE = PostgreSQL('telecom_postgres:5432', 'telecom_db', 'telecom_user', 'telecom_password', 'public');

-- ETL: Dimensions
TRUNCATE TABLE IF EXISTS dwh.dim_cell_tower;
INSERT INTO dwh.dim_cell_tower
SELECT 
    id AS cell_tower_id,
    location_name,
    c.name AS circle_name,
    c.state_code AS state_code
FROM ext_postgres.cell_towers ct
LEFT JOIN ext_postgres.circles c ON ct.circle_id = c.id;

TRUNCATE TABLE IF EXISTS dwh.dim_date;
INSERT INTO dwh.dim_date
SELECT DISTINCT
    toUInt32(toYYYYMMDD(start_time)) AS date_id,
    toDate(start_time) AS full_date,
    toYear(start_time) AS year,
    toQuarter(start_time) AS quarter,
    toMonth(start_time) AS month,
    toDayOfMonth(start_time) AS day,
    toDayOfWeek(start_time) IN (6, 7) AS is_weekend
FROM ext_postgres.cdrs;

TRUNCATE TABLE IF EXISTS dwh.dim_time;
INSERT INTO dwh.dim_time
SELECT DISTINCT
    toUInt32(formatDateTime(start_time, '%H%M')) AS time_id,
    toHour(start_time) AS hour,
    toMinute(start_time) AS minute,
    multiIf(toHour(start_time) >= 5 AND toHour(start_time) <= 11, 'Morning',
            toHour(start_time) >= 12 AND toHour(start_time) <= 16, 'Afternoon',
            toHour(start_time) >= 17 AND toHour(start_time) <= 20, 'Evening',
            'Night') AS time_of_day
FROM ext_postgres.cdrs;

TRUNCATE TABLE IF EXISTS dwh.dim_subscriber;
INSERT INTO dwh.dim_subscriber (subscriber_id, phone_number, plan_type, status)
SELECT rowNumberInAllBlocks() + 1, caller_number, 'PREPAID', 'ACTIVE'
FROM (SELECT DISTINCT caller_number FROM ext_postgres.cdrs);

-- ETL: Load Fact Table
TRUNCATE TABLE IF EXISTS dwh.fact_cdrs;
INSERT INTO dwh.fact_cdrs
SELECT
    c.id AS cdr_id,
    toUInt32(toYYYYMMDD(c.start_time)) AS date_id,
    toUInt32(formatDateTime(c.start_time, '%H%M')) AS time_id,
    c.cell_id AS cell_tower_id,
    s1.subscriber_id AS caller_id,
    s1.subscriber_id AS receiver_id,
    c.call_type,
    c.duration_seconds,
    c.cost,
    c.status AS call_status
FROM ext_postgres.cdrs AS c
LEFT JOIN dwh.dim_subscriber AS s1 ON c.caller_number = s1.phone_number;
