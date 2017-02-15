package lia.util.net.copy.transport;

import java.util.concurrent.BlockingQueue;

import lia.util.net.common.AbstractFDTIOEntity;
import lia.util.net.common.Config;
import lia.util.net.common.DirectByteBufferPool;
import lia.util.net.common.HeaderBufferPool;
import lia.util.net.copy.transport.internal.FDTSelectionKey;


public abstract class SocketTask extends AbstractFDTIOEntity implements Runnable {
    protected final static DirectByteBufferPool payloadPool = DirectByteBufferPool.getInstance();
    protected final static HeaderBufferPool headersPool = HeaderBufferPool.getInstance();
    protected static final boolean isBlocking =  Config.getInstance().isBlocking();

    protected BlockingQueue<FDTSelectionKey> readyChannelsQueue;

    public SocketTask(BlockingQueue<FDTSelectionKey> readyChannelsQueue) {
        this.readyChannelsQueue = readyChannelsQueue;
    }
}
