package com.telecom.pipeline.producer.controller;

import com.telecom.pipeline.producer.service.KafkaProducerService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SimulatorController {

    private final KafkaProducerService producerService;

    public SimulatorController(KafkaProducerService producerService) {
        this.producerService = producerService;
    }

    @PostMapping("/api/generate")
    public String generateCdrs(@RequestParam(defaultValue = "100") int count) {
        producerService.generateAndSendBatch(count);
        return "Successfully generated and sent " + count + " raw CDR events to Kafka topic cdr_raw!";
    }
}
