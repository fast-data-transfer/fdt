
package lia.util.net.copy.monitoring.lisa;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import lia.util.net.copy.monitoring.lisa.xdr.XDRClient;


public class MonClient {

    private static final Logger logger = Logger.getLogger(MonClient.class.getName());
    XDRClient lisaClient = null;
    private String lisaHost;
    private int lisaPort;

    
    public MonClient(String lisaHost, int lisaPort) throws Exception {
        this.lisaHost = lisaHost;
        this.lisaPort = lisaPort;
        checkAndInitLisaClient();
    }

    
    public void sendClientParameters(String id, Map<String, Double> parameters) throws Exception {
        try {
            StringBuilder sbCommand = new StringBuilder("exec FDTClient monitorTransfer " + id);
            if (parameters != null) {
                for (Map.Entry<String, Double> entry : parameters.entrySet()) {
                    sbCommand.append(" ").append(entry.getKey()).append(" ").append(entry.getValue());
                }
            }
            String sResult = sendDirectCommand(sbCommand.toString());
            if (logger.isLoggable(Level.FINER)) {
                logger.log(Level.FINER, "Send CLIENT params Result: " + sResult);
            }
        } catch (Throwable t) {
            if (logger.isLoggable(Level.FINER)) {
                logger.log(Level.FINEST, " Got exception sending client params to LISA", t);
            }
        }
    }

    
    public void sendServerParameters(String id, Map<String, Double> parameters) throws Exception {
        try {
            StringBuilder sbCommand = new StringBuilder("exec FDTServer monitorTransfer " + id);
            if (parameters != null) {
                for (Map.Entry<String, Double> entry : parameters.entrySet()) {
                    sbCommand.append(" ").append(entry.getKey()).append(" ").append(entry.getValue());
                }
            }
            String sResult = sendDirectCommand(sbCommand.toString());
            if (logger.isLoggable(Level.FINER)) {
                logger.log(Level.FINER, "Send SERVER params Result: " + sResult);
            }
        } catch (Throwable t) {
            if (logger.isLoggable(Level.FINER)) {
                logger.log(Level.FINEST, " Got exception sending server params to LISA", t);
            }
        }
    }

    private synchronized void checkAndInitLisaClient() {
        if (this.lisaClient == null || this.lisaClient.isClosed()) {
            this.lisaClient = XDRClient.getClient(lisaHost, lisaPort);
        }
    }

    public String sendDirectCommand(final String cmd) throws Exception {
        checkAndInitLisaClient();
        if (lisaClient != null) {
            return lisaClient.sendCommand(cmd);
        }
        
        throw new Exception("Unable to connect to LISA / ML modules");
    }
}
