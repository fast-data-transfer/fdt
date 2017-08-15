/*
 * $Id: ClientSessionManager.java 643 2011-02-13 15:00:30Z catac $
 */
package lia.util.net.copy.gui;

import java.lang.reflect.Field;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.concurrent.RunnableScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import lia.util.net.common.Config;
import lia.util.net.common.HeaderBufferPool;
import lia.util.net.common.Utils;
import lia.util.net.copy.FDTSession;
import lia.util.net.copy.FDTSessionManager;
import lia.util.net.copy.monitoring.ConsoleReportingTask;
import lia.util.net.copy.monitoring.FDTInternalMonitoringTask;
import lia.util.net.copy.monitoring.FDTSessionMonitoringTask;
import lia.util.net.copy.transport.TCPTransportProvider;

/**
 * @author Ciprian Dobre
 */
public class ClientSessionManager {

	static final transient Logger logger = Logger.getLogger(ClientSessionManager.class.getCanonicalName());

	private FDTSession currentSession;
	private FDTSessionMonitoringTask fdtSessionMTask;
	 
    private static final long KILO_BIT   =   1000;
    private static final long MEGA_BIT   =   KILO_BIT * 1000;
    private static final long GIGA_BIT   =   MEGA_BIT * 1000;
    private static final long TERA_BIT   =   GIGA_BIT * 1000;
    private static final long PETA_BIT   =   TERA_BIT * 1000;

    private RunnableScheduledFuture fdtInternalMonitoringTask = null;
    private RunnableScheduledFuture consoleReporting = null;
    
//    private Runnable progressReporter;
    
	/**
	 * Called in order to initialize a connection with a remote port...
	 * @param host
	 * @param port
	 */
	public String initTransfer(final String host, final int port, final boolean isPullMode, 
			final String[] fileList, final String destDir, final FDTPropsDialog d, final boolean isRecursive) {
		// start by constructing a dummy config
//	    System.out.println(" PullMode = " + isPullMode);
//		this.progressReporter = progressReporter;
		constructConfig(host, port, isPullMode, fileList, destDir, d, isRecursive);
		HeaderBufferPool.initInstance();
		fdtInternalMonitoringTask = (RunnableScheduledFuture)Utils.getMonitoringExecService().scheduleWithFixedDelay(FDTInternalMonitoringTask.getInstance(), 1, 5, TimeUnit.SECONDS);
        consoleReporting = (RunnableScheduledFuture)Utils.getMonitoringExecService().scheduleWithFixedDelay(ConsoleReportingTask.getInstance(), 1, 2, TimeUnit.SECONDS);
		// the session manager will check the "pull/push" mode and start the FDTSession
		try {
			currentSession = FDTSessionManager.getInstance().addFDTClientSession();
			fdtSessionMTask = currentSession.getMonitoringTask();
		} catch (Throwable t) {
			logger.log(Level.WARNING, "Got exception when initiating transfer", t);
			return t.getLocalizedMessage();
		}
		return null;
	}
	
	public FDTSession currentSession() {
		return currentSession;
	}
	
	public void cancelTransfer() {
		if (currentSession == null) return;
        currentSession.close("User pressed cancel", new Exception("User pressed cancel"));
		currentSession = null;
		fdtSessionMTask = null;
	}
	
	public void end() {
		if (fdtInternalMonitoringTask != null) {
			Utils.getMonitoringExecService().remove(fdtInternalMonitoringTask);
			fdtInternalMonitoringTask = null;
		}
		if (consoleReporting != null) {
			Utils.getMonitoringExecService().remove(consoleReporting);
			consoleReporting = null;
		}
		Utils.getMonitoringExecService().purge();
	}
	
	private final static NumberFormat nf = NumberFormat.getInstance();
	static {
		nf.setMaximumFractionDigits(2);
	}
	
	public double transferProgress() {
		if (currentSession == null) {
			return 100.0;
		}
		
        TCPTransportProvider tcpTransportProvider = currentSession.getTransportProvider();
        if (tcpTransportProvider == null) {
            return 0.0;
        }
        
		if (tcpTransportProvider.isClosed()) {
			logger.warning("Transport is closed");
			return 100.0;
		}
        final double tSize = currentSession.getSize();
        
        final long tcpSize = tcpTransportProvider.getUtilBytes();
        final double cSize = (tcpSize <= 0L) ? 0D : tcpSize;
        
        double percent = 100.0;
        try {
        	percent = Math.min((cSize*100.0)/(double)tSize, 100.0);
        } catch (Exception e) { }
        if (!Double.isNaN(percent) && !Double.isInfinite(percent) && percent >= 100.0) {
        	try {
                int state = currentSession.currentState();
        		boolean endRcv = ((state & FDTSession.END_RCV) == FDTSession.END_RCV);
        		boolean endSnt = ((state & FDTSession.END_SENT) == FDTSession.END_SENT);
        		if (((state & FDTSession.TRANSFERING) == FDTSession.TRANSFERING) || (!endRcv && !endSnt)) {
        			return 99.99;
        		}
        	} catch (Throwable t) { 
        		t.printStackTrace();
        	}
        }
        return percent;
	}
	
	public String currentSpeed() {
		if (currentSession == null) return "0.0 b/s";
		try {
			if (currentSession.getTransportProvider() != null && currentSession.getTransportProvider().isClosed()) {
				logger.warning("Transport is closed");
				return "0.0 b/s";
			}
			double rate = currentSession.getTransportProvider().monitoringTask.getTotalRate() * 8;
			return formatNetSpeed(rate, "b/s");
		} catch (Throwable t) {
			return "0.0 b/s";
		}
	}
	
    //do it nicer - TODO make same arrays and use for() ... it's not the 5th grade
	private static final String formatNetSpeed(final double number, final String append) {
		String appendUM;
		double fNo = number;
		
		if(number > PETA_BIT) {
			fNo /= PETA_BIT;
			appendUM = "P" + append;
		} else if(number > TERA_BIT) {
			fNo /= TERA_BIT;
			appendUM = "T" + append;
		} else if(number > GIGA_BIT) {
			fNo /= GIGA_BIT;
			appendUM = "G" + append;
		} else if(number > MEGA_BIT) {
			fNo /= MEGA_BIT;
			appendUM = "M" + append;
		} else if(number > KILO_BIT) {
			fNo /= KILO_BIT;
			appendUM = "K" + append;
		} else {
			appendUM = append;
		}
		
		return nf.format(fNo) + " " + appendUM;
	}

	/**
	 * Constructs a Config object based on provided arguments..
	 */
	private final void constructConfig(final String host, final int port, final boolean isPullMode, 
			final String[] fileList, final String destDir, final FDTPropsDialog d, final boolean isRecursive) {
		// first set the initialized flag on false....
		Class c = Config.class;
		// construct the hashmap
		try {
			Config.initInstance(new HashMap<String, Object>());
		} catch (Throwable t1) {
			t1.printStackTrace();
		}
		Config conf = Config.getInstance();
	    // shall I get the data from server? - used only by the client
		try {
		    conf.setHostName(host);
		} catch (Throwable t) { }
		try {
			Field f = c.getDeclaredField("portNo");
			f.setAccessible(true);
			f.set(conf, port);
		} catch (Throwable t) { }
		try {
		    conf.setPullMode(isPullMode);
		} catch (Throwable t) {
			t.printStackTrace();
		}
		try {
			Field f = c.getDeclaredField("fileList");
			f.setAccessible(true);
			f.set(conf, fileList);
		} catch (Throwable t) { }
		try {
			Field f = c.getDeclaredField("destDir");
			f.setAccessible(true);
			f.set(conf, destDir);
		} catch (Throwable t) { }
		System.out.println("hostname = "+conf.getHostName());
		System.out.println("port = "+conf.getPort());
		String files[] = conf.getFileList();
		for (int i=0; i<files.length; i++)
			System.out.println(files[i]);
		System.out.println("dest = "+conf.getDestinationDir());
		System.out.println("isPull="+conf.isPullMode());
		if (d==null) return;
		try {
			Field f = c.getDeclaredField("sockBufSize");
			f.setAccessible(true);
			f.set(conf, d.getSockBufSize());
		} catch (Throwable t) { }
		try {
			Field f = c.getDeclaredField("sockNum");
			f.setAccessible(true);
			f.set(conf, d.getSockNum());
		} catch (Throwable t) { }
		try {
			Field f = c.getDeclaredField("rateLimit");
			f.setAccessible(true);
			f.set(conf, d.getRateLimit());
		} catch (Throwable t) { }
		try {
			Field f = c.getDeclaredField("readersCount");
			f.setAccessible(true);
			f.set(conf, d.getReadersCount());
		} catch (Throwable t) { }
		try {
			Field f = c.getDeclaredField("writersCount");
			f.setAccessible(true);
			f.set(conf, d.getWritersCount());
		} catch (Throwable t) { }
		try {
			Field f = c.getDeclaredField("maxPartitionsCount");
			f.setAccessible(true);
			f.set(conf, d.getMaxPartitionsCount());
		} catch (Throwable t) { }
		try {
			Field f = c.getDeclaredField("bComputeMD5");
			f.setAccessible(true);
			f.set(conf, d.isBComputeMD5());
		} catch (Throwable t) { }
		try {
			Field f = c.getDeclaredField("bRecursive");
			f.setAccessible(true);
			f.set(conf, isRecursive);
		} catch (Throwable t) { }
		try {
			Field f = c.getDeclaredField("bUseFixedBlocks");
			f.setAccessible(true);
			f.set(conf, d.isBUseFixedBlocks());
		} catch (Throwable t) { }
		try {
			Field f = c.getDeclaredField("transferLimit");
			f.setAccessible(true);
			f.set(conf, d.getTransferLimit());
		} catch (Throwable t) { }
	}
	
} // end of class ClientSessionManager

