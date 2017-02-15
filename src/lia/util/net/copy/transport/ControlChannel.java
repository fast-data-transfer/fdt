/*
 * $Id: ControlChannel.java 707 2013-07-01 14:33:43Z ramiro $
 */
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
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.security.auth.Subject;

import lia.gsi.GSIServer;
import lia.gsi.net.GSIGssSocketFactory;
import lia.util.net.common.AbstractFDTCloseable;
import lia.util.net.common.Config;
import lia.util.net.common.DirectByteBufferPool;
import lia.util.net.common.FDTVersion;
import lia.util.net.common.Utils;

/**
 * Encapsulates the control socket ( channel ) between two peer FDTSessios When the constructor returns the
 * communication can begin ...
 * 
 * @author ramiro
 */
public class ControlChannel extends AbstractFDTCloseable implements Runnable {

    private static final Logger logger = Logger.getLogger(ControlChannel.class.getName());

    private static final CtrlMsg versionMsg = new CtrlMsg(CtrlMsg.PROTOCOL_VERSION, Config.FDT_FULL_VERSION + "-"
            + Config.FDT_RELEASE_DATE);

    private static final Config config = Config.getInstance();

    public static final int CONNECT_TIMEOUT = 20 * 1000;

    public static final int SOCKET_TIMEOUT = 60 * 1000;

    private final Socket controlSocket;

    private final ConcurrentLinkedQueue<Object> qToSend = new ConcurrentLinkedQueue<Object>();

    private final AtomicBoolean cleanupFinished = new AtomicBoolean(false);

    private UUID fdtSessionID;

    private volatile ObjectOutputStream oos = null;

    private volatile ObjectInputStream ois = null;

    private final ControlChannelNotifier notifier;

    private volatile String fullRemoteVersion;

    public Map<String, Object> remoteConf;

    public final InetAddress remoteAddress;

    public final int remotePort;

    public final int localPort;

    public volatile Subject subject;

    private volatile String myName;

    private volatile ScheduledFuture<?> ccptFuture;

    private static final class ControlChannelPingerTask implements Runnable {

        public static final CtrlMsg pingMsg = new CtrlMsg(CtrlMsg.KEEP_ALIVE_MSG, new byte[1]);

        private final ControlChannel cc;

        ControlChannelPingerTask(ControlChannel cc) {
            this.cc = cc;
            logger.log(Level.INFO, "[ ControlChannelPingerTask ] initialized");
        }

        @Override
        public void run() {

            if (logger.isLoggable(Level.FINER)) {
                logger.log(Level.FINER, "[ ControlChannelPingerTask ] sending KEEP_ALIVE_MSG");
            }

            try {
                this.cc.sendCtrlMessage(pingMsg);
            } catch (Throwable t) {
                logger.log(
                        Level.WARNING,
                        " [ ContrlChannelPingerTask ] Unable to send msg  ... Close the socket ??? This should not happen",
                        t);
            }

        }
    }

    /**
     * Try to connect to a remote FDT instance
     * 
     * @param address
     * @param port
     * @param sessionID
     * @param notifier
     * @throws Exception
     */
    public ControlChannel(String address, int port, UUID sessionID, ControlChannelNotifier notifier) throws Exception {
        this(InetAddress.getByName(address), port, sessionID, notifier);
    }

    /**
     * Try to connect to a remote FDT instance
     * 
     * @param address
     * @param port
     * @param fdtSessionID
     * @param notifier
     * @throws Exception
     */
    public ControlChannel(InetAddress inetAddress, int port, UUID fdtSessionID, ControlChannelNotifier notifier)
            throws Exception {
        try {
            this.notifier = notifier;
            this.fdtSessionID = fdtSessionID;

            if (config.isGSIModeEnabled()) {
                GSIGssSocketFactory factory = new GSIGssSocketFactory();
                controlSocket = factory.createSocket(inetAddress, config.getGSIPort(), false, false);
                this.subject = GSIGssSocketFactory.getLocalSubject(controlSocket);
            } else {
                controlSocket = new Socket();
                controlSocket.connect(new InetSocketAddress(inetAddress, port), CONNECT_TIMEOUT);
            }

            this.remoteAddress = inetAddress;
            this.remotePort = port;
            this.localPort = controlSocket.getLocalPort();

            controlSocket.setTcpNoDelay(true);

            // only the first octet will be interpreted by the AcceptTask at the other end
            if (!config.isGSIModeEnabled()) {
                controlSocket.getOutputStream().write(new byte[] { 0 });
            }

            // from now on only CtrlMsg will be sent
            initStreams();
            controlSocket.setSoTimeout(1000);

            //
        } catch (Throwable t) {
            close("Cannot instantiate ControlChannel", t);
            throw new Exception(t);
        }
    }

    public boolean isSocketClosed() {
        return (this.controlSocket == null) ? true : controlSocket.isClosed();
    }

    /**
     * A remote peer connected to FDT
     * 
     * @param s
     *            - the socket
     * @throws Exception
     *             - if anything goes wrong in intialization
     */
    public ControlChannel(Socket s, ControlChannelNotifier notifier) throws Exception {
        try {
            this.controlSocket = s;

            this.remoteAddress = s.getInetAddress();
            this.remotePort = s.getPort();
            this.localPort = s.getLocalPort();

            this.notifier = notifier;

            initStreams();
            controlSocket.setSoTimeout(1000);

        } catch (Throwable t) {
            close("Cannot instantiate ControlChannel", t);
            throw new Exception(t);
        }
    }

    /**
     * @param parent 
     * 
     */
    public ControlChannel(GSIServer parent, Socket s, Subject peerSubject, ControlChannelNotifier notifier)
            throws Exception {
        try {

            this.controlSocket = s;
            this.subject = peerSubject;
            this.remoteAddress = s.getInetAddress();
            this.remotePort = s.getPort();
            this.localPort = s.getLocalPort();

            this.notifier = notifier;

            initStreams();
            controlSocket.setSoTimeout(1000);

        } catch (Throwable t) {
            close("Cannot instantiate ControlChannel", t);
            throw new Exception(t);
        }
    }

    public UUID fdtSessionID() {
        return fdtSessionID;
    }

    @Override
    public String toString() {
        return (controlSocket == null) ? "null" : controlSocket.toString();
    }

    @SuppressWarnings("unchecked")
    private void initStreams() throws Exception {
        oos = new ObjectOutputStream(new BufferedOutputStream(controlSocket.getOutputStream()));

        // send the version

        sendMsgImpl(versionMsg);

        ois = new ObjectInputStream(new BufferedInputStream(controlSocket.getInputStream()));

        // wait for remote version
        CtrlMsg ctrlMsg = (CtrlMsg) ois.readObject();
        if (ctrlMsg.tag != CtrlMsg.PROTOCOL_VERSION) {
            throw new FDTProcolException("Unexpected remote control message. Expected PROTOCOL_VERSION tag [ "
                    + CtrlMsg.PROTOCOL_VERSION + " ] Received tag: " + ctrlMsg.tag);
        }

        this.fullRemoteVersion = (String) ctrlMsg.message;

        ctrlMsg = new CtrlMsg(CtrlMsg.INIT_FDT_CONF, Config.getInstance().getConfigMap());
        sendMsgImpl(ctrlMsg);

        // wait for remote config
        ctrlMsg = (CtrlMsg) ois.readObject();
        if (ctrlMsg.tag != CtrlMsg.INIT_FDT_CONF) {
            throw new FDTProcolException("Unexpected remote control message. Expected INIT_FDT_CONF tag [ "
                    + CtrlMsg.INIT_FDT_CONF + " ] Received tag: " + ctrlMsg.tag);
        }

        this.remoteConf = (HashMap<String, Object>) ctrlMsg.message;
        try {

            if (DirectByteBufferPool.initInstance(Integer.parseInt((String) remoteConf.get("-bs")),
                    config.getMaxTakePollIter())) {
                if (logger.isLoggable(Level.FINER)) {
                    logger.log(Level.FINER, "The buffer pool has been initialized");
                }
            } else {
                if (logger.isLoggable(Level.FINER)) {
                    logger.log(Level.FINER, "The buffer pool is already initialized");
                }
            }

        } catch (Throwable t) {
            throw new FDTProcolException("Unable to instantiate the buffer pool", t);
        }

        if (fdtSessionID == null) {// The remote peer should send my fdtSessionID

            ctrlMsg = (CtrlMsg) ois.readObject();
            if (ctrlMsg.tag == CtrlMsg.SESSION_ID) {
                fdtSessionID = (UUID) ctrlMsg.message;
            } else {
                throw new FDTProcolException("Unexpected remote control message. Expected SESSION_ID tag [ "
                        + CtrlMsg.SESSION_ID + " ] Received tag: " + ctrlMsg.tag);
            }
        } else {// I should send the ID to the remote peer

            sendMsgImpl(new CtrlMsg(CtrlMsg.SESSION_ID, fdtSessionID));
        }
        myName = " ControlThread for ( " + fdtSessionID + " ) " + controlSocket.getInetAddress() + ":"
                + controlSocket.getPort();
        logger.log(Level.INFO, "NEW CONTROL stream for " + fdtSessionID + " initialized ");

        final long localKA = Config.getInstance().getKeepAliveDelay(TimeUnit.NANOSECONDS);
        final String remoteKAS = (String) remoteConf.get("-ka");
        final long remoteKAN = (remoteKAS == null) ? localKA : TimeUnit.SECONDS.toNanos(Long.parseLong(remoteKAS));
        final long remoteKA = (remoteKAN < 0) ? localKA : remoteKAN;

        if ((this.fullRemoteVersion != null) && (Utils.compareVersions(this.fullRemoteVersion, "0.9.8") >= 0)) {
            synchronized (this.closeLock) {
                final FDTVersion localVersion = FDTVersion.fromVersionString(Config.FDT_FULL_VERSION + "-"
                        + Config.FDT_RELEASE_DATE);
                final FDTVersion remoteVersion = FDTVersion.fromVersionString(this.fullRemoteVersion);
                final long kaMinNanos = Math.min(localKA, remoteKA);
                final long kaMaxSeconds = TimeUnit.NANOSECONDS.toSeconds(kaMinNanos);
                final String strLog = ((kaMaxSeconds > 0) ? kaMaxSeconds + " second(s)" : TimeUnit.NANOSECONDS
                        .toMillis(kaMinNanos) + " millis");
                logger.log(Level.INFO, "App KeepAlive [ " + strLog + " ] enabled for control channel. Local "
                        + localVersion + ", Remote " + remoteVersion);
                ccptFuture = Utils.getMonitoringExecService().scheduleWithFixedDelay(
                        new ControlChannelPingerTask(this), kaMinNanos, kaMinNanos, TimeUnit.NANOSECONDS);
            }
        } else {
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "[ ControlChannel ] remote version " + fullRemoteVersion
                        + " does not support KEEP_ALIVE messages");
            }
        }
    }

    public String remoteVersion() {
        return fullRemoteVersion;
    }

    private final void cleanup() {
        if (cleanupFinished.compareAndSet(false, true)) {
            Utils.cancelFutureIgnoringException(ccptFuture, false);
            Utils.closeIgnoringExceptions(ois);
            Utils.closeIgnoringExceptions(oos);
            Utils.closeIgnoringExceptions(controlSocket);

            if (notifier != null) {
                try {
                    notifier.notifyCtrlSessionDown(this, downCause());
                } catch (Throwable _) {
                    //not interested
                }
            }
        }
    }

    public void sendCtrlMessage(final Object ctrlMsg) {

        if (ctrlMsg == null) {
            throw new NullPointerException("Control message cannot be null over the ControlChannel");
        }

        if (logger.isLoggable(Level.FINER)) {
            logger.log(Level.FINER, "[ CtrlChannel ] adding to send queue msg: " + ctrlMsg.toString());
            if (logger.isLoggable(Level.FINEST)) {
                // do a thread dump
                Thread.dumpStack();
            }
        }

        qToSend.add(ctrlMsg);

    }

    private void sendAllMsgs() throws Exception {
        for (;;) {
            final Object ctrlMsg = qToSend.poll();
            if (ctrlMsg == null) {
                break;
            }
            sendMsgImpl(ctrlMsg);
        }
    }

    private void sendMsgImpl(Object o) throws Exception {
        try {
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, " [ ControlChannel ] sending message " + o);
            }
            oos.writeObject(o);
            oos.reset();
            oos.flush();
        } catch (Throwable t) {
            if (!isClosed()) {
                close("Exception sending control data", t);
                throw new IOException(" Cannot send ctrl message ( " + t.getCause() + " ) ");
            }
        }
    }

    @Override
    public void run() {
        final BlockingQueue<Object> notifQueue = new ArrayBlockingQueue<Object>(10);

        // TODO - stupid hack; but gets stuck otherwise; no time to check in details ...
        final Thread iNotif = new Thread() {

            @Override
            public void run() {
                setName("INotifier for: " + myName);
                while ((controlSocket != null) && !controlSocket.isClosed()) {
                    try {
                        final Object toNotif = notifQueue.poll(1, TimeUnit.SECONDS);
                        if (toNotif == null) {
                            continue;
                        }
                        if (logger.isLoggable(Level.FINEST)) {
                            logger.log(Level.FINEST, "[ ControlChannel ] [ INotifier ] notifying msg: " + toNotif);
                        }
                        notifier.notifyCtrlMsg(ControlChannel.this, toNotif);
                    } catch (Throwable t) {
                        if (logger.isLoggable(Level.FINER)) {
                            StringBuilder sb = new StringBuilder();
                            sb.append("[ ControlChannel ] [ INotifier ] Got exception. ControlChannel isClosed(): ")
                                    .append(isClosed());
                            if (isClosed()) {
                                sb.append(" downMessage: ").append(downMessage()).append(" downCause: ")
                                        .append(Utils.getStackTrace(downCause()));
                            }
                            sb.append(" Inotifier Exception: ");
                            logger.log(Level.FINER, sb.toString(), t);
                        }
                        close("INotifier got exception ", t);
                        cleanup();
                    }
                }
            }
        };
        iNotif.setDaemon(true);
        iNotif.start();

        if (logger.isLoggable(Level.FINER)) {
            logger.log(Level.FINER, myName + " STARTED main loop");
        }

        String internalDownMsg = null;
        Throwable internalDownCause = null;

        try {

            while ((controlSocket != null) && !controlSocket.isClosed()) {
                try {
                    sendAllMsgs();
                    Object o = ois.readObject();
                    if (o == null) {
                        continue;
                    }
                    final boolean isFine = logger.isLoggable(Level.FINE);
                    if (isFine) {
                        logger.log(Level.FINE, " [ ControlChannel ] received msg: " + o);
                    }

                    if (o instanceof CtrlMsg) {
                        final CtrlMsg ctrlMsg = (CtrlMsg) o;
                        // ping - like for the socket
                        if (ctrlMsg.tag == CtrlMsg.KEEP_ALIVE_MSG) {
                            if (isFine) {
                                logger.log(Level.FINE, "Ctrl channel received app KEEP_ALIVE_MSG");
                            }
                            continue;
                        }

                        if (ctrlMsg.tag == CtrlMsg.END_SESSION_FIN2) {
                            if (!isClosed()) {
                                final String errMsg = "Remote site will close the transfer session; FINAL timeout was reached. "
                                        + "Most likely the TCP buffers on remote site are higher than normal. Try the blocking I/O -bio on both sides and no -ss.";
                                logger.log(Level.WARNING, errMsg);
                                close(errMsg, null);
                            }
                            break;
                        }
                    }

                    notifQueue.add(o);
                } catch (SocketTimeoutException ste) {
                    // ignore this??? or shall I close it() ?
                } catch (IOException ioe) {
                    close("Control channel got I/O Exception", ioe);
                    cleanup();
                } catch (Throwable t) {
                    t.printStackTrace();
                    close("Control channel got general exception. Will close!", t);
                    cleanup();
                }
            }// main loop

        } catch (Throwable t) {
            if (!isClosed()) {

                internalDownMsg = myName + " got exception in main loop: " + t.getMessage();
                internalDownCause = t;

                if (logger.isLoggable(Level.FINER)) {
                    logger.log(Level.FINER, "Control Thread for " + myName + " got exception in main loop", t);
                }
            }
        } finally {
            if ((downMessage() != null) || (downCause() != null)) {
                close(downMessage(), downCause());
            } else {
                close(internalDownMsg, internalDownCause);
            }
        }

        logger.log(Level.INFO, myName + " FINISHED");
    }

    @Override
    protected void internalClose() {

        try {
            final Thread t = new Thread() {

                @Override
                public void run() {
                    setName("(ML) ControlChannel Graceful stopper thread");

                    try {
                        int retry = 0;

                        while (retry++ < 3) {
                            try {
                                Thread.sleep(1 * 1000);
                            } catch (Throwable _) {
                                //if we're interruped - tough luck
                            }

                            try {
                                if ((controlSocket == null) || controlSocket.isClosed()) {
                                    break;
                                }
                                qToSend.add(new CtrlMsg(CtrlMsg.END_SESSION_FIN2, downMessage()
                                        + Utils.getStackTrace(downCause())));
                            } catch (Throwable _) {
                                //if we're interruped - tough luck
                            }

                        }// end while

                    } finally {
                        cleanup();
                    }
                }
            };

            // if main is out ... please let me die like a real thread :)
            t.setDaemon(true);
            t.start();
        } catch (Throwable _) {
            // smth went dreadfully wrong ... just close the session now!
            try {
                cleanup();
            } catch (Throwable exc) {
                logger.log(Level.WARNING, "Exception in cleanup()", exc);
            }
        }
    }
}
