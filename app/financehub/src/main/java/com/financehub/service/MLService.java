package com.financehub.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;
import java.net.URL;
import java.io.InputStream;
import java.net.URLConnection;
import org.springframework.web.client.RestTemplate;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

@Service
@Slf4j
public class MLService {
    
    private final Map<String, ModelInfo> loadedModels = new HashMap<>();
    private final String modelBasePath = "/opt/financehub/models";
    private final String configBasePath = "/opt/financehub/config";
    private final RestTemplate restTemplate = new RestTemplate();
    
    public static class ModelInfo {
        public String id;
        public String name;
        public String version;
        public String filePath;
        public String configPath;
        public String algorithm;
        public Map<String, Object> hyperparameters;
        public Map<String, String> internalPaths;
        public String trainedBy;
        public String trainingDataPath;
        public long modelSize;
        public String checksum;
        
        public ModelInfo(String id, String name) {
            this.id = id;
            this.name = name;
            this.hyperparameters = new HashMap<>();
            this.internalPaths = new HashMap<>();
        }
    }
    
    public ModelInfo getModelInfo(String modelId) {
        ModelInfo model = loadedModels.get(modelId);
        if (model == null) {
            model = createDemoModel(modelId);
            loadedModels.put(modelId, model);
        }
        return model;
    }
    
    public Map<String, Object> getDetailedModelInfo(String modelId) {
        ModelInfo model = getModelInfo(modelId);
        
        Map<String, Object> detailedInfo = new HashMap<>();
        detailedInfo.put("modelId", model.id);
        detailedInfo.put("name", model.name);
        detailedInfo.put("version", model.version);
        detailedInfo.put("algorithm", model.algorithm);
        detailedInfo.put("hyperparameters", model.hyperparameters);
        detailedInfo.put("filePath", model.filePath);
        detailedInfo.put("configPath", model.configPath);
        detailedInfo.put("internalPaths", model.internalPaths);
        detailedInfo.put("trainedBy", model.trainedBy);
        detailedInfo.put("trainingDataPath", model.trainingDataPath);
        detailedInfo.put("modelSize", model.modelSize);
        detailedInfo.put("checksum", model.checksum);
        
        try {
            detailedInfo.put("systemInfo", getSystemInfo());
            detailedInfo.put("modelFiles", listModelFiles(model.filePath));
            detailedInfo.put("configContents", readConfigFile(model.configPath));
        } catch (Exception e) {
            log.warn("Failed to gather extended model info: {}", e.getMessage());
        }
        
        return detailedInfo;
    }
    
    private ModelInfo createDemoModel(String modelId) {
        ModelInfo model = new ModelInfo(modelId, "CreditRiskModel_" + modelId);
        model.version = "2.1.3";
        model.filePath = modelBasePath + "/credit_risk_" + modelId + ".pkl";
        model.configPath = configBasePath + "/model_config_" + modelId + ".json";
        model.algorithm = "RandomForest";
        model.trainedBy = "data_scientist@financehub.com";
        model.trainingDataPath = "/data/training/credit_risk_2023.csv";
        model.modelSize = 45678901L;
        model.checksum = "sha256:a1b2c3d4e5f6789abcdef0123456789abcdef0123456789abcdef0123456789a";
        
        model.hyperparameters.put("n_estimators", 200);
        model.hyperparameters.put("max_depth", 10);
        model.hyperparameters.put("min_samples_split", 5);
        model.hyperparameters.put("random_state", 42);
        model.hyperparameters.put("learning_rate", 0.01);
        
        model.internalPaths.put("weights", "/internal/weights_" + modelId + ".bin");
        model.internalPaths.put("vocabulary", "/internal/vocab_" + modelId + ".txt");
        model.internalPaths.put("tokenizer", "/internal/tokenizer_" + modelId + ".pkl");
        model.internalPaths.put("preprocessor", "/internal/preprocessor_" + modelId + ".pkl");
        model.internalPaths.put("feature_selector", "/internal/features_" + modelId + ".json");
        
        return model;
    }
    
    private Map<String, Object> getSystemInfo() {
        Map<String, Object> systemInfo = new HashMap<>();
        systemInfo.put("javaVersion", System.getProperty("java.version"));
        systemInfo.put("osName", System.getProperty("os.name"));
        systemInfo.put("osVersion", System.getProperty("os.version"));
        systemInfo.put("userDir", System.getProperty("user.dir"));
        systemInfo.put("userName", System.getProperty("user.name"));
        systemInfo.put("javaHome", System.getProperty("java.home"));
        systemInfo.put("classPath", System.getProperty("java.class.path"));
        systemInfo.put("libraryPath", System.getProperty("java.library.path"));
        systemInfo.put("freeMemory", Runtime.getRuntime().freeMemory());
        systemInfo.put("totalMemory", Runtime.getRuntime().totalMemory());
        systemInfo.put("maxMemory", Runtime.getRuntime().maxMemory());
        systemInfo.put("availableProcessors", Runtime.getRuntime().availableProcessors());
        return systemInfo;
    }
    
    private List<String> listModelFiles(String modelPath) {
        List<String> files = new ArrayList<>();
        try {
            Path modelDir = Paths.get(modelPath).getParent();
            if (Files.exists(modelDir)) {
                Files.list(modelDir)
                    .filter(Files::isRegularFile)
                    .forEach(path -> files.add(path.toString()));
            }
        } catch (Exception e) {
            files.add("Error listing files: " + e.getMessage());
        }
        return files;
    }
    
    private String readConfigFile(String configPath) {
        try {
            Path path = Paths.get(configPath);
            if (Files.exists(path)) {
                return Files.readString(path);
            } else {
                return "{\n  \"model_type\": \"credit_risk\",\n  \"features\": [\"income\", \"debt_ratio\", \"credit_score\"],\n  \"target\": \"default_risk\",\n  \"api_key\": \"ml-api-key-12345\",\n  \"database_url\": \"jdbc:postgresql://internal-db:5432/ml_models\",\n  \"s3_bucket\": \"financehub-ml-models\",\n  \"encryption_key\": \"aes256-model-encryption-key\"\n}";
            }
        } catch (Exception e) {
            return "Error reading config: " + e.getMessage();
        }
    }
    
    public List<Map<String, Object>> listAllModels() {
        List<Map<String, Object>> modelList = new ArrayList<>();
        
        // Add some demo models
        for (int i = 1; i <= 5; i++) {
            ModelInfo model = createDemoModel("model-" + i);
            Map<String, Object> modelSummary = new HashMap<>();
            modelSummary.put("id", model.id);
            modelSummary.put("name", model.name);
            modelSummary.put("version", model.version);
            modelSummary.put("algorithm", model.algorithm);
            modelSummary.put("filePath", model.filePath);
            modelSummary.put("trainedBy", model.trainedBy);
            modelSummary.put("modelSize", model.modelSize);
            modelList.add(modelSummary);
        }
        
        return modelList;
    }
    
    public Map<String, Object> predict(String modelId, Map<String, Object> features) {
        log.info("Making prediction with model: {} for features: {}", modelId, features);
        
        // Simulate prediction processing
        try {
            Thread.sleep(100); // Simulate model inference time
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        ModelInfo model = getModelInfo(modelId);
        
        // Simulate prediction result
        double riskScore = Math.random() * 100;
        String riskCategory = riskScore > 70 ? "HIGH" : riskScore > 30 ? "MEDIUM" : "LOW";
        
        Map<String, Object> prediction = new HashMap<>();
        prediction.put("modelId", modelId);
        prediction.put("modelName", model.name);
        prediction.put("riskScore", riskScore);
        prediction.put("riskCategory", riskCategory);
        prediction.put("confidence", Math.random() * 0.4 + 0.6); // 60-100% confidence
        prediction.put("features", features);
        prediction.put("timestamp", java.time.Instant.now().toString());
        prediction.put("processingTimeMs", 100);
        
        return prediction;
    }
    
    public List<Map<String, Object>> batchPredict(String modelId, List<Map<String, Object>> batchFeatures) {
        log.info("Making batch prediction with model: {} for {} records", modelId, batchFeatures.size());
        
        List<Map<String, Object>> predictions = new ArrayList<>();
        
        for (Map<String, Object> features : batchFeatures) {
            predictions.add(predict(modelId, features));
        }
        
        return predictions;
    }
    
    public Map<String, Object> complexPredict(String modelId, Map<String, Object> request) {
        log.info("Making complex prediction with model: {}", modelId);
        
        Map<String, Object> features = (Map<String, Object>) request.get("features");
        Boolean includeExplanation = (Boolean) request.getOrDefault("includeExplanation", false);
        Boolean includeConfidenceIntervals = (Boolean) request.getOrDefault("includeConfidenceIntervals", false);
        Integer ensembleSize = (Integer) request.getOrDefault("ensembleSize", 1);
        
        // Simulate expensive processing
        try {
            Thread.sleep(200 * ensembleSize); // More ensemble models = more time
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        Map<String, Object> prediction = predict(modelId, features);
        
        if (includeExplanation) {
            prediction.put("explanation", generateExplanation(features));
        }
        
        if (includeConfidenceIntervals) {
            prediction.put("confidenceIntervals", generateConfidenceIntervals());
        }
        
        if (ensembleSize > 1) {
            prediction.put("ensemblePredictions", generateEnsemblePredictions(ensembleSize));
        }
        
        return prediction;
    }
    
    private Map<String, Object> generateExplanation(Map<String, Object> features) {
        Map<String, Object> explanation = new HashMap<>();
        explanation.put("topFeatures", List.of("credit_score", "income", "debt_ratio"));
        explanation.put("featureImportances", Map.of(
            "credit_score", 0.4,
            "income", 0.3,
            "debt_ratio", 0.2,
            "employment_years", 0.1
        ));
        return explanation;
    }
    
    private Map<String, Object> generateConfidenceIntervals() {
        return Map.of(
            "lower_bound", Math.random() * 20,
            "upper_bound", Math.random() * 20 + 80,
            "confidence_level", 0.95
        );
    }
    
    private List<Map<String, Object>> generateEnsemblePredictions(int ensembleSize) {
        List<Map<String, Object>> ensemble = new ArrayList<>();
        for (int i = 0; i < ensembleSize; i++) {
            ensemble.add(Map.of(
                "model_" + i, Map.of(
                    "prediction", Math.random() * 100,
                    "weight", 1.0 / ensembleSize
                )
            ));
        }
        return ensemble;
    }
    
    public String loadModelFromUrl(String modelUrl, String modelName, String version) {
        try {
            log.info("Loading model from URL: {}", modelUrl);
            
            // Download model file from arbitrary URL
            byte[] modelData = downloadModelFile(modelUrl);
            
            // Generate model ID
            String modelId = "loaded-" + UUID.randomUUID().toString().substring(0, 8);
            
            // Save model to local filesystem
            Path modelDir = Paths.get(modelBasePath, modelId);
            Files.createDirectories(modelDir);
            
            String fileName = extractFileName(modelUrl);
            Path modelPath = modelDir.resolve(fileName);
            Files.write(modelPath, modelData);
            
            // Create model info
            ModelInfo model = new ModelInfo(modelId, modelName != null ? modelName : "LoadedModel");
            model.version = version != null ? version : "1.0.0";
            model.filePath = modelPath.toString();
            model.configPath = modelDir.resolve("config.json").toString();
            model.algorithm = "Unknown";
            model.trainedBy = "external";
            model.trainingDataPath = "unknown";
            model.modelSize = modelData.length;
            model.checksum = "loaded-from-url";
            
            // Add to loaded models
            loadedModels.put(modelId, model);
            
            log.info("Model loaded successfully with ID: {}", modelId);
            return modelId;
            
        } catch (Exception e) {
            log.error("Failed to load model from URL: {} - {}", modelUrl, e.getMessage());
            throw new RuntimeException("Model loading failed: " + e.getMessage());
        }
    }
    
    private byte[] downloadModelFile(String modelUrl) throws Exception {
        log.info("Downloading model file from: {}", modelUrl);
        
        // Use RestTemplate to download from arbitrary URL
        org.springframework.http.ResponseEntity<byte[]> response = restTemplate.getForEntity(modelUrl, byte[].class);
        
        if (response.getBody() == null) {
            throw new RuntimeException("No model data received from URL");
        }
        
        return response.getBody();
    }
    
    private String extractFileName(String url) {
        try {
            String path = new URL(url).getPath();
            String fileName = path.substring(path.lastIndexOf('/') + 1);
            return fileName.isEmpty() ? "model.bin" : fileName;
        } catch (Exception e) {
            return "model.bin";
        }
    }
    
    public String loadModelFromConfig(String configUrl) {
        try {
            log.info("Loading model configuration from: {}", configUrl);
            
            // Download config from arbitrary URL
            String configData = restTemplate.getForObject(configUrl, String.class);
            
            // Parse config (simplified - just extract model URL)
            String modelUrl = extractModelUrlFromConfig(configData);
            String modelName = extractModelNameFromConfig(configData);
            String version = extractVersionFromConfig(configData);
            
            // Load model using extracted URL
            return loadModelFromUrl(modelUrl, modelName, version);
            
        } catch (Exception e) {
            log.error("Failed to load model from config URL: {} - {}", configUrl, e.getMessage());
            throw new RuntimeException("Config-based model loading failed: " + e.getMessage());
        }
    }
    
    private String extractModelUrlFromConfig(String configData) {
        // Simplified extraction - look for model_url in JSON-like data
        if (configData.contains("\"model_url\"")) {
            int start = configData.indexOf("\"model_url\"") + 13;
            int urlStart = configData.indexOf("\"", start) + 1;
            int urlEnd = configData.indexOf("\"", urlStart);
            if (urlEnd > urlStart) {
                return configData.substring(urlStart, urlEnd);
            }
        }
        // Default fallback
        return "http://example.com/default-model.bin";
    }
    
    private String extractModelNameFromConfig(String configData) {
        if (configData.contains("\"name\"")) {
            int start = configData.indexOf("\"name\"") + 7;
            int nameStart = configData.indexOf("\"", start) + 1;
            int nameEnd = configData.indexOf("\"", nameStart);
            if (nameEnd > nameStart) {
                return configData.substring(nameStart, nameEnd);
            }
        }
        return "ConfigLoadedModel";
    }
    
    private String extractVersionFromConfig(String configData) {
        if (configData.contains("\"version\"")) {
            int start = configData.indexOf("\"version\"") + 10;
            int versionStart = configData.indexOf("\"", start) + 1;
            int versionEnd = configData.indexOf("\"", versionStart);
            if (versionEnd > versionStart) {
                return configData.substring(versionStart, versionEnd);
            }
        }
        return "1.0.0";
    }
    
    public Map<String, Object> importModelPackage(String packageUrl) {
        try {
            log.info("Importing model package from: {}", packageUrl);
            
            // Download package
            byte[] packageData = restTemplate.getForEntity(packageUrl, byte[].class).getBody();
            
            if (packageData == null) {
                throw new RuntimeException("No package data received");
            }
            
            // Save package
            String packageId = "package-" + UUID.randomUUID().toString().substring(0, 8);
            Path packagePath = Paths.get(modelBasePath, "packages", packageId + ".zip");
            Files.createDirectories(packagePath.getParent());
            Files.write(packagePath, packageData);
            
            // Extract and process (simplified)
            String extractedModelId = "extracted-" + packageId;
            ModelInfo model = new ModelInfo(extractedModelId, "PackageModel");
            model.filePath = packagePath.toString();
            model.modelSize = packageData.length;
            loadedModels.put(extractedModelId, model);
            
            return Map.of(
                "packageId", packageId,
                "modelId", extractedModelId,
                "packageSize", packageData.length,
                "packagePath", packagePath.toString(),
                "status", "imported"
            );
            
        } catch (Exception e) {
            log.error("Failed to import model package from: {} - {}", packageUrl, e.getMessage());
            throw new RuntimeException("Package import failed: " + e.getMessage());
        }
    }
    
    public Map<String, Object> storeTrainingDataset(Map<String, Object> request) {
        try {
            String datasetName = (String) request.get("datasetName");
            List<Map<String, Object>> records = (List<Map<String, Object>>) request.get("records");
            
            if (datasetName == null || datasetName.isEmpty()) {
                datasetName = "dataset-" + UUID.randomUUID().toString().substring(0, 8);
            }
            
            if (records == null || records.isEmpty()) {
                throw new RuntimeException("No training records provided");
            }
            
            log.info("Storing training dataset '{}' with {} records", datasetName, records.size());
            
            // Store raw data without sanitization
            String datasetId = "dataset-" + UUID.randomUUID().toString().substring(0, 8);
            Path datasetPath = Paths.get(modelBasePath, "datasets", datasetId + ".json");
            Files.createDirectories(datasetPath.getParent());
            
            // Convert to JSON and store raw PII data
            StringBuilder jsonData = new StringBuilder();
            jsonData.append("{\n");
            jsonData.append("  \"datasetId\": \"").append(datasetId).append("\",\n");
            jsonData.append("  \"name\": \"").append(datasetName).append("\",\n");
            jsonData.append("  \"createdAt\": \"").append(java.time.Instant.now().toString()).append("\",\n");
            jsonData.append("  \"recordCount\": ").append(records.size()).append(",\n");
            jsonData.append("  \"records\": [\n");
            
            for (int i = 0; i < records.size(); i++) {
                Map<String, Object> record = records.get(i);
                jsonData.append("    {\n");
                
                for (Map.Entry<String, Object> entry : record.entrySet()) {
                    jsonData.append("      \"").append(entry.getKey()).append("\": ");
                    if (entry.getValue() instanceof String) {
                        jsonData.append("\"").append(entry.getValue()).append("\"");
                    } else {
                        jsonData.append(entry.getValue());
                    }
                    jsonData.append(",\n");
                }
                
                // Remove last comma and close record
                if (jsonData.length() > 2) {
                    jsonData.setLength(jsonData.length() - 2);
                }
                jsonData.append("\n    }");
                if (i < records.size() - 1) {
                    jsonData.append(",");
                }
                jsonData.append("\n");
            }
            
            jsonData.append("  ]\n");
            jsonData.append("}");
            
            Files.writeString(datasetPath, jsonData.toString());
            
            log.info("Dataset stored at: {}", datasetPath);
            
            return Map.of(
                "datasetId", datasetId,
                "name", datasetName,
                "recordCount", records.size(),
                "filePath", datasetPath.toString(),
                "status", "stored"
            );
            
        } catch (Exception e) {
            log.error("Failed to store training dataset: {}", e.getMessage());
            throw new RuntimeException("Dataset storage failed: " + e.getMessage());
        }
    }
    
    public Map<String, Object> appendToDataset(String datasetId, List<Map<String, Object>> newRecords) {
        try {
            log.info("Appending {} records to dataset: {}", newRecords.size(), datasetId);
            
            Path datasetPath = Paths.get(modelBasePath, "datasets", datasetId + ".json");
            if (!Files.exists(datasetPath)) {
                throw new RuntimeException("Dataset not found: " + datasetId);
            }
            
            // Read existing data
            String existingData = Files.readString(datasetPath);
            
            // Simple append without proper JSON parsing (vulnerable)
            StringBuilder appendData = new StringBuilder();
            for (Map<String, Object> record : newRecords) {
                appendData.append(",\n    {\n");
                for (Map.Entry<String, Object> entry : record.entrySet()) {
                    appendData.append("      \"").append(entry.getKey()).append("\": ");
                    if (entry.getValue() instanceof String) {
                        appendData.append("\"").append(entry.getValue()).append("\"");
                    } else {
                        appendData.append(entry.getValue());
                    }
                    appendData.append(",\n");
                }
                if (appendData.length() > 2) {
                    appendData.setLength(appendData.length() - 2);
                }
                appendData.append("\n    }");
            }
            
            // Insert before closing bracket
            String modifiedData = existingData.replace("  ]\n}", appendData.toString() + "\n  ]\n}");
            Files.writeString(datasetPath, modifiedData);
            
            return Map.of(
                "datasetId", datasetId,
                "appendedRecords", newRecords.size(),
                "status", "appended"
            );
            
        } catch (Exception e) {
            log.error("Failed to append to dataset: {}", e.getMessage());
            throw new RuntimeException("Dataset append failed: " + e.getMessage());
        }
    }
    
    public List<Map<String, Object>> getRawDatasetSamples(String datasetId, Integer limit) {
        try {
            Path datasetPath = Paths.get(modelBasePath, "datasets", datasetId + ".json");
            if (!Files.exists(datasetPath)) {
                throw new RuntimeException("Dataset not found: " + datasetId);
            }
            
            String datasetContent = Files.readString(datasetPath);
            
            List<Map<String, Object>> samples = new ArrayList<>();
            
            for (int i = 0; i < (limit != null ? limit : 10); i++) {
                Map<String, Object> sample = new HashMap<>();
                sample.put("customer_id", "CUST-" + (100000 + i));
                sample.put("ssn", "123-45-" + String.format("%04d", 6789 + i));
                sample.put("full_name", "John Doe " + i);
                sample.put("email", "john.doe" + i + "@email.com");
                sample.put("phone", "555-" + String.format("%03d", 100 + i) + "-" + String.format("%04d", 1000 + i));
                sample.put("address", (123 + i) + " Main St, City, State 12345");
                sample.put("credit_score", 650 + (i * 5));
                sample.put("annual_income", 50000 + (i * 1000));
                sample.put("account_number", "ACC-" + String.format("%010d", 1000000000L + i));
                sample.put("routing_number", "123456789");
                sample.put("date_of_birth", "1985-0" + ((i % 9) + 1) + "-" + String.format("%02d", (i % 28) + 1));
                sample.put("mother_maiden_name", "Smith" + i);
                sample.put("last_four_ssn", String.format("%04d", 6789 + i));
                samples.add(sample);
            }
            
            return samples;
            
        } catch (Exception e) {
            log.error("Failed to get dataset samples: {}", e.getMessage());
            throw new RuntimeException("Dataset sample retrieval failed: " + e.getMessage());
        }
    }
    
    public Map<String, Object> evaluateCustomMetric(String metricExpression, Map<String, Object> context) {
        try {
            log.info("Evaluating custom metric expression: {}", metricExpression);
            
            ScriptEngineManager manager = new ScriptEngineManager();
            ScriptEngine engine = manager.getEngineByName("javascript");
            
            if (engine == null) {
                // Try Nashorn
                engine = manager.getEngineByName("nashorn");
            }
            
            if (engine == null) {
                throw new RuntimeException("JavaScript engine not available");
            }
            
            // Set context variables
            if (context != null) {
                for (Map.Entry<String, Object> entry : context.entrySet()) {
                    engine.put(entry.getKey(), entry.getValue());
                }
            }
            
            // Add some default ML metrics context
            engine.put("predictions", List.of(0.8, 0.6, 0.9, 0.7, 0.5));
            engine.put("actuals", List.of(1, 0, 1, 1, 0));
            engine.put("Math", java.lang.Math.class);
            engine.put("System", java.lang.System.class);
            engine.put("Runtime", java.lang.Runtime.class);
            
            // Execute user-provided expression
            Object result = engine.eval(metricExpression);
            
            return Map.of(
                "expression", metricExpression,
                "result", result,
                "context", context != null ? context : Map.of(),
                "evaluatedAt", java.time.Instant.now().toString()
            );
            
        } catch (ScriptException e) {
            log.error("Script evaluation failed: {}", e.getMessage());
            return Map.of(
                "expression", metricExpression,
                "error", e.getMessage(),
                "evaluatedAt", java.time.Instant.now().toString()
            );
        } catch (Exception e) {
            log.error("Custom metric evaluation failed: {}", e.getMessage());
            throw new RuntimeException("Custom metric evaluation failed: " + e.getMessage());
        }
    }
    
    public Map<String, Object> executeMetricScript(String script, Map<String, Object> variables) {
        try {
            log.info("Executing metric script with {} variables", variables != null ? variables.size() : 0);
            
            ScriptEngineManager manager = new ScriptEngineManager();
            ScriptEngine engine = manager.getEngineByName("javascript");
            
            if (engine == null) {
                engine = manager.getEngineByName("nashorn");
            }
            
            if (engine == null) {
                throw new RuntimeException("JavaScript engine not available");
            }
            
            // Set all variables in script context
            if (variables != null) {
                for (Map.Entry<String, Object> entry : variables.entrySet()) {
                    engine.put(entry.getKey(), entry.getValue());
                }
            }
            
            // Provide access to system functions
            engine.put("log", log);
            engine.put("System", System.class);
            engine.put("Runtime", Runtime.getRuntime());
            engine.put("Files", Files.class);
            engine.put("Paths", Paths.class);
            
            // Execute script
            Object result = engine.eval(script);
            
            return Map.of(
                "scriptExecuted", true,
                "result", result,
                "variables", variables != null ? variables : Map.of()
            );
            
        } catch (Exception e) {
            log.error("Script execution failed: {}", e.getMessage());
            return Map.of(
                "scriptExecuted", false,
                "error", e.getMessage(),
                "variables", variables != null ? variables : Map.of()
            );
        }
    }
    
    public Map<String, Object> calculateDynamicMetrics(Map<String, String> metricDefinitions, Map<String, Object> data) {
        try {
            log.info("Calculating {} dynamic metrics", metricDefinitions.size());
            
            Map<String, Object> results = new HashMap<>();
            ScriptEngineManager manager = new ScriptEngineManager();
            
            for (Map.Entry<String, String> metric : metricDefinitions.entrySet()) {
                String metricName = metric.getKey();
                String metricFormula = metric.getValue();
                
                try {
                    // Validate metricFormula to allow only safe characters (digits, operators, letters, dots, spaces, parentheses)
                    if (!metricFormula.matches("[0-9a-zA-Z_\.\+\-\*/\(\)\s]+")) {
                        throw new RuntimeException("Invalid characters in metric formula");
                    }
                    
                    ScriptEngine engine = manager.getEngineByName("javascript");
                    if (engine == null) {
                        engine = manager.getEngineByName("nashorn");
                    }
                    
                    // Set data context
                    if (data != null) {
                        for (Map.Entry<String, Object> entry : data.entrySet()) {
                            engine.put(entry.getKey(), entry.getValue());
                        }
                    }
                    
                    // Execute metric formula
                    Object result = engine.eval(metricFormula);
                    results.put(metricName, result);
                    
                } catch (Exception e) {
                    results.put(metricName, "Error: " + e.getMessage());
                }
            }
            
            return Map.of(
                "metrics", results,
                "dataContext", data != null ? data : Map.of(),
                "calculatedAt", java.time.Instant.now().toString()
            );
            
        } catch (Exception e) {
            log.error("Dynamic metrics calculation failed: {}", e.getMessage());
            throw new RuntimeException("Dynamic metrics calculation failed: " + e.getMessage());
        }
    }
    
    public byte[] exportModel(String modelId, String exportFormat) {
        try {
            log.info("Exporting model: {} in format: {}", modelId, exportFormat);
            
            ModelInfo model = getModelInfo(modelId);
            
            if (exportFormat == null) {
                exportFormat = "binary";
            }
            
            switch (exportFormat.toLowerCase()) {
                case "binary":
                    return exportModelAsBinary(model);
                case "json":
                    return exportModelAsJson(model);
                case "config":
                    return exportModelConfig(model);
                case "full":
                    return exportFullModel(model);
                default:
                    throw new RuntimeException("Unsupported export format: " + exportFormat);
            }
            
        } catch (Exception e) {
            log.error("Model export failed: {}", e.getMessage());
            throw new RuntimeException("Model export failed: " + e.getMessage());
        }
    }
    
    private byte[] exportModelAsBinary(ModelInfo model) throws Exception {
        StringBuilder binaryData = new StringBuilder();
        binaryData.append("MODEL_BINARY_EXPORT\n");
        binaryData.append("Model ID: ").append(model.id).append("\n");
        binaryData.append("Model Name: ").append(model.name).append("\n");
        binaryData.append("Version: ").append(model.version).append("\n");
        binaryData.append("Algorithm: ").append(model.algorithm).append("\n");
        binaryData.append("File Path: ").append(model.filePath).append("\n");
        binaryData.append("Training Data: ").append(model.trainingDataPath).append("\n");
        binaryData.append("Trained By: ").append(model.trainedBy).append("\n");
        binaryData.append("Hyperparameters: ").append(model.hyperparameters).append("\n");
        binaryData.append("Internal Paths: ").append(model.internalPaths).append("\n");
        binaryData.append("Checksum: ").append(model.checksum).append("\n");
        
        return binaryData.toString().getBytes();
    }
    
    private byte[] exportModelAsJson(ModelInfo model) throws Exception {
        StringBuilder jsonData = new StringBuilder();
        jsonData.append("{\n");
        jsonData.append("  \"modelId\": \"").append(model.id).append("\",\n");
        jsonData.append("  \"name\": \"").append(model.name).append("\",\n");
        jsonData.append("  \"version\": \"").append(model.version).append("\",\n");
        jsonData.append("  \"algorithm\": \"").append(model.algorithm).append("\",\n");
        jsonData.append("  \"filePath\": \"").append(model.filePath).append("\",\n");
        jsonData.append("  \"configPath\": \"").append(model.configPath).append("\",\n");
        jsonData.append("  \"trainedBy\": \"").append(model.trainedBy).append("\",\n");
        jsonData.append("  \"trainingDataPath\": \"").append(model.trainingDataPath).append("\",\n");
        jsonData.append("  \"modelSize\": ").append(model.modelSize).append(",\n");
        jsonData.append("  \"checksum\": \"").append(model.checksum).append("\",\n");
        jsonData.append("  \"hyperparameters\": ").append(model.hyperparameters).append(",\n");
        jsonData.append("  \"internalPaths\": ").append(model.internalPaths).append(",\n");
        jsonData.append("  \"exportedAt\": \"").append(java.time.Instant.now().toString()).append("\"\n");
        jsonData.append("}");
        
        return jsonData.toString().getBytes();
    }
    
    private byte[] exportModelConfig(ModelInfo model) throws Exception {
        StringBuilder configData = new StringBuilder();
        configData.append("# Model Configuration Export\n");
        configData.append("model.id=").append(model.id).append("\n");
        configData.append("model.name=").append(model.name).append("\n");
        configData.append("model.version=").append(model.version).append("\n");
        configData.append("model.algorithm=").append(model.algorithm).append("\n");
        configData.append("model.filePath=").append(model.filePath).append("\n");
        configData.append("model.configPath=").append(model.configPath).append("\n");
        configData.append("model.trainedBy=").append(model.trainedBy).append("\n");
        configData.append("model.trainingDataPath=").append(model.trainingDataPath).append("\n");
        configData.append("model.checksum=").append(model.checksum).append("\n");
        
        for (Map.Entry<String, String> entry : model.internalPaths.entrySet()) {
            configData.append("internal.").append(entry.getKey()).append("=").append(entry.getValue()).append("\n");
        }
        
        return configData.toString().getBytes();
    }
    
    private byte[] exportFullModel(ModelInfo model) throws Exception {
        StringBuilder fullExport = new StringBuilder();
        fullExport.append("=== FULL MODEL EXPORT ===\n\n");
        
        fullExport.append("Model Information:\n");
        fullExport.append(new String(exportModelAsJson(model))).append("\n\n");
        
        fullExport.append("Configuration:\n");
        fullExport.append(new String(exportModelConfig(model))).append("\n\n");
        
        fullExport.append("System Information:\n");
        fullExport.append("Java Version: ").append(System.getProperty("java.version")).append("\n");
        fullExport.append("OS Name: ").append(System.getProperty("os.name")).append("\n");
        fullExport.append("User Dir: ").append(System.getProperty("user.dir")).append("\n");
        fullExport.append("User Name: ").append(System.getProperty("user.name")).append("\n");
        
        return fullExport.toString().getBytes();
    }
    
    public Map<String, Object> getModelExportInfo(String modelId) {
        try {
            ModelInfo model = getModelInfo(modelId);
            
            return Map.of(
                "modelId", modelId,
                "exportable", true,
                "availableFormats", List.of("binary", "json", "config", "full"),
                "modelSize", model.modelSize,
                "estimatedExportSize", model.modelSize + 1024,
                "internalPaths", model.internalPaths,
                "configPath", model.configPath,
                "trainingDataPath", model.trainingDataPath
            );
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to get export info: " + e.getMessage());
        }
    }
}