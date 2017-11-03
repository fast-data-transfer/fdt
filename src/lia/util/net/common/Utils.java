package lia.util.net.common;

import apmon.ApMon;
import lia.gsi.FDTGSIServer;
import lia.util.net.copy.FDT;
import lia.util.net.copy.FDTServer;
import lia.util.net.copy.FDTSessionManager;
import lia.util.net.copy.FileBlock;
import lia.util.net.copy.transport.ControlChannel;
import lia.util.net.copy.transport.CtrlMsg;
import lia.util.net.copy.transport.internal.FDTSelectionKey;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.opentsdb.client.ExpectResponse;
import org.opentsdb.client.HttpClient;
import org.opentsdb.client.HttpClientImpl;
import org.opentsdb.client.builder.Metric;
import org.opentsdb.client.builder.MetricBuilder;
import org.opentsdb.client.response.Response;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * Various utilities functions used in the entire application
 *
 * @author ramiro
 */
public final class Utils {

    public static final char ZERO = '0';
    public static final int VALUE_2_STRING_NO_UNIT = 1;
    public static final int VALUE_2_STRING_UNIT = 2;
    public static final int VALUE_2_STRING_SHORT_UNIT = 3;
    /**
     * Logger used by this class
     */
    private static final Logger logger = Logger.getLogger(Utils.class.getName());
    private static final ScheduledThreadPoolExecutor scheduledExecutor = getSchedExecService("FDT Monitoring ThPool",
            5, Thread.MIN_PRIORITY);
    private static final int AV_PROCS;
    private static final long KILO_BIT = 1000;
    public static final long MEGA_BIT = KILO_BIT * 1000;
    private static final long GIGA_BIT = MEGA_BIT * 1000;
    private static final long TERA_BIT = GIGA_BIT * 1000;
    private static final long PETA_BIT = TERA_BIT * 1000;
    private static final long KILO_BYTE = 1024;
    public static final long MEGA_BYTE = KILO_BYTE * 1024;
    private static final long GIGA_BYTE = MEGA_BYTE * 1024;
    private static final long TERA_BYTE = GIGA_BYTE * 1024;
    private static final long PETA_BYTE = TERA_BYTE * 1024;
    private static final long[] BYTE_MULTIPLIERS = new long[]{KILO_BYTE, MEGA_BYTE, GIGA_BYTE, TERA_BYTE, PETA_BYTE};
    private static final String[] BYTE_SUFIXES = new String[]{"KB", "MB", "GB", "TB", "PB"};
    private static final long[] BIT_MULTIPLIERS = new long[]{KILO_BIT, MEGA_BIT, GIGA_BIT, TERA_BIT, PETA_BIT};
    private static final String[] BIT_SUFIXES = new String[]{"Kb", "Mb", "Gb", "Tb", "Pb"};
    private static final int URL_CONNECTION_TIMEOUT = 20 * 1000;
    private static final Object lock = new Object();
    private static final long SECONDS_IN_MINUTE = TimeUnit.MINUTES.toSeconds(1);
    private static final long SECONDS_IN_HOUR = TimeUnit.HOURS.toSeconds(1);
    private static final long SECONDS_IN_DAY = TimeUnit.DAYS.toSeconds(1);
    private static final String[] SELECTION_KEY_OPS_NAMES = {"OP_ACCEPT", "OP_CONNECT", "OP_READ", "OP_WRITE"};
    private static final int[] SELECTION_KEY_OPS_VALUES = {SelectionKey.OP_ACCEPT, SelectionKey.OP_CONNECT,
            SelectionKey.OP_READ, SelectionKey.OP_WRITE};
    /**
     * reference to the monitor reporting api, initialized in the constructor of {@link FDT}
     */
    private static ApMon apmon = null;
    private static boolean apmonInitied = false;

    //
    // END this should not be here any more after FDT will use only Java6
    // /

    static {

        int avProcs = Runtime.getRuntime().availableProcessors();

        if (avProcs <= 0) {// smth is wrong with the JVM or the OS

            avProcs = 1;
        }

        AV_PROCS = avProcs;

    }

    public static String getStackTrace(Throwable t) {
        if (t == null) {
            return "Stack trace unavailable";
        }
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    public static ScheduledThreadPoolExecutor getSchedExecService(final String name, final int corePoolSize,
                                                                  final int threadPriority) {
        return new ScheduledThreadPoolExecutor(corePoolSize, new ThreadFactory() {

            AtomicLong l = new AtomicLong(0);

            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, name + " - WorkerTask " + l.getAndIncrement());
                t.setPriority(threadPriority);
                t.setDaemon(true);
                return t;
            }
        }, new RejectedExecutionHandler() {

            public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                try {
                    if (executor.isShutdown() || executor.isTerminated() || executor.isTerminating()) {
                        return;
                    }
                    // slow down a little bit
                    final long SLEEP_TIME = Math.round((Math.random() * 1000D) + 1);
                    try {
                        Thread.sleep(SLEEP_TIME);
                    } catch (Throwable ignore) {
                        if (logger.isLoggable(Level.FINER)) {
                            ignore.printStackTrace();
                        }
                    }
                    System.err.println("\n\n [ RejectedExecutionHandler ] for " + name + " WorkerTask slept for "
                            + SLEEP_TIME);
                    executor.execute(r);
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        });
    }

    public static ExecutorService getStandardExecService(final String name, final int corePoolSize,
                                                         final int maxPoolSize, BlockingQueue<Runnable> taskQueue, final int threadPriority) {
        ThreadPoolExecutor texecutor = new ThreadPoolExecutor(corePoolSize, maxPoolSize, 2 * 60, TimeUnit.SECONDS,
                taskQueue, new ThreadFactory() {

            final AtomicLong l = new AtomicLong(0);

            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, name + " - WorkerTask " + l.getAndIncrement());
                t.setPriority(threadPriority);
                t.setDaemon(true);
                return t;
            }
        });
        texecutor.setRejectedExecutionHandler(new RejectedExecutionHandler() {

            public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                try {
                    if (executor.isShutdown() || executor.isTerminated() || executor.isTerminating()) {
                        return;
                    }
                    // slow down a little bit
                    final long SLEEP_TIME = Math.round((Math.random() * 400D) + 1);
                    try {
                        Thread.sleep(SLEEP_TIME);
                    } catch (Throwable ignore) {
                        if (logger.isLoggable(Level.FINER)) {
                            ignore.printStackTrace();
                        }
                    }
                    System.err.println("\n\n [ RejectedExecutionHandler ] [ Full Throttle ] for " + name
                            + " WorkerTask slept for " + SLEEP_TIME);
                    executor.getQueue().put(r);
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        });

        texecutor.allowCoreThreadTimeOut(true);
        texecutor.prestartAllCoreThreads();

        return texecutor;
    }

    public static ExecutorService getStandardExecService(final String name, final int corePoolSize,
                                                         final int maxPoolSize, final int threadPriority) {
        return getStandardExecService(name, corePoolSize, maxPoolSize, new SynchronousQueue<Runnable>(), threadPriority);
    }

    public static String formatWithByteFactor(final double number, final long factor, final String append) {
        String appendUM = "";
        double fNo = number;

        boolean bFound = false;
        if (factor == 0) {
            for (int i = BYTE_MULTIPLIERS.length - 1; i >= 0; i--) {
                if (number > BYTE_MULTIPLIERS[i]) {
                    fNo /= BYTE_MULTIPLIERS[i];
                    appendUM = BYTE_SUFIXES[i];
                    bFound = true;
                    break;
                }
            }
        } else {
            for (int i = BYTE_MULTIPLIERS.length - 1; i >= 0; i--) {
                if (factor == BYTE_MULTIPLIERS[i]) {
                    fNo /= BYTE_MULTIPLIERS[i];
                    appendUM = BYTE_SUFIXES[i];
                    bFound = true;
                    break;
                }
            }
        }

        if (!bFound) {
            appendUM = "B";
        }

        appendUM += append;

        return speedDecimalFormat(fNo) + ' ' + appendUM;
    }

    public static String formatWithBitFactor(final double number, final long factor, final String append) {
        String appendUM = "";
        double fNo = number;

        boolean bFound = false;
        if (factor == 0) {
            for (int i = BIT_MULTIPLIERS.length - 1; i >= 0; i--) {
                if (number > BIT_MULTIPLIERS[i]) {
                    fNo /= BIT_MULTIPLIERS[i];
                    appendUM = BIT_SUFIXES[i];
                    bFound = true;
                    break;
                }
            }

        } else {
            for (int i = BIT_MULTIPLIERS.length - 1; i >= 0; i--) {
                if (factor == BIT_MULTIPLIERS[i]) {
                    fNo /= BIT_MULTIPLIERS[i];
                    appendUM = BIT_SUFIXES[i];
                    bFound = true;
                    break;
                }
            }
        }

        if (!bFound) {
            appendUM = "b";
        }
        appendUM += append;

        return speedDecimalFormat(fNo) + ' ' + appendUM;
    }

    private static String speedDecimalFormat(final double no) {
        final DecimalFormat SPEED_DECIMAL_FORMAT = ((DecimalFormat) DecimalFormat.getNumberInstance());
        SPEED_DECIMAL_FORMAT.applyPattern("##0.000");
        return SPEED_DECIMAL_FORMAT.format(no);
    }

    public static String percentDecimalFormat(final double no) {
        final DecimalFormat PERCENT_DECIMAL_FORMAT = ((DecimalFormat) DecimalFormat.getNumberInstance());
        PERCENT_DECIMAL_FORMAT.applyPattern("00.00");
        return PERCENT_DECIMAL_FORMAT.format(no);
    }

    public static String getETA(final long seconds) {
        long delta = seconds;
        StringBuilder sb = new StringBuilder();
        final long days = seconds / SECONDS_IN_DAY;
        if (days > 0) {
            if (days < 10) {
                sb.append(ZERO);
            }
            sb.append(days).append("d ");
            delta -= days * SECONDS_IN_DAY;
        }

        final long hours = delta / SECONDS_IN_HOUR;
        if (hours > 0) {
            delta -= hours * SECONDS_IN_HOUR;
            if (hours < 10) {
                sb.append(ZERO);
            }
            sb.append(hours).append("h ");
        }

        if (days > 0) {
            return sb.toString();
        }

        final long minutes = delta / SECONDS_IN_MINUTE;
        if (minutes > 0) {
            if (minutes < 10) {
                sb.append(ZERO);
            }
            sb.append(minutes).append("m ");
            delta -= minutes * SECONDS_IN_MINUTE;
        }

        if (hours > 0) {
            return sb.toString();
        }

        if (delta < 10) {
            sb.append(ZERO);
        }
        sb.append(delta).append('s');

        return sb.toString();
    }

    /**
     * Works with both Java5 and Java6 ....... Uncooment the code and remove ............
     *
     * @param value
     * @param unit
     * @return the ETA as String representation
     * @since Java 1.6
     */
    public static String getETA(final long value, TimeUnit unit) {
        long delta = value;

        StringBuilder sb = new StringBuilder();

        // TimeUnit.DAYS N/A in 1.5
        final long days = TimeUnit.DAYS.convert(delta, unit);
        if (days > 0) {
            if (days < 10) {
                sb.append(ZERO);
            }
            sb.append(days).append("d ");
            delta -= unit.convert(days, TimeUnit.DAYS);
        }

        long hours = TimeUnit.HOURS.convert(delta, unit);
        if (hours > 0) {
            delta -= unit.convert(hours, TimeUnit.HOURS);
            if (hours < 10) {
                sb.append(ZERO);
            }
            sb.append(hours).append("h ");
        }

        if (days > 0) {
            return sb.toString();
        }

        final long minutes = TimeUnit.MINUTES.convert(delta, unit);
        if (minutes > 0) {
            if (minutes < 10) {
                sb.append(ZERO);
            }
            sb.append(minutes).append("m ");
            delta -= unit.convert(minutes, TimeUnit.MINUTES);
        }

        if (hours > 0) {
            return sb.toString();
        }

        if (delta < 10) {
            sb.append(ZERO);
        }
        sb.append(delta).append('s');

        return sb.toString();
    }

    public static ScheduledThreadPoolExecutor getMonitoringExecService() {
        return scheduledExecutor;
    }

    public static DirectByteBufferPool getDirectBufferPool() {
        return DirectByteBufferPool.getInstance();
    }

    public static int availableProcessors() {
        return AV_PROCS;
    }

    public static HeaderBufferPool getHeaderBufferPool() {
        return HeaderBufferPool.getInstance();
    }

    public static void initApMonInstance(ApMon apmon) throws Exception {
        synchronized (Utils.class) {
            if (apmonInitied) {
                return;
            }

            Utils.apmon = apmon;
            apmonInitied = true;

            Utils.class.notifyAll();
        }
    }

    public static ApMon getApMon() {
        synchronized (Utils.class) {

            // just check that initApMonInstance will be ever called ....
            Config config = Config.getInstance();
            if (config.getApMonHosts() == null && !config.getMonitor().equals(config.OPENTSDB)) {
                return null;
            }

            while (!apmonInitied) {
                try {
                    Utils.class.wait();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }

            return apmon;
        }
    }

    public static boolean isCustomLog() {
        final String customLogProperty = System.getProperty("CustomLog");
        boolean bCustomLog = false;
        if (customLogProperty != null) {
            final String trimLower = customLogProperty.trim().toLowerCase();
            if (!trimLower.isEmpty()) {
                bCustomLog = trimLower.startsWith("t") || trimLower.startsWith("1") || trimLower.startsWith("on");
            }
        }

        return bCustomLog;
    }

    public static String buffToString(final ByteBuffer bb) {

        if (bb == null) {
            return "null";
        }

        StringBuilder sb = new StringBuilder(512);
        sb.append(bb).append(" id=").append(System.identityHashCode(bb)).append(' ');

        return sb.toString();
    }

    // TODO - should not parse here -c -d -bullshit
    public static Map<String, Object> parseArguments(final String args[], final String[] singleArgs) {

        List<String> sArgs = Arrays.asList(singleArgs);

        final Map<String, Object> rHM = new HashMap<String, Object>();
        if ((args == null) || (args.length == 0)) {
            return rHM;
        }

        List<String> lParams = new ArrayList<String>();

        ArrayList<String> sshUsers = new ArrayList<String>();
        ArrayList<String> sshHosts = new ArrayList<String>();
        ArrayList<String> sshFiles = new ArrayList<String>();

        int i = 0;
        for (i = 0; i < args.length; i++) {
            if (args[i].startsWith("-")) {
                if ((i == (args.length - 1)) || args[i + 1].startsWith("-") || sArgs.contains(args[i])) {
                    rHM.put(args[i], "");
                } else {
                    if ((sshUsers.size() > 0) && (args[i].equals("-c") || args[i].equals("-d"))) {
                        throw new IllegalArgumentException(
                                "Illegal syntax! You can use either either Client/Server (-c/-d) syntax, either SCP syntax");
                    }

                    rHM.put(args[i], args[i + 1]);
                    i++;
                }
            } else if (args[i].contains(":")) {
                int idx = args[i].indexOf(':');

                // /////////////
                // handle windows stupid FS naming
                // ////////////
                if (File.separatorChar == '\\') {// "Windowns" baby!

                    if ((idx + 1) == args[i].length()) {
                        // ////////////
                        // tricky scp-like command from windows
                        //
                        // java -jar fdt.jar C:\x n:
                        //
                        // where n may be a remote machine, and C:\x a file
                        //
                        // check that we have a single letter before ':'
                        // ///////////////

                        if ((idx - 1) == 0) {

                            // test if it is a File
                            if (new File(args[i].charAt(0) + ":").exists()) {
                                // stupid driver letter; got you
                                if (sshUsers.size() > 0) {
                                    // I am the destination directory
                                    rHM.put("destinationDir", args[i]);
                                    rHM.put("-d", rHM.get("destinationDir"));
                                    break;
                                }

                                lParams.add(args[i]);
                                continue;
                            }
                        }
                    }

                    if (((idx + 1) < args[i].length()) && (args[i].charAt(idx + 1) == File.separatorChar)) {
                        if (sshUsers.size() > 0) {
                            // I am the destination directory
                            rHM.put("destinationDir", args[i]);
                            rHM.put("-d", rHM.get("destinationDir"));
                            break;
                        }

                        lParams.add(args[i]);
                        continue;
                    }
                }

                // /////////////
                // END handle windows stupid FS naming
                // ////////////

                // SSH mode

                // System.out.print(" SCP Match: " + args[i]);
                if ((sshUsers.size() == 0) && ((rHM.get("-d") != null) || (rHM.get("-c") != null))) {
                    throw new IllegalArgumentException(
                            "Illegal syntax! You can use either Client/Server (-c/-d) syntax, either SCP syntax");
                }

                String userHost = null;
                if (idx > 0) {
                    userHost = args[i].substring(0, idx);
                }

                String user = null;
                String host = null;
                String path = null;
                if (userHost != null) {
                    int idx1 = userHost.indexOf("@");
                    if (idx1 >= 0) { // at least user

                        if (idx1 == 0) {
                            throw new IllegalArgumentException("Invalid scp syntax for " + args[i]);
                        }

                        user = userHost.substring(0, idx1);
                        if ((idx1 + 1) < userHost.length()) {
                            host = userHost.substring(idx1 + 1);
                        } else {
                            throw new IllegalArgumentException("Invalid scp syntax for " + args[i]);
                        }
                    } else {
                        host = userHost;
                    }
                }

                if ((idx + 1) == args[i].length()) {
                    path = ".";
                } else {
                    path = args[i].substring(idx + 1);
                }

                // if (alSSH == null)
                // alSSH = new ArrayList<String[]>();

                sshUsers.add(user);
                sshHosts.add(host);
                sshFiles.add(path);
            } else {
                if (sshUsers.size() > 0) {
                    // I am the destination directory
                    rHM.put("destinationDir", args[i]);
                    rHM.put("-d", rHM.get("destinationDir"));
                    break;
                }

                lParams.add(args[i]);
            }
        }// for

        int sshHostsNo = sshUsers.size();
        if (sshHostsNo > 0) {
            rHM.put("SCPSyntaxUsed", Boolean.TRUE);

            if (rHM.get("destinationDir") == null) {
                // the last one should be the destination

                rHM.put("destinationUser", sshUsers.get(sshHostsNo - 1));
                rHM.put("destinationHost", sshHosts.get(sshHostsNo - 1));
                rHM.put("destinationDir", sshFiles.get(sshHostsNo - 1));
                rHM.put("-d", rHM.get("destinationDir"));
                rHM.put("-c", rHM.get("destinationHost"));

                // remove the destination from the SSH/SCP args
                sshUsers.remove(sshHostsNo - 1);
                sshHosts.remove(sshHostsNo - 1);
                sshFiles.remove(sshHostsNo - 1);

            }

            sshHostsNo = sshUsers.size();

            if (sshHostsNo > 0) {
                // the source hosts
                final String[] sUsers = sshUsers.toArray(new String[sshHostsNo]);
                final String[] sHosts = sshHosts.toArray(new String[sshHostsNo]);
                final String[] sFiles = sshFiles.toArray(new String[sshHostsNo]);

                rHM.put("sourceUsers", sUsers);
                rHM.put("sourceHosts", sHosts);
                rHM.put("sourceFiles", sFiles);

                rHM.put("Files", rHM.get("sourceFiles"));
            }
        }

        rHM.put("LastParams", lParams);

        return rHM;
    }

    static String getStringValue(final Map<String, Object> configMap, String key, String DEFAULT_VALUE) {
        Object obj = configMap.get(key);
        if (obj == null) {
            return DEFAULT_VALUE;
        }

        return obj.toString();
    }

    public static long getLongValue(final Map<String, Object> configMap, final String key,
                                    final long DEFAULT_VALUE) {
        long rVal;
        Object obj = configMap.get(key);
        if (obj == null) {
            rVal = DEFAULT_VALUE;
        } else {
            String cVal = obj.toString();
            if (cVal.length() == 0) {
                rVal = DEFAULT_VALUE;
            } else {
                try {

                    long factor = 1;
                    if (cVal.endsWith("K") || cVal.endsWith("k")) {
                        factor = Utils.KILO_BYTE;
                        cVal = cVal.substring(0, cVal.length() - 1);
                    } else if (cVal.endsWith("M") || cVal.endsWith("m")) {
                        factor = Utils.MEGA_BYTE;
                        cVal = cVal.substring(0, cVal.length() - 1);
                    } else if (cVal.endsWith("G") || cVal.endsWith("g")) {
                        factor = Utils.GIGA_BYTE;
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

    public static double getDoubleValue(final Map<String, Object> configMap, final String key,
                                        final double DEFAULT_VALUE) {

        double rVal;
        Object obj = configMap.get(key);
        if (obj == null) {
            rVal = DEFAULT_VALUE;
        } else {
            String cVal = obj.toString();
            if (cVal.length() == 0) {
                rVal = DEFAULT_VALUE;
            } else {
                try {

                    double factor = 1;
                    if (cVal.endsWith("K") || cVal.endsWith("k")) {
                        factor = Utils.KILO_BYTE;
                        cVal = cVal.substring(0, cVal.length() - 1);
                    } else if (cVal.endsWith("M") || cVal.endsWith("m")) {
                        factor = Utils.MEGA_BYTE;
                        cVal = cVal.substring(0, cVal.length() - 1);
                    } else if (cVal.endsWith("G") || cVal.endsWith("g")) {
                        factor = Utils.GIGA_BYTE;
                        cVal = cVal.substring(0, cVal.length() - 1);
                    }

                    rVal = Double.parseDouble(cVal) * factor;
                } catch (Throwable t) {
                    rVal = DEFAULT_VALUE;
                }
            }
        }
        return rVal;
    }

    public static int getIntValue(final Map<String, Object> configMap, final String key, final int DEFAULT_VALUE) {

        int rVal;
        Object obj = configMap.get(key);
        if (obj == null) {
            rVal = DEFAULT_VALUE;
        } else {
            String cVal = obj.toString();
            if (cVal.length() == 0) {
                rVal = DEFAULT_VALUE;
            } else {
                try {

                    long factor = 1;
                    if (cVal.endsWith("K") || cVal.endsWith("k")) {
                        factor = Utils.KILO_BYTE;
                        cVal = cVal.substring(0, cVal.length() - 1);
                    } else if (cVal.endsWith("M") || cVal.endsWith("m")) {
                        factor = Utils.MEGA_BYTE;
                        cVal = cVal.substring(0, cVal.length() - 1);
                    } else if (cVal.endsWith("G") || cVal.endsWith("g")) {
                        factor = Utils.GIGA_BYTE;
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

    private static File createOrGetRWFile(final String parentDirName, final String fileName) {

        final File parentDir = new File(parentDirName);
        final File file = new File(parentDirName + File.separator + fileName);

        if (!parentDir.exists()) {
            if (parentDir.mkdirs()) {
                return null;
            }
        }

        try {
            if (!file.exists()) {
                if (file.createNewFile()) {
                    if (logger.isLoggable(Level.FINE)) {
                        logger.log(Level.FINE, "Created update conf file:" + file);
                    }
                } else {
                    if (logger.isLoggable(Level.FINE)) {
                        logger.log(Level.FINE, "Cannot create update conf file:" + file);
                    }

                    return null;
                }
            } else {
                if (logger.isLoggable(Level.FINER)) {
                    logger.log(Level.FINER, "File: " + file + "exists. ");
                }
            }

            if (file.canRead()) {
                if (logger.isLoggable(Level.FINER)) {
                    logger.log(Level.FINER, "File: " + file + " can be read.");
                }

                if (file.canWrite()) {
                    if (logger.isLoggable(Level.FINER)) {
                        logger.log(Level.FINER, "File: " + file + " can be written.");
                    }
                    return file;
                }

                if (logger.isLoggable(Level.FINER)) {
                    logger.log(Level.FINER, "File: " + file + " can not be written.");
                }
            } else {
                if (logger.isLoggable(Level.FINER)) {
                    logger.log(Level.FINER, "File: " + file + " can not be read.");
                }
            }

        } catch (Throwable t) {
            logger.log(Level.WARNING, "Cannot create " + file, t);
        }

        return null;
    }

    public static void updatePropertyAndStore(String dirName, String fileName, String property, String value) {

        synchronized (lock) {
            final File file = createOrGetRWFile(dirName, fileName);
            if (file != null) {
                FileInputStream fis = null;
                FileOutputStream fos = null;
                try {
                    final Properties props = new Properties();
                    fis = new FileInputStream(file);
                    props.load(fis);

                    props.put(property, value);

                    fos = new FileOutputStream(file);
                    props.store(fos, null);
                    fos.flush();
                } catch (Throwable t) {
                    if (logger.isLoggable(Level.FINE)) {
                        logger.log(Level.FINE, "Cannot update " + file, t);
                    }
                } finally {
                    closeIgnoringExceptions(fis);
                    closeIgnoringExceptions(fos);
                }
            }
        }

    }

    private static Properties getFDTUpdateProperties() {
        final String parentFDTConfDirName = System.getProperty("user.home") + File.separator + ".fdt";
        final String fdtUpdateConfFileName = "update.properties";
        Properties updateProperties = new Properties();
        final File confFile = createOrGetRWFile(parentFDTConfDirName, fdtUpdateConfFileName);

        if (confFile != null) {
            FileInputStream fis = null;

            try {
                fis = new FileInputStream(confFile);
                updateProperties.load(fis);
            } catch (Throwable t) {
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, "Unable to read properties file: " + confFile, t);
                }
            } finally {
                closeIgnoringExceptions(fis);
            }
        }
        return updateProperties;
    }

    private static boolean updateTotalCounter(final long total, final String property) {

        final String parentFDTConfDirName = System.getProperty("user.home") + File.separator + ".fdt";
        final String fdtUpdateConfFileName = "update.properties";
        final File confFile = createOrGetRWFile(parentFDTConfDirName, fdtUpdateConfFileName);

        if (confFile != null) {
            Properties updateProperties = new Properties();
            FileInputStream fis = null;
            FileOutputStream fos = null;

            try {
                fis = new FileInputStream(confFile);
                updateProperties.load(fis);

                closeIgnoringExceptions(fis);

                if (logger.isLoggable(Level.FINEST)) {
                    logger.log(Level.FINEST, " [ Utils ] [ updateTotalContor ] loaded properties: {0}",
                            updateProperties);
                }

                final String contorStringValue = (String) updateProperties.get(property);
                final String rstStringValue = (String) updateProperties.get(property + "_rst");

                long lastContor = 0;
                long rstContor = 0;

                if (contorStringValue != null) {
                    try {
                        lastContor = Long.parseLong(contorStringValue);
                        rstContor = Long.parseLong(rstStringValue);
                    } catch (Throwable t) {
                        if (logger.isLoggable(Level.FINE)) {
                            logger.log(Level.FINE, "Got exception parsing " + property + " param", t);
                        }
                        lastContor = 0;
                        rstContor = 0;
                    }
                }

                lastContor += total;
                if (lastContor < 0) {
                    rstContor++;
                }

                updateProperties.put(property, String.valueOf(lastContor));
                updateProperties.put(property + "_rst", String.valueOf(rstContor));

                if (logger.isLoggable(Level.FINEST)) {
                    logger.log(Level.FINEST, " [ Utils ] [ updateTotalContor ] store new properties: {0}",
                            updateProperties);
                }

                checkAndSetInstanceID(updateProperties);

                fos = new FileOutputStream(confFile);
                updateProperties.store(fos, null);
                fos.flush();

            } catch (Throwable t) {
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, "Unable to update properties file for property: " + property + " contor: "
                            + total + " file: " + confFile, t);
                }
                return false;
            } finally {
                closeIgnoringExceptions(fis);
                closeIgnoringExceptions(fos);
            }
        }

        return true;
    }

    /**
     * @param props
     * @since FDT 0.9.0 - basic instanceID per FDT instance
     */
    private static void checkAndSetInstanceID(final Properties props) {

        if (props == null) {
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, " [ Utils ] [ checkAndSetInstanceID ] Null properties ... nothing to check/set");
            }
            return;
        }

        try {
            String instID = props.getProperty("instanceID");

            if ((instID == null) || instID.trim().isEmpty()) {
                instID = UUID.randomUUID().toString();
                props.put("instanceID", instID);

                final String parentFDTConfDirName = System.getProperty("user.home") + File.separator + ".fdt";
                final String fdtUpdateConfFileName = "update.properties";
                final File confFile = createOrGetRWFile(parentFDTConfDirName, fdtUpdateConfFileName);
                FileOutputStream fos = null;

                if (confFile != null) {
                    try {
                        fos = new FileOutputStream(confFile);
                        props.store(fos, null);
                        fos.flush();
                    } catch (Throwable ignore) {
                        if (logger.isLoggable(Level.FINEST)) {
                            ignore.printStackTrace();
                        }
                    } finally {
                        closeIgnoringExceptions(fos);
                    }
                }
            }
        } catch (Throwable ignore) {
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, " [ Utils ] [ checkAndSetInstanceID ] Unable to get/set instanceID", ignore);
            }
        }

    }

    public static boolean updateTotalReadCounter(final long totalRead) throws Exception {
        return updateTotalCounter(totalRead, "totalRead");
    }

    public static boolean updateTotalWriteCounter(final long totalWrite) throws Exception {
        return updateTotalCounter(totalWrite, "totalWrite");
    }

    /**
     * @param fileBlockQueue
     * @return - number of "recovered" FileBlock-s
     */
    public static int drainFileBlockQueue(Queue<FileBlock> fileBlockQueue) {
        final boolean isInterrupted = Thread.interrupted();
        int status = 0;
        final DirectByteBufferPool bPool = DirectByteBufferPool.getInstance();
        try {
            if (fileBlockQueue == null) {
                return status;
            }

            for (; ; ) {
                try {
                    final FileBlock fb = fileBlockQueue.poll();
                    if (fb == null) {
                        return status;
                    }
                    bPool.put(fb.buff);
                    status++;
                } catch (Throwable t) {
                    logger.log(Level.WARNING, " Got exception draining fileBlockQueue", t);
                }
            }
        } finally {
            if (isInterrupted) {
                try {
                    Thread.currentThread().interrupt();
                } catch (Throwable ignore) {
                    // not interested
                }
            }
        }
    }

    public static boolean checkForUpdate(final String currentVersion, final String updateURL, boolean noLock)
            throws Exception {
        try {
            final String parentFDTConfDirName = System.getProperty("user.home") + File.separator + ".fdt";
            final String fdtUpdateConfFileName = "update.properties";
            final File confFile = createOrGetRWFile(parentFDTConfDirName, fdtUpdateConfFileName);

            if (confFile != null) {
                long lastCheck = 0;

                Properties updateProperties = new Properties();
                FileInputStream fis = null;
                FileOutputStream fos = null;

                try {
                    fis = new FileInputStream(confFile);
                    updateProperties.load(fis);

                    final String lastCheckProp = (String) updateProperties.get("LastCheck");

                    lastCheck = 0;

                    if (lastCheckProp != null) {
                        try {
                            lastCheck = Long.parseLong(lastCheckProp);
                        } catch (Throwable t) {
                            if (logger.isLoggable(Level.FINE)) {
                                logger.log(Level.FINE, "Got exception parsing LastCheck param", t);
                            }
                            lastCheck = 0;
                        }
                    }

                } catch (Throwable t) {
                    logger.log(Level.WARNING, "Cannot load update properties file: " + confFile, t);
                } finally {
                    closeIgnoringExceptions(fis);
                }

                final long now = System.currentTimeMillis();
                boolean bHaveUpdates = false;
                checkAndSetInstanceID(updateProperties);

                if ((lastCheck + FDT.UPDATE_PERIOD) < now) {
                    try {
                        System.out
                                .println("\n\nChecking for remote updates ... This may be disabled using -noupdates flag.");
                        bHaveUpdates = updateFDT(currentVersion, updateURL, false, noLock);
                        if (bHaveUpdates) {
                            logger.info("FDT may be updated using: java -jar fdt.jar -update");
                        } else {
                            if (logger.isLoggable(Level.FINE)) {
                                logger.log(Level.FINE, "No updates available");
                            }
                        }
                    } catch (Throwable t) {
                        if (logger.isLoggable(Level.FINE)) {
                            logger.log(Level.WARNING, "Got exception", t);
                        }

                    }

                    updateProperties.put("LastCheck", String.valueOf(now));

                    try {
                        fos = new FileOutputStream(confFile);
                        updateProperties.store(fos, null);
                    } catch (Throwable t1) {
                        logger.log(Level.WARNING, "Cannot store update properties file", t1);
                    } finally {
                        closeIgnoringExceptions(fos);
                    }

                    return bHaveUpdates;
                }
            } else {
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, " [ checkForUpdate ] Cannot read or write the update conf file: "
                            + parentFDTConfDirName + File.separator + fdtUpdateConfFileName);
                }
                return false;
            }

        } catch (Throwable t) {
            logger.log(Level.WARNING, "Got exception checking for updates", t);
        }

        return false;
    }

    /**
     * @return true if update was available and FDT was successfully updated, and false if no update was available
     * @throws Exception if update was unsuccessfully or there was a problem connecting to the update server
     */
    public static boolean updateFDT(final String currentVersion, final String updateURL, boolean shouldUpdate,
                                    boolean noLock) throws Exception {

        logger.info("Checking remote fdt.jar at URL: " + updateURL);
        final Properties p = getFDTUpdateProperties();

        if (p.getProperty("totalRead") == null) {
            p.put("totalRead", "0");
        }

        if (p.getProperty("totalWrite") == null) {
            p.put("totalWrite", "0");
        }

        checkAndSetInstanceID(p);

        if (p.getProperty("totalRead_rst") != null) {
            p.remove("totalRead_rst");
        }

        if (p.getProperty("totalWrite_rst") != null) {
            p.remove("totalWrite_rst");
        }


        final String finalPath = new URI(FDT.class.getProtectionDomain().getCodeSource().getLocation().toString()).getPath();

        if ((finalPath == null) || (finalPath.length() == 0)) {
            throw new IOException("Cannot determine the path to current fdt jar");
        }

        final File currentJar = new File(finalPath);

        if (!currentJar.exists()) {
            // Smth wrong with the OS or the JVM?
            throw new IOException("Current fdt.jar path seems to be [ " + finalPath
                    + " ] but the JVM cannot access it!");
        }

        if (currentJar.isFile() && currentJar.canWrite()) {
            logger.info("\nCurrent fdt.jar path is: " + finalPath);
        } else {
            throw new IOException("Current fdt.jar path seems to be [ " + finalPath
                    + " ] but it does not have write access!");
        }

        // Check if it is possible to use a temporary file
        File tmpUpdateFile = null;
        FileOutputStream fos = null;
        JarFile jf;
        InputStream connInputStream = null;
        try {
            // first try to create the destination update file
            tmpUpdateFile = File.createTempFile("fdt_update_tmp", ".jar");
            tmpUpdateFile.deleteOnExit();
            fos = new FileOutputStream(tmpUpdateFile);
            if (updateURL.equals(FDT.UPDATE_URL)) {
                connInputStream = connectTo(updateURL);
                logger.info("OK");
                JSONObject jsonObject = getJsonInfo(connInputStream);
                String tagName = getTagName(jsonObject);
                String currentVersionString = currentVersion.substring(0, tagName.length());
                logger.info("Current version: " + currentVersionString + " Latest version: " + tagName);
                if (currentVersionString.equals(tagName)) {
                    logger.info("No need to update");
                    return false;
                }
                logAdditionalInfo(jsonObject);
                String downloadUrl = getDownloadURL(jsonObject);
                downloadFDT(fos, downloadUrl);
                // try to check the version
            } else {
                downloadFDT(fos, updateURL);
            }
            jf = new JarFile(tmpUpdateFile);
            final Manifest mf = jf.getManifest();
            final Attributes attr = mf.getMainAttributes();
            final String remoteVersion = attr.getValue("Implementation-Version");

            if (!Boolean.getBoolean("skip.version.check")) {
                if ((remoteVersion == null) || (remoteVersion.trim().length() == 0)) {
                    throw new Exception("Cannot read the version from the downloaded jar...Cannot compare versions! " +
                            "You can skip version checking by using system property: skip.version.check");
                }

                if (currentVersion.equals(remoteVersion.trim())) {
                    // no need for update
                    return false;
                }
                logger.info("Remote FDT version: " + remoteVersion + " Local FDT version: " + currentVersion
                        + ". Update available.");
            } else {
                logger.info("Skipped version checking.");
            }

            if (shouldUpdate) {
                try {

                    // basic checks for parent directory
                    final String parent = currentJar.getParent();
                    if (parent == null) {
                        throw new IOException("Unable to determine parent dir for: " + currentJar);
                    }
                    final File parentDir = new File(parent);
                    if (!parentDir.canWrite()) {
                        //
                        // windows XP (at least on the system I tested) reports totaly stupid things here; make it an
                        // warning...
                        //
                        logger.log(Level.WARNING,
                                "[ WARNING CHECK ] The OS reported that is unable to write in parent dir: " + parentDir
                                        + " continue anyway; the call might be broken.");
                    }

                    final File bkpJar = new File(parentDir.getPath() + File.separator + "fdt_"
                            + Config.FDT_FULL_VERSION + ".jar");
                    boolean bDel = bkpJar.exists();
                    if (bDel) {
                        bDel = bkpJar.delete();
                        if (!bDel) {
                            logger.info("[ WARNING ] Unable to delete backup jar with the same version: "
                                    + bkpJar + " ... will continue");
                        } else {
                            logger.info("[ INFO ] Backup jar (same version as the update) " + bkpJar
                                    + " delete it.");
                        }
                    }

                    boolean renameSucced = currentJar.renameTo(bkpJar);
                    if (!renameSucced) {
                        logger.log(Level.WARNING, "Unable to create backup: " + bkpJar
                                + " for current FDT before update.");
                    } else {
                        logger.info("Backing up old FDT succeeded: " + bkpJar);
                    }

                } catch (Throwable t) {
                    logger.log(Level.WARNING, "Unable to create a backup for current FDT before update. Exception: ", t);
                }
                copyFile2File(tmpUpdateFile, currentJar, noLock);
            }

            return true;
        } finally {
            closeIgnoringExceptions(connInputStream);
            closeIgnoringExceptions(fos);
            if (tmpUpdateFile != null) {
                try {
                    tmpUpdateFile.delete();
                } catch (Throwable ignore) {
                    // not interested
                }
            }
        }
    }

    private static void downloadFDT(FileOutputStream fos, String downloadURL) throws IOException {
        InputStream downInputStream;
        if (!downloadURL.endsWith("fdt.jar")) {
            if (!downloadURL.endsWith("/")) {
                downloadURL = downloadURL + "/fdt.jar";
            } else {
                downloadURL = downloadURL + "fdt.jar";
            }
        }
        logger.info("Trying to download update from " + downloadURL);
        downInputStream = connectTo(downloadURL);
        logger.info("OK");
        byte[] buff = new byte[8192];

        int count;
        while ((count = downInputStream.read(buff)) > 0) {
            fos.write(buff, 0, count);
        }
        fos.flush();
    }

    private static void logAdditionalInfo(JSONObject jsonObject) throws JSONException {
        logReleaseName(jsonObject);
        logPublishDate(jsonObject);
        logReleaseNotes(jsonObject);
    }

    private static void logReleaseName(JSONObject jsonObject) throws JSONException {
        String name = (String) jsonObject.get("name");
        logger.info("Name: " + name);
    }

    private static void logReleaseNotes(JSONObject jsonObject) throws JSONException {
        String body = (String) jsonObject.get("body");
        logger.info("Release notes: " + body);
    }

    private static String getDownloadURL(JSONObject jsonObject) throws JSONException {
        JSONArray assets = (JSONArray) jsonObject.get("assets");
        JSONObject asset = new JSONObject(assets.get(0).toString());
        String downloadUrl = (String) asset.get("browser_download_url");
        logger.info("FDT download url: " + downloadUrl);
        return downloadUrl;
    }

    private static String logPublishDate(JSONObject jsonObject) throws JSONException {
        String publishedAt = (String) jsonObject.get("published_at");
        logger.info("Publish date: " + publishedAt);
        return publishedAt;
    }

    private static JSONObject getJsonInfo(InputStream connInputStream) throws IOException, JSONException {
        BufferedReader streamReader = new BufferedReader(new InputStreamReader(connInputStream, "UTF-8"));
        StringBuilder responseStrBuilder = new StringBuilder(10);
        String inputStr;
        while ((inputStr = streamReader.readLine()) != null)
            responseStrBuilder.append(inputStr);

        String response = responseStrBuilder.toString();
        response = getJsonString(response);
        return new JSONObject(response);
    }

    private static String getTagName(JSONObject jsonObject) throws JSONException {
        String tag_name = (String) jsonObject.get("tag_name");
        logger.info("Latest available version: " + tag_name);
        return tag_name;
    }

    private static String getJsonString(String response) {
        if (response.startsWith("["))
            response = response.substring(1);
        if (response.endsWith("]"))
            response = response.substring(0, response.length() - 1);
        return response;
    }

    private static InputStream connectTo(String updateURL) throws IOException {

        final URLConnection urlConnection = new URL(updateURL).openConnection();
        urlConnection.setDefaultUseCaches(false);
        urlConnection.setUseCaches(false);
        urlConnection.setConnectTimeout(URL_CONNECTION_TIMEOUT);
        urlConnection.setReadTimeout(URL_CONNECTION_TIMEOUT);

        logger.info("Connecting ... ");
        urlConnection.connect();
        return urlConnection.getInputStream();
    }

    public static String getUsage() {
        final String newline = System.getProperty("line.separator");

        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(Utils.class.getResourceAsStream("usage")));
            StringBuilder sb = new StringBuilder();
            for (; ; ) {
                final String line = br.readLine();
                if (line == null) {
                    break;
                }
                sb.append(line).append(newline);
            }

            return sb.toString();
        } catch (Throwable t) {
            return "Unable to load help msg.";
        }
    }

    public static String md5ToString(byte[] md5sum) {
        StringBuilder sb = new StringBuilder();

        for (byte element : md5sum) {
            sb.append(Integer.toString((element & 0xff) + 0x100, 16).substring(1));
        }

        return sb.toString();
    }

    /**
     * Optimized file transfer method. In most moder OS-es "zero-copy" should be used by the underlying OS.
     *
     * @param s source file
     * @param d destination file
     * @throws IOException
     */
    private static void copyFile2File(File s, File d, boolean noLock) throws IOException {
        FileChannel srcChannel = null;
        FileChannel dstChannel = null;

        RandomAccessFile raf = null;
        FileOutputStream fos = null;

        try {
            // source channel
            raf = new RandomAccessFile(s, "rw");
            srcChannel = raf.getChannel();

            // Create channel on the destination
            fos = new FileOutputStream(d);
            dstChannel = fos.getChannel();

            try {
                if (!noLock) {
                    srcChannel.lock();
                } else {
                    if (logger.isLoggable(Level.FINE)) {
                        logger.log(Level.FINE, " [ Utils ] [ copyFile2File ] not taking locks for: " + s);
                    }
                }
            } catch (Throwable t) {
                logger.log(Level.WARNING, "Unable to take source file (" + s
                        + ") lock. Will continue without lock taken. Cause: ", t);
            }
            try {
                if (!noLock) {
                    dstChannel.lock();
                } else {
                    if (logger.isLoggable(Level.FINE)) {
                        logger.log(Level.FINE, " [ Utils ] [ copyFile2File ] not taking locks for: " + d);
                    }
                }
            } catch (Throwable t) {
                logger.log(Level.WARNING, "Unable to take destination file (" + d
                        + ") lock. Will continue without lock taken. Cause: ", t);
            }

            // Copy file contents from source to destination - ZERO copy on most OSes!
            final long tr = dstChannel.transferFrom(srcChannel, 0, srcChannel.size());

            long ss = srcChannel.size();
            long ds = dstChannel.size();

            // dummy check - don't know what happens if disk is full
            if ((ss != ds) || (ss != tr)) {
                throw new IOException("Different size for sourceFile [ " + s + " ] DestinationFileSize [ " + d
                        + " ] Transferred [ " + tr + " ] ");
            }
        } finally {
            closeIgnoringExceptions(srcChannel);
            closeIgnoringExceptions(dstChannel);
            closeIgnoringExceptions(raf);
            closeIgnoringExceptions(fos);
        }
    }

    /**
     * fills an array of File objects based on a list of files and directories
     */
    public static void getRecursiveFiles(String fileName, String remappedFileName, List<String> allFiles,
                                         List<String> allRemappedFiles) throws Exception {

        if (allFiles == null) {
            throw new NullPointerException("File list is null");
        }
        File file = new File(fileName);
        if (file.exists() && file.canRead()) {
            if (file.isFile()) {
                allFiles.add(fileName);
                allRemappedFiles.add(remappedFileName);
            } else if (file.isDirectory()) {
                String[] listContents = file.list();
                if ((listContents != null) && (listContents.length > 0)) {
                    for (String subFile : listContents) {
                        if (remappedFileName != null) {
                            getRecursiveFiles(fileName + File.separator + subFile, remappedFileName + File.separator
                                    + subFile, allFiles, allRemappedFiles);
                        } else {
                            getRecursiveFiles(fileName + File.separator + subFile, null, allFiles, allRemappedFiles);
                        }
                    }
                }
            } else {// any other special device: e.g. /dev/zero, /dev/null :)
                allFiles.add(fileName);
                allRemappedFiles.add(remappedFileName);
            }
        }
    }

    /**
     * Helper method to close a {@link FDTCloseable} ignoring eventual exceptions
     *
     * @param closeable to be closed
     */
    public static void closeIgnoringExceptions(FDTCloseable closeable, String downMessage, Throwable downCause) {
        if (closeable != null) {
            try {
                closeable.close(downMessage, downCause);
            } catch (Throwable ign) {
                if (logger.isLoggable(Level.FINER)) {
                    logger.log(Level.FINER, "Exceptions closing FDTCloseable '" + closeable + "'. Cause: ", ign);
                }
            }
        }
    }

    /**
     * Helper method to close a {@link Closeable} ignoring eventual exceptions
     *
     * @param closeable to be closed
     */
    public static void closeIgnoringExceptions(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Throwable ign) {
                if (logger.isLoggable(Level.FINER)) {
                    logger.log(Level.FINER, "Exceptions closing Closeable '" + closeable + "'. Cause: ", ign);
                }
            }
        }
    }

    /**
     * Helper method to close a {@link Selector} ignoring eventual exceptions
     *
     * @param selector to be closed
     */
    public static void closeIgnoringExceptions(Selector selector) {
        if (selector != null) {
            try {
                selector.close();
            } catch (Throwable ign) {
                if (logger.isLoggable(Level.FINER)) {
                    logger.log(Level.FINER, "Exceptions closing Selector '" + selector + "'. Cause: ", ign);
                }
            }
        }
    }

    /**
     * Helper method to close a {@link Socket} ignoring eventual exceptions
     *
     * @param socket to be closed
     */
    public static void closeIgnoringExceptions(Socket socket) {
        if (socket != null) {
            try {
                socket.close();
            } catch (Throwable ign) {
                if (logger.isLoggable(Level.FINER)) {
                    logger.log(Level.FINER, "Exceptions closing Socket '" + socket + "'. Cause: ", ign);
                }
            }
        }
    }

    @SuppressWarnings("SameParameterValue")
    public static void cancelFutureIgnoringException(Future<?> f, boolean mayInterruptIfRunning) {
        if (f != null) {
            try {
                f.cancel(mayInterruptIfRunning);
            } catch (Throwable ign) {
                if (logger.isLoggable(Level.FINER)) {
                    logger.log(Level.FINER, "Exceptions canceling Future '" + f + "'. Cause: ", ign);
                }
            }
        }
    }

    /**
     * Helper method to close a {@link ServerSocket} ignoring eventual exceptions
     *
     * @param serverSocket to be closed
     */
    public static void closeIgnoringExceptions(ServerSocket serverSocket) {
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (Throwable ign) {
                if (logger.isLoggable(Level.FINER)) {
                    logger.log(Level.FINER, "Exceptions closing ServerSocket '" + serverSocket + "'. Cause: ", ign);
                }
            }
        }
    }

    public static String toStringSelectionKey(final FDTSelectionKey fsk) {
        if (fsk == null) {
            return " Null FDTSelectionKey ! ";
        }
        final StringBuilder sb = new StringBuilder("Socket ");

        try {
            final SocketChannel sc = fsk.channel();
            final Selector sel = fsk.selector();

            if (sc == null) {
                sb.append(" NULL! ");
            } else {
                sb.append(sc.socket());
                if (sel == null) {
                    sb.append(" NULL SELECTOR! ");
                } else {
                    final SelectionKey sk = sc.keyFor(sel);
                    if (sk == null) {
                        sb.append(" no such SelectionKey for selector! ");
                    } else {
                        sb.append(' ').append(toStringSelectionKey(sk));
                    }
                }
            }
        } catch (Throwable t) {
            sb.append(" [ toStringSelectionKey ] Exception ").append(t);
        }
        return sb.toString();
    }

    private static String toStringSelectionKey(final SelectionKey sk) {
        final StringBuilder sb = new StringBuilder("SelectionKey [ ");
        if (sk.isValid()) {
            sb.append("INVALID");
        } else {
            sb.append("VALID");
            sb.append(" :- interestOps ").append(toStringSelectionKeyOps(sk.interestOps()));
            sb.append(" :- readyOps").append(toStringSelectionKeyOps(sk.readyOps()));
        }
        sb.append(" ]");
        return sb.toString();
    }

    /**
     * @param versionString1
     * @param versionString2
     * @return 0 if equals or both null, 1 if first is first version is greater, -1 otherwise
     * @throws NullPointerException if one of the two params is null (XOR test).
     * @see FDTVersion#compareTo(FDTVersion)
     */
    @SuppressWarnings("SameParameterValue")
    public static int compareVersions(final String versionString1, final String versionString2) {
        if ((versionString1 == null) && (versionString2 == null)) {
            return 0;
        }

        if ((versionString1 == null) || (versionString2 == null)) {
            if (versionString1 == null) {
                throw new NullPointerException("versionString1 is null");
            }

            //noinspection ConstantConditions
            if (versionString2 == null) {
                throw new NullPointerException("versionString2 is null");
            }
        }

        return FDTVersion.fromVersionString(versionString1).compareTo(FDTVersion.fromVersionString(versionString2));
    }

    private static String toStringSelectionKeyOps(final int keyOps) {
        final StringBuilder sb = new StringBuilder("{");

        boolean bAdded = false;
        for (int i = 0; i < SELECTION_KEY_OPS_VALUES.length; i++) {
            if ((keyOps & SELECTION_KEY_OPS_VALUES[i]) == SELECTION_KEY_OPS_VALUES[i]) {
                if (bAdded) {
                    sb.append('|');
                } else {
                    bAdded = true;
                }

                sb.append(' ').append(SELECTION_KEY_OPS_NAMES[i]).append(' ');
            }
        }

        sb.append('}');
        return sb.toString();
    }

    static String joinString(CharSequence delimiter, CharSequence... elements) {
        StringBuilder sb = new StringBuilder();

        if (elements.length > 0) {
            sb.append(elements[0]);
        }
        for (int i = 1; i < elements.length; i++) {
            sb.append(delimiter).append(elements[i]);
        }

        return sb.toString();
    }

    static InetAddress getLoopbackAddress() {
        InetAddress localhost = null;

        try {
            byte[] address = {127, 0, 0, 1};  // 127.0.0.1
            localhost = InetAddress.getByAddress("localhost", address);
        } catch (UnknownHostException ex) {
            // do nothing
        }

        return localhost;
    }

    public static ArrayBlockingQueue<Integer> getTransportPortsValue(Map<String, Object> configMap, String key, int defaultPortNo) {
        ArrayBlockingQueue<Integer> transportPorts = new ArrayBlockingQueue<>(10);
        int i = 0;
        Object obj = configMap.get(key);
        if (obj == null || obj.toString().isEmpty()) {
            transportPorts.add(defaultPortNo);
            return transportPorts;
        }
        String tp = obj.toString();
        StringTokenizer stk = new StringTokenizer(tp, ",");
        String s[] = new String[10];
        while (stk.hasMoreTokens()) {
            s[i] = stk.nextToken();
            transportPorts.add(Integer.parseInt(s[i]));
            i++;
        }
        return transportPorts;
    }

    private static void initLocalProps(String level, Properties localProps) {

        FileInputStream fis = null;
        File confFile = null;
        try {
            confFile = new File(
                    System.getProperty("user.home") + File.separator + ".fdt" + File.separator + "fdt.properties");
            if (level.contains("FINE")) {
                logger.info("Using local properties file: " + confFile);
            }
            if (confFile.exists() && confFile.canRead()) {
                fis = new FileInputStream(confFile);
                localProps.load(fis);
            }
        } catch (Throwable t) {
            if (confFile != null) {
                if (level.contains("FINE")) {
                    System.err.println("Unable to read local configuration file " + confFile);
                    t.printStackTrace();
                }
            }
        } finally {
            Utils.closeIgnoringExceptions(fis);
        }

        if (level.contains("FINE")) {
            if (localProps.size() > 0) {
                if (level.contains("FINER")) {
                    logger.info(" LocalProperties loaded: " + localProps);
                }
            } else {
                logger.info("No local properties defined");
            }
        }
    }

    public static void initLogger(String level, File logFile, Properties localProps) {
        initLocalProps(level, localProps);
        Properties loggingProps = new Properties();
        loggingProps.putAll(localProps);

        try {
            if (!loggingProps.containsKey("handlers")) {
                loggingProps.put("handlers", "java.util.logging.ConsoleHandler");
                loggingProps.put("java.util.logging.ConsoleHandler.level", "FINEST");
                loggingProps.put("java.util.logging.ConsoleHandler.formatter", "java.util.logging.SimpleFormatter");
            }

            if (logFile != null) {
                if (loggingProps.contains("handlers")) {
                    loggingProps.remove("handlers");
                }

                loggingProps.put("handlers", "java.util.logging.FileHandler,java.util.logging.ConsoleHandler");
                loggingProps.put("java.util.logging.ConsoleHandler.level", "FINEST");
                loggingProps.put("java.util.logging.ConsoleHandler.formatter", "java.util.logging.SimpleFormatter");
                loggingProps.put("java.util.logging.FileHandler.level", "FINEST");
                loggingProps.put("java.util.logging.FileHandler.formatter", "java.util.logging.SimpleFormatter");
                loggingProps.put("java.util.logging.FileHandler.pattern", "" + logFile);
                loggingProps.put("java.util.logging.FileHandler.append", "true");

                System.setProperty("CustomLog", "true");
            }

            if (!loggingProps.containsKey(".level")) {
                loggingProps.put(".level", level);
            }

            if (level.contains("FINER")) {
                logger.info("\n Logging props: " + loggingProps);
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

    public static void waitAndWork(ExecutorService executor, ServerSocket ss, Selector sel, Config config) throws Exception {
        if (config.isGSIModeEnabled()) {
            FDTGSIServer gsiServer = new FDTGSIServer(config.getGSIPort());
            gsiServer.start();
            logger.log(Level.INFO, "FDT started in GSI mode on port: " + config.getGSIPort());
        }
        waitForTask(executor, ss, sel);

        int transferPort = getFDTTransferPort(config);
        if (transferPort > 0) {
            final FDTServer theServer = new FDTServer(transferPort);
            theServer.doWork();
        } else {
            logger.warning("There are no free transfer ports at this moment, please try again later");
            waitForTask(executor, ss, sel);
        }
    }

    public static int getFDTTransferPort(Config config) throws Exception {
        ControlChannel cc = new ControlChannel(config.getHostName(), config.getPort(), UUID.randomUUID(), FDTSessionManager.getInstance());
        int transferPort = cc.sendTransferPortMessage(new CtrlMsg(CtrlMsg.REMOTE_TRANSFER_PORT, "rtp"));
        // wait for remote config
        logger.log(Level.INFO, "Got transfer port: " + config.getHostName() + ":" + transferPort);
        return transferPort;
    }

    private static void waitForTask(ExecutorService executor, ServerSocket ss, Selector sel) throws Exception {
        try {

            for (; ; ) {
                final int count = sel.select(2000);

                if (count == 0)
                    continue;

                Iterator<SelectionKey> it = sel.selectedKeys().iterator();
                while (it.hasNext()) {
                    final SelectionKey sk = it.next();
                    it.remove();

                    if (!sk.isValid())
                        continue;// closed socket ?

                    if (sk.isAcceptable()) {
                        final ServerSocketChannel ssc = (ServerSocketChannel) sk.channel();
                        final SocketChannel sc = ssc.accept();

                        try {
                            executor.execute(new AcceptableTask(sc));
                        } catch (Throwable t) {
                            StringBuilder sb = new StringBuilder();
                            sb.append("[ FDT ] [ waitForTask ] got exception in while sumbiting the AcceptableTask for SocketChannel: ").append(sc);
                            if (sc != null) {
                                sb.append(" Socket: ").append(sc.socket());
                            }
                            sb.append(" Cause: ");
                            logger.log(Level.WARNING, sb.toString(), t);
                        }
                    }
                }
            }
        } catch (Throwable t) {
            logger.log(Level.WARNING, "[FDT] [ waitForTask ] Exception in main loop!", t);
            throw new Exception(t);
        }

    }

    public static boolean isTransferPort(int localPort) {
        return Config.getInstance().getRemoteTransferPorts().contains(localPort);
    }
}
