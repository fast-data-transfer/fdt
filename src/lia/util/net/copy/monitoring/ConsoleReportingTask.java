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

import java.net.SocketException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;


/// Libraries for writing rate monitor to a file
import java.io.*;
import lia.util.net.copy.*;

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
    private final DateFormat dateFormat = new SimpleDateFormat("dd/MM/yy\tHH:mm:ss");
    // private final DateFormat dateFormat = new SimpleDateFormat("dd/MM HH:mm:ss");
    private final Set<FDTSession> oldReaderSessions = new TreeSet<FDTSession>();
    private final Set<FDTSession> oldWriterSessions = new TreeSet<FDTSession>();
    private final boolean customLog;
    
    //StringBufferf for transfer rate bufferedWriter
    //private StringBuffer sbf = new StringBuffer();
    private int writeRateToFileCount = 0;
    
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

    private final boolean reportStatus(final Set<FDTSession> currentSessionSet, final Set<FDTSession> oldSessionSet, final String tag, final StringBuilder sb, final String buffTag) {
       
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
                                " [ ConsoleReportingTask ] The session: " + fdtSession.sessionID() + " is no longer available, but canot remove trasport provider from monitoring queue. It's probably a BUG in FDT");
                        continue;
                    }
                    if (logger.isLoggable(Level.FINE)) {
                    	logger.log(Level.FINE, " [ ConsoleReportingTask ]  Removing tcpTransportProvider " + tcpTransportProvider + " for session: " + fdtSession.sessionID());
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
                        sb.append("\n");
                        sb.append(fdtSession.sessionID());
                    }
                    	sb.append(tag);
                    	sb.append(Utils.formatWithBitFactor(8 * totalRate, 0, "/s"));
                    	sb.append("\tAvg: ");
                        sb.append(Utils.formatWithBitFactor(8 * avgTotalRate, 0, "/s"));

                    final long dtMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - fdtSession.startTimeNanos);
                    if (fdtSession.getSize() > 0 && dtMillis > 20 * 1000) {
                        final long tcpSize = tcpTransportProvider.getUtilBytes();
                        final double cSize = (tcpSize <= 0L) ? 0D : tcpSize;
                        if (cSize > 0) {
                            final double tSize = (fdtSession.getSize() <= 0L) ? 0D : fdtSession.getSize();
                            sb.append("\t");
                            sb.append(Utils.percentDecimalFormat((cSize * 100) / tSize));
                            sb.append("%");
                            sb.append("\t");
                            
                            final double remainingSeconds = (fdtSession.getSize() - cSize) / avgTotalRate;
                            sb.append(" ( ");
                            sb.append(Utils.getETA((long) remainingSeconds));
                            sb.append(" )");
                                                    
                            sb.append("\t");
                            sb.append(buffTag);
                            try {
                            	if (buffTag=="SO_SNDBUF: ") {
                					sb.append(tcpTransportProvider.getSNDBUFSize());
                				} else sb.append("null");	//Currently don't support SO_RCVBUF yet
                            } catch (SocketException e) {
                            	e.printStackTrace();
                            }
                            
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
                            logger.log(Level.FINE, " [ ConsoleReportingTask ]  Adding tcpTransportProvider " + tcpTransportProvider + " for session: " + fdtSession.sessionID());
                        }
                    } else {
                        if (logger.isLoggable(Level.FINE)) {
                            logger.log(Level.FINE, " [ ConsoleReportingTask ]  Unable to add tcpTransportProvider " + tcpTransportProvider + " for session: " + fdtSession.sessionID());
                        }
                    }

                    oldSessionSet.add(fdtSession);
                }
            }
        }

        return shouldReport;
    }

    private void reportStatus() throws IOException {
        StringBuilder sb = new StringBuilder(8192);
        
        boolean shouldReport = (reportStatus(diskWriterManager.getSessions(), oldWriterSessions, "Net In: ", sb, "SO_RCVBUF: ") || reportStatus(diskReaderManager.getSessions(), oldReaderSessions, "Net Out: ", sb, "SO_SNDBUF: "));

        if (shouldReport) {
            logger.info(sb.toString());
        }
        
        //Parse StringBuilder sb as argument for writeRateToFile Method
        writeRateToFile(sb.toString());

    }
    
    @Override
    public void rateComputed() {
        try {
            reportStatus();
        } catch (Throwable t1) {
            logger.log(Level.WARNING, " [ ConsoleReportingTask ] Got exception while reporting", t1);
        }
    }
    
    //Take the Net_IN/Net_OUT log and write to a separate text file
    private void writeRateToFile(String terminalRateLog) throws IOException{
    
    	String newRateLog = terminalRateLog;
    	String buffTag, netTag;
    	if(newRateLog.contains("Net In")) {
    		buffTag = "SO_RCVBUF:";
    		netTag = "NET_IN";
    	} else {
    		buffTag = "SO_SNDBUF:";
    		netTag = "NET_OUT";
    	}  	
    	/**
    	 * Original format:
    	 * 
    	 * Net {In/Out}: X.XXX {Gb/s|Mb/s}\tAvg: Y.YYY {Gb/s|Mb/s}\tZZ.ZZ% ( RemainingTime )\t{SO_SNDBUF|SO_RCVBUF} Size: AAAAAAA 
    	 * 
    	 * Target format:
    	 * X.XXX\tY.YYY\tZZ.ZZ\tRemainingTime\tAAAAAA
    	 * 
    	 * **/
    	if(newRateLog.contains("Net Out: ")) {
    		newRateLog = newRateLog.replaceAll("Net Out: ", "");
		newRateLog = newRateLog.replaceAll("SO_SNDBUF: ", "");
    	} else {
    		newRateLog = newRateLog.replaceAll("Net In: ", "");
		newRateLog = newRateLog.replaceAll("SO_RCVBUF: ", "");
    	}
    	
    	if(newRateLog.contains(" Gb/s")) {
    		newRateLog = newRateLog.replaceAll(" Gb/s", "");
    	} else if (newRateLog.contains(" Mb/s")) {
    		newRateLog = newRateLog.replaceAll(" Mb/s", "");
    	} else  newRateLog = newRateLog.replaceAll(" Kb/s", "");
    	
    	newRateLog = newRateLog.replaceAll("Avg: ", "");
		newRateLog = newRateLog.replaceAll("%", "");
		newRateLog = newRateLog.replaceAll("\\( ","");
		newRateLog = newRateLog.replaceAll(" \\)", "");
    	
    	
    	if (writeRateToFileCount>0) {
    		//After the file was already rreated, just append the new log the to the old file
    		
    		BufferedWriter bwr = new BufferedWriter(new FileWriter("/tmp/transfer_rate.txt", true)); //add true argument for append mode
    		//BufferedWriter bwr = new BufferedWriter(new FileWriter("/tmp/transfer_rate.txt", true)); //add true argument for append mode
    		
    		bwr.newLine();
    		bwr.write(writeRateToFileCount + "\t");
    		bwr.write(dateFormat.format(new Date()) + "\t");
    		bwr.write(newRateLog);
    		bwr.close();
    		writeRateToFileCount++;	
    	} else {
    		//BufferedWriter for writing the rate to .txt file
    		//Create new file first when the this method is called for the first time
            	
    		BufferedWriter bwr = new BufferedWriter(new FileWriter(new File("/tmp/transfer_rate.txt")));
    		//BufferedWriter bwr = new BufferedWriter(new FileWriter("/tmp/transfer_rate.txt", true)); //add true argument for append mode
    		
    		/**
    		 * Column Header for the .txt file separated with tab \t
    		 * 
    		 * [NO]\t[DATE]\t[TIME]\t{NET_IN|NET_OUT}\t[AVG]\t[PERCENT COMPLETED]\t[TIME REMAINING]\t[SO_SNDBUF]
    		 * **/
    		bwr.write("NO\tDATE\tTIME\t" + netTag +"\tAVG\tPERCENT COMPLETED\tTIME REMAINING\t" + buffTag);		
    	
    		bwr.write(writeRateToFileCount + "\t");
    		bwr.write(dateFormat.format(new Date()) + "\t");
    		bwr.write(newRateLog);
    		bwr.close();
    		writeRateToFileCount++;	
    	}
    }

}
