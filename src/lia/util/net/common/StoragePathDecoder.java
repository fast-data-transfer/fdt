/*
 * $Id$
 */
package lia.util.net.common;

/**
 * @author Ilya Narsky
 */
public class StoragePathDecoder {

    private String storageType;
    private String siteStorageID;
    private String storageRoot;
    private String pathToFileFromRoot;

    public StoragePathDecoder(String path,
                              String siteStorageID,
                              String storageRoot) {
        if (siteStorageID != null) this.siteStorageID = siteStorageID;
        if (storageRoot != null) this.storageRoot = storageRoot;
        if (path != null) this.decode(path);
    }

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

    public boolean hasStorageInfo() {
        return (this.storageType != null && this.storageType.length() != 0);
    }


    private void decode(String path) {
        // remove spaces
        String temp = path.trim();

        // the first part up to "_//" is treated as storage
        if (temp.contains("_//")) {
            String[] splitByStorage = temp.split("_//", 2);
            this.storageType = splitByStorage[0];
            temp = splitByStorage[1];
        }

        // remove the site name from the string
        temp = temp.replaceFirst(this.siteStorageID, "");

        // remove the storage root from the string
        temp = temp.replaceFirst(this.storageRoot, "");

        // the rest of it is supposed to be the full path to file
        this.pathToFileFromRoot = temp;
    }// end of decode()

}

