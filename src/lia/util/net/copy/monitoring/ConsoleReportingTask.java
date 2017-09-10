/*
 * $Id$
 */
package lia.util.net.copy.monitoring;

import lia.util.net.common.Utils;
import lia.util.net.copy.FDTSession;
import lia.util.net.copy.disk.DiskReaderManager;
import lia.util.net.copy.disk.DiskWriterManager;
import lia.util.net.copy.monitoring.base.AbstractAccountableMonitoringTask;
import lia.util.net.copy.transport.TCPTransportProvider;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class is the only class which should report to the stdout
 *
 * @author ramiro
 */
public class ConsoleReportingTask extends AbstractAccountableMonitoringTask {

    private static final Logger logger = Logger.getLogger(ConsoleReportingTask.class.getName());

    private static final DiskWriterManager diskWriterManager = DiskWriterManager.getInstance();

    private static final DiskReaderManager diskReaderManager = DiskReaderManager.getInstance();
    private static final ConsoleReportingTask thisInstace = new ConsoleReportingTask();
    // private final DateFormat dateFormat = new SimpleDateFormat("dd/MM/yy HH:mm:ss");
    private final DateFormat dateFormat = new SimpleDateFormat("dd/MM HH:mm:ss");
    private final Set<FDTSession> oldReaderSessions = new TreeSet<FDTSession>();
    private final Set<FDTSession> oldWriterSessions = new TreeSet<FDTSession>();
    private final boolean customLog;

    private ConsoleReportingTask() {
        super(null);
        if (logger.isLoggable(Level.FINER)) {
            logger.log(Level.FINER, "\n\n  [ ConsoleReportingTask ] initiated !!!! \n\n");
        }
        customLog = Utils.isCustomLog();
    }

    public static final ConsoleReportingTask getInstance() {
        return thisInstace;
    }

    private final boolean reportStatus(final Set<FDTSession> currentSessionSet, final Set<FDTSession> oldSessionSet,
                                       final String tag, final StringBuilder sb) {
        boolean shouldReport = false;

        if (oldSessionSet.size() > 0) {
            double totalReadRate = 0;
            boolean reportMultipleSessions = false;

            if (oldSessionSet.size() > 1) {
                reportMultipleSessions = true;
                sb.append(oldSessionSet.size()).append(" active sessions:");
            }

            for (Iterator<FDTSession> it = oldSessionSet.iterator(); it.hasNext(); ) {
                final FDTSession fdtSession = it.next();
                final TCPTransportProvider tcpTransportProvider = fdtSession.getTransportProvider();

                if (!currentSessionSet.contains(fdtSession)) {
                    if (tcpTransportProvider == null) {
                        // this is real big .... BUG?????
                        logger.log(Level.WARNING,
                                " [ ConsoleReportingTask ] The session: " + fdtSession
                                        .sessionID() + " is no longer "
                                        + "available, but canot remove trasport provider from monitoring queue. It's probably a BUG in FDT");
                        continue;
                    }
                    if (logger.isLoggable(Level.FINE)) {
                        logger.log(Level.FINE, " [ ConsoleReportingTask ]  Removing tcpTransportProvider "
                                + tcpTransportProvider + " for session: " + fdtSession.sessionID());
                    }
                    remove(tcpTransportProvider);
                    it.remove();
                    continue;
                }

                if (tcpTransportProvider != null) {
                    if (getMonCount(tcpTransportProvider) == 0)
                        continue;

                    final double totalRate = getTotalRate(tcpTransportProvider);
                    final double avgTotalRate = getAvgTotalRate(tcpTransportProvider);

                    shouldReport = true;
                    totalReadRate += totalRate;

                    if (reportMultipleSessions) {
                        sb.append("\n").append(fdtSession.sessionID());
                    }
                    sb.append(tag).append(Utils.formatWithBitFactor(8 * totalRate, 0, "/s")).append("\tAvg: ")
                            .append(Utils.formatWithBitFactor(8 * avgTotalRate, 0, "/s"));

                    final long dtMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - fdtSession.startTimeNanos);
                    if (fdtSession.getSize() > 0 && dtMillis > 20 * 1000) {
                        final long tcpSize = tcpTransportProvider.getUtilBytes();
                        final double cSize = (tcpSize <= 0L) ? 0D : tcpSize;
                        if (cSize > 0) {
                            final double tSize = (fdtSession.getSize() <= 0L) ? 0D : fdtSession.getSize();
                            sb.append(" ").append(Utils.percentDecimalFormat((cSize * 100) / tSize)).append("%");
                            final double remainingSeconds = (fdtSession.getSize() - cSize) / avgTotalRate;
                            sb.append(" ( ").append(Utils.getETA((long) remainingSeconds)).append(" )");
                        }
                    }
                }
            } // for all old sessions

            if (reportMultipleSessions) {
                // get it in bits/s from bytes/s
                totalReadRate *= 8;
                sb.append("\nTotal ").append(tag).append(Utils.formatWithBitFactor(totalReadRate, 0, "/s"));
            }
        }

        // add all new sessions
        for (final FDTSession fdtSession : currentSessionSet) {
            if (!oldSessionSet.contains(fdtSession)) {
                final TCPTransportProvider tcpTransportProvider = fdtSession.getTransportProvider();
                if (tcpTransportProvider != null) {
                    if (addIfAbsent(tcpTransportProvider, logger.isLoggable(Level.FINER) ? true : false)) {
                        if (logger.isLoggable(Level.FINE)) {
                            logger.log(Level.FINE, " [ ConsoleReportingTask ]  Adding tcpTransportProvider "
                                    + tcpTransportProvider + " for session: " + fdtSession.sessionID());
                        }
                    } else {
                        if (logger.isLoggable(Level.FINE)) {
                            logger.log(Level.FINE, " [ ConsoleReportingTask ]  Unable to add tcpTransportProvider "
                                    + tcpTransportProvider + " for session: " + fdtSession.sessionID());
                        }
                    }

                    oldSessionSet.add(fdtSession);
                }
            }
        }

        return shouldReport;
    }

    private void reportStatus() {
        StringBuilder sb = new StringBuilder(8192);
        sb.append(dateFormat.format(new Date())).append("\t");

        boolean shouldReport = (reportStatus(diskWriterManager.getSessions(), oldWriterSessions, "Net In: ", sb)
                || reportStatus(diskReaderManager.getSessions(), oldReaderSessions, "Net Out: ", sb));

        if (shouldReport) {
            logger.info(sb.toString());
            System.out.println(sb.toString());
        }

    }

    @Override
    public void rateComputed() {
        try {
            reportStatus();
        } catch (Throwable t1) {
            logger.log(Level.WARNING, " [ ConsoleReportingTask ] Got exception while reporting", t1);
        }
    }

}
