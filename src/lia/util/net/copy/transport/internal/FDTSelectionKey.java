/*
 * $Id: FDTSelectionKey.java 567 2010-01-28 06:06:01Z ramiro $
 */
package lia.util.net.copy.transport.internal;

import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import lia.util.net.common.Utils;
import lia.util.net.copy.transport.FDTKeyAttachement;
import lia.util.net.copy.transport.internal.SelectionManager.SelectionTask;

/**
 * This class is used in conjunction with SelectionManager to handle the NIO event readiness It is a wrapper over
 * {@link SelectionKey} with
 * 
 * @author ramiro
 */
public class FDTSelectionKey {

    private static final SelectionManager selectionManager = SelectionManager.getInstance();

    protected final Selector selector;

    protected final SelectionTask selectionTask;

    protected SocketChannel channel;

    SelectionKey selectionKey;

    final SelectionHandler handler;

    final int interests;

    final AtomicBoolean canceled;

    final AtomicBoolean registered;

    final AtomicBoolean renewed;

    protected final UUID fdtSessionID;

    public volatile int opCount;

    private volatile FDTKeyAttachement attachment;

    int MSS = -1;

    FDTSelectionKey(UUID fdtSessionID, SocketChannel channel, int interests, SelectionHandler handler, FDTKeyAttachement attachement, Selector selector, SelectionTask selectionTask) {
        this.channel = channel;
        this.interests = interests;
        this.handler = handler;

        renewed = new AtomicBoolean(false);
        canceled = new AtomicBoolean(false);
        registered = new AtomicBoolean(false);

        this.fdtSessionID = fdtSessionID;
        this.attachment = attachement;
        this.selector = selector;
        this.selectionTask = selectionTask;
    }

    public FDTSelectionKey(UUID fdtSessionID, SocketChannel channel, int interests, SelectionHandler handler, FDTKeyAttachement attachement, Selector selector) {
        this(fdtSessionID, channel, interests, handler, null, selector, null);
    }

    public FDTSelectionKey(UUID fdtSessionID, SocketChannel channel, int interests, SelectionHandler handler, Selector selector) {
        this(fdtSessionID, channel, interests, handler, null, selector);
    }

    public boolean registerInterest() {

        if (registered.compareAndSet(false, true)) {
            selectionManager.initialKeyRegister(this);
            return true;
        }

        return false;
    }

    /**
     * Should be called to renew the this interest for I/O readiness
     * 
     * @return true, if the renewal was successful ( if the key is already registered if will remain register and this
     *         function will return false)
     */
    public boolean renewInterest() {

        if (renewed.compareAndSet(false, true)) {
            opCount = 0;
            selectionManager.renewInterest(this);
            return true;
        }

        return false;
    }

    /**
     * Cancels the I/O readiness interests. It can be called multiple times.
     * 
     * @return true, only the first time when it is called, and false for any other subsequent calls
     */
    public boolean cancel() {
        if (canceled.compareAndSet(false, true)) {
            try {
                this.channel.close();
            } catch (Throwable t) {
            }

            if (handler != null) {
                try {
                    handler.canceled(this);
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }

            // Is it even possible ... only if the registration failed in the first place !!
            if (selectionKey != null) {
                try {
                    selectionKey.cancel();
                } catch (Throwable t) {
                }
                
                try {
                    if(selectionKey.selector() != null) {
                        //just for cleanup - otherwise stalled sockets
                        renewInterest();
                    }
                }catch(Throwable t) {}
            }

            final FDTKeyAttachement attach = attachment();
            if (attach != null) {
                try {
                    attach.recycleBuffers();
                } catch (Throwable t) {
                }
            }
            return true;
        }
        return false;
    }

    public boolean isValid() {
        return !canceled.get();
    }

    public SocketChannel channel() {
        return channel;
    }

    /**
     * Same functionality provided by NIO's SelectionKey
     * 
     * @param attachement
     */
    public final FDTKeyAttachement attach(FDTKeyAttachement o) {
        FDTKeyAttachement ret = this.attachment;
        this.attachment = o;
        return ret;
    }

    public UUID fdtSessionID() {
        return fdtSessionID;
    }

    /**
     * Same functionality provided by NIO's SelectionKey
     * 
     * @param attachement
     */
    public final FDTKeyAttachement attachment() {
        return attachment;
    }

    public int getMSS() {
        return MSS;
    }

    public void setMSS(int MSS) {
        this.MSS = MSS;
    }

    public Selector selector() {
        return selector;
    }

    public String toString() {
        return Utils.toStringSelectionKey(this);
    }
}
