package lia.util.net.copy.transport;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.security.auth.Subject;

import lia.gsi.GSIServer;
import lia.gsi.net.GSIGssSocketFactory;
import lia.util.net.common.AbstractFDTCloseable;
import lia.util.net.common.Config;
import lia.util.net.common.DirectByteBufferPool;
import lia.util.net.copy.FDTSessionManager;


public class ControlChannel extends AbstractFDTCloseable implements Runnable {

    private static final Logger logger = Logger.getLogger(ControlChannel.class.getName());
    
    private static final CtrlMsg versionMsg = new CtrlMsg(CtrlMsg.PROTOCOL_VERSION, Config.FDT_FULL_VERSION + "-" + Config.FDT_RELEASE_DATE);
    private static final Config config = Config.getInstance();

    public static final int CONNECT_TIMEOUT = 20 * 1000;
    
    public static final int SOCKET_TIMEOUT = 60 * 1000;
        
    private Socket controlSocket;

    private ConcurrentLinkedQueue<Object> qToSend = new ConcurrentLinkedQueue<Object>();
    
    private UUID fdtSessionID;
    private ObjectOutputStream oos = null;
    private ObjectInputStream ois = null;

    private ControlChannelNotifier notifier;
    
    private String fullRemoteVersion;
    
    public HashMap<String, Object> remoteConf;
    
    public final InetAddress remoteAddress;
    public final int remotePort;
    public final int localPort;
    
    public Subject subject;
    
    private String myName;
    
    private ControlChannel() {
        this.remoteAddress = null;
        this.remotePort = -1;
        this.localPort = -1;
    }

    
    public ControlChannel(String address, int port, UUID sessionID, ControlChannelNotifier notifier) throws Exception {
        this(InetAddress.getByName(address), port, sessionID, notifier);
    }

    
    public ControlChannel(InetAddress inetAddress, int port, UUID fdtSessionID, ControlChannelNotifier notifier) throws Exception {
        try {
            this.notifier = notifier;
            this.fdtSessionID = fdtSessionID;
            
            if(config.isGSIModeEnabled()) {
                GSIGssSocketFactory factory = new GSIGssSocketFactory();
                controlSocket = factory.createSocket(inetAddress, config.getGSIPort(), false, false);
                this.subject =  GSIGssSocketFactory.getLocalSubject(controlSocket);
            } else {
                controlSocket = new Socket();
                controlSocket.connect(new InetSocketAddress(inetAddress, port), CONNECT_TIMEOUT );
            }
            
            
            this.remoteAddress = inetAddress;
            this.remotePort = port;
            this.localPort = controlSocket.getLocalPort();
            
            controlSocket.setTcpNoDelay(true);
            
            
            if(!config.isGSIModeEnabled()) {
                controlSocket.getOutputStream().write(new byte[]{0});
            }
            
            
            
            initStreams();
            controlSocket.setSoTimeout(1000);

            
        } catch(Throwable t) {
            close("Cannot instantiate ControlChannel", t);
            throw new Exception(t);
        }
    }

    
    
    public ControlChannel(Socket s, ControlChannelNotifier notifier) throws Exception {
        try {
            this.controlSocket = s;
            
            this.remoteAddress = s.getInetAddress();
            this.remotePort = s.getPort();
            this.localPort = s.getLocalPort();
            
            this.notifier = notifier;
            
            initStreams();
            controlSocket.setSoTimeout(1000);

        } catch(Throwable t) {
            close("Cannot instantiate ControlChannel", t);
            throw new Exception(t);
        }
    }

    
    public ControlChannel(GSIServer parent, Socket s, Subject peerSubject, ControlChannelNotifier notifier) throws Exception {
        try {
            
            this.controlSocket = s;
            this.subject = peerSubject;
            this.remoteAddress = s.getInetAddress();
            this.remotePort = s.getPort();
            this.localPort = s.getLocalPort();
            
            this.notifier = notifier;
            
            initStreams();
            controlSocket.setSoTimeout(1000);

        } catch(Throwable t) {
            close("Cannot instantiate ControlChannel", t);
            throw new Exception(t);
        }
    }
    
    public UUID fdtSessionID() {
        return fdtSessionID;
    }
    
    public String toString() {
        return (controlSocket == null)?"null":controlSocket.toString();
    }
    
    private void initStreams() throws Exception {
        oos = new ObjectOutputStream(new BufferedOutputStream(controlSocket.getOutputStream()));

        
        
        sendMsgImpl(versionMsg);

        ois = new ObjectInputStream(new BufferedInputStream(controlSocket.getInputStream()));


        
        CtrlMsg ctrlMsg = (CtrlMsg)ois.readObject();
        if(ctrlMsg.tag != CtrlMsg.PROTOCOL_VERSION) {
            throw new FDTProcolException("Unexpected remote control message. Expected PROTOCOL_VERSION tag [ " + CtrlMsg.PROTOCOL_VERSION + " ] Received tag: " + ctrlMsg.tag);
        }
        
        this.fullRemoteVersion = (String)ctrlMsg.message;
        
        ctrlMsg = new CtrlMsg(CtrlMsg.INIT_FDT_CONF, Config.getInstance().getConfigMap());
        sendMsgImpl(ctrlMsg);
        
        
        ctrlMsg = (CtrlMsg)ois.readObject();
        if(ctrlMsg.tag != CtrlMsg.INIT_FDT_CONF) {
            throw new FDTProcolException("Unexpected remote control message. Expected INIT_FDT_CONF tag [ " + CtrlMsg.INIT_FDT_CONF + " ] Received tag: " + ctrlMsg.tag);
        }
        
        this.remoteConf = (HashMap<String, Object>)ctrlMsg.message;
        try {
           
            if(DirectByteBufferPool.initInstance(Integer.parseInt((String)remoteConf.get("-bs")))) {
                if(logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, "The buffer pool has been initialized");
                }
           } else {
               if(logger.isLoggable(Level.FINE)) {
                   logger.log(Level.FINE, "The buffer pool is already initialized");
               }
           }
           
        } catch(Throwable t) {
            throw new FDTProcolException("Unable to instantiate the buffer pool", t);
        }
        
        if(fdtSessionID == null) {
            ctrlMsg = (CtrlMsg)ois.readObject();
            if(ctrlMsg.tag == CtrlMsg.SESSION_ID) {
                fdtSessionID = (UUID)ctrlMsg.message;
            } else {
                throw new FDTProcolException("Unexpected remote control message. Expected SESSION_ID tag [ " + CtrlMsg.SESSION_ID + " ] Received tag: " + ctrlMsg.tag);
            }
        } else {
            sendMsgImpl(new CtrlMsg(CtrlMsg.SESSION_ID, fdtSessionID));
        }
        myName = " ControlThread for ( " + fdtSessionID + " ) " + controlSocket.getInetAddress() + ":" + controlSocket.getPort();
        logger.log(Level.INFO, "NEW CONTROL stream for " + fdtSessionID + " initialized ");
        
    }
    
    public String remoteVersion() {
        return fullRemoteVersion;
    }
    
    private void cleanup() {
        if(ois != null) {
            try {
                ois.close();
            }catch(Throwable t){
                
            }
            ois = null;
        }

        if(oos != null) {
            try {
                oos.close();
            }catch(Throwable t){
                
            }
            oos = null;
        }

        if(controlSocket != null) {
            try {
                controlSocket.close();
            }catch(Throwable t){
                
            }
            controlSocket = null;
        }
    }

    public void sendCtrlMessage(Object ctrlMsg) throws IOException, FDTProcolException {
        
        if(isClosed()) {
            throw new FDTProcolException("Control channel already closed");
        }
        
        if(logger.isLoggable(Level.FINER)) {
            logger.log(Level.FINER, "CtrlChannel Queuing for send: " + ctrlMsg.toString());
        }
        
        qToSend.add(ctrlMsg);
        
    }

    private void sendAllMsgs() throws Exception {
        for(;;) {
            Object ctrlMsg = qToSend.poll();
            if(ctrlMsg == null) break;
            sendMsgImpl(ctrlMsg);
        }
    }
    
    private void sendMsgImpl(Object o) throws Exception {
        try {
            oos.writeObject(o);
            oos.reset();
            oos.flush();
        } catch(Throwable t) {
            logger.log(Level.WARNING, "Got exception sending ctrl message", t);
            close("Exception sending control data", t);
            throw new IOException(" Cannot send ctrl message ( "  + t.getCause() + " ) ");
        }
    }
    
    public void run() {
        if(logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, myName + " STARTED");
        }
        try {

            while(!isClosed()) {
                try {
                    sendAllMsgs();
                    Object o = ois.readObject();
                    if(o == null) continue;
                    notifier.notifyCtrlMsg(this, o);
                } catch(SocketTimeoutException ste) {
                    
                } catch(FDTProcolException fdte) {
                    logger.log(Level.WARNING, myName, fdte);
                    close("FDTProtocolException", fdte);
                }
            }
            
        } catch(Throwable t) {
            if(!isClosed()) {
                if(logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, "Control Thread for " + myName + " got exception in main loop", t);
                }
            }
            close(null, t);
        }
        
        logger.log(Level.INFO, myName + " FINISHED");
        close(downMessage(), downCause());
        FDTSessionManager.getInstance().finishSession(fdtSessionID, downMessage(), downCause());
        
    }

    protected void internalClose() {
        try {
            cleanup();
        }catch(Throwable ignore){}
        
        try {
            if(notifier != null) {
                notifier.notifyCtrlSessionDown(this, downCause());
            }
        }catch(Throwable ignore){}
    }
    
}
