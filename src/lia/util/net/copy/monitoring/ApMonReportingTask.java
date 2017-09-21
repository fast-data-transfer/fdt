/*
 * $Id$
 */
package lia.util.net.copy.monitoring;

import apmon.ApMon;
import lia.util.net.common.Config;
import lia.util.net.common.Utils;
import lia.util.net.copy.FDTReaderSession;
import lia.util.net.copy.FDTSession;
import lia.util.net.copy.FDTWriterSession;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Simple implementation for a {@link FDTReportingTask} which is used to send data
 * through <code>ApMon</code>
 *
 * @author ramiro
 */
public class ApMonReportingTask extends FDTReportingTask {

    private static final Logger logger = Logger.getLogger("lia.util.net.copy.monitoring.ApMonReportingTask");

    private static final ApMon apMon;

    static {
        ApMon apMonInstace = null;
        try {
            System.out.println("Starting ApMonReportingTask ...");
            apMonInstace = Utils.getApMon();
            System.out.println("ApMonReportingTask started!");
        } catch (Throwable t) {
            logger.log(Level.WARNING, "Got exception getting apmon instance", t);
        }

        apMon = apMonInstace;
    }

    private static final void sendParams(final HashMap<String, HashMap<String, Double>> paramsToSend, final String clusterName) throws Exception {
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, " Sending to ApMonReportingTask :- ClusterName: " + clusterName + " Params: " + paramsToSend);
        }

        if (paramsToSend.size() > 0) {

            for (Map.Entry<String, HashMap<String, Double>> entry : paramsToSend.entrySet()) {
                HashMap<String, Double> hToSend = entry.getValue();
                Vector<Double> paramValues = null;
                Vector<String> paramNames = null;
                Vector<Integer> paramTypes = null;
                if (hToSend.size() > 0) {
                    paramValues = new Vector<Double>(hToSend.size());
                    paramNames = new Vector<String>(hToSend.size());
                    paramTypes = new Vector<Integer>(hToSend.size());

                    for (Map.Entry<String, Double> pEntry : hToSend.entrySet()) {
                        paramValues.add(pEntry.getValue());
                        paramNames.add(pEntry.getKey());
                        paramTypes.add(ApMon.XDR_REAL64);
                    }
                }

                if (paramValues != null) {
                    apMon.sendParameters(clusterName, entry.getKey(), paramValues.size(), paramNames, paramTypes, paramValues);
                }
            }
        }
    }

    private void publisStartFinishParams(final FDTSession fdtSession) {
        if (fdtSession != null) {
            try {
                final HashMap<String, HashMap<String, Double>> paramsToSend = new HashMap<String, HashMap<String, Double>>();
                final HashMap<String, Double> fdtSessionParams = new HashMap<String, Double>();
                final FDTSessionMonitoringTask fdtSessionMTask = fdtSession.getMonitoringTask();
                String apMonClusterName = "N/A";

                if (fdtSessionMTask != null) {
                    if (fdtSession instanceof FDTWriterSession) {
                        final double rate = fdtSessionMTask.getTotalRate() / Utils.MEGA_BYTE;
                        fdtSessionParams.put("DISK_WRITE_MB", rate);
                        final double tSize = fdtSession.getSize() / (double) Utils.MEGA_BYTE;
                        final double cSize = fdtSession.getTotalBytes() / (double) Utils.MEGA_BYTE;
                        fdtSessionParams.put("TotalMBytes", tSize);
                        fdtSessionParams.put("TransferredMBytes", cSize);
                        fdtSessionParams.put("Status", (double) fdtSession.getCurrentStatus());

                        if (fdtSession.getSize() != 0) {
                            fdtSessionParams.put("TransferRatio", (cSize * 100) / tSize);
                        }

                        final String monID = fdtSession.getMonID();
                        if (monID != null) {
                            paramsToSend.put(monID, fdtSessionParams);
                        } else {
                            paramsToSend.put(fdtSession.getRemoteAddress().getHostAddress() + ":" + fdtSession.getRemotePort(), fdtSessionParams);
                        }
                        apMonClusterName = "Readers";

                    } else if (fdtSession instanceof FDTReaderSession) {
                        final double rate = fdtSessionMTask.getTotalRate() / Utils.MEGA_BYTE;
                        fdtSessionParams.put("DISK_READ_MB", rate);
                        final double tSize = fdtSession.getSize() / (double) Utils.MEGA_BYTE;
                        final double cSize = fdtSession.getTotalBytes() / (double) Utils.MEGA_BYTE;
                        fdtSessionParams.put("TotalMBytes", tSize);
                        fdtSessionParams.put("TransferredMBytes", cSize);
                        if (fdtSession.getSize() != 0) {
                            fdtSessionParams.put("TransferRatio", (cSize * 100) / tSize);
                        }
                        fdtSessionParams.put("Status", (double) fdtSession.getCurrentStatus());

                        final String monID = fdtSession.getMonID();
                        if (monID != null) {
                            paramsToSend.put(monID, fdtSessionParams);
                        } else {
                            paramsToSend.put(fdtSession.getRemoteAddress().getHostAddress() + ":" + fdtSession.getRemotePort(), fdtSessionParams);
                        }

                        apMonClusterName = "Writers";

                    } else {
                        logger.log(Level.WARNING, "[ERROR] FDT Session is not an \"instanceof\" FDTWriterSession or FDTReaderSession!!!");
                        return;
                    }

                    if (paramsToSend.size() > 0) {

                        if (logger.isLoggable(Level.FINE)) {
                            logger.log(Level.FINE, " Sending to ApMonReportingTask :- " + apMonClusterName + " Params: " + paramsToSend);
                        }
                        for (Map.Entry<String, HashMap<String, Double>> entry : paramsToSend.entrySet()) {
                            HashMap<String, Double> hToSend = entry.getValue();
                            Vector<Double> paramValues = null;
                            Vector<String> paramNames = null;
                            Vector<Integer> paramTypes = null;
                            if (hToSend.size() > 0) {
                                paramValues = new Vector<Double>(hToSend.size());
                                paramNames = new Vector<String>(hToSend.size());
                                paramTypes = new Vector<Integer>(hToSend.size());

                                for (Map.Entry<String, Double> pEntry : hToSend.entrySet()) {
                                    paramValues.add(pEntry.getValue());
                                    paramNames.add(pEntry.getKey());
                                    paramTypes.add(ApMon.XDR_REAL64);
                                }
                            }

                            if (paramValues != null) {
                                apMon.sendParameters(apMonClusterName, entry.getKey(), paramValues.size(), paramNames, paramTypes, paramValues);
                            }
                        }
                    }

                } else {
                    logger.log(Level.WARNING, "[ERROR] FDTSessionMonitoringTask is null in finishFDTSession(fdtSession)!!!");
                }

            } catch (Throwable t) {
                logger.log(Level.WARNING, "Got expcetion notifying last params for " + fdtSession.sessionID(), t);
            }
        } else {
            logger.log(Level.WARNING, "[ERROR] FDT Session is null in finishFDTSession(fdtSession)!!!");
        }
    }

    public void startFDTSession(final FDTSession fdtSession) {
        publisStartFinishParams(fdtSession);
    }

    public void finishFDTSession(final FDTSession fdtSession) {
        publisStartFinishParams(fdtSession);
    }

    public void run() {
        try {

            if (logger.isLoggable(Level.FINEST)) {
                logger.log(Level.FINEST, "ApMonReportingTask entering run()");
            }

            HashMap<String, HashMap<String, Double>> paramsToSend = getReaderParams();

            if (paramsToSend.size() > 0) {
                sendParams(paramsToSend, "Readers");
            }

            paramsToSend = getWriterParams();

            double totalNet = 0;
            double totalDisk = 0;

            if (paramsToSend.size() > 0) {

                for (Map.Entry<String, HashMap<String, Double>> entry : paramsToSend.entrySet()) {
                    HashMap<String, Double> hToSend = entry.getValue();
                    if (hToSend.size() > 0) {
                        Double dToAdd = hToSend.get("NET_IN_Mb");
                        if (dToAdd != null) {
                            totalNet += dToAdd;
                        }

                        dToAdd = hToSend.get("DISK_WRITE_MB");
                        if (dToAdd != null) {
                            totalDisk += dToAdd;
                        }
                    }
                }
            }

            if (paramsToSend.size() > 0) {
                sendParams(paramsToSend, "Writers");
            }

            if (Config.getInstance().getHostName() == null) {
                HashMap<String, Double> localParams = new HashMap<String, Double>();
                localParams.put("CLIENTS_NO", (double) paramsToSend.size());
                localParams.put("DISK_WRITE_MB", totalDisk);
                localParams.put("NET_IN_Mb", totalNet);

//                lisaMon.sendServerParameters("FDT_PARAMS", localParams);
            }

//            HashMap<String, Double> fdtLisaParams = FDTInternalMonitoringTask.getInstance().getLisaParams();            
//            
//            String key = "FDT_MON:";
//            if(Config.getInstance().getHostName() == null) {
//                key += Config.getInstance().getPort();
//            } else {
//                String rPort = "";
//                it = diskReaderManager.getSessions().iterator();
//                while(it.hasNext()) {
//                    fdtSession = it.next();
//                    rPort += fdtSession.getLocalPort();
//                }
//
//                it = diskWriterManager.getSessions().iterator();
//                while(it.hasNext()) {
//                    fdtSession = it.next();
//                    rPort += fdtSession.getLocalPort();
//                }
//                
//                if(rPort.length() == 0) {
//                    rPort = "UNK";
//                }
//                
//                key += rPort;
//            }
//
//            if(logger.isLoggable(Level.FINER)) {
//                logger.log(Level.FINER, "FDT Params: " + fdtLisaParams + " Key: " + key);
//            }
//            
//            lisaMon.sendServerParameters(key, fdtLisaParams);

        } catch (Throwable t) {
            logger.log(Level.INFO, " LISAReportingTask got exception:", t);
        }

    }

}
