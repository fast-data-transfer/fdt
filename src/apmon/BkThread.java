/*
 * ApMon - Application Monitoring Tool
 * Version: 2.2.7
 *
 * Copyright (C) 2006 - 2010 California Institute of Technology
 *
 * Permission is hereby granted, free of charge, to use, copy and modify 
 * this software and its documentation (the "Software") for any
 * purpose, provided that existing copyright notices are retained in 
 * all copies and that this notice is included verbatim in any distributions
 * or substantial portions of the Software. 
 * This software is a part of the MonALISA framework (http://monalisa.cacr.caltech.edu).
 * Users of the Software are asked to feed back problems, benefits,
 * and/or suggestions about the software to the MonALISA Development Team
 * (developers@monalisa.cern.ch). Support for this software - fixing of bugs,
 * incorporation of new features - is done on a best effort basis. All bug
 * fixes and enhancements will be made available under the same terms and
 * conditions as the original software,
 
 * IN NO EVENT SHALL THE AUTHORS OR DISTRIBUTORS BE LIABLE TO ANY PARTY FOR
 * DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES ARISING OUT
 * OF THE USE OF THIS SOFTWARE, ITS DOCUMENTATION, OR ANY DERIVATIVES THEREOF,
 * EVEN IF THE AUTHORS HAVE BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 
 * THE AUTHORS AND DISTRIBUTORS SPECIFICALLY DISCLAIM ANY WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE, AND NON-INFRINGEMENT. THIS SOFTWARE IS
 * PROVIDED ON AN "AS IS" BASIS, AND THE AUTHORS AND DISTRIBUTORS HAVE NO
 * OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS, OR
 * MODIFICATIONS.
 */

package apmon;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import apmon.host.HostPropertiesMonitor;
import apmon.host.Parser;
import apmon.host.cmdExec;

/**
 * Separate thread which periodically checks the configuration file/URLs for changes and periodically sends datagrams
 * with monitoring information.
 */
class BkThread extends Thread {

    /* types of operations that this thread executes */
    public static final int RECHECK_CONF = 0;

    public static final int SYS_INFO_SEND = 1;

    public static final int JOB_INFO_SEND = 2;

    static String osName = System.getProperty("os.name");

    private static Logger logger = Logger.getLogger("apmon");

    ApMon apm;

    boolean hasToRun = true;

    /* LISA object used for obtaining system monitoring information */
    HostPropertiesMonitor monitor = null;

    public BkThread(ApMon apm) {
        this.apm = apm;
        this.monitor = new HostPropertiesMonitor();
        this.setDaemon(true);
    }

    void stopIt() {
        hasToRun = false;
    }

    /**
     * Returns true if the given parameter is set to be included in the system monitoring packet.
     */
    boolean isActive_Sys(long param) {
        return ((apm.sysMonitorParams & param) == param);
    }

    boolean isActive_Sys(Long param) {
        return isActive_Sys(param.longValue());
    }

    /**
     * Returns true if the given parameter is set to be included in the job monitoring packet.
     */
    boolean isActive_Job(long param) {
        return ((apm.jobMonitorParams & param) == param);
    }

    boolean isActive_Job(Long param) {
        return isActive_Job(param.longValue());
    }

    /**
     * Returns true if the given parameter is set to be included in the general system monitoring packet.
     */
    boolean isActive_Gen(long param) {
        return ((apm.genMonitorParams & param) == param);
    }

    boolean isActive_Gen(Long param) {
        return isActive_Gen(param.longValue());
    }

    void sendJobInfo() {
        int i;

        synchronized (apm.mutexBack) {

            if (apm.monJobs.size() == 0) {
                logger.warning("There are not jobs to be monitored, not sending job monitoring information...");
                return;
            }
            Date d = new Date();
            logger.info("Sending job monitoring information...");
            apm.lastJobInfoSend = d.getTime();

            for (i = 0; i < apm.monJobs.size(); i++)
                sendOneJobInfo(((MonitoredJob) apm.monJobs.get(i)));
        }
    }

    /**
     * Sends an UDP datagram with job monitoring information. Works only in Linux system; on other systems it takes no
     * action.
     */
    void sendOneJobInfo(MonitoredJob monJob) {

        Vector paramNames, paramValues;
        // , valueTypes;

        long crtTime = System.currentTimeMillis();

        apm.lastJobInfoSend = crtTime;

        paramNames = new Vector();
        paramValues = new Vector();
        // valueTypes = new Vector();

        HashMap hmJobInfo = null;

        try {
            hmJobInfo = monJob.readJobInfo();
        } catch (IOException e) {
            logger.warning("Unable to read job info for " + monJob.getPid());
            hmJobInfo = null;
        }

        if (hmJobInfo == null) {
            logger.warning("Job " + monJob.pid + " does not exist");
            apm.removeJobToMonitor(monJob.pid);
            return;
        }

        HashMap hmJobDisk = null;

        try {
            hmJobDisk = monJob.readJobDiskUsage();
        } catch (Throwable t1) {
            logger.warning("Unable to read job Disk Usage info for " + monJob.getPid());
            hmJobDisk = null;
        }

        // hmJobInfo != null FOR SURE!
        HashMap hm = hmJobInfo;

        if (hmJobDisk != null) {
            hm.putAll(hmJobDisk);
        }

        for (Iterator it = hm.keySet().iterator(); it.hasNext();) {
            Long lParam = (Long) it.next();
            try {
                if (isActive_Job(lParam)) {
                    Double val = (Double) hm.get(lParam);
                    paramNames.add(ApMonMonitoringConstants.getJobMLParamName(lParam));
                    paramValues.add(val);
                    // valueTypes.add(new Integer(ApMon.XDR_REAL64));
                }
            } catch (Throwable t) {
                logger.log(Level.WARNING, "", t);
                if (apm.autoDisableMonitoring) {
                    logger.warning("parameter " + ApMonMonitoringConstants.getJobName(lParam) + " disabled");
                    apm.sysMonitorParams &= ~lParam.longValue();
                }
            }
        }

        try {
            apm.sendParameters(monJob.clusterName, monJob.nodeName, paramNames.size(), paramNames, paramValues);
        } catch (Exception e) {
            logger.warning("Error while sending system information: " + e);
        }
    }

    /** Sends an UDP datagram with system monitoring information */
    void sendSysInfo() {
        double value = 0.0;
        Vector paramNames, paramValues;
        // , valueTypes;

        monitor.updateCall();
        long crtTime = System.currentTimeMillis();

        logger.info("Sending system monitoring information...");
        // long intervalLength = crtTime - apm.lastSysInfoSend;
        apm.lastSysInfoSend = crtTime;

        paramNames = new Vector();
        paramValues = new Vector();
        // valueTypes = new Vector();

        HashMap hms = monitor.getHashParams();

        if (hms != null) {
            for (Iterator it = hms.keySet().iterator(); it.hasNext();) {
                Long lParam = (Long) it.next();
                try {
                    if (isActive_Sys(lParam)) {
                        Double val = Double.valueOf((String) hms.get(lParam));
                        paramNames.add(ApMonMonitoringConstants.getSysMLParamName(lParam));
                        paramValues.add(val);
                        // valueTypes.add(new Integer(ApMon.XDR_REAL64));
                    }
                } catch (Throwable t) {
                    if (apm.autoDisableMonitoring) {
                        logger.warning("parameter " + ApMonMonitoringConstants.getSysName(lParam) + " disabled");
                        apm.sysMonitorParams &= ~lParam.longValue();
                    }
                }
            }
        }

        if (apm.netInterfaces != null && apm.netInterfaces.size() != 0) {
            for (int i = 0; i < apm.netInterfaces.size(); i++) {
                String iName = (String) apm.netInterfaces.get(i);
                if (iName == null)
                    continue;
                try {
                    if (isActive_Sys(ApMonMonitoringConstants.SYS_NET_IN)) {
                        value = Double.parseDouble(monitor.getNetInCall(iName));
                        if (osName.indexOf("Mac") == -1)
                            // measure in KBps
                            value = value * 1024;
                        paramNames.add(iName + "_" + ApMonMonitoringConstants.getSysMLParamName(ApMonMonitoringConstants.SYS_NET_IN));
                        paramValues.add(new Double(value));
                        // valueTypes.add(new Integer(ApMon.XDR_REAL64));
                    }
                } catch (Throwable t) {
                    logger.log(Level.WARNING, "", t);
                    // should not disable ... can work for the other interfaces
                }

                try {
                    if (isActive_Sys(ApMonMonitoringConstants.SYS_NET_OUT)) {
                        value = Double.parseDouble(monitor.getNetOutCall(iName));
                        if (osName.indexOf("Mac") == -1)
                            // measure in KBps
                            value = value * 1024;
                        paramNames.add(iName + "_" + ApMonMonitoringConstants.getSysMLParamName(ApMonMonitoringConstants.SYS_NET_OUT));
                        paramValues.add(new Double(value));
                        // valueTypes.add(new Integer(ApMon.XDR_REAL64));
                    }
                } catch (Throwable t) {
                    logger.log(Level.WARNING, "", t);
                    ;
                    // should not disable ... can work for the other interfaces
                }
            }
        }

        if (isActive_Sys(ApMonMonitoringConstants.SYS_UPTIME)) {
            try {
                value = getUpTime();
                paramNames.add(ApMonMonitoringConstants.getSysMLParamName(ApMonMonitoringConstants.SYS_UPTIME));
                paramValues.add(new Double(value));
                // valueTypes.add(new Integer(ApMon.XDR_REAL64));
            } catch (Exception e) {
                logger.log(Level.WARNING, "", e);
                ;
                if (apm.autoDisableMonitoring) {
                    logger.warning("parameter sys_uptime disabled");
                    apm.sysMonitorParams &= ~ApMonMonitoringConstants.SYS_UPTIME;
                }
            }
        }

        if (isActive_Sys(ApMonMonitoringConstants.SYS_PROCESSES)) {
            try {
                Hashtable vals = monitor.getPState();
                Enumeration e = vals.keys();
                while (e.hasMoreElements()) {
                    String name = (String) e.nextElement();
                    Integer val = (Integer) vals.get(name);
                    paramNames.add(ApMonMonitoringConstants.getSysMLParamName(ApMonMonitoringConstants.SYS_PROCESSES) + "_" + name);
                    paramValues.add(val);
                    // valueTypes.add(new Integer(ApMon.XDR_INT32));
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, "", e);
                ;
                if (apm.autoDisableMonitoring) {
                    logger.warning("parameter processes disabled");
                    apm.sysMonitorParams &= ~ApMonMonitoringConstants.SYS_PROCESSES;
                }
            }
        }

        if (isActive_Sys(ApMonMonitoringConstants.SYS_NET_SOCKETS)) {
            try {
                Hashtable vals = monitor.getSockets();
                Enumeration e = vals.keys();
                while (e.hasMoreElements()) {
                    String name = (String) e.nextElement();
                    Integer val = (Integer) vals.get(name);
                    paramNames.add(ApMonMonitoringConstants.getSysMLParamName(ApMonMonitoringConstants.SYS_NET_SOCKETS) + "_" + name);
                    paramValues.add(val);
                    // valueTypes.add(new Integer(ApMon.XDR_INT32));
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, "", e);
                ;
                if (apm.autoDisableMonitoring) {
                    logger.warning("parameter processes disabled");
                    apm.sysMonitorParams &= ~ApMonMonitoringConstants.SYS_NET_SOCKETS;
                }
            }
        }

        if (isActive_Sys(ApMonMonitoringConstants.SYS_NET_TCP_DETAILS)) {
            try {
                Hashtable vals = monitor.getTCPDetails();
                Enumeration e = vals.keys();
                while (e.hasMoreElements()) {
                    String name = (String) e.nextElement();
                    Integer val = (Integer) vals.get(name);
                    paramNames.add(ApMonMonitoringConstants.getSysMLParamName(ApMonMonitoringConstants.SYS_NET_TCP_DETAILS) + "_" + name);
                    paramValues.add(val);
                    // valueTypes.add(new Integer(ApMon.XDR_INT32));
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, "", e);
                ;
                if (apm.autoDisableMonitoring) {
                    logger.warning("parameter processes disabled");
                    apm.sysMonitorParams &= ~ApMonMonitoringConstants.SYS_NET_TCP_DETAILS;
                }
            }
        }

        try {
            apm.sendParameters(apm.sysClusterName, apm.sysNodeName, paramNames.size(), paramNames, paramValues);
        } catch (Exception e) {
            logger.warning("Error while sending system information: " + e);
        }
    }

    /**
     * Sends an UDP datagram with general system monitoring information. Only works with Linux systems; on other systems
     * it is disabled.
     */
    void sendGeneralInfo() {
        Vector paramNames, paramValues;
        // , valueTypes;
        double cpu_MHz, bogomips;
        int no_CPUs, i;

        paramNames = new Vector();
        paramValues = new Vector();
        // valueTypes = new Vector();

        paramNames.add("hostname");
        paramValues.add(apm.myHostname);
        // valueTypes.add(new Integer(ApMon.XDR_STRING));
        for (i = 0; i < apm.netInterfaces.size(); i++) {
            try {
                paramNames.add("ip_" + apm.netInterfaces.get(i));
                paramValues.add(apm.allMyIPs.get(i));
                // valueTypes.add(new Integer(ApMon.XDR_STRING));
            } catch (Exception e) {
                logger.log(Level.FINE, "BkThread got exception, ignoring network interface: ", e);
            }
        }

        Hashtable cpuInfo = new Hashtable();
        try {
            cpuInfo = getCpuInfo();
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (isActive_Gen(ApMonMonitoringConstants.GEN_CPU_MHZ)) {
            try {
                cpu_MHz = Double.parseDouble((String) cpuInfo.get(ApMonMonitoringConstants.LGEN_CPU_MHZ));
                paramNames.add(ApMonMonitoringConstants.getGenMLParamName(ApMonMonitoringConstants.LGEN_CPU_MHZ));
                paramValues.add(new Double(cpu_MHz));
                // valueTypes.add(new Integer(ApMon.XDR_REAL64));
            } catch (Throwable t) {
                logger.log(Level.WARNING, "", t);
                if (apm.autoDisableMonitoring)
                    apm.genMonitorParams &= ~ApMonMonitoringConstants.GEN_CPU_MHZ;
            }
        }

        String val;

        if (isActive_Gen(ApMonMonitoringConstants.GEN_CPU_VENDOR_ID)) {
            try {
                val = ((String) cpuInfo.get(ApMonMonitoringConstants.LGEN_CPU_VENDOR_ID)).trim();
                paramNames.add(ApMonMonitoringConstants.getGenMLParamName(ApMonMonitoringConstants.LGEN_CPU_VENDOR_ID));
                paramValues.add(val);
                // valueTypes.add(new Integer(ApMon.XDR_STRING));
            } catch (Throwable t) {
                logger.log(Level.WARNING, "", t);
                if (apm.autoDisableMonitoring)
                    apm.genMonitorParams &= ~ApMonMonitoringConstants.GEN_CPU_VENDOR_ID;
            }
        }

        if (isActive_Gen(ApMonMonitoringConstants.GEN_CPU_FAMILY)) {
            try {
                val = ((String) cpuInfo.get(ApMonMonitoringConstants.LGEN_CPU_FAMILY)).trim();
                paramNames.add(ApMonMonitoringConstants.getGenMLParamName(ApMonMonitoringConstants.LGEN_CPU_FAMILY));
                paramValues.add(val);
                // valueTypes.add(new Integer(ApMon.XDR_STRING));
            } catch (Throwable t) {
                logger.log(Level.WARNING, "", t);
                if (apm.autoDisableMonitoring)
                    apm.genMonitorParams &= ~ApMonMonitoringConstants.GEN_CPU_FAMILY;
            }
        }

        if (isActive_Gen(ApMonMonitoringConstants.GEN_CPU_MODEL)) {
            try {
                val = ((String) cpuInfo.get(ApMonMonitoringConstants.LGEN_CPU_MODEL)).trim();
                paramNames.add(ApMonMonitoringConstants.getGenMLParamName(ApMonMonitoringConstants.LGEN_CPU_MODEL));
                paramValues.add(val);
                // valueTypes.add(new Integer(ApMon.XDR_STRING));
            } catch (Throwable t) {
                logger.log(Level.WARNING, "", t);
                if (apm.autoDisableMonitoring)
                    apm.genMonitorParams &= ~ApMonMonitoringConstants.GEN_CPU_MODEL;
            }
        }

        if (isActive_Gen(ApMonMonitoringConstants.GEN_CPU_MODEL_NAME)) {
            try {
                val = ((String) cpuInfo.get(ApMonMonitoringConstants.LGEN_CPU_MODEL_NAME)).trim();
                paramNames.add(ApMonMonitoringConstants.getGenMLParamName(ApMonMonitoringConstants.LGEN_CPU_MODEL_NAME));
                paramValues.add(val);
                // valueTypes.add(new Integer(ApMon.XDR_STRING));
            } catch (Throwable t) {
                logger.log(Level.WARNING, "", t);
                if (apm.autoDisableMonitoring)
                    apm.genMonitorParams &= ~ApMonMonitoringConstants.GEN_CPU_MODEL_NAME;
            }
        }

        if (isActive_Gen(ApMonMonitoringConstants.GEN_BOGOMIPS)) {
            try {
                bogomips = Double.parseDouble((String) cpuInfo.get(ApMonMonitoringConstants.LGEN_BOGOMIPS));
                paramNames.add(ApMonMonitoringConstants.getGenMLParamName(ApMonMonitoringConstants.LGEN_BOGOMIPS));
                paramValues.add(new Double(bogomips));
                // valueTypes.add(new Integer(ApMon.XDR_REAL64));
            } catch (Throwable t) {
                logger.log(Level.WARNING, "", t);
                if (apm.autoDisableMonitoring)
                    apm.genMonitorParams &= ~ApMonMonitoringConstants.GEN_BOGOMIPS;
            }
        }

        if (isActive_Gen(ApMonMonitoringConstants.GEN_NO_CPUS)) {
            try {
                no_CPUs = getNumCPUs();
                paramNames.add(ApMonMonitoringConstants.getGenMLParamName(ApMonMonitoringConstants.LGEN_NO_CPUS));
                paramValues.add(new Integer(no_CPUs));
                // valueTypes.add(new Integer(ApMon.XDR_INT32));
            } catch (Throwable t) {
                logger.log(Level.WARNING, "", t);
                if (apm.autoDisableMonitoring)
                    apm.genMonitorParams &= ~ApMonMonitoringConstants.GEN_NO_CPUS;
            }
        }

        if (isActive_Gen(ApMonMonitoringConstants.GEN_TOTAL_MEM)) {
            try {
                Double tm = Double.valueOf(monitor.getMemTotalCall());
                paramNames.add(ApMonMonitoringConstants.getGenName(ApMonMonitoringConstants.LGEN_TOTAL_MEM));
                paramValues.add(tm);
                // valueTypes.add(new Integer(ApMon.XDR_REAL64));
            } catch (Throwable t) {
                logger.log(Level.WARNING, "", t);
                ;
                if (apm.autoDisableMonitoring)
                    apm.genMonitorParams &= ~ApMonMonitoringConstants.GEN_TOTAL_MEM;
            }
        }

        if (isActive_Gen(ApMonMonitoringConstants.GEN_TOTAL_SWAP)) {
            try {
                Double tm = Double.valueOf(monitor.getSwapTotalCall());
                paramNames.add(ApMonMonitoringConstants.getGenName(ApMonMonitoringConstants.LGEN_TOTAL_SWAP));
                paramValues.add(tm);
                // valueTypes.add(new Integer(ApMon.XDR_REAL64));
            } catch (Throwable t) {
                logger.log(Level.WARNING, "", t);
                ;
                if (apm.autoDisableMonitoring)
                    apm.genMonitorParams &= ~ApMonMonitoringConstants.GEN_TOTAL_SWAP;
            }
        }

        try {
            apm.sendParameters(apm.sysClusterName, apm.sysNodeName, paramNames.size(), paramNames, paramValues);
        } catch (Exception e) {
            logger.warning("Error while sending general system information: " + e);
        }
    }

    public void run() {
        long crtTime, timeRemained, nextOpTime;
        long nextRecheck = 0, nextJobInfoSend = 0, nextSysInfoSend = 0;
        int generalInfoCount;
        int nextOp = 0;
        boolean haveChange, haveTimeout;
        logger.info("[Starting background thread...]");

        crtTime = System.currentTimeMillis();

        synchronized (apm.mutexBack) {
            if (apm.confCheck) {
                nextRecheck = crtTime + apm.crtRecheckInterval * 1000;
            }
            if (apm.jobMonitoring)
                nextJobInfoSend = crtTime + apm.jobMonitorInterval * 1000;
            if (apm.sysMonitoring)
                nextSysInfoSend = crtTime + apm.sysMonitorInterval * 1000;
        }
        timeRemained = nextOpTime = -1;
        generalInfoCount = 0;

        while (hasToRun) {
            crtTime = System.currentTimeMillis();

            /* determine the next operation that must be performed */
            if (nextRecheck > 0 && (nextJobInfoSend <= 0 || nextRecheck <= nextJobInfoSend)) {
                if (nextSysInfoSend <= 0 || nextRecheck <= nextSysInfoSend) {
                    nextOp = RECHECK_CONF;
                    nextOpTime = nextRecheck;
                } else {
                    nextOp = SYS_INFO_SEND;
                    nextOpTime = nextSysInfoSend;
                }
            } else {
                if (nextJobInfoSend > 0 && (nextSysInfoSend <= 0 || nextJobInfoSend <= nextSysInfoSend)) {
                    nextOp = JOB_INFO_SEND;
                    nextOpTime = nextJobInfoSend;
                } else if (nextSysInfoSend > 0) {
                    nextOp = SYS_INFO_SEND;
                    nextOpTime = nextSysInfoSend;
                }
            }

            if (nextOpTime == -1)
                nextOpTime = crtTime + ApMon.RECHECK_INTERVAL * 1000;

            synchronized (apm.mutexCond) {
                synchronized (apm.mutexBack) {
                    /* check for changes in the settings */
                    haveChange = false;
                    if (apm.jobMonChanged || apm.sysMonChanged || apm.recheckChanged)
                        haveChange = true;
                    if (apm.jobMonChanged) {
                        if (apm.jobMonitoring)
                            nextJobInfoSend = crtTime + apm.jobMonitorInterval * 1000;
                        else
                            nextJobInfoSend = -1;
                        apm.jobMonChanged = false;
                    }
                    if (apm.sysMonChanged) {
                        if (apm.sysMonitoring)
                            nextSysInfoSend = crtTime + apm.sysMonitorInterval * 1000;
                        else
                            nextSysInfoSend = -1;
                        apm.sysMonChanged = false;
                    }
                    if (apm.recheckChanged) {
                        if (apm.confCheck)
                            nextRecheck = crtTime + apm.crtRecheckInterval * 1000;
                        else
                            nextRecheck = -1;
                        apm.recheckChanged = false;
                    }
                } // synchronized(apm.mutexBack)

                if (haveChange)
                    continue;

                timeRemained = nextOpTime - System.currentTimeMillis();
                haveTimeout = true;
                /*
                 * wait until the next operation should be performed or until a change in the settings occurs
                 */
                try {
                    if (timeRemained > 0)
                        apm.mutexCond.wait(timeRemained);
                } catch (InterruptedException e) {
                }

                if (apm.condChanged) {
                    haveTimeout = false;
                }
                apm.condChanged = false;
            }
            // logger.info("### have timeout " + haveTimeout);
            crtTime = System.currentTimeMillis();
            // System.out.println("### 3 crtTime " + crtTime + " nextOpTime " + nextOpTime);

            if (haveTimeout) { // the time interval until the next operation expired
                /* now perform the operation */
                if (nextOp == JOB_INFO_SEND) {
                    sendJobInfo();
                    nextJobInfoSend = crtTime + apm.getJobMonitorInterval() * 1000;
                }

                if (nextOp == SYS_INFO_SEND) {
                    sendSysInfo();
                    if (apm.getGenMonitoring()) {
                        /*
                         * send only 2 general monitoring packets in genMonitorIntervals intervals
                         */
                        if (generalInfoCount <= 1)
                            sendGeneralInfo();
                        generalInfoCount = (generalInfoCount + 1) % apm.genMonitorIntervals;
                    }
                    nextSysInfoSend = crtTime + apm.getSysMonitorInterval() * 1000;
                }

                if (nextOp == RECHECK_CONF) {
                    /* check all the configuration resources (file, URLs) */
                    Enumeration e = apm.confResources.keys();
                    boolean resourceChanged = false;

                    try {
                        while (e.hasMoreElements()) {
                            Object obj = e.nextElement();

                            Long lastModified = (Long) apm.confResources.get(obj);
                            if (obj instanceof File) {
                                File f = (File) obj;
                                logger.info(" [Checking for modifications for " + f.getCanonicalPath() + "]");

                                long lmt = f.lastModified();
                                if (lmt > lastModified.longValue()) {
                                    logger.info("[File " + f.getCanonicalPath() + " modified]");
                                    resourceChanged = true;
                                    break;
                                    // confResources.put(f, new Long(lmt));
                                }
                            }

                            if (obj instanceof URL) {
                                URL u = (URL) obj;
                                long lmt = 0;

                                logger.info("[Checking for modifications for " + u + "]");
                                URLConnection urlConn = u.openConnection();
                                lmt = urlConn.getLastModified();

                                if (lmt > lastModified.longValue() || lmt == 0) {
                                    logger.info("[Location " + u + " modified]");
                                    resourceChanged = true;
                                    break;
                                }
                            }
                        } // while

                        /*
                         * if any resource has changed we have to recheck all the others, otherwise some destinations
                         * might be ommitted
                         */
                        if (resourceChanged) {
                            if (apm.initType == ApMon.FILE_INIT) {
                                apm.initialize((String) apm.initSource, false);
                            }

                            if (apm.initType == ApMon.LIST_INIT) {
                                apm.initialize((Vector) apm.initSource, false);
                            }
                        }
                        apm.setCrtRecheckInterval(apm.getRecheckInterval());
                    } catch (Throwable exc) {
                        apm.setCrtRecheckInterval(10 * apm.getRecheckInterval());
                    }
                    crtTime = System.currentTimeMillis();
                    nextRecheck = crtTime + apm.getRecheckInterval() * 1000;
                } // while
            }
        }

    }

    public static Hashtable getCpuInfo() throws IOException, ApMonException {
        if (osName.indexOf("Linux") < 0)
            return null;

        Hashtable info = new Hashtable();

        BufferedReader in = null;
        FileReader fr = null;
        try {
            fr = new FileReader("/proc/cpuinfo");
            in = new BufferedReader(fr);
            String line = null;
            while ((line = in.readLine()) != null) {
                StringTokenizer st = new StringTokenizer(line, ":");
                if (line.startsWith("cpu MHz")) {
                    st.nextToken();
                    String freq_s = st.nextToken();
                    if (freq_s == null)
                        throw new ApMonException("Error reading CPU frequency from /proc/cpuinfo");
                    info.put(ApMonMonitoringConstants.LGEN_CPU_MHZ, freq_s);
                }
                if (line.startsWith("vendor_id")) {
                    st.nextToken();
                    String vendor = st.nextToken();
                    if (vendor == null)
                        throw new ApMonException("Error reading CPU vendor_id from /proc/cpuinfo");
                    info.put(ApMonMonitoringConstants.LGEN_CPU_VENDOR_ID, vendor);
                }
                if (line.startsWith("model") && !line.startsWith("model name")) {
                    st.nextToken();
                    String model = st.nextToken();
                    if (model == null)
                        throw new ApMonException("Error reading CPU model from /proc/cpuinfo");
                    info.put(ApMonMonitoringConstants.LGEN_CPU_MODEL, model);
                }
                if (line.startsWith("cpu family")) {
                    st.nextToken();
                    String cpufam = st.nextToken();
                    if (cpufam == null)
                        throw new ApMonException("Error reading CPU family from /proc/cpuinfo");
                    info.put(ApMonMonitoringConstants.LGEN_CPU_FAMILY, cpufam);
                }
                if (line.startsWith("model name")) {
                    st.nextToken();
                    String modelname = st.nextToken();
                    if (modelname == null)
                        throw new ApMonException("Error reading CPU model name from /proc/cpuinfo");
                    info.put(ApMonMonitoringConstants.LGEN_CPU_MODEL_NAME, modelname);
                }
                if (line.startsWith("bogomips")) {
                    st.nextToken();
                    String bogomips = st.nextToken();
                    if (bogomips == null)
                        throw new ApMonException("Error reading CPU bogomips from /proc/cpuinfo");
                    info.put(ApMonMonitoringConstants.LGEN_BOGOMIPS, bogomips);
                }
            }
        } finally {
            if (fr != null) {
                try {
                    fr.close();
                } catch (Throwable ignore) {
                }
            }
            if (in != null) {
                try {
                    in.close();
                } catch (Throwable ignore) {
                }
            }
        }
        return info;
    }

    /** Returns the system boot time in milliseconds since the Epoch. Works only on Linux systems. */
    public static long getBootTime() throws IOException, ApMonException {
        if (osName.indexOf("Linux") < 0)
            return 0;

        BufferedReader in = null;
        FileReader fr = null;
        try {
            fr = new FileReader("/proc/stat");
            in = new BufferedReader(fr);
            String line = null;
            while ((line = in.readLine()) != null) {
                if (line.startsWith("btime"))
                    break;
            }

            if (line == null)
                throw new ApMonException("Error reading boot time from /proc/stat");

            StringTokenizer st = new StringTokenizer(line);
            st.nextToken();
            String btime_s = st.nextToken();

            if (btime_s == null)
                throw new ApMonException("Error reading boot time from /proc/stat");

            return (Long.parseLong(btime_s) * 1000);
        } finally {
            if (fr != null) {
                try {
                    fr.close();
                } catch (Throwable ignore) {
                }
            }
            if (in != null) {
                try {
                    in.close();
                } catch (Throwable ignore) {
                }
            }
        }
    }

    /* in days */
    public static double getUpTime() throws IOException, ApMonException {
        if (osName.indexOf("Linux") < 0)
            return 0;

        FileReader fr = null;
        BufferedReader in = null;

        try {
            fr = new FileReader("/proc/uptime");
            in = new BufferedReader(fr);
            String line = in.readLine();
            if (line == null)
                throw new ApMonException("Error reading boot time from /proc/uptime");

            StringTokenizer st = new StringTokenizer(line);
            String up_time = st.nextToken();

            if (up_time == null)
                throw new ApMonException("Error reading optime from /proc/uptime");

            return (Double.parseDouble(up_time)) / (3600 * 24);
        } finally {
            if (fr != null) {
                try {
                    fr.close();
                } catch (Throwable ignore) {
                }
            }
            if (in != null) {
                try {
                    in.close();
                } catch (Throwable ignore) {
                }
            }
        }

    }

    /** Returns the number of CPUs in the system */
    public static int getNumCPUs() throws IOException, ApMonException {
        if (osName.indexOf("Linux") < 0)
            return 0;

        FileReader fr = null;
        BufferedReader in = null;
        try {
            fr = new FileReader("/proc/stat");
            in = new BufferedReader(fr);
            String line = null;
            int numCPUs = 0;
            while ((line = in.readLine()) != null) {
                if (line.startsWith("cpu") && Character.isDigit(line.charAt(3)))
                    numCPUs++;
            }

            if (numCPUs == 0)
                throw new ApMonException("Error reading CPU frequency from /proc/stat");

            return numCPUs;
        } finally {
            if (fr != null) {
                try {
                    fr.close();
                } catch (Throwable ignore) {
                }
            }
            if (in != null) {
                try {
                    in.close();
                } catch (Throwable ignore) {
                }
            }
        }
    }

    public static void getNetConfig(Vector netInterfaces, Vector ips) throws IOException, ApMonException {
        String line;
        Parser parser = new Parser();
        cmdExec exec = new cmdExec();

        String output = exec.executeCommandReality("/sbin/ifconfig -a", "");
        if (exec.isError())
            output = null;
        exec.stopIt();

        String crtIfaceName = null;
        if (output != null && !output.equals("")) {
            parser.parse(output);
            line = parser.nextLine();
            while (line != null) {
                if (line == null)
                    break;
                StringTokenizer lst = new StringTokenizer(line, " :\t");
                if (line.startsWith(" ") || line.startsWith("\t")) {
                    if (line.indexOf("inet") < 0) {
                        line = parser.nextLine();
                        continue;
                    }

                    lst.nextToken();
                    lst.nextToken();
                    String addr_t = lst.nextToken();
                    if (!addr_t.equals("127.0.0.1")) {
                        ips.add(addr_t);
                        netInterfaces.add(crtIfaceName);
                    }
                } else {
                    // get the name
                    String netName = lst.nextToken();
                    if (netName != null && !netName.startsWith("lo") && netName.indexOf("eth") != -1) {
                        crtIfaceName = new String(netName);
                    }
                }

                line = parser.nextLine();
            }
        }

    }

}
