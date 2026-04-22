package com.telecom.pipeline.consumer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.telecom.pipeline.consumer.model.Cdr;
import com.telecom.pipeline.consumer.repository.CdrRepository;
import com.telecom.pipeline.consumer.repository.FraudDetectionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Collection;

import org.springframework.data.neo4j.core.Neo4jClient;

@Service
public class AnalyticsCacheService {

    @Autowired
    private CdrRepository cdrRepository;

    @Autowired
    private FraudDetectionRepository fraudDetectionRepository;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private Neo4jClient neo4jClient;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    @Scheduled(fixedRate = 2000)
    public void cacheAnalytics() {
        try {
            // 1. Fetch live metrics from PostgreSQL
            long totalCalls = cdrRepository.count();
            Double revenue = cdrRepository.getTotalRevenue();
            List<Cdr> recentCdrs = cdrRepository.getRecentCdrs();

            // 2. Fetch live patterns from Neo4j using raw client to avoid SDN mapping errors
            Collection<Map<String, Object>> fraudRings = neo4jClient.query("MATCH (a:Subscriber)-[:CALLED]->(b:Subscriber)-[:CALLED]->(c:Subscriber)-[:CALLED]->(a:Subscriber) RETURN a.phoneNumber AS caller, b.phoneNumber AS intermediary1, c.phoneNumber AS intermediary2 LIMIT 20").fetch().all();
            Collection<Map<String, Object>> spammers = neo4jClient.query("MATCH (a:Subscriber)-[:CALLED]->(b:Subscriber) WITH a, COUNT(DISTINCT b) as uniqueCallees WHERE uniqueCallees > 50 RETURN a.phoneNumber AS spammer, uniqueCallees as count LIMIT 20").fetch().all();
            
            // Format frauds into unified alert objects for the frontend
            List<Map<String, Object>> unifiedFrauds = new ArrayList<>();
            for (Map<String, Object> ring : fraudRings) {
                unifiedFrauds.add(Map.of(
                    "id", UUID.randomUUID().toString(),
                    "type", "Fraud Ring",
                    "details", ring.get("caller") + " ➔ " + ring.get("intermediary1") + " ➔ " + ring.get("intermediary2") + " ➔ " + ring.get("caller"),
                    "desc", "Ping-Call Loop detected across 3 unique subscribers."
                ));
            }
            for (Map<String, Object> spammer : spammers) {
                 unifiedFrauds.add(Map.of(
                    "id", UUID.randomUUID().toString(),
                    "type", "Spammer Behavior",
                    "details", "Spammer: " + spammer.get("spammer"),
                    "desc", "High out-degree detected. Node called " + spammer.get("count") + " unique numbers."
                ));
            }

            // 3. Fetch Advanced Analytics for Charts
            List<CdrRepository.TopCallerProjection> topCallersProj = cdrRepository.getTopCallers();
            List<Map<String, Object>> topCallers = new ArrayList<>();
            for (CdrRepository.TopCallerProjection proj : topCallersProj) {
                topCallers.add(Map.of(
                    "caller", proj.getCaller(),
                    "calls", proj.getCalls(),
                    "duration", proj.getTotalDuration() != null ? proj.getTotalDuration() : 0,
                    "revenue", proj.getTotalRevenue() != null ? proj.getTotalRevenue() : 0.0
                ));
            }

            List<CdrRepository.RevenueByCircleProjection> revProj = cdrRepository.getRevenueByCircle();
            List<Map<String, Object>> revData = new ArrayList<>();
            for (CdrRepository.RevenueByCircleProjection r : revProj) {
                revData.add(Map.of("name", r.getName(), "value", r.getValue() != null ? r.getValue() : 0.0));
            }

            List<CdrRepository.TrafficByTimeProjection> trafficProj = cdrRepository.getTrafficByTime();
            List<Map<String, Object>> volData = new ArrayList<>();
            for (CdrRepository.TrafficByTimeProjection t : trafficProj) {
                volData.add(Map.of("time", t.getTime(), "calls", t.getCalls() != null ? t.getCalls() : 0));
            }

            // Build payload
            Map<String, Object> dashboardData = new HashMap<>();
            dashboardData.put("metrics", Map.of(
                "totalCalls", totalCalls,
                "revenue", revenue != null ? revenue : 0,
                "fraudAlerts", unifiedFrauds.size(),
                "activeTowers", 1845
            ));
            dashboardData.put("stream", recentCdrs);
            dashboardData.put("frauds", unifiedFrauds);
            dashboardData.put("topCallers", topCallers);
            dashboardData.put("revData", revData);
            dashboardData.put("volData", volData);
            
            // Serialize and cache into Redis
            String json = objectMapper.writeValueAsString(dashboardData);
            redisTemplate.opsForValue().set("dashboard:data", json);
            System.out.println("Analytics cached to Redis memory.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
