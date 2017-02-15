/*
 * $Id: JIperfServer.java 361 2007-08-16 14:55:41Z ramiro $
 */
package lia.util.net.jiperf;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.logging.Logger;

/**
 * 
 * This will be kept for history :). 
 * The entire package lia.util.net.jiperf is the very first version of FDT. It
 * started as an Iperf-like test for Java.
 * 
 * @author ramiro
 */
public class JIperfServer {

	/** Logger used by this class */
	private static final transient Logger logger = Logger.getLogger(JIperfServer.class.getName());

	ServerSocketChannel ssc;

	ServerSocket ss;

	Selector sel;

	int port;

	ByteBufferPool buffPool;

	ExecutorService executor;

	/** -- SSH Mode fields -- */
	boolean sshMode = false;

	/**
	 * the Client/Gateway IP that this server restricts connections from if not set, the connections will not be checked against this
	 */
	String allowedIP = null;

	/**
	 * the number of connection that the server accept from the {@linkplain allowedIP}
	 */
	int connectionNo;

	/**
	 * window size on client side (server should consider this when accepting connections?)
	 */
	int windowSize;

	class ReaderTask implements Runnable {

		SelectionKey sk;

		ByteBuffer buff;

		ReaderTask(SelectionKey sk) {
			this.sk = sk;
			// take a free buffer from the pool
			buff = buffPool.get();
		}

		private void readData() throws Exception {
			buff.clear();
			SocketChannel sc = (SocketChannel) sk.channel();
			int count = -1;
			while ((count = sc.read(buff)) > 0) {
				// TODO - in the future pass this to a "listener" which will do something useful with this buffer
				buff.clear();
			}

			if (count < 0) {
				sc.close();
			} else {
				sk.interestOps(sk.interestOps() | SelectionKey.OP_READ);
			}

			sel.wakeup();

		}// readData()

		public void run() {
			if (sk == null)
				return;
			try {
				readData();
			} catch (Throwable t) {
				t.printStackTrace();
			} finally {
				// *ALWAYS* return the buffer to the pool whatever happens
				buffPool.put(buff);
			}
		}
	}// ReaderTask class

	private void init() throws Exception {
		buffPool = ByteBufferPool.getInstance();
		executor = JIperf.getExecutor();

		ssc = ServerSocketChannel.open();
		ssc.configureBlocking(false);

		ss = ssc.socket();
		ss.bind(new InetSocketAddress(port));

		sel = Selector.open();
		ssc.register(sel, SelectionKey.OP_ACCEPT);
	}

	private void initSSH() throws Exception {
		// stdin,stdout are tunneled through SSH and used as control in/out streams
		java.io.BufferedReader stdin = new java.io.BufferedReader(new java.io.InputStreamReader(System.in));
		try {
			System.out.println("ACK1");
			allowedIP = stdin.readLine();
			port = Integer.parseInt(stdin.readLine());
			connectionNo = Integer.parseInt(stdin.readLine());
			windowSize = Integer.parseInt(stdin.readLine());
			System.err.println("Conection parameters received: IP: " + allowedIP + " PORT: " + port + " STREAMS: " + connectionNo + " WSIZE: " + windowSize);
			init();
			System.out.println("ACK2");
		} catch (Throwable t) {
			System.err.println("Invalid connection parameters" + t.getMessage());
			ssc.close();
			ss.close();
			System.exit(1);
		}
		/** --SSH mode-- */
	}

	public JIperfServer(final HashMap<String, String> config) throws Exception {
		/** --SSH mode-- */
		/* we start the remote jiperf server */
		if (config.containsKey("-ssh")) {
			sshMode = true;
			initSSH();
		} else {
			port = Integer.parseInt(config.get("-p"));
			init();
		}
	}
	
	public void doWork() throws Exception {
		for (;;) {
			//TODO, stop the server (this loop and the executor) if there are no more connected sockets
			while (sel.select() > 0)
				;
			Iterator it = sel.selectedKeys().iterator();
			while (it.hasNext()) {
				SelectionKey sk = (SelectionKey) it.next();

				if (sk.isAcceptable()) {
					ServerSocketChannel ssc = (ServerSocketChannel) sk.channel();
					SocketChannel sc = ssc.accept();
					if (!sshMode) {// standalone mode
						sc.configureBlocking(false);
						sc.register(sel, SelectionKey.OP_READ);
					} else {// SSH mode
						if (allowedIP != null && !allowedIP.equals(sc.socket().getInetAddress().getHostAddress())) {
							// just the IP passed on secured SSH control connection is allowed to connect
							System.err.println(" [" + allowedIP + "] does not match " + sc.socket().getInetAddress().getHostAddress());
							sc.close();
						} else {// allowed connection
							sc.configureBlocking(false);
							sc.register(sel, SelectionKey.OP_READ);
							if (--connectionNo == 0) {
								// stop listening for other connection
								this.ssc.keyFor(sel).cancel();
								this.ssc.close();								
							}
						}
					}
				} else if (sk.isReadable()) {
					sk.interestOps(sk.interestOps() & ~SelectionKey.OP_READ);
					executor.execute(new ReaderTask(sk));
				}

				it.remove();
			}
		}
	
	
	}
}
