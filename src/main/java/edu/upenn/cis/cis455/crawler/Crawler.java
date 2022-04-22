package edu.upenn.cis.cis455.crawler;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import javax.net.ssl.HttpsURLConnection;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.sleepycat.je.DatabaseException;

import edu.upenn.cis.cis455.crawler.utils.URLInfo;
import edu.upenn.cis.cis455.storage.StorageFactory;
import edu.upenn.cis.cis455.storage.StorageInterface;

import io.github.bonigarcia.wdm.WebDriverManager;
//import net.lightbody.bmp.BrowserMobProxy;
//import net.lightbody.bmp.BrowserMobProxyServer;
//import net.lightbody.bmp.client.ClientUtil;

import org.openqa.selenium.By;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeDriverLogLevel;
import org.openqa.selenium.chrome.ChromeOptions;

public class Crawler implements CrawlMaster {
	static final Logger logger = LogManager.getLogger(Crawler.class);
	
    static final int NUM_WORKERS = 4;
    
    protected List<Thread> crawlWorkers;
    private AtomicInteger exited;
    
    private URLQueue queue;
    
    AtomicInteger count;
    int size;
    
    List<String> disallow = null;
    int crawlDelay = 0;
    
    String host = null;
    
    AtomicInteger sHelper;
    
    String envPath = null;

    public Crawler(String startUrl, StorageInterface db, int size, int count, String envPath) {
    	exited = new AtomicInteger(Crawler.NUM_WORKERS);
    	crawlWorkers = new ArrayList<>();
    	
    	this.count = new AtomicInteger(count);
    	this.size = size;
    	sHelper = new AtomicInteger(0);
    	
    	this.envPath = envPath;
    	
    	disallow = new ArrayList<>();
    	
    	try {
    		// robots.txt
    		URLInfo url_info = new URLInfo(startUrl);
    		host = (url_info.isSecure() ? "https://" : "http://") + url_info.getHostName();
    		HttpURLConnection con = null;
    		if (url_info.isSecure())
    			con = (HttpsURLConnection) (new URL(host + "/robots.txt")).openConnection();
    		else
    			con = (HttpURLConnection) (new URL(host + "/robots.txt")).openConnection();
    		if (con.getResponseCode() == 200) {
	    		BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));
	    		String line = null;
	    		while ((line = br.readLine()) != null) {
	    			if (line.startsWith("User-agent") && line.contains("cis455crawler")) {
	    				disallow.clear();
	    				crawlDelay = 0;
	    				while ((line = br.readLine()) != null && !line.startsWith("User-agent")) {
	    					if (line.startsWith("Disallow")) {
	    						disallow.add(host + line.substring(line.indexOf(":")+1).trim());
	    					}
	    					else if (line.startsWith("Crawl-delay")) {
	    						crawlDelay = Integer.parseInt(line.split(":")[1].trim());
	    					}
	    				}
	    				break;
	    			}
	    			else if (line.startsWith("User-agent") && line.contains("User-agent: *")) {
	    				while ((line = br.readLine()) != null && !line.startsWith("User-agent")) {
	    					if (line.startsWith("Disallow")) {
	    						disallow.add(host + line.substring(line.indexOf(":")+1).trim());
	    					}
	    					else if (line.startsWith("Crawl-delay")) {
	    						crawlDelay = Integer.parseInt(line.split(":")[1].trim());
	    					}
	    				}
	    			}
	    		}
	    		
	    		for (String disallow_url : disallow) {
					if (startUrl.startsWith(disallow_url)) {
						return;
					}
				}
    		}
    		
    		host = host.substring(host.indexOf(".")+1);
    		System.out.println(host);

    		queue = new URLQueue(crawlDelay);
			queue.add(startUrl);
		} catch (InterruptedException | IOException e) {
			e.printStackTrace();
		}
    }

    /**
     * Main thread
     */
    public void start() {
    	crawlWorkers.clear();
    	
    	for (int i = 0; i < Crawler.NUM_WORKERS; i++) {
    		Thread crawlWorker = new Thread(()->{
    			String url = null;
    			HttpURLConnection con_head = null;
//    			HttpURLConnection con_get = null;
    			Document doc = null;
    			Elements links = null;
    			URLInfo url_info = null;
    			String content_type = null;
    			
    			WebDriverManager.chromedriver().setup();
		    	System.setProperty("webdriver.chrome.driver","./chromedriver");
		    	System.setProperty("webdriver.chrome.whitelistedIps", "");
		    	System.setProperty("webdriver.chrome.silentOutput", "true");
		    	
		    	ChromeOptions options = new ChromeOptions();
		    	options.setHeadless(true);
		    	ChromeDriver driver = new ChromeDriver(options);
		    	
		    	String sb = null;
		    	String md5 = null;
		    	    			
    			while (true) {
    				setWorking(true);
	    			try {
						url = queue.remove(0);
					} catch (InterruptedException e) {
						e.printStackTrace();
						System.out.println("Worker get interrupted, exit");
	    				notifyThreadExited();
						break;
					}
	    			
	    			if (url == null) {
	    				System.out.println("Worker wait too long for remove, exit");
	    				notifyThreadExited();
	    				break;
	    			}
	    			else {
	    				url_info = new URLInfo(url);
		    	    	try {
		    	    		if (url_info.isSecure()) {
		    	    			con_head = (HttpsURLConnection) (new URL(url)).openConnection();
		    	    		}
		    	    		else {
		    	    			con_head = (HttpURLConnection) (new URL(url)).openConnection();
		    	    		}
		    	    		con_head.setRequestMethod("HEAD");
		    	    		con_head.setRequestProperty("User-Agent", "cis455crawler");  	    			    				
		    				
		    				// If url has been visited
		    				if (StorageFactory.getDatabaseInstance(envPath).containsUrl(url)) {
		    					logger.debug(url + ": visited");
		    					con_head.setIfModifiedSince(StorageFactory.getDatabaseInstance(envPath).getLastModified(url));
		    					synchronized(sHelper) {
		    						con_head.connect();
			    					Thread.sleep(crawlDelay*1000);
			    				}
		    					// 304 Not modified
			    				if (con_head.getResponseCode() == 304) {
			    					logger.info(url + ": not modified");
			    					doc = Jsoup.parse(StorageFactory.getDatabaseInstance(envPath).getDocument(url),url);
			    					content_type = StorageFactory.getDatabaseInstance(envPath).getContentType(url);
			    				}
			    				// 200 OK
			    				else if (con_head.getResponseCode() == 200) {
			    					// Document too large
				    				if (con_head.getContentLength() > 1048576 * size)
				    					throw new Exception(url+": document too large, skip");
				    				content_type = con_head.getContentType();
			    				}
			    				// Others
			    				else {
			    					throw new Exception(url + ": " + con_head.getResponseCode());
			    				}
		    				}
		    				// If url has not been visited
		    				else {
		    					synchronized(sHelper) {
		    						con_head.connect();
			    					Thread.sleep(crawlDelay*1000);
			    				}
		    					// 200 OK
			    				if (con_head.getResponseCode() == 200) {
			    					// Document too large
				    				if (con_head.getContentLength() > 1048576 * size)
				    					throw new Exception(url+": document too large, skip");
				    				content_type = con_head.getContentType();
			    				}
			    				// Others
			    				else {
			    					throw new Exception(url + ": " + con_head.getResponseCode());
			    				}
		    				}
		    				
		    				// HTML document
		    				if (content_type.contains("text/html")) {	    					
			    				if (con_head.getResponseCode() != 304) {
			    					// Get content
//			    					if (url_info.isSecure()) {
//				    	    			con_get = (HttpsURLConnection) (new URL(url)).openConnection();
//				    	    		}
//				    	    		else {
//				    	    			con_get = (HttpURLConnection) (new URL(url)).openConnection();
//				    	    		}
//				    	    		con_get.setRequestMethod("GET");
//				    	    		con_get.setRequestProperty("User-Agent", "cis455crawler");
//				    	    		synchronized(sHelper) {
//				    	    			logger.info(url + ": downloading");
//				    	    			con_get.connect();
//				    	    			Thread.sleep(crawlDelay*1000);
//				    	    		}
//			    					BufferedReader br = new BufferedReader(new InputStreamReader(con_get.getInputStream()));
//			    					StringBuilder sb= new StringBuilder();
//			    					int c = 0;
//			    					while (br.ready() && (c = br.read()) != -1) {
//			    					    sb.append((char)c);
//			    					}
			    					
//			    					WebDriverManager.chromedriver().setup();
//			    			    	System.setProperty("webdriver.chrome.driver","./chromedriver");
//			    			    	System.setProperty("webdriver.chrome.whitelistedIps", "");
//			    			    	System.setProperty("webdriver.chrome.silentOutput", "true");
//			    			    	
//			    			    	ChromeOptions options = new ChromeOptions();
//			    			    	options.setHeadless(true);
//			    			    	ChromeDriver driver = new ChromeDriver(options);
			    			    	
			    			    	logger.info(url + ": downloading");
			    			    	driver.get(url);
			    			        driver.manage().timeouts().implicitlyWait(Duration.ofMillis(5000));
			    			        Thread.sleep(1000);
			    			        
			    			        sb = driver.findElement(By.tagName("html")).getAttribute("outerHTML");
			    			    	
//			    			    	driver.quit();
			    					
			    					// MD5 hash
			    					md5 = Base64.getEncoder().encodeToString(MessageDigest.getInstance("MD5").digest(sb.toString().getBytes()));
				    				if (StorageFactory.getDatabaseInstance(envPath).containsMd5(md5)) {
				    					throw new Exception(url + ": document already processed, skip");
				    				}
				    				StorageFactory.getDatabaseInstance(envPath).addDocument(url, 
				    						sb.toString(), 
				    						md5, 
				    						con_head.getLastModified(),
				    						content_type);
				    				
				    				incCount();
				    				driver.quit();
				    				driver = new ChromeDriver(options);
				    				
				    				doc = Jsoup.parse(sb.toString(), url);
			    				}
			    				
			    				links = doc.select("a[href]");
			    				for (Element link : links) {
			    					// External link
//			    					if (!link.attr("abs:href").contains(host)) {
//			    						logger.info(link.attr("abs:href") + ": external link, skip");
//			    					}
//			    					else {
			    						// Disallow page
			    						if (isAllow(link.attr("abs:href"))) {
			    							queue.add(link.attr("abs:href"));
			    						}
			    						else {
			    							logger.debug(link.attr("abs:href") + ": disallow page, skip");
			    						}
//			    					}
			    				}
		    				}
		    				// XML or RSS document
		    				else if (content_type.contains("text/xml") 
		    						|| content_type.contains("application/xml") 
		    						|| content_type.endsWith("+xml")) {
			    				if (con_head.getResponseCode() != 304) {
			    					// Get content
//			    					if (url_info.isSecure()) {
//				    	    			con_get = (HttpsURLConnection) (new URL(url)).openConnection();
//				    	    		}
//				    	    		else {
//				    	    			con_get = (HttpURLConnection) (new URL(url)).openConnection();
//				    	    		}
//				    	    		con_get.setRequestMethod("GET");
//				    	    		con_get.setRequestProperty("User-Agent", "cis455crawler");
//				    	    		synchronized(sHelper) {
//				    	    			logger.info(url + ": downloading");
//				    	    			con_get.connect();
//				    	    			Thread.sleep(crawlDelay*1000);
//				    	    		}
//			    					BufferedReader br = new BufferedReader(new InputStreamReader(con_get.getInputStream()));
//			    					StringBuilder sb= new StringBuilder();
//			    					int c = 0;
//			    					while (br.ready() && (c = br.read()) != -1) {
//			    					    sb.append((char)c);
//			    					}
			    					
//			    					WebDriverManager.chromedriver().setup();
//			    			    	System.setProperty("webdriver.chrome.driver","./chromedriver");
//			    			    	
//			    			    	ChromeOptions options = new ChromeOptions();
//			    			    	options.setHeadless(true);
//			    			    	ChromeDriver driver = new ChromeDriver(options);
			    			    	
			    			    	logger.info(url + ": downloading");
			    			    	driver.get(url);
			    			        driver.manage().timeouts().implicitlyWait(Duration.ofMillis(2000));
			    			        Thread.sleep(1000);
			    			        
			    			        sb = driver.findElement(By.tagName("html")).getAttribute("outerHTML");
			    			    	
//			    			    	driver.quit();
			    					
			    					// MD5 hash
			    					md5 = Base64.getEncoder().encodeToString(MessageDigest.getInstance("MD5").digest(sb.toString().getBytes()));
				    				if (StorageFactory.getDatabaseInstance(envPath).containsMd5(md5)) {
				    					throw new Exception(url + ": document already processed, skip");
				    				}
				    				StorageFactory.getDatabaseInstance(envPath).addDocument(url, 
				    						sb.toString(), 
				    						md5, 
				    						con_head.getLastModified(),
				    						content_type);
				    				
				    				incCount();
				    				driver.quit();
				    				driver = new ChromeDriver(options);
				    				
				    				// doc = Jsoup.parse(sb.toString(), url);
			    				}
		    				}
		    				// Others
		    				else
		    					throw new Exception(url + ": not HTML, XML or RSS, skip");

		    				// Worker finish
		    				//incCount();
		    		        if (count.get() <= 0) {
		    		        	System.out.println("Worker retrieve enough files, exit");
	    		        		notifyThreadExited();
	    	    				break;
		    		        }     
		    	    	} catch (IOException | InterruptedException | NoSuchAlgorithmException e) {
		    				e.printStackTrace();
		    				driver.quit();
		    				driver = new ChromeDriver(options);
						} catch (Exception e) {
							logger.warn(e.getMessage());
							driver.quit();
		    				driver = new ChromeDriver(options);
						}
	    			}
	    			setWorking(false);
    			}
    		});
    		crawlWorkers.add(crawlWorker);
    	}
    	
    	for (Thread crawlWorker : crawlWorkers) {
    		exited.decrementAndGet();
    		crawlWorker.start();
    	}
    }
    
    private boolean isAllow(String url) {
    	for (String disallow_url : disallow) {
			if (url.startsWith(disallow_url)) {
				return false;
			}
		}
    	return true;
    }

    /**
     * We've indexed another document
     */
    @Override
    public void incCount() {
    	count.decrementAndGet();
    }

    /**
     * Workers can poll this to see if they should exit, ie the crawl is done
     */
    @Override
    public boolean isDone() {
    	if (exited.get() >= Crawler.NUM_WORKERS)
    		return true;
    	else
    		return false;
    }

    /**
     * Workers should notify when they are processing an URL
     */
    @Override
    public void setWorking(boolean working) {
    	if (working)
    		logger.debug("Start work");
    	else
    		logger.debug("Finish work");
    }

    /**
     * Workers should call this when they exit, so the master knows when it can shut
     * down
     */
    @Override
    public void notifyThreadExited() {
    	exited.incrementAndGet();
    }

    /**
     * Main program: init database, start crawler, wait for it to notify that it is
     * done, then close.
     */
    public static void main(String args[]) {
    	org.apache.logging.log4j.core.config.Configurator.setLevel("edu.upenn.cis.cis455", Level.INFO);

//    	WebDriverManager.chromedriver().setup();
//    	System.setProperty("webdriver.chrome.driver","./chromedriver.exe");
//    	BrowserMobProxy proxy = new BrowserMobProxyServer();
//      proxy.start(0);
//      Proxy seleniumProxy = ClientUtil.createSeleniumProxy(proxy);
//      put our custom header to each request
//      proxy.addRequestFilter((request, contents, messageInfo)->{
//          request.headers().add("my-test-header", "my-test-value");
//          System.out.println(request.headers().entries().toString());
//          return null;
//      });
//    	ChromeOptions options = new ChromeOptions();
//    	options.setHeadless(true);
//    	String proxyOption = "--proxy-server=" + seleniumProxy.getHttpProxy();
//    	options.addArguments(proxyOption);
//    	ChromeDriver driver = new ChromeDriver(options);
//    	driver.get("https://crawltest.cis.upenn.edu/"); 
//      logger.info(driver.getTitle()); // => "Google"
//      driver.manage().timeouts().implicitlyWait(Duration.ofMillis(2000)); 
//      WebElement html = driver.findElement(By.tagName("html"));
//      logger.info(html.getAttribute("outerHTML"));
//    	driver.quit();
//    	System.exit(0);
    	
        if (args.length < 3 || args.length > 5) {
            System.out.println("Usage: Crawler {start URL} {database environment path} {max doc size in MB} {number of files to index}");
            System.exit(1);
        }

        System.out.println("Crawler starting");
        String startUrl = args[0];
        String envPath = args[1];
        Integer size = Integer.valueOf(args[2]);
        Integer count = args.length == 4 ? Integer.valueOf(args[3]) : 100;
        
        StorageInterface db = null;
		try {
			db = StorageFactory.getDatabaseInstance(envPath);
		} catch (DatabaseException | IllegalArgumentException | FileNotFoundException e) {
			e.printStackTrace();
		}

        Crawler crawler = new Crawler(startUrl, null, size, count, envPath);

        System.out.println("Starting crawl of " + count + " documents, starting at " + startUrl);
        crawler.start();

        while (!crawler.isDone())
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        db.close();
        System.out.println("Done crawling!");
        
        return;
    }

}
