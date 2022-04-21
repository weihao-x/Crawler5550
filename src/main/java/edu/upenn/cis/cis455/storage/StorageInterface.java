package edu.upenn.cis.cis455.storage;

import java.util.List;

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

    /**
     * Adds a user
     */
    public void addUser(String username, String password);

    /**
     * Tries to log in the user, or else throws a HaltException
     */
    public boolean getSessionForUser(String username, String password);
    
    /**
     * Check if the username has been registered
     */
    public boolean containsUsername(String username);

    /**
     * Shuts down / flushes / closes the storage system
     */
    public void close();
}
