package edu.upenn.cis.cis455.crawler.handlers;

import spark.Request;
import spark.Route;
import spark.Response;
import spark.HaltException;
import edu.upenn.cis.cis455.storage.StorageInterface;

public class LookupHandler implements Route {
    StorageInterface db;

    public LookupHandler(StorageInterface db) {
        this.db = db;
    }

    @Override
    public String handle(Request req, Response resp) throws HaltException {
    	if (db.containsUrl(req.queryParams("url"))) {
    		return db.getDocument(req.queryParams("url"));
    	}
    	else {
    		resp.status(404);
    		return "";
    	}
    }
}