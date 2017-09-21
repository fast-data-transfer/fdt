/*
 * $Id$
 */

package lia.util.net.copy.transport;

import lia.util.net.common.Config;
import lia.util.net.common.DirectByteBufferPool;
import lia.util.net.common.Utils;
import lia.util.net.copy.FileBlock;
import lia.util.net.copy.FileBlockConsumer;
import lia.util.net.copy.transport.internal.FDTSelectionKey;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The one and (not the) only reader task for a channel
 *
 * @author ramiro
 */
public class SocketReaderTask extends SocketTask {

    private static final Logger logger = Logger.getLogger(SocketReaderTask.class.getName());

    private static final int RETRY_IO_COUNT = Config.getInstance().getRetryIOCount();

    final AtomicReference<FDTSelectionKey> fdtSelectionKeyRef = new AtomicReference<FDTSelectionKey>();

    private final TCPSessionReader master;

    private final FileBlockConsumer fileBlockConsumer;

    private final boolean isNetTest;

    SocketReaderTask(BlockingQueue<FDTSelectionKey> readyChannelsQueue, FileBlockConsumer fileBlockConsumer, TCPSessionReader master) {
        super(readyChannelsQueue);
        this.fileBlockConsumer = fileBlockConsumer;
        this.master = master;
        isNetTest = master.isNetTest();
    }

    private boolean setAttachementBuffers(FDTReaderKeyAttachement attach) throws InterruptedException {
        return attach.recycleAndSetBuffers();
    }

    private final boolean checkForData() throws InterruptedException {
        final FDTSelectionKey fdtSelectionKey = fdtSelectionKeyRef.get();
        final FDTReaderKeyAttachement attach = (FDTReaderKeyAttachement) fdtSelectionKey.attachment();

        if (attach.isHeaderRead() && attach.isPayloadRead()) {// everything has been read
            fdtSelectionKey.opCount = 0;

            addAndGetUtilBytes(attach.payloadSize);
            master.addAndGetUtilBytes(attach.payloadSize);

            if (isNetTest) {
                attach.header().clear();
                return false;
            }

            if (!master.localLoop()) {
                final FileBlock fileBlock = attach.toFileBlock();
                if (logger.isLoggable(Level.FINER)) {
                    logger.log(Level.FINER, "<SocketReaderTask> read a full FileBlock for: " + Utils.buffToString(fileBlock.buff));
                }

                boolean offered = false;
                try {
                    for (; ; ) {
                        offered = fileBlockConsumer.offer(fileBlock, 5, TimeUnit.SECONDS);
                        if (offered)
                            break;
                        if (isClosed() || Thread.currentThread().isInterrupted())
                            break;
                    }
                } finally {
                    if (!offered) {
                        if (!offered && fileBlock != null && fileBlock.buff != null) {
                            DirectByteBufferPool.getInstance().put(fileBlock.buff);
                        }
                        recycleBuffers();
                        return false;
                    }
                }
            } else {
                attach.recyclePaylod();
            }

            attach.recycleHeader();
            return true;
        }

        return false;
    }

    private boolean readData() throws Exception {

        final FDTSelectionKey fdtSelectionKey = fdtSelectionKeyRef.get();
        final FDTReaderKeyAttachement attach = (FDTReaderKeyAttachement) fdtSelectionKey.attachment();
        final SocketChannel sc = fdtSelectionKey.channel();

        final boolean logFinest = logger.isLoggable(Level.FINEST);
        if (logFinest) {
            logger.log(Level.FINEST, " [ SocketReaderTask ] [ readData ] for " + Utils.toStringSelectionKey(fdtSelectionKey));
        }

        long count = -1;

        if (!attach.hasBuffers()) {
            if (!attach.recycleAndSetBuffers()) {
                return false;
            }
        } else {
            if (checkForData()) {
                if (!setAttachementBuffers(attach)) {
                    return false;
                }
                if (isClosed()) {
                    attach.recycleBuffers();
                }
            }
        }

        final ByteBuffer bpl = attach.payload();
        for (; ; ) {
            if (isClosed() || Thread.currentThread().isInterrupted())
                break;

            if (isNetTest) {
                attach.isHeaderProcessed = true;
                bpl.clear();
                // attach.header.clear();
                count = sc.read(bpl);
                if (isBlocking) {
                    while (count >= 0) {
                        addAndGetTotalBytes(count);
                        master.addAndGetTotalBytes(count);
                        bpl.clear();
                        count = sc.read(bpl);
                        continue;
                    }
                }
            } else if (attach.useFixedSizeBlocks) {
                count = sc.read(attach.asArray());
            } else {// parse the header first
                if (attach.isHeaderRead()) {
                    count = sc.read(attach.payload());
                } else {
                    count = sc.read(attach.header());
                    if (attach.isHeaderRead()) {
                        addAndGetTotalBytes(count);
                        if (logFinest) {
                            logger.log(Level.FINEST, " [ SocketReaderTask ] socket: " + sc.socket() + " count: " + count);
                        }
                        master.addAndGetTotalBytes(count);
                        count = sc.read(attach.payload());
                    }
                }
            }

            if (count > 0) {

                fdtSelectionKey.opCount = 0;

                if (logFinest) {
                    logger.log(Level.FINEST, " [ SocketReaderTask ] socket: " + sc.socket() + " count: " + count);
                }

                addAndGetTotalBytes(count);
                master.addAndGetTotalBytes(count);

                if (checkForData()) {
                    if (!setAttachementBuffers(attach)) {
                        return false;
                    }
                    if (isClosed()) {
                        attach.recycleBuffers();
                    }
                }

                continue;
            } else if (count == 0) {

                if (checkForData()) {
                    if (setAttachementBuffers(attach)) {
                        if (isClosed()) {
                            attach.recycleBuffers();
                        }
                        continue;
                    }
                }

                // try multiple IO Reads ...
                if (fdtSelectionKey.opCount++ > RETRY_IO_COUNT) {
                    if (isBlocking) {
                        if (!attach.hasBuffers()) {
                            break;
                        }

                        logger.log(Level.WARNING, " reached RETRY_IO_COUNT in blocking mode ... remote peer down?! SC is blocking: " + sc.isBlocking());
                        master.workerDown(fdtSelectionKey, null);
                    }

                    if (attach.hasBuffers()) {
                        fdtSelectionKey.renewInterest();
                        return true;
                    }

                    break;
                }

                continue;
            } else {
                master.workerDown(fdtSelectionKey, null);
                close("EOF", null);
            }

        }

        return false;
    }

    private void recycleBuffers() {
        try {
            final FDTSelectionKey fdtSelectionKey = this.fdtSelectionKeyRef.get();
            if (fdtSelectionKey != null) {
                FDTKeyAttachement attach = fdtSelectionKey.attachment();
                if (attach != null) {
                    attach.recycleBuffers();
                }
            }
        } catch (Throwable t1) {
            logger.log(Level.WARNING, " Got exception trying to recover the buffers and returning them to pool", t1);
        }
    }

    public void internalClose() {
        final FDTSelectionKey fdtSelectionKey = fdtSelectionKeyRef.getAndSet(null);
        if (fdtSelectionKey != null) {
            fdtSelectionKey.cancel();

            final SocketChannel sc = fdtSelectionKey.channel();
            if (sc != null) {
                try {
                    sc.close();
                } catch (Throwable t) {
                }
            }

            final FDTKeyAttachement keyAttachement = fdtSelectionKey.attachment();
            if (keyAttachement != null) {
                try {
                    keyAttachement.recycleBuffers();
                } catch (Throwable t) {
                }
            }
        }
    }

    public void run() {
        String cName = Thread.currentThread().getName();
        String name = " SocketReaderTask for [ " + master.fdtSession.sessionID() + " ]";
        Thread.currentThread().setName(name);

        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, name + " STARTED !");
        }

        try {
            for (; ; ) {
                // use a local FDTSelKey for a little speed-up
                FDTSelectionKey iSel = null;
                fdtSelectionKeyRef.set(null);
                try {
                    while (iSel == null) {
                        fdtSelectionKeyRef.getAndSet(readyChannelsQueue.poll(2, TimeUnit.SECONDS));
                        iSel = fdtSelectionKeyRef.get();
                        if (isClosed()) {
                            break;
                        }
                    }
                } catch (InterruptedException ie) {
                    if (!isClosed()) {
                        logger.log(Level.INFO, name + " was interrupted ... ", ie);
                    } else {
                        break;
                    }
                    Thread.interrupted();
                    close("Got interrupted exception", ie);
                }

                final FDTSelectionKey fdtSelectionKey = iSel;
                if (isClosed() || fdtSelectionKey == null)
                    break;

                try {
                    if (!readData()) {
                        if (!isClosed()) {
                            if (readyChannelsQueue.offer(fdtSelectionKeyRef.getAndSet(null))) {
                                throw new FDTProcolException(" Unable to add selection key in the selection queue");
                            }
                        } else {
                            recycleBuffers();
                        }
                    }
                } catch (Throwable t) {
                    master.close("Exception reading data", t);

                    recycleBuffers();

                    close(" Got exception trying to readData() from channel", t);
                    break;
                }
            } // for(;;)

        } finally {
            try {
                final FDTSelectionKey fdtSelectionKey = fdtSelectionKeyRef.get();

                if (fdtSelectionKey != null) {
                    final FDTKeyAttachement attach = fdtSelectionKey.attachment();
                    if (attach != null) {
                        attach.recycleBuffers();
                    }
                }
            } catch (Throwable t1) {
                logger.log(Level.WARNING, " Got exception trying to return buffers to the pool", t1);
            }

            Thread.currentThread().setName(cName);
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.INFO, name + " FINISHED !");
            }
        }
    }

}
