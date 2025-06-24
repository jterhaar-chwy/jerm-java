package jerm.jerm_java.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class LogMessageService {
    
    @Autowired
    private SqlServerConnectionManager connectionManager;
    
    /**
     * Get recent database execution errors from t_log_message
     * @param daysBack Number of days to look back (positive number, e.g., 1 for last day)
     * @return Map containing query results and metadata
     */
    public Map<String, Object> getRecentDatabaseErrors(int daysBack) throws Exception {
        String sql = """
            SELECT TOP 10000 
                logged_on_local, 
                machine_id, 
                user_id, 
                resource_name, 
                details, 
                call_stack, 
                arguments 
            FROM ADV.dbo.t_log_message WITH (NOLOCK) 
            WHERE logged_on_utc >= DATEADD(day, ?, GETUTCDATE()) 
                AND resource_name LIKE 'CANT_EXE_DB%' 
                AND call_stack <> '1: Process Exacta Divert Confirmation:32' 
            ORDER BY logged_on_utc DESC
            """;
        
        Map<String, Object> result = connectionManager.executeQuery(sql.replace("?", String.valueOf(-daysBack)));
        
        // Add metadata
        result.put("queryType", "recent_database_errors");
        result.put("daysBack", daysBack);
        result.put("description", "Recent database execution errors from t_log_message");
        result.put("executedAt", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        
        return result;
    }
    
    /**
     * Get daily summary of log messages by resource type
     * @param daysBack Number of days to analyze
     * @return Map containing aggregated daily statistics
     */
    public Map<String, Object> getDailySummaryByResourceType(int daysBack) throws Exception {
        String sql = """
            SELECT 
                CAST(logged_on_local AS DATE) as log_date, 
                resource_name, 
                COUNT(*) as message_count 
            FROM ADV.dbo.t_log_message WITH (NOLOCK) 
            WHERE logged_on_utc >= DATEADD(day, ?, GETUTCDATE()) 
            GROUP BY CAST(logged_on_local AS DATE), resource_name 
            ORDER BY log_date DESC, message_count DESC
            """;
        
        Map<String, Object> result = connectionManager.executeQuery(sql.replace("?", String.valueOf(-daysBack)));
        
        result.put("queryType", "daily_summary_by_resource");
        result.put("daysBack", daysBack);
        result.put("description", "Daily summary of log messages grouped by resource type");
        result.put("executedAt", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        
        return result;
    }
    
    /**
     * Get hourly error trends for monitoring
     * @param daysBack Number of days to analyze
     * @return Map containing hourly error statistics
     */
    public Map<String, Object> getHourlyErrorTrends(int daysBack) throws Exception {
        String sql = """
            SELECT 
                DATEPART(hour, logged_on_local) as hour_of_day, 
                COUNT(*) as error_count 
            FROM ADV.dbo.t_log_message WITH (NOLOCK) 
            WHERE logged_on_utc >= DATEADD(day, ?, GETUTCDATE()) 
                AND resource_name LIKE '%ERROR%' 
            GROUP BY DATEPART(hour, logged_on_local) 
            ORDER BY hour_of_day
            """;
        
        Map<String, Object> result = connectionManager.executeQuery(sql.replace("?", String.valueOf(-daysBack)));
        
        result.put("queryType", "hourly_error_trends");
        result.put("daysBack", daysBack);
        result.put("description", "Hourly error trends for system monitoring");
        result.put("executedAt", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        
        return result;
    }
    
    /**
     * Get top users by activity volume
     * @param daysBack Number of days to analyze
     * @param topCount Number of top users to return (default 20)
     * @return Map containing top user activity statistics
     */
    public Map<String, Object> getTopUsersByActivity(int daysBack, int topCount) throws Exception {
        String sql = """
            SELECT TOP %d 
                user_id, 
                COUNT(*) as activity_count, 
                MIN(logged_on_local) as first_activity, 
                MAX(logged_on_local) as last_activity 
            FROM ADV.dbo.t_log_message WITH (NOLOCK) 
            WHERE logged_on_utc >= DATEADD(day, ?, GETUTCDATE()) 
            GROUP BY user_id 
            ORDER BY activity_count DESC
            """.formatted(topCount);
        
        Map<String, Object> result = connectionManager.executeQuery(sql.replace("?", String.valueOf(-daysBack)));
        
        result.put("queryType", "top_users_by_activity");
        result.put("daysBack", daysBack);
        result.put("topCount", topCount);
        result.put("description", "Most active users based on log message volume");
        result.put("executedAt", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        
        return result;
    }
    
    /**
     * Get user activity patterns by hour of day
     * @param daysBack Number of days to analyze
     * @return Map containing hourly user activity patterns
     */
    public Map<String, Object> getUserActivityByHour(int daysBack) throws Exception {
        String sql = """
            SELECT 
                DATEPART(hour, logged_on_local) as hour_of_day, 
                COUNT(DISTINCT user_id) as unique_users, 
                COUNT(*) as total_activities 
            FROM ADV.dbo.t_log_message WITH (NOLOCK) 
            WHERE logged_on_utc >= DATEADD(day, ?, GETUTCDATE()) 
            GROUP BY DATEPART(hour, logged_on_local) 
            ORDER BY hour_of_day
            """;
        
        Map<String, Object> result = connectionManager.executeQuery(sql.replace("?", String.valueOf(-daysBack)));
        
        result.put("queryType", "user_activity_by_hour");
        result.put("daysBack", daysBack);
        result.put("description", "User activity patterns throughout the day");
        result.put("executedAt", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        
        return result;
    }
    
    /**
     * Get system health summary metrics
     * @return Map containing current system health indicators
     */
    public Map<String, Object> getSystemHealthSummary() throws Exception {
        String sql = """
            SELECT 'Database Errors' as metric, COUNT(*) as count 
            FROM ADV.dbo.t_log_message WITH (NOLOCK) 
            WHERE logged_on_utc >= DATEADD(hour, -1, GETUTCDATE()) 
                AND resource_name LIKE '%ERROR%' 
            UNION ALL 
            SELECT 'Total Messages' as metric, COUNT(*) as count 
            FROM ADV.dbo.t_log_message WITH (NOLOCK) 
            WHERE logged_on_utc >= DATEADD(hour, -1, GETUTCDATE())
            UNION ALL
            SELECT 'Unique Users (Last Hour)' as metric, COUNT(DISTINCT user_id) as count 
            FROM ADV.dbo.t_log_message WITH (NOLOCK) 
            WHERE logged_on_utc >= DATEADD(hour, -1, GETUTCDATE())
            UNION ALL
            SELECT 'Unique Machines (Last Hour)' as metric, COUNT(DISTINCT machine_id) as count 
            FROM ADV.dbo.t_log_message WITH (NOLOCK) 
            WHERE logged_on_utc >= DATEADD(hour, -1, GETUTCDATE())
            """;
        
        Map<String, Object> result = connectionManager.executeQuery(sql);
        
        result.put("queryType", "system_health_summary");
        result.put("description", "Current system health metrics for the last hour");
        result.put("executedAt", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        
        return result;
    }
    
    /**
     * Get daily volume trends with multiple metrics
     * @param daysBack Number of days to analyze
     * @return Map containing daily volume trend data
     */
    public Map<String, Object> getDailyVolumeTrends(int daysBack) throws Exception {
        String sql = """
            SELECT 
                CAST(logged_on_local AS DATE) as log_date, 
                COUNT(*) as total_messages, 
                COUNT(DISTINCT user_id) as unique_users, 
                COUNT(DISTINCT machine_id) as unique_machines,
                COUNT(CASE WHEN resource_name LIKE '%ERROR%' THEN 1 END) as error_count
            FROM ADV.dbo.t_log_message WITH (NOLOCK) 
            WHERE logged_on_utc >= DATEADD(day, ?, GETUTCDATE()) 
            GROUP BY CAST(logged_on_local AS DATE) 
            ORDER BY log_date DESC
            """;
        
        Map<String, Object> result = connectionManager.executeQuery(sql.replace("?", String.valueOf(-daysBack)));
        
        result.put("queryType", "daily_volume_trends");
        result.put("daysBack", daysBack);
        result.put("description", "Daily volume trends with multiple system metrics");
        result.put("executedAt", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        
        return result;
    }
    
    /**
     * Search log messages by criteria
     * @param searchCriteria Map containing search parameters
     * @return Map containing filtered log messages
     */
    public Map<String, Object> searchLogMessages(Map<String, Object> searchCriteria) throws Exception {
        StringBuilder sql = new StringBuilder("""
            SELECT TOP 1000 
                logged_on_local, 
                machine_id, 
                user_id, 
                resource_name, 
                details, 
                call_stack, 
                arguments 
            FROM ADV.dbo.t_log_message WITH (NOLOCK) 
            WHERE 1=1
            """);
        
        List<String> conditions = new ArrayList<>();
        
        // Add dynamic conditions based on search criteria
        if (searchCriteria.containsKey("daysBack")) {
            int daysBack = (Integer) searchCriteria.get("daysBack");
            conditions.add("logged_on_utc >= DATEADD(day, " + (-daysBack) + ", GETUTCDATE())");
        }
        
        if (searchCriteria.containsKey("userId") && !((String) searchCriteria.get("userId")).isEmpty()) {
            String userId = (String) searchCriteria.get("userId");
            conditions.add("user_id LIKE '%" + userId.replace("'", "''") + "%'");
        }
        
        if (searchCriteria.containsKey("resourceName") && !((String) searchCriteria.get("resourceName")).isEmpty()) {
            String resourceName = (String) searchCriteria.get("resourceName");
            conditions.add("resource_name LIKE '%" + resourceName.replace("'", "''") + "%'");
        }
        
        if (searchCriteria.containsKey("machineId") && !((String) searchCriteria.get("machineId")).isEmpty()) {
            String machineId = (String) searchCriteria.get("machineId");
            conditions.add("machine_id LIKE '%" + machineId.replace("'", "''") + "%'");
        }
        
        // Add conditions to SQL
        for (String condition : conditions) {
            sql.append(" AND ").append(condition);
        }
        
        sql.append(" ORDER BY logged_on_utc DESC");
        
        Map<String, Object> result = connectionManager.executeQuery(sql.toString());
        
        result.put("queryType", "search_log_messages");
        result.put("searchCriteria", searchCriteria);
        result.put("description", "Filtered log messages based on search criteria");
        result.put("executedAt", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        
        return result;
    }
} 