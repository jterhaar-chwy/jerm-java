package jerm.jerm_java.service;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class XMLFileService {
    
    @Value("${xml.directory.path:/data/xml}")
    private String xmlDirectoryPath;
    
    private final DocumentBuilderFactory documentBuilderFactory;
    private final XPathFactory xPathFactory;
    
    public XMLFileService() {
        this.documentBuilderFactory = DocumentBuilderFactory.newInstance();
        this.xPathFactory = XPathFactory.newInstance();
    }
    
    /**
     * Scan directory and get summary of XML files
     * @param directoryPath Optional directory path (uses default if null)
     * @return Map containing file summary information
     */
    public Map<String, Object> getXMLFilesSummary(String directoryPath) throws Exception {
        String targetPath = directoryPath != null ? directoryPath : xmlDirectoryPath;
        Path path = Paths.get(targetPath);
        
        if (!Files.exists(path) || !Files.isDirectory(path)) {
            throw new IllegalArgumentException("Directory does not exist: " + targetPath);
        }
        
        List<File> xmlFiles = Files.walk(path)
            .filter(Files::isRegularFile)
            .filter(p -> p.toString().toLowerCase().endsWith(".xml"))
            .map(Path::toFile)
            .collect(Collectors.toList());
        
        Map<String, Object> result = new HashMap<>();
        result.put("directoryPath", targetPath);
        result.put("totalXMLFiles", xmlFiles.size());
        result.put("files", xmlFiles.stream().map(f -> Map.of(
            "name", f.getName(),
            "path", f.getAbsolutePath(),
            "size", f.length(),
            "lastModified", new Date(f.lastModified()).toString()
        )).collect(Collectors.toList()));
        
        result.put("queryType", "xml_files_summary");
        result.put("description", "Summary of XML files in directory");
        result.put("executedAt", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        
        return result;
    }
    
    /**
     * Parse multiple XML files and extract specific elements for trending
     * @param directoryPath Directory containing XML files
     * @param xpathExpression XPath expression to extract data
     * @return Map containing aggregated trend data
     */
    public Map<String, Object> extractTrendDataFromXMLFiles(String directoryPath, String xpathExpression) throws Exception {
        String targetPath = directoryPath != null ? directoryPath : xmlDirectoryPath;
        Path path = Paths.get(targetPath);
        
        if (!Files.exists(path)) {
            throw new IllegalArgumentException("Directory does not exist: " + targetPath);
        }
        
        List<File> xmlFiles = Files.walk(path)
            .filter(Files::isRegularFile)
            .filter(p -> p.toString().toLowerCase().endsWith(".xml"))
            .map(Path::toFile)
            .sorted((f1, f2) -> Long.compare(f1.lastModified(), f2.lastModified()))
            .collect(Collectors.toList());
        
        List<Map<String, Object>> extractedData = new ArrayList<>();
        List<String> processingErrors = new ArrayList<>();
        
        DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
        XPath xpath = xPathFactory.newXPath();
        
        for (File xmlFile : xmlFiles) {
            try {
                Document document = documentBuilder.parse(xmlFile);
                document.getDocumentElement().normalize();
                
                // Extract data using XPath
                NodeList nodes = (NodeList) xpath.evaluate(xpathExpression, document, XPathConstants.NODESET);
                
                for (int i = 0; i < nodes.getLength(); i++) {
                    Node node = nodes.item(i);
                    Map<String, Object> extractedItem = new HashMap<>();
                    extractedItem.put("fileName", xmlFile.getName());
                    extractedItem.put("fileLastModified", new Date(xmlFile.lastModified()));
                    extractedItem.put("nodeValue", getNodeValue(node));
                    extractedItem.put("nodeAttributes", getNodeAttributes(node));
                    
                    extractedData.add(extractedItem);
                }
                
            } catch (Exception e) {
                processingErrors.add("Error processing " + xmlFile.getName() + ": " + e.getMessage());
            }
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("directoryPath", targetPath);
        result.put("xpathExpression", xpathExpression);
        result.put("filesProcessed", xmlFiles.size());
        result.put("extractedDataCount", extractedData.size());
        result.put("extractedData", extractedData);
        result.put("processingErrors", processingErrors);
        
        // Generate trend analysis
        result.put("trendAnalysis", generateTrendAnalysis(extractedData));
        
        result.put("queryType", "xml_trend_extraction");
        result.put("description", "Trend data extracted from multiple XML files");
        result.put("executedAt", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        
        return result;
    }
    
    /**
     * Parse XML files and extract common business metrics (example for log files, config files, etc.)
     * @param directoryPath Directory path
     * @return Map containing business metrics trends
     */
    public Map<String, Object> extractBusinessMetricsTrends(String directoryPath) throws Exception {
        String targetPath = directoryPath != null ? directoryPath : xmlDirectoryPath;
        Path path = Paths.get(targetPath);
        
        List<File> xmlFiles = Files.walk(path)
            .filter(Files::isRegularFile)
            .filter(p -> p.toString().toLowerCase().endsWith(".xml"))
            .map(Path::toFile)
            .sorted((f1, f2) -> Long.compare(f1.lastModified(), f2.lastModified()))
            .collect(Collectors.toList());
        
        List<Map<String, Object>> businessMetrics = new ArrayList<>();
        DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
        
        for (File xmlFile : xmlFiles) {
            try {
                Document document = documentBuilder.parse(xmlFile);
                document.getDocumentElement().normalize();
                
                Map<String, Object> fileMetrics = extractCommonMetrics(document, xmlFile);
                businessMetrics.add(fileMetrics);
                
            } catch (Exception e) {
                // Continue processing other files, but log the error
                System.err.println("Error processing " + xmlFile.getName() + ": " + e.getMessage());
            }
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("directoryPath", targetPath);
        result.put("filesProcessed", xmlFiles.size());
        result.put("businessMetrics", businessMetrics);
        
        // Generate aggregated trends
        result.put("aggregatedTrends", generateBusinessTrends(businessMetrics));
        
        result.put("queryType", "xml_business_metrics");
        result.put("description", "Business metrics trends from XML files");
        result.put("executedAt", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        
        return result;
    }
    
    /**
     * Advanced XML parsing with custom element extraction
     * @param directoryPath Directory path
     * @param elementSelectors Map of element selectors to extract
     * @return Map containing custom extracted data
     */
    public Map<String, Object> extractCustomElements(String directoryPath, Map<String, String> elementSelectors) throws Exception {
        String targetPath = directoryPath != null ? directoryPath : xmlDirectoryPath;
        Path path = Paths.get(targetPath);
        
        List<File> xmlFiles = Files.walk(path)
            .filter(Files::isRegularFile)
            .filter(p -> p.toString().toLowerCase().endsWith(".xml"))
            .map(Path::toFile)
            .collect(Collectors.toList());
        
        List<Map<String, Object>> extractedData = new ArrayList<>();
        DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
        XPath xpath = xPathFactory.newXPath();
        
        for (File xmlFile : xmlFiles) {
            try {
                Document document = documentBuilder.parse(xmlFile);
                
                Map<String, Object> fileData = new HashMap<>();
                fileData.put("fileName", xmlFile.getName());
                fileData.put("filePath", xmlFile.getAbsolutePath());
                fileData.put("fileLastModified", new Date(xmlFile.lastModified()));
                
                // Extract each requested element
                Map<String, Object> extractedElements = new HashMap<>();
                for (Map.Entry<String, String> selector : elementSelectors.entrySet()) {
                    String key = selector.getKey();
                    String xpathExpr = selector.getValue();
                    
                    try {
                        NodeList nodes = (NodeList) xpath.evaluate(xpathExpr, document, XPathConstants.NODESET);
                        List<Map<String, Object>> elementData = new ArrayList<>();
                        
                        for (int i = 0; i < nodes.getLength(); i++) {
                            Node node = nodes.item(i);
                            Map<String, Object> nodeData = new HashMap<>();
                            nodeData.put("value", getNodeValue(node));
                            nodeData.put("attributes", getNodeAttributes(node));
                            elementData.add(nodeData);
                        }
                        
                        extractedElements.put(key, elementData);
                    } catch (Exception e) {
                        extractedElements.put(key + "_error", e.getMessage());
                    }
                }
                
                fileData.put("extractedElements", extractedElements);
                extractedData.add(fileData);
                
            } catch (Exception e) {
                Map<String, Object> errorData = new HashMap<>();
                errorData.put("fileName", xmlFile.getName());
                errorData.put("error", e.getMessage());
                extractedData.add(errorData);
            }
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("directoryPath", targetPath);
        result.put("elementSelectors", elementSelectors);
        result.put("filesProcessed", xmlFiles.size());
        result.put("extractedData", extractedData);
        
        result.put("queryType", "xml_custom_extraction");
        result.put("description", "Custom element extraction from XML files");
        result.put("executedAt", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        
        return result;
    }
    
    // Helper methods
    
    private String getNodeValue(Node node) {
        if (node.getNodeType() == Node.TEXT_NODE || node.getNodeType() == Node.ATTRIBUTE_NODE) {
            return node.getNodeValue();
        } else if (node.getNodeType() == Node.ELEMENT_NODE) {
            return node.getTextContent();
        }
        return "";
    }
    
    private Map<String, String> getNodeAttributes(Node node) {
        Map<String, String> attributes = new HashMap<>();
        if (node.hasAttributes()) {
            for (int i = 0; i < node.getAttributes().getLength(); i++) {
                Node attr = node.getAttributes().item(i);
                attributes.put(attr.getNodeName(), attr.getNodeValue());
            }
        }
        return attributes;
    }
    
    private Map<String, Object> extractCommonMetrics(Document document, File xmlFile) {
        Map<String, Object> metrics = new HashMap<>();
        
        Element root = document.getDocumentElement();
        metrics.put("fileName", xmlFile.getName());
        metrics.put("fileLastModified", new Date(xmlFile.lastModified()));
        metrics.put("rootElementName", root.getTagName());
        
        // Count different types of elements
        metrics.put("totalElements", countElements(root));
        metrics.put("elementTypes", getElementTypeCounts(root));
        
        // Look for common business elements
        metrics.put("errorCount", countElementsByName(root, "error"));
        metrics.put("warningCount", countElementsByName(root, "warning"));
        metrics.put("configCount", countElementsByName(root, "config"));
        metrics.put("recordCount", countElementsByName(root, "record"));
        
        return metrics;
    }
    
    private int countElements(Element element) {
        int count = 1; // Count the current element
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                count += countElements((Element) child);
            }
        }
        return count;
    }
    
    private Map<String, Integer> getElementTypeCounts(Element element) {
        Map<String, Integer> counts = new HashMap<>();
        countElementTypes(element, counts);
        return counts;
    }
    
    private void countElementTypes(Element element, Map<String, Integer> counts) {
        String tagName = element.getTagName();
        counts.put(tagName, counts.getOrDefault(tagName, 0) + 1);
        
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                countElementTypes((Element) child, counts);
            }
        }
    }
    
    private int countElementsByName(Element element, String targetName) {
        int count = 0;
        if (element.getTagName().toLowerCase().contains(targetName.toLowerCase())) {
            count++;
        }
        
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                count += countElementsByName((Element) child, targetName);
            }
        }
        return count;
    }
    
    private Map<String, Object> generateTrendAnalysis(List<Map<String, Object>> extractedData) {
        Map<String, Object> trends = new HashMap<>();
        
        // Group by file for temporal analysis
        Map<String, List<Map<String, Object>>> fileGroups = extractedData.stream()
            .collect(Collectors.groupingBy(item -> (String) item.get("fileName")));
        
        trends.put("fileCount", fileGroups.size());
        trends.put("totalDataPoints", extractedData.size());
        trends.put("averageDataPointsPerFile", extractedData.size() / (double) Math.max(fileGroups.size(), 1));
        
        // Value frequency analysis
        Map<String, Long> valueFrequency = extractedData.stream()
            .collect(Collectors.groupingBy(
                item -> String.valueOf(item.get("nodeValue")),
                Collectors.counting()
            ));
        
        trends.put("uniqueValues", valueFrequency.size());
        trends.put("mostCommonValues", valueFrequency.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(10)
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (e1, e2) -> e1,
                LinkedHashMap::new
            )));
        
        return trends;
    }
    
    private Map<String, Object> generateBusinessTrends(List<Map<String, Object>> businessMetrics) {
        Map<String, Object> trends = new HashMap<>();
        
        if (businessMetrics.isEmpty()) {
            return trends;
        }
        
        // Calculate averages and totals
        OptionalDouble avgElements = businessMetrics.stream()
            .mapToInt(m -> (Integer) m.get("totalElements"))
            .average();
        
        int totalErrors = businessMetrics.stream()
            .mapToInt(m -> (Integer) m.get("errorCount"))
            .sum();
        
        int totalWarnings = businessMetrics.stream()
            .mapToInt(m -> (Integer) m.get("warningCount"))
            .sum();
        
        trends.put("averageElementsPerFile", avgElements.orElse(0.0));
        trends.put("totalErrorsAcrossFiles", totalErrors);
        trends.put("totalWarningsAcrossFiles", totalWarnings);
        trends.put("filesWithErrors", businessMetrics.stream()
            .mapToInt(m -> (Integer) m.get("errorCount") > 0 ? 1 : 0)
            .sum());
        
        // Most common root elements
        Map<String, Long> rootElementFrequency = businessMetrics.stream()
            .collect(Collectors.groupingBy(
                m -> (String) m.get("rootElementName"),
                Collectors.counting()
            ));
        
        trends.put("rootElementTypes", rootElementFrequency);
        
        return trends;
    }
} 