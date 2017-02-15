
package lia.util.net.copy;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
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
import java.util.logging.Level;
import java.util.logging.Logger;

import lia.gsi.FDTGSIServer;
import lia.util.net.common.AbstractFDTCloseable;
import lia.util.net.common.Config;
import lia.util.net.common.NetMatcher;
import lia.util.net.common.Utils;
import lia.util.net.copy.transport.ControlChannel;
import lia.util.net.copy.transport.gui.ServerSessionManager;


public class FDTServer extends AbstractFDTCloseable {

	private static final Logger logger = Logger.getLogger(FDTServer.class.getName());
	private static final Config config = Config.getInstance();
	private static final FDTSessionManager fdtSessionManager = FDTSessionManager.getInstance();

	static class FDTServerMonitorTask implements Runnable {

		public void run() {
			
		}
	}

	ServerSocketChannel ssc;
	ServerSocket ss;
	Selector sel;
	int port;
	ExecutorService executor;

	private static final class AcceptableTask implements Runnable {
		final SocketChannel sc;
		final Socket s;
		
		AcceptableTask(final SocketChannel sc) {
		    
		    if(sc == null) {
		        throw new NullPointerException("SocketChannel cannot be null in AcceptableTask");
		    }

		    if(sc.socket() == null) {
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

			try {
				s.setKeepAlive(true);
			} catch (Throwable t) {
				logger.log(Level.WARNING, "[ FDTServer ] [ AcceptableTask ] Cannot set KEEP_ALIVE for " + sc, t);
			}

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
                        logger.log(Level.WARNING, "[ FDTServer ] [ AcceptableTask ] Unable to read header for socket [ " + s + " ] The stream will be closed.");
						try {
							sc.close();
						} catch (Throwable _) {
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

				switch(firstB) {
				    
				    
				    case 0: {
	                    if(config.isGSIModeEnabled() || config.isGSISSHModeEnabled()) {
	                        logger.log(Level.WARNING, "[ FDTServer ] [ AcceptableTask ] Got a remote control channel [ " + s +" ] but in GSI mode ... will be rejected.");
	                        try {
	                            sc.close();
	                        } catch (Throwable _) {

	                        }
	                        return;
	                    }
	                    
	                    sc.configureBlocking(true);
	                    ControlChannel ct = null;

	                    try {
	                        ct = new ControlChannel(s, fdtSessionManager);
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
	                            try {
	                                sc.close();
	                            } catch (Throwable t1) {
	                            }
	                            return;
	                        }

	                        if (clientIDBuff.hasRemaining()) {
	                            
	                            if (config.isBlocking()) {
	                                logger.log(Level.WARNING, "[ FDTServer ] [ AcceptableTask ] Blocking mode ... unable to read clientID. The stream will be closed");
	                                try {
	                                    sc.close();
	                                } catch (Throwable t1) {
	                                }
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
	                        logger.log(Level.FINE, "[ FDTServer ] [ AcceptableTask ] New socket from clientID: " + clientSessionID);
	                    }

	                    fdtSessionManager.addWorker(clientSessionID, sc);
				        break;
				    }
				    
				    
				    case 2: {
				        break;
				    }
				    
				    
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
				}

			} catch (Throwable t) {
				logger.log(Level.WARNING, "[ FDTServer ] [ AcceptableTask ] Exception: ", t);
				try {
				    sc.close();
				} catch (Throwable _) {

				}
			} finally {
				if (logger.isLoggable(Level.FINER)) {
					logger.log(Level.FINER, " AcceptableTask for " + s + " FINISHED!");
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

	public static final boolean filterSourceAddress(java.net.Socket socket) {
		
		final NetMatcher filter = config.getSourceAddressFilter();
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
						
						try {
	                        executor.execute(new AcceptableTask(sc));
						} catch(Throwable t) {
						    StringBuilder sb = new StringBuilder();
						    sb.append("[ FDTServer ] got exception in while sumbiting the AcceptableTask for SocketChannel: ").append(sc);
						    if(sc != null) {
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
		    logger.log(Level.WARNING, "[ FDTServer ] exception main loop", t);
		    close("[ FDTServer ] exception main loop", t);
		}

		close(null, null);

		logger.info(" \n\n FDTServer finishes @ " + new Date().toString() + "!\n\n");
	}

	@Override
	protected void internalClose() {
		

	}

}
