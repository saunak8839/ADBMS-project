package com.telecom.pipeline.consumer.service;

import com.telecom.pipeline.consumer.repository.CdrRepository;
import com.telecom.pipeline.consumer.model.Cdr;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.data.neo4j.core.Neo4jClient;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Service
public class FraudDetectionService {

    private final CdrRepository cdrRepository;
    private final Neo4jClient neo4jClient;
    
    // We start polling records from slightly back in time to ensure nothing is missed on fresh boot.
    private LocalDateTime lastSyncTime = LocalDateTime.now().minusMinutes(10);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    public FraudDetectionService(CdrRepository cdrRepository, Neo4jClient neo4jClient) {
        this.cdrRepository = cdrRepository;
        this.neo4jClient = neo4jClient;
    }

    @Scheduled(fixedDelay = 2000)
    public void syncFromPostgresAndDetectFraud() {
        try {
            List<Cdr> unsyncedCdrs = cdrRepository.getUnsyncedCdrs(lastSyncTime);
            
            if (unsyncedCdrs.isEmpty()) {
                return;
            }

            java.util.List<Map<String, Object>> batch = new java.util.ArrayList<>();
            for (Cdr cdr : unsyncedCdrs) {
                batch.add(Map.of(
                    "caller", cdr.getCallerNumber(),
                    "receiver", cdr.getReceiverNumber(),
                    "cdrId", cdr.getId().toString(),
                    "duration", cdr.getDurationSeconds(),
                    "startTime", cdr.getStartTime().format(FORMATTER)
                ));
            }

            neo4jClient.query("UNWIND $batch AS row " +
                    "MERGE (a:Subscriber {phoneNumber: row.caller}) SET a.status = 'ACTIVE' " +
                    "MERGE (b:Subscriber {phoneNumber: row.receiver}) SET b.status = 'ACTIVE' " +
                    "CREATE (a)-[:CALLED {cdrId: row.cdrId, durationSec: row.duration, startTime: row.startTime}]->(b)")
                    .bindAll(Map.of("batch", batch))
                    .run();

            System.out.println("Graph Analytics: Successfully pulled and merged " + unsyncedCdrs.size() + " calls from Postgres to Neo4j!");

            lastSyncTime = unsyncedCdrs.get(unsyncedCdrs.size() - 1).getStartTime();

            // Run Fraud Detection Asynchronously/Periodically
            runFraudDetectionAlgorithms();

        } catch (Exception e) {
            System.err.println("Error polling Postgres for Graph Sync: " + e.getMessage());
        }
    }

    private void runFraudDetectionAlgorithms() {
        Collection<Map<String, Object>> spammers = neo4jClient.query(
                "MATCH (a:Subscriber)-[:CALLED]->(b:Subscriber) WITH a, COUNT(DISTINCT b) as uniqueCallees WHERE uniqueCallees > 50 RETURN DISTINCT a.phoneNumber AS spammer, uniqueCallees as count LIMIT 1000")
                .fetch().all();
        if (!spammers.isEmpty()) {
            System.out.println("🚨 FRAUD ALERT: High out-degree spammers detected 🚨");
            spammers.forEach(s -> System.out
                    .println("   Spammer: " + s.get("spammer") + " with " + s.get("count") + " unique calls"));
        }

        Collection<Map<String, Object>> rings = neo4jClient.query(
                "MATCH (a:Subscriber)-[:CALLED]->(b:Subscriber)-[:CALLED]->(c:Subscriber)-[:CALLED]->(a:Subscriber) RETURN DISTINCT a.phoneNumber AS caller, b.phoneNumber AS intermediary1, c.phoneNumber AS intermediary2 LIMIT 1000")
                .fetch().all();
        if (!rings.isEmpty()) {
            System.out.println("🚨 FRAUD ALERT: Fraud Ring (Ping-Loop) detected 🚨");
            rings.forEach(r -> System.out.println("   Loop: " + r.get("caller") + " -> " + r.get("intermediary1")
                    + " -> " + r.get("intermediary2") + " -> " + r.get("caller")));
        }
    }
}
