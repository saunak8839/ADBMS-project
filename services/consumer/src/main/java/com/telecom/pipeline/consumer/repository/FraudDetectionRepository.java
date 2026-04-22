package com.telecom.pipeline.consumer.repository;

import com.telecom.pipeline.consumer.model.SubscriberNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.stereotype.Repository;

import org.springframework.data.repository.query.Param;

@Repository
public interface FraudDetectionRepository extends Neo4jRepository<SubscriberNode, String> {

       @Query("MERGE (a:Subscriber {phoneNumber: $caller}) SET a.status = 'ACTIVE' " +
                     "MERGE (b:Subscriber {phoneNumber: $receiver}) SET b.status = 'ACTIVE' " +
                     "CREATE (a)-[:CALLED {cdrId: $cdrId, durationSec: $duration}]->(b)")
       void createCallMapping(@Param("caller") String caller, @Param("receiver") String receiver,
                     @Param("cdrId") String cdrId, @Param("duration") Integer duration);
}
