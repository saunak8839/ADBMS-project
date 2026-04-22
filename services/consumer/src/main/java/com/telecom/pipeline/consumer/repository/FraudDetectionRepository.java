package com.telecom.pipeline.consumer.repository;

import com.telecom.pipeline.consumer.model.SubscriberNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public interface FraudDetectionRepository extends Neo4jRepository<SubscriberNode, String> {

    @Query("MERGE (a:Subscriber {phoneNumber: $caller}) SET a.status = 'ACTIVE' " +
           "MERGE (b:Subscriber {phoneNumber: $receiver}) SET b.status = 'ACTIVE' " +
           "CREATE (a)-[:CALLED {cdrId: $cdrId, durationSec: $duration}]->(b)")
    void createCallMapping(String caller, String receiver, String cdrId, Integer duration);

    /**
     * Detects a basic fraud ring / ping-call loop where A calls B, B calls C, and C calls A.
     */
    @Query("MATCH (a:Subscriber)-[:CALLED]->(b:Subscriber)-[:CALLED]->(c:Subscriber)-[:CALLED]->(a:Subscriber) " +
           "RETURN a.phoneNumber AS caller, b.phoneNumber AS intermediary1, c.phoneNumber AS intermediary2")
    List<Map<String, Object>> findFraudRings();

    /**
     * Detects potential spammers who have called more than 50 unique numbers.
     */
    @Query("MATCH (a:Subscriber)-[:CALLED]->(b:Subscriber) " +
           "WITH a, COUNT(DISTINCT b) as uniqueCallees " +
           "WHERE uniqueCallees > 50 " +
           "RETURN a.phoneNumber AS spammer, uniqueCallees as count")
    List<Map<String, Object>> findSpammers();
}
