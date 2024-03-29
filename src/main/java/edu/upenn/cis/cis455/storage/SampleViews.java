package edu.upenn.cis.cis455.storage;
import com.sleepycat.bind.EntryBinding;
import com.sleepycat.bind.serial.ClassCatalog;
import com.sleepycat.bind.serial.SerialBinding;
import com.sleepycat.collections.StoredEntrySet;
import com.sleepycat.collections.StoredSortedMap;

public class SampleViews {
	@SuppressWarnings("rawtypes")
	private StoredSortedMap documentMap;
	@SuppressWarnings("rawtypes")
	private StoredSortedMap urlMap;
	@SuppressWarnings("rawtypes")
	private StoredSortedMap md5Map;
	@SuppressWarnings("rawtypes")
	private StoredSortedMap queueMap;

    @SuppressWarnings({ "unchecked", "rawtypes" })
	public SampleViews(SampleDatabase db) {
    	ClassCatalog catalog = db.getClassCatalog();
    	
        EntryBinding documentKeyBinding = new SerialBinding(catalog, DocumentKey.class);
        EntryBinding documentDataBinding = new SerialBinding(catalog, DocumentData.class);
        documentMap = new StoredSortedMap(db.getDocumentDatabase(), documentKeyBinding, documentDataBinding, true);
        
        EntryBinding urlKeyBinding = new SerialBinding(catalog, DocumentKey.class);
        EntryBinding urlDataBinding = new SerialBinding(catalog, TrivialData.class);
        urlMap = new StoredSortedMap(db.getUrlDatabase(), urlKeyBinding, urlDataBinding, true);
        
        EntryBinding md5KeyBinding = new SerialBinding(catalog, Md5Key.class);
        EntryBinding md5DataBinding = new SerialBinding(catalog, TrivialData.class);
        md5Map = new StoredSortedMap(db.getMd5Database(), md5KeyBinding, md5DataBinding, true);
        
        EntryBinding queueKeyBinding = new SerialBinding(catalog, DocumentKey.class);
        EntryBinding queueDataBinding = new SerialBinding(catalog, TrivialData.class);
        queueMap = new StoredSortedMap(db.getQueueDatabase(), queueKeyBinding, queueDataBinding, true);
    }
    
    @SuppressWarnings("rawtypes")
	public final StoredSortedMap getDocumentMap() {
        return documentMap;
    }
    
    @SuppressWarnings("rawtypes")
	public final StoredEntrySet getDocumentEntrySet() {
        return (StoredEntrySet) documentMap.entrySet();
    }
    
    @SuppressWarnings("rawtypes")
	public final StoredSortedMap getUrlMap() {
        return urlMap;
    }
    
    @SuppressWarnings("rawtypes")
	public final StoredEntrySet getUrlEntrySet() {
        return (StoredEntrySet) urlMap.entrySet();
    }
    
    @SuppressWarnings("rawtypes")
	public final StoredSortedMap getMd5Map() {
        return md5Map;
    }
    
    @SuppressWarnings("rawtypes")
	public final StoredEntrySet getMd5EntrySet() {
        return (StoredEntrySet) md5Map.entrySet();
    }
    
    @SuppressWarnings("rawtypes")
	public final StoredSortedMap getQueueMap() {
        return queueMap;
    }
    
    @SuppressWarnings("rawtypes")
	public final StoredEntrySet getQueueEntrySet() {
        return (StoredEntrySet) queueMap.entrySet();
    }
}
