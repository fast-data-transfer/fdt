package lia.util.net.copy;

import java.nio.channels.SocketChannel;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import lia.util.net.common.Config;
import lia.util.net.copy.transport.ControlChannel;
import lia.util.net.copy.transport.ControlChannelNotifier;
import lia.util.net.copy.transport.FDTProcolException;


public class FDTSessionManager implements ControlChannelNotifier {
    
    private static final Logger logger = Logger.getLogger(FDTSessionManager.class.getName());
    
    private static final FDTSessionManager _thisInstanceManager;
    private static final Config config = Config.getInstance();
    
    static {
        _thisInstanceManager = new FDTSessionManager();
    }
    
    public static final FDTSessionManager getInstance() {
        return _thisInstanceManager;
    }
    
    
    ConcurrentHashMap<UUID, FDTSession> fdtSessionMap = new ConcurrentHashMap<UUID, FDTSession>();
    private AtomicBoolean inited = new AtomicBoolean(false);
    
    private FDTSessionManager() {
    }
    
    public void addFDTClientSession(ControlChannel controlChannel) throws Exception {
        
        FDTSession fdtSession = null;
        try {
            if(controlChannel.remoteConf.get("-pull") != null) {
                
                fdtSession = new FDTReaderSession(controlChannel);
                if(logger.isLoggable(Level.FINER)) {
                    logger.log(Level.FINER, " Adding FDTReaderSession ( " + fdtSession.sessionID + " ) to the FDTSessionManager");
                }
            } else {
                
                fdtSession = new FDTWriterSession(controlChannel);
                if(logger.isLoggable(Level.FINER)) {
                    logger.log(Level.FINER, " Adding FDTWriterSession ( " + fdtSession.sessionID + " ) to the FDTSessionManager");
                }
            }
            
            fdtSessionMap.put(fdtSession.sessionID(), fdtSession);
            inited.set(true);
            
        } catch(Throwable t) {
            logger.log(Level.WARNING, " Got exception instantiate Session/RemoteConn ", t);
            
            if(fdtSession != null) {
                try {
                    fdtSession.close("exception instantiate Session/RemoteConn", t);
                }catch(Throwable ignore){}
            }
            
            if(controlChannel != null) {
                try {
                    controlChannel.close("exception instantiate Session/RemoteConn", t);
                }catch(Throwable ignore){}
            }
            
            throw new Exception(t);
        }
    }
    
    public void addFDTClientSession() throws Exception {
        
        FDTSession fdtSession = null;
        
        try {
            
            if(config.isPullMode()) {
                
                fdtSession = new FDTWriterSession();
            } else {
                
                fdtSession = new FDTReaderSession();
            }
            
            fdtSessionMap.put(fdtSession.sessionID(), fdtSession);
            inited.set(true);
            
        } catch(Throwable t) {
            logger.log(Level.WARNING, " Got exception initiation Session/RemoteConn ", t);
            
            if(fdtSession != null) {
                try {
                    fdtSession.close(null, t);
                }catch(Throwable ignore){}
            }
            throw new Exception(t);
        }
    }
    
    public int sessionsNumber() {
        return fdtSessionMap.size();
    }
    
    public boolean isInited() {
        return inited.get();
    }
    
    public FDTSession getSession(UUID fdtSessionID) {
        return fdtSessionMap.get(fdtSessionID);
    }
    
    public boolean finishSession(UUID fdtSessionID, String downMessage, Throwable downCause) {
        FDTSession fdtSession = fdtSessionMap.remove(fdtSessionID);
        if(fdtSession == null) {
            return false;
        }
        
        return fdtSession.close(downMessage, downCause);
    }
    
    public void addWorker(UUID fdtSessionID, SocketChannel sc) throws Exception {
        FDTSession fdtSession = fdtSessionMap.get(fdtSessionID);
        if(fdtSession != null) {
            fdtSession.transportProvider.addWorkerStream(sc);
        } else {
            logger.log(Level.WARNING, "\n\n No such session " + fdtSessionID + " for worker " + sc);
        }
    }
    
    public void notifyCtrlMsg(ControlChannel controlChannel, Object o) throws FDTProcolException {
        if(controlChannel == null) {
            throw new NullPointerException("ControlChannel cannot be null in notifier!");
        }
        
        FDTSession fdtSession = fdtSessionMap.get(controlChannel.fdtSessionID());
        if(fdtSession == null) {
            try {
                
                
            } catch(Throwable t) {
                controlChannel.close("No such session in my Map", t);
            }
        } else {
            fdtSession.notifyCtrlMsg(controlChannel, o);
        }
    }
    
    public void notifyCtrlSessionDown(ControlChannel controlChannel, Throwable cause) throws FDTProcolException {
        FDTSession fdtSession = fdtSessionMap.get(controlChannel.fdtSessionID());
        if(fdtSession != null) {
            fdtSession.notifyCtrlMsg(controlChannel, cause);
        } else {
            controlChannel.close("No such fdtSession in the FDT Sessions Map", null);
        }
    }
}