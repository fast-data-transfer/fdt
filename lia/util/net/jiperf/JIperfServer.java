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

public class JIperfServer {

	
	private static final transient Logger logger = Logger.getLogger(JIperfServer.class.getName());

	ServerSocketChannel ssc;

	ServerSocket ss;

	Selector sel;

	int port;

	ByteBufferPool buffPool;

	ExecutorService executor;

	
	boolean sshMode = false;

	
	String allowedIP = null;

	
	int connectionNo;

	
	int windowSize;

	class ReaderTask implements Runnable {

		SelectionKey sk;

		ByteBuffer buff;

		ReaderTask(SelectionKey sk) {
			this.sk = sk;
			
			buff = buffPool.get();
		}

		private void readData() throws Exception {
			buff.clear();
			SocketChannel sc = (SocketChannel) sk.channel();
			int count = -1;
			while ((count = sc.read(buff)) > 0) {
				
				buff.clear();
			}

			if (count < 0) {
				sc.close();
			} else {
				sk.interestOps(sk.interestOps() | SelectionKey.OP_READ);
			}

			sel.wakeup();

		}

		public void run() {
			if (sk == null)
				return;
			try {
				readData();
			} catch (Throwable t) {
				t.printStackTrace();
			} finally {
				
				buffPool.put(buff);
			}
		}
	}

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
		
	}

	public JIperfServer(final HashMap<String, String> config) throws Exception {
		
		
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
			
			while (sel.select() > 0)
				;
			Iterator it = sel.selectedKeys().iterator();
			while (it.hasNext()) {
				SelectionKey sk = (SelectionKey) it.next();

				if (sk.isAcceptable()) {
					ServerSocketChannel ssc = (ServerSocketChannel) sk.channel();
					SocketChannel sc = ssc.accept();
					if (!sshMode) {
						sc.configureBlocking(false);
						sc.register(sel, SelectionKey.OP_READ);
					} else {
						if (allowedIP != null && !allowedIP.equals(sc.socket().getInetAddress().getHostAddress())) {
							
							System.err.println(" [" + allowedIP + "] does not match " + sc.socket().getInetAddress().getHostAddress());
							sc.close();
						} else {
							sc.configureBlocking(false);
							sc.register(sel, SelectionKey.OP_READ);
							if (--connectionNo == 0) {
								
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
