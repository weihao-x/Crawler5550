package edu.upenn.cis.cis455;

import static org.junit.Assert.*;

import java.io.FileNotFoundException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.sleepycat.je.DatabaseException;

import edu.upenn.cis.cis455.crawler.Crawler;
import edu.upenn.cis.cis455.storage.StorageFactory;
import edu.upenn.cis.cis455.storage.StorageInterface;

public class TestCrawler2 {

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void test() {
		StorageInterface db = null;
		try {
			db = StorageFactory.getDatabaseInstance("./testCrawler");
		} catch (DatabaseException | IllegalArgumentException | FileNotFoundException e) {e.printStackTrace();}
		
		Crawler crawler = new Crawler("https://crawltest.cis.upenn.edu/marie/", null, 1, 10, "./testCrawler");
		
		db.close();
	}

}
