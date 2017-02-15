/*
 * $Id: XDRMessageNotifier.java 356 2007-08-16 14:31:17Z ramiro $
 */
package lia.util.net.copy.monitoring.lisa.xdr;

/**
 * 
 * @author Adrian Muraru
 */
public interface XDRMessageNotifier {
    public void notifyXDRMessage(XDRMessage message, XDRGenericComm comm);
    public void notifyXDRCommClosed(XDRGenericComm comm);
}
