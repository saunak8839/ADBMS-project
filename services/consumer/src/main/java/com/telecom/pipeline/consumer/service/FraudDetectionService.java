package com.telecom.pipeline.consumer.service;

import com.telecom.pipeline.consumer.model.CallRelationship;
import com.telecom.pipeline.consumer.model.SubscriberNode;
import com.telecom.pipeline.consumer.repository.FraudDetectionRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class FraudDetectionService {

    private final FraudDetectionRepository fraudDetectionRepository;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    public FraudDetectionService(FraudDetectionRepository fraudDetectionRepository) {
        this.fraudDetectionRepository = fraudDetectionRepository;
    }

    @KafkaListener(topics = "cdr_raw", groupId = "telecom-fraud-group")
    public void consumeForFraudDetection(List<Map<String, Object>> eventDataList) {
        try {
            for (Map<String, Object> eventData : eventDataList) {
                String callerNumber = eventData.get("callerMsisdn").toString();
                String receiverNumber = eventData.get("calleeMsisdn").toString();
                String cdrId = (String) eventData.get("cdrId");
                Integer duration = (Integer) eventData.get("durationSec");

                fraudDetectionRepository.createCallMapping(callerNumber, receiverNumber, cdrId, duration);
            }

            System.out.println("Graph Analytics: Successfully merged " + eventDataList.size() + " calls into Neo4j!");

            // Run Fraud Detection Asynchronously/Periodically
            runFraudDetectionAlgorithms();

        } catch (Exception e) {
            System.err.println("Error processing CDR batch for Fraud Detection: " + e.getMessage());
        }
    }

    private void runFraudDetectionAlgorithms() {
        List<Map<String, Object>> spammers = fraudDetectionRepository.findSpammers();
        if (!spammers.isEmpty()) {
            System.out.println("🚨 FRAUD ALERT: High out-degree spammers detected 🚨");
            spammers.forEach(s -> System.out.println("   Spammer: " + s.get("spammer") + " with " + s.get("count") + " unique calls"));
        }

        List<Map<String, Object>> rings = fraudDetectionRepository.findFraudRings();
        if (!rings.isEmpty()) {
            System.out.println("🚨 FRAUD ALERT: Fraud Ring (Ping-Loop) detected 🚨");
            rings.forEach(r -> System.out.println("   Loop: " + r.get("caller") + " -> " + r.get("intermediary1") + " -> " + r.get("intermediary2") + " -> " + r.get("caller")));
        }
    }
}
