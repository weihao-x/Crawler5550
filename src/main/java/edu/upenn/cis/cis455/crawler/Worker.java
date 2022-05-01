package edu.upenn.cis.cis455.crawler;

import static spark.Spark.*;

import org.apache.logging.log4j.Level;

import edu.upenn.cis.cis455.storage.StorageFactory;

public class Worker {
	static String status = null;
	static Crawler crawler = null;
	
	public static void main(String args[]) {
		org.apache.logging.log4j.core.config.Configurator.setLevel("edu.upenn.cis.cis455", Level.INFO);
		
		staticFileLocation("static");
		staticFiles.externalLocation("static");
	    port(45555);
	    
	    get("/start", (req, res) -> {
	    	if (crawler.getStatus().equals("IDLE")) {
	    		crawler.setStatus("RUNNING");
	    		crawler.start();
	    	}
	    	return crawler.getStatus();
	    });
	    
	    get("/status", (req, res) -> {
	    	return crawler.getStatus();
	    });
	    
	    get("/stop", (req, res) -> {
	    	if (crawler.getStatus().equals("RUNNING")) {
	    		crawler.setStatus("STOP");
	    	}
	    	
	    	for (Thread crawlWorker : Crawler.crawlWorkers) {
	    		try {
					crawlWorker.join();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
	    	}
	    	
	    	crawler.setStatus("BACKUP");
	    	
	    	synchronized(Crawler.queue.sharedQueue) {
	    		for (String url : Crawler.queue.sharedQueue) {
	    			StorageFactory.getDatabaseInstance("./data_save").backupUrl(url);
	    		}
	    	}
	    	
	    	crawler.setStatus("IDLE");
	    	
	    	return crawler.getStatus();
	    });
	    
	    post("/add", (req, res) -> {
	    	crawler.addUrl(req.queryParams("url"));
	    	return "Add successfully!";
	    });
	    
	    crawler = new Crawler("./data_save");
	    
	    System.out.println("Waiting to handle requests!");
        awaitInitialization();
	}
}
