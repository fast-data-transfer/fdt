package lia.util.net.copy.transport;

import java.net.InetAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import lia.util.net.common.Config;
import lia.util.net.common.Utils;
import lia.util.net.copy.FDTReaderSession;
import lia.util.net.copy.transport.internal.FDTSelectionKey;
import lia.util.net.copy.transport.internal.SelectionManager;

public class TCPSessionWriter extends TCPTransportProvider {
    
    private static final Logger logger = Logger.getLogger(TCPSessionReader.class.getName());
    private static final SelectionManager selectionManager = SelectionManager.getInstance();
    private static final Config config = Config.getInstance();
    
    public TCPSessionWriter(FDTReaderSession fdtSession) throws Exception {
        super(fdtSession);
        selectionQueue = new PriorityBlockingQueue<FDTSelectionKey>(10, new FDTWriterKeyAttachementComparator());
    }
    
    public TCPSessionWriter(FDTReaderSession fdtSession,
            InetAddress endPointAddress, int port,
            int numberOfStreams) throws Exception {
        
        super(fdtSession, endPointAddress, port, numberOfStreams);
        selectionQueue = new PriorityBlockingQueue<FDTSelectionKey>(10, new FDTWriterKeyAttachementComparator());
    }
    
    public void notifyAvailableBytes(final long available) {
        speedLimitLock.lock();
        try {
            availableBytes = available;
            isAvailable.signalAll();
        }finally{
            speedLimitLock.unlock();
        }
    }
    
    public long awaitSend(final long bytesNo) throws InterruptedException {
        long avForWrite = 0;
        speedLimitLock.lock();
        try {
            while(avForWrite == 0 && !isClosed()) {
                if(availableBytes > 0) {
                    final long remainingBytes = availableBytes - bytesNo;
                    
                    if(remainingBytes >= 0) {
                        availableBytes = remainingBytes;
                        avForWrite = bytesNo;
                        break;
                    }
                    
                    avForWrite = availableBytes;
                    availableBytes = 0;
                } else {
                    isAvailable.await(4, TimeUnit.SECONDS);
                }
            }
        } finally {
            speedLimitLock.unlock();
        }
        
        return avForWrite;
    }
    
    public void addWorkerStream(SocketChannel sc) throws Exception {
        synchronized(this.channels) {
            super.addWorkerStream(sc);
            FDTSelectionKey fsk = null;
            
            if(config.isBlocking()) {
                fsk = new FDTSelectionKey(fdtSession.sessionID(), sc, SelectionKey.OP_WRITE, this);
                fsk.attach(new FDTWriterKeyAttachement(fsk, fdtSession.useFixedBlockSize()));
                initExecutor();
                executor.submit(new SocketWriterTask(selectionQueue, (FDTReaderSession)fdtSession, this));
                if(logger.isLoggable(Level.FINEST)) {
                    logger.log(Level.FINEST, " BIO Mode. Adding SocketChannel " + sc + " to the selection queue");
                }
                selectionQueue.add(fsk);
            } else {
                if(logger.isLoggable(Level.FINEST)) {
                    logger.log(Level.FINEST, " NBIO Mode. Adding SocketChannel " + sc + " to the SelectionManager! ");
                }
                fsk = selectionManager.register(fdtSession.sessionID(), sc, SelectionKey.OP_WRITE, this);
                fsk.attach(new FDTWriterKeyAttachement(fsk, fdtSession.useFixedBlockSize()));
                if(!fsk.registerInterest()) {
                    logger.log(Level.WARNING, " \n\n Smth went terrible wrong ?? \n\n fsk.registerInterest() returned false \n\n");
                }
            }
            
            channels.put(sc, fsk);
        }
    }
    
    public void startTransport() throws Exception {
        super.startTransport();
        if(!config.isBlocking()) {
            for(int i =0; i <= Utils.availableProcessors()*2; i++) {
                executor.submit(new SocketWriterTask(selectionQueue, (FDTReaderSession)fdtSession, this));
            }
        }
    }
    
    
    
    
    
    public void workerDown(FDTSelectionKey fdtSelectionKey, Throwable downCause) {
        
        
        




        if(fdtSession != null) {
            try {
                ((FDTReaderSession)fdtSession).transportWorkerDown();
            }catch(Throwable ignore){}
        }
        
        close("Worker down", downCause);
    }
    
}
