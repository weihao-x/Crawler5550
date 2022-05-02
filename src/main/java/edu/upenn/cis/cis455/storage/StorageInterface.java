package edu.upenn.cis.cis455.storage;

import java.util.List;

import edu.upenn.cis.cis455.crawler.URLQueue;

public interface StorageInterface {

    /**
     * How many documents so far?
     */
    public int getCorpusSize();
    
    public List<String> allUrls();

    /**
     * Add a new document
     */
    public void addDocument(String url, String documentContents, String md5, long lastModified, String contentType);

    /**
     * Retrieves a document's contents by URL
     */
    public String getDocument(String url);
    
    /**
     * Retrieves a document's Last-Modified by URL
     */
    public long getLastModified(String url);
    
    /**
     * Retrieves a document's Content-Type by URL
     */
    public String getContentType(String url);
        
    /**
     * Check if the url has been visited
     */
    public boolean containsUrl(String url);
    
    /**
     * Check if the md5 has been indexed
     */
    public boolean containsMd5(String md5);

    public void backupUrl(String url);
    
    public void retrieveUrl(URLQueue queue) throws InterruptedException;
    
    public void clearUrl();

    /**
     * Shuts down / flushes / closes the storage system
     */
    public void close();
}
