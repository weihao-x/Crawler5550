package edu.upenn.cis.cis455.crawler;

import static spark.Spark.*;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.upenn.cis.cis455.storage.StorageFactory;

public class Worker {
	static final Logger logger = LogManager.getLogger(Worker.class);
	
	static String status = null;
	static Crawler crawler = null;
	static String master = null;
	
	public static void main(String args[]) {
		org.apache.logging.log4j.core.config.Configurator.setLevel("edu.upenn.cis.cis455", Level.ALL);
		
	    port(80);
	    
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
	    	
	    	for (int i = 0; i < Crawler.crawlWorkers.size(); i++) {
	    		crawler.addUrl(null);
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
	    			if (url != null) {
	    				StorageFactory.getDatabaseInstance("./data_save").backupUrl(url);
	    			}
	    		}
	    	}
	    	
	    	crawler.setStatus("IDLE");
	    	
	    	return crawler.getStatus();
	    });
	    
	    post("/add", (req, res) -> {
	    	crawler.addUrl(req.queryParams("url"));
        	return "";
	    });
	    
	    post("/register", (req, res) -> {
	    	master = req.queryParams("master");
	    	crawler.master = master;
	    	logger.info("Set master: " + crawler.master);
        	return "";
	    });
	    
	    crawler = new Crawler(args[0]);
	    
	    System.out.println("Waiting to handle requests!");
        awaitInitialization();
	}
}
