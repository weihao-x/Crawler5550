package edu.upenn.cis.cis455.storage;

import java.io.Serializable;

@SuppressWarnings("serial")
public class DocumentData implements Serializable {
	private String documentContents;
    private long lastModified;
    private String contentType;

    public DocumentData(String documentContents, long lastModified, String contentType) {
        this.documentContents = documentContents;
        this.lastModified = lastModified;
        this.contentType = contentType;
    }

    public final String getDocumentContents() {
        return documentContents;
    }

    public final long getLastModified() {
        return lastModified;
    }
    
    public final String getContentType() {
        return contentType;
    }
} 
