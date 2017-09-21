/*
 * $Id$
 */
package lia.util.net.copy.monitoring;

import lia.util.net.common.Utils;
import lia.util.net.copy.FDTSession;
import lia.util.net.copy.disk.DiskReaderManager;
import lia.util.net.copy.disk.DiskWriterManager;
import lia.util.net.copy.transport.TCPTransportProvider;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Abstract implementation for internal monitoring and reporting
 *
 * @author ramiro
 */
public abstract class FDTReportingTask implements Runnable {

    protected static final DiskReaderManager diskReaderManager = DiskReaderManager.getInstance();
    protected static final DiskWriterManager diskWriterManager = DiskWriterManager.getInstance();
    private static final Logger logger = Logger.getLogger("lia.util.net.copy.monitoring.FDTReportingTask");

    public abstract void finishFDTSession(final FDTSession fdtSession);

    public abstract void startFDTSession(final FDTSession fdtSession);

    public HashMap<String, HashMap<String, Double>> getReaderParams() {
        final HashMap<String, HashMap<String, Double>> monitoringParams = new HashMap<String, HashMap<String, Double>>();

        FDTSession fdtSession = null;
        Iterator<FDTSession> it = null;

        double rate = 0;

        final Set<FDTSession> fdtSessions = diskReaderManager.getSessions();
        //Client monitoring - DiskReaderManager
        it = fdtSessions.iterator();

        if (logger.isLoggable(Level.FINEST)) {
            logger.log(Level.FINEST, "FDTReportingTask - ReaderSessions: " + fdtSessions);
        }

        while (it.hasNext()) {
            fdtSession = it.next();
            TCPTransportProvider transportProvider = fdtSession.getTransportProvider();
            HashMap<String, Double> fdtSessionParams = new HashMap<String, Double>();

            if (transportProvider != null && transportProvider.monitoringTask != null) {
                rate = (transportProvider.monitoringTask.getTotalRate() * 8D) / Utils.MEGA_BIT;
                fdtSessionParams.put("NET_OUT_Mb", rate);
            }

            final FDTSessionMonitoringTask fdtSessionMTask = fdtSession.getMonitoringTask();
            if (fdtSessionMTask != null) {
                rate = fdtSessionMTask.getTotalRate() / Utils.MEGA_BYTE;
                fdtSessionParams.put("DISK_READ_MB", rate);
                final double tSize = (fdtSession.getSize() <= 0L) ? 0D : (fdtSession.getSize() / (double) Utils.MEGA_BYTE);
                final double cSize = (fdtSession.getTotalBytes() <= 0L) ? 0D : (fdtSession.getTotalBytes() / (double) Utils.MEGA_BYTE);
                fdtSessionParams.put("TotalMBytes", tSize);
                fdtSessionParams.put("TransferredMBytes", cSize);
                if (fdtSession.getSize() > 0L) {
                    fdtSessionParams.put("TransferRatio", (cSize * 100) / tSize);
                }
            } else {
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, "No FDTSessionMonitoringTask started for fdtSession: " + fdtSession);
                }
            }

            final String monID = fdtSession.getMonID();
            if (monID != null) {
                monitoringParams.put(monID, fdtSessionParams);
            } else {
                monitoringParams.put(fdtSession.getRemoteAddress().getHostAddress() + ":" + fdtSession.getRemotePort(), fdtSessionParams);
            }
        }

        if (logger.isLoggable(Level.FINEST)) {
            logger.log(Level.FINEST, "FDTReportingTask - returning ReaderParams: " + monitoringParams);
        }

        return monitoringParams;
    }

    public HashMap<String, HashMap<String, Double>> getWriterParams() {
        final HashMap<String, HashMap<String, Double>> monitoringParams = new HashMap<String, HashMap<String, Double>>();

        FDTSession fdtSession = null;
        Iterator<FDTSession> it = null;

        //Server monitoring - DiskWriterManager
        it = diskWriterManager.getSessions().iterator();

        double rate = 0;

        while (it.hasNext()) {
            fdtSession = it.next();
            TCPTransportProvider transportProvider = fdtSession.getTransportProvider();
            HashMap<String, Double> fdtSessionParams = new HashMap<String, Double>();

            if (transportProvider != null && transportProvider.monitoringTask != null) {
                rate = (transportProvider.monitoringTask.getTotalRate() * 8) / Utils.MEGA_BIT;
                fdtSessionParams.put("NET_IN_Mb", rate);
            }

            final FDTSessionMonitoringTask fdtSessionMTask = fdtSession.getMonitoringTask();
            if (fdtSessionMTask != null) {
                rate = fdtSessionMTask.getTotalRate() / Utils.MEGA_BYTE;
                fdtSessionParams.put("DISK_WRITE_MB", rate);
                final double tSize = (fdtSession.getSize() <= 0L) ? 0D : (fdtSession.getSize() / (double) Utils.MEGA_BYTE);
                final double cSize = (fdtSession.getTotalBytes() <= 0L) ? 0D : (fdtSession.getTotalBytes() / (double) Utils.MEGA_BYTE);
                fdtSessionParams.put("TotalMBytes", tSize);
                fdtSessionParams.put("TransferredMBytes", cSize);
                if (fdtSession.getSize() > 0L) {
                    fdtSessionParams.put("TransferRatio", (cSize * 100) / tSize);
                }
            } else {
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, "No FDTSessionMonitoringTask started for fdtSession: " + fdtSession);
                }
            }

            final String monID = fdtSession.getMonID();
            if (monID != null) {
                monitoringParams.put(monID, fdtSessionParams);
            } else {
                monitoringParams.put(fdtSession.getRemoteAddress().getHostAddress() + ":" + fdtSession.getRemotePort(), fdtSessionParams);
            }

        }

        return monitoringParams;
    }
}
