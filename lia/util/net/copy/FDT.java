
package lia.util.net.copy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.Date;
import java.util.List;
import java.util.Map;
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
import lia.util.net.copy.monitoring.ApMonReportingTask;
import lia.util.net.copy.monitoring.ConsoleReportingTask;
import lia.util.net.copy.monitoring.FDTInternalMonitoringTask;
import lia.util.net.copy.monitoring.lisa.LISAReportingTask;
import lia.util.net.copy.transport.FDTProcolException;
import lia.util.net.copy.transport.internal.SelectionManager;
import apmon.ApMon;


public class FDT {

    private static final String name = "FDT";

    private static String UPDATE_URL = "http://monalisa.cern.ch/FDT/lib/";

    public static final String FDT_FULL_VERSION = "0.9.4-200907141705";

    String mlDestinations = "monalisa2.cern.ch:28884,monalisa2.caltech.edu:28884";

    
    public static final long UPDATE_PERIOD = 2 * 24 * 3600 * 1000;

    private static Config config;

    private static Properties localProps = new Properties();

    
    private final static class GracefulStopper extends AbstractFDTCloseable {

        private boolean internalClosed = false;

        protected void internalClose() throws Exception {
            synchronized (this) {
                this.internalClosed = true;
                this.notifyAll();
            }
        }
    }

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

    FDT() throws Exception {

        
        final String configApMonHosts = config.getApMonHosts();
        if (configApMonHosts != null) {
            long lStart = System.currentTimeMillis();
            ApMon apmon = null;

            final String apMonHosts = (configApMonHosts.length() > 0) ? configApMonHosts : mlDestinations;

            System.out.println("Trying to instantiate apMon to: " + apMonHosts);
            try {
                Vector<String> vHosts = new Vector<String>();
                Vector<Integer> vPorts = new Vector<Integer>();
                final String[] apMonDstTks = apMonHosts.split(",");

                if (apMonDstTks == null || apMonDstTks.length == 0) {
                    System.err.println("\n\nApMon enabled but no hosts defined! Cannot send apmon statistics\n\n");
                } else {
                    for (String host_port : apMonDstTks) {
                        int index = -1;
                        String host;
                        int port;
                        if ((index = host_port.indexOf(':')) != -1) {
                            host = host_port.substring(0, index);
                            try {
                                port = Integer.parseInt(host_port.substring(index + 1));
                            } catch (Exception ex) {
                                port = 28884;
                            }
                        } else {
                            host = host_port;
                            port = 28884;
                        }
                        vHosts.add(host);
                        vPorts.add(port);
                    }

                    ApMon.setLogLevel("WARNING");
                    apmon = new ApMon(vHosts, vPorts);
                    apmon.setConfRecheck(false, -1);
                    apmon.setGenMonitoring(true, 40);
                    
                    
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
                    
                    apmon.setSysMonitoring(true, 40);
                    try {
                        apmon.sendParameter(cluster_name, node_name, "FDT_version", FDT_FULL_VERSION);
                    } catch (Exception e) {
                        System.out.println("Send operation failed: ");
                        e.printStackTrace();
                    }

                }
            } catch (Throwable ex) {
                System.err.println("Error initializing ApMon engine.");
                ex.printStackTrace();
            } finally {
                Utils.initApMonInstance(apmon);
            }

            try {
                if (Utils.getApMon() != null) {
                    ApMonReportingTask apmrt = new ApMonReportingTask();
                    Utils.getMonitoringExecService().scheduleWithFixedDelay(apmrt, 1, config.getApMonReportingInterval(), TimeUnit.SECONDS);
                } else {
                    System.err.println("Cannot start ApMonReportingTask because apMon is null!");
                }
            } catch (Throwable t) {
                System.err.println("Cannot start ApMonReportingTask because got Exception:");
                t.printStackTrace();
            }

            long lEnd = System.currentTimeMillis();
            System.out.println("ApMon initialization took " + (lEnd - lStart) + " ms");
        }

        Utils.getMonitoringExecService().scheduleWithFixedDelay(FDTInternalMonitoringTask.getInstance(), 1, 5, TimeUnit.SECONDS);
        final long reportingTaskDelay = config.getReportingTaskDelay();
        if (reportingTaskDelay > 0) {
            Utils.getMonitoringExecService().scheduleWithFixedDelay(ConsoleReportingTask.getInstance(), 0, reportingTaskDelay, TimeUnit.SECONDS);
        }

        if (config.getHostName() != null) { 
            
            FDTSessionManager.getInstance().addFDTClientSession();
        } else { 
            if (!DirectByteBufferPool.initInstance(config.getByteBufferSize(), config.getMaxTakePollIter())) {
                
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

    private static void printVersion() {
        System.out.println(name + " " + FDT_FULL_VERSION);
    }

    private int doWork() {

        FDTSessionManager fdtSessionManager = FDTSessionManager.getInstance();

        try {
            for (;;) {
                try {
                    Thread.sleep(1000);
                    if (config.getHostName() != null && fdtSessionManager.isInited()) {
                        if (fdtSessionManager.sessionsNumber() == 0) {
                            break;
                        }

                        try {
                            fdtSessionManager.awaitTermination();
                        } catch (InterruptedException ie) {
                            Thread.interrupted();
                        }
                    } else {
                        if (!config.isStandAlone() && fdtSessionManager.isInited() && fdtSessionManager.sessionsNumber() == 0) {
                            SelectionManager.getInstance().stopIt();
                            System.out.println("Server started with -S flag set and all the sessions have finished ... FDT will stop now");
                            break;
                        }
                    }
                } catch (Throwable t) {
                    System.err.println("FDT Got exception in main loop");
                    t.printStackTrace();
                }
            }
        } finally {
            try {
                System.out.println(" [ " + new Date().toString() + " ] - GracefulStopper hook started ... Waiting for the cleanup to finish");

                GracefulStopper stopper = new GracefulStopper();

                
                stopper.close(null, null);

                while (!stopper.internalClosed) {
                    synchronized (stopper) {
                        if (stopper.internalClosed) {
                            break;
                        }
                        try {
                            stopper.wait();
                        } catch (Throwable t) {
                            t.printStackTrace();
                        }
                    }
                }
                System.out.println(" [ " + new Date().toString() + " ]  - GracefulStopper hook finished!");
            } catch (Throwable gExc) {
                System.err.println(" [GracefulStopper] Got exception stopper");
                gExc.printStackTrace();
            }
        }

        final Throwable tExit = fdtSessionManager.getLasDownCause();
        final String mExit = fdtSessionManager.getLasDownMessage();
        if (tExit != null || mExit != null) {
            System.err.println("\n [ " + new Date().toString() + " ]  FDT Session finished with errors: ");
            if (mExit != null) {
                System.err.println(mExit + "\n");
            }

            if (tExit != null) {
                System.err.println(Utils.getStackTrace(tExit) + "\n");
            }

            return 1;
        }

        System.out.println("\n [ " + new Date().toString() + " ]  FDT Session finished OK.\n");
        return 0;
    }

    private static void processSCPSyntax(String[] args) throws Exception {
        int iTransferConfiguration = config.getSSHConfig();
        if (iTransferConfiguration > 0) {
            ControlStream sshConn = null;
            String localAddresses;
            String remoteCmd;
            String[] clients;

            switch (iTransferConfiguration) {

                case Config.SSH_REMOTE_SERVER_LOCAL_CLIENT_PUSH:
                    System.err.println("[SSH Mode] SSH_REMOTE_SERVER_LOCAL_CLIENT_PUSH");
                    try {
                        sshConn = config.isGSISSHModeEnabled() ? new lia.util.net.common.GSISSHControlStream(config.getHostName(), config.getDestinationUser()) : new SSHControlStream(config.getHostName(),
                                                                                                                                                                                       config.getDestinationUser());
                    } catch (NoClassDefFoundError t) {
                        throw new Exception("GSI libraries not loaded. You should set CLASSPATH accordingly!");
                    }
                    localAddresses = config.getLocalAddresses();
                    
                    remoteCmd = config.getRemoteCommand() + " -p " + config.getPort() + " -silent -S -f " + localAddresses;
                    System.err.println(" [ CONFIG ] Starting FDT server over SSH using [ " + remoteCmd + " ]");
                    sshConn.startProgram(remoteCmd);
                    sshConn.waitForControlMessage("READY");
                    System.err.println(" [ CONFIG ] FDT server successfully started on [ " + config.getHostName() + " ]");
                    break;

                case Config.SSH_REMOTE_SERVER_LOCAL_CLIENT_PULL:
                    System.err.println("[SSH Mode] SSH_REMOTE_SERVER_LOCAL_CLIENT_PULL");
                    
                    String remoteServerHost = config.getSourceHosts()[0];
                    String remoteServerUsername = null;
                    clients = config.getSourceUsers();
                    if (clients != null && clients.length > 0 && clients[0] != null) {
                        remoteServerUsername = clients[0];
                    } else {
                        remoteServerUsername = System.getProperty("user.name", "root");
                    }
                    
                    config.setPullMode(true);
                    config.setHostName(remoteServerHost);

                    try {
                        sshConn = config.isGSISSHModeEnabled() ? new lia.util.net.common.GSISSHControlStream(remoteServerHost, remoteServerUsername) : new SSHControlStream(remoteServerHost, remoteServerUsername);
                    } catch (NoClassDefFoundError t) {
                        throw new Exception("GSI libraries not loaded. You should set CLASSPATH accordingly!");
                    }
                    localAddresses = config.getLocalAddresses();
                    
                    remoteCmd = config.getRemoteCommand() + " -p " + config.getPort() + " -silent -S -f " + localAddresses;
                    System.err.println(" [ CONFIG ] Starting FDT server over SSH using [ " + remoteCmd + " ]");
                    sshConn.startProgram(remoteCmd);
                    sshConn.waitForControlMessage("READY");
                    System.err.println(" [ CONFIG ] FDT server successfully started on [ " + remoteServerHost + " ]");
                    break;

                case Config.SSH_REMOTE_SERVER_REMOTE_CLIENT_PUSH:
                    System.err.println("[SSH Mode] SSH_REMOTE_SERVER_REMOTE_CLIENT_PUSH");
                    
                    final String clientHost = config.getSourceHosts()[0];
                    
                    try {
                        sshConn = config.isGSISSHModeEnabled() ? new lia.util.net.common.GSISSHControlStream(config.getHostName(), config.getDestinationUser()) : new SSHControlStream(config.getHostName(),
                                                                                                                                                                                       config.getDestinationUser());
                    } catch (NoClassDefFoundError t) {
                        throw new Exception("GSI libraries not loaded. You should set CLASSPATH accordingly!");
                    }
                    
                    remoteCmd = config.getRemoteCommand() + " -p " + config.getPort() + " -silent -S -f " + clientHost;
                    System.err.println(" [ CONFIG ] Starting remote FDT server over SSH using [ " + remoteCmd + " ]");
                    sshConn.startProgram(remoteCmd);
                    sshConn.waitForControlMessage("READY");
                    System.err.println(" [ CONFIG ] FDT server successfully started on [ " + config.getHostName() + " ]");
                    

                    
                    String clientUser = null;
                    clients = config.getSourceUsers();
                    if (clients != null && clients.length > 0 && clients[0] != null) {
                        clientUser = clients[0];
                    } else {
                        clientUser = System.getProperty("user.name", "root");
                    }

                    try {
                        sshConn = config.isGSISSHModeEnabled() ? new lia.util.net.common.GSISSHControlStream(clientHost, clientUser) : new SSHControlStream(clientHost, clientUser);
                    } catch (NoClassDefFoundError t) {
                        throw new Exception("GSI libraries not loaded. You should set CLASSPATH accordingly!");
                    }
                    remoteCmd = config.getRemoteCommand();
                    for (int i = 0; i < args.length; i++) {
                        if (args[i].indexOf(':') < 0) {
                            remoteCmd += " " + args[i];
                        }
                    }
                    remoteCmd += " -c " + config.getHostName();
                    remoteCmd += " -d " + config.getDestinationDir();
                    String[] files = (String[]) config.getConfigMap().get("Files");
                    remoteCmd += " " + files[0];
                    System.err.println(" [ CONFIG ] Starting FDT client over SSH using [ " + remoteCmd + " ]");
                    sshConn.startProgram(remoteCmd);
                    
                    sshConn.waitForControlMessage("DONE", true);
                    
                    
                    System.exit(0);
                    break;
                default:
                    break;
            }
        }
    }

    private static final void initManagement() throws Exception {
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

        Map<String, Object> argsMap = Utils.parseArguments(args, Config.SINGLE_CMDLINE_ARGS);

        if (argsMap.get("-c") != null) {
            if (argsMap.get("-d") == null) {
                throw new IllegalArgumentException("No destination specified");
            }

            final List<String> lParams = (List<String>) argsMap.get("LastParams");

            if (argsMap.get("-fl") == null && (lParams == null || lParams.size() == 0) && argsMap.get("Files") == null) {
                throw new IllegalArgumentException("No source specified");
            }
        }

        

        
        
        
        
        
        
        
        

        if (argsMap.get("-h") != null || argsMap.get("-H") != null || argsMap.get("-help") != null || argsMap.get("--help") != null) {
            printHelp();
            System.exit(0);
        } else if (argsMap.get("-V") != null || argsMap.get("--version") != null || argsMap.get("-version") != null) {
            printVersion();
            System.exit(0);
        } else if (argsMap.get("-u") != null || argsMap.get("-U") != null || argsMap.get("-update") != null || argsMap.get("--update") != null) {
            final Object urlS = argsMap.get("-U");
            String updateURL = UPDATE_URL;

            if (urlS != null && urlS instanceof String) {
                updateURL = (String) urlS;
                if (updateURL.length() == 0) {
                    updateURL = UPDATE_URL;
                }
            }

            if (Utils.updateFDT(FDT_FULL_VERSION, updateURL, true)) {
                
                System.out.println("\nThe update finished successfully\n");
                System.exit(0);
            } else {
                System.out.println("\nNo updates available\n");
                System.exit(100);
            }
        }

        if (argsMap.get("-noupdates") == null) {
            final Object urlS = argsMap.get("-U");
            String updateURL = UPDATE_URL;

            if (urlS != null && urlS instanceof String) {
                updateURL = (String) urlS;
                if (updateURL.length() == 0) {
                    updateURL = UPDATE_URL;
                }
            }
            try {
                if (Utils.checkForUpdate(FDT_FULL_VERSION, updateURL)) {
                    if (argsMap.get("-silent") == null) {
                        System.out.print("\n\nAn update is available ... Do you want to upgrade to the new version? [Y/n]");
                        char car = (char) System.in.read();
                        System.out.println("\n");
                        if (car == 'Y' || car == 'y' || car == '\n' || car == '\r') {
                            System.out.print("\nTrying to update FDT to the new version ... ");
                            if (Utils.updateFDT(FDT_FULL_VERSION, updateURL, true)) {
                                
                                System.out.println("\nThe update finished successfully\n");
                                System.exit(0);
                            } else {
                                System.out.println("\nNo updates available\n");
                                System.exit(100);
                            }
                        }
                    }
                }
            } catch (Throwable t) {
                System.out.println("Got exception checking for updates: " + t.getCause());
                if (logLevel.startsWith("FIN")) {
                    t.printStackTrace();
                }
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

        
        
        
        try {
            if (config.massStorageType() != null && config.massStorageType().equals("dcache"))
                edu.caltech.hep.dcapj.dCapLayer.initialize();
        } catch (Throwable e) {
            System.err.println("FDT got exception trying to initialize the dCapLayer. Cause:");
            e.printStackTrace();
            System.out.flush();
            System.err.flush();
            System.exit(2501);
        }

        processSCPSyntax(args);

        HeaderBufferPool.initInstance();

        FDT jnc = null;

        if (!config.isLisaDisabled()) {
            LISAReportingTask lrt = LISAReportingTask.initInstance(config.getLisaHost(), config.getLisaPort());
            Utils.getMonitoringExecService().scheduleWithFixedDelay(lrt, 1, config.getLisaReportingInterval(), TimeUnit.SECONDS);
        }

        try {
            jnc = new FDT();
            initManagement();
        } catch (Throwable t) {
            t.printStackTrace();
            System.out.flush();
            System.err.flush();
            System.exit(1);
        }

        final int exitCode = jnc.doWork();

        Utils.getMonitoringExecService().shutdownNow();
        try {
            if (config.massStorageType() != null && config.massStorageType().equals("dcache"))
                edu.caltech.hep.dcapj.dCapLayer.close();
        } catch (Throwable t) {
            System.err.println("FDT got exception trying to close the dCapLayer. Cause:");
            t.printStackTrace();
            System.out.flush();
            System.err.flush();
            System.exit(2502);
        }

        System.exit(exitCode);
    }
}
