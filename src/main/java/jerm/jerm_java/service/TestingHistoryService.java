package jerm.jerm_java.service;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TestingHistoryService {
    
    @Value("${testing.history.base.directory:\\\\wmsdev-dev\\wmsdev\\Development Work\\AutomatedTestingHistory}")
    private String baseDirectory;
    
    @Value("${testing.history.days.back:7}")
    private int daysBack;
    
    /**
     * Get testing trends for the last N days
     * @param days Number of days to look back (default 7)
     * @return Map containing trend data for both Pester and tSQLt tests
     */
    public Map<String, Object> getTestingTrends(Integer days) throws Exception {
        int lookbackDays = days != null ? days : daysBack;
        
        Map<String, Object> result = new HashMap<>();
        result.put("lookbackDays", lookbackDays);
        result.put("baseDirectory", baseDirectory);
        
        try {
            // Get Pester test trends
            Map<String, Object> pesterTrends = getPesterTrends(lookbackDays);
            result.put("pesterTrends", pesterTrends);
        } catch (Exception e) {
            result.put("pesterError", e.getMessage());
        }
        
        try {
            // Get tSQLt test trends
            Map<String, Object> tsqltTrends = getTSQLtTrends(lookbackDays);
            result.put("tsqltTrends", tsqltTrends);
        } catch (Exception e) {
            result.put("tsqltError", e.getMessage());
        }
        
        try {
            // Generate combined summary
            Map<String, Object> summary = generateTestingSummary(result);
            result.put("summary", summary);
        } catch (Exception e) {
            result.put("summaryError", e.getMessage());
        }
        
        result.put("queryType", "testing_trends");
        result.put("description", "Automated testing history trends");
        result.put("executedAt", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        
        return result;
    }
    
    /**
     * Parse Pester XML test results
     * Pester typically uses NUnit XML format
     */
    private Map<String, Object> getPesterTrends(int days) throws Exception {
        String pesterPath = baseDirectory + File.separator + "Pester";
        Path pesterDir = Paths.get(pesterPath);
        
        if (!Files.exists(pesterDir)) {
            throw new RuntimeException("Pester directory not found: " + pesterPath);
        }
        
        List<Map<String, Object>> dailyResults = new ArrayList<>();
        LocalDate startDate = LocalDate.now().minusDays(days);
        
        // Look for XML files in the last N days
        List<File> xmlFiles = findRecentXmlFiles(pesterDir.toFile(), days);
        
        for (File xmlFile : xmlFiles) {
            try {
                Map<String, Object> testResult = parsePesterXml(xmlFile);
                if (testResult != null) {
                    dailyResults.add(testResult);
                }
            } catch (Exception e) {
                // Log error but continue processing other files
                System.err.println("Error parsing Pester file " + xmlFile.getName() + ": " + e.getMessage());
            }
        }
        
        // Sort by date
        dailyResults.sort((a, b) -> 
            ((String) a.get("date")).compareTo((String) b.get("date"))
        );
        
        Map<String, Object> trends = new HashMap<>();
        trends.put("testType", "Pester");
        trends.put("totalFiles", xmlFiles.size());
        trends.put("dailyResults", dailyResults);
        trends.put("trendAnalysis", analyzePesterTrends(dailyResults));
        
        return trends;
    }
    
    /**
     * Parse tSQLt XML test results
     * tSQLt typically uses custom XML format for SQL Server unit tests
     */
    private Map<String, Object> getTSQLtTrends(int days) throws Exception {
        String tsqltPath = baseDirectory + File.separator + "tSQLt";
        Path tsqltDir = Paths.get(tsqltPath);
        
        if (!Files.exists(tsqltDir)) {
            throw new RuntimeException("tSQLt directory not found: " + tsqltPath);
        }
        
        List<Map<String, Object>> dailyResults = new ArrayList<>();
        
        // Look for XML files in the last N days
        List<File> xmlFiles = findRecentXmlFiles(tsqltDir.toFile(), days);
        
        for (File xmlFile : xmlFiles) {
            try {
                Map<String, Object> testResult = parseTSQLtXml(xmlFile);
                if (testResult != null) {
                    dailyResults.add(testResult);
                }
            } catch (Exception e) {
                // Log error but continue processing other files
                System.err.println("Error parsing tSQLt file " + xmlFile.getName() + ": " + e.getMessage());
            }
        }
        
        // Sort by date
        dailyResults.sort((a, b) -> 
            ((String) a.get("date")).compareTo((String) b.get("date"))
        );
        
        Map<String, Object> trends = new HashMap<>();
        trends.put("testType", "tSQLt");
        trends.put("totalFiles", xmlFiles.size());
        trends.put("dailyResults", dailyResults);
        trends.put("trendAnalysis", analyzeTSQLtTrends(dailyResults));
        
        return trends;
    }
    
    /**
     * Parse Pester NUnit XML format
     */
    private Map<String, Object> parsePesterXml(File xmlFile) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(xmlFile);
        doc.getDocumentElement().normalize();
        
        Map<String, Object> result = new HashMap<>();
        result.put("fileName", xmlFile.getName());
        result.put("fileDate", getFileDate(xmlFile));
        result.put("date", LocalDate.now().toString()); // Will be overridden by actual date parsing
        
        // Parse test-results or test-run element (NUnit format)
        Element root = doc.getDocumentElement();
        
        if ("test-results".equals(root.getNodeName()) || "test-run".equals(root.getNodeName())) {
            // NUnit 2.x or 3.x format
            result.put("total", getIntAttribute(root, "total", 0));
            result.put("passed", getIntAttribute(root, "passed", 0));
            result.put("failed", getIntAttribute(root, "failed", 0));
            result.put("skipped", getIntAttribute(root, "skipped", 0));
            result.put("errors", getIntAttribute(root, "errors", 0));
            result.put("inconclusive", getIntAttribute(root, "inconclusive", 0));
            
            // Try to get execution time
            String time = root.getAttribute("time");
            if (time != null && !time.isEmpty()) {
                result.put("executionTimeSeconds", Double.parseDouble(time));
            }
            
            // Try to get date from attributes
            String date = root.getAttribute("date");
            String startTime = root.getAttribute("start-time");
            if (date != null && !date.isEmpty()) {
                result.put("date", date);
            }
            
        } else {
            // Try to parse as generic XML
            result.put("total", 0);
            result.put("passed", 0);
            result.put("failed", 0);
            result.put("skipped", 0);
        }
        
        // Calculate success rate
        int total = (Integer) result.get("total");
        int passed = (Integer) result.get("passed");
        if (total > 0) {
            result.put("successRate", (double) passed / total * 100);
        } else {
            result.put("successRate", 0.0);
        }
        
        return result;
    }
    
    /**
     * Parse tSQLt XML format
     */
    private Map<String, Object> parseTSQLtXml(File xmlFile) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(xmlFile);
        doc.getDocumentElement().normalize();
        
        Map<String, Object> result = new HashMap<>();
        result.put("fileName", xmlFile.getName());
        result.put("fileDate", getFileDate(xmlFile));
        result.put("date", LocalDate.now().toString());
        
        Element root = doc.getDocumentElement();
        
        // tSQLt specific parsing
        if ("TestResults".equals(root.getNodeName()) || "tSQLt".equals(root.getNodeName())) {
            // Count test cases
            NodeList testCases = doc.getElementsByTagName("TestCase");
            int total = testCases.getLength();
            int passed = 0;
            int failed = 0;
            
            for (int i = 0; i < testCases.getLength(); i++) {
                Element testCase = (Element) testCases.item(i);
                String result_attr = testCase.getAttribute("Result");
                if ("Success".equalsIgnoreCase(result_attr) || "Pass".equalsIgnoreCase(result_attr)) {
                    passed++;
                } else if ("Failure".equalsIgnoreCase(result_attr) || "Fail".equalsIgnoreCase(result_attr)) {
                    failed++;
                }
            }
            
            result.put("total", total);
            result.put("passed", passed);
            result.put("failed", failed);
            result.put("skipped", total - passed - failed);
            
            // Try to get execution time from XML
            NodeList durations = doc.getElementsByTagName("Duration");
            if (durations.getLength() > 0) {
                String duration = durations.item(0).getTextContent();
                try {
                    result.put("executionTimeSeconds", Double.parseDouble(duration));
                } catch (NumberFormatException e) {
                    result.put("executionTimeSeconds", 0.0);
                }
            }
            
        } else {
            // Generic XML parsing
            result.put("total", 0);
            result.put("passed", 0);
            result.put("failed", 0);
            result.put("skipped", 0);
        }
        
        // Calculate success rate
        int total = (Integer) result.get("total");
        int passed = (Integer) result.get("passed");
        if (total > 0) {
            result.put("successRate", (double) passed / total * 100);
        } else {
            result.put("successRate", 0.0);
        }
        
        return result;
    }
    
    /**
     * Find XML files modified within the last N days
     */
    private List<File> findRecentXmlFiles(File directory, int days) {
        List<File> xmlFiles = new ArrayList<>();
        long cutoffTime = System.currentTimeMillis() - (days * 24L * 60L * 60L * 1000L);
        
        if (directory.exists() && directory.isDirectory()) {
            File[] files = directory.listFiles((dir, name) -> 
                name.toLowerCase().endsWith(".xml") && 
                new File(dir, name).lastModified() >= cutoffTime
            );
            
            if (files != null) {
                Arrays.sort(files, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
                xmlFiles.addAll(Arrays.asList(files));
            }
        }
        
        return xmlFiles;
    }
    
    /**
     * Analyze Pester trends
     */
    private Map<String, Object> analyzePesterTrends(List<Map<String, Object>> dailyResults) {
        Map<String, Object> analysis = new HashMap<>();
        
        if (dailyResults.isEmpty()) {
            analysis.put("trend", "NO_DATA");
            return analysis;
        }
        
        // Calculate averages
        double avgSuccessRate = dailyResults.stream()
            .mapToDouble(r -> (Double) r.get("successRate"))
            .average().orElse(0.0);
        
        int totalTests = dailyResults.stream()
            .mapToInt(r -> (Integer) r.get("total"))
            .sum();
        
        int totalPassed = dailyResults.stream()
            .mapToInt(r -> (Integer) r.get("passed"))
            .sum();
        
        analysis.put("averageSuccessRate", avgSuccessRate);
        analysis.put("totalTests", totalTests);
        analysis.put("totalPassed", totalPassed);
        analysis.put("daysWithData", dailyResults.size());
        
        // Determine trend
        if (dailyResults.size() >= 2) {
            double firstRate = (Double) dailyResults.get(0).get("successRate");
            double lastRate = (Double) dailyResults.get(dailyResults.size() - 1).get("successRate");
            
            if (lastRate > firstRate + 5) {
                analysis.put("trend", "IMPROVING");
            } else if (lastRate < firstRate - 5) {
                analysis.put("trend", "DECLINING");
            } else {
                analysis.put("trend", "STABLE");
            }
        } else {
            analysis.put("trend", "INSUFFICIENT_DATA");
        }
        
        return analysis;
    }
    
    /**
     * Analyze tSQLt trends
     */
    private Map<String, Object> analyzeTSQLtTrends(List<Map<String, Object>> dailyResults) {
        // Similar analysis to Pester
        return analyzePesterTrends(dailyResults); // Reuse logic
    }
    
    /**
     * Generate combined testing summary
     */
    private Map<String, Object> generateTestingSummary(Map<String, Object> fullResults) {
        Map<String, Object> summary = new HashMap<>();
        
        // Extract data from both test types
        Map<String, Object> pesterTrends = (Map<String, Object>) fullResults.get("pesterTrends");
        Map<String, Object> tsqltTrends = (Map<String, Object>) fullResults.get("tsqltTrends");
        
        int totalTestFiles = 0;
        double overallSuccessRate = 0.0;
        String overallTrend = "UNKNOWN";
        
        if (pesterTrends != null) {
            totalTestFiles += (Integer) pesterTrends.getOrDefault("totalFiles", 0);
            Map<String, Object> pesterAnalysis = (Map<String, Object>) pesterTrends.get("trendAnalysis");
            if (pesterAnalysis != null) {
                overallSuccessRate += (Double) pesterAnalysis.getOrDefault("averageSuccessRate", 0.0);
            }
        }
        
        if (tsqltTrends != null) {
            totalTestFiles += (Integer) tsqltTrends.getOrDefault("totalFiles", 0);
            Map<String, Object> tsqltAnalysis = (Map<String, Object>) tsqltTrends.get("trendAnalysis");
            if (tsqltAnalysis != null) {
                overallSuccessRate += (Double) tsqltAnalysis.getOrDefault("averageSuccessRate", 0.0);
            }
        }
        
        // Average the success rates
        if (pesterTrends != null && tsqltTrends != null) {
            overallSuccessRate /= 2;
        }
        
        summary.put("totalTestFiles", totalTestFiles);
        summary.put("overallSuccessRate", overallSuccessRate);
        summary.put("overallTrend", overallTrend);
        summary.put("healthStatus", getHealthStatus(overallSuccessRate));
        
        return summary;
    }
    
    /**
     * Helper methods
     */
    private int getIntAttribute(Element element, String attributeName, int defaultValue) {
        String value = element.getAttribute(attributeName);
        if (value != null && !value.isEmpty()) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }
    
    private String getFileDate(File file) {
        long lastModified = file.lastModified();
        return LocalDateTime.ofEpochSecond(lastModified / 1000, 0, java.time.ZoneOffset.UTC)
            .format(DateTimeFormatter.ISO_LOCAL_DATE);
    }
    
    private String getHealthStatus(double successRate) {
        if (successRate >= 95) return "EXCELLENT";
        if (successRate >= 85) return "GOOD";
        if (successRate >= 70) return "FAIR";
        if (successRate >= 50) return "POOR";
        return "CRITICAL";
    }
} 