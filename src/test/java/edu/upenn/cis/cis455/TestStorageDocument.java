package edu.upenn.cis.cis455;

import static org.junit.Assert.*;

import java.io.FileNotFoundException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.sleepycat.je.DatabaseException;

import edu.upenn.cis.cis455.storage.StorageFactory;
import edu.upenn.cis.cis455.storage.StorageInterface;

public class TestStorageDocument {

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
			db = StorageFactory.getDatabaseInstance("./testStorage");
		} catch (DatabaseException | IllegalArgumentException | FileNotFoundException e) {e.printStackTrace();}
		
		// Test document
		try {
			db.addDocument("some/url1", "some-content1", 
					Base64.getEncoder().encodeToString(MessageDigest.getInstance("MD5").digest("some-content1".getBytes())), 0, "text1");
			db.addDocument("some/url2", "some-content2", 
					Base64.getEncoder().encodeToString(MessageDigest.getInstance("MD5").digest("some-content2".getBytes())), 0, "text2");
			
			assertTrue(db.getCorpusSize() == 2);
			assertTrue(db.getDocument("some/url1").equals("some-content1"));
			assertTrue(db.getDocument("some/url2").equals("some-content2"));
			assertTrue(db.getContentType("some/url1").equals("text1"));
			assertTrue(db.getContentType("some/url2").equals("text2"));
			assertTrue(db.containsUrl("some/url1"));
			assertTrue(db.containsUrl("some/url2"));
			assertTrue(!db.containsUrl("some/url3"));
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		
		db.close();
	}

}
