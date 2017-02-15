
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


public class FDTSessionManager extends AbstractFDTCloseable implements ControlChannelNotifier {
    
    private static final Logger logger = Logger.getLogger(FDTSessionManager.class.getName());
    
    private static final FDTSessionManager _thisInstanceManager = new FDTSessionManager();
    private static final Config config = Config.getInstance();

    
    private final Map<UUID, FDTSession> fdtSessionMap;
    
    
    private final Lock lock;
    private final Condition isSessionMapEmpty;
    
    
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
    
    public FDTSession addFDTClientSession() throws Exception {
        
        FDTSession fdtSession = null;
        
        try {
            
            if(config.isPullMode()) {
                
                fdtSession = new FDTWriterSession();
            } else {
                
                fdtSession = new FDTReaderSession();
            }
            
            
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
            fdtSession.transportProvider.addWorkerStream(sc);
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