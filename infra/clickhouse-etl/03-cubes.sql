-- Data Cube 1: Revenue & Duration Multi-Dimensional Analysis
-- Shows aggregations across all possible permutations of Circle, Year, and Month.
-- For example: Total revenue for "National" circle in all of 2026, total revenue for "National" in April 2026, total across all circles in 2026, etc.
CREATE VIEW IF NOT EXISTS dwh.cube_revenue_analysis AS
SELECT 
    ct.circle_name,
    d.year,
    d.month,
    SUM(f.cost) as total_revenue,
    SUM(f.duration_seconds) as total_duration,
    COUNT(f.cdr_id) as total_calls
FROM dwh.fact_cdrs f
JOIN dwh.dim_cell_tower ct ON f.cell_tower_id = ct.cell_tower_id
JOIN dwh.dim_date d ON f.date_id = d.date_id
GROUP BY CUBE (ct.circle_name, d.year, d.month)
ORDER BY ct.circle_name, d.year, d.month;

-- Rollup 2: Network Traffic Hierarchical Analysis 
-- Shows aggregations grouped hierarchically.
-- Year -> Quarter -> Month -> Time of Day.
CREATE VIEW IF NOT EXISTS dwh.rollup_network_usage AS
SELECT 
    d.year,
    d.quarter,
    d.month,
    t.time_of_day,
    COUNT(f.cdr_id) as call_volume
FROM dwh.fact_cdrs f
JOIN dwh.dim_date d ON f.date_id = d.date_id
JOIN dwh.dim_time t ON f.time_id = t.time_id
GROUP BY ROLLUP (d.year, d.quarter, d.month, t.time_of_day)
ORDER BY d.year, d.quarter, d.month, t.time_of_day;
