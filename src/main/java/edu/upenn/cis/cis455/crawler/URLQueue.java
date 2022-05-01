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
	
	public void add(String task) throws InterruptedException {
		logger.trace("Adding element to queue");
		long start = System.currentTimeMillis();
		while (System.currentTimeMillis() < start+10000 && sharedQueue.size() < sizeOfQueue) {
			synchronized (sharedQueue) {
				if (sharedQueue.size() == sizeOfQueue) {
					logger.trace("Queue is full!");
					sharedQueue.wait(10000);
				} else {
					sharedQueue.add(task);
					logger.trace("Notifying after add " + task);
					sharedQueue.notifyAll();
					return;
				}
			}
		}
	}
	
	public String remove(int index) throws InterruptedException {
		long start = System.currentTimeMillis();
		while (System.currentTimeMillis() < start+30000) {
			synchronized (sharedQueue) {
				if (sharedQueue.isEmpty()) {
					logger.trace("Queue is currently empty");
					sharedQueue.wait(30000);
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