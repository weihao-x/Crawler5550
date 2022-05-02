package edu.upenn.cis.cis455.crawler;

import static spark.Spark.*;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.codec.binary.Hex;
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
	static int n = 0;
	
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
	    	new Thread(() -> {
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
		    				StorageFactory.getDatabaseInstance(args[0]).backupUrl(url);
		    			}
		    		}
		    		Crawler.queue.sharedQueue.clear();
		    	}
		    	crawler.setStatus("IDLE");
	    	}).start();
	    	
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
        			+ "Url processed: " + String.valueOf(crawler.getCount()) + "<br/>\r\n"
        			+ "Document exported last time: " + String.valueOf(n) + "\r\n"
        			+ "<form action=\"/start\"><input type=\"submit\" value=\"Start\" /></form>\r\n"
        			+ "<form action=\"/stop\"><input type=\"submit\" value=\"Stop\" /></form>\r\n"
        			+ "<h1>Exporter</h1>\r\n"
        			+ "<form method=\"POST\" action=\"/export\">\r\n"
        			+ "Output Path: <input type=\"text\" name=\"outputPath\"/>\r\n"
        			+ "<input type=\"submit\" value=\"Export\"/>\r\n"
        			+ "</form>\r\n";
        	
        	r += "\r\n"
        			+ "</body>\r\n"
        			+ "</html>";
        	
        	return r;
        });
	    
	    post("/export", (req, res) -> {
	    	new Thread(() -> {
	    		crawler.setStatus("EXPORTING");
	    		n = StorageFactory.getDatabaseInstance(args[0]).getCorpusSize();
	    		if (!Files.exists(Paths.get(req.queryParams("outputPath")))) {
		            try {
		                Files.createDirectory(Paths.get(req.queryParams("outputPath")));
		            } catch (IOException e) {
		                e.printStackTrace();
		            }
		        }
				
				for (String url : StorageFactory.getDatabaseInstance(args[0]).allUrls()) {
					FileWriter myWriter;
					try {
						myWriter = new FileWriter(req.queryParams("outputPath") + "/" + 
								Hex.encodeHexString(MessageDigest.getInstance("SHA-256").digest(StorageFactory.getDatabaseInstance(args[0]).getDocument(url).getBytes(StandardCharsets.UTF_8))));
						myWriter.write(url + "\n");
						myWriter.write(StorageFactory.getDatabaseInstance(args[0]).getDocument(url));
					    myWriter.close();
					} catch (IOException | NoSuchAlgorithmException e) {
						e.printStackTrace();
					}		    
				}
				crawler.setStatus("IDLE");
			}).start();
	    	
	    	res.redirect("/");
        	return "";
	    });
	    
	    crawler = new Crawler(args[0]);
	    
	    System.out.println("Waiting to handle requests!");
        awaitInitialization();
	}
}
