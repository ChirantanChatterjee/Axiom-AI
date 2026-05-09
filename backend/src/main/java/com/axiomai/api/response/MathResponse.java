package com.axiomai.api.response;

import java.util.List;
import java.util.Map;

public class MathResponse {

    private String type;

    private String result;

    private String latex;

    private List<String> steps;

    // =========================================
    // ADD THIS
    // =========================================

    private Map<String, Object> graph;

    public MathResponse() {
    }

    public MathResponse(
            String type,
            String result,
            String latex,
            List<String> steps,
            Map<String, Object> graph
    ) {
        this.type = type;
        this.result = result;
        this.latex = latex;
        this.steps = steps;
        this.graph = graph;
    }

    // =========================================
    // GETTERS / SETTERS
    // =========================================

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getLatex() {
        return latex;
    }

    public void setLatex(String latex) {
        this.latex = latex;
    }

    public List<String> getSteps() {
        return steps;
    }

    public void setSteps(List<String> steps) {
        this.steps = steps;
    }

    // =========================================
    // GRAPH
    // =========================================

    public Map<String, Object> getGraph() {
        return graph;
    }

    public void setGraph(Map<String, Object> graph) {
        this.graph = graph;
    }
}