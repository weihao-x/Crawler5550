package edu.upenn.cis.cis455.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import edu.upenn.cis.cis455.crawler.URLQueue;

public class StorageFactory {
	
	private static Map<String, SampleDatabase> db = null;
    private static Map<String, SampleViews> views = null;
    private static Map<String, StorageInterface> storageInterface = null;
    
    public static StorageInterface getDatabaseInstance(String directory) {    	
    	if (!Files.exists(Paths.get(directory))) {
            try {
                Files.createDirectory(Paths.get(directory));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    	
    	if (db == null) {
    		db = new HashMap<>();
    	}
    	
    	if (views == null) {
    		views = new HashMap<>();
    	}
    	
    	if (storageInterface == null) {
    		storageInterface = new HashMap<>();
    	}
    	
    	if (db.get(directory) == null) {
    		db.put(directory, new SampleDatabase(directory));
    	}
    	
    	if (views.get(directory) == null) {
    		views.put(directory, new SampleViews(db.get(directory)));
    	}
    	
        if (storageInterface.get(directory) == null) {
	        storageInterface.put(directory, new StorageInterface() {
	        	@Override
	        	public int getCorpusSize() {
					return views.get(directory).getDocumentMap().size();
	        	}
	        	
	        	@SuppressWarnings("rawtypes")
				@Override
				public List<String> allUrls() {
					List<String> urls = new ArrayList<>();
					Iterator iterator = views.get(directory).getUrlEntrySet().iterator();
					while (iterator.hasNext())
			        {
			            Map.Entry entry = (Map.Entry) iterator.next();
			            urls.add(((DocumentKey) entry.getKey()).getUrl());
			        }
					return urls;
				}
	
				@SuppressWarnings("unchecked")
				@Override
				public void addDocument(String url, String documentContents, String md5, long lastModified, String contentType) {
					views.get(directory).getDocumentMap().put(new DocumentKey(url), new DocumentData(documentContents, lastModified, contentType));
					views.get(directory).getUrlMap().put(new DocumentKey(url), new TrivialData());
					views.get(directory).getMd5Map().put(new Md5Key(md5), new TrivialData());
				}
	
				@Override
				public String getDocument(String url) {
					if (views.get(directory).getDocumentMap().containsKey(new DocumentKey(url)))
						return ((DocumentData)views.get(directory).getDocumentMap().get(new DocumentKey(url))).getDocumentContents();
					else
						return null;
				}
			    
				@Override
			    public long getLastModified(String url) {
					if (views.get(directory).getDocumentMap().containsKey(new DocumentKey(url)))
						return ((DocumentData)views.get(directory).getDocumentMap().get(new DocumentKey(url))).getLastModified();
					else
						return 0;
			    }
				
				@Override
				public String getContentType(String url) {
					if (views.get(directory).getDocumentMap().containsKey(new DocumentKey(url)))
						return ((DocumentData)views.get(directory).getDocumentMap().get(new DocumentKey(url))).getContentType();
					else
						return null;
				}
				
				@Override
				public boolean containsUrl(String url) {
					return views.get(directory).getUrlMap().containsKey(new DocumentKey(url));
				}
				
				@Override
				public boolean containsMd5(String md5) {
					return views.get(directory).getMd5Map().containsKey(new Md5Key(md5));
				}
	
				@Override
				public void close() {
					db.get(directory).close();				
				}

				@SuppressWarnings("unchecked")
				@Override
				public void backupUrl(String url) {
					views.get(directory).getQueueMap().put(new DocumentKey(url), new TrivialData());
				}

				@SuppressWarnings("rawtypes")
				@Override
				public void retrieveUrl(URLQueue queue) throws InterruptedException {
					Iterator iterator = views.get(directory).getQueueEntrySet().iterator();
					while (iterator.hasNext())
			        {
			            Map.Entry entry = (Map.Entry) iterator.next();
			            queue.add(((DocumentKey) entry.getKey()).getUrl());
			        }
					views.get(directory).getQueueEntrySet().clear();
				}
	        });
        }
    	
        return storageInterface.get(directory);
    }
}
