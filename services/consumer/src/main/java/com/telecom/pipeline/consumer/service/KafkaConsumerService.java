package com.telecom.pipeline.consumer.service;

import com.telecom.pipeline.consumer.model.Cdr;
import com.telecom.pipeline.consumer.repository.CdrRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;

@Service
public class KafkaConsumerService {

    private final CdrRepository cdrRepository;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    public KafkaConsumerService(CdrRepository cdrRepository) {
        this.cdrRepository = cdrRepository;
    }

    @KafkaListener(topics = "cdr_raw", groupId = "telecom-consumer-group")
    public void consumeCdrBatch(List<Map<String, Object>> eventDataList) {
        try {
            List<Cdr> cdrBatch = new ArrayList<>();
            for (Map<String, Object> eventData : eventDataList) {
                Cdr cdr = new Cdr();
                cdr.setId(UUID.fromString((String) eventData.get("cdrId")));
                cdr.setCallerNumber(eventData.get("callerMsisdn").toString());
                cdr.setReceiverNumber(eventData.get("calleeMsisdn").toString());
                
                LocalDateTime start = LocalDateTime.parse((String) eventData.get("startTime"), FORMATTER);
                Integer duration = (Integer) eventData.get("durationSec");
                
                cdr.setStartTime(start);
                cdr.setDurationSeconds(duration);
                cdr.setEndTime(start.plusSeconds(duration));
                
                cdr.setCellId((Integer) eventData.get("cellId"));
                cdr.setCallType("VOICE");
                cdr.setStatus("SUCCESS");
                cdr.setCost(new BigDecimal("0.50")); // Example cost logic
                
                cdrBatch.add(cdr);
            }

            cdrRepository.saveAll(cdrBatch);
            System.out.println("Successfully batch inserted " + cdrBatch.size() + " CDRs!");
        } catch (Exception e) {
            System.err.println("Error processing CDR batch: " + e.getMessage());
        }
    }
}
