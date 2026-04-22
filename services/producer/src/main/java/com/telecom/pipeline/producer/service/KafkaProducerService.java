package com.telecom.pipeline.producer.service;

import com.telecom.pipeline.producer.model.CdrEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.Random;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import org.springframework.scheduling.annotation.Scheduled;

@Service
public class KafkaProducerService {

    private final KafkaTemplate<String, CdrEvent> kafkaTemplate;
    private static final String TOPIC = "cdr_raw";
    private final Random random = new Random();
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    // No fixed pools anymore - using purely probabilistic logic
    private final int SUBSCRIBER_POOL_SIZE = 10000;
    private final long BASE_NUM = 9000000000L;

    public KafkaProducerService(KafkaTemplate<String, CdrEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedRate = 2000)
    public void generateLiveTraffic() {
        int hour = LocalTime.now().getHour();
        int baseRate;

        // Traffic curve
        if (hour >= 0 && hour < 6)
            baseRate = 2; // Night
        else if (hour >= 6 && hour < 10)
            baseRate = 25; // Morning Rush
        else if (hour >= 10 && hour < 18)
            baseRate = 15; // Business
        else
            baseRate = 30; // Evening Peak

        int actualRate = baseRate;
        if (random.nextDouble() < 0.10) { // 10% chance of burst
            actualRate = baseRate * 5;
            System.out.println("BURST DETECTED! Generating " + actualRate + " calls.");
        }

        generateAndSendBatch(actualRate);
    }

    public void generateAndSendBatch(int count) {
        for (int i = 0; i < count; i++) {
            CdrEvent event;
            double rand = random.nextDouble();
            if (rand < 0.10) {
                // 10% chance to inject a spammer call (Picks from top 10)
                event = generateSpammerCdr();
            } else if (rand < 0.20) {
                // 10% chance to inject a fraud ring call (Picks from 50 cells)
                event = generateRingCdr();
            } else {
                // 80% normal random call
                event = generateRandomCdr();
            }
            kafkaTemplate.send(TOPIC, event.getCdrId().toString(), event);
        }
    }

    private CdrEvent generateSpammerCdr() {
        // Concentrated spammer pool: picks from only 10 nodes to trigger >50 alerts faster
        Long spammer = BASE_NUM + random.nextInt(10);
        Long callee = BASE_NUM + 100 + random.nextInt(SUBSCRIBER_POOL_SIZE - 100);
        return buildEvent(spammer, callee);
    }

    private CdrEvent generateRingCdr() {
        // Probabilistic loop closing: picks from a small "cell" of 3 nodes
        int cellId = random.nextInt(50); // 50 different potential fraud cells
        int startRange = cellId * 5;

        Long caller = BASE_NUM + 5000 + startRange + random.nextInt(3);
        Long callee = BASE_NUM + 5000 + startRange + ((random.nextInt(3) + 1) % 3);

        return buildEvent(caller, callee);
    }

    private CdrEvent generateRandomCdr() {
        Long caller = BASE_NUM + random.nextInt(SUBSCRIBER_POOL_SIZE);
        Long callee = BASE_NUM + random.nextInt(SUBSCRIBER_POOL_SIZE);
        while (caller.equals(callee))
            callee = BASE_NUM + random.nextInt(SUBSCRIBER_POOL_SIZE);
        return buildEvent(caller, callee);
    }

    private CdrEvent buildEvent(Long caller, Long callee) {
        LocalDateTime startTime = LocalDateTime.now().minusSeconds(random.nextInt(60));
        return new CdrEvent(
                UUID.randomUUID(),
                caller,
                callee,
                startTime.format(FORMATTER),
                10 + random.nextInt(3600), // duration
                1 + random.nextInt(100) // cell_id
        );
    }
}
