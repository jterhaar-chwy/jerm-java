package jerm.jerm_java.controller;

import jerm.jerm_java.service.XMLFileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/xml")
@CrossOrigin(origins = "http://localhost:3000")
public class XMLFileController {
    
    @Autowired
    private XMLFileService xmlFileService;
    
    /**
     * Get summary of XML files in a directory
     */
    @GetMapping("/files/summary")
    public ResponseEntity<Map<String, Object>> getXMLFilesSummary(
            @RequestParam(required = false) String directoryPath) {
        try {
            Map<String, Object> result = xmlFileService.getXMLFilesSummary(directoryPath);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to get XML files summary: " + e.getMessage());
            error.put("directoryPath", directoryPath);
            return ResponseEntity.status(500).body(error);
        }
    }
    
    /**
     * Extract trend data from XML files using XPath expression
     */
    @PostMapping("/extract/trends")
    public ResponseEntity<Map<String, Object>> extractTrendData(
            @RequestBody Map<String, String> request) {
        try {
            String directoryPath = request.get("directoryPath");
            String xpathExpression = request.get("xpathExpression");
            
            if (xpathExpression == null || xpathExpression.trim().isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "XPath expression is required");
                return ResponseEntity.badRequest().body(error);
            }
            
            Map<String, Object> result = xmlFileService.extractTrendDataFromXMLFiles(directoryPath, xpathExpression);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to extract trend data: " + e.getMessage());
            error.put("request", request);
            return ResponseEntity.status(500).body(error);
        }
    }
    
    /**
     * Extract business metrics trends from XML files
     */
    @GetMapping("/trends/business-metrics")
    public ResponseEntity<Map<String, Object>> getBusinessMetricsTrends(
            @RequestParam(required = false) String directoryPath) {
        try {
            Map<String, Object> result = xmlFileService.extractBusinessMetricsTrends(directoryPath);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to extract business metrics: " + e.getMessage());
            error.put("directoryPath", directoryPath);
            return ResponseEntity.status(500).body(error);
        }
    }
    
    /**
     * Extract custom elements from XML files using multiple XPath selectors
     */
    @PostMapping("/extract/custom")
    public ResponseEntity<Map<String, Object>> extractCustomElements(
            @RequestBody Map<String, Object> request) {
        try {
            String directoryPath = (String) request.get("directoryPath");
            
            @SuppressWarnings("unchecked")
            Map<String, String> elementSelectors = (Map<String, String>) request.get("elementSelectors");
            
            if (elementSelectors == null || elementSelectors.isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Element selectors are required");
                error.put("example", Map.of(
                    "elementSelectors", Map.of(
                        "timestamps", "//timestamp",
                        "errorMessages", "//error[@type='critical']",
                        "userIds", "//user/@id"
                    )
                ));
                return ResponseEntity.badRequest().body(error);
            }
            
            Map<String, Object> result = xmlFileService.extractCustomElements(directoryPath, elementSelectors);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to extract custom elements: " + e.getMessage());
            error.put("request", request);
            return ResponseEntity.status(500).body(error);
        }
    }
    
    /**
     * Comprehensive XML analytics dashboard endpoint
     */
    @GetMapping("/dashboard/analytics")
    public ResponseEntity<Map<String, Object>> getXMLAnalyticsDashboard(
            @RequestParam(required = false) String directoryPath) {
        try {
            Map<String, Object> dashboard = new HashMap<>();
            
            // Get file summary
            dashboard.put("fileSummary", xmlFileService.getXMLFilesSummary(directoryPath));
            
            // Get business metrics trends
            dashboard.put("businessMetrics", xmlFileService.extractBusinessMetricsTrends(directoryPath));
            
            dashboard.put("dashboardType", "xml-analytics");
            dashboard.put("directoryPath", directoryPath);
            
            return ResponseEntity.ok(dashboard);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to generate XML analytics dashboard: " + e.getMessage());
            error.put("directoryPath", directoryPath);
            return ResponseEntity.status(500).body(error);
        }
    }
    
    /**
     * Get available XPath examples for common XML structures
     */
    @GetMapping("/xpath/examples")
    public ResponseEntity<Map<String, Object>> getXPathExamples() {
        Map<String, Object> examples = new HashMap<>();
        
        Map<String, String> commonXPaths = new HashMap<>();
        commonXPaths.put("All elements", "//*");
        commonXPaths.put("Root element", "/*");
        commonXPaths.put("All text content", "//text()");
        commonXPaths.put("Elements with specific name", "//elementName");
        commonXPaths.put("Elements with attribute", "//*[@attributeName]");
        commonXPaths.put("Elements with specific attribute value", "//*[@type='error']");
        commonXPaths.put("Nested elements", "//parent/child");
        commonXPaths.put("Elements containing text", "//*[contains(text(), 'searchText')]");
        commonXPaths.put("First element of type", "//elementName[1]");
        commonXPaths.put("Last element of type", "//elementName[last()]");
        
        Map<String, String> logFileXPaths = new HashMap<>();
        logFileXPaths.put("Error messages", "//log[@level='ERROR']/message");
        logFileXPaths.put("Timestamps", "//log/@timestamp");
        logFileXPaths.put("User activities", "//log[contains(@message, 'user')]");
        logFileXPaths.put("Warning counts", "count(//log[@level='WARN'])");
        
        Map<String, String> configFileXPaths = new HashMap<>();
        configFileXPaths.put("Configuration values", "//config/setting/@value");
        configFileXPaths.put("Database settings", "//config[@type='database']");
        configFileXPaths.put("Environment variables", "//env/@name");
        
        examples.put("commonXPaths", commonXPaths);
        examples.put("logFileXPaths", logFileXPaths);
        examples.put("configFileXPaths", configFileXPaths);
        examples.put("description", "Common XPath expressions for XML parsing");
        
        return ResponseEntity.ok(examples);
    }
    
    /**
     * Test XML parsing with sample data
     */
    @PostMapping("/test/parse")
    public ResponseEntity<Map<String, Object>> testXMLParsing(
            @RequestBody Map<String, String> request) {
        try {
            String xmlContent = request.get("xmlContent");
            String xpathExpression = request.get("xpathExpression");
            
            if (xmlContent == null || xmlContent.trim().isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "XML content is required");
                return ResponseEntity.badRequest().body(error);
            }
            
            if (xpathExpression == null || xpathExpression.trim().isEmpty()) {
                xpathExpression = "//*"; // Default to all elements
            }
            
            // This would require a helper method in XMLFileService to parse XML content directly
            Map<String, Object> result = new HashMap<>();
            result.put("message", "XML parsing test endpoint - implement parseXMLContent method in service");
            result.put("xmlContent", xmlContent);
            result.put("xpathExpression", xpathExpression);
            result.put("note", "This endpoint can be enhanced to parse XML content directly for testing");
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to test XML parsing: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
} 