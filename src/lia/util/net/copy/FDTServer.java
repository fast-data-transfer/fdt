package lia.util.net.copy;

import lia.gsi.FDTGSIServer;
import lia.util.net.common.*;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Date;
import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The accept() task will run in it's own thread ( including here the server )
 *
 * @author ramiro
 */
public class FDTServer extends AbstractFDTCloseable {

    private static final Logger logger = Logger.getLogger(FDTServer.class.getName());

    private static final Config config = Config.getInstance();

    private static final FDTSessionManager fdtSessionManager = FDTSessionManager.getInstance();

    final ServerSocketChannel ssc;

    final ServerSocket ss;

    final Selector sel;

    //used by the AcceptableTask-s
    final ExecutorService executor;

    //signals the server stop
    final AtomicBoolean hasToRun;

    UUID fdtSessionID;

    public FDTServer(int port) throws Exception {
        hasToRun = new AtomicBoolean(true);

        // We are not very happy to welcome new clients ... so the priority will be lower
        executor = Utils.getStandardExecService("[ Acceptable ServersThreadPool ] ",
                5,
                10,
                new ArrayBlockingQueue<Runnable>(65500),
                Thread.NORM_PRIORITY - 2);
        ssc = ServerSocketChannel.open();
        ssc.configureBlocking(false);

        ss = ssc.socket();
        ss.bind(new InetSocketAddress(port));

        sel = Selector.open();
        ssc.register(sel, SelectionKey.OP_ACCEPT);

        if (config.isGSIModeEnabled()) {
            FDTGSIServer gsiServer = new FDTGSIServer(config.getGSIPort());
            logger.log(Level.INFO, "FDT started in GSI mode on port: " + config.getGSIPort());
            gsiServer.start();
        }
        // Monitoring & Nice Prnting
        final ScheduledExecutorService monitoringService = Utils.getMonitoringExecService();

        monitoringService.scheduleWithFixedDelay(new FDTServerMonitorTask(), 10, 10, TimeUnit.SECONDS);

        // in SSH mode this is a ACK message for the client to inform it that the server started ok
        // (the server stdout is piped to client through the SSH channel)
        System.out.println("READY");
    }

    public static final boolean filterSourceAddress(java.net.Socket socket) {
        /**
         * check if remote client is allowed (based on optional filter specified in command line) if the address does
         * not match, reject this IP (i.e DO NOT accept the session-tag
         * from this client)
         */
        final NetMatcher filter = config.getSourceAddressFilter();
        if (filter != null) {
            logger.info("Enforcing source address filter: " + filter);
            final String sourceIPAddress = socket.getInetAddress().getHostAddress();
            if (!filter.matchInetNetwork(sourceIPAddress)) {
                Utils.closeIgnoringExceptions(socket);
                logger.warning(" Client [" + sourceIPAddress + "] is not allowed to transfer. Socket closed!");
                return false;
            }
        }
        return true;

    }

    public static final void main(String[] args) throws Exception {
        FDTServer jncs = new FDTServer(config.getPort());
        jncs.doWork();
    }

    /**
     * Safe to call multiple times; will return false if the server was already signaled to stop
     * </br>
     * <p>
     * <b>Note:</b> Invoking this method acts as a signal for the server. Any ongoing transfers will continue until they finish
     * </p>
     *
     * @return true if server was signaled to stop
     */
    public boolean stopServer() {
        return hasToRun.compareAndSet(true, false);
    }

    public UUID getFdtSessionID() {
        return fdtSessionID;
    }

    public void doWork() throws Exception {

        Thread.currentThread().setName(" FDTServer - Main loop worker ");
        logger.info("FDTServer start listening on port: " + ss.getLocalPort());

        final boolean isStandAlone = config.isStandAlone();
        try {
            for (; ; ) {
                if (!isStandAlone) {
                    if (fdtSessionManager.isInited() && fdtSessionManager.sessionsNumber() == 0) {
                        logger.log(Level.INFO, "FDTServer will finish. No more sessions to serve.");
                        return;
                    }
                } else {
                    if (!hasToRun.get()) {
                        // stopServer was called
                        if (fdtSessionManager.isInited() && fdtSessionManager.sessionsNumber() == 0) {
                            logger.log(Level.INFO, "FDTServer will finish. No more sessions to serve.");
                            return;
                        }
                    }
                }
                final int count = sel.select(2000);

                if (count == 0)
                    continue;

                Iterator<SelectionKey> it = sel.selectedKeys().iterator();
                while (it.hasNext()) {
                    final SelectionKey sk = it.next();
                    it.remove();

                    if (!sk.isValid())
                        continue;// closed socket ?

                    if (sk.isAcceptable()) {
                        final ServerSocketChannel ssc = (ServerSocketChannel) sk.channel();
                        final SocketChannel sc = ssc.accept();

                        try {
                            executor.execute(new AcceptableTask(sc));
                        } catch (Throwable t) {
                            StringBuilder sb = new StringBuilder();
                            sb.append("[ FDTServer ] got exception in while sumbiting the AcceptableTask for SocketChannel: ").append(sc);
                            if (sc != null) {
                                sb.append(" Socket: ").append(sc.socket());
                            }
                            sb.append(" Cause: ");
                            logger.log(Level.WARNING, sb.toString(), t);
                        }
                    }
                }
            }
        } catch (Throwable t) {
            logger.log(Level.WARNING, "[FDTServer] Exception in main loop!", t);
            throw new Exception(t);
        } finally {
            logger.log(Level.INFO, "[FDTServer] main loop FINISHED!");
            // close all the stuff
            Utils.closeIgnoringExceptions(ssc);
            Utils.closeIgnoringExceptions(sel);
            Utils.closeIgnoringExceptions(ss);
            if (executor != null) {
                executor.shutdown();
            }
        }
    }

    public void run() {

        try {
            doWork();
        } catch (Throwable t) {
            logger.log(Level.WARNING, "[ FDTServer ] exception main loop", t);
            close("[ FDTServer ] exception main loop", t);
        }

        close(null, null);

        logger.info(" \n\n FDTServer finishes @ " + new Date().toString() + "!\n\n");
    }

    @Override
    protected void internalClose() {
        // TODO Auto-generated method stub

    }

    static final class FDTServerMonitorTask implements Runnable {

        public void run() {
            // TODO Later
        }
    }

}
