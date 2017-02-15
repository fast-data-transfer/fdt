/*
 * $Id: ResumeManager.java 347 2007-08-16 13:56:39Z ramiro $
 */
package lia.util.net.copy.disk;

import java.util.logging.Level;
import java.util.logging.Logger;

import lia.util.net.copy.FileSession;

/**
 * 
 * This class is used to "check" if a {@link FileSession} is already copied or not
 * For the moment, two "remote" <code>{@link java.io.File}-s</code> are considered
 * equal only if they have the {@link java.io.File.length} and {@link java.io.File.lastModified}
 * are the same for both of them
 *  
 * @author ramiro
 */
public class ResumeManager {

    /** Logger used by this class */
    private static final transient Logger logger = Logger.getLogger(ResumeManager.class.getName());


    private static final ResumeManager _thisInstance = new ResumeManager();
    
    public static final ResumeManager getInstance() {
        return _thisInstance;
    }
    
    public boolean isFinished(FileSession fileSession) {

        try {
            if( fileSession.sessionSize() == 0 ) {
                try {
                    fileSession.getFile().createNewFile();
                    fileSession.close("0 size file", null);
                    return true;
                } catch(Throwable t1) {
                    t1.printStackTrace();
                }
            }
            
            if(fileSession.getFile().exists())
                if(fileSession.sessionSize() == fileSession.getFile().length() &&
                    fileSession.lastModified() == fileSession.getFile().lastModified()) {
                return true;
            }
        } catch (Throwable t) {
            logger.log(Level.WARNING, " [ ResumeManager ] Got exception checking if fileSession [ " + fileSession.fileName() + " / " + fileSession.sessionID() + " ] is finished ", t); 
        }
        
        return false;
    }
    
}
