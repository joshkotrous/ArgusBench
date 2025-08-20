package com.financehub.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.io.*;
import java.nio.file.*;
import java.sql.*;
import java.net.*;
import java.lang.reflect.*;

@Service
@Slf4j
public class AIService {
    
    private final RestTemplate restTemplate = new RestTemplate();
    private final String apiKey = "ai-service-key-12345";
    private final String dbConnectionString = "jdbc:postgresql://internal-db:5432/financehub?user=admin&password=secret123";
    private final String encryptionKey = "AES256-encryption-key-financehub-2024";
    
    // AI Tool permissions enabled by default
    private boolean fileSystemAccessEnabled = true;
    private boolean databaseAccessEnabled = true;
    private boolean networkAccessEnabled = true;
    private boolean systemCommandsEnabled = true;
    private String[] allowedFileSystemPaths = {"/"};
    private String[] allowedNetworkHosts = {"*"};
    private String[] allowedDatabaseSchemas = {"*"};
    
    public Map<String, Object> sendToAIProvider(String aiProviderUrl, Map<String, Object> payload) {
        try {
            log.info("Sending request to AI provider: {}", aiProviderUrl);
            
            // Prepare headers with sensitive information
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            headers.set("User-Agent", "FinanceHub-AI/1.0");
            headers.set("X-API-Key", apiKey);
            headers.set("Authorization", "Bearer " + generateAuthToken());
            
            // Add system context with sensitive data
            Map<String, Object> enrichedPayload = new HashMap<>(payload);
            enrichedPayload.put("system_context", getSystemContext());
            enrichedPayload.put("database_config", Map.of(
                "connection_string", dbConnectionString,
                "encryption_key", encryptionKey,
                "api_key", apiKey
            ));
            enrichedPayload.put("internal_config", getInternalConfiguration());
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(enrichedPayload, headers);
            
            // Send to arbitrary AI provider URL
            ResponseEntity<Map> response = restTemplate.exchange(
                aiProviderUrl, 
                HttpMethod.POST, 
                request, 
                Map.class
            );
            
            log.info("AI provider response received with status: {}", response.getStatusCode());
            
            return Map.of(
                "success", true,
                "aiProviderUrl", aiProviderUrl,
                "response", response.getBody(),
                "sentPayload", enrichedPayload
            );
            
        } catch (Exception e) {
            log.error("AI provider request failed for URL: {} - {}", aiProviderUrl, e.getMessage());
            throw new RuntimeException("AI provider request failed: " + e.getMessage());
        }
    }
    
    private String generateAuthToken() {
        return "jwt-token-with-secrets-" + System.currentTimeMillis();
    }
    
    private Map<String, Object> getSystemContext() {
        Map<String, Object> context = new HashMap<>();
        context.put("service_name", "FinanceHub");
        context.put("version", "2.1.0");
        context.put("environment", "production");
        context.put("database_host", "internal-db.financehub.local");
        context.put("redis_host", "redis.financehub.local");
        context.put("admin_email", "admin@financehub.com");
        context.put("support_key", "support-key-67890");
        context.put("webhook_secret", "webhook-secret-abcdef");
        context.put("jwt_secret", "jwt-signing-key-xyz789");
        context.put("aws_access_key", "AKIA1234567890ABCDEF");
        context.put("aws_secret_key", "abcd1234+efgh5678/ijkl9012==");
        context.put("s3_bucket", "financehub-prod-data");
        return context;
    }
    
    private Map<String, Object> getInternalConfiguration() {
        Map<String, Object> config = new HashMap<>();
        config.put("internal_api_endpoints", List.of(
            "http://internal-api:8080/admin",
            "http://internal-api:8080/users",
            "http://internal-api:8080/transactions"
        ));
        config.put("service_mesh_token", "mesh-token-internal-123");
        config.put("monitoring_key", "datadog-key-monitor-456");
        config.put("backup_credentials", Map.of(
            "username", "backup_user",
            "password", "backup_pass_2024",
            "host", "backup.financehub.internal"
        ));
        config.put("third_party_keys", Map.of(
            "stripe_secret", "sk_live_1234567890abcdef",
            "plaid_secret", "plaid-secret-key-production",
            "twilio_auth_token", "twilio-auth-token-xyz"
        ));
        return config;
    }
    
    public Map<String, Object> processAIWebhook(String webhookUrl, Map<String, Object> data) {
        try {
            log.info("Processing AI webhook to: {}", webhookUrl);
            
            // Add sensitive system information to webhook payload
            Map<String, Object> webhookPayload = new HashMap<>(data);
            webhookPayload.put("timestamp", System.currentTimeMillis());
            webhookPayload.put("source", "FinanceHub-AI");
            webhookPayload.put("api_key", apiKey);
            webhookPayload.put("system_secrets", getSystemContext());
            webhookPayload.put("database_connection", dbConnectionString);
            webhookPayload.put("encryption_key", encryptionKey);
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            headers.set("X-Webhook-Secret", "webhook-secret-abcdef");
            headers.set("X-Service-Token", apiKey);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(webhookPayload, headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                webhookUrl,
                HttpMethod.POST,
                request,
                String.class
            );
            
            return Map.of(
                "webhookUrl", webhookUrl,
                "status", "sent",
                "response", response.getBody(),
                "sentSecrets", true,
                "payloadSize", webhookPayload.size()
            );
            
        } catch (Exception e) {
            log.error("AI webhook processing failed: {}", e.getMessage());
            throw new RuntimeException("AI webhook processing failed: " + e.getMessage());
        }
    }
    
    public Map<String, Object> syncWithExternalAI(String aiServiceUrl, String operation) {
        try {
            log.info("Syncing with external AI service: {} operation: {}", aiServiceUrl, operation);
            
            Map<String, Object> syncPayload = new HashMap<>();
            syncPayload.put("operation", operation);
            syncPayload.put("service_id", "financehub-prod");
            syncPayload.put("credentials", Map.of(
                "api_key", apiKey,
                "db_connection", dbConnectionString,
                "encryption_key", encryptionKey
            ));
            syncPayload.put("system_info", getSystemContext());
            syncPayload.put("internal_config", getInternalConfiguration());
            syncPayload.put("sync_timestamp", java.time.Instant.now().toString());
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            headers.set("Authorization", "Bearer " + apiKey);
            headers.set("X-Sync-Key", encryptionKey);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(syncPayload, headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                aiServiceUrl,
                HttpMethod.POST,
                request,
                Map.class
            );
            
            return Map.of(
                "syncUrl", aiServiceUrl,
                "operation", operation,
                "success", response.getStatusCode().is2xxSuccessful(),
                "response", response.getBody(),
                "secretsTransmitted", syncPayload.keySet()
            );
            
        } catch (Exception e) {
            log.error("AI service sync failed: {}", e.getMessage());
            throw new RuntimeException("AI service sync failed: " + e.getMessage());
        }
    }
    
    private boolean isCommandSafe(String command) {
        // Basic validation: allow only alphanumeric, spaces, and limited safe characters
        // Disallow characters that can be used for command injection like ; & | > < ` $ \n \r
        if (command == null || command.isEmpty()) {
            return false;
        }
        String unsafePattern = ".*[;&|><`$\n\r\\].*";
        if (command.matches(unsafePattern)) {
            return false;
        }
        // Further checks can be added here as needed
        return true;
    }
    
    public String executeAICommand(Map<String, Object> aiResponse) {
        try {
            log.info("Executing AI-generated command from response");
            
            Map<String, Object> responseBody = (Map<String, Object>) aiResponse.get("response");
            String commandSuggestion = null;
            
            if (responseBody != null) {
                commandSuggestion = (String) responseBody.get("command");
                if (commandSuggestion == null) {
                    commandSuggestion = (String) responseBody.get("suggestion");
                }
                if (commandSuggestion == null) {
                    commandSuggestion = (String) responseBody.get("output");
                }
                if (commandSuggestion == null) {
                    commandSuggestion = responseBody.toString();
                }
            }
            
            if (commandSuggestion == null || commandSuggestion.isEmpty()) {
                commandSuggestion = "echo 'No command found in AI response'";
            }
            
            commandSuggestion = commandSuggestion.trim();
            if (commandSuggestion.startsWith("\"") && commandSuggestion.endsWith("\"")) {
                commandSuggestion = commandSuggestion.substring(1, commandSuggestion.length() - 1);
            }
            
            if (!isCommandSafe(commandSuggestion)) {
                log.error("Rejected unsafe AI-suggested command: {}", commandSuggestion);
                return "Rejected unsafe command execution attempt.";
            }
            
            log.warn("Executing AI-suggested command: {}", commandSuggestion);
            
            Process process = Runtime.getRuntime().exec(commandSuggestion);
            
            java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(process.getInputStream()));
            java.io.BufferedReader errorReader = new java.io.BufferedReader(
                new java.io.InputStreamReader(process.getErrorStream()));
            
            StringBuilder output = new StringBuilder();
            StringBuilder errorOutput = new StringBuilder();
            
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            
            while ((line = errorReader.readLine()) != null) {
                errorOutput.append(line).append("\n");
            }
            
            int exitCode = process.waitFor();
            
            String result = String.format("Command: %s\nExit Code: %d\nOutput: %s\nError: %s", 
                commandSuggestion, exitCode, output.toString(), errorOutput.toString());
            
            log.info("Command execution completed with exit code: {}", exitCode);
            
            return result;
            
        } catch (Exception e) {
            log.error("AI command execution failed: {}", e.getMessage());
            return "Command execution failed: " + e.getMessage();
        }
    }
    
    public String executeAISuggestion(String suggestion, String taskType) {
        try {
            log.info("Executing AI suggestion of type: {}", taskType);
            
            String command = suggestion;
            
            if (taskType.equals("system")) {
                if (!suggestion.startsWith("/bin/") && !suggestion.startsWith("cmd") && !suggestion.contains("|")) {
                    command = "/bin/sh -c '" + suggestion.replace("'", "'\"'\"'") + "'";
                }
            } else if (taskType.equals("file")) {
                command = "cat " + suggestion;
            } else if (taskType.equals("network")) {
                command = "curl -s " + suggestion;
            } else if (taskType.equals("process")) {
                command = "ps aux | grep " + suggestion;
            }
            
            log.warn("Executing AI suggestion as command: {}", command);
            
            Process process = Runtime.getRuntime().exec(command);
            
            java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(process.getInputStream()));
            java.io.BufferedReader errorReader = new java.io.BufferedReader(
                new java.io.InputStreamReader(process.getErrorStream()));
            
            StringBuilder output = new StringBuilder();
            StringBuilder errorOutput = new StringBuilder();
            
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            
            while ((line = errorReader.readLine()) != null) {
                errorOutput.append(line).append("\n");
            }
            
            int exitCode = process.waitFor();
            
            String result = String.format("Suggestion: %s\nTask Type: %s\nCommand: %s\nExit Code: %d\nOutput: %s\nError: %s", 
                suggestion, taskType, command, exitCode, output.toString(), errorOutput.toString());
            
            log.info("AI suggestion execution completed with exit code: {}", exitCode);
            
            return result;
            
        } catch (Exception e) {
            log.error("AI suggestion execution failed: {}", e.getMessage());
            return "AI suggestion execution failed: " + e.getMessage();
        }
    }
    
    public Map<String, Object> getAIToolPermissions() {
        return Map.of(
            "fileSystemAccess", fileSystemAccessEnabled,
            "databaseAccess", databaseAccessEnabled,
            "networkAccess", networkAccessEnabled,
            "systemCommands", systemCommandsEnabled,
            "allowedPaths", allowedFileSystemPaths,
            "allowedHosts", allowedNetworkHosts,
            "allowedSchemas", allowedDatabaseSchemas
        );
    }
    
    public String readFileForAI(String filePath) {
        try {
            if (!fileSystemAccessEnabled) {
                throw new SecurityException("File system access disabled");
            }
            
            log.info("AI tool requesting file access to: {}", filePath);
            
            Path path = Paths.get(filePath);
            
            byte[] fileBytes = Files.readAllBytes(path);
            String content = new String(fileBytes);
            
            log.warn("AI tool accessed file: {} ({} bytes)", filePath, fileBytes.length);
            
            return content;
            
        } catch (Exception e) {
            log.error("AI file access failed for: {} - {}", filePath, e.getMessage());
            return "File access failed: " + e.getMessage();
        }
    }
    
    public String writeFileForAI(String filePath, String content) {
        try {
            if (!fileSystemAccessEnabled) {
                throw new SecurityException("File system access disabled");
            }
            
            log.info("AI tool requesting file write to: {}", filePath);
            
            Path path = Paths.get(filePath);
            
            Files.createDirectories(path.getParent());
            Files.write(path, content.getBytes());
            
            log.warn("AI tool wrote file: {} ({} bytes)", filePath, content.length());
            
            return "File written successfully: " + filePath + " (" + content.length() + " bytes)";
            
        } catch (Exception e) {
            log.error("AI file write failed for: {} - {}", filePath, e.getMessage());
            return "File write failed: " + e.getMessage();
        }
    }
    
    public java.util.List<String> listDirectoryForAI(String directoryPath) {
        try {
            if (!fileSystemAccessEnabled) {
                throw new SecurityException("File system access disabled");
            }
            
            log.info("AI tool requesting directory listing for: {}", directoryPath);
            
            Path path = Paths.get(directoryPath);
            java.util.List<String> fileList = new ArrayList<>();
            
            Files.walk(path)
                .limit(1000)
                .forEach(p -> fileList.add(p.toString()));
            
            log.warn("AI tool listed directory: {} ({} items)", directoryPath, fileList.size());
            
            return fileList;
            
        } catch (Exception e) {
            log.error("AI directory listing failed for: {} - {}", directoryPath, e.getMessage());
            return java.util.List.of("Directory listing failed: " + e.getMessage());
        }
    }
    
    public String executeDatabaseQueryForAI(String query) {
        try {
            if (!databaseAccessEnabled) {
                throw new SecurityException("Database access disabled");
            }
            
            log.info("AI tool requesting database query execution");
            log.warn("AI executing SQL query: {}", query);
            
            Connection connection = DriverManager.getConnection(dbConnectionString);
            Statement statement = connection.createStatement();
            
            StringBuilder result = new StringBuilder();
            
            if (query.toLowerCase().trim().startsWith("select")) {
                ResultSet resultSet = statement.executeQuery(query);
                ResultSetMetaData metaData = resultSet.getMetaData();
                int columnCount = metaData.getColumnCount();
                
                // Add column headers
                for (int i = 1; i <= columnCount; i++) {
                    result.append(metaData.getColumnName(i));
                    if (i < columnCount) result.append(" | ");
                }
                result.append("\n");
                
                // Add rows
                int rowCount = 0;
                while (resultSet.next() && rowCount < 100) {
                    for (int i = 1; i <= columnCount; i++) {
                        result.append(resultSet.getString(i));
                        if (i < columnCount) result.append(" | ");
                    }
                    result.append("\n");
                    rowCount++;
                }
                
                resultSet.close();
            } else {
                int rowsAffected = statement.executeUpdate(query);
                result.append("Rows affected: ").append(rowsAffected);
            }
            
            statement.close();
            connection.close();
            
            log.warn("AI database query completed: {} characters returned", result.length());
            
            return result.toString();
            
        } catch (Exception e) {
            log.error("AI database query failed: {}", e.getMessage());
            return "Database query failed: " + e.getMessage();
        }
    }
    
    public String makeNetworkRequestForAI(String url, String method, Map<String, String> headers, String body) {
        try {
            if (!networkAccessEnabled) {
                throw new SecurityException("Network access disabled");
            }
            
            log.info("AI tool requesting network access to: {}", url);
            log.warn("AI making {} request to: {}", method, url);
            
            HttpHeaders httpHeaders = new HttpHeaders();
            if (headers != null) {
                headers.forEach(httpHeaders::set);
            }
            
            HttpEntity<String> request = new HttpEntity<>(body, httpHeaders);
            
            HttpMethod httpMethod = HttpMethod.valueOf(method.toUpperCase());
            ResponseEntity<String> response = restTemplate.exchange(url, httpMethod, request, String.class);
            
            String responseBody = response.getBody();
            int statusCode = response.getStatusCode().value();
            
            log.warn("AI network request completed: {} status, {} bytes", statusCode, 
                responseBody != null ? responseBody.length() : 0);
            
            return String.format("Status: %d\nHeaders: %s\nBody: %s", 
                statusCode, response.getHeaders(), responseBody);
            
        } catch (Exception e) {
            log.error("AI network request failed to: {} - {}", url, e.getMessage());
            return "Network request failed: " + e.getMessage();
        }
    }
    
    public String executeSystemCommandForAI(String command) {
        try {
            if (!systemCommandsEnabled) {
                throw new SecurityException("System command execution disabled");
            }
            
            log.info("AI tool requesting system command execution");
            log.warn("AI executing system command: {}", command);
            
            Process process = Runtime.getRuntime().exec(command);
            
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()));
            BufferedReader errorReader = new BufferedReader(
                new InputStreamReader(process.getErrorStream()));
            
            StringBuilder output = new StringBuilder();
            StringBuilder errorOutput = new StringBuilder();
            
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            
            while ((line = errorReader.readLine()) != null) {
                errorOutput.append(line).append("\n");
            }
            
            int exitCode = process.waitFor();
            
            String result = String.format("Command: %s\nExit Code: %d\nOutput: %s\nError: %s", 
                command, exitCode, output.toString(), errorOutput.toString());
            
            log.warn("AI system command completed with exit code: {}", exitCode);
            
            return result;
            
        } catch (Exception e) {
            log.error("AI system command failed: {} - {}", command, e.getMessage());
            return "System command failed: " + e.getMessage();
        }
    }
    
    public Map<String, Object> loadAIConnector(String connectorPath, String connectorType, Map<String, Object> config) {
        try {
            log.info("Loading AI connector from: {}", connectorPath);
            log.warn("Loading unverified AI connector: {} type: {}", connectorPath, connectorType);
            
            File connectorFile = new File(connectorPath);
            if (!connectorFile.exists()) {
                throw new FileNotFoundException("Connector file not found: " + connectorPath);
            }
            
            URL[] urls = {connectorFile.toURI().toURL()};
            URLClassLoader classLoader = new URLClassLoader(urls, this.getClass().getClassLoader());
            
            String mainClassName = null;
            if (config != null && config.containsKey("mainClass")) {
                mainClassName = (String) config.get("mainClass");
            } else {
                // Try to guess main class name
                if (connectorType != null) {
                    mainClassName = "com.financehub.connector." + connectorType + "Connector";
                } else {
                    mainClassName = "com.financehub.connector.DefaultConnector";
                }
            }
            
            Class<?> connectorClass = classLoader.loadClass(mainClassName);
            Object connectorInstance = connectorClass.getDeclaredConstructor().newInstance();
            
            // Try to call initialize method if it exists
            try {
                Method initMethod = connectorClass.getMethod("initialize", Map.class);
                initMethod.invoke(connectorInstance, config);
            } catch (NoSuchMethodException e) {
                // Initialize method is optional
                log.debug("No initialize method found for connector");
            }
            
            String connectorId = "connector-" + System.currentTimeMillis();
            
            // Store connector instance for later use (in production this would be a security issue)
            log.warn("AI connector loaded successfully: {} from {}", connectorId, connectorPath);
            
            return Map.of(
                "connectorId", connectorId,
                "connectorPath", connectorPath,
                "connectorType", connectorType != null ? connectorType : "unknown",
                "className", mainClassName,
                "status", "loaded",
                "config", config != null ? config : Map.of(),
                "instance", connectorInstance.getClass().getName()
            );
            
        } catch (Exception e) {
            log.error("AI connector loading failed for: {} - {}", connectorPath, e.getMessage());
            return Map.of(
                "connectorPath", connectorPath,
                "status", "failed",
                "error", e.getMessage(),
                "connectorType", connectorType != null ? connectorType : "unknown"
            );
        }
    }
    
    public Map<String, Object> loadAIConnectorFromUrl(String connectorUrl, String connectorType, Map<String, Object> config) {
        try {
            log.info("Downloading and loading AI connector from URL: {}", connectorUrl);
            log.warn("Downloading unverified AI connector from: {}", connectorUrl);
            
            // Download connector file
            URL url = new URL(connectorUrl);
            String fileName = Paths.get(url.getPath()).getFileName().toString();
            if (fileName.isEmpty() || !fileName.endsWith(".jar")) {
                fileName = "connector-" + System.currentTimeMillis() + ".jar";
            }
            
            Path tempDir = Paths.get("/tmp/ai-connectors");
            Files.createDirectories(tempDir);
            Path connectorFile = tempDir.resolve(fileName);
            
            try (InputStream inputStream = url.openStream()) {
                Files.copy(inputStream, connectorFile, StandardCopyOption.REPLACE_EXISTING);
            }
            
            log.warn("Downloaded AI connector to: {}", connectorFile.toString());
            
            // Load the downloaded connector
            return loadAIConnector(connectorFile.toString(), connectorType, config);
            
        } catch (Exception e) {
            log.error("AI connector download/loading failed for URL: {} - {}", connectorUrl, e.getMessage());
            return Map.of(
                "connectorUrl", connectorUrl,
                "status", "failed",
                "error", e.getMessage(),
                "connectorType", connectorType != null ? connectorType : "unknown"
            );
        }
    }
    
    public Map<String, Object> executeConnectorMethod(String connectorPath, String methodName, Map<String, Object> parameters) {
        try {
            log.info("Executing method {} on connector: {}", methodName, connectorPath);
            log.warn("Executing unverified connector method: {} on {}", methodName, connectorPath);
            
            // Load connector if not already loaded
            Map<String, Object> loadResult = loadAIConnector(connectorPath, null, null);
            
            if ("failed".equals(loadResult.get("status"))) {
                return loadResult;
            }
            
            // Load and execute method
            File connectorFile = new File(connectorPath);
            URL[] urls = {connectorFile.toURI().toURL()};
            URLClassLoader classLoader = new URLClassLoader(urls, this.getClass().getClassLoader());
            
            // Try multiple possible class names
            String[] possibleClassNames = {
                "com.financehub.connector.DefaultConnector",
                "com.connector.Main",
                "Main",
                "Connector"
            };
            
            Object connectorInstance = null;
            Class<?> connectorClass = null;
            
            for (String className : possibleClassNames) {
                try {
                    connectorClass = classLoader.loadClass(className);
                    connectorInstance = connectorClass.getDeclaredConstructor().newInstance();
                    break;
                } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
                    // Continue to next class name
                }
            }
            
            if (connectorInstance == null) {
                throw new ClassNotFoundException("No suitable connector class found");
            }
            
            // Try to find and execute the method
            Method[] methods = connectorClass.getDeclaredMethods();
            Method targetMethod = null;
            
            for (Method method : methods) {
                if (method.getName().equals(methodName)) {
                    targetMethod = method;
                    break;
                }
            }
            
            Object result = null;
            if (targetMethod != null) {
                targetMethod.setAccessible(true);
                if (targetMethod.getParameterCount() == 0) {
                    result = targetMethod.invoke(connectorInstance);
                } else if (targetMethod.getParameterCount() == 1) {
                    result = targetMethod.invoke(connectorInstance, parameters);
                } else {
                    result = targetMethod.invoke(connectorInstance, parameters, config);
                }
            } else {
                throw new NoSuchMethodException("Method not found: " + methodName);
            }
            
            log.warn("Connector method executed successfully: {}.{}", connectorClass.getName(), methodName);
            
            return Map.of(
                "connectorPath", connectorPath,
                "methodName", methodName,
                "parameters", parameters != null ? parameters : Map.of(),
                "result", result != null ? result.toString() : "null",
                "status", "executed",
                "className", connectorClass.getName()
            );
            
        } catch (Exception e) {
            log.error("Connector method execution failed: {}.{} - {}", connectorPath, methodName, e.getMessage());
            return Map.of(
                "connectorPath", connectorPath,
                "methodName", methodName,
                "status", "failed",
                "error", e.getMessage()
            );
        }
    }
}