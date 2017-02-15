
package lia.util.net.copy.transport.gui;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.ConcurrentLinkedQueue;

import lia.util.net.common.AbstractFDTCloseable;
import lia.util.net.common.Config;
import lia.util.net.copy.transport.CtrlMsg;
import lia.util.net.copy.transport.FDTProcolException;


public class GUIControlChannel extends AbstractFDTCloseable implements Runnable {

    private static final CtrlMsg versionMsg = new CtrlMsg(CtrlMsg.PROTOCOL_VERSION, Config.FDT_FULL_VERSION + "-" + Config.FDT_RELEASE_DATE);

    private GUIControlChannelNotifier notifier;
    private Socket controlSocket;
    public final InetAddress remoteAddress;
    public final int remotePort;
    public final int localPort;

    private ObjectOutputStream oos = null;
    private ObjectInputStream ois = null;

    public static final int CONNECT_TIMEOUT = 20 * 1000;

    private ConcurrentLinkedQueue<Object> qToSend = new ConcurrentLinkedQueue<Object>();

    
    public GUIControlChannel(String address, int port, GUIControlChannelNotifier notifier) throws Exception {
        this(InetAddress.getByName(address), port, notifier);
    }

    
    public GUIControlChannel(InetAddress inetAddress, int port, GUIControlChannelNotifier notifier) throws Exception {
        try {
            this.notifier = notifier;
            
            controlSocket = new Socket();
            controlSocket.connect(new InetSocketAddress(inetAddress, port), CONNECT_TIMEOUT );
            
            this.remoteAddress = inetAddress;
            this.remotePort = port;
            this.localPort = controlSocket.getLocalPort();
            
            controlSocket.setTcpNoDelay(true);
            
            
            controlSocket.getOutputStream().write(new byte[]{3});
            
            
            initStreams();
            controlSocket.setSoTimeout(1000);

            
        } catch(Throwable t) {
            close("Cannot instantiate ControlChannel", t);
            throw new Exception(t);
        }
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
        
        Object o = ois.readObject(); 
        notifier.notifyCtrlMsg(this, o);
        o = ois.readObject(); 
        notifier.notifyCtrlMsg(this, o);
        o = ois.readObject(); 
        notifier.notifyCtrlMsg(this, o);
        o = ois.readObject(); 
        notifier.notifyCtrlMsg(this, o);
        o = ois.readObject(); 
        notifier.notifyCtrlMsg(this, o);
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
        
        if(isClosed() && controlSocket != null && !controlSocket.isClosed()) {
            throw new FDTProcolException("Control channel already closed");
        }
        
        qToSend.add(ctrlMsg);
    }
    
    public boolean isClosed() {
    	return super.isClosed() || controlSocket == null || controlSocket.isClosed();
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
            close("Exception sending control data", t);
            throw new IOException(" Cannot send ctrl message ( "  + t.getCause() + " ) ");
        }
    }
    
    public void run() {
        try {
            while(!isClosed()) {
                try {
                    sendAllMsgs();
                    Object o = ois.readObject();
                    if(o == null) continue;
                    notifier.notifyCtrlMsg(this, o);
                } catch(SocketTimeoutException ste) {
                    
                } catch(FDTProcolException fdte) {
                    close("FDTProtocolException", fdte);
                }
            }
            
        } catch(Throwable t) {
            close(null, t);
        }
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


