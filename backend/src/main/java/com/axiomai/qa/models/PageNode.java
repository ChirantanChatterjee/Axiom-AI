package com.axiomai.qa.models;

import java.util.List;

public class PageNode {

    private String url;

    private String title;

    private List<PageElement> elements;

    private List<PageLink> links;

    public PageNode() {
    }

    public PageNode(
            String url,
            String title,
            List<PageElement> elements,
            List<PageLink> links
    ) {

        this.url = url;
        this.title = title;
        this.elements = elements;
        this.links = links;
    }

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

    public List<PageLink> getLinks() {
        return links;
    }

    public void setLinks(List<PageLink> links) {
        this.links = links;
    }
}