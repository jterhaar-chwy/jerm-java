package jerm.jerm_java.controller;

import jerm.jerm_java.service.TestingHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/testing")
@CrossOrigin(origins = {"http://localhost:3000", "http://frontend:3000"})
public class TestingHistoryController {

    @Autowired
    private TestingHistoryService testingHistoryService;

    /**
     * Get testing trends for the last N days (default 7)
     * GET /api/testing/trends?days=7
     */
    @GetMapping("/trends")
    public ResponseEntity<Map<String, Object>> getTestingTrends(
            @RequestParam(value = "days", required = false) Integer days) {
        try {
            Map<String, Object> trends = testingHistoryService.getTestingTrends(days);
            return ResponseEntity.ok(trends);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to retrieve testing trends");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("queryType", "testing_trends_error");
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Get testing trends for the last 3 days (quick view)
     * GET /api/testing/trends/quick
     */
    @GetMapping("/trends/quick")
    public ResponseEntity<Map<String, Object>> getQuickTrends() {
        try {
            Map<String, Object> trends = testingHistoryService.getTestingTrends(3);
            return ResponseEntity.ok(trends);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to retrieve quick testing trends");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("queryType", "quick_trends_error");
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Get testing trends for the last 14 days (extended view)
     * GET /api/testing/trends/extended
     */
    @GetMapping("/trends/extended")
    public ResponseEntity<Map<String, Object>> getExtendedTrends() {
        try {
            Map<String, Object> trends = testingHistoryService.getTestingTrends(14);
            return ResponseEntity.ok(trends);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to retrieve extended testing trends");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("queryType", "extended_trends_error");
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Get testing trends for the last 30 days (monthly view)
     * GET /api/testing/trends/monthly
     */
    @GetMapping("/trends/monthly")
    public ResponseEntity<Map<String, Object>> getMonthlyTrends() {
        try {
            Map<String, Object> trends = testingHistoryService.getTestingTrends(30);
            return ResponseEntity.ok(trends);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to retrieve monthly testing trends");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("queryType", "monthly_trends_error");
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Health check endpoint
     * GET /api/testing/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "TestingHistoryService");
        health.put("description", "Automated testing history and trend analysis");
        health.put("endpoints", Map.of(
            "trends", "/api/testing/trends?days=N",
            "quick", "/api/testing/trends/quick (3 days)",
            "extended", "/api/testing/trends/extended (14 days)",
            "monthly", "/api/testing/trends/monthly (30 days)"
        ));
        return ResponseEntity.ok(health);
    }

    /**
     * Get available endpoints documentation
     * GET /api/testing/endpoints
     */
    @GetMapping("/endpoints")
    public ResponseEntity<Map<String, Object>> getEndpoints() {
        Map<String, Object> endpoints = new HashMap<>();
        endpoints.put("service", "Testing History API");
        endpoints.put("version", "1.0");
        endpoints.put("baseUrl", "/api/testing");
        
        Map<String, Object> availableEndpoints = new HashMap<>();
        availableEndpoints.put("GET /trends", "Get testing trends (default 7 days, ?days=N to customize)");
        availableEndpoints.put("GET /trends/quick", "Get 3-day testing trends");
        availableEndpoints.put("GET /trends/extended", "Get 14-day testing trends");
        availableEndpoints.put("GET /trends/monthly", "Get 30-day testing trends");
        availableEndpoints.put("GET /health", "Service health check");
        availableEndpoints.put("GET /endpoints", "This endpoint documentation");
        
        endpoints.put("endpoints", availableEndpoints);
        
        Map<String, Object> dataStructure = new HashMap<>();
        dataStructure.put("pesterTrends", "PowerShell Pester test results and analysis");
        dataStructure.put("tsqltTrends", "SQL Server tSQLt test results and analysis");
        dataStructure.put("summary", "Combined testing summary with health status");
        dataStructure.put("lookbackDays", "Number of days analyzed");
        
        endpoints.put("responseStructure", dataStructure);
        
        return ResponseEntity.ok(endpoints);
    }
} 