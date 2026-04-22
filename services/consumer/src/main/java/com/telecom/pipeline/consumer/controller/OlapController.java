package com.telecom.pipeline.consumer.controller;

import com.telecom.pipeline.consumer.service.ClickHouseHttpService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/olap")
@CrossOrigin(origins = "*")
public class OlapController {

    private final ClickHouseHttpService clickHouse;

    public OlapController(ClickHouseHttpService clickHouse) {
        this.clickHouse = clickHouse;
    }

    @GetMapping("/status")
    public Map<String, Object> getStatus() {
        Map<String, Object> status = new HashMap<>();
        List<Map<String, Object>> result = clickHouse.query("SELECT count() as total FROM dwh.fact_cdrs");
        if (!result.isEmpty()) {
            status.put("connected", true);
            status.put("total_records", result.get(0).get("total"));
        } else {
            status.put("connected", false);
            status.put("error", "No data returned");
        }
        return status;
    }

    @GetMapping("/hourly-trend")
    public List<Map<String, Object>> getHourlyTrend() {
        return clickHouse.query(
            "SELECT (time_id / 100) as hour, " +
            "count() as call_count, " +
            "sum(cost) as total_revenue " +
            "FROM dwh.fact_cdrs " +
            "GROUP BY hour ORDER BY hour ASC"
        );
    }

    @GetMapping("/regional-distribution")
    public List<Map<String, Object>> getRegionalDistribution() {
        return clickHouse.query(
            "SELECT toString(cell_tower_id) as region, " +
            "count() as calls, " +
            "sum(cost) as revenue " +
            "FROM dwh.fact_cdrs " +
            "GROUP BY region ORDER BY calls DESC LIMIT 10"
        );
    }

    @GetMapping("/call-type-analytics")
    public List<Map<String, Object>> getCallTypeAnalytics() {
        return clickHouse.query(
            "SELECT call_type, " +
            "count() as total_calls, " +
            "avg(duration_seconds) as avg_duration " +
            "FROM dwh.fact_cdrs " +
            "GROUP BY call_type"
        );
    }
}
