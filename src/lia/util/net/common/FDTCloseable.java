/*
 * $Id: FDTCloseable.java 346 2007-08-16 13:48:25Z ramiro $
 */
package lia.util.net.common;


/**
 * An extended version of {@link java.io.Closeable} with the possibility to specify 
 * an eventual message and/or exception
 * 
 * The close() methods return true if they have been already called 
 * 
 * @author ramiro
 * 
 */
public interface FDTCloseable {

    public boolean close(String downMessage, Throwable downCause);
    public boolean isClosed();
    
}
