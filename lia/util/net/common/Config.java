package lia.util.net.common;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Config {
    
    private static final Logger logger = Logger.getLogger(Config.class.getName());
    
    
    
    public static final int NETWORK_BUFF_LEN_SIZE = 8 * 1024;
    
    
    
    private final static String[] exportedSysProps = { "user.name", "user.home", "user.dir", "file.separator", "file.encoding", "path.separator" };
    
    public static final String SINGLE_ARGS[] = { "-v", "-vv", "-vvv", "-loop", "-r", "-pull", "-printStats", "-N", "-bio", "-gsi", "-gsissh" };
    
    
    public static final String FDT_MAJOR_VERSION = "0";
    public static final String FDT_MINOR_VERSION = "6";
    public static final String FDT_MAINTENANCE_VERSION = "0";
    
    public static final String FDT_FULL_VERSION = FDT_MAJOR_VERSION + "." + FDT_MINOR_VERSION + "." + FDT_MAINTENANCE_VERSION;
    public static final String FDT_RELEASE_DATE = "2007-03-29";
    
    private volatile static boolean initialized;
    private static Config _thisInstance;
    
    
    
    public static final int HEADER_SIZE = 56;
    public static final int HEADER_SIZE_v2 = 56;
    
    
    public int DEFAULT_BUFFER_SIZE = 8 * 1024 * 1024; 
    private int byteBufferSize = DEFAULT_BUFFER_SIZE;
    
    
    private boolean isNagleEnabled;
    
    
    private boolean isPullMode = false;
    
    
    public static final Object BIG_FDTAPP_LOCK = new Object();
    
    
    public static final int DEFAULT_SOCKET_NO = 4;
    private int sockNum = DEFAULT_SOCKET_NO;
    
    public static final int DEFAULT_PORT_NO = 54321;
    public static final int DEFAULT_PORT_NO_GSI = 54320;
    
    private int sockBufSize = -1;
    private long rateLimit = -1;
    
    private String hostname;
    
    private String lisaHost;
    private int lisaPort;
    
    private int portNo;
    private int portNoGSI;
    
    private boolean isStandAlone;
    
    private String[] fileList;
    
    private String destDir;
    
    private boolean bWithApMon = true;
    
    private boolean bComputeMD5 = false;
    
    private boolean bRecursive = false;
    
    private boolean bCheckUpdate = false;
    
    private boolean isBlocking = false;
    
    private boolean bUseFixedBlocks = false;
    
    private boolean bLocalLoop = false;
    
    private boolean bLoop = false;
    
    private NetMatcher sourceAddressFilter = null;
    
    private int IORetryFactor = 1;
    
    private int selectorsNo = 1;
    
    private double transferLimit = -1;
    
    private boolean bDisableLisa = false;
    
    private long lisaReportInterval = 20;
    
    
    private boolean bSSHMode = false;
    
    private boolean bGSISSHMode = false;
    private boolean bGSIMode = false;
    
    private String[] aSourceUsers = null;
    private String[] aSourceHosts = null;
    private String sDestinationUser = null;
    private String sLocalAddresses = null;
    private String sStartServerCommand = null;
    private boolean isLisaRestartEnabled;
    
    private String writeMode;
    private String preFilters;
    private String postFilters;
    
    private String massStorageConfig = null;
    private MassStorage storageParams = null;
    
    private Level statsLevel = null;
    
    private final HashMap<String, Object> configMap;
    
    private static final String usage = "\n"
            + "Usage: java -jar fdt.jar [ OPTIONS ]\n"
            + "       java -jar fdt.jar [ OPTIONS ] -c <host> STANDALONE_ARGS\n"
            + "       java -jar fdt.jar [ OPTIONS ] SCP_SYNTAX\n"
            + "\n"
            + "\nSTANDALONE_ARGS: [file1 ...]|[-fl <fileList>]"
            + "\nSCP_SYNTAX: [-gsissh] [[user@][host1:]]file1 [[user@][host2:]]file2"
            + "\n\nClient specific:"
            + "\n   -c <host>\t\t connect to the specified <host>"
            + "\n   \t\t\t If this parameter is missing FDT will become server"
            + "\n   -d <destinationDir>\t The destination directory used to copy the files"
            + "\n   -fl <fileList>\t a list of files (MUST containe one file per line)"
            + "\n   -r\t\t\t Recursive. Searches for files in all specified "
            + "\n   \t\t\t directories and subdirectories"
            + "\n   -pull\t\t Pull Mode.The client will receive the data from"
            + "\n   \t\t\t the server"
            + "\n   -ss <windowSize>\t Set TCP SO_SND_BUFFER window size to windowSize"
            + "\n   \t\t\t [K(ilo)|M(ega)] may be used as suffixes"
            + "\n   -P <numberOfStreams>\t number of paralel streams(sockets) to use"
            + "\n   \t\t\t Default is 1"
            + "\n   -limit <rate>\t Restrict the transfer speed at the specified rate"
            + "\n   \t\t\t [K(ilo)|M(ega)] may be used. If no suffix is specified"
            + "\n   \t\t\t Bytes/s is considered to be the default."
            + "\n   -N\t\t\t disable Nagle's algorithm"
            + "\n   -gsissh\t\t uses gsi over ssh authentication. It is presumed that"
            + "\n   \t\t\t the remote sshd server supports GSI authentication"
            + "\n\nServer specific:"
            + "\n   -S\t\t\t disable standalone mode; if specified, the server"
            + "\n   \t\t\t will stop after the last client finishes"
            + "\n   -bs <bufferSize>\t Size for the I/O buffers. [K(ilo)|M(ega)] may be used"
            + "\n   \t\t\t as suffixes. The default is 512K."
            + "\n   -f <allowedIPsList>\t A list of IP addresses allowed to connect to the server"
            + "\n   \t\t\t Multiple IP addresses may be separated by ':'"
            + "\n\nClient/Server:"
            + "\n   -gsi\t\t\t enables the GSI authentication scheme. It must be used"
            + "\n   \t\t\t for both the FDT client and server.If the flag is"
            + "\n   \t\t\t enabled, the FDT server will accept only GSI"
            + "\n   \t\t\t authenticated FDT clients."
            + "\n   -p <portNumber>\t port number to listen on/connect to (server/client)"
            + "\n   \t\t\t or to connect to for the client.Default is 54321."
            + "\n   -gsip <GSICtrlPort>\t the GSI control port to listen on/connect to"
            + "\n   \t\t\t (server/client) for GSI authentication. In the GSI"
            + "\n   \t\t\t mode FDT will use two ports: one for control(-gsip)"
            + "\n   \t\t\t and one for the data channels(-p). Default value for"
            + "\n   \t\t\t GSICtrlPort is 54320."
            + "\n   -preFilters f1,..,fn\t User defined pre-processing filters. The"
            + "\n   \t\t\t preProcessing filters must be in the classpath and"
            + "\n   \t\t\t may be cascadated. f1,...,fn are java classes, which"
            + "\n   \t\t\t will be loaded in the preProcessing phase.They must be"
            + "\n   \t\t\t specified in the FDT \"sender\" command line."
            + "\n   -postFilters f1,..,fn User defined post-processing filters. The"
            + "\n   \t\t\t postProcessing filters must be in the classpath and"
            + "\n   \t\t\t may be cascadated. f1,...,fn are java classes, which"
            + "\n   \t\t\t will be loaded in the postProcessing phase.They must be"
            + "\n   \t\t\t specified in the FDT \"receiver\" command line."
            + "\n   -ms <properties_file> Java properties file for transfers"
            + "\n   \t\t\t to/from storage. By default mass storage is not used."
            + "\n   -md5\t\t\t enables MD5 checksum for every file transfered."
            + "\n   \t\t\t It must be specified in the \"sender\" and the"
            + "\n   \t\t\t \"receiver\" will print am `md5sum`-like list at the end"
            + "\n   \t\t\t of the transfer. Default is the checksum is disabled."
            
            
            + "\n   -bio\t\t\t Blocking I/O mode. In the blocking mode every"
            + "\n   \t\t\t stream(socket) will use a thread to perform the I/O."
            + "\n   \t\t\t By default non-blocking mode is used"
            + "\n   -iof <iof>\t\t Non blocking I/O retry factor. Repeat every"
            + "\n   \t\t\t read/write operation (non-blocking I/O mode only),"
            + "\n   \t\t\t which returns 0, up to <iof> times before waiting for"
            + "\n   \t\t\t I/O readiness.Default is 1, which should be ok, but "
            + "\n   \t\t\t values of 2 or 3 may show slight gains in network I/O"
            + "\n   \t\t\t performance. Usual values are between 1 and 4."
            + "\n   \t\t\t Higher values are not recommended because the gain"
            + "\n   \t\t\t will be zero while the CPU System will increase"
            + "\n   -printStats\t\t Print statistics for buffer pools, sessions, etc"
            + "\n   -v\t\t\t Verbose mode. Multiple 'v'-s may be used to increase"
            + "\n   \t\t\t the verbosity level. Maximum level is 3 (-vvv), which"
            + "\n   \t\t\t corresponds to Level.FINEST for the standard Java"
            + "\n   \t\t\t logging system used by FDT"
            + "\n\nMiscellaneous:"
            + "\n   -u, -update,--update\t Update the fdt.jar, if a newer version"
            + "\n   \t\t\t is available on the update server and exits"
            + "\n   -V, -version,\t print version information and quit"
            + "\n   --version"
            + "\n   -h, -help, --help\t print this help message and quit"
            + "\n";
    
    
    private Config(final FDTCommandLine fdtCommandLine) throws InvalidFDTParameterException{
        this.fileList = null;
        this.configMap = null;
        if (fdtCommandLine.getOptionsMap() == null)
            return;
        
    }
    
    private void fillOptions(final HashMap<String, Object> configMap) {
        if (configMap == null)
            return;
    }
    
    
    private Config(final HashMap<String, Object> configMap) throws InvalidFDTParameterException {
        this.configMap = configMap;
        if (configMap == null)
            return;
        
        for (int i = 0; i < exportedSysProps.length; i++) {
            configMap.put(exportedSysProps[i], System.getProperty(exportedSysProps[i]));
        }
        
        isStandAlone = (configMap.get("-S") == null);
        byteBufferSize = getIntValue(configMap, "-bs", DEFAULT_BUFFER_SIZE);
        configMap.put("-bs", "" + byteBufferSize);
        
        sockNum = getIntValue(configMap, "-P", DEFAULT_SOCKET_NO);
        configMap.put("-P", "" + sockNum);
        
        sockBufSize = getIntValue(configMap, "-ss", -1);
        configMap.put("-ss", "" + sockBufSize);
        
        rateLimit = getLongValue(configMap, "-limit", -1);
        if(rateLimit > 0 && rateLimit < NETWORK_BUFF_LEN_SIZE) {
            rateLimit = 2 * ( NETWORK_BUFF_LEN_SIZE + 1 );
            logger.log(Level.WARNING, " The rate limit (-limit) is too small. It will be set to " + rateLimit + " Bytes/s");
        }
        configMap.put("-limit", "" + rateLimit);
        
        
        preFilters = getStringValue(configMap, "-preFilters", null);
        postFilters = getStringValue(configMap, "-postFilters", null);
        
        this.massStorageConfig = getStringValue(configMap, "-ms", null);
        
        hostname = getStringValue(configMap, "-c", null);
        
        if (hostname != null && hostname.length() == 0) {
            hostname = null;
        } else {
            isPullMode = (configMap.get("-pull") != null);
        }
        
        portNo = getIntValue(configMap, "-p", DEFAULT_PORT_NO);
        portNoGSI = getIntValue(configMap, "-gsip", DEFAULT_PORT_NO_GSI);
        IORetryFactor = getIntValue(configMap, "-iof", 1);
        selectorsNo = getIntValue(configMap, "-sn", 1);
        
        isNagleEnabled = (configMap.get("-N") != null);
        bGSIMode = (configMap.get("-gsi") != null);
        isBlocking = (configMap.get("-bio") != null);
        bWithApMon = (configMap.get("-no_apmon") == null);
        bRecursive = (configMap.get("-r") != null);
        bUseFixedBlocks = (configMap.get("-fbs") != null);
        bLocalLoop = (configMap.get("-ll") != null);
        bLoop = (configMap.get("-loop") != null);
        isLisaRestartEnabled = (configMap.get("-enableLisaRestart") != null);
        bCheckUpdate = (configMap.get("-u") != null);
        destDir = getStringValue(configMap, "-d", null);
        bComputeMD5 = (configMap.get("-md5") != null);

        if(hostname != null && ( destDir == null || destDir.length() == 0 )) {
            throw new IllegalArgumentException("No destination specified");
        }
        
        
        if( this.massStorageConfig != null ) {
            this.storageParams = new MassStorage();
            if( !this.storageParams.init(massStorageConfig) ) {
                throw new IllegalArgumentException("Invalid mass storage configuration file"); 
            }
        }
               
        if (configMap.get("-printStats") != null) {
            statsLevel = Level.INFO;
        }
        
        writeMode = getStringValue(configMap, "-writeMode", null);
        
        String sLisa = getStringValue(configMap, "-lisafdtclient", null);
        if (sLisa == null) {
            sLisa = getStringValue(configMap, "-lisafdtserver", null);
            
            if (sLisa != null) {
                sLisa = getStringValue(configMap, "-lisafdtserver", "127.0.0.1:11001");
            }
            
        } else {
            sLisa = getStringValue(configMap, "-lisafdtclient", "127.0.0.1:11001");
        }
        
        if (sLisa == null) {
            bDisableLisa = true;
        }
        
        lisaReportInterval = getIntValue(configMap, "-lisa_rep_delay", 20);
        
        lisaHost = null;
        lisaPort = -1;
        
        if (!bDisableLisa) {
            try {
                int idx = sLisa.indexOf(":");
                if (idx > 0) {
                    lisaHost = sLisa.substring(0, idx);
                    lisaPort = Integer.parseInt(sLisa.substring(idx + 1));
                } else if (idx == 0) {
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
        
        
        if (configMap.get("-ssh") != null || configMap.get("SCPSyntaxUsed") != null) {
        	
        	if(configMap.get("-gsissh") != null){
        		bGSISSHMode = true;
        	}
            bSSHMode = true;
            Object oSrc = configMap.get("sourceUsers");
            if (oSrc != null) {
                String[] aSourceUsers = (String[]) oSrc;
                if (aSourceUsers.length > 0)
                    this.aSourceUsers = aSourceUsers;
            }
            oSrc = configMap.get("sourceHosts");
            if (oSrc != null) {
                String[] aSourceHosts = (String[]) oSrc;
                if (aSourceHosts.length > 0)
                    this.aSourceHosts = aSourceHosts;
            }
            sDestinationUser = getStringValue(configMap, "destinationUser", System.getProperty("user.name"));

            sLocalAddresses = getStringValue(configMap, "-local", null);
            sStartServerCommand = getStringValue(configMap, "-remote", "java -jar fdt.jar "+"-p "+portNo);
        }
        
        if (configMap.containsKey("-f") && !configMap.containsKey("-F")) {
            String sAllowedHosts = getStringValue(configMap, "-f", "");
            if (sAllowedHosts.trim().length() == 0) {
                
                sAllowedHosts = System.getenv("SSH_CLIENT");
                if (sAllowedHosts != null)
                    sAllowedHosts = sAllowedHosts.split("(\\s)+")[0];
                
                if (sAllowedHosts == null || sAllowedHosts.length() == 0)
                    throw new InvalidFDTParameterException("source filter used but no host/ip supplied");
            }
            sourceAddressFilter = new NetMatcher(sAllowedHosts.split(":"));
        }
        
        String[] tmpFlist = null;
        
        String lastParams = getStringValue(configMap, "LastParams", null);
        if (lastParams == null || lastParams.length() == 0) {
            String fList = getStringValue(configMap, "-fl", null);
            if (fList != null && fList.length() != 0) {
                ArrayList<String> arrayFileList = new ArrayList<String>();
                BufferedReader br = null;
                try {
                    br = new BufferedReader(new FileReader(fList));
                    String line = br.readLine();
                    while (line != null) {
                        arrayFileList.add(line);
                        line = br.readLine();
                    }
                    
                    fileList = arrayFileList.toArray(new String[arrayFileList.size()]);
                } catch (Throwable t) {
                    System.err.println("Unable to read the file list data");
                    t.printStackTrace();
                    this.fileList = null;
                } finally {
                    try {
                        if (br != null) {
                            br.close();
                        }
                    } catch (Throwable ignore) {
                    }
                }
            }
        } else {
            configMap.remove("-fl");
            fileList = lastParams.trim().split("(\\s)+");
            if (fileList != null && fileList.length == 0) {
                fileList = null;
            }
        }
        Object files = configMap.get("Files");
        if (files != null && files instanceof String[] && ((String[]) files).length > 0)
            fileList = (String[]) files;
        ArrayList<String> arrayRecFileList = new ArrayList<String>();
        
        logger.log(Level.INFO, "FDT started in " + (hostname == null ? "server" : "client") + " mode");
        if (hostname != null) {
            if (logger.isLoggable(Level.FINE)) {
                StringBuilder sb = new StringBuilder();
                sb.append("Source file list:\n");
                for (int i = 0; fileList != null && i < fileList.length; i++) {
                    sb.append(fileList[i]).append("\n");
                }
                logger.log(Level.FINE, sb.toString());
                logger.log(Level.FINE, "Remote destination directory: " + destDir + "\nRemote host: " + hostname + " port: " + portNo);
            }
        } else {
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.INFO, "Local server will try to bind on port:" + portNo);
            }
        }
    }
    
    public HashMap<String, Object> getConfigMap() {
        return configMap;
    }
    
    public static final String getUsage() {
        return usage;
    }
    
    private static final String getStringValue(final HashMap<String, Object> configMap, String key, String DEFAULT_VALUE) {
        Object obj = configMap.get(key);
        if (obj == null) {
            return DEFAULT_VALUE;
        }
        
        return obj.toString();
    }
    
    public Level getStatsLevel() {
        return statsLevel;
    }
    
    public static final long getLongValue(final HashMap<String, Object> configMap, final String key, final long DEFAULT_VALUE) {
        long rVal = DEFAULT_VALUE;
        Object obj = configMap.get(key);
        if (obj == null)
            rVal = DEFAULT_VALUE;
        else {
            String cVal = obj.toString();
            if (cVal.length() == 0) {
                rVal = DEFAULT_VALUE;
            } else {
                try {
                    
                    long factor = 1;
                    if (cVal.endsWith("K") || cVal.endsWith("k")) {
                        factor = Utils.KILO;
                        cVal = cVal.substring(0, cVal.length() - 1);
                    } else if (cVal.endsWith("M") || cVal.endsWith("m")) {
                        factor = Utils.MEGA;
                        cVal = cVal.substring(0, cVal.length() - 1);
                    } else if (cVal.endsWith("G") || cVal.endsWith("g")) {
                        factor = Utils.GIGA;
                        cVal = cVal.substring(0, cVal.length() - 1);
                    }
                    
                    rVal = Long.parseLong(cVal) * factor;
                } catch (Throwable t) {
                    rVal = DEFAULT_VALUE;
                }
            }
        }
        return rVal;
    }
    
    public static final double getDoubleValue(final HashMap<String, Object> configMap, final String key, final double DEFAULT_VALUE) {
        
        double rVal = DEFAULT_VALUE;
        Object obj = configMap.get(key);
        if (obj == null)
            rVal = DEFAULT_VALUE;
        else {
            String cVal = obj.toString();
            if (cVal.length() == 0) {
                rVal = DEFAULT_VALUE;
            } else {
                try {
                    
                    double factor = 1;
                    if (cVal.endsWith("K") || cVal.endsWith("k")) {
                        factor = Utils.KILO;
                        cVal = cVal.substring(0, cVal.length() - 1);
                    } else if (cVal.endsWith("M") || cVal.endsWith("m")) {
                        factor = Utils.MEGA;
                        cVal = cVal.substring(0, cVal.length() - 1);
                    } else if (cVal.endsWith("G") || cVal.endsWith("g")) {
                        factor = Utils.GIGA;
                        cVal = cVal.substring(0, cVal.length() - 1);
                    }
                    
                    rVal = Double.parseDouble(cVal) * (double)factor;
                } catch (Throwable t) {
                    rVal = DEFAULT_VALUE;
                }
            }
        }
        return rVal;
    }
    
    public static final int getIntValue(final HashMap<String, Object> configMap, final String key, final int DEFAULT_VALUE) {
        
        int rVal = DEFAULT_VALUE;
        Object obj = configMap.get(key);
        if (obj == null)
            rVal = DEFAULT_VALUE;
        else {
            String cVal = obj.toString();
            if (cVal.length() == 0) {
                rVal = DEFAULT_VALUE;
            } else {
                try {
                    
                    long factor = 1;
                    if (cVal.endsWith("K") || cVal.endsWith("k")) {
                        factor = Utils.KILO;
                        cVal = cVal.substring(0, cVal.length() - 1);
                    } else if (cVal.endsWith("M") || cVal.endsWith("m")) {
                        factor = Utils.MEGA;
                        cVal = cVal.substring(0, cVal.length() - 1);
                    } else if (cVal.endsWith("G") || cVal.endsWith("g")) {
                        factor = Utils.GIGA;
                        cVal = cVal.substring(0, cVal.length() - 1);
                    }
                    
                    rVal = Integer.parseInt(cVal) * (int) factor;
                } catch (Throwable t) {
                    rVal = DEFAULT_VALUE;
                }
            }
        }
        return rVal;
    }
    
    public static final Config getInstance() {
        if (!initialized) {
            synchronized (Config.class) {
                while (!initialized) {
                    try {
                        Config.class.wait();
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                }
            }
        }
        
        return _thisInstance;
    }
    
    public int getRetryIOCount() {
        return IORetryFactor;
    }
    
    public static final void initInstance(final HashMap<String, Object> configMap) throws Exception {
        
        if (!initialized) {
            synchronized (Config.class) {
                if (!initialized) {
                    _thisInstance = new Config(configMap);
                    initialized = true;
                    Config.class.notifyAll();
                }
            }
        }
    }
    
    public static final void initInstance(final FDTCommandLine fdtCommandLine) throws Exception {
        
        if (!initialized) {
            synchronized (Config.class) {
                if (!initialized) {
                    _thisInstance = new Config(fdtCommandLine);
                    initialized = true;
                    Config.class.notifyAll();
                }
            }
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
    
    
    public long getRateLimit() {
        return rateLimit;
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
    
    public void setHostName(String hostname){
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
    
    public boolean isStandAlone() {
        return isStandAlone;
    }
    
    public String getWriteMode() {
        return writeMode;
    }
    
    public String[] getFileList() {
        return fileList;
    }
    
    public String getPreFilters() {
        return preFilters;
    }

    public String getPostFilters() {
        return postFilters;
    }
    
    public void  setDestinationDir(String destDir) {
        this.destDir= destDir;
    }
    
    public String getDestinationDir() {
        return destDir;
    }
    
    
    public int getNumberOfSelectors() {
        return selectorsNo;
    }
    
    public boolean withApMon() {
        
        return false;
    }
    
    public void setPullMode(){
        this.isPullMode=true;
        this.configMap.put("-pull", true);
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
    
    
    
    public static final int SSH_NO_REMOTE = -1;
    
    public static final int SSH_REMOTE_SERVER_LOCAL_CLIENT_PUSH = 1;
    
    public static final int SSH_REMOTE_SERVER_LOCAL_CLIENT_PULL = 2;
    
    public static final int SSH_REMOTE_SERVER_REMOTE_CLIENT_PUSH = 3;
    
    public int getSSHConfig() {
        
        if (!bSSHMode)
            return SSH_NO_REMOTE;
        final String sDestinationHost = getStringValue(configMap, "destinationHost", null);
        if ((aSourceHosts == null || aSourceHosts.length == 0 || aSourceHosts[0]==null) && sDestinationHost != null)
            
            
            return SSH_REMOTE_SERVER_LOCAL_CLIENT_PUSH;
        
        if (aSourceHosts != null && aSourceHosts.length > 0 && sDestinationHost == null) {
            
            return SSH_REMOTE_SERVER_LOCAL_CLIENT_PULL;
        }
        
        
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

    public MassStorage storageParams() {
        return this.storageParams;
    }

}
