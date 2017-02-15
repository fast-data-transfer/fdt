package lia.util.net.copy;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import lia.gsi.FDTGSIServer;
import lia.util.net.common.AbstractFDTCloseable;
import lia.util.net.common.Config;
import lia.util.net.common.NetMatcher;
import lia.util.net.common.Utils;
import lia.util.net.copy.transport.ControlChannel;


public class FDTServer extends AbstractFDTCloseable {

	private static final Logger logger = Logger.getLogger(FDTServer.class.getName());
	private static final Config config = Config.getInstance();
	private static final FDTSessionManager fdtSessionManager = FDTSessionManager.getInstance();

	static class FDTServerMonitorTask implements Runnable {

		public void run() {
			
		}
	}

	AtomicInteger cWorkers = new AtomicInteger(0);

	ServerSocketChannel ssc;
	ServerSocket ss;
	Selector sel;
	int port;
	ExecutorService executor;

	private class AcceptableTask implements Runnable {
		SocketChannel sc;

		AcceptableTask(SocketChannel sc) {
			this.sc = sc;
		}

		public void run() {

			if (!FDTServer.this.filterSourceAddress(sc.socket()))
				return;

			if (logger.isLoggable(Level.FINER)) {
				logger.log(Level.FINER, " AcceptableTask for " + sc + " STARTED!");
			}

			try {
				sc.socket().setKeepAlive(true);
			} catch (Throwable t) {
				logger.log(Level.WARNING, " Cannot set KEEP_ALIVE for " + sc, t);
			}

			boolean control = false;
			ByteBuffer firstByte = ByteBuffer.allocate(1);
			ByteBuffer clientIDBuff = ByteBuffer.allocate(16);

			UUID clientSessionID;
			Selector tmpSelector = null;
			SelectionKey sk = null;

			try {

				
				int count = -1;
				while (firstByte.hasRemaining()) {
					count = sc.read(firstByte);
					if (count < 0) {
						try {
							sc.close();
						} catch (Throwable t1) {
						}
						;
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
				byte fb = firstByte.get();

				if (fb == 0) {
					control = true;
				}

				if (!control) {
					if (config.isBlocking()) {
						sc.configureBlocking(true);
					} else {
						sc.configureBlocking(false);
					}

					
					
					while (clientIDBuff.hasRemaining()) {
						count = sc.read(clientIDBuff);
						if (count < 0) {
							try {
								sc.close();
							} catch (Throwable t1) {
							}
							;
							return;
						}

						if (clientIDBuff.hasRemaining()) {
							
							if (config.isBlocking()) {
								logger.log(Level.WARNING, " Blocking mode ... unable to read clientID. The socket will be closed");
								try {
									sc.close();
								} catch (Throwable t1) {
								}
								;
								return;
							}
						} else {
							
							break;
						}

						if (tmpSelector == null) {
							tmpSelector = Selector.open();
						}

						if (!config.isBlocking()) {
							sk = sc.register(tmpSelector, SelectionKey.OP_READ);
							tmpSelector.select();
						}
					}

					if (sk != null) {
						sk.cancel();
					}

					clientIDBuff.flip();
					clientSessionID = new UUID(clientIDBuff.getLong(), clientIDBuff.getLong());
					if (logger.isLoggable(Level.FINE)) {
						logger.log(Level.FINE, "new socket from clientID: " + clientSessionID);
					}

					fdtSessionManager.addWorker(clientSessionID, sc);
				} else {
                    
                    if(config.isGSIModeEnabled() || config.isGSISSHModeEnabled()) {
                        logger.log(Level.INFO, " Got a remote control channel [ " + sc.socket() +" ] but in GSI mode ... will be rejected.");
                        try {
                            sc.close();
                        } catch (Throwable ignore) {

                        }
                        return;
                    }
                    
					sc.configureBlocking(true);
					ControlChannel ct = null;

					try {
						ct = new ControlChannel(sc.socket(), fdtSessionManager);
						fdtSessionManager.addFDTClientSession(ct);
					} catch (Throwable t) {
						logger.log(Level.WARNING, "Cannot instantiate ControlChannel", t);
						ct = null;
					}

					if (ct != null) {
						new Thread(ct, "ControlChannel thread for [ " + sc.socket().getInetAddress() + ":" + sc.socket().getPort() + " ]").start();
					}
				}
			} catch (Throwable t) {
				logger.log(Level.WARNING, " Got exception in AcceptableTask", t);
				if (sc != null) {
					try {
						sc.close();
					} catch (Throwable ignore) {

					}
					;
				}
			} finally {
				if (logger.isLoggable(Level.FINER)) {
					logger.log(Level.FINER, " AcceptableTask for " + sc + " FINISHED!");
				}
				if (tmpSelector != null) {
					try {
						tmpSelector.close();
					} catch (Throwable ingnore) { 
					}
				}
			}

		}
	}

	public FDTServer() throws Exception {

		
		executor = Utils.getStandardExecService("[ Acceptable ServersThreadPool ] ", 5, 10, new ArrayBlockingQueue<Runnable>(10), Thread.NORM_PRIORITY - 2);
		port = config.getPort();
		init();

		
		ScheduledExecutorService monitoringService = Utils.getMonitoringExecService();

		monitoringService.scheduleWithFixedDelay(new FDTServerMonitorTask(), 10, 10, TimeUnit.SECONDS);

		
		
		System.out.println("READY");
	}

	private void init() throws Exception {
		ssc = ServerSocketChannel.open();
		ssc.configureBlocking(false);

		ss = ssc.socket();
		ss.bind(new InetSocketAddress(port));

		sel = Selector.open();
		ssc.register(sel, SelectionKey.OP_ACCEPT);
		
        if(config.isGSIModeEnabled()) {
            FDTGSIServer gsiServer = new FDTGSIServer(config.getGSIPort());
            gsiServer.start();
        }
	}

	public boolean filterSourceAddress(java.net.Socket socket) {
		
		final NetMatcher filter = this.config.getSourceAddressFilter();
		if (filter != null) {
			logger.info("Enforcing source address filter: "+filter);
			final String sourceIPAddress = socket.getInetAddress().getHostAddress();
			if (!filter.matchInetNetwork(sourceIPAddress)) {
				try {
					socket.close();
				} catch (java.io.IOException e) {
				}
				logger.warning(" Client [" + sourceIPAddress + "] now allowed to transfer: ");
				return false;
			}
		}
		return true;

	}

	public void doWork() throws Exception {

		Thread.currentThread().setName(" FDTServer - Main loop worker ");
		logger.info("FDTServer start listening on port: " + ss.getLocalPort());

		int count = 0;

		try {
			for (;;) {

				if (!config.isStandAlone()) {
					if (fdtSessionManager.isInited() && fdtSessionManager.sessionsNumber() == 0) {
						logger.log(Level.INFO, "FDTServer will finish. No more sessions to serve.");
						return;
					}
				}
				count = sel.select(2000);

				if (count == 0)
					continue;

				Iterator<SelectionKey> it = sel.selectedKeys().iterator();
				while (it.hasNext()) {
					SelectionKey sk = it.next();
					it.remove();

					if (!sk.isValid())
						continue;

					if (sk.isAcceptable()) {

						ServerSocketChannel ssc = (ServerSocketChannel) sk.channel();
						SocketChannel sc = ssc.accept();

						
						
						executor.execute(new AcceptableTask(sc));
					}
				}
			}
		} catch (Throwable t) {
			logger.log(Level.WARNING, "[FDTServer] Exception in main loop!", t);
		} finally {
			logger.log(Level.INFO, "[FDTServer] main loop FINISHED!");
			if (executor != null) {
				executor.shutdown();
			}
		}
	}

	public static final void main(String[] args) throws Exception {
		FDTServer jncs = new FDTServer();
		jncs.doWork();
	}

	public void run() {

		
		try {
			doWork();
		} catch (Throwable t) {
			t.printStackTrace();
		}

		close(null, null);

		logger.info(" \n\n FDTServer finishes @ " + new Date().toString() + "!\n\n");

		
	}

	@Override
	protected void internalClose() {
		

	}

}
