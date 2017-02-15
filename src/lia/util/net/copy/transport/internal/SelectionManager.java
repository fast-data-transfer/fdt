/*
 * $Id: SelectionManager.java 635 2011-02-08 15:47:33Z ramiro $
 */
package lia.util.net.copy.transport.internal;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import lia.util.net.common.Config;
import lia.util.net.common.Utils;
import lia.util.net.copy.transport.FDTKeyAttachement;

/**
 * Wrapper class for NIO Selector-s and SocketChannel-s, hopefully optimizes the selection process for the entire app.
 * Every select() runs in its own Task. The synchronization should be "loose" enough ( a sync per Task ), and probably
 * the contention of selector.wakeup() also. Having queues for FDTKey should scale at high load-s
 * 
 * @author ramiro
 */
public class SelectionManager {

    private static final Logger logger = Logger.getLogger(SelectionManager.class.getName());

    // should be one for every Selector - handles the selection process
    final static class SelectionTask implements Runnable {

        private final Selector selector;

        private final AtomicBoolean hasToRun;

        // this lock guards the (re)new queues
        // performance is a moving target :); synchronized in better in Java6 ( AQS ;) )
        // private final ReentrantLock lock = new ReentrantLock();

        private final Deque<FDTSelectionKey> renewQueue;

        private final Deque<FDTSelectionKey> newQueue;

        SelectionTask(Selector selector) {

            renewQueue = new ArrayDeque<FDTSelectionKey>();
            newQueue = new ArrayDeque<FDTSelectionKey>();

            hasToRun = new AtomicBoolean(false);

            if (selector == null) {
                throw new NullPointerException("Selector cannot be null in SelectionTask constructor");
            }

            if (!selector.isOpen()) {
                throw new IllegalArgumentException("Selector is not open in SelectionTask constructor");
            }

            this.selector = selector;
            hasToRun.set(true);
        }

        private void checkRenew() {
            final Queue<FDTSelectionKey> l = renewQueue;

            if (l.isEmpty())
                return;

            final boolean finest = logger.isLoggable(Level.FINEST);
            // the lock must be taken already
            while (!l.isEmpty()) {
                final FDTSelectionKey fdtSelectionKey = l.remove();
                if (finest) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("[ SelectionManager ] [ checkRenew ] for ").append(Utils.toStringSelectionKey(fdtSelectionKey));
                    logger.log(Level.FINEST, sb.toString());
                }

                final SelectionKey sk = fdtSelectionKey.selectionKey;

                if (!sk.isValid()) {
                    fdtSelectionKey.cancel();
                    continue;
                }

                sk.interestOps(fdtSelectionKey.selectionKey.interestOps() | fdtSelectionKey.interests);
            }
        }

        private void checkNew() {
            final Queue<FDTSelectionKey> l = newQueue;
            // speed-up things a little bit
            if (l.isEmpty())
                return;

            final boolean finest = logger.isLoggable(Level.FINEST);
            // the lock must be taken already
            while (!l.isEmpty()) {
                try {
                    final FDTSelectionKey fdtSelectionKey = l.remove();
                    fdtSelectionKey.selectionKey = fdtSelectionKey.channel.register(selector, fdtSelectionKey.interests, fdtSelectionKey);
                    if (finest) {
                        StringBuilder sb = new StringBuilder();
                        sb.append("[ SelectionManager ] [ checkNew ] for ").append(Utils.toStringSelectionKey(fdtSelectionKey));
                        logger.log(Level.FINEST, sb.toString());
                    }
                } catch (Throwable t) {
                    logger.log(Level.WARNING, " [ SelectionManager ] [ checkNew ] got exception. Cause", t);
                }
            }

        }

        public void run() {

            int count = 0;

            // final ReentrantLock lock = this.lock;
            // final Object lock = this;

            while (hasToRun.get()) {

                synchronized (this) {
                    checkRenew();
                    checkNew();
                }
                // try {
                // } finally {
                // lock.unlock();
                // }

                try {
                    count = selector.select();
                } catch (IOException ioe) {
                    logger.log(Level.WARNING, " [ SelectionManager ] [ SelectionTask ] IOException in selector.select(). Cause: ", ioe);
                } catch (Throwable t) {
                    logger.log(Level.WARNING, " [ SelectionManager ] [ SelectionTask ] Generic Exception in selector.select(). Cause: ", t);
                }

                // maybe a renew ...
                if (count == 0)
                    continue;

                final Iterator<SelectionKey> it = selector.selectedKeys().iterator();
                while (it.hasNext()) {

                    final SelectionKey sk = it.next();
                    try {
                        it.remove();

                        final FDTSelectionKey fdtSelectionKey = (FDTSelectionKey) sk.attachment();

                        if (!sk.isValid()) {
                            if (fdtSelectionKey != null) {
                                fdtSelectionKey.cancel();
                            }
                            continue;
                        }

                        if (fdtSelectionKey != null) {

                            // cancel the interest
                            // TODO - This works for single registration keys (
                            // it does not work if fdtSelectionKey.interests ==
                            // OP_READ | OP_WRITE )

                            sk.interestOps(sk.interestOps() & ~fdtSelectionKey.interests);
                            fdtSelectionKey.renewed.set(false);
                            fdtSelectionKey.handler.handleSelection(fdtSelectionKey);
                        } else {
                            logger.log(Level.WARNING, "\n\n fdtSelectionKey is null in selection loop for sk: " + sk + " channel: " + sk.channel() + ". The channle will be closed\n\n");
                            sk.cancel();
                            try {
                                sk.channel().close();
                            } catch (Throwable ignore) {
                            }
                        }
                    } catch (Throwable t) {
                        logger.log(Level.WARNING, " [ SelectionManager ] [ SelectionTask ] Exception in main loop notifying FDTKeys to handlers. Cause: ", t);
                    }
                }// while - iterator

            }// while - hasToRun

            cancelAllKeys();
        }

        private void cancelAllKeys() {
            SelectionKey sk = null;
            FDTSelectionKey fsk = null;
            for (Iterator<SelectionKey> it = selector.keys().iterator(); it.hasNext();) {
                try {
                    sk = it.next();

                    if (sk != null) {
                        fsk = (FDTSelectionKey) sk.attachment();
                        if (fsk != null) {
                            fsk.cancel();
                        }
                    }
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }// for()
        }

        public void stopIt() {
            if (hasToRun.compareAndSet(true, false)) {
                selector.wakeup();
            }
        }
    }

    // the one and only. Now, go outside and play
    private static final SelectionManager _thisInstance;

    final Map<Selector, SelectionTask> selTasksMap;

    private final BlockingQueue<Selector> selectorsQueue;

    static {
        SelectionManager tmpSMgr = null;

        try {
            tmpSMgr = new SelectionManager();
            int i = 0;
            for (Map.Entry<Selector, SelectionTask> entry : tmpSMgr.selTasksMap.entrySet()) {
                Thread t = new Thread(entry.getValue(), " [ SelectionManager ] Selection task ( " + i++ + " )");
                t.setDaemon(true);
                t.start();
            }
        } catch (Throwable t) {
            logger.log(Level.WARNING, "Got exception initializing SelectionManager. Cannot continue. Will stop", t);
            throw new RuntimeException("Cannot instantiate SelectionManager. Do you want to continue ( N/N )? ", t);
        }

        _thisInstance = tmpSMgr;
    }

    private SelectionManager() throws IOException {
        selTasksMap = new HashMap<Selector, SelectionTask>();
        int sNo = Config.getInstance().getNumberOfSelectors();

        selectorsQueue = new ArrayBlockingQueue<Selector>(sNo);
        for (int i = 0; i < sNo; i++) {
            Selector sel = Selector.open();
            selectorsQueue.add(sel);
            SelectionTask st = new SelectionTask(sel);
            selTasksMap.put(sel, st);
        }
    }

    public static final SelectionManager getInstance() {
        return _thisInstance;
    }

    void renewInterest(FDTSelectionKey fdtSelectionKey) {
        final SelectionTask st = fdtSelectionKey.selectionTask;
        boolean bShouldWakeup = false;

        // final ReentrantLock lock = st.lock;
        // lock.lock();
        // try {
        // } finally {
        // lock.unlock();
        // }
        synchronized (st) {
            if (st.renewQueue.isEmpty() && st.newQueue.isEmpty()) {
                bShouldWakeup = true;
            }

            st.renewQueue.add(fdtSelectionKey);
        }

        if (bShouldWakeup) {
            st.selector.wakeup();
        }
    }

    /**
     * This should not be used from FDT ....
     * 
     * @param channel
     * @param interests
     * @param selectionHandler
     * @return
     * @throws InterruptedException
     * @throws IOException
     */
    public FDTSelectionKey register(final SocketChannel channel, final int interests, final SelectionHandler selectionHandler) throws InterruptedException, IOException {
        return register(null, channel, interests, selectionHandler);
    }

    /**
     * This method register a channel with specific interests. The readiness will be notified to
     * <code>SelectionHandler</code> parameter If this key is canceled during the selection process ( e.g stream closes
     * ) the same handler will be notified
     * 
     * @param fdtsessionID
     * @param channel
     * @param interests
     * @param selectionHandler
     * @return
     * @throws InterruptedException
     * @throws IOException
     */
    public FDTSelectionKey register(final UUID fdtsessionID, final SocketChannel channel, final int interests, final SelectionHandler selectionHandler) throws InterruptedException {
        return register(fdtsessionID, channel, interests, selectionHandler, null);
    }

    // TODO ... extra checks to see if a channel is already registered
    public FDTSelectionKey register(final UUID fdtsessionID, final SocketChannel channel, final int interests, final SelectionHandler selectionHandler, final FDTKeyAttachement attach) throws InterruptedException {

        if (channel == null) {
            throw new NullPointerException("SocketChannel cannot be null");
        }

        if (selectionHandler == null) {
            throw new NullPointerException("SelectionHanfler cannot be null");
        }

        final Selector sel = getAndRotateSelector();
        final SelectionTask sTask = selTasksMap.get(sel);

        FDTSelectionKey fdtSelectionKey = new FDTSelectionKey(fdtsessionID, channel, interests, selectionHandler, attach, sel, sTask);

        return fdtSelectionKey;
    }

    public void stopIt() {
        try {
            for (Map.Entry<Selector, SelectionTask> entry : selTasksMap.entrySet()) {
                entry.getValue().stopIt();
                entry.getValue().selector.wakeup();
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    void initialKeyRegister(FDTSelectionKey fdtSelectionKey) {

        final Selector sel = fdtSelectionKey.selector;
        final SelectionTask st = fdtSelectionKey.selectionTask;

        boolean bShouldWakeup = false;

        // something gone wrong if either the Selector or the SelectionTask are
        // null

        // final ReentrantLock lock = st.lock;

        // lock.lock();
        // try {
        // } finally {
        // lock.unlock();
        // }

        synchronized (st) {
            if (st.renewQueue.isEmpty() && st.newQueue.isEmpty()) {
                bShouldWakeup = true;
            }

            st.newQueue.add(fdtSelectionKey);
        }

        if (bShouldWakeup) {
            sel.wakeup();
        }
    }

    // just serve them in round robin stile
    private Selector getAndRotateSelector() throws InterruptedException {
        final Selector sel = selectorsQueue.take();
        selectorsQueue.put(sel);
        return sel;
    }

}
