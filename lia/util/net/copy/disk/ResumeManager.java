package lia.util.net.copy.disk;

import java.util.logging.Level;
import java.util.logging.Logger;

import lia.util.net.copy.FileSession;


public class ResumeManager {

    
    private static final transient Logger logger = Logger.getLogger(ResumeManager.class.getName());


    public static ResumeManager _thisInstance;
    private static volatile boolean initialized;
    
    public static final ResumeManager getInstance() {
        
        if(!initialized) {
            synchronized(ResumeManager.class) {
                if(!initialized) {
                    initialized = true;
                    _thisInstance = new ResumeManager();
                }
            }
        }
        
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
