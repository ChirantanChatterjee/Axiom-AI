package com.axiomai.ml;

import com.axiomai.ml.entity.MLTrainingExampleEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MLTrainingDatasetExporter {

    private final ObjectMapper objectMapper =
            new ObjectMapper();

    public Path exportJsonl(
            List<MLTrainingExampleEntity> examples,
            Path path
    ) {

        try {

            Files.createDirectories(
                    path.getParent()
            );

            StringBuilder content =
                    new StringBuilder();

            for (
                    MLTrainingExampleEntity example
                    : examples
            ) {

                content.append(
                        objectMapper.writeValueAsString(
                                row(example)
                        )
                );
                content.append(System.lineSeparator());
            }

            Files.writeString(
                    path,
                    content.toString()
            );

            return path;

        } catch (IOException e) {

            throw new RuntimeException(
                    "Unable to export ML training dataset.",
                    e
            );
        }
    }

    private Map<String, Object> row(
            MLTrainingExampleEntity example
    ) {

        Map<String, Object> row =
                new LinkedHashMap<>();

        row.put("id", example.getId());
        row.put("modelName", example.getModelName());
        row.put("inputText", example.getInputText());
        row.put("predictedLabel", example.getPredictedLabel());
        row.put("finalAcceptedLabel", example.getFinalAcceptedLabel());
        row.put("confidence", example.getConfidence());
        row.put("sourceType", example.getSourceType());
        row.put("metadataJson", example.getMetadataJson());
        row.put(
                "createdAt",
                example.getCreatedAt() == null
                        ? null
                        : example.getCreatedAt()
                        .toString()
        );

        return row;
    }
}
