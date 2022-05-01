package edu.upenn.cis.cis455.crawler;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.Level;

import edu.upenn.cis.cis455.storage.StorageFactory;

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
		String url = null;
		HttpURLConnection con = null;
		String host = null;
		int hash = 0;
		
		while (workers.size() > 0 && !status.equals("STOP")) {
			try {
				url = queue.remove(0);
				hash = 7;
				host = (new URL(url)).getHost();
				for (int i = 0; i < host.length() ; i++) {
				    hash = hash*31 + host.charAt(i);
				}
				con = (HttpURLConnection) (new URL("http://" + workers.get(hash % workers.size()) + "/add?url=" + url)).openConnection();
				con.setRequestMethod("POST");
				con.getResponseCode();
			} catch (InterruptedException | IOException e) {
				e.printStackTrace();
				continue;
			}
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
        			+ "Url sent: " + String.valueOf(count) + "<br/>\r\n"
        			+ "Url in queue: " + String.valueOf(queue.sharedQueue.size()) + "\r\n"
        			+ "<form action=\"/backup\"><input type=\"submit\" value=\"Backup urls\" /></form>\r\n"
        			+ "<form action=\"/reload\"><input type=\"submit\" value=\"Reload urls\" /></form>\r\n"
        			+ "<form action=\"/clear\"><input type=\"submit\" value=\"Clear queue\" /></form>\r\n"
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
        
        get("/backup", (req, res) -> {
        	synchronized(queue.sharedQueue) {
	    		for (String url : queue.sharedQueue) {
	    			StorageFactory.getDatabaseInstance(args[0]).backupUrl(url);
	    		}
	    	}
        	res.redirect("/");
        	return "";
        });
        
        get("/reload", (req, res) -> {
        	StorageFactory.getDatabaseInstance(args[0]).retrieveUrl(queue);
        	res.redirect("/");
        	return "";
        });
        
        get("/clear", (req, res) -> {
        	queue.sharedQueue.clear();
        	res.redirect("/");
        	return "";
        });
        
        master = new Master(args[0]);

        System.out.println("Waiting to handle requests!");
        awaitInitialization();
    }
}