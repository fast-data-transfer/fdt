/*
 * $Id$
 */
package lia.util.net.copy.monitoring.lisa;

import lia.util.net.copy.monitoring.lisa.cmdExec.CommandResult;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.text.NumberFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class that monitors the internals of the Linux box...
 *
 * @author Ciprian Dobre
 */
public class ProcReader {

    final static NumberFormat nf = NumberFormat.getInstance();
    /**
     * PARAMETER UNITS
     */

//	( PARAM, UNIT):
//	("CpuUsage", "%")
//	("CpuUsr", "%")
//	("CpuSys", "%")
//	("CpuNice", "%")
//	("CpuIdle", "%")
//	("CpuIoWait", "%")
//	("CpuInt", "%")
//	("CpuSoftInt", "%")
//	("CpuSteal", "%")
//	("PagesIn", "")
//	("PagesOut", "")
//	("SwapIn", "")
//	("SwapOut", "")
//	("MemUsage", "%")
//	("MemFree", "MB")
//	("MemUsed", "MB")
//	("DiskIO", "KB/s")
//	("TPS", "TPS")
//	("DiskTotal", "GB")
//	("DiskUsed", "GB")
//	("DiskFree", "GB")
//	("DiskUsage", "%")
//	("NoProcesses", "")
//	("Load1", "")
//	("Load5", "")
//	("Load15", "")
//	("In_"+ifName, "Mbps")
//	("Out_"+ifName, "Mbps")

    private static final String SYS_EXTENDED_BIN_PATH = "/bin,/sbin,/usr/bin,/usr/sbin,/usr/local/bin";
    static public String[] ResTypes;

    static {
        nf.setMaximumFractionDigits(4);
        nf.setMinimumFractionDigits(4);
    }

    protected final Logger logger;
    private final cmdExec exec;
    private final LinkedList<String> devices = new LinkedList<String>();
    private final HashMap<String, String> diskIOReadKB = new HashMap<String, String>();
    private final HashMap<String, String> diskIOWriteKB = new HashMap<String, String>();
    private final HashMap<String, String> dKBRead = new HashMap<String, String>();
    private final HashMap<String, String> dKBWrite = new HashMap<String, String>();
    private final Hashtable<String, String> netIn = new Hashtable<String, String>();
    private final Hashtable<String, String> dnetIn = new Hashtable<String, String>();
    private final Hashtable<String, String> netOut = new Hashtable<String, String>();
    private final Hashtable<String, String> dnetOut = new Hashtable<String, String>();
    protected String PROC_FILE_NAMES[];
    protected FileReader fileReaders[];
    protected BufferedReader bufferedReaders[];
    String[] old; // = new long[8];
    String[] cur;
    String[] xtmp;
    String[] diff;
    long last_time_diskIO;
    long last_time_cpu;
    long last_time_net;
    private boolean hasCommonCPUStats;
    private boolean hasCPUIOWaitStats;
    private boolean hasCPUIntStats;
    private boolean hasCPUStealStats;
    private boolean hasPageProcStat;
    private boolean hasSwapProcStat;
    private boolean hasPageProcVmStat;
    private boolean hasSwapProcVmStat;
    /**
     * is this a 64 bits arch ?
     */
    private boolean is64BitArch = false;
    private boolean alreadyInitCPU = false;
    private String diskIO = null;
    private String tps = null;
    private double diskTotal;
    private double diskUsed;
    private double diskFree;
    private double memUsage;
    private double memUsed;
    private double memFree;
    private double load1, load5, load15;
    private String[] netInterfaces = null;

    public ProcReader(final Logger logger) {
        this.logger = logger;
        exec = cmdExec.getInstance();
    }

    public static void main(String args[]) {
        ProcReader reader = new ProcReader(Logger.getLogger("lisa"));
        System.out.println("***" + reader.getMACAddress());
//		try {HashMap h = reader.getCPUVM();
//		System.out.println(h);} catch (Throwable t) { t.printStackTrace(); }
//		try { Thread.sleep(1000); } catch (Throwable t) { }
//		try {HashMap h = reader.getCPUVM();
//		System.out.println(h);} catch (Throwable t) { t.printStackTrace(); }
//		try { Thread.sleep(1000); } catch (Throwable t) { }
//		try {HashMap h = reader.getCPUVM();
//		System.out.println(h);} catch (Throwable t) { t.printStackTrace(); }
        try {
            HashMap h = reader.getNet();
            System.out.println(h);
        } catch (Throwable t) {
            t.printStackTrace();
        }
        try {
            Thread.sleep(1000);
        } catch (Throwable t) {
        }
        try {
            HashMap h = reader.getNet();
            System.out.println(h);
        } catch (Throwable t) {
            t.printStackTrace();
        }
        try {
            Thread.sleep(1000);
        } catch (Throwable t) {
        }
    }

    /**
     * Method that returns the MAC hw address.
     *
     * @return The hw address or null
     * @since java1.6
     */
    public final String getMACAddress() {
        try {
            Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
            while (en.hasMoreElements()) {
                NetworkInterface ni = en.nextElement();
                // get the hardware address
                if (!ni.isLoopback()) {
                    try {
                        final String hadr = getHexa(ni.getHardwareAddress());
                        if (hadr != null) return hadr;
                    } catch (SocketException se) {
                    } // nothing, just move on here
                }
                Enumeration<NetworkInterface> en1 = ni.getSubInterfaces();
                while (en1.hasMoreElements()) {
                    ni = en1.nextElement();
                    // get the hardware address
                    if (!ni.isLoopback()) {
                        try {
                            final String hadr = getHexa(ni.getHardwareAddress());
                            if (hadr != null) return hadr;
                        } catch (SocketException se) {
                        } // nothing, just move on here
                    }
                }
            }
        } catch (Throwable se) {
            logger.log(Level.INFO, "Got error retrieving the network interfaces", se);
        }
        // if we did not succeded in this way, then try using ifconfig instead.

        final String path = getIfConfigPath();
        CommandResult cmdRes = exec.executeCommandReality("ifconfig -a", "b", path);
        String output = cmdRes.getOutput();
        if (cmdRes.failed())
            output = null;
        if (output != null && output.length() != 0 && !output.contains("No such file or directory") && !output.contains("Segmentation fault")) {
            StringTokenizer st = new StringTokenizer(output);
            String line = st.nextToken("\n");
            while (line != null) {
                if (line != null && (line.startsWith(" ") || line.startsWith("\t"))) {
                    line = st.nextToken("\n");
                    continue;
                }
                if (line == null)
                    break;
                StringTokenizer ast = new StringTokenizer(line);
                // get the name
                ast.nextToken(" \t\n"); // netName
                // get the hw address
                if (line.indexOf("HWaddr ") >= 0) return line.substring(line.indexOf("HWaddr ") + 7);
                line = st.nextToken("\n");
            }
        }
        return null;
    }

    /**
     * Method to retrieve CPU related parameters..
     *
     * @return A hashmap<String, Double>, where String is the name of the param and Double is the current value
     */
    public final HashMap<String, Double> getCPUVM() throws Exception {
        initCPUReaders();

        if (cur == null)
            cur = new String[ResTypes.length];
        if (xtmp == null)
            xtmp = new String[ResTypes.length];
        if (diff == null)
            diff = new String[ResTypes.length];


        if (hasSwapProcVmStat || hasPageProcVmStat) {
            PROC_FILE_NAMES = new String[]{"/proc/stat", "/proc/vmstat"};
        } else if (hasCommonCPUStats || hasSwapProcStat || hasPageProcStat) {
            PROC_FILE_NAMES = new String[]{"/proc/stat"};
        } else
            PROC_FILE_NAMES = null;
        createReaders();

        HashMap<String, Double> results = new HashMap<String, Double>();

        long sTime = System.currentTimeMillis();
        int len = ResTypes.length;
        BufferedReader br = bufferedReaders[0];
        int index = 0;
        boolean parsedCPU = !hasCommonCPUStats;

        for (; ; ) {
            String lin = br.readLine();

            if (lin == null)
                break;

            lin = lin.trim();
            if (lin.length() == 0)
                continue;

            StringTokenizer tz = new StringTokenizer(lin);

            String item = tz.nextToken().trim();
            if (!parsedCPU && hasCommonCPUStats && item.equals("cpu")) {
                parsedCPU = true;
                cur[index++] = tz.nextToken();
                cur[index++] = tz.nextToken();
                cur[index++] = tz.nextToken();
                cur[index++] = tz.nextToken();

                if (hasCPUIOWaitStats) {
                    cur[index++] = tz.nextToken();
                    if (hasCPUIntStats) {
                        cur[index++] = tz.nextToken();
                        cur[index++] = tz.nextToken();
                        if (hasCPUStealStats) {
                            cur[index++] = tz.nextToken();
                        }
                    }
                }
                if (hasPageProcVmStat && hasSwapProcVmStat)
                    break;
            } else if (item.equals("page")) {
                cur[index++] = tz.nextToken();
                cur[index++] = tz.nextToken();
            } else if (item.equals("swap")) {
                cur[index++] = tz.nextToken();
                cur[index++] = tz.nextToken();
            }
        }

        if (hasSwapProcVmStat || hasPageProcVmStat) {
            br = bufferedReaders[1];
            int cCount = 0;
            String lin = br.readLine();
            for (; cCount < 4 && lin != null; lin = br.readLine()) {
                lin = lin.trim();
                if (lin.startsWith("pgpgin")) {
                    cur[len - 4] = lin.substring(7);
                    cCount++;
                    continue;
                }

                if (lin.startsWith("pgpgout")) {
                    cur[len - 3] = lin.substring(8);
                    cCount++;
                    continue;
                }

                if (lin.startsWith("pswpin")) {
                    cur[len - 2] = lin.substring(7);
                    cCount++;
                    continue;
                }

                if (lin.startsWith("pswpout")) {
                    cur[len - 1] = lin.substring(8);
                    cCount++;
                    continue;
                }
            }
        }
        if (old == null) {
            old = cur;
            cur = xtmp;
            last_time_cpu = System.currentTimeMillis();
        } else {
            for (int i = 0; i < diff.length; i++) {
                diff[i] = diffWithOverflowCheck(cur[i], old[i]);
            }

            String sum = addWithOverflowCheck(diff[0], diff[1]);
            sum = addWithOverflowCheck(sum, diff[2]);
            sum = addWithOverflowCheck(sum, diff[3]);
            if (hasCPUIOWaitStats) {
                sum = addWithOverflowCheck(sum, diff[4]);
                if (hasCPUIntStats) {
                    sum = addWithOverflowCheck(sum, diff[5]);
                    sum = addWithOverflowCheck(sum, diff[6]);
                    if (hasCPUStealStats) {
                        sum = addWithOverflowCheck(sum, diff[7]);
                    }
                }
            }
            index = 0;

            String str1 = divideWithOverflowCheck(diff[0], sum);
            double d = Double.parseDouble(str1) * 100D;
            double totalP = d;
            results.put(ResTypes[index++], d);
            str1 = divideWithOverflowCheck(diff[1], sum);
            d = Double.parseDouble(str1) * 100D;
            totalP += d;
            results.put(ResTypes[index++], d);
            str1 = divideWithOverflowCheck(diff[2], sum);
            d = Double.parseDouble(str1) * 100D;
            totalP += d;
            results.put(ResTypes[index++], d);

            if (hasCPUIOWaitStats) {
                str1 = divideWithOverflowCheck(diff[4], sum);
                d = Double.parseDouble(str1) * 100D;
                totalP += d;
                results.put(ResTypes[index++], d);
                if (hasCPUIntStats) {
                    str1 = divideWithOverflowCheck(diff[5], sum);
                    d = Double.parseDouble(str1) * 100D;
                    totalP += d;
                    results.put(ResTypes[index++], d);
                    str1 = divideWithOverflowCheck(diff[6], sum);
                    d = Double.parseDouble(str1) * 100D;
                    totalP += d;
                    results.put(ResTypes[index++], d);
                    if (hasCPUStealStats) {
                        str1 = divideWithOverflowCheck(diff[7], sum);
                        d = Double.parseDouble(str1) * 100D;
                        totalP += d;
                        results.put(ResTypes[index++], d);
                    }
                }
            }

            str1 = divideWithOverflowCheck(diff[3], sum);
            d = Double.parseDouble(str1) * 100D;
            results.put(ResTypes[index++], d);
            if (Math.abs(totalP + d - 0D) < 0.0001)
                results.put("CPU_Usage", 1.0);
            else
                results.put("CPU_usage", (totalP / (totalP + d)));
            double imp = ((sTime - last_time_cpu) / 1000D) * 1024D;

			/*
             * Little comment here ( hopefully both page|swap _in|out will be in MB/s Check this first: - http://lkml.org/lkml/2002/4/12/6 -
			 * http://marc.theaimsgroup.com/?l=linux-kernel&m=101770318012189&w=2 the page_[ in | out ] represents in 1KB values the swap_[ in | out ] represents in PAGE_SIZE,
			 * usually 4KB values
			 */
            results.put(ResTypes[len - 4], Double.valueOf(divideWithOverflowCheck(diff[len - 4], "" + imp)));
            results.put(ResTypes[len - 3], Double.valueOf(divideWithOverflowCheck(diff[len - 3], "" + imp)));

            // TODO - this can be buggy
			/*
			 * To termine the PAGE_SIZE you can run this code cat << EOF | #include <stdio.h> main() { printf ("%d bytes\n",getpagesize()); } EOF gcc -xc - -o /tmp/getpagesize
			 * /tmp/getpagesize; rm -f /tmp/getpagesize PAGE_SIZE was 4096 bytes on all these machines ( though on SunOS the module does not work ! ): Linux pccit16 2.6.17-rc2 #2
			 * SMP Wed Apr 19 10:25:58 CEST 2006 i686 pentium4 i386 GNU/Linux ( Slack_10.2 ) Linux pccit15 2.4.20-18.7.cernsmp #1 SMP Thu Jun 12 12:27:49 CEST 2003 i686 unknown (
			 * RH_7.3 ) Linux lxplus056.cern.ch 2.4.21-40.EL.cernsmp #1 SMP Fri Mar 17 00:53:42 CET 2006 i686 i686 i386 GNU/Linux ( SLC 3.0.6 ) SunOS vinci 5.10 Generic_118844-26
			 * i86pc i386 i86pc Linux pccil 2.6.5-7.252-smp #1 SMP Tue Feb 14 11:11:04 UTC 2006 i686 i686 i386 GNU/Linux ( SuSE Linux 9.1 )
			 */
            results.put(ResTypes[len - 2], Double.valueOf(divideWithOverflowCheck((mulWithOverflowCheck(diff[len - 2], "" + 4)), "" + imp)));
            results.put(ResTypes[len - 1], Double.valueOf(divideWithOverflowCheck((mulWithOverflowCheck(diff[len - 1], "" + 4)), "" + imp)));

            last_time_cpu = sTime;
            xtmp = old;
            old = cur;
            cur = xtmp;
        }

        cleanup();
        return results;
    }

    /**
     * Method to retrieve memory information
     *
     * @return Mapping <String, Double> if the memory related params
     */
    public HashMap<String, Double> getMEM() throws Exception {

        BufferedReader in = new BufferedReader(new FileReader("/proc/meminfo"));
        String line = in.readLine();
        String sMemFree, sMemTotal;
        HashMap<String, Double> results = new HashMap<String, Double>();
        double dmemTotal = 0.0, dmemFree = 0.0;
        while (line != null) {
            if (line.startsWith("MemTotal:")) {
                line = line.substring(9);
                StringTokenizer ast = new StringTokenizer(line);
                try {
                    sMemTotal = ast.nextToken();
                } catch (Exception e) {
                    sMemTotal = null;
                }
                double d = 0.0;
                try {
                    d = Double.parseDouble(sMemTotal);
                } catch (Exception e) {
                    d = -1.0;
                }
                if (d >= 0.0)
                    dmemTotal = d;
            }
            if (line.startsWith("MemFree:")) {
                line = line.substring(8);
                StringTokenizer ast = new StringTokenizer(line);
                try {
                    sMemFree = ast.nextToken();
                } catch (Exception e) {
                    sMemFree = null;
                }
                double d = 0.0;
                try {
                    d = Double.parseDouble(sMemFree);
                } catch (Exception e) {
                    d = -1.0;
                }
                if (d >= 0.0)
                    dmemFree = d;
            }
            line = in.readLine();
        }
        memFree = (dmemFree / 1024.0);
        memUsed = ((dmemTotal - dmemFree) / 1024.0);
        memUsage = (100.0 * (dmemTotal - dmemFree) / dmemTotal);
        results.put("MemUsage", memUsage);
        results.put("MemUsed", memUsed);
        results.put("MemFree", memFree);
        in.close();
        return results;
    }

    /**
     * Method to retrieve information related to disk usage..
     *
     * @return Mapping <String, Double> of disk params...
     */
    public HashMap<String, Double> getDISK() throws Exception {
        String output = executeIOStat();
        String line, str;
        if (output != null && !output.equals("")) {
            long now = System.currentTimeMillis();
            diskIO = "0.00";
            tps = "0.00";
            StringTokenizer st = new StringTokenizer(output);
            try {
                line = st.nextToken("\n");
            } catch (Exception e) {
                line = null;
            }
            double imp = ((now - last_time_diskIO) / 1000D) * 1024D;
            while (line != null && !line.contains("Device:")) {
                try {
                    line = st.nextToken("\n");
                } catch (Exception e) {
                    line = null;
                }
            }
            devices.clear();
            if (line != null) {
                String diff1 = null, diff2 = null;
                while (true) {
                    try {
                        str = st.nextToken(" \t\n");
                    } catch (Exception e) {
                        str = null;
                    }
                    if (str == null)
                        break;
                    final String device = str;
                    devices.addLast(device);
                    try {
                        str = st.nextToken(" \t\n");
                    } catch (Exception e) {
                        str = null;
                    } // tps
                    tps = addWithOverflowCheck(str, tps);
                    try {
                        str = st.nextToken(" \t\n");
                    } catch (Exception e) {
                        str = null;
                    } // skip KB read / sec
                    try {
                        str = st.nextToken(" \t\n");
                    } catch (Exception e) {
                        str = null;
                    } // skip KB write / sec
                    try {
                        str = st.nextToken(" \t\n");
                    } catch (Exception e) {
                        str = null;
                    } // KB read
                    final String dKBr = (String) dKBRead.get(device);
                    diff1 = diffWithOverflowCheck(str, dKBr == null ? str : dKBr);
                    diskIO = addWithOverflowCheck(diff1, diskIO);
                    if (dKBRead.containsKey(device)) {
                        String s = diffWithOverflowCheck(str, dKBr);
                        diskIOReadKB.put(device, divideWithOverflowCheck(s, "" + imp));
                    }
                    dKBRead.put(device, str);
                    // sec
                    try {
                        str = st.nextToken(" \t\n");
                    } catch (Exception e) {
                        str = null;
                    } // skip KB write /
                    final String dKBw = (String) dKBWrite.get(device);
                    diff2 = diffWithOverflowCheck(str, dKBw == null ? str : dKBw);
                    diskIO = addWithOverflowCheck(diff2, diskIO);
                    if (dKBWrite.containsKey(device)) {
                        String s = diffWithOverflowCheck(str, dKBw);
                        diskIOWriteKB.put(device, divideWithOverflowCheck(s, "" + imp));
                    }
                    dKBWrite.put(device, str);
                }
            }

            diskIO = divideWithOverflowCheck(diskIO, "" + imp);
            last_time_diskIO = now;
        } else return null;

        HashMap<String, Double> results = new HashMap<String, Double>();

        LinkedList<String> toRemove = new LinkedList<String>();
        for (Iterator<String> it = dKBRead.keySet().iterator(); it.hasNext(); ) {
            String d = it.next();
            if (!devices.contains(d)) {
                toRemove.addLast(d);
            }
        }
        for (Iterator<String> it = toRemove.iterator(); it.hasNext(); ) {
            String d = it.next();
            dKBRead.remove(d);
            dKBWrite.remove(d);
            diskIOReadKB.remove(d);
            diskIOWriteKB.remove(d);
        }

        output = executeDF();
        double size = 0.0, used = 0.0, available = 0.0, usage = 0.0;
        if (output != null && output != "") {
            StringTokenizer st = new StringTokenizer(output);
            try {
                line = st.nextToken(" \t\n");
            } catch (Exception e) {
                line = null;
            }
            int nr = 0;
            for (int i = 0; i < 6 && line != null; i++)
                try {
                    line = st.nextToken(" \t\n");
                } catch (Exception e) {
                    line = null;
                }
            while (true) {
                try {
                    line = st.nextToken(" \t\n");
                } catch (Exception e) {
                    line = null;
                }
                if (line == null)
                    break;
                try {
                    line = st.nextToken(" \t\n");
                } catch (Exception e) {
                    line = null;
                } // size
                double d = 0.0;
                try {
                    d = Double.parseDouble(line);
                } catch (Exception e) {
                    d = -1.0;
                }
                if (d < 0.0)
                    break;
                size += d;
                try {
                    line = st.nextToken(" \t\n");
                } catch (Exception e) {
                    line = null;
                } // used
                d = 0.0;
                try {
                    d = Double.parseDouble(line);
                } catch (Exception e) {
                    d = -1.0;
                }
                if (d < 0.0)
                    break;
                used += d;
                try {
                    line = st.nextToken(" \t\n");
                } catch (Exception e) {
                    line = null;
                } // available
                d = 0.0;
                try {
                    d = Double.parseDouble(line);
                } catch (Exception e) {
                    d = -1.0;
                }
                if (d < 0.0)
                    break;
                available += d;
                try {
                    line = st.nextToken(" \t\n");
                } catch (Exception e) {
                    line = null;
                } // usage
                try {
                    line = line.substring(0, line.indexOf("%"));
                } catch (Exception e) {
                }
                d = 0.0;
                try {
                    d = Double.parseDouble(line);
                } catch (Exception e) {
                    d = -1.0;
                }
                if (d < 0.0)
                    break;
                usage += d;
                nr++;
                try {
                    line = st.nextToken(" \t\n");
                } catch (Exception e) {
                    line = null;
                }
                if (line == null)
                    break;
            }
            diskTotal = (size / (1024.0 * 1024.0)); // total size (GB)
            diskUsed = (used / (1024.0 * 1024.0)); // used size (GB)
            diskFree = (available / (1024.0 * 1024.0)); // free size
            // (GB)
//			diskUsage = (usage * 1.0 / nr); // usage (%)
        } else { // read from /proc/ide
            String files[] = listFiles("/proc/ide");
            if (files != null && files.length != 0)
                for (int i = 0; i < files.length; i++)
                    if (files[i].startsWith("hd")) {
                        try {
                            BufferedReader in = new BufferedReader(new FileReader("/proc/ide/" + files[i] + "/capacity"));
                            line = in.readLine();
                            double d = 0.0;
                            try {
                                d = Double.parseDouble(line);
                            } catch (Exception e) {
                                d = -1.0;
                            }
                            if (d >= 0.0)
                                size += d;
                            in.close();
                        } catch (Exception e) {
                        }
                    }
            diskTotal = (size / (1024.0 * 1024.0)); // disk total (GB)
            diskFree = diskTotal;
        }

        try {
            results.put("DiskIO", Double.valueOf(diskIO));
        } catch (Exception e) {
            results.put("DiskIO", -1.0);
        }
        try {
            results.put("TPS", Double.valueOf(tps));
        } catch (Exception e) {
            results.put("TPS", -1.0);
        }
        if (devices != null)
            for (String dev : devices) {
                if (diskIOReadKB.containsKey(dev)) {
                    try {
                        results.put("DiskIORead_" + dev, Double.valueOf(diskIOReadKB.get(dev)));
                    } catch (Exception e) {
                        results.put("DiskIORead_" + dev, -1.0);
                    }
                }
                if (diskIOWriteKB.containsKey(dev)) {
                    try {
                        results.put("DiskIORead_" + dev, Double.valueOf(diskIOReadKB.get(dev)));
                    } catch (Exception e) {
                        results.put("DiskIORead_" + dev, -1.0);
                    }
                }
            }
        results.put("DiskTotal", diskTotal);
        results.put("DiskUsed", diskUsed);
        results.put("DiskFree", diskFree);
        results.put("DiskUsage", new Double(100.0 * diskUsed / (diskUsed + diskFree)));
        toRemove.clear();
        return results;
    }

    /**
     * Retrieves the number of running processes or -1 if error
     *
     * @return The number of processes
     */
    public int getProcesses() {
        String[] files = listFiles("/proc");
        if (files != null && files.length != 0) {
            int nr = 0;
            for (int i = 0; i < files.length; i++) {
                char[] chars = files[i].toCharArray();
                boolean isProc = true;
                for (int j = 0; j < chars.length; j++)
                    if (!Character.isDigit(chars[j])) {
                        isProc = false;
                        break;
                    }
                if (isProc)
                    nr++;
            }
            return nr;
        }
        return -1;
    }

    /**
     * Retrieves the current load values
     *
     * @return Mapping <String, Double> of load1, 5, 15
     */
    public HashMap<String, Double> getLOAD() throws Exception {
        BufferedReader in = new BufferedReader(new FileReader("/proc/loadavg"));
        String f = "";
        String line;
        while ((line = in.readLine()) != null) {
            f = f + "\n" + line;
        }
        in.close();
        HashMap<String, Double> results = new HashMap<String, Double>();
        StringTokenizer st = new StringTokenizer(f);
        try {
            line = st.nextToken(" \t\n");
        } catch (Exception e) {
            line = null;
        } // load1
        if (line != null) {
            double d = 0.0;
            try {
                d = Double.parseDouble(line);
            } catch (Exception e) {
                d = -1.0;
            }
            if (d >= 0.0)
                load1 = d;
            try {
                line = st.nextToken(" \t\n");
            } catch (Exception e) {
                line = null;
            }// load5
            d = 0.0;
            try {
                d = Double.parseDouble(line);
            } catch (Exception e) {
                d = -1.0;
            }
            if (d >= 0.0)
                load5 = d;
            try {
                line = st.nextToken(" \t\n");
            } catch (Exception e) {
                line = null;
            } // load15
            d = 0.0;
            try {
                d = Double.parseDouble(line);
            } catch (Exception e) {
                d = -1.0;
            }
            if (d >= 0.0)
                load15 = d;
        }
        results.put("Load1", load1);
        results.put("Load5", load5);
        results.put("Load15", load15);
        return results;
    }

    /**
     * Method to retrieve traffic information
     *
     * @return Mapping <String, Double> of in and out traffic for each found interfaces
     */
    public HashMap<String, Double> getNet() throws Exception {
        String line;
        BufferedReader in = new BufferedReader(new FileReader("/proc/net/dev"));
        String f = "";
        while ((line = in.readLine()) != null) {
            f = f + "\n" + line;
        }
        in.close();
        HashMap<String, Double> results = new HashMap<String, Double>();
        StringTokenizer st = new StringTokenizer(f);
        long now = System.currentTimeMillis();
        if (netInterfaces == null) {
            while (true) {
                try {
                    line = st.nextToken(":\n\t ");
                } catch (Exception e) {
                    line = null;
                }
                if (line == null)
                    break;
                if (line.startsWith("eth") || line.startsWith("lo")) {
                    addNetInterface(line);
                    String name = line;
                    try {
                        line = st.nextToken(" \t\n");
                    } catch (Exception e) {
                        line = null;
                    } // bytes received
                    if (dnetIn.containsKey(name)) {
                        final String lastIn = dnetIn.get(name);
                        try {
                            double d = Double.parseDouble(diffWithOverflowCheck(line, lastIn));
                            d = (d * 8.0D) / (now - last_time_net) / 1000.0D;
                            netIn.put(name, "" + d);
                        } catch (Exception e) {
                        }
                        dnetIn.put(name, line);
                    } else {
                        netIn.put(name, line);
                        dnetIn.put(name, line);
                    }
                    for (int i = 0; i < 7; i++) {
                        try {
                            line = st.nextToken(" \t\n");
                        } catch (Exception e) {
                            line = null;
                        }
                    }
                    try {
                        line = st.nextToken(" \t\n");
                    } catch (Exception e) {
                        line = null;
                    } // bytes sent
                    if (dnetOut.containsKey(name)) {
                        final String lastOut = dnetOut.get(name);
                        try {
                            double d = Double.parseDouble(diffWithOverflowCheck(line, lastOut));
                            d = (d * 8.0D) / (now - last_time_net) / 1000.0D;
                            netOut.put(name, "" + d);
                        } catch (Exception e) {
                        }
                        dnetOut.put(name, line);
                    } else {
                        netOut.put(name, line);
                        dnetOut.put(name, line);
                    }
                }
            }
        } else {
            while (true) {
                try {
                    line = st.nextToken(":\n\t ");
                } catch (Exception e) {
                    line = null;
                }
                if (line == null)
                    break;
                boolean found = false;
                for (int i = 0; i < netInterfaces.length; i++)
                    if (line.equals(netInterfaces[i])) {
                        found = true;
                        break;
                    }
                if (found) {
                    String name = line;
                    try {
                        line = st.nextToken(" \t\n:");
                    } catch (Exception e) {
                        line = null;
                    }// bytes received
                    if (dnetIn.containsKey(name)) {
                        final String lastIn = dnetIn.get(name);
                        try {
                            double d = Double.parseDouble(diffWithOverflowCheck(line, lastIn));
                            d = (d * 8.0D) / (now - last_time_net) / 1000.0D;
                            // logger.info("For "+name+"in = "+d+" old="+lastIn+" new="+line+" diff="+(now - last_time_net));
                            netIn.put(name, "" + d);
                        } catch (Exception e) {
                        }
                        dnetIn.put(name, line);
                    } else {
                        netIn.put(name, line);
                        dnetIn.put(name, line);
                    }
                    for (int i = 0; i < 7; i++) {
                        try {
                            line = st.nextToken(" \t\n");
                        } catch (Exception e) {
                            line = null;
                        }
                    }
                    try {
                        line = st.nextToken(" \t\n");
                    } catch (Exception e) {
                        line = null;
                    } // bytes sent
                    if (dnetOut.containsKey(name)) {
                        final String lastOut = dnetOut.get(name);
                        try {
                            double d = Double.parseDouble(diffWithOverflowCheck(line, lastOut));
                            d = (d * 8.0D) / (now - last_time_net) / 1000.0D;
                            // logger.info("For "+name+" out = "+d+" old="+lastOut+" new="+line+" diff="+(now - last_time_net));
                            netOut.put(name, "" + d);
                        } catch (Exception e) {
                        }
                        dnetOut.put(name, line);
                    } else {
                        netOut.put(name, line);
                        dnetOut.put(name, line);
                    }
                } else {
                    if (line.startsWith("eth") || line.startsWith("lo")) {
                        addNetInterface(line);
                        String name = line;
                        try {
                            line = st.nextToken(" \t\n");
                        } catch (Exception e) {
                            line = null;
                        } // bytes received
                        if (dnetIn.containsKey(name)) {
                            final String lastIn = dnetIn.get(name);
                            try {
                                double d = Double.parseDouble(diffWithOverflowCheck(line, lastIn));
                                d = (d * 8.0D) / (now - last_time_net) / 1000.0D;
                                netIn.put(name, "" + d);
                            } catch (Exception e) {
                            }
                            dnetIn.put(name, line);
                        } else {
                            netIn.put(name, line);
                            dnetIn.put(name, line);
                        }
                        for (int i = 0; i < 7; i++) {
                            try {
                                line = st.nextToken(" \t\n");
                            } catch (Exception e) {
                                line = null;
                            }
                        }
                        try {
                            line = st.nextToken(" \t\n");
                        } catch (Exception e) {
                            line = null;
                        } // bytes sent
                        if (dnetOut.containsKey(name)) {
                            final String lastOut = dnetOut.get(name);
                            try {
                                double d = Double.parseDouble(diffWithOverflowCheck(line, lastOut));
                                d = (d * 8.0D) / (now - last_time_net) / 1000.0D;
                                netOut.put(name, "" + d);
                            } catch (Exception e) {
                            }
                            dnetOut.put(name, line);
                        } else {
                            netOut.put(name, line);
                            dnetOut.put(name, line);
                        }
                    }
                }
            }
        }
        last_time_net = now;
        if (netInterfaces != null && netInterfaces.length != 0)
            for (int i = 0; i < netInterfaces.length; i++) {
                final String ifName = netInterfaces[i];
                try {
                    results.put("In_" + ifName, Double.valueOf(netIn.get(ifName)));
                } catch (Exception e) {
                }
                try {
                    results.put("Out_" + ifName, Double.valueOf(netOut.get(ifName)));
                } catch (Exception e) {
                }
            }
        return results;
    }

    /**
     * UTILITY METHODS
     */

    private final String getHexa(byte[] b) {
        if (b == null || b.length == 0) return null;
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < b.length; i++) {
            byte value = b[i];
            int d1 = value & 0xF;
            d1 += (d1 < 10) ? 48 : 55;
            int d2 = (value & 0xF0) >> 4;
            d2 += (d2 < 10) ? 48 : 55;
            buf.append((char) d2).append((char) d1);
            if (i < b.length - 1) buf.append(':');
        }
        return buf.toString();
    }

    private final synchronized String getIfConfigPath() {
        String path = SYS_EXTENDED_BIN_PATH;
        if (System.getProperty("ifconfig.path", null) != null)
            path = System.getProperty("ifconfig.path");
        if (path == null || path.length() == 0) {
            logger.warning("[Host - ifconfig can not be found in " + path + "]");
            return null;
        }
        return path.replace(',', ':').trim();
    }

    private final void initCPUReaders() {
        if (alreadyInitCPU) return;
        try {
            ArrayList<String> al = new ArrayList<String>(15);// at least
            // 12
            FileReader fr = null;
            BufferedReader br = null;
            File procFile = null;
            // check for info that can be processed from /proc/stat
            try {
                procFile = new File("/proc/stat");
                if (procFile.exists() && procFile.canRead()) {
                    fr = new FileReader("/proc/stat");
                    br = new BufferedReader(fr);
                    String line = br.readLine();
                    boolean parsedCPU = false;
                    for (; line != null; line = br.readLine()) {
                        line = line.trim();
                        if (!parsedCPU && line.startsWith("cpu")) {
                            parsedCPU = true;
                            String[] tokens = line.split("(\\s)+");
                            int len = tokens.length;
                            if (len >= 5) {
                                al.add("CPU_usr");
                                al.add("CPU_nice");
                                al.add("CPU_sys");
                                hasCommonCPUStats = true;
                                if (len >= 6) {
                                    al.add("CPU_iowait");
                                    hasCPUIOWaitStats = true;
                                    if (len >= 8) {
                                        al.add("CPU_int");
                                        al.add("CPU_softint");
                                        hasCPUIntStats = true;
                                        if (len >= 9) {
                                            al.add("CPU_steal");
                                            hasCPUStealStats = true;
                                        }
                                    }
                                }
                                al.add("CPU_idle");
                            }// if (len >= 5 )
                        } else {// if ( "cpu" )
                            if (line.startsWith("page")) {
                                hasPageProcStat = true;
                            } else if (line.startsWith("swap")) {
                                hasSwapProcStat = true;
                            }
                        }
                    }// for
                }// if ( procStatF.exists() )
            } catch (Throwable pft) {
                logger.log(Level.WARNING, "Checking for /proc/stat yield a caught exception ", pft);
            } finally {
                try {
                    if (fr != null)
                        fr.close();
                    if (br != null)
                        br.close();
                } catch (Throwable ignore) {
                }
                fr = null;
                br = null;
            }

            // check for info that can be processed from /proc/vmstat
            procFile = new File("/proc/vmstat");
            try {
                if (procFile.exists() && procFile.canRead()) {
                    fr = new FileReader("/proc/vmstat");
                    br = new BufferedReader(fr);

                    String line = br.readLine();
                    for (; line != null; line = br.readLine()) {
                        line = line.trim();
                        if (line.startsWith("pgpgin")) {
                            hasPageProcVmStat = true;
                            continue;
                        }
                        if (line.startsWith("pswpin")) {
                            hasSwapProcVmStat = true;
                            continue;
                        }
                    }// for
                }// if - exists && canRead
            } catch (Throwable pft) {
                logger.log(Level.WARNING, "Checking for /proc/vmstat yield a caught exception ", pft);
            } finally {
                try {
                    if (fr != null)
                        fr.close();
                    if (br != null)
                        br.close();
                } catch (Throwable ignore) {
                }
                fr = null;
                br = null;
            }

            if (hasPageProcStat || hasPageProcVmStat) {
                al.add("Page_in");
                al.add("Page_out");
            }

            if (hasSwapProcStat || hasSwapProcVmStat) {
                al.add("Swap_in");
                al.add("Swap_out");
            }
            ResTypes = (String[]) al.toArray(new String[al.size()]);
            alreadyInitCPU = true;
        } catch (Throwable t) {
            logger.log(Level.WARNING, "Got exception in init. The module will not be used for local monitoring ", t);
        }
    }

    protected void resetReaders() throws Exception {
        createReaders();
    }

    public void cleanup() {
        if (bufferedReaders != null) {
            for (int i = 0; i < bufferedReaders.length; i++) {
                try {
                    if (bufferedReaders[i] != null) {
                        bufferedReaders[i].close();
                    }
                } catch (Throwable t) {
                    logger.log(Level.WARNING, "Got exception closing buffered reader [ " + i + " ] ", t);
                } finally {
                    bufferedReaders[i] = null; // let GC do the job
                }
            }
        }// if bufferedReaders
        if (fileReaders != null) {
            for (int i = 0; i < fileReaders.length; i++) {
                try {
                    if (fileReaders[i] != null) {
                        fileReaders[i].close();
                    }
                } catch (Throwable t) {
                    logger.log(Level.WARNING, "Got exception closing file reader [ " + i + " ] ", t);
                } finally {
                    fileReaders[i] = null; // let GC do the job
                }
            }
        }
    }

    private void createReaders() throws Exception {
        cleanup();
        if (PROC_FILE_NAMES == null)
            throw new Exception(" PROC_FILE_NAMES is null");

        if (bufferedReaders == null || bufferedReaders.length != PROC_FILE_NAMES.length) {
            bufferedReaders = new BufferedReader[PROC_FILE_NAMES.length];
        }

        if (fileReaders == null || fileReaders.length != PROC_FILE_NAMES.length) {
            fileReaders = new FileReader[PROC_FILE_NAMES.length];
        }

        for (int i = 0; i < PROC_FILE_NAMES.length; i++) {
            try {
                if (PROC_FILE_NAMES[i] != null) {
                    fileReaders[i] = new FileReader(PROC_FILE_NAMES[i]);
                    bufferedReaders[i] = new BufferedReader(fileReaders[i]);
                } else {
                    logger.warning("PROC_FILE_NAMES[" + i + "] is null");
                }
            } catch (Throwable t) {
                logger.log(Level.WARNING, "Got exc creating Readers [ " + i + " ] :- " + PROC_FILE_NAMES[i] + " ", t);
            }
        }
    }

    private String addWithOverflowCheck(String newVal, String oldVal) throws NumberFormatException {

        if (newVal == null)
            return oldVal;
        if (oldVal == null)
            return newVal;

        if (is64BitArch) {
            String str = prepareString(newVal);
            BigDecimal newv = null;
            try {
                newv = new BigDecimal(str);
            } catch (Throwable t) {
                logger.log(Level.WARNING, "Got exception " + t + " for " + str);
            }
            str = prepareString(oldVal);
            BigDecimal oldv = null;
            try {
                oldv = new BigDecimal(str);
            } catch (Throwable t) {
                logger.log(Level.WARNING, "Got exception " + t + " for " + str);
            }
            return newv.add(oldv).toString();
        }
        // otherwise we still assume 32 bits arch
        double toCompare = 1L << 32;
        double newv = Double.parseDouble(newVal);
        double oldv = Double.parseDouble(oldVal);
        if (newv >= toCompare || oldv >= toCompare) {
            is64BitArch = true;
            return addWithOverflowCheck(newVal, oldVal);
        }
        // so it's still 32 bits arch
        return "" + (newv + oldv);
    }

    private String divideWithOverflowCheck(String newVal, String oldVal) throws NumberFormatException {

        if (is64BitArch) {
            String str = prepareString(newVal);
            BigDecimal newv = null;
            try {
                newv = new BigDecimal(str);
            } catch (Throwable t) {
                logger.log(Level.WARNING, "Got exception " + t + " for " + str);
            }
            str = prepareString(oldVal);
            BigDecimal oldv = null;
            try {
                oldv = new BigDecimal(str);
            } catch (Throwable t) {
                logger.log(Level.WARNING, "Got exception " + t + " for " + str);
            }
            return newv.divide(oldv, BigDecimal.ROUND_FLOOR).toString();
        }
        // otherwise we still assume 32 bits arch
        double toCompare = 1L << 32;
        double newv = Double.parseDouble(newVal);
        double oldv = Double.parseDouble(oldVal);
        if (newv >= toCompare || oldv >= toCompare) {
            is64BitArch = true;
            return divideWithOverflowCheck(newVal, oldVal);
        }
        // so it's still 32 bits arch
        return "" + (newv / oldv);
    }

    private String mulWithOverflowCheck(String newVal, String oldVal) throws NumberFormatException {
        if (is64BitArch) {
            String str = prepareString(newVal);
            BigDecimal newv = null;
            try {
                newv = new BigDecimal(str);
            } catch (Throwable t) {
                logger.log(Level.WARNING, "Got exception " + t + " for " + str);
            }
            str = prepareString(oldVal);
            BigDecimal oldv = null;
            try {
                oldv = new BigDecimal(str);
            } catch (Throwable t) {
                logger.log(Level.WARNING, "Got exception " + t + " for " + str);
            }
            return newv.multiply(oldv).toString();
        }
        // otherwise we still assume 32 bits arch
        double toCompare = 1L << 32;
        double newv = Double.parseDouble(newVal);
        double oldv = Double.parseDouble(oldVal);
        if (newv >= toCompare || oldv >= toCompare) {
            is64BitArch = true;
            return mulWithOverflowCheck(newVal, oldVal);
        }
        // so it's still 32 bits arch
        return "" + (newv * oldv);
    }

    private String diffWithOverflowCheck(String newVal, String oldVal) throws NumberFormatException {
        if (is64BitArch) {
            String str = prepareString(newVal);
            BigDecimal newv = null;
            try {
                newv = new BigDecimal(str);
            } catch (Throwable t) {
                logger.log(Level.WARNING, "Got exception " + t + " for " + str);
            }
            str = prepareString(oldVal);
            BigDecimal oldv = null;
            try {
                oldv = new BigDecimal(str);
            } catch (Throwable t) {
                logger.log(Level.WARNING, "Got exception " + t + " for " + str);
            }
            if (newv.compareTo(oldv) >= 0)
                return newv.subtract(oldv).toString();
            BigInteger overflow = new BigInteger("1").shiftLeft(64);
            BigDecimal d = new BigDecimal(overflow.toString());
            return newv.add(d).subtract(oldv).toString();
        }
        // otherwise we still assume 32 bits arch
        double toCompare = 1L << 32;
        double newv = Double.parseDouble(newVal);
        double oldv = Double.parseDouble(oldVal);
        if (newv >= toCompare || oldv >= toCompare) {
            is64BitArch = true;
            return diffWithOverflowCheck(newVal, oldVal);
        }
        // so it's still 32 bits arch
        if (newv >= oldv) {
            return "" + (newv - oldv);
        }
        long vmax = 1L << 32; // 32 bits
        return "" + (newv - oldv + vmax);
    }

    private final String prepareString(String str) {

        // first try to make it double
        try {
            double d = Double.parseDouble(str);
            if (!Double.isInfinite(d) && !Double.isNaN(d)) {
                String n = nf.format(d);
                n = n.replaceAll(",", "");
                return n;
            }
        } catch (Throwable t) {
        }

        if (!str.contains(".")) {
            return str + ".0000";
        }
        int nr = str.lastIndexOf('.') + 1;
        nr = str.length() - nr;
        for (int i = nr; i < 4; i++)
            str += "0";
        return str;
    }

    private final synchronized String executeIOStat() {

        String path = SYS_EXTENDED_BIN_PATH + "," + System.getProperty("user.home");
        if (System.getProperty("iostat.path", null) != null)
            path = System.getProperty("iostat.path");
        if (path != null && path.length() != 0) {
            path = path.replace(',', ':').trim();
        }

        CommandResult cmdRes = exec.executeCommandReality("iostat -k", "L", path);
        final String output = cmdRes.getOutput();
        if (!cmdRes.failed() && output.length() != 0 && !output.contains("No such file or directory") && !output.contains("Segmentation fault"))
            return output;
        return null;
    }

    private final synchronized String executeDF() {

        String path = SYS_EXTENDED_BIN_PATH;
        if (System.getProperty("df.path", null) != null)
            path = System.getProperty("df.path");
        if (path != null && path.length() != 0) {
            path = path.replace(",", ":").trim();
        }
        CommandResult cmdRes = exec.executeCommandReality("df -B 1024", "o", path);
        String output = cmdRes.getOutput();
        if (!cmdRes.failed() && output.length() != 0 && !output.contains("No such file or directory") && !output.contains("Segmentation fault"))
            return output;
        return null;
    }

    private String[] listFiles(String directory) {
        String[] fileList = null;
        try {
            File dir = new File(directory);
            if (!dir.isDirectory()) return null;
            File[] list = dir.listFiles();
            if (list == null) return null;
            fileList = new String[list.length];
            for (int i = 0; i < list.length; i++)
                fileList[i] = list[i].getName();
        } catch (Exception e) {
            return null;
        }
        return fileList;
    }

    private void addNetInterface(String netInterface) {
        if (netInterface == null || netInterface.equals(""))
            return;
        netInterface = netInterface.trim();
        if (netInterfaces == null) {
            netInterfaces = new String[1];
            netInterfaces[0] = netInterface;
            return;
        }
        for (int i = 0; i < netInterfaces.length; i++)
            if (netInterface.equals(netInterfaces[i]))
                return;
        String[] tmpNetInterfaces = new String[netInterfaces.length + 1];
        System.arraycopy(netInterfaces, 0, tmpNetInterfaces, 0, netInterfaces.length);
        tmpNetInterfaces[netInterfaces.length] = netInterface;
        netInterfaces = tmpNetInterfaces;
    }

} // end of class ProcReader

