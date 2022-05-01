package edu.upenn.cis.cis455.crawler;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.Level;

import edu.upenn.cis.cis455.storage.StorageFactory;
import edu.upenn.cis.cis455.storage.StorageInterface;

import static spark.Spark.*;

public class Master {
	static Master master = null;
	static URLQueue queue = null;
	static List<String> workers = null;
	String status = null;
	static int count = 0;
	
	public Master(String database_location) {
		queue = new URLQueue();
		synchronized(queue.sharedQueue) {
    		for (String url : queue.sharedQueue) {
    			StorageFactory.getDatabaseInstance("database_location").backupUrl(url);
    		}
    	}
		workers = new ArrayList<>();
		status = "IDLE";
	}
	
	public void start() {
		while (workers.size() > 0 && !status.equals("STOP")) {
			break;
		}
	}
	
    public static void main(String args[]) {
    	org.apache.logging.log4j.core.config.Configurator.setLevel("edu.upenn.cis.cis455", Level.INFO);
    	
		if (args.length == 0 || args.length > 1) {
		    System.out.println("Syntax: Master {database_location}");
		    System.exit(1);
		}
	    	
	    port(80);
        
        get("/", (req, res) -> {
        	String r = "<!DOCTYPE html>\r\n"
        			+ "<html>\r\n"
        			+ "<head>\r\n"
        			+ "    <title>Crawler Master Dashboard</title>\r\n"
        			+ "</head>\r\n"
        			+ "<body>\r\n"
        			+ "<h1>Crawler Master Dashboard</h1>\r\n"
        			+ "<h2>Status</h3>\r\n"
        			+ "Url sent: " + String.valueOf(count) + "\r\n"
        			+ "Url in queue: " + String.valueOf(queue.sharedQueue.size()) + "\r\n"
        			+ "<h2>Queue</h3>\r\n"
        			+ "<form method=\"POST\" action=\"/add\">\r\n"
        			+ "Url: <input type=\"text\" name=\"url\"/>\r\n"
        			+ "<input type=\"submit\" value=\"Add to queue\"/>\r\n"
        			+ "</form>\r\n"
        			+ "<h2>Workers</h3>\r\n"
        			+ "<form method=\"POST\" action=\"/register\">\r\n"
        			+ "Host: <input type=\"text\" name=\"host\"/>\r\n"
        			+ "<input type=\"submit\" value=\"Register\"/>\r\n"
        			+ "</form>"
        			+ "\r\n";
        	for (String worker : workers) {
        		r += worker + "\r\n"
        				+ "<a href=\"http://" + worker + "/start\">start<a>\r\n"
        				+ "<a href=\"http://" + worker + "/status\">status<a>\r\n"
        				+ "<a href=\"http://" + worker + "/stop\">stop<a>\r\n"
        				+ "<a href=\"http://localhost:45555/remove?host=" + worker +"\">remove<a>\r\n"
        				+ "<br/>";
        	}
        	
        	r += "\r\n"
        			+ "</body>\r\n"
        			+ "</html>";
        	
        	return r;
        });
        
        post("/add", (req, res) -> {
        	queue.add(0, req.queryParams("url"));
        	res.redirect("/");
        	return "";
	    });
        
        post("/register", (req, res) -> {
        	workers.add(req.queryParams("host"));
        	res.redirect("/");
        	return "";
        });
        
        get("/remove", (req, res) -> {
        	workers.remove(req.queryParams("host"));
        	res.redirect("/");
        	return "";
        });
        
        get("/stop", (req, res) -> {
        	synchronized(queue.sharedQueue) {
	    		for (String url : queue.sharedQueue) {
	    			StorageFactory.getDatabaseInstance("./data_save").backupUrl(url);
	    		}
	    	}
        	return "";
        });
        
        master = new Master(args[0]);

        System.out.println("Waiting to handle requests!");
        awaitInitialization();
    }
}
