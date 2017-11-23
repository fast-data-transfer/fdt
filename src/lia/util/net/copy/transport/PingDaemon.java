/*
 * $Id$
 */
package lia.util.net.copy.transport;

import lia.util.net.common.Utils;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Should provide RTT between peers
 *
 * @author ramiro
 */
public class PingDaemon {

    private static final Logger logger = Logger.getLogger("lia.util.net.copy.transport.PingDaemon");
    private static final int DEFAULT_CONNECT_DELAY = 20;
    private static final int DEFAULT_PING_DELAY = 20;
    private static final TimeUnit DEFAULT_PING_DELAY_TIME_UNIT = TimeUnit.SECONDS;
    private static final int DEFAULT_PACKET_SIZE = 1500;//bytes
    private static final int FDT_PING = 1;
    private static final int FDT_PING_REPLY = 2;
    private long pingDelay;
    private TimeUnit pingDelayTimeUnit;
    private SocketChannel sc;
    private int packetSize;
    private ByteBuffer header;
    private ByteBuffer payload;
    private long SEQ = 0;
    private ByteBuffer[] toSend = new ByteBuffer[2];

    public PingDaemon(SocketChannel sc) throws Exception {

        sc.configureBlocking(true);
        this.sc = sc;
        initChannels();

        new PingServer().start();
    }

    public PingDaemon(InetAddress inetAddr, int port) throws Exception {
        this(inetAddr, port, DEFAULT_CONNECT_DELAY, DEFAULT_PING_DELAY, DEFAULT_PING_DELAY_TIME_UNIT, DEFAULT_PACKET_SIZE);
    }

    public PingDaemon(InetAddress inetAddr, int port, long pingDelay,
                      TimeUnit pingDelayTimeUnit, int packetSize) throws Exception {
        this(inetAddr, port, DEFAULT_CONNECT_DELAY, pingDelay, pingDelayTimeUnit, packetSize);
    }

    public PingDaemon(InetAddress inetAddr, int port, int connectDelay, long pingDelay,
                      TimeUnit pingDelayTimeUnit, int packetSize) throws Exception {

        this.packetSize = packetSize;
        this.pingDelay = pingDelay;
        this.pingDelayTimeUnit = pingDelayTimeUnit;

        sc = SocketChannel.open();
        sc.configureBlocking(true);

        sc.socket().connect(new InetSocketAddress(inetAddr, port), 30 * 1000);

        initChannels();

        payload = ByteBuffer.allocateDirect(DEFAULT_PACKET_SIZE - header.capacity());

        header.putInt(FDT_PING);
        header.putInt(1);
        header.putInt(payload.capacity());

        toSend[0] = header;
        toSend[1] = payload;

        Utils.getMonitoringExecService().scheduleWithFixedDelay(new PingerTask(), 5, 20, TimeUnit.SECONDS);

    }

    public static final void main(String[] args) throws Exception {

        try {
            //Start in server mode
            if (args == null || args.length == 0) {
            } else if (args.length == 1) {
                final int port = Integer.parseInt(args[0]);
                ServerSocketChannel ssc = ServerSocketChannel.open();
                ssc.configureBlocking(true);
                ssc.socket().bind(new InetSocketAddress(port));

                try {
                    SocketChannel sc = ssc.accept();
                    new PingDaemon(sc);
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            } else if (args.length == 2) {
                InetAddress ia = InetAddress.getByName(args[0]);
                final int port = Integer.parseInt(args[1]);
                new PingDaemon(ia, port);
            }
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(1);
        }


        for (; ; ) {
            try {
                Thread.sleep(100000);
            } catch (Throwable t1) {
            }
        }
    }

    private void initChannels() throws Exception {

        header = ByteBuffer.allocateDirect(20);

    }

    public void setDelay(long pingDelay, TimeUnit pingDelayTimeUnit) {
        this.pingDelay = pingDelay;
        this.pingDelayTimeUnit = pingDelayTimeUnit;
    }

    public void setPacketSize(int packetSize) {
        this.packetSize = packetSize;
    }

    private final class PingServer extends Thread {

        public PingServer() {
            super(" (FDT) Ping Daemon");
            this.setDaemon(true);
        }

        public void run() {

            for (; ; ) {
                try {

                    if (!sc.isOpen() || sc.socket().isClosed()) {
                        break;
                    }

                    header.clear();
                    if (sc.read(header) == -1) {
                        break;
                    }

                    header.flip();

                    final int type = header.getInt();
                    final int version = header.getInt();
                    final int payloadSize = header.getInt();
                    final long seq = header.getLong();

                    System.out.println("Read from client: [ SEQ: " + seq + " type: " + type + " version: " + version + " payloadSize: " + payloadSize + " ]");

                    if (payload == null || payload.capacity() != payloadSize) {
                        payload = ByteBuffer.allocateDirect(payloadSize);
                        toSend[0] = header;
                        toSend[1] = payload;
                    }

                    payload.clear();
                    sc.read(payload);

                    header.flip();
                    header.putInt(FDT_PING_REPLY);

                    header.position(header.capacity());
                    payload.position(payload.capacity());

                    header.flip();
                    payload.flip();

                    sc.write(toSend);

                } catch (Throwable t) {
                    logger.log(Level.WARNING, "Got exception ", t);
                }
            }//end for

            System.out.println("Server exits!");
        }
    }

    private final class PingerTask implements Runnable {

        public void run() {
            try {

                header.position(12);
                header.putLong(SEQ++);
                header.position(header.capacity());

                payload.position(payload.capacity());
                header.flip();
                payload.flip();

                final long sTime = System.nanoTime();
                System.out.println(" Written: " + sc.write(toSend));

                header.clear();
                payload.clear();

                sc.read(toSend);
                final long finishTime = System.nanoTime();

                System.out.println(" DT = " + (finishTime - sTime) / (1000D * 1000D) + " ms");
            } catch (Throwable t) {
                logger.log(Level.WARNING, "Got exception", t);
            }
        }
    }
}
