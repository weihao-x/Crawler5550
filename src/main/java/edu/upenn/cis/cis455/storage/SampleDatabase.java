package edu.upenn.cis.cis455.storage;

import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;

import java.io.File;
import java.io.FileNotFoundException;

import com.sleepycat.bind.serial.StoredClassCatalog;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;


public class SampleDatabase {
	private Environment env;
	private static final String CLASS_CATALOG = "java_class_catalog";
	private StoredClassCatalog javaCatalog;
    private static final String DOCUMENT_STORE = "document_store";
    private Database documentDb;
    private static final String USER_STORE = "user_store";
    private Database userDb;
    private static final String URL_STORE = "url_store";
    private Database urlDb;
    private static final String MD5_STORE = "md5_store";
    private Database md5Db;

    public SampleDatabase(String homeDirectory) throws DatabaseException, FileNotFoundException {
    	System.out.println("Opening environment in: " + homeDirectory);

    	EnvironmentConfig envConfig = new EnvironmentConfig();
        envConfig.setTransactional(true);
        envConfig.setAllowCreate(true);
        env = new Environment(new File(homeDirectory), envConfig);
        
        DatabaseConfig dbConfig = new DatabaseConfig();
        dbConfig.setTransactional(true);
        dbConfig.setAllowCreate(true);
        Database catalogDb = env.openDatabase(null, CLASS_CATALOG, dbConfig);

        javaCatalog = new StoredClassCatalog(catalogDb);
        
        documentDb = env.openDatabase(null, DOCUMENT_STORE, dbConfig);
        userDb = env.openDatabase(null, USER_STORE, dbConfig);
        urlDb = env.openDatabase(null, URL_STORE, dbConfig);
        md5Db = env.openDatabase(null, MD5_STORE, dbConfig);
    }

    public void close() throws DatabaseException {
    	md5Db.close();
    	urlDb.close();
    	userDb.close();
    	documentDb.close();
    	javaCatalog.close();
    	env.truncateDatabase(null, MD5_STORE, false);
    	env.close();
    }
    
    public final Environment getEnvironment() {
        return env;
    }
    
    public final StoredClassCatalog getClassCatalog() {
        return javaCatalog;
    }
    
    public final Database getDocumentDatabase() {
        return documentDb;
    }
    
    public final Database getUserDatabase() {
        return userDb;
    }
    
    public final Database getUrlDatabase() {
        return urlDb;
    }
    
    public final Database getMd5Database() {
        return md5Db;
    }
}