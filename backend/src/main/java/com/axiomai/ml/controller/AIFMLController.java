package com.axiomai.ml.controller;

import com.axiomai.ml.AIFModelRegistryService;
import com.axiomai.ml.AIFModelTrainingService;
import com.axiomai.ml.MLModelTrainingResult;
import com.axiomai.ml.entity.MLModelVersionEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ml")
@RequiredArgsConstructor
public class AIFMLController {

    private final AIFModelTrainingService modelTrainingService;

    private final AIFModelRegistryService modelRegistryService;

    @PostMapping("/models/retrain")
    public Map<String, MLModelTrainingResult> retrainAllModels() {

        return modelTrainingService.retrainAllModels();
    }

    @PostMapping("/models/{modelName}/retrain")
    public MLModelTrainingResult retrainModel(
            @PathVariable String modelName
    ) {

        return modelTrainingService.retrainModel(
                modelName
        );
    }

    @PostMapping("/models/{modelName}/rollback/{version}")
    public Map<String, Object> rollback(
            @PathVariable String modelName,
            @PathVariable String version
    ) {

        return modelRegistryService.rollback(
                        modelName,
                        version
                )
                .<Map<String, Object>>map(entity -> Map.of(
                        "rolledBack",
                        true,
                        "modelName",
                        entity.getModelName(),
                        "version",
                        entity.getVersion()
                ))
                .orElseGet(() -> Map.of(
                        "rolledBack",
                        false,
                        "modelName",
                        modelName,
                        "version",
                        version,
                        "message",
                        "Model version was not found."
                ));
    }

    @GetMapping("/models/{modelName}/versions")
    public List<MLModelVersionEntity> versions(
            @PathVariable String modelName
    ) {

        return modelRegistryService.versions(
                modelName
        );
    }
}
