package com.telecom.pipeline.consumer.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "*")
public class DashboardController {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @GetMapping("/data")
    public ResponseEntity<String> getDashboardData() {
        String data = redisTemplate.opsForValue().get("dashboard:data");
        if (data == null) {
            return ResponseEntity.ok("{}");
        }
        return ResponseEntity.ok(data);
    }
}
