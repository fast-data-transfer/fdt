package lia.util.net.copy;

import apmon.ApMon;
import lia.util.net.common.*;
import lia.util.net.copy.monitoring.ApMonReportingTask;
import lia.util.net.copy.monitoring.ConsoleReportingTask;
import lia.util.net.copy.monitoring.FDTInternalMonitoringTask;
import lia.util.net.copy.monitoring.lisa.LISAReportingTask;
import lia.util.net.copy.transport.ControlChannel;
import lia.util.net.copy.transport.CtrlMsg;
import lia.util.net.copy.transport.FDTProcolException;
import lia.util.net.copy.transport.FDTSessionConfigMsg;
import lia.util.net.copy.transport.internal.SelectionManager;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The "main" class ... Everything will start from here, more or less
 * <p>
 * Due to Java checks the app entry point is {@link FDTMain}
 *
 * @author ramiro
 */
public class FDT {

    public static final String MONALISA2_CERN_CH = "monalisa2.cern.ch:28884";
    private static final String name = "FDT";

    private static final Logger logger = Logger.getLogger(FDT.class.getName());

    private static String UPDATE_OWNER = "fast-data-transfer";
    private static String UPDATE_REPO = "fdt";
    public static String UPDATE_URL = "https://api.github.com/repos/" + UPDATE_OWNER + "/" + UPDATE_REPO + "/releases";

    public static final String FDT_FULL_VERSION = "0.26.0-201708081850";

    /**
     * two weeks between checking for updates
     */
    public static final long UPDATE_PERIOD = 2 * 24 * 3600 * 1000;

    private static Config config;

    private static Properties localProps = new Properties();

    /**
     * Helper class for "graceful" shutdown of FDT.
     */
    private final static class GracefulStopper extends AbstractFDTCloseable {

        private boolean internalClosed = false;

        protected synchronized void internalClose() throws Exception {
            this.internalClosed = true;
            this.notifyAll();
        }
    }

    FDT() throws Exception {

        // initialize monitoring, if requested
        final String configApMonHosts = config.getApMonHosts();
        if (configApMonHosts != null) {
            long lStart = System.currentTimeMillis();

            ApMon apmon = null;

            final String apMonHosts = (configApMonHosts.length() > 0) ? configApMonHosts : MONALISA2_CERN_CH;

            logger.info("Trying to instantiate apMon to: " + apMonHosts);
            try {
                Vector<String> vHosts = new Vector<>();
                Vector<Integer> vPorts = new Vector<>();
                final String[] apMonDstTks = apMonHosts.split(",");

                if (apMonDstTks.length == 0) {
                    logger.log(Level.WARNING, "\n\nApMon enabled but no hosts defined! Cannot send apmon statistics\n\n");
                } else {
                    for (String host_port : apMonDstTks) {
                        int index;
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
                    // apmon.setJobMonitoring(, )
                    // apmon.setMaxMsgRate(50);
                    String cluster_name;
                    String node_name;
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
                        logger.info("Send operation failed: ");
                        e.printStackTrace();
                    }

                }
            } catch (Throwable ex) {
                logger.log(Level.WARNING, "Error initializing ApMon engine.", ex);
            } finally {
                Utils.initApMonInstance(apmon);
            }

            try {
                if (Utils.getApMon() != null) {
                    ApMonReportingTask apmrt = new ApMonReportingTask();
                    Utils.getMonitoringExecService().scheduleWithFixedDelay(apmrt, 1,
                            config.getApMonReportingInterval(), TimeUnit.SECONDS);
                } else {
                    logger.log(Level.WARNING, "Cannot start ApMonReportingTask because apMon is null!");
                }
            } catch (Throwable t) {
                logger.log(Level.WARNING, "Cannot start ApMonReportingTask because got Exception.", t);
            }

            long lEnd = System.currentTimeMillis();
            logger.info("ApMon initialization took " + (lEnd - lStart) + " ms");
        }

        Utils.getMonitoringExecService().scheduleWithFixedDelay(FDTInternalMonitoringTask.getInstance(), 1, 5,
                TimeUnit.SECONDS);
        final long reportingTaskDelay = config.getReportingTaskDelay();
        if (reportingTaskDelay > 0) {
            Utils.getMonitoringExecService().scheduleWithFixedDelay(ConsoleReportingTask.getInstance(), 0,
                    reportingTaskDelay, TimeUnit.SECONDS);
        }

        if (config.isCoordinatorMode()) {
            ControlChannel cc = new ControlChannel(config.getHostName(), config.getPort(), UUID.randomUUID(), FDTSessionManager.getInstance());
            UUID sessionID = cc.sendCoordinatorMessage(new CtrlMsg(CtrlMsg.THIRD_PARTY_COPY, new FDTSessionConfigMsg(config)));
            // wait for remote config
            logger.log(Level.INFO,"Message sent to: " + config.getHostName() +":"+ config.getPort() + " Remote Job Session ID: " + sessionID);
            System.exit(0);
        } else {
            if (config.getHostName() != null) { // role == client
                // the session manager will check the "pull/push" mode and start the FDTSession
                FDTSessionManager.getInstance().addFDTClientSession();
            } else { // is server
                if (!DirectByteBufferPool.initInstance(config.getByteBufferSize(), Config.getMaxTakePollIter())) {
                    // this is really wrong ... I cannot be already initialized
                    throw new FDTProcolException("The buffer pool cannot be alredy initialized");
                }

                final FDTServer theServer = new FDTServer(); // ( because it's the only one )
                theServer.doWork();
            }
        }
    }

    private static void printHelp() {
        logger.log(Level.INFO, Config.getUsage());
    }

    private static void printVersion() {
        logger.info(name + ' ' + FDT_FULL_VERSION);
        logger.info("Contact: support-fdt@monalisa.cern.ch");
    }

    private static int doWork() {

        Exception e = null;
        FDTSessionManager fdtSessionManager = FDTSessionManager.getInstance();

        try {
            for (; ; ) {
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
                        if (!config.isStandAlone() && fdtSessionManager.isInited()
                                && fdtSessionManager.sessionsNumber() == 0) {
                            SelectionManager.getInstance().stopIt();
                            logger.info(
                                    "Server started with -S flag set and all the sessions have finished ... FDT will stop now");
                            break;
                        }
                    }
                } catch (Throwable t) {
                    logger.log(Level.WARNING, "FDT Got exception in main loop", t);
                }
            }
        }
        catch (Exception ex) {
            e = ex;
        } finally {
            try {
                logger.info(" [ " + new Date().toString()
                        + " ] - GracefulStopper hook started ... Waiting for the cleanup to finish");

                AtomicReference<GracefulStopper> stopper = new AtomicReference<>(new GracefulStopper());

                // it will be the last in the queue ;)
                stopper.get().close(null, e);

                while (!stopper.get().internalClosed) {
                    synchronized (stopper.get()) {
                        if (stopper.get().internalClosed) {
                            break;
                        }
                        try {
                            stopper.get().wait();
                        } catch (Throwable t) {
                            t.printStackTrace();
                        }
                    }
                }
                logger.info(" [ " + new Date().toString() + " ]  - GracefulStopper hook finished!");
            } catch (Throwable gExc) {
                logger.log(Level.WARNING, " [GracefulStopper] Got exception stopper", gExc);
            }
        }

        final Throwable tExit = fdtSessionManager.getLasDownCause();
        final String mExit = fdtSessionManager.getLasDownMessage();
        if (tExit != null || mExit != null) {
            logger.log(Level.WARNING, "\n [ " + new Date().toString() + " ]  FDT Session finished with errors: ");
            if (mExit != null) {
                logger.log(Level.WARNING, mExit + '\n');
            }

            if (tExit != null) {
                logger.log(Level.WARNING, Utils.getStackTrace(tExit) + '\n');
            }

            return 1;
        }

        logger.info("\n [ " + new Date().toString() + " ]  FDT Session finished OK.\n");
        return 0;
    }

    private static void processSCPSyntax(String[] args) throws Exception {
        int iTransferConfiguration = config.getSSHConfig();
        if (iTransferConfiguration > 0) {
            final int sshPort = config.getSSHPort();

            switch (iTransferConfiguration) {
                case Config.SSH_REMOTE_SERVER_LOCAL_CLIENT_PUSH:
                    sshRemoteServerLocalClientPush(sshPort);
                    break;
                case Config.SSH_REMOTE_SERVER_LOCAL_CLIENT_PULL:
                    sshRemoteServerLocalClientPull(sshPort);
                    break;
                case Config.SSH_REMOTE_SERVER_REMOTE_CLIENT_PUSH:
                    sshRemoteServerAndClientPush(args, sshPort);
                    break;
                default:
                    break;
            }
        }
    }

    private static void sshRemoteServerLocalClientPush(int sshPort) throws Exception {
        ControlStream sshConn;
        String localAddresses;
        StringBuilder remoteCmd;
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("[SSH Mode] SSH_REMOTE_SERVER_LOCAL_CLIENT_PUSH. Remote ssh port: " + sshPort);
        }
        try {// here we can have some class-not-found exceptions if GSI libraries are not loaded
            sshConn = config.isGSISSHModeEnabled() ? //
                    new GSISSHControlStream(config.getHostName(),
                            config.getDestinationUser(), sshPort)
                    : //
                    new SSHControlStream(config.getHostName(), config.getDestinationUser(), sshPort);
        } catch (NoClassDefFoundError t) {
            throw new Exception("GSI libraries not loaded. You should set CLASSPATH accordingly!");
        }
        sshConn.connect();
        localAddresses = config.getLocalAddresses();
        // append the required options to the configurable java command
        remoteCmd = new StringBuilder(config.getRemoteCommand() + " -p " + config.getPort() + " -noupdates -silent -S -f "
                + localAddresses);
        if (logger.isLoggable(Level.FINE)) {
            logger.fine(" [ CONFIG ] Starting FDT server over SSH using [ " + remoteCmd + " ]");
        }
        sshConn.startProgram(remoteCmd.toString());
        sshConn.waitForControlMessage("READY");
        if (logger.isLoggable(Level.FINE)) {
            logger.fine(" [ CONFIG ] FDT server successfully started on [ " + config.getHostName() + " ]");
        }
    }

    private static void sshRemoteServerLocalClientPull(int sshPort) throws Exception {
        String[] clients;
        ControlStream sshConn;
        String localAddresses;
        StringBuilder remoteCmd;
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("[SSH Mode] SSH_REMOTE_SERVER_LOCAL_CLIENT_PULL. Remote ssh port: " + sshPort);
        }
        // the host running the FDT server is the source in this case
        String remoteServerHost = config.getSourceHosts()[0];
        String remoteServerUsername;
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
            sshConn = config.isGSISSHModeEnabled()
                    ? new GSISSHControlStream(remoteServerHost, remoteServerUsername,
                    sshPort)
                    : new SSHControlStream(remoteServerHost, remoteServerUsername, sshPort);
        } catch (NoClassDefFoundError t) {
            throw new Exception("GSI libraries not loaded. You should set CLASSPATH accordingly!");
        }
        sshConn.connect();
        localAddresses = config.getLocalAddresses();
        // append the required options to the configurable java command
        remoteCmd = new StringBuilder(config.getRemoteCommand() + " -p " + config.getPort() + " -noupdates -silent -S -f "
                + localAddresses);
        if (logger.isLoggable(Level.FINE)) {
            logger.fine(" [ CONFIG ] Starting FDT server over SSH using [ " + remoteCmd + " ]");
        }
        sshConn.startProgram(remoteCmd.toString());
        sshConn.waitForControlMessage("READY");
        if (logger.isLoggable(Level.FINE)) {
            logger.fine(" [ CONFIG ] FDT server successfully started on [ " + remoteServerHost + " ]");
        }
    }

    private static void sshRemoteServerAndClientPush(String[] args, int sshPort) throws Exception {
        ControlStream sshConn;
        StringBuilder remoteCmd;
        String[] clients;
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("[SSH Mode] SSH_REMOTE_SERVER_REMOTE_CLIENT_PUSH. Remote ssh port: " + sshPort);
        }
        // the host starting the fdt client
        final String clientHost = config.getSourceHosts()[0];
        // start FDT Server
        try {// here we can have some class-not-found exceptions if GSI libraries are not loaded
            sshConn = config.isGSISSHModeEnabled()
                    ? new GSISSHControlStream(config.getHostName(),
                    config.getDestinationUser(), sshPort)
                    : new SSHControlStream(config.getHostName(), config.getDestinationUser(), sshPort);
        } catch (NoClassDefFoundError t) {
            throw new Exception("GSI libraries not loaded. You should set CLASSPATH accordingly!");
        }
        // append the required options to the configurable java command
        remoteCmd = new StringBuilder(config.getRemoteCommand() + " -p " + config.getPort() + " -noupdates -silent -S -f "
                + clientHost);
        if (logger.isLoggable(Level.FINE)) {
            logger.fine(" [ CONFIG ] Starting remote FDT server over SSH using [ " + remoteCmd + " ]");
        }
        sshConn.startProgram(remoteCmd.toString());
        sshConn.waitForControlMessage("READY");
        if (logger.isLoggable(Level.FINE)) {
            logger.fine(" [ CONFIG ] FDT server successfully started on [ " + config.getHostName() + " ]");
        }
        // server ok

        // start FDT client
        String clientUser;
        clients = config.getSourceUsers();
        if (clients != null && clients.length > 0 && clients[0] != null) {
            clientUser = clients[0];
        } else {
            clientUser = System.getProperty("user.name", "root");
        }

        try {// here we can have some class-not-found exceptions if GSI libraries are not loaded
            sshConn = config.isGSISSHModeEnabled()
                    ? new GSISSHControlStream(clientHost, clientUser, sshPort)
                    : new SSHControlStream(clientHost, clientUser, sshPort);
        } catch (NoClassDefFoundError t) {
            throw new Exception("GSI libraries not loaded. You should set CLASSPATH accordingly!");
        }
        remoteCmd = new StringBuilder(config.getRemoteCommand());
        for (String arg : args) {
            if (arg.indexOf(':') < 0) {
                remoteCmd.append(' ').append(arg);
            }
        }
        remoteCmd.append(" -c ").append(config.getHostName());
        remoteCmd.append(" -d ").append(config.getDestinationDir());
        String[] files = (String[]) config.getConfigMap().get("Files");
        remoteCmd.append(' ').append(files[0]);
        if (logger.isLoggable(Level.FINE)) {
            logger.fine(" [ CONFIG ] Starting FDT client over SSH using [ " + remoteCmd + " ]");
        }
        sshConn.startProgram(remoteCmd.toString());
        // wait for client termination or forced exit
        sshConn.waitForControlMessage("DONE", true);
        // after the remote client finished, our 'proxy' program should also exit
        // maybe we should change this 'exit' with some method return code
        System.exit(0);
    }

    private static void initManagement() throws Exception {
        // not there yet
    }

    // the one and only entry point
    public static void main(String[] args) throws Exception {
        // Init the logging
        String logLevel = initLogging(args);

        Map<String, Object> argsMap = Utils.parseArguments(args, Config.SINGLE_CMDLINE_ARGS);

        checkMainParams(argsMap);

        final boolean noLock = checkAdditionalParams(argsMap);

        updateOrSkip(logLevel, argsMap, noLock);

        logger.info("\n\n" + name + " [ " + FDT_FULL_VERSION + " ] STARTED ... \n\n");

        initConfig(argsMap, logLevel);

        if (!config.isCoordinatorMode() || !config.isRetrievingLogFile()) {
            logger.info("FDT uses" + ((!config.isBlocking()) ? " *non-" : " *") + "blocking* I/O mode.");
        }

        processSCPSyntax(args);

        HeaderBufferPool.initInstance();

        if (!config.isLisaDisabled()) {
            LISAReportingTask lrt = LISAReportingTask.initInstance(config.getLisaHost(), config.getLisaPort());
            Utils.getMonitoringExecService().scheduleWithFixedDelay(lrt, 1, config.getLisaReportingInterval(),
                    TimeUnit.SECONDS);
        }

        try {
            new FDT();
            initManagement();
        } catch (Throwable t) {
            logger.log(Level.WARNING, "Failed to instantiate FDT", t);
            System.exit(1);
        }

        final int exitCode = FDT.doWork();

        Utils.getMonitoringExecService().shutdownNow();
        try {
            if (config.massStorageType() != null && config.massStorageType().equals("dcache")) {
                final FileChannelProviderFactory fcpf = config.getFileChannelProviderFactory();
                if (fcpf instanceof FDTCloseable) {
                    ((FDTCloseable) fcpf).close(null, null);
                }
            }
        } catch (Throwable t) {
            logger.log(Level.WARNING, "FDT got exception trying to close the dCapLayer. Cause:", t);
            System.exit(2502);
        }

        System.exit(exitCode);
    }

    private static boolean checkAdditionalParams(Map<String, Object> argsMap) throws Exception {
        final boolean noLock = argsMap.get("-nolock") != null || argsMap.get("-nolocks") != null;
        if (argsMap.get("-h") != null || argsMap.get("-H") != null || argsMap.get("-help") != null
                || argsMap.get("--help") != null) {
            printHelp();
            System.exit(0);
        } else if (argsMap.get("-V") != null || argsMap.get("--version") != null || argsMap.get("-version") != null) {
            printVersion();
            System.exit(0);
        } else if (argsMap.get("-u") != null || argsMap.get("-U") != null || argsMap.get("-update") != null
                || argsMap.get("--update") != null) {
            updateIfAvailable(argsMap, noLock);
        }
        return noLock;
    }

    private static void initConfig(Map<String, Object> argsMap, String logLevel) {
        try {
            Config.initInstance(argsMap);
        } catch (InvalidFDTParameterException e) {
            logger.log(Level.WARNING,"Invalid parameters supplied: " + e.getMessage(), e);
            System.exit(1);
        } catch (Throwable t1) {
            logger.log(Level.WARNING,"got exception parsing command args", t1);
            System.exit(1);
        }
        config = Config.getInstance();
        config.setLogLevel(logLevel);
    }

    private static void updateOrSkip(String logLevel, Map<String, Object> argsMap, boolean noLock) {
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
                        System.out.print(
                                "\n\nAn update is available ... Do you want to upgrade to the new version? [Y/n]");
                        char car = (char) System.in.read();
                        logger.info("\n");
                        if (car == 'Y' || car == 'y' || car == '\n' || car == '\r') {
                            System.out.print("\nTrying to update FDT to the new version ... ");
                            if (Utils.updateFDT(FDT_FULL_VERSION, updateURL, true, noLock)) {
                                // Just print the current version ...
                                logger.info("\nThe update finished successfully\n");
                                System.exit(0);
                            } else {
                                logger.info("\nNo updates available\n");
                                System.exit(100);
                            }
                        }
                    }
                }
            } catch (Throwable t) {
                logger.info("Got exception checking for updates: " + t.getCause());
                if (logLevel.startsWith("FIN")) {
                    t.printStackTrace();
                }
            }
        }
    }

    private static void updateIfAvailable(Map<String, Object> argsMap, boolean noLock) throws Exception {
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
            logger.info("\nThe update finished successfully\n");
            System.exit(0);
        } else {
            logger.info("\nNo updates available\n");
            System.exit(100);
        }
    }

    private static void checkMainParams(Map<String, Object> argsMap) {
        if (argsMap.get("-c") != null) {
            if (argsMap.get("-d") == null && argsMap.get("-nettest") == null) {
                throw new IllegalArgumentException("No destination specified");
            }

            @SuppressWarnings("unchecked") final List<String> lParams = (List<String>) argsMap.get("LastParams");

            if ((argsMap.get("-nettest") == null && argsMap.get("-fl") == null
                    && (lParams == null || lParams.size() == 0) && argsMap.get("Files") == null) && argsMap.get("-sID") == null) {
                throw new IllegalArgumentException("No source specified");
            }
        }
    }

    private static String initLogging(String[] args) throws IOException {
        String logLevel = null;
        File logFile = null;
        for (int i = 0; i < args.length; i++) {
            if (logLevel == null) {
                if (args[i].equals("-v")) {
                    logLevel = "FINE";
                }

                if (args[i].equals("-vv")) {
                    logLevel = "FINER";
                }

                if (args[i].equals("-vvv")) {
                    logLevel = "FINEST";
                }

                if (logFile == null) {
                    if (args[i].equals("-log")) {
                        if (i >= args.length - 1) {
                            throw new IllegalArgumentException("The -log parameter expects a file path");
                        }

                        final String logPathParam = args[i + 1];
                        if (logPathParam.startsWith("-")) {
                            throw new IllegalArgumentException(
                                    "The -log parameter expects a file path which does not start with '-'");
                        }

                        //Java 6 still to be used for a while, will take it down next year ...
                        final File logF = new File(logPathParam);
                        final File logFParent = logF.getParentFile();
                        if (logFParent != null && !logFParent.exists()) {
                            try {
                                final boolean mkdirsResult = logFParent.mkdirs();
                                if (!mkdirsResult) {
                                    throw new IllegalArgumentException("Unable to create parent dirs for log file: '"
                                            + logFParent + "' OS syscall failed");
                                }
                            } catch (Throwable t) {
                                throw new IllegalArgumentException(
                                        "Unable to create parent dirs for log file: '" + logFParent + "' Cause:", t);
                            }
                        }

                        if (logF.exists()) {
                            if (!logF.canWrite()) {
                                throw new IOException(
                                        "The provided log file: '" + logF + "' exists but is not writable!");
                            }
                        } else {
                            final boolean createFileResult = logF.createNewFile();
                            if (!createFileResult) {
                                throw new IOException("The provided log file: '" + logF + "' cannot be created!");
                            }
                        }

                        //finally all looks good for now
                        logFile = logF;
                    }
                }
            }
        }

        if (logLevel == null) {
            logLevel = "INFO";
        }

        if (logLevel.startsWith("FIN")) {
            logger.info(" LogLevel: " + logLevel);
        }
        Utils.initLogger(logLevel, logFile, localProps);
        return logLevel;
    }
}
