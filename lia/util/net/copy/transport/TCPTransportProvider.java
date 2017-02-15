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
import lia.util.net.common.Utils;
import lia.util.net.copy.FDTSession;
import lia.util.net.copy.monitoring.NetSessionMonitoringTask;
import lia.util.net.copy.transport.internal.FDTSelectionKey;
import lia.util.net.copy.transport.internal.SelectionHandler;
import lia.util.net.copy.transport.internal.SelectionManager;

public abstract class TCPTransportProvider extends AbstractFDTIOEntity implements SelectionHandler, SpeedLimiter {
    
    private static final Logger logger = Logger.getLogger(TCPTransportProvider.class.getName());
    private static final Config config = Config.getInstance();
    
    protected static final SelectionManager selectionManager = SelectionManager.getInstance();
    
    protected final Lock speedLimitLock = new ReentrantLock(true);
    protected final Condition isAvailable = speedLimitLock.newCondition();
    protected long availableBytes;
    
    protected HashMap<SocketChannel, FDTSelectionKey> channels;
    protected FDTSession fdtSession;
    protected ExecutorService executor;
    protected Object execLock = new Object();
    
    protected ArrayList<SocketTask> socketTasks = new ArrayList<SocketTask>();
    
    protected BlockingQueue<FDTSelectionKey> selectionQueue = new LinkedBlockingQueue<FDTSelectionKey>();
    
    protected InetAddress endPointAddress;
    protected int port;
    protected int numberOfStreams;
    public NetSessionMonitoringTask monitoringTask;
    
    ScheduledFuture<?> monitoringTaskFuture;
    ScheduledFuture<?> limiterTask;
    
    public TCPTransportProvider(FDTSession fdtSession) throws Exception {
        this.channels = new HashMap<SocketChannel, FDTSelectionKey>();
        this.fdtSession = fdtSession;
        if(!isClosed()) {
            this.monitoringTask = new NetSessionMonitoringTask(this);
            ScheduledExecutorService monitoringService = Utils.getMonitoringExecService();
            monitoringTaskFuture = monitoringService.scheduleWithFixedDelay(monitoringTask, 1, 5, TimeUnit.SECONDS);
            limiterTask = SpeedLimitManager.getInstance().addLimiter(this);
        }
    }
    
    public final boolean useFixedBlockSize() {
        return fdtSession.useFixedBlockSize();
    }
    
    public final boolean localLoop() {
        return fdtSession.localLoop();
    }
    
    public long getNotifyDelay() {
        return 1000;
    }
    
    public void notifyAvailableBytes(long available) {
        
    }
    
    public final long getRateLimit() {
        return fdtSession.getRateLimit();
    }
    
    private static final List<SocketChannel>  tryToConnect(InetSocketAddress addr,
            int numberOfStreams, ByteBuffer connectCookie) throws Exception {
        
        if(addr == null) throw new NullPointerException("Address cannot be null");
        if(numberOfStreams <= 0) throw new IllegalArgumentException("Number of streams must be > 0 ( " + numberOfStreams + ")");
        
        ArrayList<SocketChannel> tmpChannels = new ArrayList<SocketChannel>();
        
        
        Selector tmpSelector = null;
        ArrayList<SocketChannel> connectedChannels = new ArrayList<SocketChannel>();
        
        try {
            int windowSize = config.getSockBufSize();
            boolean noDelay = config.isNagleEnabled();
            int usedWindowSize = -1;
            
            tmpSelector = Selector.open();
            
            for (int i = 0; i < numberOfStreams; i++) {
                
                SocketChannel sc = SocketChannel.open();
                
                sc.configureBlocking(config.isBlocking());
                
                sc.connect(addr);
                tmpChannels.add(sc);
                
                if(windowSize > 0) {
                    sc.socket().setSendBufferSize(windowSize);
                }
                
                usedWindowSize = sc.socket().getSendBufferSize();
                
                if (noDelay) {
                    sc.socket().setTcpNoDelay(true);
                } else {
                    sc.socket().setTcpNoDelay(false);
                }
                
                try {
                    sc.socket().setKeepAlive(true);
                }catch(Throwable t) {
                    logger.log(Level.WARNING, " Cannot set KEEP_ALIVE", t);
                }
                
                if(!sc.isBlocking()) {
                    sc.register(tmpSelector, SelectionKey.OP_CONNECT | SelectionKey.OP_WRITE);
                } else {
                    sc.finishConnect();
                    if(connectCookie != null) {
                        if(sc.write(connectCookie) >= 0 && !connectCookie.hasRemaining()) {
                            connectedChannels.add(sc);
                        } else {
                            throw new IOException("Cannot connect");
                        }
                        connectCookie.flip();
                    }
                }
                
            }
            
            logger.log(Level.INFO, "Requested window size " + windowSize +". Using window size: " + usedWindowSize);
            
            
            int i=0;
            
            
            while(tmpChannels.size() != connectedChannels.size()) {
                int n = tmpSelector.select();
                
                if(n == 0) continue;
                
                Set<SelectionKey> selectedKeys = tmpSelector.selectedKeys();
                
                for(Iterator<SelectionKey> it = selectedKeys.iterator(); it.hasNext(); ) {
                    SelectionKey ssk = it.next();
                    it.remove();
                    SocketChannel sc = (SocketChannel)ssk.channel();
                    if(ssk.isConnectable()) {
                        ssk.interestOps(ssk.interestOps() & ~SelectionKey.OP_CONNECT);
                        while(!sc.finishConnect()) {
                            System.out.println("Socket not yet connected!!!");
                            Thread.yield();
                        }
                    } else {
                        ssk.interestOps(ssk.interestOps() & ~SelectionKey.OP_WRITE);
                        if(connectCookie != null) {
                            while(sc.write(connectCookie) >= 0 && connectCookie.hasRemaining()) {
                                Thread.yield();
                            }
                            connectCookie.flip();
                        }
                        i++;
                        if(logger.isLoggable(Level.FINE)) {
                            logger.log(Level.FINE, "Connection ( " + i + " ) established [ " + sc.socket().getLocalAddress() + ":" + sc.socket().getLocalPort() + " -> " + sc.socket().getInetAddress() + ":" + sc.socket().getPort() + " ]");
                        }
                        
                        connectedChannels.add(sc);
                    }
                }
            }
            
        } catch(Throwable t) {
            logger.log(Level.WARNING, "Unable to connect to " + addr.toString(), t);
            for(SocketChannel sc: tmpChannels) {
                try {
                    sc.close();
                }catch(Throwable t1){
                    
                }
            }
            
            throw new Exception(t);
        } finally {
            if(tmpSelector != null) {
                try {
                    tmpSelector.close();
                }catch(Throwable ingnore) {                   
                }
            }
        }
        
        return tmpChannels;
    }
    
    public TCPTransportProvider(FDTSession fdtSession,
            InetAddress endPointAddress, int port,
            int numberOfStreams) throws Exception {
        this(fdtSession);

        
        this.endPointAddress = endPointAddress;
        this.port = port;
        this.numberOfStreams = numberOfStreams;
    }
    
    public int getNumberOfStreams() {
        synchronized(channels) {
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
            while(fsk != null) {
                if(!fsk.equals(FDTSelectionKey.END_PROCESSING_NOTIF_KEY)) {
                    Object attachment = fsk.attachment();
                    if(attachment != null && attachment instanceof FDTKeyAttachement) {
                        FDTKeyAttachement fattach = (FDTKeyAttachement)attachment;
                        fattach.recycleBuffers();
                    }
                }
                fsk = selectionQueue.poll();
            }
            
            selectionQueue.offer(FDTSelectionKey.END_PROCESSING_NOTIF_KEY);
        }catch(Throwable t) {
            logger.log(Level.WARNING, "Got exception", t);
        }
    }
    
    protected void internalClose() {
        
        if(limiterTask != null) {
            limiterTask.cancel(true);
        }
        
        if(monitoringTaskFuture != null) {
            monitoringTaskFuture.cancel(false);
        }
        
        if(monitoringTask != null) {
            ScheduledThreadPoolExecutor monitoringService = Utils.getMonitoringExecService();
            monitoringService.remove(monitoringTask);
            monitoringService.purge();
        }
        
        if(executor != null) {
            executor.shutdown();
        }
        
        if(fdtSession != null) {
            fdtSession.close(downMessage(), downCause());
        }
        
        int cSize = 1;
        
        if(channels != null) {
            cSize = channels.size();
            synchronized(channels) {
                for(Map.Entry<SocketChannel, FDTSelectionKey> entry: channels.entrySet()) {
                    try {
                        entry.getKey().close();
                    }catch(Throwable t){
                        
                    }
                    
                    try {
                        entry.getValue().cancel();
                    }catch(Throwable t){
                        
                    }
                }
                channels.clear();
            }
        }
        
        if(socketTasks != null) {
            synchronized(socketTasks) {
                for(SocketTask st: socketTasks) {
                    try {
                        st.close(downMessage(), downCause());
                    }catch(Throwable ignore){}
                }
                
                socketTasks.clear();
            }
        }
        
        if(executor != null) {
            for( ;; ) {
                
                clearSelectionQueue();
                if(executor.isTerminated()) {
                    clearSelectionQueue();
                    break;
                }
                
                if(logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, "TCPTrasportProvider waiting for TCPWorkers tasks to finish");
                }
                try {
                    closeLock.wait(1 * 1000);
                }catch(Throwable t1){}
                
            }
        } else {
            clearSelectionQueue();
        }
        
    }
    
    protected boolean addSocketTask(SocketTask socketTask) {
        synchronized(socketTasks) {
            if(isClosed()) {
                socketTask.close(downMessage(), downCause());
                return false;
            }
            return socketTasks.add(socketTask);
        }
    }
    
    public void startTransport() throws Exception {
        if(endPointAddress != null) {
            InetSocketAddress addr = new InetSocketAddress(endPointAddress, port);
            
            
            ByteBuffer connectCookie = ByteBuffer.allocate(1 + 128);
            connectCookie.put((byte)1).putLong(fdtSession.sessionID().getMostSignificantBits()).putLong(fdtSession.sessionID().getLeastSignificantBits()).flip();
            
            addChannels(tryToConnect(addr, numberOfStreams, connectCookie));
        }
        
        initExecutor();
    }
    
    protected void initExecutor() {
        synchronized(execLock) {
            if(executor == null) {
                executor = Utils.getStandardExecService(
                        " TCPTransportProvider task executor for FDTSession ( " + fdtSession.sessionID() + " )",
                        Utils.availableProcessors(), 4096, Thread.MAX_PRIORITY - 2);
            }
        }
    }
    public void addChannels(List<SocketChannel> channels) throws Exception {
        if(isClosed()) throw new FDTProcolException("The transport provider is down");
        synchronized(this.channels) {
            for(SocketChannel sc: channels) {
                addWorkerStream(sc);
            }
        }
    }
    
    public void handleSelection(FDTSelectionKey fdtSelectionKey) {
        if(logger.isLoggable(Level.FINEST)) {
            logger.log(Level.FINEST, "    [ SELECTION ] [ NBIO ] handle selection for " + fdtSelectionKey);
        }
        selectionQueue.add(fdtSelectionKey);
    }
    
    
    public void canceled(FDTSelectionKey fdtSelectionKey) {
        
        if(logger.isLoggable(Level.FINER)) {
            logger.log(Level.FINER, "  [ SELECTION ] [ NBIO ] Canceled event for " + fdtSelectionKey);
        }
        
        SocketChannel sc = fdtSelectionKey.channel();
        try {
            sc.close();
        }catch(Throwable ignore) {
            if(logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, " Got exception closing socket " + sc, ignore);
            }
        }
        
        FDTKeyAttachement attachement = (FDTKeyAttachement)fdtSelectionKey.attachment();
        if(attachement != null) {
            attachement.recycleBuffers();
        } else {
            logger.log(Level.WARNING, " Null attachement for fdtSelectionKey: " + fdtSelectionKey + " sc: " + sc);
        }
    }
    
    public void addWorkerStream(SocketChannel channel) throws Exception {
        if(isClosed()) throw new FDTProcolException("The transport provider is down");
        synchronized(this.channels) {
            if(logger.isLoggable(Level.FINER)) {
                logger.log(Level.FINER, " TCPTransportProvider add working stream for channel: " + channel);
            }
            this.channels.put(channel, null);
        }
    }
    
}