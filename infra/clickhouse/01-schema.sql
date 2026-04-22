CREATE DATABASE IF NOT EXISTS dwh;

-- Dimensions
CREATE TABLE IF NOT EXISTS dwh.dim_date (
    date_id Int32,
    full_date Date,
    year Int32,
    quarter Int32,
    month Int32,
    day Int32,
    is_weekend UInt8
) ENGINE = MergeTree()
ORDER BY date_id;

CREATE TABLE IF NOT EXISTS dwh.dim_time (
    time_id Int32,
    hour Int32,
    minute Int32,
    time_of_day String
) ENGINE = MergeTree()
ORDER BY time_id;

CREATE TABLE IF NOT EXISTS dwh.dim_cell_tower (
    cell_tower_id Int32,
    location_name String,
    circle_name String,
    state_code String
) ENGINE = MergeTree()
ORDER BY cell_tower_id;

CREATE TABLE IF NOT EXISTS dwh.dim_subscriber (
    subscriber_id Int32,
    phone_number String,
    plan_type String,
    status String
) ENGINE = MergeTree()
ORDER BY subscriber_id;

-- Fact Table
CREATE TABLE IF NOT EXISTS dwh.fact_cdrs (
    cdr_id UUID,
    date_id Int32,
    time_id Int32,
    cell_tower_id Int32,
    caller_id Int32,
    receiver_id Int32,
    call_type String,
    duration_seconds Int32,
    cost Float32,
    call_status String
) ENGINE = MergeTree()
ORDER BY (date_id, cell_tower_id, caller_id)
PARTITION BY toYYYYMM(toDate(toString(date_id), 'YYYYMMDD'));
