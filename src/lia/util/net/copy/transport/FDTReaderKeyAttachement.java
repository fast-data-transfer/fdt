/*
 * $Id$
 */

package lia.util.net.copy.transport;

import lia.util.net.copy.FileBlock;
import lia.util.net.copy.transport.internal.FDTSelectionKey;

import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * The key selection used by the "read" sockets TODO - Use finer grained synchronization mechanism instead of
 * syncronized on the entire key
 *
 * @author ramiro
 */
class FDTReaderKeyAttachement extends FDTKeyAttachement {

    public int version;

    public int packetType;

    public UUID uuid;

    public long fileOffset;

    // payload size()
    public volatile int payloadSize;

    public volatile long seq;

    public volatile long tstamp;

    // cache the header processing
    boolean isHeaderProcessed;

    FDTReaderKeyAttachement(FDTSelectionKey fdtSelectionKey, boolean useFixedSizeBlocks) {
        super(fdtSelectionKey, useFixedSizeBlocks);
        isHeaderProcessed = false;
    }

    public synchronized final FileBlock toFileBlock() {
        final ByteBuffer payload = payload();
        payload.flip();
        payload.limit(payloadSize);
        FileBlock fileBlock = FileBlock.getInstance(fdtSelectionKey.fdtSessionID(), uuid, fileOffset, payload);
        setPayload(null);
        return fileBlock;
    }

    public synchronized boolean isHeaderRead() {
        if (isHeaderProcessed) {
            return true;
        }

        final ByteBuffer header = header();
        if (header != null && !header.hasRemaining()) {
            processHeader();
            isHeaderProcessed = true;
            return true;
        }
        return false;
    }

    private void processHeader() {

        final ByteBuffer header = header();

        header.flip();

        // read the version
        version = header.getInt();

        // packet type
        packetType = header.getInt();

        // header size
        header.getInt();

        // payload size
        payloadSize = header.getInt();

        // the packet tag
        seq = header.getLong();

        // timeStamp
        tstamp = header.getLong();

        // read the uuid
        uuid = new UUID(header.getLong(), header.getLong());

        fileOffset = header.getLong();

        if (!useFixedSizeBlocks) {
            payload().limit(payloadSize);
        }
    }

    public synchronized void setBuffers(ByteBuffer header, ByteBuffer payload) {
        super.setBuffers(header, payload);
        isHeaderProcessed = false;
    }

    public synchronized boolean isPayloadRead() {
        final ByteBuffer payload = payload();
        return (payload != null && !payload.hasRemaining());
    }

}
