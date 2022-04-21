package edu.upenn.cis.cis455.crawler.handlers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.upenn.cis.cis455.storage.StorageInterface;
import spark.Request;
import spark.Filter;
import spark.Response;


public class LoginFilter implements Filter {
    Logger logger = LogManager.getLogger(LoginFilter.class);
    
    public LoginFilter(StorageInterface db) {}

    @Override
    public void handle(Request req, Response response) throws Exception {
        // Some basic logic to get you started
        if (!req.pathInfo().equals("/login-form.html") &&
                !req.pathInfo().equals("/login") &&
                !req.pathInfo().equals("/register") &&
                !req.pathInfo().equals("/register.html")) {
            logger.info("Request is NOT login/registration");
            if (req.session(false) == null) {
                logger.info("Not logged in - redirecting!");
                response.redirect("/login-form.html");
            } else {
                logger.info("Logged in!");
                req.attribute("user", req.session().attribute("user"));
            }
        } else {
            logger.info("Request is LOGIN FORM");
        }
    }
}
