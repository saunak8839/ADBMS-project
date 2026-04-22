-- 1. Regional Hierarchy
CREATE TABLE circles (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    state_code VARCHAR(10) NOT NULL
);

CREATE TABLE cell_towers (
    id SERIAL PRIMARY KEY,
    location_name VARCHAR(100) NOT NULL,
    circle_id INT REFERENCES circles(id)
);

-- 2. Subscriber Base
CREATE TABLE subscribers (
    id SERIAL PRIMARY KEY,
    phone_number VARCHAR(20) UNIQUE NOT NULL, -- MSISDN
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    home_circle_id INT REFERENCES circles(id),
    plan_type VARCHAR(20) CHECK (plan_type IN ('PREPAID', 'POSTPAID')),
    status VARCHAR(20) DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'SUSPENDED', 'DEACTIVATED')),
    balance DECIMAL(10, 2) DEFAULT 0.00,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 3. Partitioned Call Records (The CDR Table)
CREATE TABLE cdrs (
    id UUID NOT NULL,
    caller_number VARCHAR(20) NOT NULL,
    receiver_number VARCHAR(20) NOT NULL,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    duration_seconds INT NOT NULL,
    cell_id INT REFERENCES cell_towers(id),
    call_type VARCHAR(20) DEFAULT 'VOICE',
    status VARCHAR(20) DEFAULT 'SUCCESS',
    cost DECIMAL(10, 2) DEFAULT 0.00,
    PRIMARY KEY (id, start_time) -- Partition key must be in PK
) PARTITION BY RANGE (start_time);

-- Example Partition for April 2026
CREATE TABLE cdrs_y2026_m04 PARTITION OF cdrs
    FOR VALUES FROM ('2026-04-01') TO ('2026-05-01');

-- 4. Analytical Indexes
CREATE INDEX idx_cdrs_caller ON cdrs(caller_number);
CREATE INDEX idx_cdrs_cell_id ON cdrs(cell_id);

-- 5. Seed Data for DWH & Constraints
INSERT INTO circles (id, name, state_code) VALUES (1, 'National', 'NAT') ON CONFLICT DO NOTHING;

DO $$
BEGIN
    FOR i IN 1..100 LOOP
        INSERT INTO cell_towers (id, location_name, circle_id) 
        VALUES (i, 'Tower ' || i, 1) 
        ON CONFLICT DO NOTHING;
    END LOOP;
END;
$$;