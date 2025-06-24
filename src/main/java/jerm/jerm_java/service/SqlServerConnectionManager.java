package jerm.jerm_java.service;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jerm.jerm_java.model.SqlServerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class SqlServerConnectionManager {
    
    private HikariDataSource dataSource;
    private SqlServerConfig config;
    
    // Statistics
    private final AtomicInteger totalConnections = new AtomicInteger(0);
    private final AtomicInteger activeConnections = new AtomicInteger(0);
    private final AtomicLong totalQueries = new AtomicLong(0);
    private final AtomicLong successfulQueries = new AtomicLong(0);
    private final AtomicLong failedQueries = new AtomicLong(0);
    
    // Environment variables
    @Value("${DB_HOST:localhost}")
    private String dbHost;
    
    @Value("${DB_PORT:1433}")
    private Integer dbPort;
    
    @Value("${DB_NAME:master}")
    private String dbName;
    
    @Value("${DB_USER:sa}")
    private String dbUser;
    
    @Value("${DB_PASSWORD:}")
    private String dbPassword;
    
    @Value("${DB_INTEGRATED_SECURITY:false}")
    private Boolean integratedSecurity;
    
    @PostConstruct
    public void initialize() {
        // Debug: Print the actual values being used
        System.out.println("=== SQL Server Configuration Debug ===");
        System.out.println("DB_HOST: " + dbHost);
        System.out.println("DB_PORT: " + dbPort);
        System.out.println("DB_NAME: " + dbName);
        System.out.println("DB_USER: " + dbUser);
        System.out.println("DB_PASSWORD: " + (dbPassword != null && !dbPassword.isEmpty() ? "[SET]" : "[NOT SET]"));
        System.out.println("DB_INTEGRATED_SECURITY: " + integratedSecurity);
        System.out.println("========================================");
        
        // Create configuration from environment variables
        config = new SqlServerConfig("default", dbHost, dbPort, dbName, dbUser, dbPassword);
        config.setIntegratedSecurity(integratedSecurity);
        
        System.out.println("JDBC URL: " + config.getJdbcUrl());
        
        // Initialize connection pool lazily to avoid startup failures
        try {
            initializeConnectionPool();
            System.out.println("SQL Server connection pool initialized successfully");
        } catch (Exception e) {
            System.out.println("SQL Server connection pool initialization deferred: " + e.getMessage());
            // Don't fail startup - connection pool will be initialized on first use
        }
    }
    
    private void initializeConnectionPool() {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(config.getJdbcUrl());
        
        if (!config.isIntegratedSecurity()) {
            hikariConfig.setUsername(config.getUsername());
            hikariConfig.setPassword(config.getPassword());
        }
        
        // Connection pool settings
        hikariConfig.setMaximumPoolSize(10);
        hikariConfig.setMinimumIdle(2);
        hikariConfig.setConnectionTimeout(30000);
        hikariConfig.setIdleTimeout(600000);
        hikariConfig.setMaxLifetime(1800000);
        
        // SQL Server specific settings
        hikariConfig.setDriverClassName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        hikariConfig.addDataSourceProperty("applicationName", "Jerm Java App");
        
        dataSource = new HikariDataSource(hikariConfig);
        
        System.out.println("SQL Server connection pool initialized for: " + config.toString());
    }
    
    public Connection getConnection() throws SQLException {
        if (dataSource == null) {
            // Try to initialize lazily
            try {
                initializeConnectionPool();
            } catch (Exception e) {
                throw new SQLException("Failed to initialize connection pool: " + e.getMessage());
            }
        }
        
        Connection connection = dataSource.getConnection();
        activeConnections.incrementAndGet();
        totalConnections.incrementAndGet();
        return connection;
    }
    
    public void releaseConnection(Connection connection) {
        if (connection != null) {
            try {
                connection.close();
                activeConnections.decrementAndGet();
            } catch (SQLException e) {
                System.err.println("Error releasing connection: " + e.getMessage());
            }
        }
    }
    
    public boolean testConnection() {
        try (Connection connection = getConnection()) {
            PreparedStatement stmt = connection.prepareStatement("SELECT 1");
            ResultSet rs = stmt.executeQuery();
            boolean hasResult = rs.next();
            releaseConnection(connection);
            return hasResult;
        } catch (SQLException e) {
            System.err.println("Connection test failed: " + e.getMessage());
            return false;
        }
    }
    
    public Map<String, Object> executeQuery(String sql) throws SQLException {
        long startTime = System.currentTimeMillis();
        totalQueries.incrementAndGet();
        
        try (Connection connection = getConnection()) {
            PreparedStatement stmt = connection.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            
            // Build result set data
            java.util.List<Map<String, Object>> rows = new java.util.ArrayList<>();
            java.util.List<String> columnNames = new java.util.ArrayList<>();
            
            // Get column metadata
            var metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            
            for (int i = 1; i <= columnCount; i++) {
                columnNames.add(metaData.getColumnName(i));
            }
            
            // Get row data (limit to first 100 rows for safety)
            int rowCount = 0;
            while (rs.next() && rowCount < 100) {
                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnName(i);
                    Object value = rs.getObject(i);
                    row.put(columnName, value);
                }
                rows.add(row);
                rowCount++;
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("executionTime", System.currentTimeMillis() - startTime);
            result.put("rowCount", rowCount);
            result.put("columns", columnNames);
            result.put("data", rows);
            result.put("hasMoreRows", rowCount == 100);
            
            successfulQueries.incrementAndGet();
            releaseConnection(connection);
            return result;
            
        } catch (SQLException e) {
            failedQueries.incrementAndGet();
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("error", e.getMessage());
            errorResult.put("executionTime", System.currentTimeMillis() - startTime);
            throw e;
        }
    }
    
    public Map<String, Object> getConnectionStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalConnections", totalConnections.get());
        stats.put("activeConnections", activeConnections.get());
        stats.put("availableConnections", dataSource != null ? dataSource.getHikariPoolMXBean().getIdleConnections() : 0);
        stats.put("totalQueries", totalQueries.get());
        stats.put("successfulQueries", successfulQueries.get());
        stats.put("failedQueries", failedQueries.get());
        stats.put("successRate", calculateSuccessRate());
        stats.put("isHealthy", isHealthy());
        stats.put("configuration", config.toString());
        return stats;
    }
    
    private double calculateSuccessRate() {
        long total = totalQueries.get();
        if (total == 0) return 100.0;
        return (successfulQueries.get() * 100.0) / total;
    }
    
    public boolean isHealthy() {
        return dataSource != null && !dataSource.isClosed() && testConnection();
    }
    
    @PreDestroy
    public void destroy() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            System.out.println("SQL Server connection pool closed");
        }
    }
} 