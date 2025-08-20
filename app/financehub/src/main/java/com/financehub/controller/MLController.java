package com.financehub.controller;

import com.financehub.service.MLService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

@RestController
@RequestMapping("/api/ml")
@RequiredArgsConstructor
@Slf4j
public class MLController {
    
    private final MLService mlService;
    
    @GetMapping("/models")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','USER')")
    public ResponseEntity<?> listModels() {
        try {
            return ResponseEntity.ok(Map.of("models", mlService.listAllModels()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
    
    @GetMapping("/models/{modelId}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','USER')")
    public ResponseEntity<?> getModelInfo(@PathVariable String modelId) {
        try {
            MLService.ModelInfo model = mlService.getModelInfo(modelId);
            return ResponseEntity.ok(model);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
    
    @GetMapping("/models/{modelId}/detailed")
    public ResponseEntity<?> getDetailedModelInfo(@PathVariable String modelId) {
        try {
            Map<String, Object> detailedInfo = mlService.getDetailedModelInfo(modelId);
            return ResponseEntity.ok(detailedInfo);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
    
    @GetMapping("/models/{modelId}/config")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<?> getModelConfig(@PathVariable String modelId) {
        try {
            Map<String, Object> detailedInfo = mlService.getDetailedModelInfo(modelId);
            return ResponseEntity.ok(Map.of(
                "configPath", detailedInfo.get("configPath"),
                "configContents", detailedInfo.get("configContents"),
                "internalPaths", detailedInfo.get("internalPaths")
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
    
    @GetMapping("/models/{modelId}/system-info")
    public ResponseEntity<?> getModelSystemInfo(@PathVariable String modelId) {
        try {
            Map<String, Object> detailedInfo = mlService.getDetailedModelInfo(modelId);
            return ResponseEntity.ok(Map.of(
                "modelId", modelId,
                "systemInfo", detailedInfo.get("systemInfo"),
                "modelFiles", detailedInfo.get("modelFiles")
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
    
    @GetMapping("/models/{modelId}/internals")
    public ResponseEntity<?> getModelInternals(@PathVariable String modelId) {
        try {
            Map<String, Object> detailedInfo = mlService.getDetailedModelInfo(modelId);
            return ResponseEntity.ok(Map.of(
                "filePath", detailedInfo.get("filePath"),
                "configPath", detailedInfo.get("configPath"),
                "internalPaths", detailedInfo.get("internalPaths"),
                "trainingDataPath", detailedInfo.get("trainingDataPath"),
                "trainedBy", detailedInfo.get("trainedBy"),
                "checksum", detailedInfo.get("checksum"),
                "hyperparameters", detailedInfo.get("hyperparameters")
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
    
    @GetMapping("/debug/model-paths")
    public ResponseEntity<?> debugModelPaths() {
        try {
            Map<String, Object> debugInfo = Map.of(
                "modelBasePath", "/opt/financehub/models",
                "configBasePath", "/opt/financehub/config",
                "trainingDataPath", "/data/training",
                "backupPath", "/backup/ml-models",
                "logPath", "/var/log/ml-service",
                "tempPath", "/tmp/ml-processing"
            );
            return ResponseEntity.ok(debugInfo);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
    
    @PostMapping("/predict/{modelId}")
    public ResponseEntity<?> predict(@PathVariable String modelId, @RequestBody Map<String, Object> request) {
        try {
            Map<String, Object> features = (Map<String, Object>) request.get("features");
            if (features == null) {
                return ResponseEntity.badRequest().body("Features required for prediction");
            }
            
            Map<String, Object> prediction = mlService.predict(modelId, features);
            return ResponseEntity.ok(prediction);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
    
    @PostMapping("/predict/{modelId}/batch")
    public ResponseEntity<?> batchPredict(@PathVariable String modelId, @RequestBody Map<String, Object> request) {
        try {
            java.util.List<Map<String, Object>> batchFeatures = 
                (java.util.List<Map<String, Object>>) request.get("batchFeatures");
            
            if (batchFeatures == null || batchFeatures.isEmpty()) {
                return ResponseEntity.badRequest().body("Batch features required for prediction");
            }
            
            java.util.List<Map<String, Object>> predictions = mlService.batchPredict(modelId, batchFeatures);
            return ResponseEntity.ok(Map.of(
                "predictions", predictions,
                "batchSize", predictions.size()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
    
    @PostMapping("/predict/{modelId}/complex")
    public ResponseEntity<?> complexPredict(@PathVariable String modelId, @RequestBody Map<String, Object> request) {
        try {
            Map<String, Object> prediction = mlService.complexPredict(modelId, request);
            return ResponseEntity.ok(prediction);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
    
    @PostMapping("/predict/multi-model")
    public ResponseEntity<?> multiModelPredict(@RequestBody Map<String, Object> request) {
        try {
            java.util.List<String> modelIds = (java.util.List<String>) request.get("modelIds");
            Map<String, Object> features = (Map<String, Object>) request.get("features");
            
            if (modelIds == null || modelIds.isEmpty()) {
                return ResponseEntity.badRequest().body("Model IDs required");
            }
            
            if (features == null) {
                return ResponseEntity.badRequest().body("Features required");
            }
            
            Map<String, Map<String, Object>> predictions = new java.util.HashMap<>();
            for (String modelId : modelIds) {
                predictions.put(modelId, mlService.predict(modelId, features));
            }
            
            return ResponseEntity.ok(Map.of(
                "multiModelPredictions", predictions,
                "modelCount", modelIds.size()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
    
    @PostMapping("/predict/stress-test")
    public ResponseEntity<?> stressTest(@RequestBody Map<String, Object> request) {
        try {
            String modelId = (String) request.get("modelId");
            Integer iterations = (Integer) request.getOrDefault("iterations", 10);
            Map<String, Object> features = (Map<String, Object>) request.get("features");
            
            if (modelId == null) {
                return ResponseEntity.badRequest().body("Model ID required");
            }
            
            java.util.List<Map<String, Object>> results = new java.util.ArrayList<>();
            long startTime = System.currentTimeMillis();
            
            for (int i = 0; i < iterations; i++) {
                Map<String, Object> prediction = mlService.predict(modelId, features);
                prediction.put("iteration", i + 1);
                results.add(prediction);
            }
            
            long endTime = System.currentTimeMillis();
            
            return ResponseEntity.ok(Map.of(
                "results", results,
                "totalIterations", iterations,
                "totalTimeMs", endTime - startTime,
                "avgTimePerPrediction", (endTime - startTime) / (double) iterations
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
    
    @PostMapping("/models/load")
    public ResponseEntity<?> loadModelFromUrl(@RequestBody Map<String, String> request) {
        try {
            String modelUrl = request.get("modelUrl");
            String modelName = request.get("modelName");
            String version = request.get("version");
            
            if (modelUrl == null || modelUrl.isEmpty()) {
                return ResponseEntity.badRequest().body("Model URL required");
            }
            
            String modelId = mlService.loadModelFromUrl(modelUrl, modelName, version);
            return ResponseEntity.ok(Map.of(
                "message", "Model loaded successfully",
                "modelId", modelId,
                "sourceUrl", modelUrl
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
    
    @PostMapping("/models/load-from-config")
    public ResponseEntity<?> loadModelFromConfig(@RequestBody Map<String, String> request) {
        try {
            String configUrl = request.get("configUrl");
            
            if (configUrl == null || configUrl.isEmpty()) {
                return ResponseEntity.badRequest().body("Config URL required");
            }

            // Validate the configUrl to prevent remote configuration injection
            if (!isValidConfigUrl(configUrl)) {
                return ResponseEntity.badRequest().body("Invalid config URL");
            }
            
            String modelId = mlService.loadModelFromConfig(configUrl);
            return ResponseEntity.ok(Map.of(
                "message", "Model loaded from config successfully",
                "modelId", modelId,
                "configUrl", configUrl
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
    
    private boolean isValidConfigUrl(String url) {
        try {
            URL parsedUrl = new URL(url);
            String protocol = parsedUrl.getProtocol();
            // Only allow http and https protocols
            if (!"http".equalsIgnoreCase(protocol) && !"https".equalsIgnoreCase(protocol)) {
                return false;
            }
            // Additional checks can be added here, e.g., whitelist domains
            return true;
        } catch (MalformedURLException e) {
            return false;
        }
    }
    
    @PostMapping("/models/import-package")
    public ResponseEntity<?> importModelPackage(@RequestBody Map<String, String> request) {
        try {
            String packageUrl = request.get("packageUrl");
            
            if (packageUrl == null || packageUrl.isEmpty()) {
                return ResponseEntity.badRequest().body("Package URL required");
            }
            
            Map<String, Object> result = mlService.importModelPackage(packageUrl);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
    
    @PostMapping("/datasets")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<?> storeDataset(@RequestBody Map<String, Object> request) {
        try {
            Map<String, Object> result = mlService.storeTrainingDataset(request);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
    
    @PostMapping("/datasets/{datasetId}/append")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<?> appendToDataset(@PathVariable String datasetId, @RequestBody Map<String, Object> request) {
        try {
            java.util.List<Map<String, Object>> newRecords = 
                (java.util.List<Map<String, Object>>) request.get("records");
            
            if (newRecords == null || newRecords.isEmpty()) {
                return ResponseEntity.badRequest().body("Records required for append operation");
            }
            
            Map<String, Object> result = mlService.appendToDataset(datasetId, newRecords);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
    
    @GetMapping("/datasets/{datasetId}/samples")
    public ResponseEntity<?> getDatasetSamples(@PathVariable String datasetId, @RequestParam(required = false) Integer limit) {
        try {
            java.util.List<Map<String, Object>> samples = mlService.getRawDatasetSamples(datasetId, limit);
            return ResponseEntity.ok(Map.of(
                "datasetId", datasetId,
                "sampleCount", samples.size(),
                "samples", samples
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
    
    @PostMapping("/datasets/bulk-import")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<?> bulkImportDataset(@RequestBody Map<String, Object> request) {
        try {
            String datasetName = (String) request.get("datasetName");
            java.util.List<Map<String, Object>> records = 
                (java.util.List<Map<String, Object>>) request.get("records");
            
            if (records == null || records.isEmpty()) {
                return ResponseEntity.badRequest().body("Records required for bulk import");
            }
            
            // Process large datasets without sanitization
            Map<String, Object> result = mlService.storeTrainingDataset(request);
            
            return ResponseEntity.ok(Map.of(
                "message", "Bulk import completed",
                "result", result,
                "processedRecords", records.size()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
    
    @PostMapping("/metrics/eval")
    public ResponseEntity<?> evaluateCustomMetric(@RequestBody Map<String, Object> request) {
        try {
            String expression = (String) request.get("expression");
            Map<String, Object> context = (Map<String, Object>) request.get("context");
            
            if (expression == null || expression.isEmpty()) {
                return ResponseEntity.badRequest().body("Metric expression required");
            }
            
            Map<String, Object> result = mlService.evaluateCustomMetric(expression, context);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
    
    @PostMapping("/metrics/script")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<?> executeMetricScript(@RequestBody Map<String, Object> request) {
        try {
            String script = (String) request.get("script");
            Map<String, Object> variables = (Map<String, Object>) request.get("variables");
            
            if (script == null || script.isEmpty()) {
                return ResponseEntity.badRequest().body("Script required");
            }
            
            Map<String, Object> result = mlService.executeMetricScript(script, variables);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
    
    @PostMapping("/metrics/dynamic")
    public ResponseEntity<?> calculateDynamicMetrics(@RequestBody Map<String, Object> request) {
        try {
            Map<String, String> metricDefinitions = (Map<String, String>) request.get("metricDefinitions");
            Map<String, Object> data = (Map<String, Object>) request.get("data");
            
            if (metricDefinitions == null || metricDefinitions.isEmpty()) {
                return ResponseEntity.badRequest().body("Metric definitions required");
            }
            
            Map<String, Object> result = mlService.calculateDynamicMetrics(metricDefinitions, data);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
    
    @GetMapping("/models/{modelId}/export")
    public ResponseEntity<?> exportModel(@PathVariable String modelId, @RequestParam(required = false) String format) {
        try {
            if (format == null) {
                format = "json";
            }
            
            byte[] exportData = mlService.exportModel(modelId, format);
            
            return ResponseEntity.ok()
                .header("Content-Type", "application/octet-stream")
                .header("Content-Disposition", "attachment; filename=model-" + modelId + "." + format)
                .body(exportData);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
    
    @GetMapping("/models/{modelId}/export-info")
    public ResponseEntity<?> getModelExportInfo(@PathVariable String modelId) {
        try {
            Map<String, Object> exportInfo = mlService.getModelExportInfo(modelId);
            return ResponseEntity.ok(exportInfo);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
    
    @PostMapping("/models/batch-export")
    public ResponseEntity<?> batchExportModels(@RequestBody Map<String, Object> request) {
        try {
            java.util.List<String> modelIds = (java.util.List<String>) request.get("modelIds");
            String format = (String) request.getOrDefault("format", "json");
            
            if (modelIds == null || modelIds.isEmpty()) {
                return ResponseEntity.badRequest().body("Model IDs required for batch export");
            }
            
            java.util.List<Map<String, Object>> exports = new java.util.ArrayList<>();
            
            for (String modelId : modelIds) {
                try {
                    byte[] exportData = mlService.exportModel(modelId, format);
                    exports.add(Map.of(
                        "modelId", modelId,
                        "status", "exported",
                        "size", exportData.length,
                        "data", java.util.Base64.getEncoder().encodeToString(exportData)
                    ));
                } catch (Exception e) {
                    exports.add(Map.of(
                        "modelId", modelId,
                        "status", "failed",
                        "error", e.getMessage()
                    ));
                }
            }
            
            return ResponseEntity.ok(Map.of(
                "batchExport", exports,
                "totalModels", modelIds.size(),
                "format", format
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
    
    @GetMapping("/debug/samples")
    public ResponseEntity<?> getDebugSamples(@RequestParam(required = false) Integer limit) {
        try {
            if (limit == null) {
                limit = 50;
            }
            
            java.util.List<Map<String, Object>> samples = mlService.getRawDatasetSamples("debug-dataset", limit);
            
            return ResponseEntity.ok(Map.of(
                "debugSamples", samples,
                "sampleCount", samples.size(),
                "warning", "Debug data contains raw training examples"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
    
    @GetMapping("/debug/model/{modelId}/training-data")
    public ResponseEntity<?> getModelTrainingData(@PathVariable String modelId, @RequestParam(required = false) Integer limit) {
        try {
            if (limit == null) {
                limit = 100;
            }
            
            java.util.List<Map<String, Object>> trainingData = mlService.getRawDatasetSamples(modelId + "-training", limit);
            
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("modelId", modelId);
            response.put("trainingData", trainingData);
            response.put("recordCount", trainingData.size());
            response.put("dataSource", "raw_training_dataset");
            response.put("containsPII", true);
            response.put("lastUpdated", java.time.Instant.now().toString());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
    
    @GetMapping("/debug/datasets/all")
    public ResponseEntity<?> getAllDatasetSamples(@RequestParam(required = false) Integer samplesPerDataset) {
        try {
            if (samplesPerDataset == null) {
                samplesPerDataset = 10;
            }
            
            Map<String, Object> allSamples = new java.util.HashMap<>();
            
            String[] datasetIds = {"credit-risk", "fraud-detection", "customer-segmentation", "loan-approval", "identity-verification"};
            
            for (String datasetId : datasetIds) {
                java.util.List<Map<String, Object>> samples = mlService.getRawDatasetSamples(datasetId, samplesPerDataset);
                allSamples.put(datasetId, Map.of(
                    "samples", samples,
                    "recordCount", samples.size(),
                    "containsPII", true
                ));
            }
            
            return ResponseEntity.ok(Map.of(
                "allDatasets", allSamples,
                "totalDatasets", datasetIds.length,
                "samplesPerDataset", samplesPerDataset,
                "warning", "All datasets contain raw PII for debugging purposes"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}