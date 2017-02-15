
package lia.util.net.copy.transport.gui;

import java.io.Serializable;


public class GUIMessage implements Serializable {
    
    private static final long serialVersionUID = 1988671591829311032L;
    
    private final int mID;
    
    private final Object msg;
    public GUIMessage(int mID, Object msg) {
        this.mID = mID;
        this.msg = msg;
    }
    public final int getMID() { return mID; }
    public final Object getMsg() { return msg; }
}