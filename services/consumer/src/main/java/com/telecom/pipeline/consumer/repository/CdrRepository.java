package com.telecom.pipeline.consumer.repository;

import com.telecom.pipeline.consumer.model.Cdr;
import com.telecom.pipeline.consumer.model.CdrCompositeKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import java.util.List;

@Repository
public interface CdrRepository extends JpaRepository<Cdr, CdrCompositeKey> {
    
    interface TopCallerProjection {
        String getCaller();
        Long getCalls();
        Long getTotalDuration();
        Double getTotalRevenue();
    }

    interface RevenueByCircleProjection {
        String getName();
        Double getValue();
    }

    interface TrafficByTimeProjection {
        String getTime();
        Long getCalls();
    }

    @Query(value = "SELECT caller_number as caller, COUNT(*) as calls, CAST(SUM(duration_seconds) as BIGINT) as totalDuration, SUM(cost) as totalRevenue FROM cdrs GROUP BY caller_number ORDER BY totalRevenue DESC LIMIT 10", nativeQuery = true)
    List<TopCallerProjection> getTopCallers();

    @Query(value = "SELECT CASE WHEN cell_id < 33 THEN 'National' WHEN cell_id < 66 THEN 'Metro' ELSE 'Circle A' END as name, SUM(cost) as value FROM cdrs GROUP BY CASE WHEN cell_id < 33 THEN 'National' WHEN cell_id < 66 THEN 'Metro' ELSE 'Circle A' END", nativeQuery = true)
    List<RevenueByCircleProjection> getRevenueByCircle();

    @Query(value = "SELECT TO_CHAR(start_time, 'HH24:00') as time, COUNT(*) as calls FROM cdrs GROUP BY TO_CHAR(start_time, 'HH24:00') ORDER BY time ASC", nativeQuery = true)
    List<TrafficByTimeProjection> getTrafficByTime();

    @Query(value = "SELECT COALESCE(SUM(cost), 0) FROM cdrs", nativeQuery = true)
    Double getTotalRevenue();

    @Query(value = "SELECT * FROM cdrs ORDER BY start_time DESC LIMIT 20", nativeQuery = true)
    List<Cdr> getRecentCdrs();
}
