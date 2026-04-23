package com.telecom.pipeline.consumer.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Scheduled ETL: Automatically syncs data from Postgres (OLTP) 
 * to ClickHouse (OLAP) every 5 minutes via ClickHouse's federated
 * PostgreSQL engine.
 */
@Service
public class OlapEtlService {

    private final ClickHouseHttpService clickHouse;

    public OlapEtlService(ClickHouseHttpService clickHouse) {
        this.clickHouse = clickHouse;
    }

    /**
     * Runs every 5 minutes. Re-creates the federated link if needed,
     * then refreshes all dimension tables and the fact table.
     */
    @Scheduled(fixedDelay = 60000, initialDelay = 30000) // 1 min interval, 30s startup delay
    public void runEtl() {
        System.out.println("⚙️ OLAP ETL: Starting sync from Postgres → ClickHouse...");
        long start = System.currentTimeMillis();

        try {
            // Step 0: Ensure federated Postgres link exists
            clickHouse.query("CREATE DATABASE IF NOT EXISTS ext_postgres " +
                "ENGINE = PostgreSQL('telecom_postgres:5432', 'telecom_db', 'telecom_user', 'telecom_password', 'public')");

            // Step 1: Refresh dim_cell_tower
            clickHouse.execute("TRUNCATE TABLE IF EXISTS dwh.dim_cell_tower");
            clickHouse.execute(
                "INSERT INTO dwh.dim_cell_tower " +
                "SELECT id AS cell_tower_id, location_name, " +
                "c.name AS circle_name, c.state_code AS state_code " +
                "FROM ext_postgres.cell_towers ct " +
                "LEFT JOIN ext_postgres.circles c ON ct.circle_id = c.id"
            );

            // Step 2: Refresh dim_date
            clickHouse.execute("TRUNCATE TABLE IF EXISTS dwh.dim_date");
            clickHouse.execute(
                "INSERT INTO dwh.dim_date " +
                "SELECT DISTINCT toUInt32(toYYYYMMDD(start_time)) AS date_id, " +
                "toDate(start_time) AS full_date, toYear(start_time) AS year, " +
                "toQuarter(start_time) AS quarter, toMonth(start_time) AS month, " +
                "toDayOfMonth(start_time) AS day, " +
                "toDayOfWeek(start_time) IN (6, 7) AS is_weekend " +
                "FROM ext_postgres.cdrs"
            );

            // Step 3: Refresh dim_time
            clickHouse.execute("TRUNCATE TABLE IF EXISTS dwh.dim_time");
            clickHouse.execute(
                "INSERT INTO dwh.dim_time " +
                "SELECT DISTINCT toUInt32(toHour(start_time) * 100 + toMinute(start_time)) AS time_id, " +
                "toHour(start_time) AS hour, toMinute(start_time) AS minute, " +
                "multiIf(toHour(start_time) >= 5 AND toHour(start_time) <= 11, 'Morning', " +
                "toHour(start_time) >= 12 AND toHour(start_time) <= 16, 'Afternoon', " +
                "toHour(start_time) >= 17 AND toHour(start_time) <= 20, 'Evening', " +
                "'Night') AS time_of_day " +
                "FROM ext_postgres.cdrs"
            );

            // Step 4: Refresh dim_subscriber
            clickHouse.execute("TRUNCATE TABLE IF EXISTS dwh.dim_subscriber");
            clickHouse.execute(
                "INSERT INTO dwh.dim_subscriber (subscriber_id, phone_number, plan_type, status) " +
                "SELECT rowNumberInAllBlocks() + 1, caller_number, 'PREPAID', 'ACTIVE' " +
                "FROM (SELECT DISTINCT caller_number FROM ext_postgres.cdrs)"
            );

            // Step 5: Refresh fact_cdrs
            clickHouse.execute("TRUNCATE TABLE IF EXISTS dwh.fact_cdrs");
            clickHouse.execute(
                "INSERT INTO dwh.fact_cdrs " +
                "SELECT c.id AS cdr_id, toUInt32(toYYYYMMDD(c.start_time)) AS date_id, " +
                "toUInt32(toHour(c.start_time) * 100 + toMinute(c.start_time)) AS time_id, " +
                "c.cell_id AS cell_tower_id, " +
                "s1.subscriber_id AS caller_id, s1.subscriber_id AS receiver_id, " +
                "c.call_type, c.duration_seconds, c.cost, c.status AS call_status " +
                "FROM ext_postgres.cdrs AS c " +
                "LEFT JOIN dwh.dim_subscriber AS s1 ON c.caller_number = s1.phone_number"
            );

            long elapsed = (System.currentTimeMillis() - start) / 1000;
            System.out.println("✅ OLAP ETL: Sync complete in " + elapsed + "s");

        } catch (Exception e) {
            System.err.println("❌ OLAP ETL failed: " + e.getMessage());
        }
    }
}
