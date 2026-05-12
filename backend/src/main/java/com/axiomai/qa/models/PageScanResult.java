package com.axiomai.qa.models;

import java.util.List;

public class PageScanResult {

    private String url;

    private String title;

    private List<PageElement> elements;

    // =====================================================
    // CONSTRUCTORS
    // =====================================================

    public PageScanResult() {
    }

    public PageScanResult(
            String url,
            String title,
            List<PageElement> elements
    ) {

        this.url = url;
        this.title = title;
        this.elements = elements;
    }

    // =====================================================
    // GETTERS / SETTERS
    // =====================================================

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<PageElement> getElements() {
        return elements;
    }

    public void setElements(List<PageElement> elements) {
        this.elements = elements;
    }
}