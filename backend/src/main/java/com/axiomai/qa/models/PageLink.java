package com.axiomai.qa.models;

public class PageLink {

    private String text;

    private String href;

    private String sourcePage;

    public PageLink() {
    }

    public PageLink(
            String text,
            String href,
            String sourcePage
    ) {

        this.text = text;
        this.href = href;
        this.sourcePage = sourcePage;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getHref() {
        return href;
    }

    public void setHref(String href) {
        this.href = href;
    }

    public String getSourcePage() {
        return sourcePage;
    }

    public void setSourcePage(String sourcePage) {
        this.sourcePage = sourcePage;
    }
}