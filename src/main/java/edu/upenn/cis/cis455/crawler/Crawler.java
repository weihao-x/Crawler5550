package edu.upenn.cis.cis455.crawler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.net.ssl.HttpsURLConnection;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.sleepycat.je.DatabaseException;

import edu.upenn.cis.cis455.storage.StorageFactory;

import io.github.bonigarcia.wdm.WebDriverManager;

import org.openqa.selenium.By;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

public class Crawler {
	static final Logger logger = LogManager.getLogger(Crawler.class);
	
    static final int NUM_WORKERS = 8;
    String status = null;
    protected static List<Thread> crawlWorkers = null;
    AtomicInteger count = null;
    String envPath = null;
    Map<String, List<String>> robots = null;
    protected static URLQueue queue = null;
    protected String master = null;

    public Crawler(String envPath) {
    	status = "IDLE";
    	crawlWorkers = new ArrayList<>();
    	count = new AtomicInteger(0);
    	this.envPath = envPath;
    	robots = Collections.synchronizedMap(new HashMap<String, List<String>>());
    	queue = new URLQueue();
    	try {
			StorageFactory.getDatabaseInstance(envPath).retrieveUrl(queue);
		} catch (DatabaseException | IllegalArgumentException | InterruptedException e) {
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
    			HttpURLConnection con = null;
    			HttpURLConnection con_head = null;
    			HttpURLConnection con_robots = null;
    			Document doc = null;
    			Elements links = null;
    			String content_type = null;
    			
    			WebDriverManager.chromedriver().setup();
		    	System.setProperty("webdriver.chrome.driver","./chromedriver");
		    	System.setProperty("webdriver.chrome.whitelistedIps", "");
		    	System.setProperty("webdriver.chrome.silentOutput", "true");
		    	System.setProperty("webdriver.chrome.silentLogging", "true");
		    	
		    	ChromeOptions options = new ChromeOptions();
		    	options.addArguments("--no-sandbox");
		    	options.setHeadless(true);
		    	ChromeDriver driver = null;
		    	
		    	String sb = null;
		    	String md5 = null;
		    	
		    	URL aURL = null;
		    	String line = null;
		    	    			
    			while (true) {
    				if (status.equals("STOP")) {
    					break;
    				}
    				setWorking(true);
	    			try {
						url = queue.remove(0);
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}
	    			
	    			if (url == null) {
	    				setWorking(false);
	    				break;
	    			}
	    			
	    			// If url has been visited
	    			try {
						if (StorageFactory.getDatabaseInstance(envPath).containsUrl(url)) {
							setWorking(false);
							continue;
						}
					} catch (DatabaseException | IllegalArgumentException e) {
						e.printStackTrace();
						setWorking(false);
						continue;
					}
	    			
	    			try {
						aURL = new URL(url);
					} catch (MalformedURLException e) {
						e.printStackTrace();
						setWorking(false);
						continue;
					}
	    			
	    			// robots.txt
	    			if (!robots.containsKey(aURL.getHost())) {
		    			try {
			        		robots.put(aURL.getHost(), Collections.synchronizedList(new ArrayList<String>()));
			        		if (aURL.getProtocol().equals("https"))
			        			con_robots = (HttpsURLConnection) (new URL("https://" + aURL.getHost() + "/robots.txt")).openConnection();
			        		else
			        			con_robots = (HttpURLConnection) (new URL("http://" + aURL.getHost() + "/robots.txt")).openConnection();
			        		if (con_robots.getResponseCode() == 200) {
			    	    		BufferedReader br = new BufferedReader(new InputStreamReader(con_robots.getInputStream()));
			    	    		while ((line = br.readLine()) != null) {
			    	    			if (line.startsWith("User-agent") && line.contains("cis455crawler")) {
			    	    				robots.get(aURL.getHost()).clear();
			    	    				while ((line = br.readLine()) != null && !line.startsWith("User-agent")) {
			    	    					if (line.startsWith("Disallow") && !line.equals("Disallow:")) {
			    	    						robots.get(aURL.getHost()).add(aURL.getHost() + line.substring(line.indexOf(":")+1).trim());
			    	    					}
			    	    				}
			    	    				break;
			    	    			}
			    	    			else if (line.startsWith("User-agent") && line.contains("User-agent: *")) {
			    	    				while ((line = br.readLine()) != null && !line.startsWith("User-agent")) {
			    	    					if (line.startsWith("Disallow") && !line.equals("Disallow:")) {
			    	    						robots.get(aURL.getHost()).add(aURL.getHost() + line.substring(line.indexOf(":")+1).trim());
			    	    					}
			    	    				}
			    	    			}
			    	    		}
			        		}
		    			} catch (IOException e) {
		    				e.printStackTrace();
		    				continue;
		    			}
	    			}
	    			
	    	    	try {
	    	    		driver = new ChromeDriver(options);
	    	    		
	    	    		if (aURL.getProtocol().equals("https")) {
	    	    			con_head = (HttpsURLConnection) aURL.openConnection();
	    	    		}
	    	    		else {
	    	    			con_head = (HttpURLConnection) aURL.openConnection();
	    	    		}
	    	    		con_head.setRequestMethod("HEAD");
	    	    		con_head.setRequestProperty("User-Agent", "cis455crawler");
	    	    		con_head.connect();

    					// 200 OK
	    				if (con_head.getResponseCode() == 200) {
	    					// Document too large
		    				if (con_head.getContentLength() > 1048576)
		    					throw new Exception(url+": document too large, skip");
		    				content_type = con_head.getContentType();
	    				}
	    				// Others
	    				else {
	    					throw new Exception(url + ": " + con_head.getResponseCode());
	    				}
	    				
	    				// HTML document
	    				if (content_type.contains("text/html")) {	    					
	    					// Get content		    			    	
	    			    	logger.info(url + ": downloading");
	    			    	driver.get(url);
	    			        driver.manage().timeouts().implicitlyWait(Duration.ofMillis(5000));
	    			        sb = driver.findElement(By.tagName("html")).getAttribute("outerHTML");
	    			    			    					
	    					// MD5 hash
	    					md5 = Base64.getEncoder().encodeToString(MessageDigest.getInstance("MD5").digest(sb.toString().getBytes()));
		    				if (StorageFactory.getDatabaseInstance(envPath).containsMd5(md5)) {
		    					throw new Exception(url + ": document already processed, skip");
		    				}
		    				StorageFactory.getDatabaseInstance(envPath).addDocument(url,sb.toString(),md5,con_head.getLastModified(),content_type);
		    				
		    				incCount();
		    				logger.info("Finish " + count.get() + " documents");
		    				
		    				// Extract links
		    				doc = Jsoup.parse(sb.toString(), url);
		    				links = doc.select("a[href]");
		    				for (Element link : links) {
	    						// Disallow page
	    						if (isAllow(aURL.getHost(), link.attr("abs:href")) && !StorageFactory.getDatabaseInstance(envPath).containsUrl(link.attr("abs:href"))) {
	    							con = (HttpURLConnection) (new URL("http://" + master + "/workeradd?url=" + link.attr("abs:href"))).openConnection();
	    							con.setRequestMethod("POST");
	    							con.setRequestProperty("Content-Type", "application/json");
	    							con.getResponseCode();
	    						}
	    						else {
	    							logger.debug(link.attr("abs:href") + ": disallow page, skip");
	    						}
		    				}
	    				}
	    				// XML or RSS document
	    				else if (content_type.contains("text/xml") 
	    						|| content_type.contains("application/xml") 
	    						|| content_type.endsWith("+xml")) {
	    					// Get content
	    			    	logger.info(url + ": downloading");
	    			    	driver.get(url);
	    			        driver.manage().timeouts().implicitlyWait(Duration.ofMillis(2000));
	    			        sb = driver.findElement(By.tagName("html")).getAttribute("outerHTML");
	    			    		    					
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
		    			}
	    				// Others
	    				else {
	    					throw new Exception(url + ": not HTML, XML or RSS, skip");
	    				}
	    	    	} catch (IOException | InterruptedException | NoSuchAlgorithmException e) {
	    				e.printStackTrace();
					} catch (Exception e) {
						logger.warn(e.getMessage());
					} finally {
						driver.quit();
					}
	    			setWorking(false);
    			}
    		});
    		crawlWorkers.add(crawlWorker);
    	}
    	
    	for (Thread crawlWorker : crawlWorkers) {
    		crawlWorker.start();
    	}
    }
    
    private boolean isAllow(String host, String url) {
    	for (String disallow_url : robots.get(host)) {
			if (url.startsWith(disallow_url)) {
				return false;
			}
		}
    	return true;
    }
    
    public void setStatus(String status) {
    	this.status = status;
    }
    
    public String getStatus() {
    	return status;
    }
    
    public void addUrl(String url) {
    	try {
			queue.add(url);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
    }

    /**
     * We've indexed another document
     */
    public void incCount() {
    	count.incrementAndGet();
    }

    /**
     * Workers should notify when they are processing an URL
     */
    public void setWorking(boolean working) {
    	if (working)
    		logger.debug("Start work");
    	else
    		logger.debug("Finish work");
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
        
        URL aURL = null;
		try {
			aURL = new URL(startUrl);
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		System.out.println("protocol = " + aURL.getProtocol());
		System.out.println("authority = " + aURL.getAuthority());
		System.out.println("host = " + aURL.getHost());
		System.out.println("port = " + aURL.getPort());
		System.out.println("path = " + aURL.getPath());
		System.out.println("query = " + aURL.getQuery());
		System.out.println("filename = " + aURL.getFile());
		System.out.println("ref = " + aURL.getRef());
        
//        StorageInterface db = null;
//		try {
//			db = StorageFactory.getDatabaseInstance(envPath);
//		} catch (DatabaseException | IllegalArgumentException | FileNotFoundException e) {
//			e.printStackTrace();
//		}
//
//        Crawler crawler = new Crawler(startUrl, null, size, count, envPath);
//
//        System.out.println("Starting crawl of " + count + " documents, starting at " + startUrl);
//        crawler.start();
//
//        while (!crawler.isDone())
//            try {
//                Thread.sleep(10);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//
//        db.close();
        System.out.println("Done crawling!");
        
        return;
    }

}
