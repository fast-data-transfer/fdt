package lia.util.net.copy.monitoring.lisa;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import lia.util.net.common.Config;
import lia.util.net.copy.FDTSession;
import lia.util.net.copy.disk.DiskReaderManager;
import lia.util.net.copy.disk.DiskWriterManager;
import lia.util.net.copy.monitoring.FDTInternalMonitoringTask;
import lia.util.net.copy.transport.TCPTransportProvider;

public class LISAReportingTask implements Runnable {

    private static final Logger logger = Logger.getLogger(LISAReportingTask.class.getName());

    private static final DiskWriterManager diskWriterManager = DiskWriterManager.getInstance();
    private static final DiskReaderManager diskReaderManager = DiskReaderManager.getInstance();
    
    private final String lisaHost;
    private final int lisaPort;
    
    MonClient lisaMon;
    private static final double MEGA_BYTE_FACTOR = 1024D * 1024D;
    private static final double MEGA_BIT_FACTOR = 1000D * 1000D;
    private boolean errorReported = false;
    
    private static LISAReportingTask _thisInstance;
    
    public static final LISAReportingTask initInstance(String lisaHost, int lisaPort) {
        synchronized(LISAReportingTask.class) {
            if(_thisInstance == null) {
                _thisInstance = new LISAReportingTask(lisaHost, lisaPort);
                LISAReportingTask.class.notifyAll();
            }
        }
        return _thisInstance;
    }

    public static final LISAReportingTask getInstance() {
        synchronized(LISAReportingTask.class) {
            while(_thisInstance == null) {
                try {
                    LISAReportingTask.class.wait(5000);
                    logger.log(Level.WARNING, " getInstace timeout on LISAReporting task ... ");
                }catch(Throwable t) {
                    t.printStackTrace();
                }
            }
            return _thisInstance;
        }
    }
    
    private LISAReportingTask(String lisaHost, int lisaPort) {
        this.lisaHost = lisaHost;
        this.lisaPort = lisaPort;
        lisaMon = null;
    }
    
    public void run() {
        try {
            if(lisaMon == null) {
                try {
                    lisaMon = new MonClient(lisaHost, lisaPort);
                }catch(Throwable t) {
                    if(!errorReported) {
                        logger.log(Level.WARNING, " Cannot connect to lisa", t);
                    } else {
                        logger.log(Level.FINER, " Cannot connect to lisa", t);
                    }
                    errorReported = true;
                }
            }
            
            if(lisaMon == null) return;
            
            errorReported = false;
            
            FDTSession fdtSession = null;
            Iterator<FDTSession> it = null;
            
            double totalNet = 0;
            double totalDisk = 0;
            
            HashMap<String, HashMap<String, Double>> lisaParams = new HashMap<String, HashMap<String, Double>>();
            double rate = 0;
            
            
            it = diskReaderManager.getSessions().iterator();
            while(it.hasNext()) {
                fdtSession = it.next();
                TCPTransportProvider transportProvider = fdtSession.getTransportProvider();
                HashMap<String, Double> fdtSessionParams = new HashMap<String, Double>();
                
                if(transportProvider != null && transportProvider.monitoringTask != null) {
                    rate = ( transportProvider.monitoringTask.getTotalRate() * 8D ) / MEGA_BIT_FACTOR;
                    totalNet += rate;
                    fdtSessionParams.put("NET_OUT", rate);
                }
                

                if(fdtSession.getMonitoringTask() != null) {
                    rate = fdtSession.getMonitoringTask().getTotalRate() /MEGA_BYTE_FACTOR;
                    fdtSessionParams.put("DISK_READ", rate);
                    totalDisk += rate;
                }

                lisaParams.put(fdtSession.getRemoteAddress().getHostAddress()+":"+fdtSession.getRemotePort(), fdtSessionParams);
            }
            
            if(lisaParams.size() > 0) {
                if(logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, " Sending to LISA :- Client Params: " + lisaParams);
                }
                for(Map.Entry<String, HashMap<String, Double>> entry: lisaParams.entrySet()) {
                    HashMap<String, Double> hToSend = entry.getValue();
                    if(hToSend.size() > 0) {
                        lisaMon.sendClientParameters(entry.getKey(), hToSend);
                    }
                }
            }
            
            lisaParams.clear();
            
            
            it = diskWriterManager.getSessions().iterator();
            totalNet = 0;
            totalDisk = 0;
            
            while(it.hasNext()) {
                fdtSession = it.next();
                TCPTransportProvider transportProvider = fdtSession.getTransportProvider();
                HashMap<String, Double> fdtSessionParams = new HashMap<String, Double>();
                
                if(transportProvider != null && transportProvider.monitoringTask != null) {
                    rate = ( transportProvider.monitoringTask.getTotalRate() * 8 ) / MEGA_BIT_FACTOR;
                    totalNet += rate;
                    fdtSessionParams.put("NET_IN", rate);
                }
                

                if(fdtSession.getMonitoringTask() != null) {
                    rate = fdtSession.getMonitoringTask().getTotalRate() /MEGA_BYTE_FACTOR;
                    fdtSessionParams.put("DISK_WRITE", rate);
                    totalDisk += rate;
                }
                
                lisaParams.put(fdtSession.getRemoteAddress().getHostAddress()+":"+fdtSession.getRemotePort(), fdtSessionParams);
                
            }
            
            if(lisaParams.size() > 0) {
                if(logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, " Sending to LISA :- Server Params: " + lisaParams);
                }
                for(Map.Entry<String, HashMap<String, Double>> entry: lisaParams.entrySet()) {
                    HashMap<String, Double> hToSend = entry.getValue();
                    if(hToSend.size() > 0) {
                        lisaMon.sendServerParameters(entry.getKey(), hToSend);
                    }
                }
            }
            
            if(Config.getInstance().getHostName() == null) {
                HashMap<String, Double> localParams = new HashMap<String, Double>();
                localParams.put("CLIENTS_NO", (double)lisaParams.size());
                localParams.put("DISK_WRITE", totalDisk);
                localParams.put("NET_IN", totalNet);
                
                lisaMon.sendServerParameters("FDT_PARAMS", localParams);
            }
            
            HashMap<String, Double> fdtLisaParams = FDTInternalMonitoringTask.getInstance().getLisaParams();            
            
            String key = "FDT_MON:";
            if(Config.getInstance().getHostName() == null) {
                key += Config.getInstance().getPort();
            } else {
                String rPort = "";
                it = diskReaderManager.getSessions().iterator();
                while(it.hasNext()) {
                    fdtSession = it.next();
                    rPort += fdtSession.getLocalPort();
                }

                it = diskWriterManager.getSessions().iterator();
                while(it.hasNext()) {
                    fdtSession = it.next();
                    rPort += fdtSession.getLocalPort();
                }
                
                if(rPort.length() == 0) {
                    rPort = "UNK";
                }
                
                key += rPort;
            }

            if(logger.isLoggable(Level.FINER)) {
                logger.log(Level.FINER, "FDT Params: " + fdtLisaParams + " Key: " + key);
            }
            
            lisaMon.sendServerParameters(key, fdtLisaParams);
            
        }catch(Throwable t) {
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
