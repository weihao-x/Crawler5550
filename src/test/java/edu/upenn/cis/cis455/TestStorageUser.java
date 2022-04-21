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

public class TestStorageUser {

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
		
		db.addUser("user1","123456");
		db.addUser("user2","654321");
		
		assertTrue(db.getSessionForUser("user1", "123456"));
		assertTrue(db.getSessionForUser("user2", "654321"));
		assertTrue(!db.getSessionForUser("user2", "123456"));
		assertTrue(!db.getSessionForUser("user1", "654321"));
		assertTrue(db.containsUsername("user1"));
		assertTrue(db.containsUsername("user2"));
		assertTrue(!db.containsUsername("user3"));
		
		db.close();
	}

}
