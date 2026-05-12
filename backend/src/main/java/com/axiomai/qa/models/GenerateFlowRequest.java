package com.axiomai.qa.models;

public class GenerateFlowRequest {

    private String url;

    public GenerateFlowRequest() {
    }

    public GenerateFlowRequest(String url) {

        this.url = url;
    }

    public String getUrl() {

        return url;
    }

    public void setUrl(String url) {

        this.url = url;
    }
}