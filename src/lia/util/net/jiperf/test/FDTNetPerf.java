/*
 * $Id$
 */
package lia.util.net.jiperf.test;

import lia.util.net.common.Config;
import lia.util.net.common.Utils;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The simplest JIperf blocking Client/Server ... used only for testing
 *
 * @author ramiro
 */
public class FDTNetPerf {

    private static final ExecutorService execThPool = Utils.getStandardExecService("ExecService", 3, 100, Thread.MAX_PRIORITY);
    private static final ScheduledThreadPoolExecutor monitorThPool = Utils.getSchedExecService("MonitorService", 1, Thread.MIN_PRIORITY);
    private static int port = 54320;
    private static AtomicLong totalBytes = new AtomicLong(0);
    private static int byteBufferSize = 512 * 1024;
    private static int buffCount = 1;

    private FDTNetPerf(Map<String, Object> argsMap) throws Exception {
        monitorThPool.scheduleWithFixedDelay(new FDTNetPerfMonitorTask(), 1, 2, TimeUnit.SECONDS);
        Object host = argsMap.get("-c");
        if (host != null) {
            execThPool.execute(new FDTNetPerfClient(host.toString(), port));
        } else {
            execThPool.execute(new FDTNetPerfServer());
        }
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        Map<String, Object> argsMap = Utils.parseArguments(args, Config.SINGLE_CMDLINE_ARGS);

        byteBufferSize = Utils.getIntValue(argsMap, "-bs", byteBufferSize);
        buffCount = Utils.getIntValue(argsMap, "-bn", buffCount);
        port = Utils.getIntValue(argsMap, "-p", port);

        try {
            new FDTNetPerf(argsMap);
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(1);
        }

        for (; ; ) {
            try {
                Thread.sleep(10000000);
            } catch (Throwable ignore) {
            }
        }

    }

    private static class FDTNetPerfServer implements Runnable {

        private ServerSocketChannel ssc;

        FDTNetPerfServer() throws Exception {
            ssc = ServerSocketChannel.open();
            ssc.configureBlocking(true);

            ssc.socket().bind(new InetSocketAddress(port));
        }

        public void run() {
            try {
                SocketChannel sc = ssc.accept();
                sc.configureBlocking(true);
                execThPool.execute(new FDTNetPerfClient(sc));
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }

    private static class FDTNetPerfClient implements Runnable {
        SocketChannel sc;
        //        private ByteBuffer buff = ByteBuffer.allocateDirect(8 * 1024 * 1024);
//        private ByteBuffer buff = ByteBuffer.allocateDirect(512 * 1024);
        private ByteBuffer[] buffs;

        private boolean shouldWrite;

        private FDTNetPerfClient() throws Exception {
            int bCount = 0;
            ArrayList<ByteBuffer> buffsList = new ArrayList<ByteBuffer>(buffCount);
            try {
                for (int i = 0; i < buffCount; i++) {
                    buffsList.add(ByteBuffer.allocateDirect(byteBufferSize));
                    bCount++;
                }
            } catch (OutOfMemoryError oom) {
                System.out.println("Reached OOM while alocating buffers. Allocated " + bCount + " buffers");
            }

            if (bCount > 0) {
                buffs = buffsList.toArray(new ByteBuffer[buffsList.size()]);
                System.out.println("buffs.size() = " + (buffs.length * byteBufferSize) / 1024 + " KB");
            } else {
                throw new Exception("Cannot instantiate the buff pool");
            }
        }

        FDTNetPerfClient(String host, int port) throws Exception {
            this();
            this.sc = SocketChannel.open();
            this.sc.configureBlocking(true);
            this.sc.socket().connect(new InetSocketAddress(InetAddress.getByName(host), port));
            shouldWrite = true;
        }

        FDTNetPerfClient(SocketChannel sc) throws Exception {
            this();
            this.sc = sc;
            shouldWrite = false;
        }

        public void run() {
            try {
                for (; ; ) {
                    if (shouldWrite) {
                        for (ByteBuffer buff : buffs) {
                            buff.position(0);
                            buff.limit(buff.capacity());
                        }
                        totalBytes.addAndGet(sc.write(buffs));
                    } else {
                        for (ByteBuffer buff : buffs) {
                            buff.clear();
                        }
                        totalBytes.addAndGet(sc.read(buffs));
                    }
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }

    private static class FDTNetPerfMonitorTask implements Runnable {

        private long lastCount;
        private long lastRun;

        public void run() {

            final long currentCount = totalBytes.get();
            final long now = System.currentTimeMillis();

            if (lastRun > 0) {
                long diff = (currentCount - lastCount);
                double speed = diff * 8D / (now - lastRun);
                System.out.println(new Date() + " CurentSpeed: " + speed / 1000 + " Mb/s");
            }

            lastRun = now;
            lastCount = currentCount;
        }
    }

}
