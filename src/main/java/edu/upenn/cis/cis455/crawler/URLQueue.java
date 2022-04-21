package edu.upenn.cis.cis455.crawler;

import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Stub class for implementing the queue of HttpTasks
 */
public class URLQueue {
	static final Logger logger = LogManager.getLogger(URLQueue.class);	
	
	protected ArrayList<String> sharedQueue = new ArrayList<>();
	int sizeOfQueue = 50000;
	int crawlDelay = 0;
	
	public URLQueue(int crawlDelay) {
		this.crawlDelay = (10*crawlDelay+10)*1000;
	}
	
	public void add(String task) throws InterruptedException {
		logger.trace("Adding element to queue");//This would be logged in the log file created and to the console.
		//wait if the queue is full
		long start = System.currentTimeMillis();
		long end = start + crawlDelay;
		while (System.currentTimeMillis() < end && sharedQueue.size() < 10000) {
			synchronized (sharedQueue) {
				if (sharedQueue.size() == sizeOfQueue) {
					// Synchronizing on the sharedQueue to make sure no more than one
					// thread is accessing the queue same time.
					logger.trace("Queue is full!");
					sharedQueue.wait(crawlDelay);
					// We use wait as a way to avoid polling the queue to see if
					// there was any space for the producer to push.
				} else {
					//Adding element to queue and notifying all waiting consumers
					sharedQueue.add(task);
					logger.trace("Notifying after add " + task);//This would be logged in the log file created and to the console.
					sharedQueue.notifyAll();
					return;
				}
			}
		}
	}
	
	public String remove(int index) throws InterruptedException {
		long start = System.currentTimeMillis();
		long end = start + crawlDelay;
		while (System.currentTimeMillis() < end) {
			synchronized (sharedQueue) {
				if (sharedQueue.isEmpty()) {
					//If the queue is empty, we push the current thread to waiting state. Way to avoid polling.
					logger.trace("Queue is currently empty");
					sharedQueue.wait(crawlDelay);
				} else {
					String task = sharedQueue.remove(0);
					logger.trace("Notifying everyone we are removing an item");
					sharedQueue.notifyAll();
					logger.trace("Exiting queue with return");
					return task;
				}
			}
		}
		return null;
	}
}