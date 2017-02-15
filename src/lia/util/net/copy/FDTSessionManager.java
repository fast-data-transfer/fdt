/*
 * $Id: FDTSessionManager.java 555 2009-12-14 17:16:27Z ramiro $
 */
package lia.util.net.copy;

import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import lia.util.net.common.AbstractFDTCloseable;
import lia.util.net.common.Config;
import lia.util.net.copy.transport.ControlChannel;
import lia.util.net.copy.transport.ControlChannelNotifier;
import lia.util.net.copy.transport.FDTProcolException;

/**
 *
 * This class is the placeholder for all the alve FDTSessions instantiated
 * in the entire FDT app
 *
 * @author ramiro
 */
public class FDTSessionManager extends AbstractFDTCloseable implements ControlChannelNotifier {
    
    private static final Logger logger = Logger.getLogger(FDTSessionManager.class.getName());
    
    private static final FDTSessionManager _thisInstanceManager = new FDTSessionManager();
    private static final Config config = Config.getInstance();

    //the map with all the FDT sessions
    private final Map<UUID, FDTSession> fdtSessionMap;
    
    //used to wait for the sessions to finish
    private final Lock lock;
    private final Condition isSessionMapEmpty;
    
    //at least one session started
    private final AtomicBoolean inited;
    
    private volatile String lastDownMsg;
    private volatile Throwable lastDownCause;
    
    public static final FDTSessionManager getInstance() {
        return _thisInstanceManager;
    }
    
    
    
    private FDTSessionManager() {
        lock = new ReentrantLock();
        isSessionMapEmpty = lock.newCondition();
        fdtSessionMap = new ConcurrentHashMap<UUID, FDTSession>();
        inited = new AtomicBoolean(false);
    }
    
    public void addFDTClientSession(ControlChannel controlChannel) throws Exception {
        
        FDTSession fdtSession = null;
        try {
            if(controlChannel.remoteConf.get("-pull") != null) {
                //-> Start a reader and connect to the server
                fdtSession = new FDTReaderSession(controlChannel);
                if(logger.isLoggable(Level.FINER)) {
                    logger.log(Level.FINER, " Adding FDTReaderSession ( " + fdtSession.sessionID + " ) to the FDTSessionManager");
                }
            } else {
                //-> Start a writer and connect to the server
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
    //called from
    public FDTSession addFDTClientSession() throws Exception {
        
        FDTSession fdtSession = null;
        
        try {
            
            if(config.isPullMode()) {
                //-> Start a writer and connect to the server
                fdtSession = new FDTWriterSession();
            } else {
                //-> Start a reader and connect to the server
                fdtSession = new FDTReaderSession();
            }
            
            //I may start the control thread now; FindBugs suggestion
            fdtSession.startControlThread();
            
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
        return fdtSession;
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
        
        //I know ... it's not very well sync, but should be enough for the client side ... which will have only one FDT Session
        if (fdtSessionMap.size() == 0) {
            lock.lock();
            try {
                
                lastDownMsg = downMessage;
                lastDownCause = downCause;
                
                isSessionMapEmpty.signalAll();
            } finally {
                lock.unlock();
            }
        }
        return fdtSession.close(downMessage, downCause);
    }
    
    public void addWorker(final UUID fdtSessionID, final SocketChannel sc) throws Exception {
        final FDTSession fdtSession = fdtSessionMap.get(fdtSessionID);
        if(fdtSession != null) {
            fdtSession.transportProvider.addWorkerStream(sc, true);
        } else {
            logger.log(Level.WARNING, "\n\n [ FDTSessionManager ] No such session " + fdtSessionID + " for worker: " + sc + ". The channel will be closed");
            try {
                sc.close();
            }catch(Throwable _) {}
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
    
    public void awaitTermination() throws InterruptedException {
        lock.lock();
        try {
            while(fdtSessionMap.size() > 0) {
                isSessionMapEmpty.await();
            }
        } finally {
            lock.unlock();
        }
    }
    
    public Throwable getLasDownCause() {
        lock.lock();
        try {
            return lastDownCause;
        } finally {
            lock.unlock();
        }
    }
    
    public String getLasDownMessage() {
        lock.lock();
        try {
            return lastDownMsg;
        } finally {
            lock.unlock();
        }
    }
    
    public void notifyCtrlSessionDown(ControlChannel controlChannel, Throwable cause) {
        final FDTSession fdtSession = fdtSessionMap.get(controlChannel.fdtSessionID());
        if(fdtSession != null) {
            fdtSession.notifyCtrlSessionDown(controlChannel, cause);
        }
    }

    @Override
    protected void internalClose() throws Exception {
        
    }
}