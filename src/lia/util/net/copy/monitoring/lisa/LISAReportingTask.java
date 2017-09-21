/*
 * $Id$
 */
package lia.util.net.copy.monitoring.lisa;

import lia.util.net.common.Config;
import lia.util.net.common.Utils;
import lia.util.net.copy.FDTReaderSession;
import lia.util.net.copy.FDTSession;
import lia.util.net.copy.FDTWriterSession;
import lia.util.net.copy.monitoring.FDTReportingTask;
import lia.util.net.copy.monitoring.FDTSessionMonitoringTask;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class is used to send the internal monitoring informations back to LISA/MonALISA
 * over an (XDR) Socket
 *
 * @author ramiro
 */
public class LISAReportingTask extends FDTReportingTask {

    private static final Logger logger = Logger.getLogger(LISAReportingTask.class.getName());
    private static LISAReportingTask _thisInstance;
    private final String lisaHost;
    private final int lisaPort;
    volatile public MonClient lisaMon;
    private boolean errorReported = false;

    private LISAReportingTask(String lisaHost, int lisaPort) {
        this.lisaHost = lisaHost;
        this.lisaPort = lisaPort;
        setupMonClient();
    }

    public static final LISAReportingTask initInstance(String lisaHost, int lisaPort) {
        synchronized (LISAReportingTask.class) {
            if (_thisInstance == null) {
                _thisInstance = new LISAReportingTask(lisaHost, lisaPort);
                LISAReportingTask.class.notifyAll();
            }
        }
        return _thisInstance;
    }

    public static final LISAReportingTask getInstanceNow() {
        synchronized (LISAReportingTask.class) {
            return _thisInstance;
        }
    }

    public static final LISAReportingTask getInstance() {
        synchronized (LISAReportingTask.class) {
            while (_thisInstance == null) {
                try {
                    LISAReportingTask.class.wait(5000);
                    logger.log(Level.WARNING, " getInstace timeout on LISAReporting task ... ");
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
            return _thisInstance;
        }
    }

    private void publishStartFinishParams(final FDTSession fdtSession) {
        if (fdtSession != null) {
            try {
                final HashMap<String, HashMap<String, Double>> lisaParams = new HashMap<String, HashMap<String, Double>>();
                final HashMap<String, Double> fdtSessionParams = new HashMap<String, Double>();
                final FDTSessionMonitoringTask fdtSessionMTask = fdtSession.getMonitoringTask();

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
                            lisaParams.put(monID, fdtSessionParams);
                        } else {
                            lisaParams.put(fdtSession.getRemoteAddress().getHostAddress() + ":" + fdtSession.getRemotePort(), fdtSessionParams);
                        }

                        if (lisaParams.size() > 0) {
                            if (logger.isLoggable(Level.FINE)) {
                                logger.log(Level.FINE, " Sending to LISA :- Client Params: " + lisaParams);
                            }
                            for (Map.Entry<String, HashMap<String, Double>> entry : lisaParams.entrySet()) {
                                HashMap<String, Double> hToSend = entry.getValue();
                                if (hToSend.size() > 0) {
                                    lisaMon.sendServerParameters(entry.getKey(), hToSend);
                                }
                            }
                        }

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
                            lisaParams.put(monID, fdtSessionParams);
                        } else {
                            lisaParams.put(fdtSession.getRemoteAddress().getHostAddress() + ":" + fdtSession.getRemotePort(), fdtSessionParams);
                        }

                        if (lisaParams.size() > 0) {
                            if (logger.isLoggable(Level.FINE)) {
                                logger.log(Level.FINE, " Sending to LISA :- Server Params: " + lisaParams);
                            }
                            for (Map.Entry<String, HashMap<String, Double>> entry : lisaParams.entrySet()) {
                                HashMap<String, Double> hToSend = entry.getValue();
                                if (hToSend.size() > 0) {
                                    lisaMon.sendClientParameters(entry.getKey(), hToSend);
                                }
                            }
                        }

                    } else {
                        logger.log(Level.WARNING, "[ERROR] FDT Session is not an \"instanceof\" FDTWriterSession or FDTReaderSession!!!");
                        return;
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
        publishStartFinishParams(fdtSession);
    }

    public void finishFDTSession(final FDTSession fdtSession) {
        publishStartFinishParams(fdtSession);
    }

    private void setupMonClient() {
        try {
            lisaMon = new MonClient(lisaHost, lisaPort);
        } catch (Throwable t) {
            if (!errorReported) {
                logger.log(Level.WARNING, " Cannot connect to lisa", t);
            } else {
                logger.log(Level.FINER, " Cannot connect to lisa", t);
            }
            errorReported = true;
        }
    }

    public void run() {
        try {
            if (lisaMon == null) {
                setupMonClient();
            }

            if (lisaMon == null) return;

            errorReported = false;

            HashMap<String, HashMap<String, Double>> lisaParams = getReaderParams();

            if (lisaParams.size() > 0) {
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, " Sending to LISA :- Client Params: " + lisaParams);
                }
                for (Map.Entry<String, HashMap<String, Double>> entry : lisaParams.entrySet()) {
                    HashMap<String, Double> hToSend = entry.getValue();
                    if (hToSend.size() > 0) {
                        lisaMon.sendClientParameters(entry.getKey(), hToSend);
                    }
                }
            }

            lisaParams = getWriterParams();

            double totalNet = 0;
            double totalDisk = 0;

            if (lisaParams.size() > 0) {
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, " Sending to LISA :- Server Params: " + lisaParams);
                }
                for (Map.Entry<String, HashMap<String, Double>> entry : lisaParams.entrySet()) {
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

                        lisaMon.sendServerParameters(entry.getKey(), hToSend);
                    }
                }
            }

            if (Config.getInstance().getHostName() == null) {
                HashMap<String, Double> localParams = new HashMap<String, Double>();
                localParams.put("CLIENTS_NO", (double) lisaParams.size());
                localParams.put("DISK_WRITE_MB", totalDisk);
                localParams.put("NET_IN_Mb", totalNet);

                lisaMon.sendServerParameters("FDT_PARAMS", localParams);
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

    public void sendClientNow(String key, HashMap<String, Double> params) throws Exception {
        lisaMon.sendClientParameters(key, params);
    }

    public void sendServerNow(String key, HashMap<String, Double> params) throws Exception {
        lisaMon.sendServerParameters(key, params);
    }

}
