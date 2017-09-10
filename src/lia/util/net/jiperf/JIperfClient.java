/*
 * $Id$
 */
package lia.util.net.jiperf;

import lia.util.net.jiperf.control.ControlStream;

import java.io.File;
import java.io.FileInputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This will be kept for history :).
 * The entire package lia.util.net.jiperf is the very first version of FDT. It
 * started as an Iperf-like test for Java.
 *
 * @author ramiro
 */
public class JIperfClient {

    /**
     * Logger used by this class
     */
    private static final transient Logger logger = Logger.getLogger(JIperfClient.class.getName());

    Selector sel;

    int serverPort;

    int sockNum;

    String serverHost;

    Executor executor;

    ByteBufferPool buffPool;

    // this should be the "staging" queue ... should be a "self-adjusting" buffer
    ArrayBlockingQueue<ByteBuffer> queueToSend;

    //SSH control stream
    private ControlStream control;

    public JIperfClient(final HashMap<String, String> config) throws Exception {
        serverPort = Integer.parseInt(config.get("-p"));
        serverHost = config.get("-c");

        try {
            sockNum = Integer.parseInt(config.get("-P"));
        } catch (Throwable t) {
            sockNum = 1;
        }

        /** --SSH mode-- */
		/* we start the remote jiperf server */
        if (config.containsKey("-ssh")) {
            String user;
            if (config.containsKey("-u"))
                user = config.get("-u");
            else
                user = System.getProperty("user.name");
            String command;
            if (config.containsKey("-E"))
                command = config.get("-E");
            else //TODO, maybe set the default the default command to a more generic path
                command = "java -XX:MaxDirectMemorySize=512m -cp ~/JIPERF/TEST_JAVA_IO_PERF/JPERF_NIO/bin lia.util.net.jiperf.JIperf -ssh -s";
            System.out.println(" [Client] Using SSH mode: connecting to " + user + "@" + serverHost + " start command:" + command);
            try {
                control = new ControlStream();
                control.startServer(serverHost, user, command);
                control.waitAck();
                //if NAT is in place, the user may specify the gateway IP to be allowed in JIPerf server
                String myIP = null;
                if (config.containsKey("-F"))
                    myIP = config.get("-F");
                control.sendInitCommands(myIP, serverPort, sockNum, -1);
                control.waitAck();
            } catch (Exception e) {
                System.out.println(" [Client] ERROR: " + e);
                System.exit(1);
            }
        }
        /** --SSH mode-- */

        // TODO - the size should also dynamically ajust .... depends very much on the performance
        // of the "filling" threads ;)
        queueToSend = new ArrayBlockingQueue<ByteBuffer>(ByteBufferPool.POOL_SIZE + 1);
        buffPool = ByteBufferPool.getInstance();

        if (sockNum < 1)
            sockNum = 1;
        init();
        executor = JIperf.getExecutor();

    }

    public void init() throws Exception {

        sel = Selector.open();

        InetSocketAddress addr = new InetSocketAddress(serverHost, serverPort);
        for (int i = 0; i < sockNum; i++) {
            SocketChannel sc = SocketChannel.open();

            sc.configureBlocking(false);

            System.out.println("initiating connection");

            sc.connect(addr);

            // TODO ... for the moment there is a 1-1 mapping between "filling threads" and number of sockets ...
            Thread t = new Thread(new FillingTask());
            t.setDaemon(true);
            t.start();

            while (!sc.finishConnect()) {
                // TODO - do something useful
                try {
                    Thread.sleep(100);
                } catch (Exception ex) {
                }
                ;
            }

            System.out.println("connection established");
            sc.register(sel, SelectionKey.OP_WRITE);
        }

    }

    public void flood() throws Exception {
        for (; ; ) {
            while (sel.select() > 0)
                ;

            Iterator it = sel.selectedKeys().iterator();
            while (it.hasNext()) {
                SelectionKey sk = (SelectionKey) it.next();

                if (sk.isWritable()) {
                    sk.interestOps(sk.interestOps() & ~SelectionKey.OP_WRITE);
                    executor.execute(new WriterTask(sk));
                }

                it.remove();
            }
        }

    }

    class FillingTask implements Runnable {

        FileChannel readChannel;

        FillingTask() throws Exception {
            File dev_zero = new File("/dev/zero");
            readChannel = new FileInputStream(dev_zero).getChannel();
        }

        public void run() {
            for (; ; ) {
                try {
                    ByteBuffer buff = buffPool.get();

                    readChannel.read(buff);// TODO - should check if it read buff.size()
                    buff.flip();
                    queueToSend.put(buff);

                } catch (Throwable t) {
                    logger.log(Level.WARNING, "Filling task got exc", t);
                    try {
                        Thread.sleep(50);
                    } catch (Throwable t1) {
                    }
                }
            }
        }
    }

    class WriterTask implements Runnable {

        SelectionKey sk;

        ByteBuffer buff;

        WriterTask(SelectionKey sk) {
            this.sk = sk;
            // take a free buffer from the pool
        }

        private void writeData() throws Exception {
            buff = queueToSend.take();
            SocketChannel sc = (SocketChannel) sk.channel();
            int count = -1;
            while ((count = sc.write(buff)) > 0)
                ;

            // TODO - here we should check for buff.remainig() !!!!
            if (count < 0) {
                sc.close();
            } else {
                sk.interestOps(sk.interestOps() | SelectionKey.OP_WRITE);
            }

            sel.wakeup();

        }// readData()

        public void run() {
            if (sk == null)
                return;
            try {
                writeData();
                /*try { disable flooding
					Thread.sleep(1000);
				} catch (Throwable t1) {
				}*/
            } catch (Throwable t) {
                t.printStackTrace();
            } finally {
                // *ALWAYS* return the buffer to the pool whatever happens
                buffPool.put(buff);
            }
        }
    }// WriterTask class
}
