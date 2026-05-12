package com.axiomai.qa.models;

import java.util.List;

public class SiteMapResult {

    private String rootUrl;

    private List<PageNode> pages;

    public SiteMapResult() {
    }

    public SiteMapResult(
            String rootUrl,
            List<PageNode> pages
    ) {

        this.rootUrl = rootUrl;
        this.pages = pages;
    }

    public String getRootUrl() {
        return rootUrl;
    }

    public void setRootUrl(String rootUrl) {
        this.rootUrl = rootUrl;
    }

    public List<PageNode> getPages() {
        return pages;
    }

    public void setPages(List<PageNode> pages) {
        this.pages = pages;
    }
}