/*
 * $Id$
 */
package lia.util.net.common;

import lia.util.net.copy.PosixFSFileChannelProviderFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Configuration params for FDT
 *
 * @author ramiro
 * @author Lucian Musat
 */
public class Config {

    /**
     * Logger used by this class
     */
    private static final Logger logger = Logger.getLogger("lia.util.net.common.Config");
    // The size of the buffer which is sent over the wire!
    // TODO make this a parameter
    public static final int NETWORK_BUFF_LEN_SIZE;

    static {
        int defaultMSSSize;
        int minMTU;
        try {
            minMTU = getMinMTU();
            defaultMSSSize = minMTU - 40;
        } catch (Throwable ignore) {
            defaultMSSSize = 1460;
        }

        if (defaultMSSSize < 1000) {
            defaultMSSSize = 1460;
        }

        NETWORK_BUFF_LEN_SIZE = defaultMSSSize;
    }

    // env props which will be sent to remote peer
    private final static String[] exportedSysProps = {"user.name", "user.home", "user.dir", "file.separator",
            "file.encoding", "path.separator"};
    // public static final String SINGLE_CMDLINE_ARGS[] = { "-S", "-pull", "-N", "-gsi", "-bio", "-r", "-fbs", "-ll",
    // "-loop", "-enableLisaRestart", "-md5", "-printStats", "-gsissh", "-noupdates", "-silent"};
    public static final String[] SINGLE_CMDLINE_ARGS = {"-v", "-vv", "-vvv", "-loop", "-r", "-pull", "-printStats",
            "-N", "-bio", "-gsi", "-gsissh", "-notmp", "-nolock", "-nolocks", "-nettest", "-genb"};
    public static final String[] VALUE_CMDLINE_ARGS = {"-bs", "-P", "-ss", "-limit", "-preFilters", "-postFilters",
            "-monID", "-ms", "-c", "-p", "-sshp", "-gsip", "-iof", "-sn", "-rCount", "-wCount", "-pCount", "-d",
            "-writeMode", "-lisa_rep_delay", "-apmon_rep_delay", "-fl", "-reportDelay", "-ka"};
    public static final String POSSIBLE_VALUE_CMDLINE_ARGS[] = {"-enable_apmon", "-lisafdtclient", "-lisafdtserver",
            "-f", "-F", "-h", "-H", "--help", "-help," + "-u", "-U", "--update", "-update"};

    /**
     * used in conjuction with -fl to delimit the eventual destination file name
     * e.g. {@code /orginal/file/name / /destination/file/name}
     */
    public static final String REGEX_REMAP_DELIMITER = "(\\s)+/(\\s)+";

    // all of this are set by the ant script
    public static final String FDT_MAJOR_VERSION = "0";
    public static final String FDT_MINOR_VERSION = "25";
    public static final String FDT_MAINTENANCE_VERSION = "0";
    public static final String FDT_FULL_VERSION = FDT_MAJOR_VERSION + "." + FDT_MINOR_VERSION + "."
            + FDT_MAINTENANCE_VERSION;
    public static final String FDT_RELEASE_DATE = "2017-04-20";
    private volatile static Config _thisInstance;
    // the size of header packet sent over the wire -
    // TODO - this should be dynamic ... or not ( performance resons ?! )
    public static final int HEADER_SIZE = 56;
    public static final int HEADER_SIZE_v2 = 56;
    public static final boolean TRACK_ALLOCATIONS = true;
    public static final int KILO = 1024;
    // 1 MByte
    public static final int DEFAULT_BUFFER_SIZE = KILO * KILO; // 1MB
    private int byteBufferSize = DEFAULT_BUFFER_SIZE;
    // default will be false
    private final boolean isNagleEnabled;
    // shall I get the data from server? - used only by the client
    private boolean isPullMode = false;
    private boolean isCoordinatorMode;
    private boolean isRetrievingLogFile;
    private boolean isThirdPartyCopyAgent;
    // this should be used for syncronizations at application level ()
    public static final Object BIG_FDTAPP_LOCK = new Object();
    // default is 4
    public static final int DEFAULT_SOCKET_NO = 4;
    private int sockNum = DEFAULT_SOCKET_NO;
    public static final int DEFAULT_PORT_NO = 54321;
    public static final long DEFAULT_KEEP_ALIVE_NANOS = TimeUnit.MINUTES.toNanos(2);
    public static final int DEFAULT_PORT_NO_GSI = 54320;
    public static final int DEFAULT_PORT_NO_SSH = 22;
    private int sockBufSize = -1;
    private long rateLimit = -1;
    private long rateLimitDelayMillis = 300L;
    private int readersCount = 1;
    private int writersCount = 1;
    private int maxPartitionsCount = 100;
    private String hostname;
    private String lisaHost;
    private int lisaPort;
    private int portNo;
    private int portNoGSI;
    private int portNoSSH;
    private int destPort;
    private int remoteTransferPort;
    private ArrayBlockingQueue<Integer> transportPorts;
    private final boolean isStandAlone;
    private String[] fileList;
    private String[] remappedFileList;
    private String destDir;
    private String listFilesFrom;
    private String sIP;
    private String dIP;
    private final String sshKeyPath;
    private final String apMonHosts;
    private boolean bComputeMD5 = false;
    private boolean bRecursive = false;
    private boolean bCheckUpdate = false;
    private boolean isBlocking = false;
    private boolean bUseFixedBlocks = false;
    private boolean bLocalLoop = false;
    private boolean bLoop = false;
    private NetMatcher sourceAddressFilter = null;
    private int IORetryFactor = 2;
    private int selectorsNo = 1;
    private boolean bDisableLisa = false;
    private long lisaReportInterval = 20;
    private long apMonReportInterval = 20;
    // for client side in SSH mode
    private boolean bSSHMode = false;
    private boolean bGSISSHMode = false;
    private boolean bGSIMode = false;
    private String[] aSourceUsers = null;
    private String[] aSourceHosts = null;
    private String sDestinationUser = null;
    private String sLocalAddresses = null;
    private String sStartServerCommand = null;
    private final boolean isLisaRestartEnabled;
    private final String writeMode;
    private final String preFilters;
    private final String postFilters;
    private final String monID;
    private String logLevel;
    private String massStorageConfig = null;
    private String massStorageType = null;
    private MassStorage storageParams = null;
    private Level statsLevel = null;
    private Map<String, Object> configMap;
    private boolean isNoTmpFlagSet = false;
    private boolean isNoLockFlagSet = false;
    private long consoleReportingTaskDelay = 5;
    private final boolean isNetTest;
    private final boolean isGenTest;
    private final long keepAliveDelayNanos;
    private final FileChannelProviderFactory fileChannelProviderFactory;
    private Map<String, Integer> sessionPortMap = new HashMap<>();
    private Map<Integer, List<Object>> sessionSocketMap = new HashMap<>();

    private static final int getMinMTU() {
        int retMTU = 1500;

        try {
            final Enumeration<NetworkInterface> netInterfacesEnum = NetworkInterface.getNetworkInterfaces();
            while (netInterfacesEnum.hasMoreElements()) {
                final NetworkInterface netInteface = netInterfacesEnum.nextElement();

                try {
                    if (!netInteface.isUp()) {
                        continue;
                    }
                } catch (NoSuchMethodError nsme) {
                    // java < 1.6
                    if (logger.isLoggable(Level.FINE)) {
                        System.out.println("The current JVM is not able to determine if the net interface "
                                + netInteface + "is up and running. JVM >= 1.6 should support this feature");
                    }
                    return retMTU;
                } catch (Throwable t) {
                    System.err.println(" Cannot determine if the interface: " + netInteface + " is up");
                    return retMTU;
                }

                int cMTU = -1;
                try {
                    cMTU = netInteface.getMTU();
                } catch (SocketException se) {
                    System.err.println(" Cannot get MTU for netInterface: " + netInteface + " SocketException: " + se);
                } catch (NoSuchMethodError nsme) {
                    if (logger.isLoggable(Level.FINE)) {
                        System.out.println("The current JVM is not able to determine the MTU for the net interface "
                                + netInteface + "is up and running. JVM >= 1.6 should support this feature");
                    }
                    continue;
                } catch (Throwable t) {
                    // probably incompatible JVM version
                    System.err.println(" Cannot get MTU for netInterface: " + netInteface + " Exception: " + t);
                }

                if ((cMTU < retMTU) && (cMTU > 0)) {
                    retMTU = cMTU;
                }
            }// while
        } catch (SocketException se) {
            System.err.println(" Cannot get min MTU for current instance of FDT. SocketException: " + se);
        } catch (Throwable t) {
            System.err.println(" Cannot get min MTU for current instance of FDT. Exception: " + t);
        }

        return retMTU;
    }

    /**
     * @param configMap
     * @throws InvalidFDTParameterException if incorrect values are supplied for parameters
     */
    private Config(final Map<String, Object> configMap) throws InvalidFDTParameterException {
        this.configMap = configMap;
        if (configMap == null) {
            throw new InvalidFDTParameterException("Null config map");
        }

        for (String exportedSysProp : exportedSysProps) {
            configMap.put(exportedSysProp, System.getProperty(exportedSysProp));
        }

        isStandAlone = (configMap.get("-S") == null);
        setRetrievingLogFile(configMap.get("-sID"));
        byteBufferSize = Utils.getIntValue(configMap, "-bs", DEFAULT_BUFFER_SIZE);
        configMap.put("-bs", String.valueOf(byteBufferSize));

        sockNum = Utils.getIntValue(configMap, "-P", DEFAULT_SOCKET_NO);
        configMap.put("-P", String.valueOf(sockNum));

        sockBufSize = Utils.getIntValue(configMap, "-ss", -1);
        configMap.put("-ss", String.valueOf(sockBufSize));

        rateLimit = Utils.getLongValue(configMap, "-limit", -1);
        if ((rateLimit > 0) && (rateLimit < NETWORK_BUFF_LEN_SIZE)) {
            rateLimit = NETWORK_BUFF_LEN_SIZE;
            logger.log(Level.WARNING, " The rate limit (-limit) is too small. It will be set to " + rateLimit
                    + " Bytes/s");
        }
        configMap.put("-limit", String.valueOf(rateLimit));
        rateLimitDelayMillis = Utils.getLongValue(configMap, "-limitDelay", 300L);
        configMap.put("-limitDelay", String.valueOf(rateLimitDelayMillis));

        preFilters = Utils.getStringValue(configMap, "-preFilters", null);
        postFilters = Utils.getStringValue(configMap, "-postFilters", null);
        monID = Utils.getStringValue(configMap, "-monID", null);

        this.massStorageConfig = Utils.getStringValue(configMap, "-ms", null);
        this.massStorageType = Utils.getStringValue(configMap, "-mst", null);

        try {
            if ((massStorageType() != null) && massStorageType().equals("dcache")) {
                System.setProperty("lia.util.net.common.FileChannelProviderFactory",
                        "edu.caltech.hep.dcapj.dCacheFileChannelProviderFactory");
            }
        } catch (Throwable e) {
            System.err.println("FDT was unable to set the FileChannelProviderFactory env variable");
            e.printStackTrace();
            System.out.flush();
            System.err.flush();
            System.exit(2502);
        }

        final String className = System.getProperty("lia.util.net.common.FileChannelProviderFactory");
        FileChannelProviderFactory tmpFCPF = null;

        if (className != null) {
            logger.log(Level.INFO, "Trying to load user defined FileChannelProviderFactory: " + className);
            try {
                final Class<?> c = Class.forName(className, true, ClassLoader.getSystemClassLoader());
                tmpFCPF = (FileChannelProviderFactory) c.newInstance();
            } catch (Throwable t) {
                throw new InvalidFDTParameterException("Unable to load the FileChannelProviderFactory", t);
            }
        } else {
            tmpFCPF = new PosixFSFileChannelProviderFactory();
        }

        this.fileChannelProviderFactory = tmpFCPF;

        // should not get here
        if (this.fileChannelProviderFactory == null) {
            throw new InvalidFDTParameterException("The FileChannelProviderFactory cannot be null!");
        }

        logger.log(Level.INFO, "Using " + this.fileChannelProviderFactory.getClass().getName()
                + " as FileChannelProviderFactory");

        hostname = Utils.getStringValue(configMap, "-c", null);

        if ((hostname != null) && (hostname.length() == 0)) {
            hostname = null;
        } else {
            isPullMode = (configMap.get("-pull") != null) || (configMap.get("-sID") != null);
            if (isPullMode) {
                configMap.put("-pull", "");
            }
        }

        final long ka = Utils.getLongValue(configMap, "-ka", TimeUnit.NANOSECONDS.toSeconds(DEFAULT_KEEP_ALIVE_NANOS));
        this.keepAliveDelayNanos = (ka < 0) ? DEFAULT_KEEP_ALIVE_NANOS : TimeUnit.SECONDS.toNanos(ka);
        configMap.put("-ka", String.valueOf(TimeUnit.NANOSECONDS.toSeconds(this.keepAliveDelayNanos)));

        portNo = Utils.getIntValue(configMap, "-p", DEFAULT_PORT_NO);
        transportPorts = Utils.getTransportPortsValue(configMap, "-tp", DEFAULT_PORT_NO);
        isCoordinatorMode = Boolean.getBoolean("coordinator");
        isThirdPartyCopyAgent = (configMap.get("-agent") != null);

        portNoGSI = Utils.getIntValue(configMap, "-gsip", DEFAULT_PORT_NO_GSI);
        portNoSSH = Utils.getIntValue(configMap, "-sshp", DEFAULT_PORT_NO_SSH);
        IORetryFactor = Utils.getIntValue(configMap, "-iof", 1);
        int avP = Runtime.getRuntime().availableProcessors();
        if (avP < 1) {
            avP = 1;
        }
        selectorsNo = Utils.getIntValue(configMap, "-sn", avP);
        readersCount = Utils.getIntValue(configMap, "-rCount", 1);
        writersCount = Utils.getIntValue(configMap, "-wCount", 1);
        maxPartitionsCount = Utils.getIntValue(configMap, "-pCount", 100);

        consoleReportingTaskDelay = Utils.getLongValue(configMap, "-reportDelay", 5);

        isNagleEnabled = (configMap.get("-N") != null);
        isNetTest = (configMap.get("-nettest") != null);
        isGenTest = (configMap.get("-genb") != null);
        bGSIMode = (configMap.get("-gsi") != null);
        isBlocking = (configMap.get("-bio") != null);
        if (!isBlocking) {
            isBlocking = (configMap.get("-nbio") == null);
        }

        apMonHosts = Utils.getStringValue(configMap, "-enable_apmon", null);
        bRecursive = (configMap.get("-r") != null);
        bUseFixedBlocks = (configMap.get("-fbs") != null);
        bLocalLoop = (configMap.get("-ll") != null);
        bLoop = (configMap.get("-loop") != null);
        isLisaRestartEnabled = (configMap.get("-enableLisaRestart") != null);
        bCheckUpdate = (configMap.get("-u") != null);
        destDir = Utils.getStringValue(configMap, "-d", null);
        sIP = Utils.getStringValue(configMap, "-sIP", null);
        dIP = Utils.getStringValue(configMap, "-dIP", null);
        destPort = Utils.getIntValue(configMap, "-dp", -1);
        remoteTransferPort = -1;
        listFilesFrom = Utils.getStringValue(configMap, "-ls", null);
        bComputeMD5 = (configMap.get("-md5") != null);
        sshKeyPath = Utils.getStringValue(configMap, "-sshKey", null);

        if (isNetTest) {
            destDir = "/dev/null";
            @SuppressWarnings("unchecked")
            List<String> lastParams = (List<String>) configMap.get("LastParams");
            lastParams.add("/dev/zero");
        }

        if ((hostname != null) && (((destDir == null) || (destDir.length() == 0)) && listFilesFrom == null)) {
            throw new IllegalArgumentException("No destination specified");
        }

        // process local storage params
        if (this.massStorageConfig != null) {
            this.storageParams = new MassStorage();
            if (!this.storageParams.init(massStorageConfig)) {
                throw new IllegalArgumentException("Invalid mass storage configuration file");
            }
        }

        if (configMap.get("-printStats") != null) {
            statsLevel = Level.INFO;
        }

        isNoTmpFlagSet = (configMap.get("-notmp") != null);
        isNoLockFlagSet = ((configMap.get("-nolock") != null) || (configMap.get("-nolocks") != null));

        writeMode = Utils.getStringValue(configMap, "-writeMode", null);

        String sLisa = Utils.getStringValue(configMap, "-lisafdtclient", null);
        if (sLisa == null) {
            sLisa = Utils.getStringValue(configMap, "-lisafdtserver", null);

            if (sLisa != null) {
                sLisa = Utils.getStringValue(configMap, "-lisafdtserver", "127.0.0.1:11001");
            }

        } else {
            sLisa = Utils.getStringValue(configMap, "-lisafdtclient", "127.0.0.1:11001");
        }

        if (sLisa == null) {
            bDisableLisa = true;
        }

        lisaReportInterval = Utils.getIntValue(configMap, "-lisa_rep_delay", 20);
        apMonReportInterval = Utils.getIntValue(configMap, "-apmon_rep_delay", 20);

        lisaHost = null;
        lisaPort = -1;

        if (!bDisableLisa) {
            try {
                int idx = sLisa.indexOf(":");
                if (idx > 0) {
                    lisaHost = sLisa.substring(0, idx);
                    lisaPort = Integer.parseInt(sLisa.substring(idx + 1));
                } else if (idx == 0) {// only port
                    lisaHost = "127.0.0.1";
                    lisaPort = Integer.parseInt(sLisa.substring(1));
                } else {
                    lisaHost = sLisa;
                    lisaPort = 11001;
                }
            } catch (Throwable t) {
                if (lisaHost == null) {
                    lisaHost = "127.0.0.1";
                }
                if (lisaPort == -1) {
                    lisaPort = 11001;
                }
            }
        }

        /* start the server through SSH (just for client side)? */
        if (/* configMap.get("-ssh") != null || */configMap.get("SCPSyntaxUsed") != null) {
            // use GSI-SSH?
            if (configMap.get("-gsissh") != null) {
                bGSISSHMode = true;
            }
            bSSHMode = true;
            Object oSrc = configMap.get("sourceUsers");
            if (oSrc != null) {
                String[] aSourceUsers = (String[]) oSrc;
                if (aSourceUsers.length > 0) {
                    this.aSourceUsers = aSourceUsers;
                }
            }
            oSrc = configMap.get("sourceHosts");
            if (oSrc != null) {
                String[] aSourceHosts = (String[]) oSrc;
                if (aSourceHosts.length > 0) {
                    this.aSourceHosts = aSourceHosts;
                }
            }
            sDestinationUser = Utils.getStringValue(configMap, "destinationUser", System.getProperty("user.name"));
            // sDestinationUser = ( sDestinationUser==null?System.getProperty("user.name"):sDestinationUser );
            sLocalAddresses = Utils.getStringValue(configMap, "-local", null);
            sStartServerCommand = Utils.getStringValue(configMap, "-remote", "java -jar fdt.jar");
        }

        if (configMap.containsKey("-f") && !configMap.containsKey("-F")) {
            String sAllowedHosts = Utils.getStringValue(configMap, "-f", "");
            if (sAllowedHosts.trim().length() == 0) {
                // no hosts suplied, try to guess from the SSH_CLIENT env var
                sAllowedHosts = System.getenv("SSH_CLIENT");
                if (sAllowedHosts != null) {
                    sAllowedHosts = sAllowedHosts.split("(\\s)+")[0];
                }
                // cannot continue, raise exception
                if ((sAllowedHosts == null) || (sAllowedHosts.length() == 0)) {
                    throw new InvalidFDTParameterException("source filter used but no host/ip supplied");
                }
            }
            sourceAddressFilter = new NetMatcher(sAllowedHosts.split(":"));
        }

        // try to get the fileList[]
        @SuppressWarnings("unchecked")
        List<String> lastParams = (List<String>) configMap.get("LastParams");
        if ((lastParams == null) || lastParams.isEmpty()) {
            String fList = Utils.getStringValue(configMap, "-fl", null);
            if ((fList != null) && (fList.length() != 0)) {
                //source files
                List<String> arrayFileList = new ArrayList<String>();
                //must accept null values
                List<String> remappedArrayFileList = new ArrayList<String>();
                BufferedReader br = null;
                FileReader fr = null;
                try {
                    final Pattern splitPattern = Pattern.compile(REGEX_REMAP_DELIMITER);
                    fr = new FileReader(fList);
                    br = new BufferedReader(fr);
                    String line = br.readLine();
                    while (line != null) {
                        final String[] tkns = splitPattern.split(line);
                        final int tknsCount = tkns.length;
                        if (tknsCount == 1) {
                            arrayFileList.add(line);
                            remappedArrayFileList.add(null);
                        } else if (tknsCount == 2) {
                            arrayFileList.add(tkns[0]);
                            remappedArrayFileList.add(tkns[1]);
                        } else {
                            throw new IllegalArgumentException("The line=" + line
                                    + ", from -fl parameter cannot be parsed");
                        }
                        line = br.readLine();
                    }
                    remappedFileList = remappedArrayFileList.toArray(new String[0]);
                    fileList = arrayFileList.toArray(new String[0]);
                } catch (Throwable t) {
                    throw new IllegalArgumentException("Unable to decode file list", t);
                } finally {
                    Utils.closeIgnoringExceptions(fr);
                    Utils.closeIgnoringExceptions(br);
                }
            } else if (configMap.get("-sID") != null) {
                String sessionID = (String) configMap.get("-sID");
                String[] logFiles = getLogFiles(sessionID);
                remappedFileList = logFiles;
                fileList = logFiles;
            }
        } else {
            configMap.remove("-fl");
            fileList = lastParams.toArray(new String[lastParams.size()]);
            if ((fileList != null) && (fileList.length == 0)) {
                fileList = null;
            }
        }
        Object files = configMap.get("Files");
        if ((files != null) && (files instanceof String[]) && (((String[]) files).length > 0)) {
            fileList = (String[]) files;
        }

        logger.log(Level.INFO, "FDT started in {0} mode", getFDTMode(configMap));
        if (hostname != null) {// client mode
            if (logger.isLoggable(Level.FINE)) {
                StringBuilder sb = new StringBuilder();
                sb.append("Source file list --> remaped file list:\n");
                final boolean bRemapFLNull = (remappedFileList == null);
                for (int i = 0; (fileList != null) && (i < fileList.length); i++) {
                    sb.append(fileList[i])
                            .append(" ---> ")
                            .append((bRemapFLNull || (remappedFileList[i] == null)) ? " default mapping: "
                                    + fileList[i] : " remapped to: " + remappedFileList[i]).append("\n");
                }
                logger.log(Level.FINE, sb.toString());
                logger.log(Level.FINE, "Remote destination directory: {0}\nRemote host: {1} port: {2}", new Object[]{
                        destDir, hostname, portNo});
            }
        } else {// server mode
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.INFO, "Local server will try to bind on port:{0}", portNo);
            }
        }
    }

    private String[] getLogFiles(String sessionID) {
        return new String[]{"/tmp/" + sessionID + ".log"};
    }

    public String getListFilesFrom() {
        return listFilesFrom;
    }

    public void setListFilesFrom(String listFilesFrom) {
        this.listFilesFrom = listFilesFrom;
    }

    private String getFDTMode(Map<String, Object> configMap) {
        if (configMap.get("-coord") != null) {
            return "coordinator";
        } else if (configMap.get("-ls") != null) {
            return "list files";
        } else if (configMap.get("-agent") != null) {
            return "agent worker";
        }
        return (hostname == null) && (configMap.get("SCPSyntaxUsed") == null) ? "server" : "client";
    }

    public static int getBulkSockConnect() {
        return 30;
    }

    public long getKeepAliveDelay(TimeUnit unit) {
        return unit.convert(keepAliveDelayNanos, TimeUnit.NANOSECONDS);
    }

    public static long getBulkSockConnectWait() {
        return 1500;
    }

    public Map<String, Object> getConfigMap() {
        return configMap;
    }

    public void setConfigMap(Map<String, Object> configMap) {
        this.configMap = configMap;
    }

    public static final String getUsage() {
        return Utils.getUsage();
    }

    public static int getMaxTakePollIter() {
        return 1000;
    }

    public long getReportingTaskDelay() {
        return consoleReportingTaskDelay;
    }

    public Level getStatsLevel() {
        return statsLevel;
    }

    public String getMonID() {
        return monID;
    }

    public static final Config getInstance() {
        synchronized (Config.class) {
            while (_thisInstance == null) {
                try {
                    Config.class.wait();
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        }

        return _thisInstance;
    }

    public int getRetryIOCount() {
        return IORetryFactor;
    }

    public static final void initInstance(final Map<String, Object> configMap) throws Exception {

        synchronized (Config.class) {
            if (_thisInstance == null) {
                _thisInstance = new Config(configMap);
            }
            Config.class.notifyAll();
        }
    }

    public int getByteBufferSize() {
        return byteBufferSize;
    }

    public boolean isNagleEnabled() {
        return isNagleEnabled;
    }

    public boolean isGSIModeEnabled() {
        return bGSIMode;
    }

    public int getSockNum() {
        return sockNum;
    }

    public long getLisaReportingInterval() {
        return lisaReportInterval;
    }

    public long getApMonReportingInterval() {
        return apMonReportInterval;
    }

    /**
     * @return the rate in Bytes/s
     */
    public long getRateLimit() {
        return rateLimit;
    }

    public long getRateLimitDelay() {
        return rateLimitDelayMillis;
    }

    public int getSockBufSize() {
        return sockBufSize;
    }

    public String getLisaHost() {
        return lisaHost;
    }

    public int getLisaPort() {
        return lisaPort;
    }

    public String getSshKeyPath() {
        return sshKeyPath;
    }

    public boolean isNoTmpFlagSet() {
        return isNoTmpFlagSet;
    }

    public boolean isNoLockFlagSet() {
        return isNoLockFlagSet;
    }

    public void setHostName(String hostname) {
        this.configMap.put("-destinationHost", hostname);
        this.hostname = hostname;
    }

    public String getHostName() {
        return hostname;
    }

    public int getPort() {
        return portNo;
    }

    public int getGSIPort() {
        return portNoGSI;
    }

    public int getSSHPort() {
        return portNoSSH;
    }

    public void setPortNo(int port) {
        this.portNo = port;
    }

    public void setGSIPort(int port) {
        this.portNoGSI = port;
    }

    public void setSSHPort(int port) {
        this.portNoSSH = port;
    }

    public boolean isStandAlone() {
        return isStandAlone;
    }

    public String getWriteMode() {
        return writeMode;
    }

    public String[] getFileList() {
        return fileList;
    }

    public String[] getRemappedFileList() {
        return remappedFileList;
    }

    public String getPreFilters() {
        return preFilters;
    }

    public String getPostFilters() {
        return postFilters;
    }

    public void setDestinationDir(String destDir) {
        this.destDir = destDir;
    }

    public String getDestinationDir() {
        return destDir;
    }

    public void setDestinationPort(int destPort) {
        this.destPort = destPort;
    }

    public int getDestinationPort() {
        return destPort;
    }

    public void setRemoteTransferPort(int remoteTransferPort) {
        this.remoteTransferPort = remoteTransferPort;
    }

    public void registerTransferPortForSession(int newTransferPort, String sessionID) {
        this.sessionPortMap.put(sessionID, newTransferPort);
    }

    public int getNewRemoteTransferPort() {
        try {
            if (!transportPorts.isEmpty()) {
                int rtp = this.transportPorts.poll(20, TimeUnit.SECONDS);
                System.out.println("Took new remote transfer port " + rtp);
                return rtp;
            }
        } catch (Exception e) {
            if (transportPorts.size() == 0) {
                logger.log(Level.WARNING, "No transfer ports defined or no free transfer ports left...", e);
            } else {
                logger.log(Level.WARNING, "Failed to retrieve remote transfer port", e);
            }
        }
        return -1;
    }

    public void setSessionSocket(ServerSocketChannel ssc, ServerSocket ss, SocketChannel sc, Socket s, int port) {
        List<Object> socks = new ArrayList<>();
        socks.add(ssc);
        socks.add(ss);
        socks.add(sc);
        socks.add(s);
        sessionSocketMap.put(port, socks);
    }

    public void releaseRemoteTransferPort(String sessionID) {
        if (sessionPortMap.keySet().contains(sessionID)) {
            logger.log(Level.FINER, "Trying to release transfer port from session " + sessionID);
            int sessionPort = sessionPortMap.get(sessionID);
            if (sessionPort > 0) {
                try {
                    transportPorts.put(sessionPort);
                    sessionPortMap.remove(sessionID);
                    closeSessionRelatedSocks(sessionSocketMap.get(sessionPort));
                } catch (InterruptedException e) {
                    logger.log(Level.WARNING, "Failed to release remote transfer port: " + remoteTransferPort, e);
                }
            }
        }
    }

    private static void closeSessionRelatedSocks(List<Object> socks) {
        if (socks != null) {
            for (Object o : socks) {
                if (o instanceof ServerSocketChannel) {
                    try {
                        ((ServerSocketChannel) o).close();
                    } catch (IOException e) {
                        logger.log(Level.WARNING, "Failed to close ServerSocketChannel", e);
                    }
                }
                if (o instanceof ServerSocket) {
                    try {
                        ((ServerSocket) o).close();
                    } catch (IOException e) {
                        logger.log(Level.WARNING, "Failed to close ServerSocket", e);
                    }
                }
                if (o instanceof SocketChannel) {
                    try {
                        ((SocketChannel) o).close();
                    } catch (IOException e) {
                        logger.log(Level.WARNING, "Failed to close SocketChannel", e);
                    }
                }
                if (o instanceof Socket) {
                    try {
                        ((Socket) o).close();
                    } catch (IOException e) {
                        logger.log(Level.WARNING, "Failed to close Socket", e);
                    }
                }
            }
        }
    }

    public int getRemoteTransferPort() {
        return remoteTransferPort;
    }

    public String getSourceIP() {
        return sIP;
    }

    public String getDestinationIP() {
        return dIP;
    }

    public void setDestinationIP(String dIP) {
        this.dIP = dIP;
    }

    public void setFileList(String[] fileList) {
        this.fileList = fileList;
    }

    public void setLisaPort(int lisaPort) {
        this.lisaPort = lisaPort;
    }


    // TODO - As param ...
    public int getNumberOfSelectors() {
        return selectorsNo;
    }

    public String getApMonHosts() {
        return apMonHosts;
    }

    public void setPullMode(boolean pullMode) {
        this.isPullMode = pullMode;
        if (pullMode) {
            this.configMap.put("-pull", true);
        } else {
            this.configMap.remove("-pull");
        }
    }

    public void setCoordinatorMode(boolean coordinatorMode) {
        this.isCoordinatorMode = coordinatorMode;
        if (coordinatorMode) {
            this.configMap.put("-coord", true);
        } else {
            this.configMap.remove("-coord");
        }
    }

    public void setThirdPartyCopyAgent(boolean isThirdPartyCopyAgent) {
        this.isThirdPartyCopyAgent = isThirdPartyCopyAgent;
        if (isThirdPartyCopyAgent) {
            this.configMap.put("-agent", true);
        } else {
            this.configMap.remove("-agent");
        }
    }

    public void setRetrievingLogFile(Object sessionID) {
        this.isRetrievingLogFile = sessionID != null;
        if (isRetrievingLogFile) {
            this.configMap.put("-sID", (String) sessionID);
        } else {
            this.configMap.remove("-sID");
        }
    }

    public boolean isCoordinatorMode() {
        return isCoordinatorMode;
    }

    public boolean isListFilesMode() {
        return listFilesFrom != null && configMap.get("-ls") != null;
    }

    public boolean isRetrievingLogFile() {
        return isRetrievingLogFile || configMap.containsKey("-sID");
    }


    public boolean isPullMode() {
        return isPullMode;
    }

    public boolean shouldUpdate() {
        return bCheckUpdate;
    }

    public boolean isRecursive() {
        return bRecursive;
    }

    public boolean isLisaDisabled() {
        return bDisableLisa;
    }

    public boolean loop() {
        return bLoop;
    }

    public boolean localLoop() {
        return bLocalLoop;
    }

    public boolean useFixedBlocks() {
        return bUseFixedBlocks;
    }

    public NetMatcher getSourceAddressFilter() {
        return sourceAddressFilter;
    }

    public boolean computeMD5() {
        return bComputeMD5;
    }

    public boolean isSSHModeEnabled() {
        return bSSHMode;
    }

    public boolean isGSISSHModeEnabled() {
        return bGSISSHMode;
    }

    /**
     * Check if remote server is needed. We use SSH channels to control remote startup. <br>
     * In SSH/SCP mode we have three types of syntax we need to support:
     * <ul>
     * <li>fdt /local/path [user]@remotehost:/remote/path : <br>
     * In this case a remote server si started on remote host and client starts in "PUSH" mode The server accept
     * connection just from the the given client and exits when the transfer finishes
     * <li>fdt /local/path [user]@remotehost:/remote/path : <br>
     * In this case a remote server si started on remote host and client starts in "PULL" mode The server accept
     * connection just from the the given client and exits when the transfer finishes
     * <li>fdt [user]@remotehost1:/remote/path1 [user]@remotehost2:/remote/path2 : <br>
     * In this case both the server and the client are started remotely (the local fdt acts as an agent for the transfer
     * </ul>
     */
    // different client/server configuration set using SSH
    public static final int SSH_NO_REMOTE = -1;
    // REMOTE server, local client in push mode
    public static final int SSH_REMOTE_SERVER_LOCAL_CLIENT_PUSH = 1;
    // REMOTE server, local client in pull mode
    public static final int SSH_REMOTE_SERVER_LOCAL_CLIENT_PULL = 2;
    // REMOTE server, REMOTE client in push mode)
    public static final int SSH_REMOTE_SERVER_REMOTE_CLIENT_PUSH = 3;

    public int getReadersCount() {
        return readersCount;
    }

    public int getWritersCount() {
        return writersCount;
    }

    public int getMaxPartitionCount() {
        return maxPartitionsCount;
    }

    public int getSSHConfig() {

        if (!bSSHMode) {
            return SSH_NO_REMOTE;
        }
        final String sDestinationHost = Utils.getStringValue(configMap, "destinationHost", null);
        if (((aSourceHosts == null) || (aSourceHosts.length == 0) || (aSourceHosts[0] == null))
                && (sDestinationHost != null)) // /local/path [user]@remotehost:/remote/path
        // the client is locally
        {
            return SSH_REMOTE_SERVER_LOCAL_CLIENT_PUSH;
        }

        if ((aSourceHosts != null) && (aSourceHosts.length > 0) && (sDestinationHost == null)) {
            // [2] [user]@remotehost:/remote/path /local/path
            return SSH_REMOTE_SERVER_LOCAL_CLIENT_PULL;
        }

        // [3] [user]@remotehost:/remote/path [user]@remotehost:/remote/path1
        return SSH_REMOTE_SERVER_REMOTE_CLIENT_PUSH;

    }

    public boolean isBlocking() {
        return isBlocking;
    }

    public String getDestinationUser() {
        return sDestinationUser;
    }

    public boolean isLisaRestartEnabled() {
        return isLisaRestartEnabled;
    }

    public String getLocalAddresses() {
        return this.sLocalAddresses == null ? LocalHost.getStringPublicIPs4() : this.sLocalAddresses;
    }

    public String getRemoteCommand() {
        return this.sStartServerCommand;
    }

    public String[] getSourceUsers() {
        return this.aSourceUsers;
    }

    public String[] getSourceHosts() {
        return this.aSourceHosts;
    }

    public String massStorageConfig() {
        return this.massStorageConfig;
    }

    public String massStorageType() {
        return this.massStorageType;
    }

    public MassStorage storageParams() {
        return this.storageParams;
    }

    public boolean isNetTest() {
        return isNetTest;
    }

    public boolean isGenTest() {
        return isGenTest;
    }

    public void setLogLevel(String logLevel) {
        this.logLevel = logLevel;
    }

    public String getLogLevel() {
        return logLevel;
    }

    public FileChannelProviderFactory getFileChannelProviderFactory() {
        return this.fileChannelProviderFactory;
    }

    public boolean isThirdPartyCopyAgent() {
        return this.isThirdPartyCopyAgent;
    }
}
