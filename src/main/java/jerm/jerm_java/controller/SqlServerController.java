package jerm.jerm_java.controller;

import jerm.jerm_java.service.SqlServerConnectionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/database")
@CrossOrigin(origins = {"http://localhost:3000", "http://frontend:3000"})
public class SqlServerController {
    
    @Autowired
    private SqlServerConnectionManager connectionManager;
    
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getDatabaseStatus() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            boolean isHealthy = connectionManager.isHealthy();
            Map<String, Object> stats = connectionManager.getConnectionStatistics();
            
            response.put("status", isHealthy ? "connected" : "disconnected");
            response.put("healthy", isHealthy);
            response.put("statistics", stats);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("healthy", false);
            response.put("error", e.getMessage());
            return ResponseEntity.ok(response);
        }
    }
    
    @GetMapping("/test")
    public ResponseEntity<Map<String, Object>> testConnection() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            boolean testResult = connectionManager.testConnection();
            response.put("success", testResult);
            response.put("message", testResult ? "Connection test successful" : "Connection test failed");
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Connection test error: " + e.getMessage());
            response.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.ok(response);
        }
    }
    
    @GetMapping("/test/detailed")
    public ResponseEntity<Map<String, Object>> testConnectionDetailed() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Get configuration details first
            Map<String, Object> config = new HashMap<>();
            config.put("jdbcUrl", "jdbc:sqlserver://FLL2FHJDEV8935.chewy.local:1433;databaseName=master;encrypt=true;trustServerCertificate=true");
            config.put("host", "FLL2FHJDEV8935.chewy.local");
            config.put("port", 1433);
            config.put("database", "master");
            config.put("username", "CHEWY\\aa-jterhaar");
            config.put("encrypt", true);
            config.put("trustServerCertificate", true);
            
            response.put("configuration", config);
            
            // Test the actual connection
            boolean testResult = connectionManager.testConnection();
            response.put("connectionSuccess", testResult);
            
            if (testResult) {
                response.put("message", "✅ JDBC Connection successful!");
                response.put("status", "CONNECTED");
            } else {
                response.put("message", "❌ JDBC Connection failed - check logs for details");
                response.put("status", "FAILED");
            }
            
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("connectionSuccess", false);
            response.put("status", "ERROR");
            response.put("message", "Connection test error: " + e.getMessage());
            response.put("error", e.getClass().getSimpleName());
            response.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.ok(response);
        }
    }
    
    @PostMapping("/query")
    public ResponseEntity<Map<String, Object>> executeQuery(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String sql = request.get("sql");
            if (sql == null || sql.trim().isEmpty()) {
                response.put("success", false);
                response.put("error", "SQL query is required");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Simple validation - only allow SELECT statements for safety
            String trimmedSql = sql.trim().toUpperCase();
            if (!trimmedSql.startsWith("SELECT")) {
                response.put("success", false);
                response.put("error", "Only SELECT statements are allowed for security");
                return ResponseEntity.badRequest().body(response);
            }
            
            Map<String, Object> queryResult = connectionManager.executeQuery(sql);
            response.put("success", true);
            response.put("result", queryResult);
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            response.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.ok(response);
        }
    }
    
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        try {
            Map<String, Object> stats = connectionManager.getConnectionStatistics();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.ok(errorResponse);
        }
    }
    
    // Note: Service-based endpoints have been moved to dedicated controllers
    // - LogMessageController for log message queries
    // - XMLFileController for XML file processing  
    // - GitController for git operations
    
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getDatabaseInfo() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Database connection controller");
        response.put("description", "Provides basic database connectivity and health checks");
        response.put("availableEndpoints", Map.of(
            "status", "/api/database/status - Get database connection status",
            "test", "/api/database/test - Test database connection",
            "statistics", "/api/database/statistics - Get connection pool statistics",
            "query", "/api/database/query - Execute ad-hoc SELECT queries"
        ));
        response.put("note", "For structured queries, use dedicated service controllers: /api/logs, /api/xml, /api/git");
        return ResponseEntity.ok(response);
    }


} 