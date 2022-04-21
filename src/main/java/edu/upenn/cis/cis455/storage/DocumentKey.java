package edu.upenn.cis.cis455.storage;

import java.io.Serializable;

@SuppressWarnings("serial")
public class DocumentKey implements Serializable
{
    private String url;

    public DocumentKey(String url) {
        this.url = url;
    }

    public final String getUrl() {
        return url;
    }
} 