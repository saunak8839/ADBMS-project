package com.telecom.pipeline.consumer.service;

import com.telecom.pipeline.consumer.repository.FraudDetectionRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.data.neo4j.core.Neo4jClient;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Service
public class FraudDetectionService {

    private final FraudDetectionRepository fraudDetectionRepository;
    private final Neo4jClient neo4jClient;

    public FraudDetectionService(FraudDetectionRepository fraudDetectionRepository, Neo4jClient neo4jClient) {
        this.fraudDetectionRepository = fraudDetectionRepository;
        this.neo4jClient = neo4jClient;
    }

    @KafkaListener(topics = "cdr_raw", groupId = "telecom-fraud-group")
    public void consumeForFraudDetection(List<Map<String, Object>> eventDataList) {
        try {
            java.util.List<Map<String, Object>> batch = new java.util.ArrayList<>();
            for (Map<String, Object> eventData : eventDataList) {
                batch.add(Map.of(
                        "caller", eventData.get("callerMsisdn").toString(),
                        "receiver", eventData.get("calleeMsisdn").toString(),
                        "cdrId", eventData.get("cdrId"),
                        "duration", eventData.get("durationSec"),
                        "startTime", eventData.get("startTime").toString()));
            }

            neo4jClient.query("UNWIND $batch AS row " +
                    "MERGE (a:Subscriber {phoneNumber: row.caller}) SET a.status = 'ACTIVE' " +
                    "MERGE (b:Subscriber {phoneNumber: row.receiver}) SET b.status = 'ACTIVE' " +
                    "CREATE (a)-[:CALLED {cdrId: row.cdrId, durationSec: row.duration, startTime: row.startTime}]->(b)")
                    .bindAll(Map.of("batch", batch))
                    .run();

            System.out.println("Graph Analytics: Successfully merged " + eventDataList.size() + " calls into Neo4j!");

            // Run Fraud Detection Asynchronously/Periodically
            runFraudDetectionAlgorithms();

        } catch (Exception e) {
            System.err.println("Error processing CDR batch for Fraud Detection: " + e.getMessage());
        }
    }

    private void runFraudDetectionAlgorithms() {
        Collection<Map<String, Object>> spammers = neo4jClient.query(
                "MATCH (a:Subscriber)-[:CALLED]->(b:Subscriber) WITH a, COUNT(DISTINCT b) as uniqueCallees WHERE uniqueCallees > 50 RETURN a.phoneNumber AS spammer, uniqueCallees as count LIMIT 20")
                .fetch().all();
        if (!spammers.isEmpty()) {
            System.out.println("🚨 FRAUD ALERT: High out-degree spammers detected 🚨");
            spammers.forEach(s -> System.out
                    .println("   Spammer: " + s.get("spammer") + " with " + s.get("count") + " unique calls"));
        }

        Collection<Map<String, Object>> rings = neo4jClient.query(
                "MATCH (a:Subscriber)-[:CALLED]->(b:Subscriber)-[:CALLED]->(c:Subscriber)-[:CALLED]->(a:Subscriber) RETURN a.phoneNumber AS caller, b.phoneNumber AS intermediary1, c.phoneNumber AS intermediary2 LIMIT 20")
                .fetch().all();
        if (!rings.isEmpty()) {
            System.out.println("🚨 FRAUD ALERT: Fraud Ring (Ping-Loop) detected 🚨");
            rings.forEach(r -> System.out.println("   Loop: " + r.get("caller") + " -> " + r.get("intermediary1")
                    + " -> " + r.get("intermediary2") + " -> " + r.get("caller")));
        }
    }
}
