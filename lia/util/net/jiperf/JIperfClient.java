package lia.util.net.jiperf;

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

import lia.util.net.jiperf.control.ControlStream;

public class JIperfClient {

	
	private static final transient Logger logger = Logger.getLogger(JIperfClient.class.getName());

	Selector sel;

	int serverPort;

	int sockNum;

	String serverHost;

	Executor executor;

	ByteBufferPool buffPool;

	
	ArrayBlockingQueue<ByteBuffer> queueToSend;

	
	private ControlStream control;

	class FillingTask implements Runnable {

		FileChannel readChannel;

		FillingTask() throws Exception {
			File dev_zero = new File("/dev/zero");
			readChannel = new FileInputStream(dev_zero).getChannel();
		}

		public void run() {
			for (;;) {
				try {
					ByteBuffer buff = buffPool.get();

					readChannel.read(buff);
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
			
		}

		private void writeData() throws Exception {
			buff = queueToSend.take();
			SocketChannel sc = (SocketChannel) sk.channel();
			int count = -1;
			while ((count = sc.write(buff)) > 0)
				;

			
			if (count < 0) {
				sc.close();
			} else {
				sk.interestOps(sk.interestOps() | SelectionKey.OP_WRITE);
			}

			sel.wakeup();

		}

		public void run() {
			if (sk == null)
				return;
			try {
				writeData();
				
			} catch (Throwable t) {
				t.printStackTrace();
			} finally {
				
				buffPool.put(buff);
			}
		}
	}

	public JIperfClient(final HashMap<String, String> config) throws Exception {
		serverPort = Integer.parseInt(config.get("-p"));
		serverHost = config.get("-c");

		try {
			sockNum = Integer.parseInt(config.get("-P"));
		} catch (Throwable t) {
			sockNum = 1;
		}

		
		
		if (config.containsKey("-ssh")) {
			String user;
			if (config.containsKey("-u")) 
				user = config.get("-u");
			else
				user = System.getProperty("user.name");
			String command;
			if (config.containsKey("-E")) 
				command = config.get("-E");
			else 
				command = "java -XX:MaxDirectMemorySize=512m -cp ~/JIPERF/TEST_JAVA_IO_PERF/JPERF_NIO/bin lia.util.net.jiperf.JIperf -ssh -s";
			System.out.println(" [Client] Using SSH mode: connecting to "+user+"@"+serverHost + " start command:"+command );
			try{
			control = new ControlStream();
			control.startServer(serverHost, user, command);
			control.waitAck();
			
			String myIP=null;
			if (config.containsKey("-F")) 
				myIP = config.get("-F");
			control.sendInitCommands(myIP,serverPort,sockNum,-1);
			control.waitAck();			
			}catch (Exception e) {
				System.out.println(" [Client] ERROR: "+e);
				System.exit(1);
			}
		}
		

		
		
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

			
			Thread t = new Thread(new FillingTask());
			t.setDaemon(true);
			t.start();

			while (!sc.finishConnect()) {
				
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
		for (;;) {
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
}
