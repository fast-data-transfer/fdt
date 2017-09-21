/*
 * $Id$
 * Created on Aug 20, 2007
 *
 * moved from lia.util.net.copy.gui
 */
package lia.util.net.copy.transport.gui;

import java.io.Serializable;

/**
 * @author Ciprian Dobre
 * The serializable object representing the properties of a file...
 */
public class FileHandler implements Serializable {
    /**
     * <code>serialVersionUID</code>
     */
    private static final long serialVersionUID = 1988671591829311032L;
    /**
     * The name of the file or folder
     */
    private final String name;
    /**
     * The last time it was modified
     */
    private final long modif;
    /**
     * The length of the file, should be -1 if folder
     */
    private final long size;
    /**
     * Read flag is set ?
     */
    private final boolean read;
    /**
     * Write flag is set ?
     */
    private final boolean write;

    public FileHandler(String name, long modif, long size, boolean read, boolean write) {
        this.name = name;
        this.modif = modif;
        this.size = size;
        this.read = read;
        this.write = write;
    }

    public final String getName() {
        return name;
    }

    public final long getModif() {
        return modif;
    }

    public final long getSize() {
        return size;
    }

    public final boolean canRead() {
        return read;
    }

    public final boolean canWrite() {
        return write;
    }
}