
package lia.util.net.copy.transport.gui;

import java.io.Serializable;


public class FileHandler implements Serializable {
    
    private static final long serialVersionUID = 1988671591829311032L;
    
    private final String name;
    
    private final long modif;
    
    private final long size;
    
    private final boolean read;
    
    private final boolean write;
    public FileHandler(String name, long modif, long size, boolean read, boolean write) {
        this.name = name;
        this.modif = modif;
        this.size = size;
        this.read = read;
        this.write = write;
    }
    public final String getName() { return name; }
    public final long getModif() { return modif; }
    public final long getSize() { return size; }
    public final boolean canRead() { return read; }
    public final boolean canWrite() { return write; }
}