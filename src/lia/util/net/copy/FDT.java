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
import lia.util.net.common.FDTCloseable;
import lia.util.net.common.FileChannelProviderFactory;
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

/**
 * The "main" class ... Everything will start from here, more or less
 * 
 * Due to Java checks the app entry point is {@link FDTMain}
 * 
 * @author ramiro
 */
public class FDT {

    private static final String name = "FDT";

    private static String UPDATE_URL = "http://monalisa.cern.ch/FDT/lib/";

    public static final String FDT_FULL_VERSION = "0.19.2-201311171818";

    String mlDestinations = "monalisa2.cern.ch:28884,monalisa2.caltech.edu:28884";

    /** two weeks between checking for updates */
    public static final long UPDATE_PERIOD = 2 * 24 * 3600 * 1000;

    private static Config config;

    private static Properties localProps = new Properties();

    /**
     * Helper class for "graceful" shutdown of FDT.
     */
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
            if (confFile.exists() && confFile.canRead()) {
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
            Utils.closeIgnoringExceptions(fis);
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

        // initialize monitoring, if requested
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
                        vPorts.add(Integer.valueOf(port));
                    }

                    ApMon.setLogLevel("WARNING");
                    apmon = new ApMon(vHosts, vPorts);
                    apmon.setConfRecheck(false, -1);
                    apmon.setGenMonitoring(true, 40);
                    // apmon.setJobMonitoring(, )
                    // apmon.setMaxMsgRate(50);
                    String cluster_name = "";
                    String node_name = "";
                    if (config.getHostName() != null) {// client
                        cluster_name = "Clients";
                        node_name = config.getHostName();
                    } else {// server
                        cluster_name = "Servers";
                        node_name = apmon.getMyHostname();
                    }
                    apmon.setMonitorClusterNode(cluster_name, node_name);
                    // apmon.setRecheckInterval(-1)
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

        if (config.getHostName() != null) { // role == client
            // the session manager will check the "pull/push" mode and start the FDTSession
            FDTSessionManager.getInstance().addFDTClientSession();
        } else { // is server
            if (!DirectByteBufferPool.initInstance(config.getByteBufferSize(), config.getMaxTakePollIter())) {
                // this is really wrong ... I cannot be already initialized
                throw new FDTProcolException("The buffer pool cannot be alredy initialized");
            }

            final FDTServer theServer = new FDTServer(); // ( because it's the only one )
            theServer.doWork();
        }

    }

    private static void printHelp() {
        System.err.println(Config.getUsage());
    }

    private static void printVersion() {
        System.out.println(name + " " + FDT_FULL_VERSION);
        System.out.println("Contact: support-fdt@monalisa.cern.ch");
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

                // it will be the last in the queue ;)
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

            final int sshPort = config.getSSHPort();

            switch (iTransferConfiguration) {

                case Config.SSH_REMOTE_SERVER_LOCAL_CLIENT_PUSH:
                    System.err.println("[SSH Mode] SSH_REMOTE_SERVER_LOCAL_CLIENT_PUSH. Remote ssh port: " + sshPort);
                    try {// here we can have some class-not-found exceptions if GSI libraries are not loaded
                        sshConn = config.isGSISSHModeEnabled() ? //
                                new lia.util.net.common.GSISSHControlStream(config.getHostName(), config.getDestinationUser(), sshPort) : //
                                new SSHControlStream(config.getHostName(), config.getDestinationUser(), sshPort);
                    } catch (NoClassDefFoundError t) {
                        throw new Exception("GSI libraries not loaded. You should set CLASSPATH accordingly!");
                    }
                    sshConn.connect();
                    localAddresses = config.getLocalAddresses();
                    // append the required options to the configurable java command
                    remoteCmd = config.getRemoteCommand() + " -p " + config.getPort() + " -noupdates -silent -S -f " + localAddresses;
                    System.err.println(" [ CONFIG ] Starting FDT server over SSH using [ " + remoteCmd + " ]");
                    sshConn.startProgram(remoteCmd);
                    sshConn.waitForControlMessage("READY");
                    System.err.println(" [ CONFIG ] FDT server successfully started on [ " + config.getHostName() + " ]");
                    break;

                case Config.SSH_REMOTE_SERVER_LOCAL_CLIENT_PULL:
                    System.err.println("[SSH Mode] SSH_REMOTE_SERVER_LOCAL_CLIENT_PULL. Remote ssh port: " + sshPort);
                    // the host running the FDT server is the source in this case
                    String remoteServerHost = config.getSourceHosts()[0];
                    String remoteServerUsername = null;
                    clients = config.getSourceUsers();
                    if (clients != null && clients.length > 0 && clients[0] != null) {
                        remoteServerUsername = clients[0];
                    } else {
                        remoteServerUsername = System.getProperty("user.name", "root");
                    }
                    // update the local client parameters
                    config.setPullMode(true);
                    config.setHostName(remoteServerHost);

                    try {// here we can have some class-not-found exceptions if GSI libraries are not loaded
                        sshConn = config.isGSISSHModeEnabled() ? new lia.util.net.common.GSISSHControlStream(remoteServerHost,
                                                                                                             remoteServerUsername,
                                                                                                             sshPort)
                                : new SSHControlStream(remoteServerHost, remoteServerUsername, sshPort);
                    } catch (NoClassDefFoundError t) {
                        throw new Exception("GSI libraries not loaded. You should set CLASSPATH accordingly!");
                    }
                    sshConn.connect();
                    localAddresses = config.getLocalAddresses();
                    // append the required options to the configurable java command
                    remoteCmd = config.getRemoteCommand() + " -p " + config.getPort() + " -noupdates -silent -S -f " + localAddresses;
                    System.err.println(" [ CONFIG ] Starting FDT server over SSH using [ " + remoteCmd + " ]");
                    sshConn.startProgram(remoteCmd);
                    sshConn.waitForControlMessage("READY");
                    System.err.println(" [ CONFIG ] FDT server successfully started on [ " + remoteServerHost + " ]");
                    break;

                case Config.SSH_REMOTE_SERVER_REMOTE_CLIENT_PUSH:
                    System.err.println("[SSH Mode] SSH_REMOTE_SERVER_REMOTE_CLIENT_PUSH. Remote ssh port: " + sshPort);
                    // the host starting the fdt client
                    final String clientHost = config.getSourceHosts()[0];
                    // start FDT Server
                    try {// here we can have some class-not-found exceptions if GSI libraries are not loaded
                        sshConn = config.isGSISSHModeEnabled() ? new lia.util.net.common.GSISSHControlStream(config.getHostName(),
                                                                                                             config.getDestinationUser(),
                                                                                                             sshPort)
                                : new SSHControlStream(config.getHostName(), config.getDestinationUser(), sshPort);
                    } catch (NoClassDefFoundError t) {
                        throw new Exception("GSI libraries not loaded. You should set CLASSPATH accordingly!");
                    }
                    // append the required options to the configurable java command
                    remoteCmd = config.getRemoteCommand() + " -p " + config.getPort() + " -noupdates -silent -S -f " + clientHost;
                    System.err.println(" [ CONFIG ] Starting remote FDT server over SSH using [ " + remoteCmd + " ]");
                    sshConn.startProgram(remoteCmd);
                    sshConn.waitForControlMessage("READY");
                    System.err.println(" [ CONFIG ] FDT server successfully started on [ " + config.getHostName() + " ]");
                    // server ok

                    // start FDT client
                    String clientUser = null;
                    clients = config.getSourceUsers();
                    if (clients != null && clients.length > 0 && clients[0] != null) {
                        clientUser = clients[0];
                    } else {
                        clientUser = System.getProperty("user.name", "root");
                    }

                    try {// here we can have some class-not-found exceptions if GSI libraries are not loaded
                        sshConn = config.isGSISSHModeEnabled() ? new lia.util.net.common.GSISSHControlStream(clientHost, clientUser, sshPort)
                                : new SSHControlStream(clientHost, clientUser, sshPort);
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
                    // wait for client termination or forced exit
                    sshConn.waitForControlMessage("DONE", true);
                    // after the remote client finished, our 'proxy' program should also exit
                    // maybe we should change this 'exit' with some method return code
                    System.exit(0);
                    break;
                default:
                    break;
            }
        }
    }

    private static final void initManagement() throws Exception {
        // not there yet
    }

    // the one and only entry point
    public static final void main(String[] args) throws Exception {

        // Init the logging

        // If the ${HOME}/.fdt/fdt.properties exists
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
            if (argsMap.get("-d") == null && argsMap.get("-nettest") == null) {
                throw new IllegalArgumentException("No destination specified");
            }

            @SuppressWarnings("unchecked")
            final List<String> lParams = (List<String>) argsMap.get("LastParams");

            if (argsMap.get("-nettest") == null && argsMap.get("-fl") == null && (lParams == null || lParams.size() == 0)
                    && argsMap.get("Files") == null) {
                throw new IllegalArgumentException("No source specified");
            }
        }

        // FDTCommandLine fdtCommandLine = new FDTCommandLine(args);

        // if( fdtCommandLine.getOption("-h") != null || fdtCommandLine.getOption("-help") != null ||
        // fdtCommandLine.getOption("--help") != null ) {
        // printHelp();
        // System.exit(0);
        // } else if ( fdtCommandLine.getOption("-v")!=null || fdtCommandLine.getOption("--version")!=null ) {
        // printVersion();
        // System.exit(0);
        // }

        final boolean noLock = argsMap.get("-nolock") != null || argsMap.get("-nolocks") != null;
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

            if (Utils.updateFDT(FDT_FULL_VERSION, updateURL, true, noLock)) {
                // Just print the current version ...
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
                if (Utils.checkForUpdate(FDT_FULL_VERSION, updateURL, noLock)) {
                    if (argsMap.get("-silent") == null) {
                        System.out.print("\n\nAn update is available ... Do you want to upgrade to the new version? [Y/n]");
                        char car = (char) System.in.read();
                        System.out.println("\n");
                        if (car == 'Y' || car == 'y' || car == '\n' || car == '\r') {
                            System.out.print("\nTrying to update FDT to the new version ... ");
                            if (Utils.updateFDT(FDT_FULL_VERSION, updateURL, true, noLock)) {
                                // Just print the current version ...
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
            System.err.println("Invalid parameters supplied: " + e.getMessage());
            e.printStackTrace();
            System.err.flush();
            System.exit(1);
        } catch (Throwable t1) {
            System.err.println("got exception parsing command args");
            t1.printStackTrace();
            System.err.flush();
            System.exit(1);
        }

        config = Config.getInstance();
        System.out.println("FDT uses" + ((!config.isBlocking()) ? " *non-" : " *") + "blocking* I/O mode.");

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
            if (config.massStorageType() != null && config.massStorageType().equals("dcache")) {
                final FileChannelProviderFactory fcpf = config.getFileChannelProviderFactory();
                if (fcpf instanceof FDTCloseable) {
                    ((FDTCloseable) fcpf).close(null, null);
                }
            }
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
