package com.telecom.pipeline.consumer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Queries ClickHouse directly via its built-in HTTP API.
 * No JDBC driver needed — eliminates all driver compatibility issues
 * and connection pool overhead that was crashing the container.
 */
@Service
public class ClickHouseHttpService {

    private static final String CH_URL = "http://localhost:8123/";
    private static final String CH_USER = "default";
    private static final String CH_PASS = "telecom_password";
    private static final String CH_DB   = "dwh";

    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<Map<String, Object>> query(String sql) {
        try {
            // ClickHouse HTTP API: POST the SQL, get JSON back
            String fullSql = sql + " FORMAT JSON";
            URL url = new URL(CH_URL + "?user=" + CH_USER +
                    "&password=" + CH_PASS +
                    "&database=" + CH_DB);

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(10000);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(fullSql.getBytes(StandardCharsets.UTF_8));
            }

            if (conn.getResponseCode() != 200) {
                String error = new String(conn.getErrorStream().readAllBytes());
                System.err.println("ClickHouse HTTP Error: " + error);
                return Collections.emptyList();
            }

            String body = new String(conn.getInputStream().readAllBytes());
            JsonNode root = objectMapper.readTree(body);
            JsonNode dataNode = root.get("data");

            List<Map<String, Object>> rows = new ArrayList<>();
            if (dataNode != null && dataNode.isArray()) {
                for (JsonNode row : dataNode) {
                    Map<String, Object> map = new LinkedHashMap<>();
                    row.fields().forEachRemaining(e -> {
                        JsonNode val = e.getValue();
                        if (val.isNumber()) map.put(e.getKey(), val.numberValue());
                        else map.put(e.getKey(), val.asText());
                    });
                    rows.add(map);
                }
            }
            return rows;
        } catch (Exception e) {
            System.err.println("ClickHouse query failed: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Executes a DDL/DML statement (INSERT, TRUNCATE, CREATE) that doesn't return rows.
     */
    public boolean execute(String sql) {
        try {
            URL url = new URL(CH_URL + "?user=" + CH_USER +
                    "&password=" + CH_PASS +
                    "&database=" + CH_DB);

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(60000); // ETL can take a while

            try (OutputStream os = conn.getOutputStream()) {
                os.write(sql.getBytes(StandardCharsets.UTF_8));
            }

            if (conn.getResponseCode() != 200) {
                String error = new String(conn.getErrorStream().readAllBytes());
                System.err.println("ClickHouse execute error: " + error);
                return false;
            }
            return true;
        } catch (Exception e) {
            System.err.println("ClickHouse execute failed: " + e.getMessage());
            return false;
        }
    }
}
