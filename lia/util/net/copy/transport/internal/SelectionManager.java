
package lia.util.net.copy.transport.internal;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import lia.util.net.common.Config;
import lia.util.net.common.Utils;


public class SelectionManager {

    private static final Logger logger = Logger.getLogger(SelectionManager.class.getName());

    
    private final static class SelectionTask implements Runnable {

        private final Selector                    selector;

        private final AtomicBoolean               hasToRun;

        
        private final ReentrantLock    lock = new ReentrantLock();

        private final List<FDTSelectionKey> renewQueue;

        private final List<FDTSelectionKey> newQueue;

        SelectionTask(Selector selector) {

            renewQueue = new LinkedList<FDTSelectionKey>();
            newQueue = new LinkedList<FDTSelectionKey>();

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
        	final List<FDTSelectionKey> l = renewQueue;
        	
        	if(l.isEmpty()) return;
        	
        	final boolean finest = logger.isLoggable(Level.FINEST);
            
        	final Iterator<FDTSelectionKey> it = l.iterator();
            while (it.hasNext()) {
                final FDTSelectionKey fdtSelectionKey = it.next();
                if (finest) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("[ SelectionManager ] [ checkRenew ] for ").append(Utils.toStringSelectionKey(fdtSelectionKey));
                    logger.log(Level.FINEST, sb.toString());
                }
                fdtSelectionKey.selectionKey.interestOps(fdtSelectionKey.selectionKey.interestOps() | fdtSelectionKey.interests);
            }
            
            l.clear();
        }

        private void checkNew() {
        	final List<FDTSelectionKey> l = newQueue;
        	
        	if(l.isEmpty()) return;
        	
        	final boolean finest = logger.isLoggable(Level.FINEST);
            
        	final Iterator<FDTSelectionKey> it = l.iterator();
            while (it.hasNext()) {
                try {
                    final FDTSelectionKey fdtSelectionKey = it.next();
                    fdtSelectionKey.selector = selector;
                    fdtSelectionKey.selectionKey = fdtSelectionKey.channel.register(selector, fdtSelectionKey.interests);
                    fdtSelectionKey.selectionKey.attach(fdtSelectionKey);
                    if (finest) {
                        StringBuilder sb = new StringBuilder();
                        sb.append("[ SelectionManager ] [ checkNew ] for ").append(Utils.toStringSelectionKey(fdtSelectionKey));
                        logger.log(Level.FINEST, sb.toString());
                    }
                } catch (Throwable t) {
                    logger.log(Level.WARNING, " [ SelectionManager ] [ checkNew ] got exception. Cause", t);
                }
            }
            
            l.clear();
        }

        public void run() {

            int count = 0;

            final ReentrantLock lock = this.lock;

            while (hasToRun.get()) {
                
                lock.lock();
                try {
                    checkRenew();
                    checkNew();
                } finally {
                    lock.unlock();
                }

                try {
                    count = selector.select();
                } catch (IOException ioe) {
                    logger.log(Level.WARNING, " [ SelectionManager ] [ SelectionTask ] IOException in selector.select(). Cause: ", ioe);
                } catch (Throwable t) {
                    logger.log(Level.WARNING, " [ SelectionManager ] [ SelectionTask ] Generic Exception in selector.select(). Cause: ", t);
                }

                
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
                }

            }

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
            }
        }

        public void stopIt() {
            if (hasToRun.compareAndSet(true, false)) {
                selector.wakeup();
            }
        }
    }

    
    private static final SelectionManager _thisInstance;

    final Map<Selector, SelectionTask>    selTasksMap;

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
        SelectionTask st = selTasksMap.get(fdtSelectionKey.selector);
        boolean bShouldWakeup = false;

        final ReentrantLock lock = st.lock;

        lock.lock();
        try {
            if (st.newQueue.isEmpty() || st.renewQueue.isEmpty()) {
                bShouldWakeup = true;
            }

            st.renewQueue.add(fdtSelectionKey);
        } finally {
            lock.unlock();
        }


        if (bShouldWakeup) {
            st.selector.wakeup();
        }
    }

    
    public FDTSelectionKey register(
            final SocketChannel channel,
            final int interests,
            final SelectionHandler selectionHandler) throws InterruptedException, IOException {
        return register(null, channel, interests, selectionHandler);
    }

    
    public FDTSelectionKey register(
            final UUID fdtsessionID,
            final SocketChannel channel,
            final int interests,
            final SelectionHandler selectionHandler) throws InterruptedException {
        return register(fdtsessionID, channel, interests, selectionHandler, null);
    }

    
    public FDTSelectionKey register(
            final UUID fdtsessionID,
            final SocketChannel channel,
            final int interests,
            final SelectionHandler selectionHandler,
            final Object attach) throws InterruptedException {

        if (channel == null) {
            throw new NullPointerException("SocketChannel cannot be null");
        }

        if (selectionHandler == null) {
            throw new NullPointerException("SelectionHanfler cannot be null");
        }

        FDTSelectionKey fdtSelectionKey = new FDTSelectionKey(fdtsessionID,
                                                              channel,
                                                              interests,
                                                              selectionHandler,
                                                              attach);
        newSelectorForKey(fdtSelectionKey);

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
        final SelectionTask st = selTasksMap.get(sel);

        boolean bShouldWakeup = false;

        
        

        final ReentrantLock lock = st.lock;

        lock.lock();
        try {

            if (st.newQueue.isEmpty() || st.renewQueue.isEmpty()) {
                bShouldWakeup = true;
            }

            st.newQueue.add(fdtSelectionKey);
            fdtSelectionKey.selector = sel;
        } finally {
            lock.unlock();
        }

        if (bShouldWakeup) {
            sel.wakeup();
        }
    }

    
    private void newSelectorForKey(FDTSelectionKey fdtSelectionKey) throws InterruptedException {
        final Selector sel = selectorsQueue.take();
        fdtSelectionKey.selector = sel;
        selectorsQueue.put(sel);
    }

}
