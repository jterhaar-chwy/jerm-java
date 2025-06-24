package jerm.jerm_java.controller;

import jerm.jerm_java.service.GitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/git")
@CrossOrigin(origins = {"http://localhost:3000", "http://frontend:3000"})
public class GitController {

    @Autowired
    private GitService gitService;

    // ============== Git Branch and Repository Information ==============
    
    /**
     * Get git branch information for a specific directory
     * GET /api/git/branch-info?directoryPath=/path/to/repo
     */
    @GetMapping("/branch-info")
    public ResponseEntity<Map<String, Object>> getBranchInfo(
            @RequestParam(required = false) String directoryPath) {
        try {
            Map<String, Object> result = gitService.getGitBranchInfo(directoryPath);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                Map.of("error", e.getMessage(), 
                      "queryType", "git_branch_info_error")
            );
        }
    }
    
    /**
     * Get git information for multiple directories
     * POST /api/git/multiple-repos
     * Request Body: {"directoryPaths": ["/path/to/repo1", "/path/to/repo2"]}
     */
    @PostMapping("/multiple-repos")
    public ResponseEntity<Map<String, Object>> getMultipleRepositoryInfo(
            @RequestBody Map<String, List<String>> request) {
        try {
            List<String> directoryPaths = request.get("directoryPaths");
            if (directoryPaths == null || directoryPaths.isEmpty()) {
                return ResponseEntity.badRequest().body(
                    Map.of("error", "directoryPaths parameter is required", 
                          "queryType", "multiple_repository_info_error")
                );
            }
            
            Map<String, Object> result = gitService.getMultipleRepositoryInfo(directoryPaths);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                Map.of("error", e.getMessage(), 
                      "queryType", "multiple_repository_info_error")
            );
        }
    }

    // ============== GitHub Actions Integration ==============
    
    /**
     * List GitHub Actions workflows
     * GET /api/git/workflows?repositoryPath=/path/to/repo
     */
    @GetMapping("/workflows")
    public ResponseEntity<Map<String, Object>> listWorkflows(
            @RequestParam(required = false) String repositoryPath) {
        try {
            Map<String, Object> result = gitService.listGitHubWorkflows(repositoryPath);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                Map.of("error", e.getMessage(), 
                      "queryType", "github_workflows_error")
            );
        }
    }
    
    /**
     * Trigger a GitHub Actions workflow
     * POST /api/git/workflows/trigger
     * Request Body: {"repositoryPath": "/path/to/repo", "workflowId": "workflow.yml", "ref": "main"}
     */
    @PostMapping("/workflows/trigger")
    public ResponseEntity<Map<String, Object>> triggerWorkflow(
            @RequestBody Map<String, String> request) {
        try {
            String repositoryPath = request.get("repositoryPath");
            String workflowId = request.get("workflowId");
            String ref = request.get("ref");
            
            if (workflowId == null || workflowId.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                    Map.of("error", "workflowId parameter is required", 
                          "queryType", "github_workflow_trigger_error")
                );
            }
            
            Map<String, Object> result = gitService.triggerGitHubWorkflow(repositoryPath, workflowId, ref);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                Map.of("error", e.getMessage(), 
                      "queryType", "github_workflow_trigger_error")
            );
        }
    }
    
    /**
     * Get GitHub Actions workflow runs
     * GET /api/git/workflow-runs?repositoryPath=/path/to/repo&limit=10
     */
    @GetMapping("/workflow-runs")
    public ResponseEntity<Map<String, Object>> getWorkflowRuns(
            @RequestParam(required = false) String repositoryPath,
            @RequestParam(defaultValue = "10") int limit) {
        try {
            Map<String, Object> result = gitService.getWorkflowRuns(repositoryPath, limit);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                Map.of("error", e.getMessage(), 
                      "queryType", "github_workflow_runs_error")
            );
        }
    }

    // ============== GitHub Repository Information ==============
    
    /**
     * Get GitHub repository information
     * GET /api/git/repo-info?repositoryPath=/path/to/repo
     */
    @GetMapping("/repo-info")
    public ResponseEntity<Map<String, Object>> getRepositoryInfo(
            @RequestParam(required = false) String repositoryPath) {
        try {
            Map<String, Object> result = gitService.getRepositoryInfo(repositoryPath);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                Map.of("error", e.getMessage(), 
                      "queryType", "github_repository_info_error")
            );
        }
    }

    // ============== Dashboard and Comprehensive Views ==============
    
    /**
     * Get comprehensive git dashboard for a repository
     * GET /api/git/dashboard?repositoryPath=/path/to/repo
     */
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getGitDashboard(
            @RequestParam(required = false) String repositoryPath) {
        try {
            Map<String, Object> result = gitService.getGitDashboard(repositoryPath);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                Map.of("error", e.getMessage(), 
                      "queryType", "git_dashboard_error")
            );
        }
    }

    // ============== API Documentation ==============
    
    /**
     * Get Git API endpoints documentation
     * GET /api/git/endpoints
     */
    @GetMapping("/endpoints")
    public ResponseEntity<Map<String, Object>> getApiEndpoints() {
        Map<String, Object> endpoints = Map.of(
            "git_operations", Map.of(
                "branch_info", Map.of(
                    "method", "GET",
                    "endpoint", "/api/git/branch-info",
                    "parameters", Map.of("directoryPath", "Path to git repository (optional)"),
                    "description", "Get git branch and status information for a repository"
                ),
                "multiple_repos", Map.of(
                    "method", "POST",
                    "endpoint", "/api/git/multiple-repos",
                    "body", Map.of("directoryPaths", "Array of repository paths"),
                    "description", "Get git information for multiple repositories"
                )
            ),
            "github_actions", Map.of(
                "list_workflows", Map.of(
                    "method", "GET",
                    "endpoint", "/api/git/workflows",
                    "parameters", Map.of("repositoryPath", "Path to repository (optional)"),
                    "description", "List GitHub Actions workflows"
                ),
                "trigger_workflow", Map.of(
                    "method", "POST",
                    "endpoint", "/api/git/workflows/trigger",
                    "body", Map.of(
                        "workflowId", "Workflow ID or filename (required)",
                        "repositoryPath", "Path to repository (optional)",
                        "ref", "Branch/tag to run on (optional)"
                    ),
                    "description", "Trigger a GitHub Actions workflow"
                ),
                "workflow_runs", Map.of(
                    "method", "GET",
                    "endpoint", "/api/git/workflow-runs",
                    "parameters", Map.of(
                        "repositoryPath", "Path to repository (optional)",
                        "limit", "Number of runs to retrieve (default: 10)"
                    ),
                    "description", "Get recent workflow runs"
                )
            ),
            "github_repository", Map.of(
                "repo_info", Map.of(
                    "method", "GET",
                    "endpoint", "/api/git/repo-info",
                    "parameters", Map.of("repositoryPath", "Path to repository (optional)"),
                    "description", "Get GitHub repository information"
                )
            ),
            "dashboard", Map.of(
                "comprehensive_dashboard", Map.of(
                    "method", "GET",
                    "endpoint", "/api/git/dashboard",
                    "parameters", Map.of("repositoryPath", "Path to repository (optional)"),
                    "description", "Get comprehensive git and GitHub information dashboard"
                )
            ),
            "configuration", Map.of(
                "note", "Configure base directory with git.base.directory property",
                "example_config", "git.base.directory=/Users/jterhaar/repos",
                "timeout_config", "git.timeout.seconds=30"
            ),
            "queryType", "git_api_endpoints",
            "description", "Git service API documentation and available endpoints"
        );
        
        return ResponseEntity.ok(endpoints);
    }
} 