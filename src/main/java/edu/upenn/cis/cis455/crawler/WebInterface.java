package edu.upenn.cis.cis455.crawler;

import java.io.FileNotFoundException;

import com.sleepycat.je.DatabaseException;

import static spark.Spark.*;
import edu.upenn.cis.cis455.crawler.handlers.LoginFilter;
import edu.upenn.cis.cis455.storage.StorageFactory;
import edu.upenn.cis.cis455.storage.StorageInterface;
import edu.upenn.cis.cis455.crawler.handlers.LoginHandler;
import edu.upenn.cis.cis455.crawler.handlers.LookupHandler;
import edu.upenn.cis.cis455.crawler.handlers.RegisterHandler;

public class WebInterface {
    public static void main(String args[]) {
//        if (args.length < 1 || args.length > 2) {
//            System.out.println("Syntax: WebInterface {path} {root}");
//            System.exit(1);
//        }
//
//        StorageInterface database = null;
//		try {
//			database = StorageFactory.getDatabaseInstance(args[0]);
//		} catch (DatabaseException | IllegalArgumentException | FileNotFoundException e) {e.printStackTrace();}
//
//        LoginFilter testIfLoggedIn = new LoginFilter(database);
    	staticFileLocation("/.well-known");
        port(80);
        
        
//        if (args.length == 2) {
//            staticFiles.externalLocation(args[1]);
//            staticFileLocation(args[1]);
//        }
        
      get("/", (req, res) -> {
    	  return "Hello!";
      });
        

//        before("/*", "*/*", testIfLoggedIn);
//        
//        // TODO:  add /register, /logout, /index.html, /, /lookup
//        post("/login", new LoginHandler(database));
//        get("/login", (req, res) -> {
//        	res.redirect("/login-form.html");
//        	return "";
//        });
//        post("/register", new RegisterHandler(database));
//        get("/register", (req, res) -> {
//        	res.redirect("/register.html");
//        	return "";
//        });
//        get("/logout", (req, res) -> {
//        	req.session().invalidate();
//        	res.redirect("/login-form.html");
//        	return "";
//        });
//        
//        get("/index.html", (req, res) -> {
//        	if (req.session(false) == null) {
//                return "";
//            } else {
//                return "Welcome " + req.session().attribute("user");
//            }});
//        
//        get("/", (req, res) -> {
//        	res.redirect("/index.html");
//        	return "";
//        });
//        
//        get("/lookup", new LookupHandler(database));

        System.out.println("Waiting to handle requests!");
        awaitInitialization();
    }
}
