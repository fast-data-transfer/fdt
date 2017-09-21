package lia.util.net.common;

import lia.util.net.copy.FDTServer;
import lia.util.net.copy.FDTSessionManager;
import lia.util.net.copy.transport.ControlChannel;
import lia.util.net.copy.transport.gui.ServerSessionManager;

import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Raimondas Sirvinskas
 * @version 1.0
 */
public final class AcceptableTask implements Runnable {

    private static final Logger logger = Logger.getLogger(AcceptableTask.class.getName());

    private static final Config config = Config.getInstance();

    private static final FDTSessionManager fdtSessionManager = FDTSessionManager.getInstance();

    final SocketChannel sc;

    final Socket s;

    public AcceptableTask(final SocketChannel sc) throws IOException {

        if (sc == null) {
            throw new NullPointerException("SocketChannel cannot be null in AcceptableTask");
        }

        if (sc.socket() == null) {
            throw new NullPointerException("Null Socket for SocketChannel in AcceptableTask");
        }

        this.sc = sc;
        this.s = sc.socket();
    }

    public void run() {

        if (!FDTServer.filterSourceAddress(s))
            return;

        if (logger.isLoggable(Level.FINER)) {
            logger.log(Level.FINER, " AcceptableTask for " + sc + " STARTED!");
        }
        final String sdpConfFlag = System.getProperty("com.sun.sdp.conf");
        final boolean bSDP = (sdpConfFlag != null && !sdpConfFlag.isEmpty());
        if (!bSDP) {
            try {
                s.setKeepAlive(true);
            } catch (Throwable t) {
                logger.log(Level.WARNING, "[ FDTServer ] [ AcceptableTask ] Cannot set KEEP_ALIVE for " + sc
                        + ". Will ignore the error. Contact your sys admin.", t);
            }

            try {
                // IPTOS_LOWCOST (0x02) IPTOS_RELIABILITY (0x04) IPTOS_THROUGHPUT (0x08) IPTOS_LOWDELAY (0x10)
                s.setTrafficClass(0x04 | 0x08 | 0x010);
            } catch (Throwable t) {
                logger.log(Level.WARNING,
                        "[ FDTServer ] [ AcceptableTask ] Cannot set traffic class for "
                                + sc
                                + "[ IPTOS_RELIABILITY (0x04) | IPTOS_THROUGHPUT (0x08) | IPTOS_LOWDELAY (0x10) ] Will ignore the error. Contact your sys admin.",
                        t);
            }
        }

        final ByteBuffer firstByte = ByteBuffer.allocate(1);
        final ByteBuffer clientIDBuff = ByteBuffer.allocate(16);

        Selector tmpSelector = null;
        SelectionKey sk = null;

        configureSocket(firstByte, clientIDBuff, tmpSelector, sk);
    }

    private void configureSocket(ByteBuffer firstByte, ByteBuffer clientIDBuff, Selector tmpSelector, SelectionKey sk) {
        UUID clientSessionID;
        try {

            int count = -1;
            while (firstByte.hasRemaining()) {
                count = sc.read(firstByte);
                if (count < 0) {
                    logger.log(Level.WARNING, "[ FDTServer ] [ AcceptableTask ] Unable to read header for socket [ " + s
                            + " ] The stream will be closed.");
                    try {
                        sc.close();
                    } catch (Throwable ignored) {
                        // ignore
                    }
                    return;
                }

                if (firstByte.hasRemaining()) {
                    tmpSelector = Selector.open();
                    sk = sc.register(tmpSelector, SelectionKey.OP_READ);
                    tmpSelector.select();
                }
            }

            if (sk != null) {
                sk.cancel();
                sk = null;
            }

            firstByte.flip();
            final byte firstB = firstByte.get();

            switch (firstB) {

                // Control channel
                case 0: {
                    if (config.isGSIModeEnabled() || config.isGSISSHModeEnabled()) {
                        logger.log(Level.WARNING, "[ FDTServer ] [ AcceptableTask ] Got a remote control channel [ " + s
                                + " ] but in GSI mode ... will be rejected.");
                        try {
                            byte[] key = {0};
                            sc.write(ByteBuffer.wrap(key));
                            sc.close();
                        } catch (Throwable ignored) {
                            // ignore
                        }
                        return;
                    }

                    sc.configureBlocking(true);
                    ControlChannel ct = null;

                    try {
                        ct = new ControlChannel(s, fdtSessionManager);
                        // fdtSessionID = ct.fdtSessionID();
                        fdtSessionManager.addFDTClientSession(ct);
                    } catch (Throwable t) {
                        logger.log(Level.WARNING, "[ FDTServer ] [ AcceptableTask ] Cannot instantiate ControlChannel", t);
                        ct = null;
                    }

                    if (ct != null) {
                        new Thread(ct, "ControlChannel thread for [ " + s.getInetAddress() + ":" + s.getPort() + " ]").start();
                    }
                    break;
                }

                // Worker channel
                case 1: {
                    if (config.isBlocking()) {
                        sc.configureBlocking(true);
                    } else {
                        sc.configureBlocking(false);
                    }

                    while (clientIDBuff.hasRemaining()) {
                        count = sc.read(clientIDBuff);
                        if (count < 0) {
                            logger.log(Level.WARNING, "[ FDTServer ] [ AcceptableTask ] Unable to read clientID. The stream will be closed");
                            Utils.closeIgnoringExceptions(sc);
                            return;
                        }

                        if (clientIDBuff.hasRemaining()) {
                            // THIS CANNOT?!? happen in blocking mode ( JVM should throw exception if this happens
                            // ... but )
                            if (config.isBlocking()) {
                                logger.log(Level.WARNING,
                                        "[ FDTServer ] [ AcceptableTask ] Blocking mode ... unable to read clientID. The stream will be closed");
                                Utils.closeIgnoringExceptions(sc);
                                return;
                            }
                        } else {
                            // everything has been read
                            break;
                        }

                        if (tmpSelector == null) {
                            tmpSelector = Selector.open();
                        }

                        if (!config.isBlocking()) {
                            sk = sc.register(tmpSelector, SelectionKey.OP_READ);
                            tmpSelector.select();
                        }
                    }// while

                    if (sk != null) {
                        sk.cancel();
                    }

                    clientIDBuff.flip();
                    clientSessionID = new UUID(clientIDBuff.getLong(), clientIDBuff.getLong());
                    if (logger.isLoggable(Level.FINE)) {
                        logger.log(Level.FINE, "[ FDTServer ] [ AcceptableTask ] New socket from clientID: " + clientSessionID);
                    }

                    fdtSessionManager.addWorker(clientSessionID, sc);
                    break;
                }

                // Ping channel - RTT
                case 2: {
                    break;
                }

                // GUI special (sounds special, doesn't it) channel
                case 3: {
                    sc.configureBlocking(true);
                    ServerSessionManager sm = null;
                    try {
                        sm = new ServerSessionManager(s);
                        new Thread(sm, "GUIControlChannel thread for [ " + s.getInetAddress() + ":" + s.getPort() + " ]").start();
                    } catch (Throwable t) {
                        logger.log(Level.WARNING, "[ FDTServer ] [ AcceptableTask ] Cannot instantiate GUI ControlChannel", t);
                    }
                    break;
                }

                default: {
                    logger.log(Level.WARNING, "[ FDTServer ] [ AcceptableTask ] Unable to understand initial cookie: " + firstB);
                    Utils.closeIgnoringExceptions(s);
                    return;
                }
            }

        } catch (Throwable t) {
            logger.log(Level.WARNING, "[ FDTServer ] [ AcceptableTask ] Exception: ", t);
            Utils.closeIgnoringExceptions(sc);
        } finally {
            if (logger.isLoggable(Level.FINER)) {
                logger.log(Level.FINER, " AcceptableTask for " + s + " FINISHED!");
            }
            Utils.closeIgnoringExceptions(tmpSelector);
        }
    }
}
