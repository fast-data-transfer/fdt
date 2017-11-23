/*
 * $Id$
 */
package lia.util.net.copy.transport;

import lia.util.net.common.AbstractFDTIOEntity;
import lia.util.net.common.Config;
import lia.util.net.copy.transport.internal.FDTSelectionKey;

import java.util.concurrent.BlockingQueue;

/**
 * The base class for to fill/drain a socket channel.
 *
 * @author ramiro
 */
public abstract class SocketTask extends AbstractFDTIOEntity implements Runnable {
    protected static final boolean isBlocking = Config.getInstance().isBlocking();

    protected final BlockingQueue<FDTSelectionKey> readyChannelsQueue;

    public SocketTask(BlockingQueue<FDTSelectionKey> readyChannelsQueue) {
        this.readyChannelsQueue = readyChannelsQueue;
    }

    public long getSize() {
        return -1;
    }

    protected void internalClose() throws Exception {
        try {
            for (final FDTSelectionKey selKey : readyChannelsQueue) {
                final FDTKeyAttachement attach = selKey.attachment();
                if (attach != null) {
                    attach.recycleBuffers();
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
