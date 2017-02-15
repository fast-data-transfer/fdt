/*
 * $Id: TCPTransportProvider.java 567 2010-01-28 06:06:01Z ramiro $
 */
package lia.util.net.copy.transport;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import lia.util.net.common.AbstractFDTIOEntity;
import lia.util.net.common.Config;
import lia.util.net.common.DirectByteBufferPool;
import lia.util.net.common.Utils;
import lia.util.net.copy.FDTSession;
import lia.util.net.copy.monitoring.NetSessionMonitoringTask;
import lia.util.net.copy.transport.internal.FDTSelectionKey;
import lia.util.net.copy.transport.internal.SelectionHandler;
import lia.util.net.copy.transport.internal.SelectionManager;

/**
 * The base class for all read/write over the wire, and also has the "SpeedLimiter" incorporated, or not :)
 * 
 * @author ramiro
 */
public abstract class TCPTransportProvider extends AbstractFDTIOEntity implements SelectionHandler, SpeedLimiter {

    private static final Logger logger = Logger.getLogger(TCPTransportProvider.class.getName());

    private static final Config config = Config.getInstance();

    protected static final SelectionManager selectionManager = SelectionManager.getInstance();

    protected final Lock speedLimitLock = new ReentrantLock(true);

    protected final Condition isAvailable = speedLimitLock.newCondition();

    protected long availableBytes;

    // GuardedBy - this.closeLock
    protected final HashMap<SocketChannel, FDTSelectionKey> channels = new HashMap<SocketChannel, FDTSelectionKey>();

    protected final FDTSession fdtSession;

    protected final ExecutorService executor;

    protected final ArrayList<SocketTask> socketTasks = new ArrayList<SocketTask>();

    protected final BlockingQueue<FDTSelectionKey> selectionQueue;

    protected InetAddress endPointAddress;

    protected int port;

    protected int numberOfStreams;

    public NetSessionMonitoringTask monitoringTask;

    ScheduledFuture<?> monitoringTaskFuture;

    ScheduledFuture<?> limiterTask;

    public TCPTransportProvider(FDTSession fdtSession) throws Exception {
        this(fdtSession, new LinkedBlockingQueue<FDTSelectionKey>());
    }

    public TCPTransportProvider(FDTSession fdtSession, BlockingQueue<FDTSelectionKey> selectionQueue) throws Exception {
        this.fdtSession = fdtSession;
        this.selectionQueue = selectionQueue;
        executor = Utils.getStandardExecService(" TCPTransportProvider task executor for FDTSession ( " + fdtSession.sessionID() + " )", Utils.availableProcessors(), 4096, Thread.MAX_PRIORITY - 2);
        if (!isClosed()) {
            limiterTask = SpeedLimitManager.getInstance().addLimiter(this);
        }
    }

    public TCPTransportProvider(FDTSession fdtSession, InetAddress endPointAddress, int port, int numberOfStreams) throws Exception {
        this(fdtSession, endPointAddress, port, numberOfStreams, new LinkedBlockingQueue<FDTSelectionKey>());
    }

    public TCPTransportProvider(FDTSession fdtSession, InetAddress endPointAddress, int port, int numberOfStreams, BlockingQueue<FDTSelectionKey> selectionQueue) throws Exception {
        this(fdtSession, selectionQueue);
        this.endPointAddress = endPointAddress;
        this.port = port;
        this.numberOfStreams = numberOfStreams;
    }

    public final boolean useFixedBlockSize() {
        return fdtSession.useFixedBlockSize();
    }

    public final boolean localLoop() {
        return fdtSession.localLoop();
    }

    public final boolean isNetTest() {
        return fdtSession.isNetTest();
    }

    public long getSize() {
        return -1;
    }

    public long getNotifyDelay() {
        return 1 * 1000;
    }

    public void notifyAvailableBytes(long available) {
    }

    public final long getRateLimit() {
        return fdtSession.getRateLimit();
    }

    private static final List<SocketChannel> tryToConnect(InetSocketAddress addr, int numberOfStreams, ByteBuffer connectCookie, final boolean sendCookie) throws Exception {

        if (addr == null) {
            throw new NullPointerException("Address cannot be null");
        }
        if (numberOfStreams <= 0) {
            throw new IllegalArgumentException("Number of streams must be > 0 ( " + numberOfStreams + ")");
        }
        ArrayList<SocketChannel> tmpChannels = new ArrayList<SocketChannel>();

        // use a temporary selector to wait for sockets to finish their connect()
        Selector tmpSelector = null;
        ArrayList<SocketChannel> connectedChannels = new ArrayList<SocketChannel>();

        try {
            int windowSize = config.getSockBufSize();
            boolean noDelay = config.isNagleEnabled();
            int usedWindowSize = -1;

            tmpSelector = Selector.open();

            final int bSockConn = config.getBulkSockConnect();
            final long bSockConnWait = config.getBulkSockConnectWait();

            logger.log(Level.INFO, " bSockConn: " + bSockConn + " bSockConnWait: " + bSockConnWait);
            int cCounter = 0;
            for (int i = 0; i < numberOfStreams; i++) {

                if (bSockConn > 0 && cCounter > bSockConn) {
                    try {
                        final long sTime = (bSockConnWait > 100) ? bSockConnWait : 1000;
                        logger.log(Level.INFO, " connected " + i + " sockets. sleeping " + sTime + " ms");
                        Thread.sleep(sTime);
                    } catch (Throwable t) {
                        logger.log(Level.WARNING, " Exception trying to wait for bulk connections", t);
                    }
                    cCounter = 0;
                }

                cCounter++;
                final SocketChannel sc = SocketChannel.open();

                sc.configureBlocking(config.isBlocking());

                sc.connect(addr);
                tmpChannels.add(sc);

                if (windowSize > 0) {
                    sc.socket().setSendBufferSize(windowSize);
                }

                if (!sc.isBlocking()) {
                    sc.register(tmpSelector, SelectionKey.OP_CONNECT | SelectionKey.OP_WRITE);
                } else {
                    sc.finishConnect();

                    usedWindowSize = sc.socket().getSendBufferSize();

                    if (!sendCookie) {
                        connectedChannels.add(sc);
                    } else {
                        if (connectCookie != null) {
                            if (sc.write(connectCookie) >= 0 && !connectCookie.hasRemaining()) {
                                connectedChannels.add(sc);
                            } else {
                                throw new IOException("Cannot connect");
                            }
                            connectCookie.flip();
                        }
                    }
                }
            }

            int i = 0;

            // CONNECT
            while (tmpChannels.size() != connectedChannels.size()) {
                int n = tmpSelector.select();

                if (n == 0) {
                    continue;
                }
                Set<SelectionKey> selectedKeys = tmpSelector.selectedKeys();

                for (Iterator<SelectionKey> it = selectedKeys.iterator(); it.hasNext();) {
                    SelectionKey ssk = it.next();
                    it.remove();
                    SocketChannel sc = (SocketChannel) ssk.channel();
                    if (ssk.isConnectable()) {
                        ssk.interestOps(ssk.interestOps() & ~SelectionKey.OP_CONNECT);
                        while (!sc.finishConnect()) {
                            System.out.println("Socket not yet connected!!!");
                            Thread.yield();
                        }
                    } else {
                        try {
                            if (noDelay) {
                                sc.socket().setTcpNoDelay(true);
                            }
                        } catch (Throwable t) {
                            logger.log(Level.WARNING, " Cannot enable/disable Nagle's alg", t);
                        }

                        try {
                            sc.socket().setKeepAlive(true);
                        } catch (Throwable t) {
                            logger.log(Level.WARNING, " Cannot set KEEP_ALIVE", t);
                        }

                        usedWindowSize = sc.socket().getSendBufferSize();

                        ssk.interestOps(ssk.interestOps() & ~SelectionKey.OP_WRITE);
                        if (sendCookie && connectCookie != null) {
                            while (sc.write(connectCookie) >= 0 && connectCookie.hasRemaining()) {
                                Thread.yield();
                            }
                            connectCookie.flip();
                        }
                        i++;
                        if (logger.isLoggable(Level.FINE)) {
                            logger.log(Level.FINE, "Connection ( " + i + " ) established [ " + sc.socket().getLocalAddress() + ":" + sc.socket().getLocalPort() + " -> " + sc.socket().getInetAddress() + ":" + sc.socket().getPort() + " ]");
                        }

                        connectedChannels.add(sc);
                    }
                }
            }

            logger.log(Level.INFO, "Requested window size " + windowSize + ". Using window size: " + usedWindowSize);
        } catch (Throwable t) {
            logger.log(Level.WARNING, "Unable to connect to " + addr.toString(), t);
            for (SocketChannel sc : tmpChannels) {
                try {
                    sc.close();
                } catch (Throwable t1) {
                    // ignore
                }
            }

            throw new Exception(t);
        } finally {
            if (tmpSelector != null) {
                try {
                    tmpSelector.close();
                } catch (Throwable ingnore) { // ignore
                }
            }
        }

        return tmpChannels;
    }

    public int getNumberOfStreams() {
        synchronized (this.closeLock) {
            return channels.size();
        }
    }

    public InetAddress getRemoteEndPointAddress() {
        return null;
    }

    public FDTSession getSession() {
        return fdtSession;
    }

    private final void clearSelectionQueue() {
        try {
            FDTSelectionKey fsk = selectionQueue.poll();
            while (fsk != null) {
                FDTKeyAttachement attachment = fsk.attachment();
                if (attachment != null) {
                    attachment.recycleBuffers();
                }
                fsk = selectionQueue.poll();
            }
        } catch (Throwable t) {
            logger.log(Level.WARNING, "Got exception", t);
        }
    }

    protected void internalClose() {

        if (limiterTask != null) {
            limiterTask.cancel(true);
        }

        if (monitoringTaskFuture != null) {
            monitoringTaskFuture.cancel(false);
        }

        if (monitoringTask != null) {
            ScheduledThreadPoolExecutor monitoringService = Utils.getMonitoringExecService();
            monitoringService.remove(monitoringTask);
            monitoringService.purge();
        }

        // be nice ... will be more aggressive a little bit later
        if (executor != null) {
            executor.shutdown();
        }

        if (fdtSession != null) {
            fdtSession.close(downMessage(), downCause());
        }

        for (Map.Entry<SocketChannel, FDTSelectionKey> entry : channels.entrySet()) {
            final SocketChannel sc = entry.getKey();
            final FDTSelectionKey fdtSelKey = entry.getValue();

            try {
                final FDTKeyAttachement attach = fdtSelKey.attachment();
                if (attach != null) {
                    attach.recycleBuffers();
                }
            } catch (Throwable t) {
                // ignore closing exceptions ...
            }

            try {
                sc.close();
            } catch (Throwable t) {
                // ignore closing exceptions ...
            }

            try {
                fdtSelKey.cancel();
            } catch (Throwable t) {
                // ignore closing exceptions ...
            }

        }
        channels.clear();

        synchronized (socketTasks) {
            for (SocketTask st : socketTasks) {
                try {
                    st.close(downMessage(), downCause());
                } catch (Throwable ignore) {
                }
            }

            socketTasks.clear();
        }

        clearSelectionQueue();
    }

    protected boolean addSocketTask(SocketTask socketTask) {
        synchronized (socketTasks) {
            if (isClosed()) {
                socketTask.close(downMessage(), downCause());
                return false;
            }
            return socketTasks.add(socketTask);
        }
    }

    public void startTransport(boolean sendCookie) throws Exception {
        if (endPointAddress != null) {
            InetSocketAddress addr = new InetSocketAddress(endPointAddress, port);

            ByteBuffer connectCookie = null;
            DirectByteBufferPool instance = DirectByteBufferPool.getInstance();
            try {
                connectCookie = instance.take();
                connectCookie.limit(1 + 16);
                connectCookie.put((byte) 1).putLong(fdtSession.sessionID().getMostSignificantBits()).putLong(fdtSession.sessionID().getLeastSignificantBits());
                connectCookie.flip();
                addChannels(tryToConnect(addr, numberOfStreams, connectCookie, sendCookie), sendCookie);
            } finally {
                if (connectCookie != null) {
                    instance.put(connectCookie);
                }
            }
        }

    }

    public void addChannels(List<SocketChannel> channels, boolean sentCookie) throws Exception {
        if (isClosed()) {
            throw new FDTProcolException("The transport provider is down");
        }
        synchronized (this.closeLock) {
            for (final SocketChannel sc : channels) {
                addWorkerStream(sc, sentCookie);
            }
        }
    }

    public void handleSelection(FDTSelectionKey fdtSelectionKey) {
        if (logger.isLoggable(Level.FINEST)) {
            logger.log(Level.FINEST, " [ TCPTransportProvider ]  [ SELECTION ] [ NBIO ] handle selection for " + Utils.toStringSelectionKey(fdtSelectionKey));
        }
        selectionQueue.add(fdtSelectionKey);
    }

    public void canceled(FDTSelectionKey fdtSelectionKey) {

        if (logger.isLoggable(Level.FINER)) {
            logger.log(Level.FINER, "  [ SELECTION ] [ NBIO ] Canceled event for " + fdtSelectionKey);
        }

        SocketChannel sc = fdtSelectionKey.channel();
        try {
            sc.close();
        } catch (Throwable ignore) {
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, " Got exception closing socket " + sc, ignore);
            }
        }

        FDTKeyAttachement attachement = fdtSelectionKey.attachment();
        if (attachement != null) {
            attachement.recycleBuffers();
        } else {
            logger.log(Level.WARNING, " Null attachement for fdtSelectionKey: " + fdtSelectionKey + " sc: " + sc);
        }
    }

    public void addWorkerStream(SocketChannel channel, boolean sentCookie) throws Exception {
        final ScheduledExecutorService monitoringService = Utils.getMonitoringExecService();
        synchronized (closeLock) {
            if (monitoringTaskFuture == null && !closed) {
                this.monitoringTask = new NetSessionMonitoringTask(this);
                monitoringTaskFuture = monitoringService.scheduleWithFixedDelay(monitoringTask, 1, 1, TimeUnit.SECONDS);
            }

            if (logger.isLoggable(Level.FINER)) {
                logger.log(Level.FINER, " TCPTransportProvider add working stream for channel: " + channel);
            }

            this.channels.put(channel, null);
        }
    }
}
