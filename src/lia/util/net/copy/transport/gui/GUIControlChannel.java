/*
 * $Id$
 */
package lia.util.net.copy.transport.gui;

import lia.util.net.common.AbstractFDTCloseable;
import lia.util.net.common.Config;
import lia.util.net.copy.transport.CtrlMsg;
import lia.util.net.copy.transport.FDTProcolException;

import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * A special control channel used to transport only GUI messages - on the client side of the connection.
 *
 * @author Ciprian Dobre
 */
public class GUIControlChannel extends AbstractFDTCloseable implements Runnable {

    public static final int CONNECT_TIMEOUT = 20 * 1000;
    private static final CtrlMsg versionMsg = new CtrlMsg(CtrlMsg.PROTOCOL_VERSION, Config.FDT_FULL_VERSION + "-" + Config.FDT_RELEASE_DATE);
    public final InetAddress remoteAddress;
    public final int remotePort;
    public final int localPort;
    private GUIControlChannelNotifier notifier;
    private Socket controlSocket;
    private ObjectOutputStream oos = null;
    private ObjectInputStream ois = null;
    private ConcurrentLinkedQueue<Object> qToSend = new ConcurrentLinkedQueue<Object>();

    /**
     * Try to connect to a remote FDT instance
     *
     * @param address
     * @param port
     * @param notifier
     * @throws Exception
     */
    public GUIControlChannel(String address, int port, GUIControlChannelNotifier notifier) throws Exception {
        this(InetAddress.getByName(address), port, notifier);
    }

    /**
     * Try to connect to a remote FDT instance
     *
     * @param inetAddress
     * @param port
     * @param notifier
     * @throws Exception
     */
    public GUIControlChannel(InetAddress inetAddress, int port, GUIControlChannelNotifier notifier) throws Exception {
        try {
            this.notifier = notifier;

            controlSocket = new Socket();
            controlSocket.connect(new InetSocketAddress(inetAddress, port), CONNECT_TIMEOUT);

            this.remoteAddress = inetAddress;
            this.remotePort = port;
            this.localPort = controlSocket.getLocalPort();

            controlSocket.setTcpNoDelay(true);

            //only the first octet will be interpreted by the AcceptTask at the other end
            controlSocket.getOutputStream().write(new byte[]{3});

            //from now on only CtrlMsg will be sent
            initStreams();
            controlSocket.setSoTimeout(1000);

            //
        } catch (Throwable t) {
            close("Cannot instantiate ControlChannel", t);
            throw new Exception(t);
        }
    }

    public String toString() {
        return (controlSocket == null) ? "null" : controlSocket.toString();
    }

    private void initStreams() throws Exception {
        oos = new ObjectOutputStream(new BufferedOutputStream(controlSocket.getOutputStream()));
        //send the version

        sendMsgImpl(versionMsg);

        ois = new ObjectInputStream(new BufferedInputStream(controlSocket.getInputStream()));

        //wait for remote version
        CtrlMsg ctrlMsg = (CtrlMsg) ois.readObject();
        if (ctrlMsg.tag != CtrlMsg.PROTOCOL_VERSION) {
            throw new FDTProcolException("Unexpected remote control message. Expected PROTOCOL_VERSION tag [ " + CtrlMsg.PROTOCOL_VERSION + " ] Received tag: " + ctrlMsg.tag);
        }

        Object o = ois.readObject(); // read the working dir
        notifier.notifyCtrlMsg(this, o);
        o = ois.readObject(); // read the user home
        notifier.notifyCtrlMsg(this, o);
        o = ois.readObject(); // read the os name
        notifier.notifyCtrlMsg(this, o);
        o = ois.readObject(); // read the list of files
        notifier.notifyCtrlMsg(this, o);
        o = ois.readObject(); // read the current roots
        notifier.notifyCtrlMsg(this, o);
    }

    private void cleanup() {
        if (ois != null) {
            try {
                ois.close();
            } catch (Throwable t) {
                //ignore
            }
            ois = null;
        }

        if (oos != null) {
            try {
                oos.close();
            } catch (Throwable t) {
                //ignore
            }
            oos = null;
        }

        if (controlSocket != null) {
            try {
                controlSocket.close();
            } catch (Throwable t) {
                //ignore
            }
            controlSocket = null;
        }
    }

    public void sendCtrlMessage(Object ctrlMsg) throws IOException, FDTProcolException {

        if (isClosed() && controlSocket != null && !controlSocket.isClosed()) {
            throw new FDTProcolException("Control channel already closed");
        }

        qToSend.add(ctrlMsg);
    }

    public boolean isClosed() {
        return super.isClosed() || controlSocket == null || controlSocket.isClosed();
    }

    private void sendAllMsgs() throws Exception {
        for (; ; ) {
            Object ctrlMsg = qToSend.poll();
            if (ctrlMsg == null) break;
            sendMsgImpl(ctrlMsg);
        }
    }

    private void sendMsgImpl(Object o) throws Exception {
        try {
            oos.writeObject(o);
            oos.reset();//DO NOT CACHE!
            oos.flush();
        } catch (Throwable t) {
            close("Exception sending control data", t);
            throw new IOException(" Cannot send ctrl message ( " + t.getCause() + " ) ");
        }
    }

    public void run() {
        try {
            while (!isClosed()) {
                try {
                    sendAllMsgs();
                    Object o = ois.readObject();
                    if (o == null) continue;
                    notifier.notifyCtrlMsg(this, o);
                } catch (SocketTimeoutException ste) {
                    //ignore this??? or shall I close it() ?
                } catch (FDTProcolException fdte) {
                    close("FDTProtocolException", fdte);
                }
            }

        } catch (Throwable t) {
            close(null, t);
        }
    }

    protected void internalClose() {
        try {
            cleanup();
        } catch (Throwable ignore) {
        }

        try {
            if (notifier != null) {
                notifier.notifyCtrlSessionDown(this, downCause());
            }
        } catch (Throwable ignore) {
        }
    }

} // end of class GUIControlChannel


