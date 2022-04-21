package edu.upenn.cis.cis455.crawler;

import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;

import com.sleepycat.je.DatabaseException;

import edu.upenn.cis.cis455.storage.StorageFactory;
import edu.upenn.cis.cis455.storage.StorageInterface;

public class Exporter {
	public static void main(String args[]) {
		if (args.length < 1 || args.length > 3) {
            System.out.println("Usage: Export {database environment path} {output path}");
            System.exit(1);
        }

        System.out.println("Exporter starting");
//        String envPath = args[0];
//        String outputPath = args[1];
//        
//        StorageInterface db = null;
//		try {
//			db = StorageFactory.getDatabaseInstance(envPath);
//		} catch (DatabaseException | IllegalArgumentException | FileNotFoundException e) {
//			e.printStackTrace();
//		}
//		
//		System.out.println(db.getCorpusSize());
//		
//		if (!Files.exists(Paths.get(outputPath))) {
//            try {
//                Files.createDirectory(Paths.get(outputPath));
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//		
//		for (String url : db.allUrls()) {
//			FileWriter myWriter;
//			try {
//				myWriter = new FileWriter(outputPath + "/" + UUID.randomUUID().toString());
//				myWriter.write(url + "\n");
//				myWriter.write(db.getDocument(url));
//			    myWriter.close();
//			} catch (IOException e) {
//				e.printStackTrace();
//			}		    
//		}
//
//        db.close();
	}
}
