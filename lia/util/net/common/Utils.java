
package lia.util.net.common;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.FileChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;

import lia.util.net.copy.FDT;
import lia.util.net.copy.transport.internal.FDTSelectionKey;
import apmon.ApMon;
import java.util.UUID;


public final class Utils {

    
    private static final Logger logger = Logger.getLogger(Utils.class.getName());
    private static final ScheduledThreadPoolExecutor scheduledExecutor = getSchedExecService("FDT Monitoring ThPool",
            5,
            Thread.MIN_PRIORITY);
    
    private static ApMon apmon = null;
    private static boolean apmonInitied = false;
    public static final int VALUE_2_STRING_NO_UNIT = 1;
    public static final int VALUE_2_STRING_UNIT = 2;
    public static final int VALUE_2_STRING_SHORT_UNIT = 3;
    private static final int AV_PROCS;
    public static final long KILO_BIT = 1000;
    public static final long MEGA_BIT = KILO_BIT * 1000;
    public static final long GIGA_BIT = MEGA_BIT * 1000;
    public static final long TERA_BIT = GIGA_BIT * 1000;
    public static final long PETA_BIT = TERA_BIT * 1000;
    public static final long KILO_BYTE = 1024;
    public static final long MEGA_BYTE = KILO_BYTE * 1024;
    public static final long GIGA_BYTE = MEGA_BYTE * 1024;
    public static final long TERA_BYTE = GIGA_BYTE * 1024;
    public static final long PETA_BYTE = TERA_BYTE * 1024;
    private static final long[] BYTE_MULTIPLIERS = new long[]{
        KILO_BYTE, MEGA_BYTE, GIGA_BYTE, TERA_BYTE, PETA_BYTE
    };
    private static final String[] BYTE_SUFIXES = new String[]{
        "KB", "MB", "GB", "TB", "PB"
    };
    private static final long[] BIT_MULTIPLIERS = new long[]{
        KILO_BIT, MEGA_BIT, GIGA_BIT, TERA_BIT, PETA_BIT
    };
    private static final String[] BIT_SUFIXES = new String[]{
        "Kb", "Mb", "Gb", "Tb", "Pb"
    };
    public static final int URL_CONNECTION_TIMEOUT = 20 * 1000;
    private static final Object lock = new Object();

    
    private static final long SECONDS_IN_MINUTE = 60;
    private static final long SECONDS_IN_HOUR = 60 * SECONDS_IN_MINUTE;
    private static final long SECONDS_IN_DAY = 24 * SECONDS_IN_HOUR;
    private static final String[] SELECTION_KEY_OPS_NAMES = {
        "OP_ACCEPT", "OP_CONNECT", "OP_READ", "OP_WRITE"
    };
    private static final int[] SELECTION_KEY_OPS_VALUES = {
        SelectionKey.OP_ACCEPT, SelectionKey.OP_CONNECT, SelectionKey.OP_READ, SelectionKey.OP_WRITE
    };

    
    
    
    

    static {

        int avProcs = Runtime.getRuntime().availableProcessors();

        if (avProcs <= 0) {

            avProcs = 1;
        }

        AV_PROCS = avProcs;

    }

    public static final String getStackTrace(Throwable t) {
        if (t == null) {
            return "Stack trace unavailable";
        }
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    public static final ScheduledThreadPoolExecutor getSchedExecService(
            final String name,
            final int corePoolSize,
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
                    
                    final long SLEEP_TIME = Math.round(Math.random() * 1000D + 1);
                    try {
                        Thread.sleep(SLEEP_TIME);
                    } catch (Throwable ignore) {
                    }
                    System.err.println("\n\n [ RejectedExecutionHandler ] for " + name + " WorkerTask slept for " + SLEEP_TIME);
                    executor.execute(r);
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        });
    }

    public static final ExecutorService getStandardExecService(
            final String name,
            final int corePoolSize,
            final int maxPoolSize,
            BlockingQueue<Runnable> taskQueue,
            final int threadPriority) {
        ThreadPoolExecutor texecutor = new ThreadPoolExecutor(corePoolSize,
                maxPoolSize,
                2 * 60,
                TimeUnit.SECONDS,
                taskQueue,
                new ThreadFactory() {

                    AtomicLong l = new AtomicLong(0);

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
                    
                    final long SLEEP_TIME = Math.round(Math.random() * 400D + 1);
                    try {
                        Thread.sleep(SLEEP_TIME);
                    } catch (Throwable ignore) {
                    }
                    System.err.println("\n\n [ RejectedExecutionHandler ] [ Full Throttle ] for " + name + " WorkerTask slept for " + SLEEP_TIME);
                    executor.getQueue().put(r);
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        });
        
        
        texecutor.prestartAllCoreThreads();

        return texecutor;
    }

    public static final ExecutorService getStandardExecService(
            final String name,
            final int corePoolSize,
            final int maxPoolSize,
            final int threadPriority) {
        return getStandardExecService(name, corePoolSize, maxPoolSize, new SynchronousQueue<Runnable>(), threadPriority);
    }

    public static final String formatWithByteFactor(final double number, final long factor, final String append) {
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

        return speedDecimalFormat(fNo) + " " + appendUM;
    }

    public static final String formatWithBitFactor(final double number, final long factor, final String append) {
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

        return speedDecimalFormat(fNo) + " " + appendUM;
    }

    public static final String speedDecimalFormat(final double no) {
        final DecimalFormat SPEED_DECIMAL_FORMAT = ((DecimalFormat) DecimalFormat.getNumberInstance());
        SPEED_DECIMAL_FORMAT.applyPattern("##0.000");
        return SPEED_DECIMAL_FORMAT.format(no);
    }

    public static final String percentDecimalFormat(final double no) {
        final DecimalFormat PERCENT_DECIMAL_FORMAT = ((DecimalFormat) DecimalFormat.getNumberInstance());
        PERCENT_DECIMAL_FORMAT.applyPattern("00.00");
        return PERCENT_DECIMAL_FORMAT.format(no);
    }

    public final static String getETA(final long seconds) {
        long delta = seconds;
        StringBuilder sb = new StringBuilder();
        final long days = seconds / SECONDS_IN_DAY;
        if (days > 0) {
            if (days < 10) {
                sb.append("0");
            }
            sb.append(days).append("d ");
            delta -= days * SECONDS_IN_DAY;
        }

        final long hours = delta / SECONDS_IN_HOUR;
        if (hours > 0) {
            delta -= hours * SECONDS_IN_HOUR;
            if (hours < 10) {
                sb.append("0");
            }
            sb.append(hours).append("h ");
        }

        if (days > 0) {
            return sb.toString();
        }

        final long minutes = delta / SECONDS_IN_MINUTE;
        if (minutes > 0) {
            if (minutes < 10) {
                sb.append("0");
            }
            sb.append(minutes).append("m ");
            delta -= minutes * SECONDS_IN_MINUTE;
        }

        if (hours > 0) {
            return sb.toString();
        }

        if (delta < 10) {
            sb.append("0");
        }
        sb.append(delta).append("s");

        return sb.toString();
    }

    
    public final static String getETA(final long value, TimeUnit unit) {
        long delta = value;

        StringBuilder sb = new StringBuilder();

        
        final long days = TimeUnit.DAYS.convert(delta, unit);
        if (days > 0) {
            if (days < 10) {
                sb.append("0");
            }
            sb.append(days).append("d ");
            delta -= unit.convert(days, TimeUnit.DAYS);
        }

        long hours = TimeUnit.HOURS.convert(delta, unit);
        if (hours > 0) {
            delta -= unit.convert(hours, TimeUnit.HOURS);
            if (hours < 10) {
                sb.append("0");
            }
            sb.append(hours).append("h ");
        }

        if (days > 0) {
            return sb.toString();
        }

        final long minutes = TimeUnit.MINUTES.convert(delta, unit);
        if (minutes > 0) {
            if (minutes < 10) {
                sb.append("0");
            }
            sb.append(minutes).append("m ");
            delta -= unit.convert(minutes, TimeUnit.MINUTES);
        }

        if (hours > 0) {
            return sb.toString();
        }

        if (delta < 10) {
            sb.append("0");
        }
        sb.append(delta).append("s");

        return sb.toString();
    }

    public static final ScheduledThreadPoolExecutor getMonitoringExecService() {
        return scheduledExecutor;
    }

    public static final DirectByteBufferPool getDirectBufferPool() {
        return DirectByteBufferPool.getInstance();
    }

    public static final int availableProcessors() {
        return AV_PROCS;
    }

    public static final HeaderBufferPool getHeaderBufferPool() {
        return HeaderBufferPool.getInstance();
    }

    public static final void initApMonInstance(ApMon apmon) throws Exception {
        synchronized (Utils.class) {
            if (apmonInitied) {
                return;
            }

            Utils.apmon = apmon;
            apmonInitied = true;

            Utils.class.notifyAll();
        }
    }

    public static final ApMon getApMon() {
        synchronized (Utils.class) {

            
            if (Config.getInstance().getApMonHosts() == null) {
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

    
    public static final Map<String, Object> parseArguments(final String args[], final String[] singleArgs) {

        List<String> sArgs = Arrays.asList(singleArgs);

        final Map<String, Object> rHM = new HashMap<String, Object>();
        if (args == null || args.length == 0) {
            return rHM;
        }
        
        List<String> lParams = new ArrayList<String>();

        ArrayList<String> sshUsers = new ArrayList<String>();
        ArrayList<String> sshHosts = new ArrayList<String>();
        ArrayList<String> sshFiles = new ArrayList<String>();

        
        int i = 0;
        for (i = 0; i < args.length; i++) {
            
            if (args[i].startsWith("-")) {
                if (i == args.length - 1 || args[i + 1].startsWith("-") || sArgs.contains(args[i])) {
                    rHM.put(args[i], "");
                } else {
                    if (sshUsers.size() > 0 && (args[i].equals("-c") || args[i].equals("-d"))) {
                        throw new IllegalArgumentException("Illegal syntax! You can use either either Client/Server (-c/-d) syntax, either SCP syntax");
                    }

                    rHM.put(args[i], args[i + 1]);
                    i++;
                }
            } else if (args[i].indexOf(":") >= 0) {
                int idx = args[i].indexOf(":");

                
                
                
                if (File.separatorChar == '\\') {

                    if (idx + 1 == args[i].length()) {
                        
                        
                        
                        
                        
                        
                        
                        
                        

                        if (idx - 1 == 0) {

                            
                            if (new File(args[i].charAt(0) + ":").exists()) {
                                
                                if (sshUsers.size() > 0) {
                                    
                                    rHM.put("destinationDir", args[i]);
                                    rHM.put("-d", rHM.get("destinationDir"));
                                    break;
                                }

                                lParams.add(args[i]);
                                continue;
                            }
                        }
                    }

                    if ((idx + 1 < args[i].length()) && args[i].charAt(idx + 1) == File.separatorChar) {
                        if (sshUsers.size() > 0) {
                            
                            rHM.put("destinationDir", args[i]);
                            rHM.put("-d", rHM.get("destinationDir"));
                            break;
                        }

                        lParams.add(args[i]);
                        continue;
                    }
                }

                
                
                

                

                
                if (sshUsers.size() == 0 && (rHM.get("-d") != null || rHM.get("-c") != null)) {
                    throw new IllegalArgumentException("Illegal syntax! You can use either Client/Server (-c/-d) syntax, either SCP syntax");
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
                    if (idx1 >= 0) { 

                        if (idx1 == 0) {
                            throw new IllegalArgumentException("Invalid scp syntax for " + args[i]);
                        }

                        user = userHost.substring(0, idx1);
                        if (idx1 + 1 < userHost.length()) {
                            host = userHost.substring(idx1 + 1);
                        } else {
                            throw new IllegalArgumentException("Invalid scp syntax for " + args[i]);
                        }
                    } else {
                        host = userHost;
                    }
                }

                if (idx + 1 == args[i].length()) {
                    path = ".";
                } else {
                    path = args[i].substring(idx + 1);
                }

                
                

                sshUsers.add(user);
                sshHosts.add(host);
                sshFiles.add(path);
            } else {
                if (sshUsers.size() > 0) {
                    
                    rHM.put("destinationDir", args[i]);
                    rHM.put("-d", rHM.get("destinationDir"));
                    break;
                }

                lParams.add(args[i]);
            }
        }

        int sshHostsNo = sshUsers.size();
        if (sshHostsNo > 0) {
            rHM.put("SCPSyntaxUsed", true);

            if (rHM.get("destinationDir") == null) {
                

                rHM.put("destinationUser", sshUsers.get(sshHostsNo - 1));
                rHM.put("destinationHost", sshHosts.get(sshHostsNo - 1));
                rHM.put("destinationDir", sshFiles.get(sshHostsNo - 1));
                rHM.put("-d", rHM.get("destinationDir"));
                rHM.put("-c", rHM.get("destinationHost"));

                
                sshUsers.remove(sshHostsNo - 1);
                sshHosts.remove(sshHostsNo - 1);
                sshFiles.remove(sshHostsNo - 1);

            }

            sshHostsNo = sshUsers.size();

            if (sshHostsNo > 0) {
                
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

    public static final String getStringValue(final Map<String, Object> configMap, String key, String DEFAULT_VALUE) {
        Object obj = configMap.get(key);
        if (obj == null) {
            return DEFAULT_VALUE;
        }

        return obj.toString();
    }

    public static final long getLongValue(
            final Map<String, Object> configMap,
            final String key,
            final long DEFAULT_VALUE) {
        long rVal = DEFAULT_VALUE;
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

    public static final double getDoubleValue(
            final Map<String, Object> configMap,
            final String key,
            final double DEFAULT_VALUE) {

        double rVal = DEFAULT_VALUE;
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

    public static final int getIntValue(final Map<String, Object> configMap, final String key, final int DEFAULT_VALUE) {

        int rVal = DEFAULT_VALUE;
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

    private static final File createOrGetRWFile(final String parentDirName, final String fileName) {

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

    public static final void updatePropertyAndStore(String dirName, String fileName, String property, String value) {

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
                    if (fis != null) {
                        try {
                            fis.close();
                        } catch (Throwable ignore) {
                        }
                    }

                    if (fos != null) {
                        try {
                            fos.close();
                        } catch (Throwable ignore) {
                        }
                    }
                }
            }
        }

    }

    private static final Properties getFDTUpdateProperties() {
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
                if (fis != null) {
                    try {
                        fis.close();
                    } catch (Throwable ignore) {
                    }
                    ;
                }
            }
        }
        return updateProperties;
    }

    private static final boolean updateTotalContor(final long total, final String property) {

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

                if (fis != null) {
                    try {
                        fis.close();
                    } catch (Throwable ignore) {
                    }
                    ;
                }

                if (logger.isLoggable(Level.FINEST)) {
                    logger.log(Level.FINEST, " [ Utils ] [ updateTotalContor ] loaded properties: " + updateProperties);
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

                updateProperties.put(property, "" + lastContor);
                updateProperties.put(property + "_rst", "" + rstContor);

                if (logger.isLoggable(Level.FINEST)) {
                    logger.log(Level.FINEST, " [ Utils ] [ updateTotalContor ] store new properties: " + updateProperties);
                }

                checkAndSetInstanceID(updateProperties);

                fos = new FileOutputStream(confFile);
                updateProperties.store(fos, null);
                fos.flush();

            } catch (Throwable t) {
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, "Unable to update properties file for property: " + property + " contor: " + total + " file: " + confFile, t);
                }
                return false;
            } finally {
                if (fis != null) {
                    try {
                        fis.close();
                    } catch (Throwable ignore) {
                    }
                    ;
                }
                if (fos != null) {
                    try {
                        fos.close();
                    } catch (Throwable ignore) {
                    }
                    ;
                }
            }
        }

        return true;
    }

    
    private static final void checkAndSetInstanceID(final Properties props) {

        if (props == null) {
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, " [ Utils ] [ checkAndSetInstanceID ] Null properties ... nothing to check/set");
            }
            return;
        }

        try {
            String instID = props.getProperty("instanceID");

            if (instID == null || instID.trim().equals("")) {
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
                    } finally {
                        if (fos != null) {
                            try {
                                fos.close();
                            } catch (Throwable ignore1) {
                            }
                            ;
                        }
                    }
                }
            }
        } catch (Throwable ignore) {
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, " [ Utils ] [ checkAndSetInstanceID ] Unable to get/set instanceID", ignore);
            }
        }

    }

    public static final boolean updateTotalReadContor(final long totalRead) throws Exception {
        return updateTotalContor(totalRead, "totalRead");
    }

    public static final boolean updateTotalWriteContor(final long totalWrite) throws Exception {
        return updateTotalContor(totalWrite, "totalWrite");
    }

    public static final boolean checkForUpdate(final String currentVersion, final String updateURL) throws Exception {
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
                    if (fis != null) {
                        try {
                            fis.close();
                        } catch (Throwable ignore) {
                        }
                        ;
                    }
                }

                final long now = System.currentTimeMillis();
                boolean bHaveUpdates = false;
                checkAndSetInstanceID(updateProperties);

                if (lastCheck + FDT.UPDATE_PERIOD < now) {
                    lastCheck = now;
                    try {
                        System.out.println("\n\nChecking for remote updates ... This may be disabled using -noupdates flag.");
                        bHaveUpdates = updateFDT(currentVersion, updateURL, false);
                        if (bHaveUpdates) {
                            System.out.println("FDT may be updated using: java -jar fdt.jar -update");
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

                    updateProperties.put("LastCheck", "" + now);

                    try {
                        fos = new FileOutputStream(confFile);
                        updateProperties.store(fos, null);
                    } catch (Throwable t1) {
                        logger.log(Level.WARNING, "Cannot store update properties file", t1);
                    } finally {
                        if (fos != null) {
                            try {
                                fos.close();
                            } catch (Throwable ignore) {
                            }
                            ;
                        }
                    }

                    return bHaveUpdates;
                }
            } else {
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, " [ checkForUpdate ] Cannot read or write the update conf file: " + parentFDTConfDirName + File.separator + fdtUpdateConfFileName);
                }
                return false;
            }

        } catch (Throwable t) {
            logger.log(Level.WARNING, "Got exception checking for updates", t);
        }

        return false;
    }

    
    public static final boolean updateFDT(final String currentVersion, final String updateURL, boolean shouldUpdate)
            throws Exception {

        final String partialURL = updateURL + (updateURL.endsWith("/") ? "" : "/") + "fdt.jar";
        System.out.print("Checking remote fdt.jar at URL: " + partialURL);
        String JVMVersion = "NotAvailable";
        String JVMRuntimeVersion = "NotAvailable";
        String OSVersion = "NotAvailable";
        String OSName = "NotAvailable";
        String OSArch = "NotAvailable";

        try {
            JVMVersion = System.getProperty("java.vm.version");
        } catch (Throwable t) {
            JVMVersion = "NotAvailable";
        }

        try {
            JVMRuntimeVersion = System.getProperty("java.runtime.version");
        } catch (Throwable t) {
            JVMRuntimeVersion = "NotAvailable";
        }

        try {
            OSName = System.getProperty("os.name");
        } catch (Throwable t) {
            OSName = "NotAvailable";
        }

        try {
            OSArch = System.getProperty("os.arch");
        } catch (Throwable t) {
            OSArch = "NotAvailable";
        }

        try {
            OSVersion = System.getProperty("os.version");
        } catch (Throwable t) {
            OSVersion = "NotAvailable";
        }

        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append(partialURL);
        urlBuilder.append("?FDTCurrentVersion=").append(currentVersion);
        urlBuilder.append("&shouldUpdate=").append(shouldUpdate);
        urlBuilder.append("&tstamp=").append(System.currentTimeMillis());
        urlBuilder.append("&java.vm.version=").append(JVMVersion);
        urlBuilder.append("&java.runtime.version=").append(JVMRuntimeVersion);
        urlBuilder.append("&os.name=").append(OSName);
        urlBuilder.append("&os.version=").append(OSVersion);
        urlBuilder.append("&os.arch=").append(OSArch);

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

        if (p != null && p.size() > 0) {
            for (final Map.Entry<Object, Object> entry : p.entrySet()) {
                urlBuilder.append("&").append(entry.getKey()).append("=").append(entry.getValue());
            }
        }

        final String finalPath = FDT.class.getProtectionDomain().getCodeSource().getLocation().getPath();

        if (finalPath == null || finalPath.length() == 0) {
            throw new IOException("Cannot determine the path to current fdtJar");
        }

        final File currentJar = new File(finalPath);

        if (!currentJar.exists()) {
            
            throw new IOException("Current fdt.jar path seems to be [ " + finalPath + " ] but the JVM cannot access it!");
        }

        if (currentJar.isFile() && currentJar.canWrite()) {
            System.out.println("\nCurrent fdt.jar path is: " + finalPath);
        } else {
            throw new IOException("Current fdt.jar path seems to be [ " + finalPath + " ] but it does not have write access!");
        }

        
        File tmpUpdateFile = null;
        FileOutputStream fos = null;
        JarFile jf = null;
        InputStream connInputStream = null;
        try {
            
            tmpUpdateFile = File.createTempFile("fdt_update_tmp", ".jar");
            tmpUpdateFile.deleteOnExit();

            fos = new FileOutputStream(tmpUpdateFile);

            final URLConnection urlConnection = new URL(urlBuilder.toString()).openConnection();
            urlConnection.setDefaultUseCaches(false);
            urlConnection.setUseCaches(false);

            urlConnection.setConnectTimeout(URL_CONNECTION_TIMEOUT);
            urlConnection.setReadTimeout(URL_CONNECTION_TIMEOUT);

            System.out.print("Connecting ... ");
            urlConnection.connect();
            connInputStream = urlConnection.getInputStream();
            System.out.println("OK");
            byte[] buff = new byte[8192];

            int count = 0;
            while ((count = connInputStream.read(buff)) > 0) {
                fos.write(buff, 0, count);
                fos.flush();
            }

            fos.flush();
            fos.close();

            
            jf = new JarFile(tmpUpdateFile);
            final Manifest mf = jf.getManifest();
            final Attributes attr = mf.getMainAttributes();
            final String remoteVersion = attr.getValue("Implementation-Version");

            jf.close();

            if (remoteVersion == null || remoteVersion.trim().length() == 0) {
                throw new Exception("Cannot read the version from the downloaded jar...Cannot compare versions!");
            }

            if (currentVersion.equals(remoteVersion.trim())) {
                
                return false;
            }

            System.out.println("Remote FDT version: " + remoteVersion + " Local FDT version: " + currentVersion + ". Update available.");

            if (shouldUpdate) {
                copyFile2File(tmpUpdateFile, currentJar);
            }

            return true;
        } finally {

            if (connInputStream != null) {
                try {
                    connInputStream.close();
                } catch (Throwable ignore) {
                }
            }

            if (fos != null) {
                try {
                    fos.close();
                } catch (Throwable ignore) {
                }
            }

            if (tmpUpdateFile != null) {
                try {
                    tmpUpdateFile.delete();
                } catch (Throwable ignore) {
                }
            }

            if (jf != null) {
                try {
                    jf.close();
                } catch (Throwable ignore) {
                }
            }
        }

    }

    public static final String md5ToString(byte[] md5sum) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < md5sum.length; i++) {
            sb.append(Integer.toString((md5sum[i] & 0xff) + 0x100, 16).substring(1));
        }

        return sb.toString();
    }

    
    public static final boolean checkForUpdate(
            final String currentVersion,
            final boolean shouldUpdate,
            final long updatePeriod,
            final String updateURL) {
        String updateFile = System.getProperty("user.home") + File.separatorChar + ".fdt" + File.separatorChar + "fdt_update";
        File f = new File(updateFile);
        long lTime = -1;

        if (!f.exists()) {
            new File(f.getParent()).mkdirs();
            try {
                f.createNewFile();
            
            
            
            
            } catch (IOException ex) {
                System.out.println("Could not create update checking file. Information about new updates will not be available.");
            }
        } else {
            FileReader fr = null;
            BufferedReader br = null;
            try {
                fr = new FileReader(f);
                br = new BufferedReader(fr);
                String line = br.readLine();
                fr.close();
                if (line != null) {
                    lTime = Long.parseLong(line);
                }
            } catch (Throwable t) {
                System.out.println("Could not read update checking file. Information about new updates will not be available.");
            
            } finally {
                if (br != null) {
                    try {
                        br.close();
                    } catch (Throwable ignore) {
                    }
                }

                if (fr != null) {
                    try {
                        fr.close();
                    } catch (Throwable ignore) {
                    }
                }
            }
        }
        long currentTime = System.currentTimeMillis();
        if (lTime == -1 || currentTime - lTime > updatePeriod || shouldUpdate) {
            System.out.println("Checking for updates... ");

            BufferedWriter bw = null;
            BufferedReader brDown = null;
            InputStream isDown = null;
            InputStreamReader isr = null;

            try {
                bw = new BufferedWriter(new FileWriter(f));
                if (bw != null) {
                    bw.write("" + currentTime);
                }
                bw.close();
                URL urlDown;
                urlDown = new URL(updateURL + "version");
                URLConnection connection = urlDown.openConnection();
                isDown = connection.getInputStream();
                isr = new InputStreamReader(isDown);
                brDown = new BufferedReader(isr);
                String new_version = brDown.readLine();
                brDown.close();

                if (new_version == null) {
                    System.out.println("Unable to check for remote version ... got null response from the web server");
                    return false;
                }

                if (!new_version.equals(currentVersion)) {
                    System.out.println("There is a new version (" + new_version + ") available at " + updateURL);
                    System.out.println("Would you like to update? [Y/n] ");
                    char car = (char) System.in.read();
                    if (car == 'Y' || car == 'y' || car == '\n' || car == '\r') {
                        
                        
                        URL urlDownJar = new URL(updateURL + "fdt.jar");
                        URLConnection connectionJar = urlDownJar.openConnection();
                        isDown = connectionJar.getInputStream();
                        File tempFile = File.createTempFile("update", "new_version.jar");
                        FileOutputStream fos = new FileOutputStream(tempFile);
                        byte[] buf = new byte[10240];
                        int read = -1;
                        while ((read = isDown.read(buf)) != -1) {
                            fos.write(buf, 0, read);
                        }
                        ;
                        fos.close();
                        
                        
                        String finalPath = FDT.class.getProtectionDomain().getCodeSource().getLocation().getPath();
                        File thisFile = new File(finalPath);
                        

                        
                        copyFile2File(tempFile, thisFile);
                        System.out.println("Application updated successfully. Exiting.");
                        return true;
                    
                    
                    
                    
                    
                    }
                } else {
                    System.out.println("You have the lastest version.");
                }
            } catch (Exception ex) {
                System.out.println("Error. Please check manually the site for new updates: " + updateURL);
                ex.printStackTrace();
            } finally {
                if (bw != null) {
                    try {
                        bw.close();
                    } catch (Throwable ignore) {
                    }
                }
                if (brDown != null) {
                    try {
                        brDown.close();
                    } catch (Throwable ignore) {
                    }
                }
                if (isDown != null) {
                    try {
                        isDown.close();
                    } catch (Throwable ignore) {
                    }
                }
                if (isr != null) {
                    try {
                        isr.close();
                    } catch (Throwable ignore) {
                    }
                }
            }
        }

        return false;
    }

    public static final void copyFile2File(File s, File d) throws Exception {
        FileChannel srcChannel = null;
        FileChannel dstChannel = null;

        try {
            srcChannel = new RandomAccessFile(s, "rw").getChannel();

            
            dstChannel = new FileOutputStream(d).getChannel();

            srcChannel.lock();
            dstChannel.lock();

            
            final long tr = dstChannel.transferFrom(srcChannel, 0, srcChannel.size());

            long ss = srcChannel.size();
            long ds = dstChannel.size();

            if (ss != ds || ss != tr) {
                throw new Exception("Different size for sourceFile [ " + s + " ] DestinationFileSize [ " + d + " ] Transferred [ " + tr + " ] ");
            }
        } finally {
            if (srcChannel != null) {
                try {
                    srcChannel.close();
                } catch (Throwable _) {
                }
            }

            if (dstChannel != null) {
                try {
                    dstChannel.close();
                } catch (Throwable _) {
                }
            }
        }
    }

    
    public static final void getRecursiveFiles(String fileName, List<String> allFiles) throws Exception {

        if (allFiles == null) {
            throw new NullPointerException("File list is null");
        }
        File file = new File(fileName);
        if (file.exists() && file.canRead()) {
            if (file.isFile()) {
                allFiles.add(fileName);
            } else if (file.isDirectory()) {
                String[] listContents = file.list();
                if (listContents != null && listContents.length > 0) {
                    for (String subFile : listContents) {
                        getRecursiveFiles(fileName + File.separator + subFile, allFiles);
                    }
                }
            } else {

                allFiles.add(fileName);
            }
        }
    }

    public static final String toStringSelectionKey(final FDTSelectionKey fsk) {
        if (fsk == null) {
            return " Null FDTSelectionKey ! ";
        }
        final SocketChannel sc = fsk.channel();
        final Selector sel = fsk.selector();

        final StringBuilder sb = new StringBuilder("Socket ");
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
                    sb.append(" ").append(toStringSelectionKey(sk));
                }
            }
        }
        return sb.toString();
    }

    public static final String toStringSelectionKey(final SelectionKey sk) {
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

    public static final String toStringSelectionKeyOps(final int keyOps) {
        final StringBuilder sb = new StringBuilder("{");

        boolean bAdded = false;
        for (int i = 0; i < SELECTION_KEY_OPS_VALUES.length; i++) {
            if ((keyOps & SELECTION_KEY_OPS_VALUES[i]) == SELECTION_KEY_OPS_VALUES[i]) {
                if (bAdded) {
                    sb.append("|");
                } else {
                    bAdded = true;
                }

                sb.append(" ").append(SELECTION_KEY_OPS_NAMES[i]).append(" ");
            }
        }

        sb.append("}");
        return sb.toString();
    }
}
