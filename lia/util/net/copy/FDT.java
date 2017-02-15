package lia.util.net.copy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Properties;
import java.util.Vector;
import java.util.concurrent.TimeUnit;
import java.util.logging.LogManager;

import lia.util.net.common.AbstractFDTCloseable;
import lia.util.net.common.Config;
import lia.util.net.common.ControlStream;
import lia.util.net.common.DirectByteBufferPool;
import lia.util.net.common.HeaderBufferPool;
import lia.util.net.common.InvalidFDTParameterException;
import lia.util.net.common.SSHControlStream;
import lia.util.net.common.Utils;
import lia.util.net.copy.monitoring.FDTInternalMonitoringTask;
import lia.util.net.copy.monitoring.lisa.LISAReportingTask;
import lia.util.net.copy.transport.FDTProcolException;
import lia.util.net.copy.transport.internal.SelectionManager;
import apmon.ApMon;

public class FDT {

	private static Config config;
	private static Properties localProps = new Properties();

	private static final void initLocalProps(String level) {

		FileInputStream fis = null;
		File confFile = null;
		try {
			confFile = new File(System.getProperty("user.home") + File.separator + ".fdt" + File.separator + "fdt.properties");
			if (level.indexOf("FINE") >= 0) {
				System.out.println("Using local properties file: " + confFile);
			}
			if (confFile != null && confFile.exists() && confFile.canRead()) {
				fis = new FileInputStream(confFile);
				localProps.load(fis);
			}
		} catch (Throwable t) {
			if (confFile != null) {
				if (level.indexOf("FINE") >= 0) {
					System.err.println("Unable to read local configuration file " + confFile);
					t.printStackTrace();
				}
			}
		} finally {
			if (fis != null) {
				try {
					fis.close();
				} catch (Throwable ignore) {
				}
			}
		}

		if (level.indexOf("FINE") >= 0) {
			if (localProps.size() > 0) {
				if (level.indexOf("FINER") >= 0) {
					System.out.println(" LocalProperties loaded: " + localProps);
				}
			} else {
				System.out.println("No local properties defined");
			}
		}
	}

	private static final void initLogger(String level) {
		initLocalProps(level);
		Properties loggingProps = new Properties();
		loggingProps.putAll(localProps);

		try {

			if (!loggingProps.containsKey("handlers")) {
				loggingProps.put("handlers", "java.util.logging.ConsoleHandler");
				loggingProps.put("java.util.logging.ConsoleHandler.level", "FINEST");
				loggingProps.put("java.util.logging.ConsoleHandler.formatter", "java.util.logging.SimpleFormatter");
			}

			if (!loggingProps.containsKey(".level")) {
				loggingProps.put(".level", level);
			}

			if (level.indexOf("FINER") >= 0) {
				System.out.println("\n Logging props: " + loggingProps);
			}

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			loggingProps.store(baos, null);
			LogManager.getLogManager().reset();
			LogManager.getLogManager().readConfiguration(new ByteArrayInputStream(baos.toByteArray()));

		} catch (Throwable t) {
			System.err.println(" Got exception setting the logging level ");
			t.printStackTrace();
		}
	}

	String mlDestinations = "monalisa.cern.ch:8884";

	FDT() throws Exception {

		
		if (config.withApMon()) {
			long lStart = System.currentTimeMillis();
			Vector<String> vHosts = new Vector<String>();
			Vector<Integer> vPorts = new Vector<Integer>();
			for (String host_port : mlDestinations.split(" ")) {
				int index = -1;
				String host;
				int port;
				if ((index = host_port.indexOf(':')) != -1) {
					host = host_port.substring(0, index);
					try {
						port = Integer.parseInt(host_port.substring(index + 1));
					} catch (Exception ex) {
						port = 8884;
					}
				} else {
					host = host_port;
					port = 8884;
				}
				vHosts.add(host);
				vPorts.add(port);
			}
			try {
				ApMon apmon = null;
				ApMon.setLogLevel("DEBUG");
				apmon = new ApMon(vHosts, vPorts);
				apmon.setConfRecheck(false, -1);
				apmon.setGenMonitoring(true, 30);
				
				
				String cluster_name = "";
				String node_name = "";
				if (config.getHostName() != null) {
					cluster_name = "Clients";
					node_name = config.getHostName();
				} else {
					cluster_name = "Servers";
					node_name = apmon.getMyHostname();
				}
				apmon.setMonitorClusterNode(cluster_name, node_name);
				
				apmon.setSysMonitoring(true, 30);
				try {
					apmon.sendParameter(cluster_name, node_name, "version", FDT_FULL_VERSION);
				} catch (Exception e) {
					System.out.println("Send operation failed: ");
					e.printStackTrace();
				}

				Utils.initApMonInstance(apmon);
			} catch (Exception ex) {
				System.err.println("Error initializing ApMon engine.");
				ex.printStackTrace();
			}
			long lEnd = System.currentTimeMillis();
			System.out.println("ApMon initialization took " + (lEnd - lStart) + " ms");
		}

		Utils.getMonitoringExecService().scheduleWithFixedDelay(FDTInternalMonitoringTask.getInstance(), 3, 5, TimeUnit.SECONDS);

		if (config.getHostName() != null) { 
			
			FDTSessionManager.getInstance().addFDTClientSession();
		} else { 
			if (!DirectByteBufferPool.initInstance(config.getByteBufferSize())) {
				
				throw new FDTProcolException("The buffer pool cannot be alredy initialized");
			}

			FDTServer theServer = null; 
			theServer = new FDTServer();

			if (theServer != null) {
				theServer.doWork();
			}
		}

	}

	private static void printHelp() {
		System.err.println(Config.getUsage());
	}

	private static String UPDATE_URL = "http://monalisa.cern.ch/FDT/lib/";
	public static final String FDT_FULL_VERSION = "0.6.1-200704021518";
	private static String date = "2007-04-02";
	private static String name = "FDT";

	private static void printVersion() {
		System.out.println(name + " " + FDT_FULL_VERSION);
	}

	private void doWork() {

		FDTSessionManager fdtSessionManager = FDTSessionManager.getInstance();

		for (;;) {
			try {
				Thread.sleep(2000);
				if (config.getHostName() != null) {
					if (fdtSessionManager.sessionsNumber() == 0) {
						System.exit(0);
					}
				} else {
					if (!config.isStandAlone() && fdtSessionManager.isInited() && fdtSessionManager.sessionsNumber() == 0) {
						SelectionManager.getInstance().stopIt();
						System.out.println("Server started with -S flag set and all the sessions have finished ... FDT will stop now");
						AbstractFDTCloseable stopper = new AbstractFDTCloseable() {
							protected void internalClose() throws Exception {
								synchronized (this) {
									this.notifyAll();
								}
							}
						};

						
						stopper.close(null, null);

						while (!stopper.isClosed()) {
							synchronized (stopper) {
								stopper.wait();
							}
						}

						return;
						
						
					}
				}
			} catch (Throwable t) {

			}
		}
	}

	public static final void main(String[] args) throws Exception {

		

		
		String logLevel = "INFO";

		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-v")) {
				logLevel = "FINE";
				break;
			}

			if (args[i].equals("-vv")) {
				logLevel = "FINER";
				break;
			}

			if (args[i].equals("-vvv")) {
				logLevel = "FINEST";
				break;
			}
		}

		if (logLevel.startsWith("FIN")) {
			System.out.println(" LogLevel: " + logLevel);
		}
		initLogger(logLevel);

		HashMap<String, Object> argsMap = Utils.parseArguments(args, Config.SINGLE_ARGS);

		

		
		
		
		
		
		
		
                
		if (argsMap.get("-h") != null || argsMap.get("-H") != null 
                        || argsMap.get("-help") != null || argsMap.get("--help") != null) {
			printHelp();
			System.exit(0);
		} else if (argsMap.get("-V") != null || argsMap.get("--version") != null || argsMap.get("-version") != null) {
			printVersion();
			System.exit(0);
		} else if (argsMap.get("-u") != null || argsMap.get("-U") != null 
						|| argsMap.get("-update") != null || argsMap.get("--update") != null) {
			final Object urlS = argsMap.get("-U");
			String updateURL = UPDATE_URL;

			if (urlS != null && urlS instanceof String) {
				updateURL = (String) urlS;
				if (updateURL.length() == 0) {
					updateURL = UPDATE_URL;
				}
			}

			if (Utils.updateFDT(FDT_FULL_VERSION, updateURL)) {
				
				System.out.println("\nThe update finished successfully\n");
				System.exit(0);
			} else {
				System.out.println("\nNo updates available\n");
				System.exit(100);
			}
		}

		System.out.println("\n\n" + name + " [ " + FDT_FULL_VERSION + " ] STARTED ... \n\n");

		try {
			
			Config.initInstance(argsMap);
		} catch (InvalidFDTParameterException e) {
			System.err.println("Invalid parameters supplied:" + e.getMessage());
			System.err.flush();
			System.exit(1);
		} catch (Throwable t1) {
			System.err.println("got exception parsing command args");
			t1.printStackTrace();
			System.err.flush();
			System.exit(1);
		}

		config = Config.getInstance();

		
		
		

		processSCPSyntax(args);

		HeaderBufferPool.initInstance();

		FDT jnc = null;

		if (!config.isLisaDisabled()) {
			LISAReportingTask lrt = LISAReportingTask.initInstance(config.getLisaHost(), config.getLisaPort());
			Utils.getMonitoringExecService().scheduleWithFixedDelay(lrt, 1, config.getLisaReportingInterval(), TimeUnit.SECONDS);
		}

		try {
			jnc = new FDT();
		} catch (Throwable t) {
			t.printStackTrace();
			System.out.flush();
			System.err.flush();
			System.exit(1);
		}

		jnc.doWork();

		Utils.getMonitoringExecService().shutdownNow();
		System.out.println("DONE");
	}

	
	public static final long UPDATE_PERIOD = 2 * 24 * 3600 * 1000;

	private static void processSCPSyntax(String[] args) throws Exception {
		int iTransferConfiguration = config.getSSHConfig();
		if (iTransferConfiguration > 0) {
			ControlStream sshConn = null;
			String localAddresses;
			String remoteCmd;

			switch (iTransferConfiguration) {
			case Config.SSH_REMOTE_SERVER_LOCAL_CLIENT_PUSH:
				System.err.println("[SSH Mode] SSH_REMOTE_SERVER_LOCAL_CLIENT_PUSH");
				try {
					sshConn = config.isGSISSHModeEnabled()
							? new lia.util.net.common.GSISSHControlStream(config.getHostName(), config.getDestinationUser())
							: new SSHControlStream(config.getHostName(), config.getDestinationUser());
				} catch (NoClassDefFoundError t) {
					throw new Exception ("GSI libraries not loaded. You should set CLASSPATH accordingly!");
				}
				localAddresses = config.getLocalAddresses();
				remoteCmd = config.getRemoteCommand() + " -S -f " + localAddresses;
				System.err.println(" [ CONFIG ] Starting server through ssh using [ " + remoteCmd + " ]");
				sshConn.startProgram(remoteCmd);
				sshConn.waitForControlMessage("READY");
				System.err.println(" [ CONFIG ] FDT server successfully started on [ " + config.getHostName() + " ]");
				break;

			case Config.SSH_REMOTE_SERVER_LOCAL_CLIENT_PULL:
				System.err.println("[SSH Mode] SSH_REMOTE_SERVER_LOCAL_CLIENT_PULL");

				String remoteHost = config.getSourceHosts()[0];
				try {
					sshConn = config.isGSISSHModeEnabled()
							? new lia.util.net.common.GSISSHControlStream(config.getHostName(), config.getDestinationUser())
							: new SSHControlStream(config.getHostName(), config.getDestinationUser());
				} catch (NoClassDefFoundError t) {
					throw new Exception ("GSI libraries not loaded. You should set CLASSPATH accordingly!");
				}
				localAddresses = config.getLocalAddresses();
				remoteCmd = config.getRemoteCommand() + " -S -f " + localAddresses;
				System.err.println(" [ CONFIG ] Starting server through ssh using [ " + remoteCmd + " ]");
				sshConn.startProgram(remoteCmd);
				sshConn.waitForControlMessage("READY");
				System.err.println(" [ CONFIG ] FDT server successfully started on [ " + remoteHost + " ]");

				
				config.setPullMode();
				config.setHostName(remoteHost);
				break;

			case Config.SSH_REMOTE_SERVER_REMOTE_CLIENT_PUSH:
				System.err.println("[SSH Mode] SSH_REMOTE_SERVER_REMOTE_CLIENT_PUSH");

				String clientHost = config.getSourceHosts()[0];
				
				try {
					sshConn = config.isGSISSHModeEnabled()
							? new lia.util.net.common.GSISSHControlStream(config.getHostName(), config.getDestinationUser())
							: new SSHControlStream(config.getHostName(), config.getDestinationUser());
				} catch (NoClassDefFoundError t) {
					throw new Exception ("GSI libraries not loaded. You should set CLASSPATH accordingly!");
				}
				remoteCmd = config.getRemoteCommand() + " -S -f " + clientHost;
				System.err.println(" [ CONFIG ] Starting server through ssh using [ " + remoteCmd + " ]");
				sshConn.startProgram(remoteCmd);
				sshConn.waitForControlMessage("READY");
				System.err.println(" [ CONFIG ] FDT server successfully started on [ " + config.getHostName() + " ]");
				

				
				try {
					sshConn = config.isGSISSHModeEnabled()
							? new lia.util.net.common.GSISSHControlStream(config.getHostName(), config.getDestinationUser())
							: new SSHControlStream(config.getHostName(), config.getDestinationUser());
				} catch (NoClassDefFoundError t) {
					throw new Exception ("GSI libraries not loaded. You should set CLASSPATH accordingly!");
				}
				remoteCmd = config.getRemoteCommand();
				for (int i = 0; i < args.length; i++) {
					if (args[i].indexOf(':') < 0)
						remoteCmd += " " + args[i];
				}
				remoteCmd += " -c " + config.getHostName();
				remoteCmd += " -d " + config.getDestinationDir();
				String[] files = (String[]) config.getConfigMap().get("Files");
				remoteCmd += " " + files[0];
				System.err.println(" [ CONFIG ] Starting client through ssh using [ " + remoteCmd + " ]");
				sshConn.startProgram(remoteCmd);
				
				sshConn.waitForControlMessage("DONE", true);
				
				
				System.exit(0);
				break;
			default:
				break;
			}
		}
	}
}
