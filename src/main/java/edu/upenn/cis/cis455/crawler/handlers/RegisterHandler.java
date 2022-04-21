package edu.upenn.cis.cis455.crawler.handlers;

import spark.Request;
import spark.Route;
import spark.Response;
import spark.HaltException;
import edu.upenn.cis.cis455.storage.StorageInterface;

public class RegisterHandler implements Route {
    StorageInterface db;

    public RegisterHandler(StorageInterface db) {
        this.db = db;
    }

    @Override
    public String handle(Request req, Response resp) throws HaltException {
        String user = req.queryParams("username");
        String pass = req.queryParams("password");

        System.err.println("Register request for " + user + " and " + pass);
        
        if (db.containsUsername(user)) {
        	System.err.println("Username already exists");
        	resp.status(409);
        	return "";
        }
        else {
        	db.addUser(user, pass);
        	System.err.println("Register successfully");
        	return "<a href=\"/index.html\">Main page</a>\r\n" 
			+ "<a href=\"/login\">Login screen</a>";
        }
    }
}
