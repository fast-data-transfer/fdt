
package lia.util.net.copy.transport;

import java.io.Serializable;


public class CtrlMsg implements Serializable {
    
    private static final long serialVersionUID = 6815237091777780075L;
    
    
    public static final int KEEP_ALIVE_MSG          = 0;
    
    
    public static final int PROTOCOL_VERSION        = 1;
    
    
    public static final int SESSION_ID              = 2;

    
    public static final int SESSION_TYPE            = 3;
    
    
    public static final int INIT_FDT_CONF           = 4;
    
    
    
    
    public static final int PING_SESSION            = 5;
    
    
    
    
    public static final int INIT_FDTSESSION_CONF   = 6;
    
    
    public static final int FINAL_FDTSESSION_CONF  = 7;
    
    
    public static final int FINISHED_FILE_SESSIONS  = 8;
    
    
    public static final int START_SESSION           = 9;
    
    
    public static final int END_SESSION             = 10;
    
    
    public static final int GUI_MSG					= 11;
    
    
    public static final int END_SESSION_FIN2        = 12;
    
    private static final String[] CTRL_MSG_TAGS = new String[] {
        "KEEP_ALIVE_MSG", "PROTOCOL_VERSION", "SESSION_ID", "SESSION_TYPE", "INIT_FDT_CONF", "PING_SESSION", 
        "INIT_FDTSESSION_CONF", "FINAL_FDTSESSION_CONF", "FINISHED_FILE_SESSIONS", "START_SESSION", "END_SESSION",
        "GUI_MSG"
        };
    
    
    public final int tag;
    
    
    
    public final Object message;
    
    public CtrlMsg(int tag, Object message) {
        this.tag = tag;
        this.message = message;
    }
    
    public String toString() {
        StringBuilder sb = new StringBuilder();
        
        sb.append("tag ( ").append(tag).append(" ): ");
        if(tag < 0 || tag >= CTRL_MSG_TAGS.length) {
            sb.append("UNKNOWN_TAG");
        } else {
            sb.append(CTRL_MSG_TAGS[tag]);
        }
        sb.append(" msg: ").append(message);
        
        return sb.toString();
    }
}
