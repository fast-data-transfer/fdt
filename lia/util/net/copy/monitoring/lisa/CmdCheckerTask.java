
package lia.util.net.copy.monitoring.lisa;

import java.util.logging.Level;
import java.util.logging.Logger;

import lia.util.net.copy.FDTSession;


public class CmdCheckerTask implements Runnable {

    
    private static final Logger logger = Logger.getLogger(CmdCheckerTask.class.getName());
    private final FDTSession fdtSession;
    private final LISAReportingTask lisaReportingTask;
    private final LisaCtrlNotifier notifier;
    private String cmdToSend;

    public CmdCheckerTask(final FDTSession fdtSession, LISAReportingTask lrt, LisaCtrlNotifier notifier) {
        this.fdtSession = fdtSession;
        this.lisaReportingTask = lrt;
        this.notifier = notifier;
    }

    public void run() {
    	if(cmdToSend == null){
    		cmdToSend = "exec FDTClientController getControlParams " + fdtSession.getMonID();
            logger.log(Level.INFO, "[ CmdCheckerTask ] LISA/ML remote command checker started with sessionID: " + fdtSession.getMonID());
    	}
    	
        if (logger.isLoggable(Level.FINER)) {
            logger.log(Level.FINER, "[ CmdCheckerTask ] LISA/ML remote command checker for sessionID: " + fdtSession.getMonID() + " running");
        }

        try {
            final String response = lisaReportingTask.lisaMon.sendDirectCommand(cmdToSend);
            if (logger.isLoggable(Level.FINER)) {
                logger.log(Level.FINER, "[ CmdCheckerTask ] for sessionID: " + fdtSession.getMonID() + " received: " + response);
            }
            
            if(response != null) {
                notifier.notifyLisaCtrlMsg(response);
            }
            
        } catch (Throwable t) {
            logger.log(Level.WARNING, "[ CmdCheckerTask ] Exception in main loop ", t);
        }

        if (logger.isLoggable(Level.FINEST)) {
            logger.log(Level.FINER, "[ CmdCheckerTask ] LISA/ML remote command checker for sessionID: " + fdtSession.getMonID() + " finished");
        }
    }
}
