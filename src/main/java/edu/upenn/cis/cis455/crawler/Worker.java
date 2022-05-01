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
	static String worker = null;
	static int count = 0;
	
	public static void main(String args[]) {
		org.apache.logging.log4j.core.config.Configurator.setLevel("edu.upenn.cis.cis455", Level.INFO);
		
	    port(80);
	    
	    get("/start", (req, res) -> {
	    	if (crawler.getStatus().equals("IDLE")) {
	    		crawler.setStatus("RUNNING");
	    		crawler.start();
	    	}
	    	res.redirect("/");
        	return "";
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
	    	
	    	res.redirect("/");
        	return "";
	    });
	    
	    post("/add", (req, res) -> {
	    	crawler.addUrl(req.queryParams("url"));
	    	count += 1;
        	return "";
	    });
	    
	    post("/register", (req, res) -> {
	    	master = req.queryParams("master");
	    	worker = req.queryParams("worker");
	    	crawler.master = master;
	    	logger.info("Set master: " + crawler.master);
        	return "";
	    });
	    
	    get("/", (req, res) -> {
        	String r = "<!DOCTYPE html>\r\n"
        			+ "<html>\r\n"
        			+ "<head>\r\n"
        			+ "    <title>Crawler Worker Status</title>\r\n"
        			+ "</head>\r\n"
        			+ "<body>\r\n"
        			+ "<h1>Crawler Worker Status</h1>\r\n"
        			+ "<h2>" + worker + "</h2>\r\n"
        			+ "Worker is " + crawler.getStatus() + "<br/>\r\n"
        			+ "Url received: " + String.valueOf(count) + "<br/>\r\n"
        			+ "Url processed: " + String.valueOf(crawler.getCount()) + "\r\n"
        			+ "<form action=\"/start\"><input type=\"submit\" value=\"Start\" /></form>\r\n"
        			+ "<form action=\"/stop\"><input type=\"submit\" value=\"Stop\" /></form>\r\n";
        	
        	r += "\r\n"
        			+ "</body>\r\n"
        			+ "</html>";
        	
        	return r;
        });
	    
	    crawler = new Crawler(args[0]);
	    
	    System.out.println("Waiting to handle requests!");
        awaitInitialization();
	}
}
