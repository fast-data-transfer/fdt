/*
 * $Id$
 */
package lia.util.net.copy.transport;

import java.io.Serializable;

/**
 * This class will be the only message that will be sent over the control channel
 * it will encapsulate message between two endpoint <code>FDTSession</code>-s 
 * 
 * @author ramiro
 */
public class CtrlMsg implements Serializable {
    
    private static final long serialVersionUID = 6815237091777780075L;
    
    /** a ping like message used to test the connection  - at the connection Level*/
    public static final int KEEP_ALIVE_MSG          = 0;
    
    /** message will be a string in the followinf format "major.minor.maintenance-releaseDate" */
    public static final int PROTOCOL_VERSION        = 1;
    
    /** message will be the UUID ( aka the sessionID which will be the same at both ends )*/
    public static final int SESSION_ID              = 2;

    /** message will be an Short */
    public static final int SESSION_TYPE            = 3;
    
    /** message will be a FDTInitMsg */
    public static final int INIT_FDT_CONF           = 4;
    
    //FDTSession messages
    
    /** ping-like message - this is at the session LEVEL*/
    public static final int PING_SESSION            = 5;
    
    //From here on are ctrl messages used by the managers  
    
    /** message will be a SessionConfig message */
    public static final int INIT_FDTSESSION_CONF   = 6;
    
    /** message will be a SessionConfig message */
    public static final int FINAL_FDTSESSION_CONF  = 7;
    
    /** message will be a SessionConfig message */
    public static final int FINISHED_FILE_SESSIONS  = 8;
    
    /** Notified whenever the other END is able to start */
    public static final int START_SESSION           = 9;
    
    /** message can be null or a String representing the cause*/
    public static final int END_SESSION             = 10;
    
    /** message types designated to GUI */
    public static final int GUI_MSG					= 11;

    /**
     *  
     * sent, eventually, by the FDTWriter session to notify <b>ONLY</b>
     * the ControlChannel that it may close the socket...
     * 
     **/
    public static final int END_SESSION_FIN2        = 12;

    /** message types designated for third party copy feature */
    public static final int THIRD_PARTY_COPY				= 13;
    
    private static final String[] CTRL_MSG_TAGS = new String[] {
        "KEEP_ALIVE_MSG", "PROTOCOL_VERSION", "SESSION_ID", "SESSION_TYPE", "INIT_FDT_CONF", "PING_SESSION", 
        "INIT_FDTSESSION_CONF", "FINAL_FDTSESSION_CONF", "FINISHED_FILE_SESSIONS", "START_SESSION", "END_SESSION",
        "GUI_MSG", "END_SESSION_FIN2", "THIRD_PARTY_COPY"
        };
    
    /**
     * the tag of the REQ/RESPONSE; based on this message instanceof can be avoided,
     * though I do not think is such a big performance gain
     */
    public final int tag;
    
    
    /**
     * the message, the one and only 
     */
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
