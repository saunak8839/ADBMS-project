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

    // Sophisticated Fraud Entities
    private final Long[] SPAMMERS = { 9999999999L, 9991111111L, 9992222222L, 9993333333L, 9994444444L };
    private final Long[] RING_POOL = { 8880000001L, 8880000002L, 8880000003L, 8880000004L, 8880000005L, 8880000006L,
            8880000007L, 8880000008L, 8880000009L };

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
            if (rand < 0.05) {
                // 5% chance to inject a spammer call
                event = generateSpammerCdr();
            } else if (rand < 0.10) {
                // 5% chance to inject a fraud ring call
                event = generateRingCdr();
            } else {
                // 90% normal random call
                event = generateRandomCdr();
            }
            kafkaTemplate.send(TOPIC, event.getCdrId().toString(), event);
        }
    }

    private CdrEvent generateSpammerCdr() {
        Long spammer = SPAMMERS[random.nextInt(SPAMMERS.length)];
        Long callee = 9000000000L + random.nextInt(999999999);
        return buildEvent(spammer, callee);
    }

    private CdrEvent generateRingCdr() {
        // Pick a base index in the pool and create a triplet cycle
        int startIdx = random.nextInt(RING_POOL.length);
        int offset = 1 + random.nextInt(2); // Randomize step in pool

        Long caller = RING_POOL[startIdx];
        Long callee = RING_POOL[(startIdx + offset) % RING_POOL.length];

        return buildEvent(caller, callee);
    }

    private CdrEvent generateRandomCdr() {
        Long caller = 9000000000L + random.nextInt(999999999);
        Long callee = 9000000000L + random.nextInt(999999999);
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
