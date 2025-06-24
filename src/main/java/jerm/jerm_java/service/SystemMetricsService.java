package jerm.jerm_java.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class SystemMetricsService {
    
    @Autowired
    private SqlServerConnectionManager connectionManager;
    
    /**
     * Get connection pool health metrics
     * @return Map containing connection pool statistics
     */
    public Map<String, Object> getConnectionPoolMetrics() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Map<String, Object> stats = connectionManager.getConnectionStatistics();
            result.put("success", true);
            result.put("connectionPool", stats);
            result.put("healthy", connectionManager.isHealthy());
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
            result.put("healthy", false);
        }
        
        result.put("queryType", "connection_pool_metrics");
        result.put("description", "Current database connection pool health metrics");
        result.put("executedAt", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        
        return result;
    }
    
    /**
     * Get database performance metrics (example - would need actual performance tables)
     * @return Map containing database performance data
     */
    public Map<String, Object> getDatabasePerformanceMetrics() throws Exception {
        // This is an example of how you might query system performance tables
        String sql = """
            SELECT 
                'Database Size' as metric,
                SUM(size * 8.0 / 1024) as value_mb
            FROM sys.master_files
            WHERE database_id = DB_ID()
            UNION ALL
            SELECT 
                'Active Connections' as metric,
                COUNT(*) as value_mb
            FROM sys.dm_exec_sessions
            WHERE is_user_process = 1
            """;
        
        Map<String, Object> result = connectionManager.executeQuery(sql);
        
        result.put("queryType", "database_performance_metrics");
        result.put("description", "Database performance and resource utilization metrics");
        result.put("executedAt", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        
        return result;
    }
    
    /**
     * Get application health summary combining multiple metrics
     * @return Map containing overall application health
     */
    public Map<String, Object> getApplicationHealthSummary() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // Get connection health
            boolean dbHealthy = connectionManager.isHealthy();
            Map<String, Object> connectionStats = connectionManager.getConnectionStatistics();
            
            // Calculate overall health score
            int healthScore = 0;
            List<String> healthIssues = new ArrayList<>();
            
            if (dbHealthy) {
                healthScore += 50;
            } else {
                healthIssues.add("Database connection unhealthy");
            }
            
            // Check connection pool utilization
            if (connectionStats.containsKey("active") && connectionStats.containsKey("max")) {
                int active = (Integer) connectionStats.get("active");
                int max = (Integer) connectionStats.get("max");
                double utilization = (double) active / max;
                
                if (utilization < 0.8) {
                    healthScore += 25;
                } else if (utilization < 0.9) {
                    healthScore += 15;
                    healthIssues.add("High connection pool utilization: " + String.format("%.1f%%", utilization * 100));
                } else {
                    healthIssues.add("Critical connection pool utilization: " + String.format("%.1f%%", utilization * 100));
                }
            }
            
            // Add JVM memory check (example)
            Runtime runtime = Runtime.getRuntime();
            long maxMemory = runtime.maxMemory();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            double memoryUtilization = (double) usedMemory / maxMemory;
            
            if (memoryUtilization < 0.8) {
                healthScore += 25;
            } else if (memoryUtilization < 0.9) {
                healthScore += 15;
                healthIssues.add("High memory utilization: " + String.format("%.1f%%", memoryUtilization * 100));
            } else {
                healthIssues.add("Critical memory utilization: " + String.format("%.1f%%", memoryUtilization * 100));
            }
            
            // Determine overall health status
            String healthStatus;
            if (healthScore >= 90) {
                healthStatus = "EXCELLENT";
            } else if (healthScore >= 70) {
                healthStatus = "GOOD";
            } else if (healthScore >= 50) {
                healthStatus = "FAIR";
            } else {
                healthStatus = "POOR";
            }
            
            result.put("success", true);
            result.put("healthScore", healthScore);
            result.put("healthStatus", healthStatus);
            result.put("databaseHealthy", dbHealthy);
            result.put("connectionPool", connectionStats);
            result.put("memoryUtilization", String.format("%.1f%%", memoryUtilization * 100));
            result.put("healthIssues", healthIssues);
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
            result.put("healthStatus", "ERROR");
            result.put("healthScore", 0);
        }
        
        result.put("queryType", "application_health_summary");
        result.put("description", "Overall application health assessment");
        result.put("executedAt", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        
        return result;
    }
} 