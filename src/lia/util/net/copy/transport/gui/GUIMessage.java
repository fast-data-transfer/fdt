/*
 * $Id: GUIMessage.java 530 2009-06-04 13:38:13Z cipsm $
 * Created on Aug 20, 2007
 *
 * moved from lia.util.net.copy.gui
 */
package lia.util.net.copy.transport.gui;

import java.io.Serializable;

/**
 * 
 * @author Ciprian Dobre
 * 
 */
public class GUIMessage implements Serializable {
    /**
     * <code>serialVersionUID</code>
     */
    private static final long serialVersionUID = 1988671591829311032L;
    /** The tag of the message */
    private final int mID;
    /** The message */
    private final Object msg;
    
    /** Possible exception */
    private final Exception e;
    
    public GUIMessage(int mID, Object msg) {
        this.mID = mID;
        this.msg = msg;
        this.e = null;
    }
    
    public GUIMessage(int mID, Object msg, Exception e) {
    	this.mID = mID;
    	this.msg = msg;
    	this.e = e;
    }
    
    public final int getMID() { return mID; }
    public final Object getMsg() { return msg; }
    public final Exception getException() { return e; }
}