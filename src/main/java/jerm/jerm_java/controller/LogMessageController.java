package jerm.jerm_java.controller;

import jerm.jerm_java.service.LogMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/logs")
@CrossOrigin(origins = "http://localhost:3000")
public class LogMessageController {
    
    @Autowired
    private LogMessageService logMessageService;
    
    // System Monitoring Dashboard Endpoints
    
    @GetMapping("/errors/recent")
    public ResponseEntity<Map<String, Object>> getRecentDatabaseErrors(
            @RequestParam(defaultValue = "1") int daysBack) {
        try {
            Map<String, Object> result = logMessageService.getRecentDatabaseErrors(daysBack);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to get recent database errors: " + e.getMessage());
            error.put("daysBack", daysBack);
            return ResponseEntity.status(500).body(error);
        }
    }
    
    @GetMapping("/errors/trends")
    public ResponseEntity<Map<String, Object>> getHourlyErrorTrends(
            @RequestParam(defaultValue = "1") int daysBack) {
        try {
            Map<String, Object> result = logMessageService.getHourlyErrorTrends(daysBack);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to get error trends: " + e.getMessage());
            error.put("daysBack", daysBack);
            return ResponseEntity.status(500).body(error);
        }
    }
    
    @GetMapping("/system/health")
    public ResponseEntity<Map<String, Object>> getSystemHealthSummary() {
        try {
            Map<String, Object> result = logMessageService.getSystemHealthSummary();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to get system health summary: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
    
    // User Analytics Dashboard Endpoints
    
    @GetMapping("/users/top-active")
    public ResponseEntity<Map<String, Object>> getTopUsersByActivity(
            @RequestParam(defaultValue = "7") int daysBack,
            @RequestParam(defaultValue = "20") int topCount) {
        try {
            Map<String, Object> result = logMessageService.getTopUsersByActivity(daysBack, topCount);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to get top users by activity: " + e.getMessage());
            error.put("daysBack", daysBack);
            error.put("topCount", topCount);
            return ResponseEntity.status(500).body(error);
        }
    }
    
    @GetMapping("/users/activity-by-hour")
    public ResponseEntity<Map<String, Object>> getUserActivityByHour(
            @RequestParam(defaultValue = "7") int daysBack) {
        try {
            Map<String, Object> result = logMessageService.getUserActivityByHour(daysBack);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to get user activity by hour: " + e.getMessage());
            error.put("daysBack", daysBack);
            return ResponseEntity.status(500).body(error);
        }
    }
    
    // Operations Overview Dashboard Endpoints
    
    @GetMapping("/summary/daily")
    public ResponseEntity<Map<String, Object>> getDailySummaryByResourceType(
            @RequestParam(defaultValue = "7") int daysBack) {
        try {
            Map<String, Object> result = logMessageService.getDailySummaryByResourceType(daysBack);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to get daily summary: " + e.getMessage());
            error.put("daysBack", daysBack);
            return ResponseEntity.status(500).body(error);
        }
    }
    
    @GetMapping("/trends/daily-volume")
    public ResponseEntity<Map<String, Object>> getDailyVolumeTrends(
            @RequestParam(defaultValue = "30") int daysBack) {
        try {
            Map<String, Object> result = logMessageService.getDailyVolumeTrends(daysBack);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to get daily volume trends: " + e.getMessage());
            error.put("daysBack", daysBack);
            return ResponseEntity.status(500).body(error);
        }
    }
    
    // General Search and Utility Endpoints
    
    @PostMapping("/search")
    public ResponseEntity<Map<String, Object>> searchLogMessages(
            @RequestBody Map<String, Object> searchCriteria) {
        try {
            Map<String, Object> result = logMessageService.searchLogMessages(searchCriteria);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to search log messages: " + e.getMessage());
            error.put("searchCriteria", searchCriteria);
            return ResponseEntity.status(500).body(error);
        }
    }
    
    // Dashboard-specific aggregated endpoints
    
    @GetMapping("/dashboard/system-monitoring")
    public ResponseEntity<Map<String, Object>> getSystemMonitoringDashboard(
            @RequestParam(defaultValue = "1") int daysBack) {
        try {
            Map<String, Object> dashboard = new HashMap<>();
            
            // Get multiple metrics for the dashboard
            dashboard.put("recentErrors", logMessageService.getRecentDatabaseErrors(daysBack));
            dashboard.put("errorTrends", logMessageService.getHourlyErrorTrends(daysBack));
            dashboard.put("systemHealth", logMessageService.getSystemHealthSummary());
            
            dashboard.put("dashboardType", "system-monitoring");
            dashboard.put("daysBack", daysBack);
            
            return ResponseEntity.ok(dashboard);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to get system monitoring dashboard: " + e.getMessage());
            error.put("daysBack", daysBack);
            return ResponseEntity.status(500).body(error);
        }
    }
    
    @GetMapping("/dashboard/user-analytics")
    public ResponseEntity<Map<String, Object>> getUserAnalyticsDashboard(
            @RequestParam(defaultValue = "7") int daysBack) {
        try {
            Map<String, Object> dashboard = new HashMap<>();
            
            dashboard.put("topUsers", logMessageService.getTopUsersByActivity(daysBack, 20));
            dashboard.put("activityByHour", logMessageService.getUserActivityByHour(daysBack));
            
            dashboard.put("dashboardType", "user-analytics");
            dashboard.put("daysBack", daysBack);
            
            return ResponseEntity.ok(dashboard);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to get user analytics dashboard: " + e.getMessage());
            error.put("daysBack", daysBack);
            return ResponseEntity.status(500).body(error);
        }
    }
    
    @GetMapping("/dashboard/operations-overview")
    public ResponseEntity<Map<String, Object>> getOperationsOverviewDashboard(
            @RequestParam(defaultValue = "7") int daysBack) {
        try {
            Map<String, Object> dashboard = new HashMap<>();
            
            dashboard.put("dailySummary", logMessageService.getDailySummaryByResourceType(daysBack));
            dashboard.put("volumeTrends", logMessageService.getDailyVolumeTrends(daysBack));
            
            dashboard.put("dashboardType", "operations-overview");
            dashboard.put("daysBack", daysBack);
            
            return ResponseEntity.ok(dashboard);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to get operations overview dashboard: " + e.getMessage());
            error.put("daysBack", daysBack);
            return ResponseEntity.status(500).body(error);
        }
    }
} 