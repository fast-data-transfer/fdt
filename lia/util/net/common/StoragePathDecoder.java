
package lia.util.net.common;


public class StoragePathDecoder {
    
    private String storageType;
    private String siteStorageID;
    private String storageRoot;
    private String pathToFileFromRoot;
    
    public String storageType() {
	return this.storageType;
    }
    public String siteStorageID() {
	return this.siteStorageID;
    }
    public String storageRoot() {
	return this.storageRoot;
    }
    public String pathToFileFromRoot() {
	return this.pathToFileFromRoot;
    }
    
    
    public StoragePathDecoder(String path, 
			      String siteStorageID, 
			      String storageRoot) {
	if( siteStorageID != null ) this.siteStorageID = siteStorageID;
	if( storageRoot != null ) this.storageRoot = storageRoot;
	if( path != null ) this.decode(path);
    }

    
    public boolean hasStorageInfo() {
	return (this.storageType!=null && this.storageType.length()!=0);
    }
    
    
    private void decode(String path) {
	
	String temp = path.trim();
	
	
	if( temp.contains("_//") ) {
	    String[] splitByStorage = temp.split("_//",2);
	    this.storageType = splitByStorage[0];
	    temp = splitByStorage[1];
	}
	
	
	temp = temp.replaceFirst(this.siteStorageID,"");
	
	
	temp = temp.replaceFirst(this.storageRoot,"");
	
	
	this.pathToFileFromRoot = temp;
    }
    
}

