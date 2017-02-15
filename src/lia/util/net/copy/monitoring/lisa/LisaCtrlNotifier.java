/*
 * $Id: $
 */ 
package lia.util.net.copy.monitoring.lisa;

/**
 * Receiver of remote commands from LISA/ML modules
 * 
 * @author ramiro
 */
public interface LisaCtrlNotifier {

    public void notifyLisaCtrlMsg(String lisaCtrlMsg);
    
}
