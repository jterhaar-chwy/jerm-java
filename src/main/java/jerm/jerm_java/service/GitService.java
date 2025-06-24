package jerm.jerm_java.service;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class GitService {
    
    @Value("${git.base.directory:/Users/jterhaar/repos}")
    private String baseDirectory;
    
    @Value("${git.timeout.seconds:30}")
    private int timeoutSeconds;
    
    /**
     * Get git branch information for a specific directory
     * @param directoryPath Path to the git repository
     * @return Map containing branch information
     */
    public Map<String, Object> getGitBranchInfo(String directoryPath) throws Exception {
        String targetPath = directoryPath != null ? directoryPath : baseDirectory;
        Path path = Paths.get(targetPath);
        
        if (!Files.exists(path) || !Files.isDirectory(path)) {
            throw new IllegalArgumentException("Directory does not exist: " + targetPath);
        }
        
        if (!isGitRepository(targetPath)) {
            throw new IllegalArgumentException("Directory is not a git repository: " + targetPath);
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("directoryPath", targetPath);
        
        // Get current branch
        String currentBranch = executeGitCommand(targetPath, "git", "branch", "--show-current");
        result.put("currentBranch", currentBranch.trim());
        
        // Get all branches
        String allBranches = executeGitCommand(targetPath, "git", "branch", "-a");
        List<String> branches = Arrays.stream(allBranches.split("\n"))
            .map(String::trim)
            .filter(line -> !line.isEmpty())
            .collect(Collectors.toList());
        result.put("allBranches", branches);
        
        // Get remote origin URL
        try {
            String remoteUrl = executeGitCommand(targetPath, "git", "remote", "get-url", "origin");
            result.put("remoteUrl", remoteUrl.trim());
        } catch (Exception e) {
            result.put("remoteUrl", "No remote origin found");
        }
        
        // Get latest commit info
        String latestCommit = executeGitCommand(targetPath, "git", "log", "-1", "--pretty=format:%H|%an|%ad|%s", "--date=iso");
        if (!latestCommit.trim().isEmpty()) {
            String[] commitParts = latestCommit.split("\\|");
            if (commitParts.length >= 4) {
                Map<String, String> commitInfo = new HashMap<>();
                commitInfo.put("hash", commitParts[0]);
                commitInfo.put("author", commitParts[1]);
                commitInfo.put("date", commitParts[2]);
                commitInfo.put("message", commitParts[3]);
                result.put("latestCommit", commitInfo);
            }
        }
        
        // Get git status
        String gitStatus = executeGitCommand(targetPath, "git", "status", "--porcelain");
        result.put("hasUncommittedChanges", !gitStatus.trim().isEmpty());
        result.put("statusDetails", gitStatus.trim());
        
        result.put("queryType", "git_branch_info");
        result.put("description", "Git branch and repository information");
        result.put("executedAt", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        
        return result;
    }
    
    /**
     * Get git information for multiple directories
     * @param directoryPaths List of directory paths to check
     * @return Map containing information for all directories
     */
    public Map<String, Object> getMultipleRepositoryInfo(List<String> directoryPaths) throws Exception {
        List<Map<String, Object>> repositoryInfoList = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        
        for (String dirPath : directoryPaths) {
            try {
                Map<String, Object> repoInfo = getGitBranchInfo(dirPath);
                repositoryInfoList.add(repoInfo);
            } catch (Exception e) {
                errors.add("Error processing " + dirPath + ": " + e.getMessage());
            }
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("repositoriesProcessed", directoryPaths.size());
        result.put("successfulRepositories", repositoryInfoList.size());
        result.put("repositoryInfo", repositoryInfoList);
        result.put("errors", errors);
        
        result.put("queryType", "multiple_repository_info");
        result.put("description", "Git information for multiple repositories");
        result.put("executedAt", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        
        return result;
    }
    
    /**
     * List GitHub Actions workflows using gh CLI
     * @param repositoryPath Path to the repository
     * @return Map containing workflow information
     */
    public Map<String, Object> listGitHubWorkflows(String repositoryPath) throws Exception {
        String targetPath = repositoryPath != null ? repositoryPath : baseDirectory;
        
        if (!isGitRepository(targetPath)) {
            throw new IllegalArgumentException("Directory is not a git repository: " + targetPath);
        }
        
        // Check if gh CLI is available
        try {
            executeCommand(targetPath, "gh", "--version");
        } catch (Exception e) {
            throw new RuntimeException("GitHub CLI (gh) is not installed or not available in PATH");
        }
        
        // List workflows - using correct field names
        String workflowsJson = executeCommand(targetPath, "gh", "workflow", "list", "--json", "id,name,state,path");
        
        Map<String, Object> result = new HashMap<>();
        result.put("repositoryPath", targetPath);
        result.put("workflowsRaw", workflowsJson);
        
        // Parse JSON if possible (you might want to add Jackson parsing here)
        result.put("note", "Workflows listed in JSON format - consider adding JSON parsing");
        
        result.put("queryType", "github_workflows");
        result.put("description", "GitHub Actions workflows for repository");
        result.put("executedAt", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        
        return result;
    }
    
    /**
     * Trigger a GitHub Actions workflow
     * @param repositoryPath Path to the repository  
     * @param workflowId Workflow ID or filename
     * @param ref Branch/tag to run workflow on (optional)
     * @return Map containing trigger result
     */
    public Map<String, Object> triggerGitHubWorkflow(String repositoryPath, String workflowId, String ref) throws Exception {
        String targetPath = repositoryPath != null ? repositoryPath : baseDirectory;
        
        if (!isGitRepository(targetPath)) {
            throw new IllegalArgumentException("Directory is not a git repository: " + targetPath);
        }
        
        List<String> command = new ArrayList<>();
        command.add("gh");
        command.add("workflow");
        command.add("run");
        command.add(workflowId);
        
        if (ref != null && !ref.trim().isEmpty()) {
            command.add("--ref");
            command.add(ref);
        }
        
        String result = executeCommand(targetPath, command.toArray(new String[0]));
        
        Map<String, Object> response = new HashMap<>();
        response.put("repositoryPath", targetPath);
        response.put("workflowId", workflowId);
        response.put("ref", ref);
        response.put("triggerResult", result);
        response.put("success", true);
        
        response.put("queryType", "github_workflow_trigger");
        response.put("description", "GitHub Actions workflow trigger result");
        response.put("executedAt", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        
        return response;
    }
    
    /**
     * Get GitHub Actions workflow runs
     * @param repositoryPath Path to the repository
     * @param limit Number of runs to retrieve (default 10)
     * @return Map containing workflow runs
     */
    public Map<String, Object> getWorkflowRuns(String repositoryPath, int limit) throws Exception {
        String targetPath = repositoryPath != null ? repositoryPath : baseDirectory;
        
        if (!isGitRepository(targetPath)) {
            throw new IllegalArgumentException("Directory is not a git repository: " + targetPath);
        }
        
        String runsJson = executeCommand(targetPath, "gh", "run", "list", 
            "--limit", String.valueOf(limit), 
            "--json", "conclusion,createdAt,displayTitle,event,headBranch,databaseId,name,status,url");
        
        Map<String, Object> result = new HashMap<>();
        result.put("repositoryPath", targetPath);
        result.put("limit", limit);
        result.put("runsRaw", runsJson);
        
        result.put("queryType", "github_workflow_runs");
        result.put("description", "Recent GitHub Actions workflow runs");
        result.put("executedAt", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        
        return result;
    }
    
    /**
     * Get repository information using gh CLI
     * @param repositoryPath Path to the repository
     * @return Map containing repository information
     */
    public Map<String, Object> getRepositoryInfo(String repositoryPath) throws Exception {
        String targetPath = repositoryPath != null ? repositoryPath : baseDirectory;
        
        if (!isGitRepository(targetPath)) {
            throw new IllegalArgumentException("Directory is not a git repository: " + targetPath);
        }
        
        String repoJson = executeCommand(targetPath, "gh", "repo", "view", 
            "--json", "name,owner,description,url,isPrivate,defaultBranchRef,stargazerCount,forkCount");
        
        Map<String, Object> result = new HashMap<>();
        result.put("repositoryPath", targetPath);
        result.put("repositoryInfoRaw", repoJson);
        
        result.put("queryType", "github_repository_info");
        result.put("description", "GitHub repository information");
        result.put("executedAt", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        
        return result;
    }
    
    /**
     * Get comprehensive git dashboard combining multiple sources
     * @param repositoryPath Path to the repository
     * @return Map containing comprehensive git information
     */
    public Map<String, Object> getGitDashboard(String repositoryPath) throws Exception {
        String targetPath = repositoryPath != null ? repositoryPath : baseDirectory;
        
        Map<String, Object> dashboard = new HashMap<>();
        
        try {
            // Get git branch info
            dashboard.put("branchInfo", getGitBranchInfo(targetPath));
        } catch (Exception e) {
            dashboard.put("branchInfoError", e.getMessage());
        }
        
        try {
            // Get GitHub repository info
            dashboard.put("repositoryInfo", getRepositoryInfo(targetPath));
        } catch (Exception e) {
            dashboard.put("repositoryInfoError", e.getMessage());
        }
        
        try {
            // Get recent workflow runs
            dashboard.put("recentRuns", getWorkflowRuns(targetPath, 5));
        } catch (Exception e) {
            dashboard.put("recentRunsError", e.getMessage());
        }
        
        try {
            // Get workflows
            dashboard.put("workflows", listGitHubWorkflows(targetPath));
        } catch (Exception e) {
            dashboard.put("workflowsError", e.getMessage());
        }
        
        dashboard.put("dashboardType", "git-comprehensive");
        dashboard.put("repositoryPath", targetPath);
        dashboard.put("executedAt", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        
        return dashboard;
    }
    
    // Helper methods
    
    private boolean isGitRepository(String directoryPath) {
        Path gitDir = Paths.get(directoryPath, ".git");
        return Files.exists(gitDir);
    }
    
    private String executeGitCommand(String workingDirectory, String... command) throws Exception {
        return executeCommand(workingDirectory, command);
    }
    
    private String executeCommand(String workingDirectory, String... command) throws Exception {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(new File(workingDirectory));
        processBuilder.redirectErrorStream(true);
        
        Process process = processBuilder.start();
        
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }
        
        boolean finished = process.waitFor(timeoutSeconds, java.util.concurrent.TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new RuntimeException("Command timed out after " + timeoutSeconds + " seconds");
        }
        
        int exitCode = process.exitValue();
        if (exitCode != 0) {
            throw new RuntimeException("Command failed with exit code " + exitCode + ": " + output.toString());
        }
        
        return output.toString();
    }
} 